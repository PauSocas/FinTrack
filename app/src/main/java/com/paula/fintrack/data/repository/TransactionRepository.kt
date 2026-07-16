package com.paula.fintrack.data.repository

import com.paula.fintrack.data.local.AppDatabase
import com.paula.fintrack.data.local.Transaction
import kotlinx.coroutines.flow.Flow

class TransactionRepository(database: AppDatabase) {

    private val dao = database.transactionDao()

    val allTransactions: Flow<List<Transaction>> = dao.getAll()

    suspend fun insert(transaction: Transaction) = dao.insert(transaction)

    suspend fun delete(transaction: Transaction) = dao.delete(transaction)

    suspend fun update(transaction: Transaction) = dao.update(transaction)
}
