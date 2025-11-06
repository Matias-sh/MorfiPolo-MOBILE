package com.cocido.morfipolo.ui.menu.daily

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.cocido.morfipolo.MorfipoloApplication
import com.cocido.morfipolo.R
import com.cocido.morfipolo.databinding.FragmentDailyMenuBinding
import com.cocido.morfipolo.domain.model.MenuStatus
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DailyMenuFragment : Fragment() {

    private var _binding: FragmentDailyMenuBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DailyMenuViewModel by viewModels {
        DailyMenuViewModelFactory(
            (requireActivity().application as MorfipoloApplication).menuRepository,
            (requireActivity().application as MorfipoloApplication).userRepository
        )
    }

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDailyMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is DailyMenuUiState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.menuCardView.visibility = View.GONE
                        binding.actionButton.visibility = View.GONE
                    }
                    is DailyMenuUiState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.menuCardView.visibility = View.VISIBLE
                        binding.actionButton.visibility = View.VISIBLE

                        updateUI(state)
                    }
                    is DailyMenuUiState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun updateUI(state: DailyMenuUiState.Success) {
        val menu = state.menu

        // Fecha
        binding.dateTextView.text = dateFormat.format(menu.fecha)

        // Horario
        binding.timeRangeTextView.text = getString(
            R.string.selection_time,
            menu.horarioInicio,
            menu.horarioFin
        )

        // Estado
        binding.statusTextView.text = if (menu.estado == MenuStatus.ABIERTO) {
            getString(R.string.open)
        } else {
            getString(R.string.closed)
        }
        binding.statusTextView.setBackgroundResource(
            if (menu.estado == MenuStatus.ABIERTO) {
                R.drawable.status_badge_green
            } else {
                R.drawable.status_badge_red
            }
        )

        // Ya elegiste
        binding.alreadySelectedTextView.visibility = if (state.hasSelected) {
            View.VISIBLE
        } else {
            View.GONE
        }

        // Descripción del menú
        binding.menuDescriptionTextView.text = menu.descripcion

        // Botón de acción
        if (state.hasSelected) {
            binding.actionButton.text = getString(R.string.remove_selection)
            binding.actionButton.setIconResource(android.R.drawable.ic_menu_delete)
            binding.actionButton.setBackgroundResource(R.drawable.button_red)
            binding.actionButton.setOnClickListener {
                viewModel.deselectMenu()
            }
        } else {
            binding.actionButton.text = getString(R.string.choose_option)
            binding.actionButton.setIconResource(android.R.drawable.ic_menu_add)
            binding.actionButton.setBackgroundResource(R.drawable.button_gradient_pressed)
            binding.actionButton.isEnabled = state.isWithinTime && menu.estado == MenuStatus.ABIERTO
            binding.actionButton.setOnClickListener {
                if (state.isWithinTime) {
                    viewModel.selectMenu()
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.time_expired),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun setupListeners() {
        binding.previousButton.setOnClickListener {
            viewModel.navigateToPreviousDay()
        }

        binding.nextButton.setOnClickListener {
            viewModel.navigateToNextDay()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class DailyMenuViewModelFactory(
    private val menuRepository: com.cocido.morfipolo.data.repository.MenuRepository,
    private val userRepository: com.cocido.morfipolo.data.repository.UserRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DailyMenuViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DailyMenuViewModel(menuRepository, userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

