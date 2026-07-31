package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [ChatMessage::class, Order::class, FaqItem::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun supportDao(): SupportDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "infinity_ai_db"
                )
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        seedDatabase(database.supportDao())
                    }
                }
            }

            private suspend fun seedDatabase(dao: SupportDao) {
                // Initial Welcome Message
                dao.insertMessage(
                    ChatMessage(
                        sender = "ai",
                        text = "Hello! I'm InfinityAI, your support assistant. How can I help you today?"
                    )
                )

                // Seed Orders matching design mockups
                dao.insertOrders(
                    listOf(
                        Order(
                            orderNumber = "#12345",
                            status = "In Transit",
                            placedDate = "Oct 20, 2023",
                            estimatedDelivery = "Oct 24, 2023",
                            latestUpdate = "Latest Update: Departed Facility - Oct 22, 2023 at 10:45 AM - Chicago Logistics Hub",
                            recipientName = "Alex Johnson",
                            streetAddress = "123 Future Lane, Tech District",
                            cityStateZip = "San Francisco, CA 94105",
                            country = "United States",
                            itemsSummary = "Infinity Core V2 (x1), Neural Interface Kit (x2), Cloud Sync Dongle (x1)",
                            currentStep = 2
                        ),
                        Order(
                            orderNumber = "#12340",
                            status = "Delivered",
                            placedDate = "Oct 15, 2023",
                            estimatedDelivery = "Oct 18, 2023",
                            latestUpdate = "Delivered: Left at front porch - Oct 18, 2023 at 2:15 PM",
                            recipientName = "Alex Johnson",
                            streetAddress = "123 Future Lane, Tech District",
                            cityStateZip = "San Francisco, CA 94105",
                            country = "United States",
                            itemsSummary = "Quantum Processor Module (x1)",
                            currentStep = 3
                        ),
                        Order(
                            orderNumber = "#12338",
                            status = "Delivered",
                            placedDate = "Oct 12, 2023",
                            estimatedDelivery = "Oct 15, 2023",
                            latestUpdate = "Delivered: Signed by Alex Johnson - Oct 15, 2023 at 11:30 AM",
                            recipientName = "Alex Johnson",
                            streetAddress = "123 Future Lane, Tech District",
                            cityStateZip = "San Francisco, CA 94105",
                            country = "United States",
                            itemsSummary = "Infinity Smart Sensor Array (x3)",
                            currentStep = 3
                        )
                    )
                )

                // Seed FAQs
                dao.insertFaqs(
                    listOf(
                        FaqItem(
                            category = "Returns & Refunds",
                            question = "How do I return an item?",
                            answer = "To initiate a return, log into your InfinityAI account, go to 'Orders', select the item you wish to return, and click 'Initiate Return'. You'll receive a pre-paid shipping label via email within 24 hours. Items must be returned in their original packaging within 30 days of receipt."
                        ),
                        FaqItem(
                            category = "Returns & Refunds",
                            question = "Where is my refund?",
                            answer = "Once we receive your returned item at our warehouse, it takes approximately 3-5 business days for our team to inspect the product. After approval, the refund will be processed back to your original payment method. Depending on your bank, it may take an additional 5-10 business days for the funds to appear in your account."
                        ),
                        FaqItem(
                            category = "Shipping Info",
                            question = "Can I change my shipping address after an order is placed?",
                            answer = "Shipping addresses can be modified within the first 6 hours of placing an order. Navigate to your Order Dashboard and select 'Edit Shipping Address'. If the order has already entered the fulfillment process, we are unable to change the destination, but you may be able to redirect it via the carrier's website once a tracking number is issued."
                        ),
                        FaqItem(
                            category = "Account Security",
                            question = "Is my data secure with InfinityAI?",
                            answer = "Absolutely. We employ enterprise-grade AES-256 encryption for all data at rest and TLS 1.3 for data in transit. We perform regular third-party security audits and maintain SOC2 compliance to ensure your information remains private and secure."
                        ),
                        FaqItem(
                            category = "Shipping Info",
                            question = "What happens if my package is lost or damaged?",
                            answer = "If your tracking status shows no updates for over 5 days or if your item arrives damaged, please let our AI Assistant know or tap 'Talk to a human' to be transferred directly to a live claims specialist for an instant replacement."
                        ),
                        FaqItem(
                            category = "Privacy Policy",
                            question = "How does InfinityAI use my conversational data?",
                            answer = "Your support chat data is strictly used to resolve your immediate support queries and improve our customer service quality. We never sell your personal or billing information to third parties."
                        )
                    )
                )
            }
        }
    }
}
