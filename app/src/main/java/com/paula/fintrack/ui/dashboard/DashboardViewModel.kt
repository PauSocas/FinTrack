package com.paula.fintrack.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import com.paula.fintrack.data.local.AppDatabase
import com.paula.fintrack.data.local.Transaction
import com.paula.fintrack.data.repository.TransactionRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DashboardData(
    val balance: Double,
    val totalIngresos: Double,
    val totalGastos: Double,
    val gastosByCategory: Map<String, Double>,
    val transactionsByCategory: Map<String, List<Transaction>>,
    val monthName: String,
    val spendingRate: Int,
    val recentTransactions: List<Transaction>
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TransactionRepository(AppDatabase.getDatabase(application))

    val dashboardData: LiveData<DashboardData> =
        repository.allTransactions.asLiveData().map { transactions ->
            val cal = Calendar.getInstance()
            val currentMonth = cal.get(Calendar.MONTH)
            val currentYear = cal.get(Calendar.YEAR)

            val monthTransactions = transactions.filter {
                val txCal = Calendar.getInstance()
                txCal.timeInMillis = it.date
                txCal.get(Calendar.MONTH) == currentMonth &&
                        txCal.get(Calendar.YEAR) == currentYear
            }

            val totalIngresos = monthTransactions
                .filter { it.type == "INGRESO" }
                .sumOf { it.amount }

            val transactionsByCategory = monthTransactions
                .filter { it.type == "GASTO" }
                .groupBy { it.category }

            val gastosByCategory = transactionsByCategory
                .mapValues { (_, list) -> list.sumOf { it.amount } }

            val totalGastos = gastosByCategory.values.sum()

            val spendingRate = if (totalIngresos > 0)
                ((totalGastos / totalIngresos) * 100).toInt().coerceIn(0, 100)
            else 0

            val monthFormat = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
            val monthName = monthFormat.format(Date()).replaceFirstChar { it.uppercase() }

            DashboardData(
                balance = totalIngresos - totalGastos,
                totalIngresos = totalIngresos,
                totalGastos = totalGastos,
                gastosByCategory = gastosByCategory,
                transactionsByCategory = transactionsByCategory,
                monthName = monthName,
                spendingRate = spendingRate,
                recentTransactions = monthTransactions.sortedByDescending { it.date }.take(3)
            )
        }
}
