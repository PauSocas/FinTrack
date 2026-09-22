package com.paula.fintrack.ui.dashboard

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.paula.fintrack.R
import com.paula.fintrack.data.local.Transaction
import com.paula.fintrack.databinding.FragmentDashboardBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()
    private var latestData: DashboardData? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPieChart()

        viewModel.dashboardData.observe(viewLifecycleOwner) { data ->
            latestData = data

            val balanceSign = if (data.balance >= 0) "+" else ""
            binding.tvBalance.text = "$balanceSign%.2f €".format(data.balance)
            binding.tvMonthName.text = data.monthName
            binding.tvIngresos.text = "+%.2f €".format(data.totalIngresos)
            binding.tvGastos.text = "-%.2f €".format(data.totalGastos)

            binding.progressSpending.setProgressCompat(data.spendingRate, true)
            binding.tvSpendingRate.text = "${data.spendingRate}%"

            val barColor = when {
                data.spendingRate <= 70 -> requireContext().getColor(R.color.progress_ok)
                data.spendingRate <= 90 -> requireContext().getColor(R.color.progress_warn)
                else -> requireContext().getColor(R.color.progress_danger)
            }
            binding.progressSpending.setIndicatorColor(barColor)

            if (data.totalIngresos > 0) {
                binding.tvSpendingDesc.text =
                    "%.2f € gastados de %.2f €".format(data.totalGastos, data.totalIngresos)
            } else {
                binding.tvSpendingDesc.text = "Sin ingresos registrados este mes"
            }

            if (data.gastosByCategory.isEmpty()) {
                binding.pieChart.visibility = View.GONE
                binding.tvNoData.visibility = View.VISIBLE
            } else {
                binding.pieChart.visibility = View.VISIBLE
                binding.tvNoData.visibility = View.GONE
                updatePieChart(data.gastosByCategory)
            }

            updateRecentTransactions(data.recentTransactions)
        }
    }

    private fun setupPieChart() {
        binding.pieChart.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 40f
            setHoleColor(Color.TRANSPARENT)
            legend.isEnabled = true
            setEntryLabelColor(Color.WHITE)
            setEntryLabelTextSize(11f)
            animateY(800)

            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    val category = (e as? PieEntry)?.label ?: return
                    val transactions = latestData?.transactionsByCategory?.get(category) ?: return
                    showCategoryDetail(category, transactions)
                }
                override fun onNothingSelected() {}
            })
        }
    }

    private fun showCategoryDetail(category: String, transactions: List<Transaction>) {
        val bottomSheet = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_category_detail, null)

        val total = transactions.sumOf { it.amount }
        val count = transactions.size

        sheetView.findViewById<TextView>(R.id.tvCategoryEmoji).text = categoryEmoji(category)
        sheetView.findViewById<TextView>(R.id.tvCategoryName).text = category
        sheetView.findViewById<TextView>(R.id.tvCategoryTotal).text =
            "Total: %.2f €".format(total)
        sheetView.findViewById<TextView>(R.id.tvTransactionCount).text =
            "$count ${if (count == 1) "gasto" else "gastos"}"

        val container = sheetView.findViewById<LinearLayout>(R.id.containerTransactions)
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("es", "ES"))

        transactions.sortedByDescending { it.date }.forEachIndexed { index, tx ->
            if (index > 0) {
                val divider = View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1
                    ).apply {
                        leftMargin = dpToPx(20)
                        rightMargin = dpToPx(20)
                    }
                    setBackgroundColor(requireContext().getColor(R.color.outline_variant))
                }
                container.addView(divider)
            }

            val rowView = layoutInflater.inflate(R.layout.item_category_row, container, false)
            rowView.findViewById<TextView>(R.id.tvRowDesc).text = tx.description
            rowView.findViewById<TextView>(R.id.tvRowDate).text =
                dateFormat.format(Date(tx.date))
            rowView.findViewById<TextView>(R.id.tvRowAmount).text =
                "-%.2f €".format(tx.amount)
            container.addView(rowView)
        }

        bottomSheet.setOnDismissListener {
            binding.pieChart.highlightValues(null)
        }

        bottomSheet.setContentView(sheetView)
        bottomSheet.show()
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    private fun updateRecentTransactions(transactions: List<Transaction>) {
        val dateFormat = SimpleDateFormat("dd MMM", Locale("es", "ES"))

        if (transactions.isEmpty()) {
            binding.tvNoRecent.visibility = View.VISIBLE
            binding.layoutRecent1.visibility = View.GONE
            binding.divider2.visibility = View.GONE
            binding.layoutRecent2.visibility = View.GONE
            binding.divider3.visibility = View.GONE
            binding.layoutRecent3.visibility = View.GONE
            return
        }

        binding.tvNoRecent.visibility = View.GONE

        val tx1 = transactions[0]
        binding.layoutRecent1.visibility = View.VISIBLE
        binding.tvRecent1Emoji.text = categoryEmoji(tx1.category)
        binding.tvRecent1Desc.text = tx1.description
        binding.tvRecent1Date.text = dateFormat.format(Date(tx1.date))
        binding.tvRecent1Amount.text =
            if (tx1.type == "INGRESO") "+%.2f €".format(tx1.amount) else "-%.2f €".format(tx1.amount)
        binding.tvRecent1Amount.setTextColor(
            if (tx1.type == "INGRESO") requireContext().getColor(R.color.income_green)
            else requireContext().getColor(R.color.expense_red)
        )

        if (transactions.size >= 2) {
            val tx2 = transactions[1]
            binding.divider2.visibility = View.VISIBLE
            binding.layoutRecent2.visibility = View.VISIBLE
            binding.tvRecent2Emoji.text = categoryEmoji(tx2.category)
            binding.tvRecent2Desc.text = tx2.description
            binding.tvRecent2Date.text = dateFormat.format(Date(tx2.date))
            binding.tvRecent2Amount.text =
                if (tx2.type == "INGRESO") "+%.2f €".format(tx2.amount) else "-%.2f €".format(tx2.amount)
            binding.tvRecent2Amount.setTextColor(
                if (tx2.type == "INGRESO") requireContext().getColor(R.color.income_green)
                else requireContext().getColor(R.color.expense_red)
            )
        } else {
            binding.divider2.visibility = View.GONE
            binding.layoutRecent2.visibility = View.GONE
        }

        if (transactions.size >= 3) {
            val tx3 = transactions[2]
            binding.divider3.visibility = View.VISIBLE
            binding.layoutRecent3.visibility = View.VISIBLE
            binding.tvRecent3Emoji.text = categoryEmoji(tx3.category)
            binding.tvRecent3Desc.text = tx3.description
            binding.tvRecent3Date.text = dateFormat.format(Date(tx3.date))
            binding.tvRecent3Amount.text =
                if (tx3.type == "INGRESO") "+%.2f €".format(tx3.amount) else "-%.2f €".format(tx3.amount)
            binding.tvRecent3Amount.setTextColor(
                if (tx3.type == "INGRESO") requireContext().getColor(R.color.income_green)
                else requireContext().getColor(R.color.expense_red)
            )
        } else {
            binding.divider3.visibility = View.GONE
            binding.layoutRecent3.visibility = View.GONE
        }
    }

    private fun categoryEmoji(category: String) = when (category) {
        "Comida" -> "🍔"
        "Transporte" -> "🚗"
        "Ocio" -> "🎮"
        "Salud" -> "💊"
        "Hogar" -> "🏠"
        "Nómina" -> "💼"
        else -> "📦"
    }

    private fun updatePieChart(gastosByCategory: Map<String, Double>) {
        val entries = gastosByCategory.map { (category, amount) ->
            PieEntry(amount.toFloat(), category)
        }

        val colors = listOf(
            Color.parseColor("#6650A4"),
            Color.parseColor("#9C7FE8"),
            Color.parseColor("#42A5F5"),
            Color.parseColor("#26C6DA"),
            Color.parseColor("#EC407A"),
            Color.parseColor("#FFA726"),
            Color.parseColor("#66BB6A")
        )

        val euroFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float) = "%.0f €".format(value)
        }

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            valueTextColor = Color.WHITE
            valueTextSize = 12f
            valueFormatter = euroFormatter
        }

        binding.pieChart.data = PieData(dataSet)
        binding.pieChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
