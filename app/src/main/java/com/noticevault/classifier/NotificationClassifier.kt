package com.noticevault.classifier

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class NotificationClassifier(private val context: Context) {

    private val client = OkHttpClient()

    companion object {
        private val PROMOTIONAL = listOf("sale", "offer", "discount", "% off", "deal", "promo",
            "buy now", "limited time", "exclusive", "free shipping", "coupon", "save", "shop now")
        private val SOCIAL = listOf("liked", "commented", "followed", "mentioned", "tagged",
            "message", "replied", "reacted", "shared your", "sent you", "accepted your")
        private val TRANSACTIONAL = listOf("otp", "password", "transaction", "payment", "order",
            "delivered", "shipped", "receipt", "invoice", "bank", "credit", "debit", "amount",
            "balance", "statement", "verification code")
        private val URGENT = listOf("urgent", "alert", "warning", "critical", "failed", "error",
            "missed call", "emergency", "due", "expir", "overdue", "action required")
        private val NEWS = listOf("breaking", "headline", "news", "update", "report", "story")
    }

    suspend fun classify(appName: String, title: String, content: String): String {
        val combined = "$appName $title $content".lowercase()
        val (localCategory, confidence) = localClassify(combined)
        if (confidence >= 0.65f) return localCategory
        return remoteClassify(appName, title, content) ?: localCategory
    }

    private fun localClassify(text: String): Pair<String, Float> {
        val scores = mapOf(
            "promotional" to scoreKeywords(text, PROMOTIONAL),
            "social"      to scoreKeywords(text, SOCIAL),
            "transactional" to scoreKeywords(text, TRANSACTIONAL),
            "urgent"      to scoreKeywords(text, URGENT),
            "news"        to scoreKeywords(text, NEWS)
        )
        val best = scores.maxByOrNull { it.value }
        return if (best != null && best.value > 0)
            Pair(best.key, minOf(best.value * 0.25f, 1f))
        else
            Pair("other", 0.1f)
    }

    private fun scoreKeywords(text: String, keywords: List<String>): Float =
        keywords.count { text.contains(it) }.toFloat()

    private suspend fun remoteClassify(appName: String, title: String, content: String): String? {
        val prefs = context.getSharedPreferences("noticevault_prefs", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("claude_api_key", "") ?: ""
        if (apiKey.isBlank()) return null

        return withContext(Dispatchers.IO) {
            try {
                val prompt = """Classify this Android notification into exactly one word.
App: $appName
Title: $title
Content: $content

Reply with ONLY one word from: promotional, social, transactional, urgent, news, system, other"""

                val body = JSONObject().apply {
                    put("model", "claude-sonnet-4-20250514")
                    put("max_tokens", 20)
                    put("messages", JSONArray().put(
                        JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        }
                    ))
                }.toString()

                val request = Request.Builder()
                    .url("https://api.anthropic.com/v1/messages")
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", "2023-06-01")
                    .addHeader("content-type", "application/json")
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val json = JSONObject(response.body?.string() ?: return@withContext null)
                val text = json.getJSONArray("content")
                    .getJSONObject(0).getString("text").trim().lowercase()

                val valid = setOf("promotional", "social", "transactional", "urgent", "news", "system", "other")
                if (text in valid) text else "other"
            } catch (e: Exception) {
                Log.e("Classifier", "API call failed: ${e.message}")
                null
            }
        }
    }
}
