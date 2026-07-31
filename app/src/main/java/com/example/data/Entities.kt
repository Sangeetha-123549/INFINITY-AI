package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String, // "user", "ai", "system"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isEscalation: Boolean = false,
    val handoffNote: String? = null
)

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey val orderNumber: String, // e.g. "#12345"
    val status: String, // "In Transit", "Delivered", "Processing", "Delayed"
    val placedDate: String, // "Oct 20, 2023"
    val estimatedDelivery: String, // "Oct 24, 2023"
    val latestUpdate: String, // "Departed Facility - Oct 22, 2023 at 10:45 AM - Chicago Logistics Hub"
    val recipientName: String, // "Alex Johnson"
    val streetAddress: String, // "123 Future Lane, Tech District"
    val cityStateZip: String, // "San Francisco, CA 94105"
    val country: String, // "United States"
    val itemsSummary: String, // "Infinity Core V2 x1, Neural Interface Kit x2, Cloud Sync Dongle x1"
    val currentStep: Int // 0=Ordered, 1=Shipped, 2=In Transit, 3=Delivered
)

@Entity(tableName = "faqs")
data class FaqItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String, // "Returns & Refunds", "Shipping Info", "Account Security", "Privacy Policy"
    val question: String,
    val answer: String
)
