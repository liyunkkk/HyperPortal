// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.ui

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.github.leaf.hyperportal.ai.AiRole
import com.github.leaf.hyperportal.ai.AiRoleSettings
import com.github.leaf.hyperportal.ai.AiTranslationConfig
import com.github.leaf.hyperportal.ai.AiTranslationEngine
import com.github.leaf.hyperportal.ui.component.PortalPageScaffold
import com.github.leaf.hyperportal.ui.component.nextBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference

class AiSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiuixTheme {
                AiSettingsScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
private fun AiSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var config by remember { mutableStateOf(AiRoleSettings.read(context)) }
    var testing by remember { mutableStateOf(false) }
    var fetchingModels by remember { mutableStateOf(false) }

    fun updateAndSave(newConfig: AiTranslationConfig) {
        config = newConfig
        AiRoleSettings.save(context, newConfig)
    }

    fun showRoleEditorDialog(existing: AiRole?) {
        val nameInput = android.widget.EditText(context).apply {
            hint = "角色名称（如：专业翻译官）"
            setText(existing?.name.orEmpty())
        }
        val promptInput = android.widget.EditText(context).apply {
            hint = "System Prompt（系统提示词）"
            minLines = 3
            maxLines = 6
            setText(existing?.prompt.orEmpty())
        }
        val container = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 24, 48, 8)
            addView(nameInput)
            addView(promptInput)
        }
        AlertDialog.Builder(context)
            .setTitle(if (existing == null) "新建 AI 角色" else "编辑 AI 角色")
            .setView(container)
            .setPositiveButton("保存") { _, _ ->
                val name = nameInput.text?.toString()?.trim().orEmpty()
                val prompt = promptInput.text?.toString()?.trim().orEmpty()
                if (name.isEmpty()) {
                    Toast.makeText(context, "角色名称不能为空", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val updatedRoles = if (existing == null) {
                    val created = AiRole(AiRoleSettings.newRoleId(), name, prompt)
                    config.roles + created
                } else {
                    config.roles.map {
                        if (it.id == existing.id) it.copy(name = name, prompt = prompt) else it
                    }
                }
                updateAndSave(config.copy(roles = updatedRoles))
            }
            .setNegativeButton("取消", null)
            .apply {
                if (existing != null && config.roles.size > 1) {
                    setNeutralButton("删除") { _, _ ->
                        val remaining = config.roles.filterNot { it.id == existing.id }
                        val nextActive = if (config.activeRoleId == existing.id) {
                            remaining.first().id
                        } else {
                            config.activeRoleId
                        }
                        updateAndSave(config.copy(roles = remaining, activeRoleId = nextActive))
                    }
                }
            }
            .show()
    }

    PortalPageScaffold(title = "AI 翻译与角色", bottomPadding = 24.dp) {
        item(key = "title_api") {
            SmallTitle(text = "接口配置")
        }
        item(key = "card_api") {
            Card(modifier = Modifier.nextBlock()) {
                val apiItems = listOf("OpenAI 兼容 (/chat/completions)", "Anthropic Claude (/messages)")
                OverlayDropdownPreference(
                    title = "接口协议",
                    summary = apiItems[config.apiType],
                    items = apiItems,
                    selectedIndex = config.apiType,
                    onSelectedIndexChange = { idx ->
                        updateAndSave(config.copy(apiType = idx))
                    },
                )
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    OutlinedTextField(
                        value = config.baseUrl,
                        onValueChange = { updateAndSave(config.copy(baseUrl = it)) },
                        label = { Text("Base URL") },
                        placeholder = {
                            Text(
                                if (config.apiType == AiTranslationConfig.API_TYPE_CLAUDE) {
                                    AiTranslationConfig.DEFAULT_CLAUDE_BASE_URL
                                } else {
                                    AiTranslationConfig.DEFAULT_OPENAI_BASE_URL
                                },
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = config.apiKey,
                        onValueChange = { updateAndSave(config.copy(apiKey = it)) },
                        label = { Text("API Key") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = config.model,
                        onValueChange = { updateAndSave(config.copy(model = it)) },
                        label = { Text("模型名称 (留空使用默认)") },
                        placeholder = { Text(config.effectiveModel()) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        TextButton(
                            text = if (fetchingModels) "拉取中..." else "拉取模型列表",
                            enabled = !fetchingModels && !testing,
                            onClick = {
                                fetchingModels = true
                                scope.launch {
                                    val res = withContext(Dispatchers.IO) {
                                        AiTranslationEngine.fetchModels(config)
                                    }
                                    fetchingModels = false
                                    res.onSuccess { models ->
                                        if (models.isEmpty()) {
                                            Toast.makeText(context, "未返回可用模型", Toast.LENGTH_SHORT).show()
                                        } else {
                                            AlertDialog.Builder(context)
                                                .setTitle("选择模型 (${models.size})")
                                                .setItems(models.toTypedArray()) { _, which ->
                                                    updateAndSave(config.copy(model = models[which]))
                                                }
                                                .show()
                                        }
                                    }.onFailure { err ->
                                        AlertDialog.Builder(context)
                                            .setTitle("拉取失败")
                                            .setMessage(err.message ?: "未知错误")
                                            .setPositiveButton("确定", null)
                                            .show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            enabled = !testing && !fetchingModels,
                            onClick = {
                                testing = true
                                scope.launch {
                                    val res = withContext(Dispatchers.IO) {
                                        AiTranslationEngine.testConnection(config)
                                    }
                                    testing = false
                                    AlertDialog.Builder(context)
                                        .setTitle(if (res.isSuccess) "连接成功" else "连接失败")
                                        .setMessage(res.getOrElse { it.message ?: "未知错误" })
                                        .setPositiveButton("确定", null)
                                        .show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(if (testing) "测试中..." else "测试连接")
                        }
                    }
                }
            }
        }

        item(key = "title_roles") {
            SmallTitle(text = "自定义 AI 角色（点击设为默认，长按或点右侧编辑）")
        }
        item(key = "card_roles") {
            Card(modifier = Modifier.nextBlock()) {
                val roleNames = config.roles.map { it.name }
                val activeIdx = config.roles.indexOfFirst { it.id == config.activeRoleId }.coerceAtLeast(0)
                OverlayDropdownPreference(
                    title = "当前默认角色",
                    summary = config.activeRole().name,
                    items = roleNames,
                    selectedIndex = activeIdx,
                    onSelectedIndexChange = { idx ->
                        val picked = config.roles.getOrNull(idx) ?: return@OverlayDropdownPreference
                        updateAndSave(config.copy(activeRoleId = picked.id))
                    },
                )
                config.roles.forEach { role ->
                    val isActive = role.id == config.activeRoleId
                    ArrowPreference(
                        title = if (isActive) "${role.name}（默认）" else role.name,
                        summary = role.prompt.ifBlank { "无系统提示词" },
                        onClick = { showRoleEditorDialog(role) },
                    )
                }
                ArrowPreference(
                    title = "+ 新建 AI 角色",
                    summary = "自定义角色名称与 System Prompt",
                    onClick = { showRoleEditorDialog(null) },
                )
            }
        }
    }
}
