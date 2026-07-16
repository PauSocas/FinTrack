package com.paula.fintrack.ui.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.paula.fintrack.data.local.Transaction
import com.paula.fintrack.databinding.DialogAddTransactionBinding
import com.paula.fintrack.databinding.FragmentTransactionsBinding

class TransactionsFragment : Fragment() {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionsViewModel by viewModels()

    private val categories = listOf("Comida", "Transporte", "Ocio", "Salud", "Hogar", "Nómina", "Otros")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = TransactionAdapter { transaction ->
            AlertDialog.Builder(requireContext())
                .setTitle(transaction.description)
                .setItems(arrayOf("✏️  Editar", "🗑️  Eliminar")) { _, which ->
                    when (which) {
                        0 -> showEditDialog(transaction)
                        1 -> AlertDialog.Builder(requireContext())
                            .setTitle("Eliminar transacción")
                            .setMessage("¿Eliminar \"${transaction.description}\"?")
                            .setPositiveButton("Eliminar") { _, _ -> viewModel.delete(transaction) }
                            .setNegativeButton("Cancelar", null)
                            .show()
                    }
                }
                .show()
        }

        binding.rvTransactions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTransactions.adapter = adapter

        viewModel.allTransactions.observe(viewLifecycleOwner) { transactions ->
            adapter.submitList(transactions)
            binding.tvEmpty.visibility = if (transactions.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.fabAdd.setOnClickListener { showAddDialog() }
    }

    private fun showEditDialog(transaction: Transaction) {
        val dialogBinding = DialogAddTransactionBinding.inflate(layoutInflater)

        val categoryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categories
        )
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerCategory.adapter = categoryAdapter

        // Pre-rellenar con los datos actuales
        dialogBinding.etDescription.setText(transaction.description)
        dialogBinding.etAmount.setText(transaction.amount.toString())
        val categoryIndex = categories.indexOf(transaction.category)
        if (categoryIndex >= 0) dialogBinding.spinnerCategory.setSelection(categoryIndex)
        if (transaction.type == "INGRESO") dialogBinding.rbIngreso.isChecked = true
        else dialogBinding.rbGasto.isChecked = true

        AlertDialog.Builder(requireContext())
            .setTitle("Editar transacción")
            .setView(dialogBinding.root)
            .setPositiveButton("Guardar") { _, _ ->
                val description = dialogBinding.etDescription.text.toString().trim()
                val amountText = dialogBinding.etAmount.text.toString().trim()
                val category = dialogBinding.spinnerCategory.selectedItem.toString()
                val type = if (dialogBinding.rbIngreso.isChecked) "INGRESO" else "GASTO"

                if (description.isNotEmpty() && amountText.isNotEmpty()) {
                    val amount = amountText.toDoubleOrNull() ?: return@setPositiveButton
                    viewModel.update(
                        transaction.copy(
                            description = description,
                            amount = amount,
                            category = category,
                            type = type
                        )
                    )
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showAddDialog() {
        val dialogBinding = DialogAddTransactionBinding.inflate(layoutInflater)

        val categoryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categories
        )
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerCategory.adapter = categoryAdapter

        AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton("Guardar") { _, _ ->
                val description = dialogBinding.etDescription.text.toString().trim()
                val amountText = dialogBinding.etAmount.text.toString().trim()
                val category = dialogBinding.spinnerCategory.selectedItem.toString()
                val type = if (dialogBinding.rbIngreso.isChecked) "INGRESO" else "GASTO"

                if (description.isNotEmpty() && amountText.isNotEmpty()) {
                    val amount = amountText.toDoubleOrNull() ?: return@setPositiveButton
                    viewModel.insert(
                        Transaction(
                            amount = amount,
                            description = description,
                            category = category,
                            date = System.currentTimeMillis(),
                            type = type
                        )
                    )
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
