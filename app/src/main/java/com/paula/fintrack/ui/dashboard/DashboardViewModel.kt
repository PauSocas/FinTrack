package com.paula.fintrack.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import com.paula.fintrack.data.local.AppDatabase
import com.paula.fintrack.data.local.Transaction
import com.paula.fintrack.data.repository.TransactionRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class DashboardData(
    val balance: Double,
    val totalIngresos: Double,
    val totalGastos: Double,
    val gastosByCategory: Map<String, Double>,
    val transactionsByCategory: Map<String, List<Transaction>>,
    val monthName: String,
    val spendingRate: Int,
    val recentTransactions: List<Transaction>,
    val isCurrentMonth: Boolean
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TransactionRepository(AppDatabase.getDatabase(application))

    private val _selectedCal = MutableLiveData(Calendar.getInstance())

    fun previousMonth() {
        val cal = _selectedCal.value!!.clone() as Calendar
        cal.add(Calendar.MONTH, -1)
        _selectedCal.value = cal
    }

    fun nextMonth() {
        val now = Calendar.getInstance()
        val cal = _selectedCal.value!!.clone() as Calendar
        if (cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            cal.get(Calendar.MONTH) == now.get(Calendar.MONTH)) return
        cal.add(Calendar.MONTH, 1)
        _selectedCal.value = cal
    }

    val dashboardData: LiveData<DashboardData> = MediatorLiveData<DashboardData>().also { mediator ->
        val allTransactions = repository.allTransactions.asLiveData()

        fun recalculate() {
            val transactions = allTransactions.value ?: return
            val cal = _selectedCal.value ?: return
            mediator.value = buildDashboardData(transactions, cal)
        }

        mediator.addSource(allTransactions) { recalculate() }
        mediator.addSource(_selectedCal) { recalculate() }
    }

    private fun buildDashboardData(transactions: List<Transaction>, cal: Calendar): DashboardData {
        val selectedMonth = cal.get(Calendar.MONTH)
        val selectedYear = cal.get(Calendar.YEAR)

        val monthTransactions = transactions.filter {
            val txCal = Calendar.getInstance()
            txCal.timeInMillis = it.date
            txCal.get(Calendar.MONTH) == selectedMonth &&
                    txCal.get(Calendar.YEAR) == selectedYear
        }

        val totalIngresos = monthTransactions.filter { it.type == "INGRESO" }.sumOf { it.amount }
        val transactionsByCategory = monthTransactions.filter { it.type == "GASTO" }.groupBy { it.category }
        val gastosByCategory = transactionsByCategory.mapValues { (_, list) -> list.sumOf { it.amount } }
        val totalGastos = gastosByCategory.values.sum()

        val spendingRate = if (totalIngresos > 0)
            ((totalGastos / totalIngresos) * 100).toInt().coerceIn(0, 100)
        else 0

        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
        val monthName = monthFormat.format(cal.time).replaceFirstChar { it.uppercase() }

        val now = Calendar.getInstance()
        val isCurrentMonth = selectedMonth == now.get(Calendar.MONTH) &&
                selectedYear == now.get(Calendar.YEAR)

        return DashboardData(
            balance = totalIngresos - totalGastos,
            totalIngresos = totalIngresos,
            totalGastos = totalGastos,
            gastosByCategory = gastosByCategory,
            transactionsByCategory = transactionsByCategory,
            monthName = monthName,
            spendingRate = spendingRate,
            recentTransactions = monthTransactions.sortedByDescending { it.date }.take(3),
            isCurrentMonth = isCurrentMonth
        )
    }
}
