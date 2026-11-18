// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.ai

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
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
        val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

        fun dp(v: Int): Int = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            v.toFloat(),
            context.resources.displayMetrics,
        ).toInt()

        val glassBgColor = if (isDark) Color.parseColor("#E01A202C") else Color.parseColor("#E6F8FAFF")
        val glassChipBg = if (isDark) Color.parseColor("#1FFFFFFF") else Color.parseColor("#99FFFFFF")
        val glassBorderColor = if (isDark) Color.parseColor("#33FFFFFF") else Color.parseColor("#CCFFFFFF")
        val activeBlue = if (isDark) Color.parseColor("#CC3898FF") else Color.parseColor("#DD0D84FF")
        val textMain = if (isDark) Color.parseColor("#F3F6FC") else Color.parseColor("#111827")
        val textSub = if (isDark) Color.parseColor("#9AA6B8") else Color.parseColor("#5B6475")

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(16))
            background = GradientDrawable().apply {
                cornerRadius = dp(26).toFloat()
                setColor(glassBgColor)
                setStroke(dp(1), glassBorderColor)
            }
        }

        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(12))
        }
        val titleTv = TextView(context).apply {
            text = "AI 助手"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            setTextColor(textMain)
            paint.isFakeBoldText = true
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val modelTv = TextView(context).apply {
            text = config.effectiveModel()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(textSub)
        }
        headerRow.addView(titleTv)
        headerRow.addView(modelTv)
        card.addView(headerRow)

        val roleScroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
        }
        val roleRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        roleScroll.addView(roleRow)
        card.addView(roleScroll)

        val sourcePreview = TextView(context).apply {
            text = sourceText.trim()
            maxLines = 2
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(textSub)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(glassChipBg)
                setStroke(dp(1), glassBorderColor)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(12)
                bottomMargin = dp(10)
            }
        }
        card.addView(sourcePreview)

        val resultScroll = ScrollView(context).apply {
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(glassChipBg)
                setStroke(dp(1), glassBorderColor)
            }
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(210),
            )
        }
        val resultText = TextView(context).apply {
            text = "正在连接 ${config.effectiveModel()}..."
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextColor(textMain)
            setTextIsSelectable(true)
            setLineSpacing(dp(4).toFloat(), 1f)
        }
        resultScroll.addView(resultText)
        card.addView(resultScroll)

        val actionsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, dp(14), 0, 0)
        }
        val closeBtn = TextView(context).apply {
            text = "关闭"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(textMain)
            paint.isFakeBoldText = true
            setPadding(dp(18), dp(8), dp(18), dp(8))
            background = GradientDrawable().apply {
                cornerRadius = dp(999).toFloat()
                setColor(glassChipBg)
                setStroke(dp(1), glassBorderColor)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                marginEnd = dp(10)
            }
        }
        val copyBtn = TextView(context).apply {
            text = "复制结果"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(Color.WHITE)
            paint.isFakeBoldText = true
            setPadding(dp(18), dp(8), dp(18), dp(8))
            background = GradientDrawable().apply {
                cornerRadius = dp(999).toFloat()
                setColor(activeBlue)
            }
        }
        actionsRow.addView(closeBtn)
        actionsRow.addView(copyBtn)
        card.addView(actionsRow)

        fun styleRoleChip(chip: TextView, selected: Boolean) {
            chip.background = GradientDrawable().apply {
                cornerRadius = dp(999).toFloat()
                setColor(if (selected) activeBlue else glassChipBg)
                setStroke(dp(1), glassBorderColor)
            }
            chip.setTextColor(if (selected) Color.WHITE else textMain)
        }

        val roleChips = mutableListOf<Pair<AiRole, TextView>>()

        fun runTranslation(role: AiRole) {
            currentRole = role
            roleChips.forEach { (r, tv) -> styleRoleChip(tv, r.id == role.id) }
            resultText.text = "[${role.name}] 思考中..."

            executor.execute {
                AiTranslationEngine.streamTranslate(
                    config = config,
                    role = role,
                    text = sourceText,
                    stream = true,
                    isCancelled = { cancelled.get() || currentRole.id != role.id },
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
                paint.isFakeBoldText = true
                setPadding(dp(13), dp(6), dp(13), dp(6))
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

        val dialog = Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(card)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window?.setLayout(
                (context.resources.displayMetrics.widthPixels * 0.90f).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            setOnDismissListener { cancelled.set(true) }
        }

        closeBtn.setOnClickListener {
            cancelled.set(true)
            dialog.dismiss()
        }
        copyBtn.setOnClickListener {
            val out = resultText.text?.toString().orEmpty()
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("HyperPortal AI", out))
            Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
            cancelled.set(true)
            dialog.dismiss()
        }

        dialog.show()
        runTranslation(currentRole)
    }
}
