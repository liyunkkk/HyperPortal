// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.ai

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

object AiTranslationEngine {
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 45_000
    private const val ANTHROPIC_VERSION = "2023-06-01"

    interface StreamCallback {
        fun onDelta(delta: String, fullText: String)
        fun onComplete(fullText: String)
        fun onError(message: String)
    }

    fun buildChatUrl(baseUrl: String, apiType: Int): String {
        val normalized = baseUrl.trim().trimEnd('/')
        return if (apiType == AiTranslationConfig.API_TYPE_CLAUDE) {
            if (normalized.endsWith("/messages")) normalized else "$normalized/messages"
        } else {
            if (normalized.endsWith("/chat/completions")) normalized else "$normalized/chat/completions"
        }
    }

    fun buildModelsUrl(baseUrl: String): String {
        val normalized = baseUrl.trim().trimEnd('/')
        return if (normalized.endsWith("/models")) normalized else "$normalized/models"
    }

    fun fetchModels(config: AiTranslationConfig): Result<List<String>> {
        if (config.apiKey.isBlank()) {
            return Result.failure(IllegalArgumentException("请先填写 API Key"))
        }
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(buildModelsUrl(config.effectiveBaseUrl()))
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = CONNECT_TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
                if (config.apiType == AiTranslationConfig.API_TYPE_CLAUDE) {
                    setRequestProperty("x-api-key", config.apiKey.trim())
                    setRequestProperty("anthropic-version", ANTHROPIC_VERSION)
                } else {
                    setRequestProperty("Authorization", "Bearer ${config.apiKey.trim()}")
                }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                return Result.failure(IllegalStateException("HTTP $code: ${extractErrorMessage(body)}"))
            }
            val root = JSONObject(body)
            val data = root.optJSONArray("data") ?: JSONArray()
            val models = ArrayList<String>(data.length())
            for (i in 0 until data.length()) {
                val id = data.optJSONObject(i)?.optString("id")?.trim().orEmpty()
                if (id.isNotEmpty()) models.add(id)
            }
            Result.success(models.sorted())
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            conn?.disconnect()
        }
    }

    fun testConnection(config: AiTranslationConfig): Result<String> {
        if (config.apiKey.isBlank()) {
            return Result.failure(IllegalArgumentException("请先填写 API Key"))
        }
        val start = System.currentTimeMillis()
        var resultText: String? = null
        var errorText: String? = null
        streamTranslate(
            config = config,
            role = AiRole("test", "Test", "Reply with OK only."),
            text = "OK",
            stream = false,
            callback = object : StreamCallback {
                override fun onDelta(delta: String, fullText: String) {}
                override fun onComplete(fullText: String) {
                    resultText = fullText
                }
                override fun onError(message: String) {
                    errorText = message
                }
            },
        )
        val cost = System.currentTimeMillis() - start
        return if (errorText != null) {
            Result.failure(IllegalStateException(errorText))
        } else {
            Result.success("连接成功 (${cost}ms) · 模型 ${config.effectiveModel()} · 返回: ${resultText?.take(30).orEmpty()}")
        }
    }

    fun streamTranslate(
        config: AiTranslationConfig,
        role: AiRole,
        text: String,
        stream: Boolean = true,
        callback: StreamCallback,
        isCancelled: () -> Boolean = { false },
    ) {
        if (config.apiKey.isBlank()) {
            callback.onError("未配置 API Key，请先在设置中配置")
            return
        }
        if (text.isBlank()) {
            callback.onError("待处理文本为空")
            return
        }

        var conn: HttpURLConnection? = null
        try {
            val url = URL(buildChatUrl(config.effectiveBaseUrl(), config.apiType))
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", if (stream) "text/event-stream" else "application/json")
                if (config.apiType == AiTranslationConfig.API_TYPE_CLAUDE) {
                    setRequestProperty("x-api-key", config.apiKey.trim())
                    setRequestProperty("anthropic-version", ANTHROPIC_VERSION)
                } else {
                    setRequestProperty("Authorization", "Bearer ${config.apiKey.trim()}")
                }
            }

            val payload = buildRequestJson(
                apiType = config.apiType,
                model = config.effectiveModel(),
                systemPrompt = role.prompt,
                userText = text,
                stream = stream,
            )
            OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use {
                it.write(payload)
                it.flush()
            }

            val code = conn.responseCode
            if (code !in 200..299) {
                val errBody = conn.errorStream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
                callback.onError("HTTP $code: ${extractErrorMessage(errBody)}")
                return
            }

            if (!stream) {
                val body = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val parsed = parseNonStreamResponse(config.apiType, body)
                if (parsed.isNotEmpty()) {
                    callback.onComplete(parsed)
                } else {
                    callback.onError("模型返回为空")
                }
                return
            }

            val sb = StringBuilder()
            BufferedReader(InputStreamReader(conn.inputStream, StandardCharsets.UTF_8)).use { reader ->
                while (true) {
                    if (isCancelled()) return
                    val line = reader.readLine() ?: break
                    if (!line.startsWith("data:")) continue
                    val data = line.removePrefix("data:").trim()
                    if (data.isEmpty() || data == "[DONE]") continue
                    val delta = parseStreamDelta(config.apiType, data)
                    if (delta.isNotEmpty()) {
                        sb.append(delta)
                        callback.onDelta(delta, sb.toString())
                    }
                }
            }
            if (!isCancelled()) {
                val finalOut = sb.toString().trim()
                if (finalOut.isNotEmpty()) {
                    callback.onComplete(finalOut)
                } else {
                    callback.onError("流式响应未返回有效内容")
                }
            }
        } catch (e: Exception) {
            if (!isCancelled()) {
                callback.onError(e.message ?: e.javaClass.simpleName)
            }
        } finally {
            conn?.disconnect()
        }
    }

    private fun buildRequestJson(
        apiType: Int,
        model: String,
        systemPrompt: String,
        userText: String,
        stream: Boolean,
    ): String {
        val body = JSONObject()
        body.put("model", model)
        body.put("stream", stream)
        if (apiType == AiTranslationConfig.API_TYPE_CLAUDE) {
            body.put("max_tokens", 2048)
            if (systemPrompt.isNotBlank()) {
                body.put("system", systemPrompt)
            }
            val messages = JSONArray().put(
                JSONObject().apply {
                    put("role", "user")
                    put("content", userText)
                },
            )
            body.put("messages", messages)
        } else {
            body.put("temperature", 0.3)
            val messages = JSONArray()
            if (systemPrompt.isNotBlank()) {
                messages.put(
                    JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    },
                )
            }
            messages.put(
                JSONObject().apply {
                    put("role", "user")
                    put("content", userText)
                },
            )
            body.put("messages", messages)
        }
        return body.toString()
    }

    private fun parseStreamDelta(apiType: Int, jsonLine: String): String {
        return try {
            val obj = JSONObject(jsonLine)
            if (apiType == AiTranslationConfig.API_TYPE_CLAUDE) {
                if (obj.optString("type") == "content_block_delta") {
                    obj.optJSONObject("delta")?.optString("text").orEmpty()
                } else {
                    ""
                }
            } else {
                val choices = obj.optJSONArray("choices") ?: return ""
                val first = choices.optJSONObject(0) ?: return ""
                val delta = first.optJSONObject("delta") ?: return ""
                delta.optString("content").orEmpty()
            }
        } catch (_: Exception) {
            ""
        }
    }

    private fun parseNonStreamResponse(apiType: Int, body: String): String {
        return try {
            val obj = JSONObject(body)
            if (apiType == AiTranslationConfig.API_TYPE_CLAUDE) {
                val content = obj.optJSONArray("content") ?: return ""
                buildString {
                    for (i in 0 until content.length()) {
                        val item = content.optJSONObject(i) ?: continue
                        if (item.optString("type") == "text") {
                            append(item.optString("text"))
                        }
                    }
                }.trim()
            } else {
                val choices = obj.optJSONArray("choices") ?: return ""
                choices.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
                    ?.trim()
                    .orEmpty()
            }
        } catch (_: Exception) {
            ""
        }
    }

    private fun extractErrorMessage(body: String): String {
        if (body.isBlank()) return "Empty response"
        return try {
            val obj = JSONObject(body)
            obj.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
                ?: obj.optString("message").takeIf { it.isNotBlank() }
                ?: body.take(160)
        } catch (_: Exception) {
            body.take(160)
        }
    }
}
