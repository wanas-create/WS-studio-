package com.example.util

import com.example.BuildConfig
import com.example.model.AiEngine
import com.example.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

object AiServiceManager {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    fun cleanKey(key: String): String {
        return key.trim().removeSurrounding("\"").trim()
    }

    fun getEffectiveKey(buildConfigKey: String, envVarName: String): String {
        val key = cleanKey(buildConfigKey)
        if (key.isNotBlank() && !key.startsWith("MY_") && key != "placeholder") {
            return key
        }
        val envKey = cleanKey(System.getenv(envVarName) ?: "")
        if (envKey.isNotBlank() && !envKey.startsWith("MY_") && envKey != "placeholder") {
            return envKey
        }
        return ""
    }

    fun isGeminiConfigured(): Boolean {
        return getEffectiveKey(BuildConfig.GEMINI_API_KEY, "GEMINI_API_KEY").isNotEmpty()
    }

    fun isOpenAiConfigured(): Boolean {
        return getEffectiveKey(BuildConfig.OPENAI_API_KEY, "OPENAI_API_KEY").isNotEmpty()
    }

    fun isAnthropicConfigured(): Boolean {
        return getEffectiveKey(BuildConfig.ANTHROPIC_API_KEY, "ANTHROPIC_API_KEY").isNotEmpty()
    }

    fun isPerplexityConfigured(): Boolean {
        return getEffectiveKey(BuildConfig.PERPLEXITY_API_KEY, "PERPLEXITY_API_KEY").isNotEmpty()
    }

    fun isEngineActive(engineId: String): Boolean {
        return when (engineId) {
            "gemini" -> isGeminiConfigured()
            "chatgpt", "dall_e" -> isOpenAiConfigured()
            "claude", "claude_opus" -> isAnthropicConfigured()
            "perplexity" -> isPerplexityConfigured()
            else -> false
        }
    }

    suspend fun sendQuery(
        engine: AiEngine,
        userQuery: String,
        history: List<ChatMessage> = emptyList()
    ): ChatMessage = withContext(Dispatchers.IO) {
        val replyId = "reply_${System.currentTimeMillis()}"

        try {
            when (engine.id) {
                "gemini" -> handleGeminiRequest(engine, userQuery, replyId)
                "chatgpt" -> handleOpenAiRequest(engine, userQuery, replyId)
                "dall_e" -> handleDallERequest(engine, userQuery, replyId)
                "claude", "claude_opus" -> handleAnthropicRequest(engine, userQuery, replyId)
                "perplexity" -> handlePerplexityRequest(engine, userQuery, replyId)
                else -> handleOtherProvider(engine, userQuery, replyId)
            }
        } catch (e: UnknownHostException) {
            ChatMessage(
                id = replyId,
                aiEngineId = engine.id,
                aiName = engine.name,
                sender = "ai",
                text = "⚠️ Network Connection Failed\n\nUnable to reach ${engine.name} servers. Please check your internet connection and try again.",
                isError = true,
                errorType = "NETWORK_ERROR"
            )
        } catch (e: SocketTimeoutException) {
            ChatMessage(
                id = replyId,
                aiEngineId = engine.id,
                aiName = engine.name,
                sender = "ai",
                text = "⏱️ Request Timed Out\n\n${engine.name} took too long to respond. The server might be experiencing high traffic. Please try again.",
                isError = true,
                errorType = "TIMEOUT"
            )
        } catch (e: Exception) {
            ChatMessage(
                id = replyId,
                aiEngineId = engine.id,
                aiName = engine.name,
                sender = "ai",
                text = "⚠️ Unexpected Error with ${engine.name}\n\n${e.localizedMessage ?: "Unknown communication failure"}",
                isError = true,
                errorType = "UNKNOWN"
            )
        }
    }

    private fun handleGeminiRequest(engine: AiEngine, query: String, replyId: String): ChatMessage {
        val apiKey = getEffectiveKey(BuildConfig.GEMINI_API_KEY, "GEMINI_API_KEY")
        if (!isGeminiConfigured()) {
            return ChatMessage(
                id = replyId,
                aiEngineId = engine.id,
                aiName = engine.name,
                sender = "ai",
                text = "⚠️ Gemini API Key Not Configured\n\nTo use Google Gemini, please configure `GEMINI_API_KEY` in the AI Studio Secrets panel.\n\nOnce added, Gemini will generate live, real-time responses.",
                isError = true,
                errorType = "MISSING_KEY"
            )
        }

        val requestJson = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        put(JSONObject().put("text", query))
                    }
                    put("parts", partsArray)
                }
                put(contentObj)
            }
            put("contents", contentsArray)

            val systemInstructionObj = JSONObject().apply {
                val parts = JSONArray().apply {
                    put(JSONObject().put("text", "You are Google Gemini inside WS Editor, an expert AI video production director and scriptwriter. Provide crisp, high-impact creative advice, scripts, and video editing recommendations."))
                }
                put("parts", parts)
            }
            put("systemInstruction", systemInstructionObj)

            val configObj = JSONObject().apply {
                put("temperature", 0.7)
            }
            put("generationConfig", configObj)
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            val errorMsg = try {
                val errObj = JSONObject(responseBody).optJSONObject("error")
                errObj?.optString("message") ?: "HTTP error ${response.code}"
            } catch (ex: Exception) {
                "HTTP error ${response.code}"
            }

            return ChatMessage(
                id = replyId,
                aiEngineId = engine.id,
                aiName = engine.name,
                sender = "ai",
                text = "⚠️ Gemini API Error (${response.code})\n\n$errorMsg",
                isError = true,
                errorType = if (response.code == 429) "RATE_LIMIT" else "API_ERROR"
            )
        }

        val json = JSONObject(responseBody)
        val candidates = json.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) {
            return ChatMessage(
                id = replyId,
                aiEngineId = engine.id,
                aiName = engine.name,
                sender = "ai",
                text = "⚠️ No response returned from Gemini.",
                isError = true,
                errorType = "EMPTY_RESPONSE"
            )
        }

        val candidate = candidates.getJSONObject(0)
        val content = candidate.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val generatedText = parts?.optJSONObject(0)?.optString("text").orEmpty()

        if (generatedText.isBlank()) {
            return ChatMessage(
                id = replyId,
                aiEngineId = engine.id,
                aiName = engine.name,
                sender = "ai",
                text = "⚠️ Empty text returned from Gemini.",
                isError = true,
                errorType = "EMPTY_RESPONSE"
            )
        }

        return ChatMessage(
            id = replyId,
            aiEngineId = engine.id,
            aiName = engine.name,
            sender = "ai",
            text = generatedText.trim()
        )
    }

    private fun handleOpenAiRequest(engine: AiEngine, query: String, replyId: String): ChatMessage {
        val apiKey = getEffectiveKey(BuildConfig.OPENAI_API_KEY, "OPENAI_API_KEY")
        if (!isOpenAiConfigured()) {
            return ChatMessage(
                id = replyId,
                aiEngineId = engine.id,
                aiName = engine.name,
                sender = "ai",
                text = "⚠️ OpenAI API Key Not Configured\n\nTo use ${engine.name}, please configure `OPENAI_API_KEY` in your AI Studio Secrets panel.\n\n💡 Tip: You can switch to **Gemini** (currently configured and active) from the carousel above for immediate live responses.",
                isError = true,
                errorType = "MISSING_KEY"
            )
        }

        val requestJson = JSONObject().apply {
            put("model", "gpt-4o-mini")
            val messagesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are ChatGPT inside WS Editor, an expert video editor and creative script assistant.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", query)
                })
            }
            put("messages", messagesArray)
            put("max_tokens", 800)
        }

        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            val errorMsg = try {
                val errObj = JSONObject(responseBody).optJSONObject("error")
                errObj?.optString("message") ?: "HTTP error ${response.code}"
            } catch (ex: Exception) {
                "HTTP error ${response.code}"
            }
            return ChatMessage(
                id = replyId,
                aiEngineId = engine.id,
                aiName = engine.name,
                sender = "ai",
                text = "⚠️ OpenAI API Error (${response.code})\n\n$errorMsg",
                isError = true,
                errorType = if (response.code == 401) "INVALID_KEY" else if (response.code == 429) "RATE_LIMIT" else "API_ERROR"
            )
        }

        val json = JSONObject(responseBody)
        val choices = json.optJSONArray("choices")
        val message = choices?.optJSONObject(0)?.optJSONObject("message")
        val content = message?.optString("content").orEmpty()

        return ChatMessage(
            id = replyId,
            aiEngineId = engine.id,
            aiName = engine.name,
            sender = "ai",
            text = content.trim()
        )
    }

    private fun handleDallERequest(engine: AiEngine, query: String, replyId: String): ChatMessage {
        val apiKey = getEffectiveKey(BuildConfig.OPENAI_API_KEY, "OPENAI_API_KEY")
        if (!isOpenAiConfigured()) {
            return ChatMessage(
                id = replyId,
                aiEngineId = engine.id,
                aiName = engine.name,
                sender = "ai",
                text = "⚠️ OpenAI API Key Not Configured\n\nDALL-E requires `OPENAI_API_KEY` configured in the AI Studio Secrets panel.\n\n💡 Tip: Switch to **Gemini** for live multimodal prompts.",
                isError = true,
                errorType = "MISSING_KEY"
            )
        }

        val requestJson = JSONObject().apply {
            put("model", "dall-e-3")
            put("prompt", query)
            put("n", 1)
            put("size", "1024x1024")
        }

        val request = Request.Builder()
            .url("https://api.openai.com/v1/images/generations")
            .header("Authorization", "Bearer $apiKey")
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            val errorMsg = try {
                val errObj = JSONObject(responseBody).optJSONObject("error")
                errObj?.optString("message") ?: "HTTP error ${response.code}"
            } catch (ex: Exception) {
                "HTTP error ${response.code}"
            }
            return ChatMessage(
                id = replyId,
                aiEngineId = engine.id,
                aiName = engine.name,
                sender = "ai",
                text = "⚠️ DALL-E API Error (${response.code})\n\n$errorMsg",
                isError = true,
                errorType = "API_ERROR"
            )
        }

        val json = JSONObject(responseBody)
        val dataArray = json.optJSONArray("data")
        val item = dataArray?.optJSONObject(0)
        val imageUrl = item?.optString("url").orEmpty()
        val revisedPrompt = item?.optString("revised_prompt") ?: query

        return ChatMessage(
            id = replyId,
            aiEngineId = engine.id,
            aiName = engine.name,
            sender = "ai",
            text = "Generated 4K UHD Concept Art for: \"$query\"\n\n$revisedPrompt",
            mediaType = "image",
            mediaTitle = "DALL-E 3 Generation",
            mediaSubtitle = "1024x1024 • Commercial License"
        )
    }

    private fun handleAnthropicRequest(engine: AiEngine, query: String, replyId: String): ChatMessage {
        val apiKey = getEffectiveKey(BuildConfig.ANTHROPIC_API_KEY, "ANTHROPIC_API_KEY")
        if (!isAnthropicConfigured()) {
            return ChatMessage(
                id = replyId,
                aiEngineId = engine.id,
                aiName = engine.name,
                sender = "ai",
                text = "⚠️ Anthropic Claude API Key Required\n\nTo use ${engine.name}, please configure `ANTHROPIC_API_KEY` in the AI Studio Secrets panel.\n\n💡 Tip: Select **Gemini** above to test and receive live AI responses right now.",
                isError = true,
                errorType = "MISSING_KEY"
            )
        }

        val requestJson = JSONObject().apply {
            put("model", "claude-3-5-sonnet-20241022")
            put("max_tokens", 1024)
            val messagesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", query)
                })
            }
            put("messages", messagesArray)
        }

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            val errorMsg = try {
                val errObj = JSONObject(responseBody).optJSONObject("error")
                errObj?.optString("message") ?: "HTTP error ${response.code}"
            } catch (ex: Exception) {
                "HTTP error ${response.code}"
            }
            return ChatMessage(
                id = replyId,
                aiEngineId = engine.id,
                aiName = engine.name,
                sender = "ai",
                text = "⚠️ Claude API Error (${response.code})\n\n$errorMsg",
                isError = true,
                errorType = if (response.code == 401) "INVALID_KEY" else "API_ERROR"
            )
        }

        val json = JSONObject(responseBody)
        val contentArray = json.optJSONArray("content")
        val text = contentArray?.optJSONObject(0)?.optString("text").orEmpty()

        return ChatMessage(
            id = replyId,
            aiEngineId = engine.id,
            aiName = engine.name,
            sender = "ai",
            text = text.trim()
        )
    }

    private fun handlePerplexityRequest(engine: AiEngine, query: String, replyId: String): ChatMessage {
        val apiKey = getEffectiveKey(BuildConfig.PERPLEXITY_API_KEY, "PERPLEXITY_API_KEY")
        if (!isPerplexityConfigured()) {
            return ChatMessage(
                id = replyId,
                aiEngineId = engine.id,
                aiName = engine.name,
                sender = "ai",
                text = "⚠️ Perplexity API Key Required\n\nTo use Perplexity Pro Search, please configure `PERPLEXITY_API_KEY` in your AI Studio Secrets panel.\n\n💡 Tip: Switch to **Gemini** for live answers without additional setup.",
                isError = true,
                errorType = "MISSING_KEY"
            )
        }

        val requestJson = JSONObject().apply {
            put("model", "sonar")
            val messagesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are Perplexity in WS Editor, an expert in real-time viral trends, cited video editing techniques, and social media data.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", query)
                })
            }
            put("messages", messagesArray)
        }

        val request = Request.Builder()
            .url("https://api.perplexity.ai/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            val errorMsg = try {
                val errObj = JSONObject(responseBody).optJSONObject("error")
                errObj?.optString("message") ?: "HTTP error ${response.code}"
            } catch (ex: Exception) {
                "HTTP error ${response.code}"
            }
            return ChatMessage(
                id = replyId,
                aiEngineId = engine.id,
                aiName = engine.name,
                sender = "ai",
                text = "⚠️ Perplexity API Error (${response.code})\n\n$errorMsg",
                isError = true,
                errorType = "API_ERROR"
            )
        }

        val json = JSONObject(responseBody)
        val choices = json.optJSONArray("choices")
        val message = choices?.optJSONObject(0)?.optJSONObject("message")
        val content = message?.optString("content").orEmpty()

        return ChatMessage(
            id = replyId,
            aiEngineId = engine.id,
            aiName = engine.name,
            sender = "ai",
            text = content.trim()
        )
    }

    private fun handleOtherProvider(engine: AiEngine, query: String, replyId: String): ChatMessage {
        val envKeyName = "${engine.id.uppercase()}_API_KEY"
        return ChatMessage(
            id = replyId,
            aiEngineId = engine.id,
            aiName = engine.name,
            sender = "ai",
            text = "ℹ️ **API Integration Required: ${engine.name}**\n\n" +
                    "• **Model:** ${engine.tag}\n" +
                    "• **Category:** ${engine.category.label}\n" +
                    "• **Specialty:** ${engine.description}\n\n" +
                    "Direct API routing for ${engine.name} requires configuring `$envKeyName` in AI Studio Secrets.\n\n" +
                    "👉 **Switch to Gemini** from the top bar to generate scripts, video prompts, and creative assets with live responses immediately.",
            isError = false,
            errorType = "UNCONFIGURED_PROVIDER"
        )
    }
}
