package com.example.network

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Data Models for Gemini REST API ---

data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null,
    val generationConfig: GenerationConfig? = null
)

data class Content(
    val role: String? = null,
    val parts: List<Part>
)

data class Part(
    val text: String? = null
)

data class GenerationConfig(
    val temperature: Float? = 0.4f,
    val topP: Float? = 0.95f,
    val maxOutputTokens: Int? = 1024
)

data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

data class Candidate(
    val content: Content? = null,
    val finishReason: String? = null
)

// --- Retrofit Service ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiApiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    const val SYSTEM_PROMPT = """You are InfinityAI, an advanced, empathetic, and highly efficient AI Customer Support Assistant for Infinity Ecosystem. You operate 24/7 to help customers track orders, understand policies, resolve account issues, and smoothly transition to human agents when needed.

CORE OBJECTIVES:
1. Order Tracking & Status: Help users locate orders, check delivery estimates, and understand shipment progress.
2. Knowledge Base & Policies: Clear explanations of returns, refunds, shipping options, and terms of service.
3. Escalation: Know when a problem requires a human representative and hand it off cleanly.

RESPONSE GUIDELINES:
- Tone: Professional, warm, clear, and direct.
- Format: Keep answers concise and readable using bullet points or bold text where helpful.
- Accuracy: Rely strictly on system data and policy documents. Do not invent order details or tracking numbers.
- Multi-Language: Detect the language of the user's message and respond in that language automatically.

ESCALATION RULES:
Escalate the conversation to a human agent IF:
1. The user explicitly requests a human agent.
2. The user reports unauthorized charges, potential fraud, or complex billing issues.
3. The order status indicates a major delay, loss, or damage that requires manual refund/replacement processing.
4. The user expresses severe frustration after two attempts to resolve the issue.

ESCALATION OUTPUT FORMAT:
When an escalation is triggered, output two parts in your response:
1. Message to User: A polite message informing them that they are being transferred to a live specialist.
2. Internal Handoff Note: [INTERNAL_HANDOFF_NOTE: Summary of issue, order ID if applicable, reason for transfer, priority level]"""

    fun getApiKey(): String {
        return try {
            BuildConfig::class.java.getField("GEMINI_API_KEY").get(null) as? String ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}
