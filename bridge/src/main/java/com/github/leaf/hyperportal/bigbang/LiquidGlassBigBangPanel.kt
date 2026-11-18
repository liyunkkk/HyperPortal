// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.bigbang

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.leaf.hyperportal.ai.AiRoleSettings
import com.github.leaf.hyperportal.ai.AiTranslateDialog

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiquidGlassBigBangPanel(
    words: List<String>,
    fullText: String,
    onSearch: (String) -> Unit,
    onEdit: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()

    val glassBg = if (dark) Color(0xA6161B24) else Color(0x9EF5F8FF)
    val glassBar = if (dark) Color(0x851E2430) else Color(0x7AFFFFFF)
    val glassBorder = if (dark) Color(0x2EFFFFFF) else Color(0xC7FFFFFF)
    val chipBg = if (dark) Color(0x1AFFFFFF) else Color(0x8FFFFFFF)
    val chipActiveBg = if (dark) Color(0xBF3898FF) else Color(0xC70D84FF)
    val textMain = if (dark) Color(0xFFF3F6FC) else Color(0xFF111827)

    val selectedIndices = remember(words) {
        mutableStateListOf<Int>().apply {
            if (words.isNotEmpty()) {
                for (i in words.indices) add(i)
            }
        }
    }

    fun selectedText(): String {
        if (selectedIndices.isEmpty()) return fullText
        return selectedIndices.sorted().mapNotNull { words.getOrNull(it) }.joinToString("")
    }

    fun triggerAiTranslate(text: String) {
        if (text.isBlank()) return
        if (AiRoleSettings.read(context).isConfigured()) {
            AiTranslateDialog.show(context, text)
        } else {
            Toast.makeText(context, "请先在设置 → AI 助手中配置 API Key", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyText(text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("HyperPortal", text))
        Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
    }

    fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "分享文本"))
    }

    val panelShape = RoundedCornerShape(32.dp)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 18.dp)
            .clip(panelShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        glassBg,
                        if (dark) Color(0xB8121722) else Color(0xB8EEF4FF),
                    ),
                ),
            )
            .border(1.2.dp, glassBorder, panelShape),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部玻璃栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .background(glassBar)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                GlassCircleButton("✎", chipBg, glassBorder, textMain) { onEdit() }
                Text("文本分词", color = textMain, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                GlassCircleButton("↗", chipBg, glassBorder, textMain) { shareText(selectedText()) }
            }

            // 中间词块区 + 悬浮操作胶囊
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                // 悬浮操作胶囊栏
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(glassBar)
                        .border(1.dp, glassBorder, RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlassCircleButton("搜", chipBg, glassBorder, textMain) { onSearch(selectedText()) }
                    GlassCircleButton("译", chipActiveBg, glassBorder, Color.White) { triggerAiTranslate(selectedText()) }
                    GlassCircleButton("全", chipBg, glassBorder, textMain) {
                        if (selectedIndices.size == words.size) {
                            selectedIndices.clear()
                        } else {
                            selectedIndices.clear()
                            for (i in words.indices) selectedIndices.add(i)
                        }
                    }
                    GlassCircleButton("复", chipBg, glassBorder, textMain) { copyText(selectedText()) }
                }

                Spacer(Modifier.height(14.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    words.forEachIndexed { idx, word ->
                        val active = selectedIndices.contains(idx)
                        val shape = RoundedCornerShape(14.dp)
                        Box(
                            modifier = Modifier
                                .clip(shape)
                                .background(if (active) chipActiveBg else chipBg)
                                .border(1.dp, glassBorder, shape)
                                .clickable {
                                    if (active) selectedIndices.remove(idx) else selectedIndices.add(idx)
                                }
                                .padding(horizontal = 13.dp, vertical = 7.dp),
                        ) {
                            Text(
                                text = word,
                                color = if (active) Color.White else textMain,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }

            // 底部玻璃栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .background(glassBar)
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                GlassCircleButton("全", chipBg, glassBorder, textMain) {
                    selectedIndices.clear()
                    for (i in words.indices) selectedIndices.add(i)
                }
                GlassCircleButton("✕", chipBg, glassBorder, textMain) { onClose() }
                GlassCircleButton("译", chipActiveBg, glassBorder, Color.White) { triggerAiTranslate(fullText) }
            }
        }
    }
}

@Composable
private fun GlassCircleButton(
    label: String,
    bg: Color,
    border: Color,
    textColor: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
