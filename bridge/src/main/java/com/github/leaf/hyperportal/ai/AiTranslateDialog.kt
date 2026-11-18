// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.ai

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

object AiTranslateDialog {
    private val executor = Executors.newCachedThreadPool()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun show(context: Context, sourceText: String) {
        val config = AiRoleSettings.read(context)
        var currentRole = config.activeRole()
        val cancelled = AtomicBoolean(false)
        val activeRequestFlag = AtomicBoolean(false)

        fun dp(v: Int): Int = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            v.toFloat(),
            context.resources.displayMetrics,
        ).toInt()

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(12))
        }

        val roleScroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
        }
        val roleRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        roleScroll.addView(roleRow)
        root.addView(roleScroll)

        val sourcePreview = TextView(context).apply {
            text = sourceText.trim()
            maxLines = 2
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(Color.parseColor("#888888"))
            setPadding(0, dp(10), 0, dp(8))
        }
        root.addView(sourcePreview)

        val resultScroll = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(220),
            )
        }
        val resultText = TextView(context).apply {
            text = "正在连接 ${config.effectiveModel()}..."
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextColor(Color.parseColor("#222222"))
            setTextIsSelectable(true)
            setLineSpacing(dp(4).toFloat(), 1f)
        }
        resultScroll.addView(resultText)
        root.addView(resultScroll)

        fun styleRoleChip(chip: TextView, selected: Boolean) {
            val bg = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(if (selected) Color.parseColor("#0D84FF") else Color.parseColor("#EFEFEF"))
            }
            chip.background = bg
            chip.setTextColor(if (selected) Color.WHITE else Color.parseColor("#333333"))
        }

        val roleChips = mutableListOf<Pair<AiRole, TextView>>()

        fun runTranslation(role: AiRole) {
            activeRequestFlag.set(true)
            val requestCancelled = AtomicBoolean(false)
            activeRequestFlag.set(false)
            currentRole = role
            roleChips.forEach { (r, tv) -> styleRoleChip(tv, r.id == role.id) }
            resultText.text = "[${role.name}] 思考中..."

            executor.execute {
                AiTranslationEngine.streamTranslate(
                    config = config,
                    role = role,
                    text = sourceText,
                    stream = true,
                    isCancelled = { cancelled.get() || requestCancelled.get() || currentRole.id != role.id },
                    callback = object : AiTranslationEngine.StreamCallback {
                        override fun onDelta(delta: String, fullText: String) {
                            mainHandler.post {
                                if (!cancelled.get() && currentRole.id == role.id) {
                                    resultText.text = fullText
                                }
                            }
                        }

                        override fun onComplete(fullText: String) {
                            mainHandler.post {
                                if (!cancelled.get() && currentRole.id == role.id) {
                                    resultText.text = fullText
                                }
                            }
                        }

                        override fun onError(message: String) {
                            mainHandler.post {
                                if (!cancelled.get() && currentRole.id == role.id) {
                                    resultText.text = "请求失败：$message"
                                }
                            }
                        }
                    },
                )
            }
        }

        config.roles.forEach { role ->
            val chip = TextView(context).apply {
                text = role.name
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(dp(12), dp(6), dp(12), dp(6))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    marginEnd = dp(8)
                }
                setOnClickListener {
                    if (currentRole.id != role.id) {
                        runTranslation(role)
                    }
                }
            }
            styleRoleChip(chip, role.id == currentRole.id)
            roleChips.add(role to chip)
            roleRow.addView(chip)
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle("AI 助手")
            .setView(root)
            .setPositiveButton("复制结果") { _, _ ->
                val out = resultText.text?.toString().orEmpty()
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("HyperPortal AI", out))
                Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("关闭") { d, _ ->
                cancelled.set(true)
                d.dismiss()
            }
            .setOnDismissListener {
                cancelled.set(true)
            }
            .create()

        dialog.show()
        runTranslation(currentRole)
    }
}
