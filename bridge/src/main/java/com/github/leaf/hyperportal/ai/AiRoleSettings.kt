// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class AiRole(
    val id: String,
    val name: String,
    val prompt: String,
)

data class AiTranslationConfig(
    val apiType: Int = API_TYPE_OPENAI,
    val baseUrl: String = DEFAULT_OPENAI_BASE_URL,
    val apiKey: String = "",
    val model: String = "",
    val activeRoleId: String = DEFAULT_ROLES.first().id,
    val roles: List<AiRole> = DEFAULT_ROLES,
) {
    fun effectiveBaseUrl(): String {
        val raw = baseUrl.trim().trimEnd('/')
        return if (raw.isEmpty()) {
            if (apiType == API_TYPE_CLAUDE) DEFAULT_CLAUDE_BASE_URL else DEFAULT_OPENAI_BASE_URL
        } else {
            raw
        }
    }

    fun effectiveModel(): String {
        val raw = model.trim()
        return if (raw.isEmpty()) {
            if (apiType == API_TYPE_CLAUDE) DEFAULT_CLAUDE_MODEL else DEFAULT_OPENAI_MODEL
        } else {
            raw
        }
    }

    fun activeRole(): AiRole {
        return roles.firstOrNull { it.id == activeRoleId } ?: roles.firstOrNull() ?: DEFAULT_ROLES.first()
    }

    fun isConfigured(): Boolean = apiKey.isNotBlank()

    companion object {
        const val API_TYPE_OPENAI = 0
        const val API_TYPE_CLAUDE = 1

        const val DEFAULT_OPENAI_BASE_URL = "https://api.openai.com/v1"
        const val DEFAULT_CLAUDE_BASE_URL = "https://api.anthropic.com/v1"
        const val DEFAULT_OPENAI_MODEL = "gpt-4o-mini"
        const val DEFAULT_CLAUDE_MODEL = "claude-3-5-haiku-latest"

        val DEFAULT_ROLES = listOf(
            AiRole(
                id = "default_translator",
                name = "专业翻译官",
                prompt = "你是一个专业、地道的多语言翻译官。如果输入是中文，请翻译成自然流畅的英文；如果输入是外文，请翻译成自然地道的简体中文。只输出译文，不要附加任何解释或多余符号。",
            ),
            AiRole(
                id = "tech_translator",
                name = "技术文档翻译",
                prompt = "你是一个资深软件工程师与技术翻译专家。请将输入的技术文本翻译为简体中文（若已是中文则译为英文），保留专有名词、代码标识符和术语的准确性，语言简洁专业。",
            ),
            AiRole(
                id = "english_tutor",
                name = "英语精讲",
                prompt = "你是一个耐心的英语老师。请先给出输入内容的准确中文翻译，然后简要解析其中的核心词汇、短语搭配或语法结构，排版清晰。",
            ),
            AiRole(
                id = "summarizer",
                name = "核心提炼",
                prompt = "你是一个信息提炼专家。请用简明扼要的简体中文分点总结用户输入文本的核心观点与关键信息。",
            ),
        )
    }
}

object AiRoleSettings {
    private const val PREFS_NAME = "hyperportal_ai_settings"
    private const val KEY_API_TYPE = "api_type"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_MODEL = "model"
    private const val KEY_ACTIVE_ROLE_ID = "active_role_id"
    private const val KEY_ROLES_JSON = "roles_json"

    fun read(context: Context): AiTranslationConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val apiType = prefs.getInt(KEY_API_TYPE, AiTranslationConfig.API_TYPE_OPENAI)
            .let { if (it == AiTranslationConfig.API_TYPE_CLAUDE) it else AiTranslationConfig.API_TYPE_OPENAI }
        val baseUrl = prefs.getString(KEY_BASE_URL, "") ?: ""
        val apiKey = prefs.getString(KEY_API_KEY, "") ?: ""
        val model = prefs.getString(KEY_MODEL, "") ?: ""
        val roles = parseRoles(prefs.getString(KEY_ROLES_JSON, null))
        val activeRoleId = prefs.getString(KEY_ACTIVE_ROLE_ID, null)
            ?.takeIf { id -> roles.any { it.id == id } }
            ?: roles.first().id

        return AiTranslationConfig(
            apiType = apiType,
            baseUrl = baseUrl,
            apiKey = apiKey,
            model = model,
            activeRoleId = activeRoleId,
            roles = roles,
        )
    }

    fun save(context: Context, config: AiTranslationConfig) {
        val safeRoles = config.roles.ifEmpty { AiTranslationConfig.DEFAULT_ROLES }
        val arr = JSONArray()
        safeRoles.forEach { role ->
            arr.put(
                JSONObject().apply {
                    put("id", role.id)
                    put("name", role.name)
                    put("prompt", role.prompt)
                },
            )
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_API_TYPE, config.apiType)
            .putString(KEY_BASE_URL, config.baseUrl.trim())
            .putString(KEY_API_KEY, config.apiKey.trim())
            .putString(KEY_MODEL, config.model.trim())
            .putString(KEY_ACTIVE_ROLE_ID, config.activeRoleId)
            .putString(KEY_ROLES_JSON, arr.toString())
            .apply()
    }

    fun newRoleId(): String = "role_${UUID.randomUUID().toString().take(8)}"

    private fun parseRoles(raw: String?): List<AiRole> {
        if (raw.isNullOrBlank()) return AiTranslationConfig.DEFAULT_ROLES
        return try {
            val arr = JSONArray(raw)
            val list = ArrayList<AiRole>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id").trim()
                val name = obj.optString("name").trim()
                val prompt = obj.optString("prompt").trim()
                if (id.isNotEmpty() && name.isNotEmpty()) {
                    list.add(AiRole(id = id, name = name, prompt = prompt))
                }
            }
            list.ifEmpty { AiTranslationConfig.DEFAULT_ROLES }
        } catch (_: Exception) {
            AiTranslationConfig.DEFAULT_ROLES
        }
    }
}
