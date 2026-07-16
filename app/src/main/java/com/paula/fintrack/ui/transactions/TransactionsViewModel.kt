package com.paula.fintrack.ui.transactions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.paula.fintrack.data.local.AppDatabase
import com.paula.fintrack.data.local.Transaction
import com.paula.fintrack.data.repository.TransactionRepository
import kotlinx.coroutines.launch

class TransactionsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository

    val allTransactions: LiveData<List<Transaction>>

    init {
        val db = AppDatabase.getDatabase(application)
        repository = TransactionRepository(db)
        allTransactions = repository.allTransactions.asLiveData()
    }

    fun insert(transaction: Transaction) {
        viewModelScope.launch {
            repository.insert(transaction)
        }
    }

    fun delete(transaction: Transaction) {
        viewModelScope.launch {
            repository.delete(transaction)
        }
    }

    fun update(transaction: Transaction) {
        viewModelScope.launch {
            repository.update(transaction)
        }
    }
}
