package com.cocido.morfipolo.ui.notifications

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TimePicker
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cocido.morfipolo.MorfipoloApplication
import com.cocido.morfipolo.R
import com.cocido.morfipolo.databinding.FragmentNotificationSettingsBinding
import com.cocido.morfipolo.domain.model.CustomNotification
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class NotificationSettingsFragment : Fragment() {

    private var _binding: FragmentNotificationSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotificationSettingsViewModel by viewModels {
        val app = requireActivity().application as MorfipoloApplication
        NotificationSettingsViewModelFactory(
            app.notificationConfigRepository,
            requireContext()
        )
    }

    private lateinit var adapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar insets para el botón de retroceso
        ViewCompat.setOnApplyWindowInsetsListener(binding.backButton) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val layoutParams = v.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            layoutParams?.let {
                val originalMarginTop = 24
                val marginTopInPx = (originalMarginTop * resources.displayMetrics.density).toInt()
                it.topMargin = marginTopInPx + systemBars.top
                v.layoutParams = it
            }
            insets
        }

        // Configurar insets para el header
        ViewCompat.setOnApplyWindowInsetsListener(binding.titleTextView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val layoutParams = v.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            layoutParams?.let {
                val originalMarginTop = 24
                val marginTopInPx = (originalMarginTop * resources.displayMetrics.density).toInt()
                it.topMargin = marginTopInPx + systemBars.top
                v.layoutParams = it
            }
            insets
        }

        // Configurar insets para el RecyclerView
        ViewCompat.setOnApplyWindowInsetsListener(binding.notificationsRecyclerView) { v, insets ->
            val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val bottomNavHeightDp = 80f
            val bottomNavHeightPx = (bottomNavHeightDp * resources.displayMetrics.density).toInt()
            val totalBottomPadding = navigationBars.bottom + bottomNavHeightPx + (16 * resources.displayMetrics.density).toInt()
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, totalBottomPadding)
            insets
        }

        setupRecyclerView()
        setupObservers()
        setupListeners()
        setupBackButton()
    }
    
    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter(
            onNotificationClick = { notification ->
                showEditNotificationDialog(notification)
            },
            onToggleChanged = { notification, isEnabled ->
                viewModel.toggleNotification(notification, isEnabled)
            }
        )
        binding.notificationsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.notificationsRecyclerView.adapter = adapter
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is NotificationSettingsUiState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.emptyStateLayout.visibility = View.GONE
                    }
                    is NotificationSettingsUiState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        adapter.submitList(state.notifications)
                        binding.emptyStateLayout.visibility = if (state.notifications.isEmpty()) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                    }
                    is NotificationSettingsUiState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        // Mostrar error si es necesario
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.addNotificationFab.setOnClickListener {
            showEditNotificationDialog(null)
        }
    }

    private fun showEditNotificationDialog(notification: CustomNotification?) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_notification_edit, null)

        val timePicker = dialogView.findViewById<TimePicker>(R.id.timePicker)
        val mondayChip = dialogView.findViewById<Chip>(R.id.mondayChip)
        val tuesdayChip = dialogView.findViewById<Chip>(R.id.tuesdayChip)
        val wednesdayChip = dialogView.findViewById<Chip>(R.id.wednesdayChip)
        val thursdayChip = dialogView.findViewById<Chip>(R.id.thursdayChip)
        val fridayChip = dialogView.findViewById<Chip>(R.id.fridayChip)
        val saturdayChip = dialogView.findViewById<Chip>(R.id.saturdayChip)
        val sundayChip = dialogView.findViewById<Chip>(R.id.sundayChip)
        val deleteButton = dialogView.findViewById<View>(R.id.deleteButton)
        
        // Constantes para el rango permitido
        val MIN_HOUR = 8
        val MAX_HOUR = 11

        val chips = mapOf(
            CustomNotification.MONDAY to mondayChip,
            CustomNotification.TUESDAY to tuesdayChip,
            CustomNotification.WEDNESDAY to wednesdayChip,
            CustomNotification.THURSDAY to thursdayChip,
            CustomNotification.FRIDAY to fridayChip,
            CustomNotification.SATURDAY to saturdayChip,
            CustomNotification.SUNDAY to sundayChip
        )

        // Función helper para validar y ajustar la hora al rango permitido
        fun validateAndAdjustHour(hour: Int): Int {
            return when {
                hour < MIN_HOUR -> MIN_HOUR
                hour > MAX_HOUR -> MAX_HOUR
                else -> hour
            }
        }
        
        // Listener para limitar la hora al rango permitido (8:00 AM - 11:00 AM)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            timePicker.setOnTimeChangedListener { _, hour, minute ->
                val adjustedHour = validateAndAdjustHour(hour)
                if (hour != adjustedHour) {
                    timePicker.hour = adjustedHour
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Las notificaciones solo pueden configurarse entre las 8:00 AM y 11:00 AM",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        
        // Si es edición, cargar valores existentes
        if (notification != null) {
            val hour = validateAndAdjustHour(notification.hour)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                timePicker.hour = hour
                timePicker.minute = notification.minute
            } else {
                @Suppress("DEPRECATION")
                timePicker.currentHour = hour
                @Suppress("DEPRECATION")
                timePicker.currentMinute = notification.minute
            }
            
            notification.daysOfWeek.forEach { day ->
                chips[day]?.isChecked = true
            }
            
            deleteButton.visibility = View.VISIBLE
        } else {
            deleteButton.visibility = View.GONE
            // Por defecto, 9:00 AM (dentro del rango permitido)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                timePicker.hour = 9
                timePicker.minute = 0
            } else {
                @Suppress("DEPRECATION")
                timePicker.currentHour = 9
                @Suppress("DEPRECATION")
                timePicker.currentMinute = 0
            }
            // Por defecto, solo días laborales
            chips[CustomNotification.MONDAY]?.isChecked = true
            chips[CustomNotification.TUESDAY]?.isChecked = true
            chips[CustomNotification.WEDNESDAY]?.isChecked = true
            chips[CustomNotification.THURSDAY]?.isChecked = true
            chips[CustomNotification.FRIDAY]?.isChecked = true
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save), null)
            .setNegativeButton(getString(R.string.cancel), null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                var hour = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    timePicker.hour
                } else {
                    @Suppress("DEPRECATION")
                    timePicker.currentHour
                }
                
                val minute = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    timePicker.minute
                } else {
                    @Suppress("DEPRECATION")
                    timePicker.currentMinute
                }
                
                // Validar que la hora esté en el rango permitido (8:00 AM - 11:00 AM)
                if (hour < MIN_HOUR || hour > MAX_HOUR) {
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Las notificaciones solo pueden configurarse entre las 8:00 AM y 11:00 AM",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    // Ajustar la hora al rango permitido
                    hour = when {
                        hour < MIN_HOUR -> MIN_HOUR
                        hour > MAX_HOUR -> MAX_HOUR
                        else -> hour
                    }
                    // Actualizar el TimePicker
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        timePicker.hour = hour
                    } else {
                        @Suppress("DEPRECATION")
                        timePicker.currentHour = hour
                    }
                    return@setOnClickListener
                }
                
                val selectedDays = chips.filter { it.value.isChecked }.keys.toSet()
                
                // Log para debugging
                val selectedDaysStr = selectedDays.sorted().joinToString(", ") { 
                    CustomNotification.getDayNameFull(it) 
                }
                android.util.Log.d("NotificationSettingsFragment", "📅 Días seleccionados para ${hour}:${minute}: [$selectedDaysStr] (valores: ${selectedDays.sorted()})")
                
                if (selectedDays.isEmpty()) {
                    // Mostrar error: debe seleccionar al menos un día
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Debes seleccionar al menos un día de la semana",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                
                val notificationId = notification?.id ?: CustomNotification.generateId(hour, minute)
                val updatedNotification = CustomNotification(
                    id = notificationId,
                    hour = hour,
                    minute = minute,
                    isEnabled = notification?.isEnabled ?: true,
                    daysOfWeek = selectedDays
                )
                
                viewModel.saveNotification(updatedNotification)
                dialog.dismiss()
            }
            
            if (notification != null) {
                deleteButton.setOnClickListener {
                    AlertDialog.Builder(requireContext())
                        .setTitle(getString(R.string.delete))
                        .setMessage("¿Estás seguro que deseas eliminar esta notificación?")
                        .setPositiveButton(getString(R.string.delete)) { _, _ ->
                            viewModel.deleteNotification(notification)
                            dialog.dismiss()
                        }
                        .setNegativeButton(getString(R.string.cancel), null)
                        .show()
                }
            }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class NotificationSettingsViewModelFactory(
    private val notificationConfigRepository: com.cocido.morfipolo.data.repository.NotificationConfigRepository,
    private val context: android.content.Context
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationSettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotificationSettingsViewModel(notificationConfigRepository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
