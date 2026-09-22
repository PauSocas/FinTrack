package com.paula.fintrack.ui.transactions

import android.view.LayoutInflater
import com.paula.fintrack.R
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.paula.fintrack.data.local.Transaction
import com.paula.fintrack.databinding.ItemTransactionBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionAdapter(
    private val onLongClick: (Transaction) -> Unit
) : ListAdapter<Transaction, TransactionAdapter.ViewHolder>(DiffCallback()) {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private val categoryEmoji = mapOf(
        "Comida" to "🍔",
        "Transporte" to "🚗",
        "Ocio" to "🎮",
        "Salud" to "💊",
        "Hogar" to "🏠",
        "Nómina" to "💼",
        "Otros" to "📦"
    )

    inner class ViewHolder(private val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Transaction) {
            binding.tvDescription.text = transaction.description
            binding.tvDate.text = dateFormat.format(Date(transaction.date))
            binding.tvCategory.text = categoryEmoji[transaction.category] ?: "📦"

            if (transaction.type == "INGRESO") {
                binding.tvAmount.text = "+%.2f €".format(transaction.amount)
                binding.tvAmount.setTextColor(binding.root.context.getColor(R.color.income_green))
            } else {
                binding.tvAmount.text = "-%.2f €".format(transaction.amount)
                binding.tvAmount.setTextColor(binding.root.context.getColor(R.color.expense_red))
            }

            binding.root.setOnLongClickListener {
                onLongClick(transaction)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction) =
            oldItem == newItem
    }
}
