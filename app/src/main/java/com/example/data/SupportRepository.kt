package com.example.data

import com.example.network.Content
import com.example.network.GeminiApiClient
import com.example.network.GenerateContentRequest
import com.example.network.Part
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class SupportRepository(private val dao: SupportDao) {

    val chatMessages: Flow<List<ChatMessage>> = dao.getAllMessages()
    val orders: Flow<List<Order>> = dao.getAllOrders()
    val faqs: Flow<List<FaqItem>> = dao.getAllFaqs()

    suspend fun getOrderByNumber(orderNumber: String): Order? = dao.getOrderByNumber(orderNumber)

    suspend fun addMessage(message: ChatMessage): Long {
        return dao.insertMessage(message)
    }

    suspend fun clearChat() {
        dao.clearMessages()
        // Re-insert initial greeting
        dao.insertMessage(
            ChatMessage(
                sender = "ai",
                text = "Hello! I'm InfinityAI, your support assistant. How can I help you today?"
            )
        )
    }

    suspend fun processUserMessage(userText: String): ChatMessage = withContext(Dispatchers.IO) {
        // 1. Save user message to database
        val userMsg = ChatMessage(sender = "user", text = userText)
        dao.insertMessage(userMsg)

        // Fetch recent order list to provide as context
        val orderList = dao.getAllOrders().first()
        val ordersContext = orderList.joinToString("\n") { o ->
            "- Order ${o.orderNumber}: Status '${o.status}', Placed '${o.placedDate}', Est. Delivery '${o.estimatedDelivery}', Items: '${o.itemsSummary}', Latest Update: '${o.latestUpdate}', Address: '${o.recipientName}, ${o.streetAddress}, ${o.cityStateZip}'"
        }

        // Fetch conversation history
        val history = dao.getAllMessages().first().takeLast(10)

        // Check if explicit escalation request or billing/fraud query
        val lowerText = userText.lowercase()
        val isExplicitEscalation = lowerText.contains("talk to a human") ||
                lowerText.contains("speak to human") ||
                lowerText.contains("live agent") ||
                lowerText.contains("human agent") ||
                lowerText.contains("representative") ||
                lowerText.contains("fraud") ||
                lowerText.contains("unauthorized charge")

        val apiKey = GeminiApiClient.getApiKey()

        var aiText = ""
        var isEscalated = false
        var handoffNote: String? = null

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                // Build contents list
                val systemPromptWithData = GeminiApiClient.SYSTEM_PROMPT +
                        "\n\nAVAILABLE SYSTEM ORDER DATA:\n$ordersContext"

                val contentsList = history.map { msg ->
                    Content(
                        role = if (msg.sender == "user") "user" else "model",
                        parts = listOf(Part(text = msg.text))
                    )
                }

                val request = GenerateContentRequest(
                    contents = contentsList,
                    systemInstruction = Content(parts = listOf(Part(text = systemPromptWithData)))
                )

                val response = GeminiApiClient.service.generateContent(apiKey, request)
                val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

                if (!rawText.isNullOrBlank()) {
                    if (rawText.contains("[INTERNAL_HANDOFF_NOTE:")) {
                        isEscalated = true
                        val parts = rawText.split("[INTERNAL_HANDOFF_NOTE:")
                        aiText = parts[0].trim()
                        handoffNote = parts.getOrNull(1)?.replace("]", "")?.trim()
                    } else {
                        aiText = rawText.trim()
                    }
                }
            } catch (e: Exception) {
                // Network error or fallback
                aiText = ""
            }
        }

        // Fallback local AI responder if API response was empty or API key missing
        if (aiText.isBlank()) {
            val localResult = generateLocalResponse(userText, orderList, isExplicitEscalation)
            aiText = localResult.first
            isEscalated = localResult.second
            handoffNote = localResult.third
        }

        // Save AI response to DB
        val aiMessage = ChatMessage(
            sender = "ai",
            text = aiText,
            isEscalation = isEscalated,
            handoffNote = handoffNote
        )
        dao.insertMessage(aiMessage)
        return@withContext aiMessage
    }

    private fun generateLocalResponse(
        input: String,
        orders: List<Order>,
        isExplicitEscalation: Boolean
    ): Triple<String, Boolean, String?> {
        val lower = input.lowercase()

        if (isExplicitEscalation || lower.contains("human") || lower.contains("agent") || lower.contains("support representative")) {
            val userOrder = orders.firstOrNull { lower.contains(it.orderNumber.lowercase().removePrefix("#")) } ?: orders.firstOrNull()
            return Triple(
                "I understand you'd like to speak with a human agent. I am transferring your ticket to a Live Specialist who will assist you shortly.",
                true,
                "[INTERNAL_HANDOFF_NOTE: User requested live agent. Related Order: ${userOrder?.orderNumber ?: "N/A"}. Priority: High. Status: Pending Specialist Assignee]"
            )
        }

        // Order tracking query
        if (lower.contains("track") || lower.contains("order") || lower.contains("status") || lower.contains("#")) {
            val matchedOrder = orders.firstOrNull {
                lower.contains(it.orderNumber.lowercase()) || lower.contains(it.orderNumber.lowercase().removePrefix("#"))
            } ?: orders.firstOrNull()

            if (matchedOrder != null) {
                return Triple(
                    "Here are the latest details for **Order ${matchedOrder.orderNumber}**:\n\n" +
                            "• **Status:** ${matchedOrder.status}\n" +
                            "• **Placed Date:** ${matchedOrder.placedDate}\n" +
                            "• **Estimated Delivery:** ${matchedOrder.estimatedDelivery}\n" +
                            "• **Latest Update:** ${matchedOrder.latestUpdate}\n" +
                            "• **Destination:** ${matchedOrder.recipientName}, ${matchedOrder.streetAddress}\n\n" +
                            "You can view complete shipment tracking in the **Orders** tab.",
                    false,
                    null
                )
            }
        }

        // Return policy query
        if (lower.contains("return") || lower.contains("refund")) {
            return Triple(
                "**InfinityAI Return & Refund Policy:**\n\n" +
                        "• **Return Window:** 30 days from date of delivery.\n" +
                        "• **Shipping Label:** Pre-paid shipping label provided via email once initiated.\n" +
                        "• **Refund Processing:** Inspection takes 3-5 business days; refunds post back to original payment in 5-10 business days.\n\n" +
                        "You can tap 'Return policy' or search in our **FAQs** screen for more details.",
                false,
                null
            )
        }

        // Billing / Unauthorized charge query -> triggers escalation
        if (lower.contains("charge") || lower.contains("billing") || lower.contains("credit card") || lower.contains("stolen") || lower.contains("fraud")) {
            return Triple(
                "Security & billing issues are prioritized for human verification. I am immediately connecting you with an Account Billing Specialist.",
                true,
                "[INTERNAL_HANDOFF_NOTE: Billing inquiry/unauthorized charge report. Flagged for fraud review. Priority: Urgent]"
            )
        }

        // General greeting or fallback
        return Triple(
            "I'm here to assist you with order tracking, return policies, shipping updates, or account issues. Please let me know how I can help, or select one of the quick options below!",
            false,
            null
        )
    }
}
