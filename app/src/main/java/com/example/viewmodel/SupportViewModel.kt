package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ChatMessage
import com.example.data.FaqItem
import com.example.data.Order
import com.example.data.SupportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SupportViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SupportRepository

    val chatMessages: StateFlow<List<ChatMessage>>
    val orders: StateFlow<List<Order>>
    private val allFaqs: StateFlow<List<FaqItem>>

    private val _selectedOrder = MutableStateFlow<Order?>(null)
    val selectedOrder: StateFlow<Order?> = _selectedOrder.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    val filteredFaqs: StateFlow<List<FaqItem>>

    init {
        val dao = AppDatabase.getDatabase(application).supportDao()
        repository = SupportRepository(dao)

        chatMessages = repository.chatMessages.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        orders = repository.orders.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allFaqs = repository.faqs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        filteredFaqs = combine(allFaqs, _searchQuery, _selectedCategory) { faqs, query, category ->
            faqs.filter { item ->
                val matchesCategory = category == null || item.category.equals(category, ignoreCase = true)
                val matchesQuery = query.isBlank() ||
                        item.question.contains(query, ignoreCase = true) ||
                        item.answer.contains(query, ignoreCase = true) ||
                        item.category.contains(query, ignoreCase = true)
                matchesCategory && matchesQuery
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Pre-select first order (#12345) by default for order view
        viewModelScope.launch {
            val list = repository.orders.firstOrNull()
            if (!list.isNullOrEmpty()) {
                _selectedOrder.value = list.first()
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _isGenerating.value = true
            try {
                repository.processUserMessage(text.trim())
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun selectOrder(orderNumber: String) {
        viewModelScope.launch {
            val matched = repository.getOrderByNumber(orderNumber)
            if (matched != null) {
                _selectedOrder.value = matched
            }
        }
    }

    fun trackOrderSearch(queryNumber: String) {
        if (queryNumber.isBlank()) return
        val formatted = if (queryNumber.startsWith("#")) queryNumber else "#$queryNumber"
        viewModelScope.launch {
            val matched = repository.getOrderByNumber(formatted)
            if (matched != null) {
                _selectedOrder.value = matched
            } else {
                // If not found in DB, try selecting first or creating temporary search
                val currentOrders = orders.value
                val found = currentOrders.firstOrNull { it.orderNumber.contains(queryNumber, ignoreCase = true) }
                if (found != null) {
                    _selectedOrder.value = found
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChat()
        }
    }

    fun escalateToHuman() {
        sendMessage("Talk to a human agent")
    }
}
