package com.paula.fintrack.ui.investor_profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.paula.fintrack.databinding.FragmentInvestorProfileBinding

class InvestorProfileFragment : Fragment() {

    private var _binding: FragmentInvestorProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInvestorProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCalcular.setOnClickListener {
            val score = calculateScore()
            if (score < 0) {
                Toast.makeText(requireContext(), "Por favor, responde todas las preguntas.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showResult(score)
        }
    }

    private fun calculateScore(): Int {
        val groups = listOf(binding.rg1, binding.rg2, binding.rg3, binding.rg4, binding.rg5)

        for (group in groups) {
            if (group.checkedRadioButtonId == -1) return -1
        }

        fun scoreForGroup(groupIndex: Int): Int {
            val group = groups[groupIndex]
            val checkedId = group.checkedRadioButtonId
            val buttons = (0 until group.childCount).map { group.getChildAt(it).id }
            return buttons.indexOf(checkedId)
        }

        return (0..4).sumOf { scoreForGroup(it) }
    }

    private fun showResult(score: Int) {
        val (emoji, title, description) = when {
            score <= 3 -> Triple(
                "🛡️",
                "Perfil Conservador",
                "Priorizas la seguridad sobre la rentabilidad.\n\nProductos recomendados:\n• Depósitos bancarios\n• Fondos monetarios\n• Bonos del Estado\n• Letras del Tesoro"
            )
            score <= 6 -> Triple(
                "⚖️",
                "Perfil Moderado",
                "Buscas un equilibrio entre seguridad y crecimiento.\n\nProductos recomendados:\n• Fondos mixtos\n• ETFs diversificados\n• Bonos corporativos\n• Fondos indexados"
            )
            else -> Triple(
                "🚀",
                "Perfil Dinámico",
                "Estás dispuesto a asumir riesgos para maximizar la rentabilidad.\n\nProductos recomendados:\n• Acciones de bolsa\n• ETFs de renta variable\n• Fondos de crecimiento\n• Mercados emergentes"
            )
        }

        binding.tvResultEmoji.text = emoji
        binding.tvResultTitle.text = title
        binding.tvResultDesc.text = description
        binding.cardResult.visibility = View.VISIBLE

        binding.cardResult.post {
            val scrollView = binding.root
            scrollView.smoothScrollTo(0, binding.cardResult.top)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
