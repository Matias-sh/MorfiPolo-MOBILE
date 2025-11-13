package com.cocido.morfipolo.ui.menu.daily

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.cocido.morfipolo.MorfipoloApplication
import com.cocido.morfipolo.R
import com.cocido.morfipolo.databinding.FragmentDailyMenuBinding
import com.cocido.morfipolo.util.NetworkUtils
import com.google.android.material.snackbar.Snackbar
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
    private val dateFormatApi = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

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
        setupPullToRefresh()
        checkNetworkStatus()
        
        // Cargar menú: usar fecha del argumento si existe, sino usar hoy
        val menuDateArg = arguments?.getString("menuDate", "") ?: ""
        val dateToLoad = if (menuDateArg.isNotEmpty()) {
            try {
                dateFormatApi.parse(menuDateArg) ?: getTodayDate()
            } catch (e: Exception) {
                getTodayDate()
            }
        } else {
            getTodayDate()
        }
        viewModel.loadMenuForDate(dateToLoad)
    }
    
    private fun getTodayDate(): Date {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }
    
    private fun setupPullToRefresh() {
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.nonna_brown_primary,
            R.color.nonna_accent_warm,
            R.color.nonna_success
        )
        binding.swipeRefreshLayout.setOnRefreshListener {
            val currentDate = viewModel.getCurrentDate()
            viewModel.loadMenuForDate(currentDate)
        }
    }
    
    private fun checkNetworkStatus() {
        val isOnline = NetworkUtils.isNetworkAvailable(requireContext())
        binding.offlineIndicator.visibility = if (!isOnline) View.VISIBLE else View.GONE
    }
    
    private fun showErrorWithRetry(message: String, retryAction: () -> Unit) {
        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_INDEFINITE)
        snackbar.setAction(getString(R.string.error_retry)) {
            retryAction()
        }
        snackbar.setActionTextColor(resources.getColor(R.color.nonna_brown_primary, null))
        snackbar.show()
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.swipeRefreshLayout.isRefreshing = false
                
                when (state) {
                    is DailyMenuUiState.Loading -> {
                        if (!binding.swipeRefreshLayout.isRefreshing) {
                            binding.progressBar.visibility = View.VISIBLE
                        }
                        binding.optionsContainer.visibility = View.GONE
                        binding.menuDescriptionTextView.visibility = View.GONE
                        checkNetworkStatus()
                    }
                    is DailyMenuUiState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.optionsContainer.visibility = View.VISIBLE
                        binding.menuDescriptionTextView.visibility = View.VISIBLE
                        binding.offlineIndicator.visibility = View.GONE
                        
                        updateUI(state)
                    }
                    is DailyMenuUiState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        checkNetworkStatus()
                        
                        val errorMessage = when {
                            !NetworkUtils.isNetworkAvailable(requireContext()) -> getString(R.string.error_no_connection)
                            state.message.contains("sesión", ignoreCase = true) || state.message.contains("session", ignoreCase = true) -> getString(R.string.error_session_expired)
                            else -> state.message
                        }
                        
                        showErrorWithRetry(errorMessage) {
                            val currentDate = viewModel.getCurrentDate()
                            viewModel.loadMenuForDate(currentDate)
                        }
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

        // Horario fijo: 08:00 - 11:00
        binding.timeRangeTextView.text = getString(
            R.string.selection_time,
            "08:00",
            "11:00"
        )

        // Estado - validar si realmente está abierto según el horario (08:00 - 11:00)
        val isActuallyOpen = menu.status == "open" && state.isWithinTime
        val statusText = when {
            isActuallyOpen -> getString(R.string.open)
            menu.status == "closed" -> getString(R.string.closed)
            else -> getString(R.string.closed) // Si pasó el horario, mostrar cerrado
        }
        binding.statusTextView.text = statusText
        binding.statusTextView.setBackgroundResource(
            if (isActuallyOpen) {
                R.drawable.badge_success_modern
            } else {
                R.drawable.badge_error_modern
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
        
        // Mostrar todas las opciones del menú (reutilizar isActuallyOpen ya calculado arriba)
        displayMenuOptions(menu, state.userVote, state.isWithinTime, isActuallyOpen)
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
                    showConfirmDeleteVoteDialog {
                        viewModel.deleteVote()
                    }
                }
            } else {
                // Opción no seleccionada
                optionButton.text = getString(R.string.choose_option)
                optionButton.setIconResource(android.R.drawable.ic_menu_add)
                optionButton.setBackgroundResource(R.drawable.button_primary_solid)
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

    private fun showConfirmDeleteVoteDialog(onConfirm: () -> Unit) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.confirm_remove_vote_title))
            .setMessage(getString(R.string.confirm_remove_vote_message))
            .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                onConfirm()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
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

