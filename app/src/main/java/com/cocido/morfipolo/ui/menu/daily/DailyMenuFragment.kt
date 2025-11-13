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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DailyMenuFragment : Fragment() {

    private var _binding: FragmentDailyMenuBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DailyMenuViewModel by viewModels {
        DailyMenuViewModelFactory(
            (requireActivity().application as MorfipoloApplication).menuRepository,
            (requireActivity().application as MorfipoloApplication).userRepository,
            (requireActivity().application as MorfipoloApplication).voteRepository
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
        
        // Cargar menú cuando el Fragment esté listo
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        viewModel.loadMenuForDate(today.time)
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is DailyMenuUiState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.menuCardView.visibility = View.GONE
                    }
                    is DailyMenuUiState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.menuCardView.visibility = View.VISIBLE

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

        // Fecha - convertir de String a Date
        val menuDate = try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(menu.date) ?: Date()
        } catch (e: Exception) {
            Date()
        }
        binding.dateTextView.text = dateFormat.format(menuDate)

        // Horario - extraer hora de ISO 8601
        val startTime = try {
            menu.start_time.split("T")[1].substring(0, 5) // HH:mm
        } catch (e: Exception) {
            "08:00"
        }
        val endTime = try {
            menu.end_time.split("T")[1].substring(0, 5) // HH:mm
        } catch (e: Exception) {
            "11:00"
        }
        
        binding.timeRangeTextView.text = getString(
            R.string.selection_time,
            startTime,
            endTime
        )

        // Estado
        val statusText = when (menu.status) {
            "open" -> getString(R.string.open)
            "closed" -> getString(R.string.closed)
            else -> menu.status
        }
        binding.statusTextView.text = statusText
        binding.statusTextView.setBackgroundResource(
            if (menu.status == "open") {
                R.drawable.status_badge_green
            } else {
                R.drawable.status_badge_red
            }
        )

        // Ya elegiste
        binding.alreadySelectedTextView.visibility = if (state.userVote != null) {
            View.VISIBLE
        } else {
            View.GONE
        }

        // Descripción del menú
        binding.menuDescriptionTextView.text = menu.description
        
        // Mostrar todas las opciones del menú
        displayMenuOptions(menu, state.userVote, state.isWithinTime, state.menu.status == "open")
    }

    private fun displayMenuOptions(
        menu: com.cocido.morfipolo.domain.model.Menu,
        userVote: com.cocido.morfipolo.domain.model.Vote?,
        isWithinTime: Boolean,
        isMenuOpen: Boolean
    ) {
        // Limpiar opciones anteriores
        binding.optionsContainer.removeAllViews()
        
               if (menu.getOptionsOrEmpty().isEmpty()) {
            // Si no hay opciones, mostrar mensaje
            val noOptionsTextView = android.widget.TextView(requireContext()).apply {
                text = getString(R.string.no_menu_available)
                textSize = 14f
                setTextColor(resources.getColor(R.color.text_secondary, null))
                gravity = android.view.Gravity.CENTER
                setPadding(0, 16, 0, 16)
            }
            binding.optionsContainer.addView(noOptionsTextView)
            return
        }
        
               // Crear una vista para cada opción
               menu.getOptionsOrEmpty().forEachIndexed { index, option ->
            val optionView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_menu_option, binding.optionsContainer, false)
            
            val optionNameTextView = optionView.findViewById<android.widget.TextView>(R.id.optionNameTextView)
            val optionButton = optionView.findViewById<com.google.android.material.button.MaterialButton>(R.id.optionButton)
            val selectedIndicator = optionView.findViewById<android.widget.ImageView>(R.id.selectedIndicator)
            
                   // Nombre de la opción
                   optionNameTextView.text = if (menu.getOptionsOrEmpty().size > 1) {
                "Opción ${index + 1}: ${option.name}"
            } else {
                option.name
            }
            
            // Verificar si esta opción está seleccionada
            val isSelected = userVote?.option?.id == option.id
            
            if (isSelected) {
                // Opción seleccionada
                optionButton.text = getString(R.string.remove_selection)
                optionButton.setIconResource(android.R.drawable.ic_menu_delete)
                optionButton.setBackgroundResource(R.drawable.button_red)
                selectedIndicator.visibility = View.VISIBLE
                optionButton.setOnClickListener {
                    viewModel.deleteVote()
                }
            } else {
                // Opción no seleccionada
                optionButton.text = getString(R.string.choose_option)
                optionButton.setIconResource(android.R.drawable.ic_menu_add)
                optionButton.setBackgroundResource(R.drawable.button_gradient_pressed)
                selectedIndicator.visibility = View.GONE
                optionButton.isEnabled = isWithinTime && isMenuOpen
                optionButton.setOnClickListener {
                    if (isWithinTime && isMenuOpen) {
                        viewModel.selectOption(option.id)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.time_expired),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            
            binding.optionsContainer.addView(optionView)
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
    private val userRepository: com.cocido.morfipolo.data.repository.UserRepository,
    private val voteRepository: com.cocido.morfipolo.data.repository.VoteRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DailyMenuViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DailyMenuViewModel(menuRepository, userRepository, voteRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

