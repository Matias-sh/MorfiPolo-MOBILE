package com.cocido.morfipolo.ui.notifications

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
import com.google.android.material.slider.Slider
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

        val hourSlider = dialogView.findViewById<Slider>(R.id.hourSlider)
        val minuteSlider = dialogView.findViewById<Slider>(R.id.minuteSlider)
        val timeDisplayTextView = dialogView.findViewById<TextView>(R.id.timeDisplayTextView)
        val hourValueTextView = dialogView.findViewById<TextView>(R.id.hourValueTextView)
        val minuteValueTextView = dialogView.findViewById<TextView>(R.id.minuteValueTextView)
        val mondayChip = dialogView.findViewById<Chip>(R.id.mondayChip)
        val tuesdayChip = dialogView.findViewById<Chip>(R.id.tuesdayChip)
        val wednesdayChip = dialogView.findViewById<Chip>(R.id.wednesdayChip)
        val thursdayChip = dialogView.findViewById<Chip>(R.id.thursdayChip)
        val fridayChip = dialogView.findViewById<Chip>(R.id.fridayChip)
        val saturdayChip = dialogView.findViewById<Chip>(R.id.saturdayChip)
        val sundayChip = dialogView.findViewById<Chip>(R.id.sundayChip)
        val deleteButton = dialogView.findViewById<View>(R.id.deleteButton)

        val chips = mapOf(
            CustomNotification.MONDAY to mondayChip,
            CustomNotification.TUESDAY to tuesdayChip,
            CustomNotification.WEDNESDAY to wednesdayChip,
            CustomNotification.THURSDAY to thursdayChip,
            CustomNotification.FRIDAY to fridayChip,
            CustomNotification.SATURDAY to saturdayChip,
            CustomNotification.SUNDAY to sundayChip
        )

        // Update display whenever sliders change
        fun updateTimeDisplay() {
            val h = hourSlider.value.toInt()
            val m = minuteSlider.value.toInt()
            val minuteStr = m.toString().padStart(2, '0')
            timeDisplayTextView.text = "$h:$minuteStr"
            hourValueTextView.text = "$h:00 AM"
            minuteValueTextView.text = ":$minuteStr"
        }

        hourSlider.addOnChangeListener { _, _, _ -> updateTimeDisplay() }
        minuteSlider.addOnChangeListener { _, _, _ -> updateTimeDisplay() }

        // Load existing values or defaults
        if (notification != null) {
            val hour = notification.hour.coerceIn(8, 11)
            hourSlider.value = hour.toFloat()
            minuteSlider.value = notification.minute.toFloat()
            notification.daysOfWeek.forEach { day -> chips[day]?.isChecked = true }
            deleteButton.visibility = View.VISIBLE
        } else {
            hourSlider.value = 9f
            minuteSlider.value = 0f
            deleteButton.visibility = View.GONE
            chips[CustomNotification.MONDAY]?.isChecked = true
            chips[CustomNotification.TUESDAY]?.isChecked = true
            chips[CustomNotification.WEDNESDAY]?.isChecked = true
            chips[CustomNotification.THURSDAY]?.isChecked = true
            chips[CustomNotification.FRIDAY]?.isChecked = true
        }
        updateTimeDisplay()

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save), null)
            .setNegativeButton(getString(R.string.cancel), null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            positiveButton.setBackgroundColor(
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.slate_600)
            )
            positiveButton.setTextColor(
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white)
            )
            negativeButton.setTextColor(
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.neutral_600)
            )
            positiveButton.setOnClickListener {
                val hour = hourSlider.value.toInt()
                val minute = minuteSlider.value.toInt()

                val selectedDays = chips.filter { it.value.isChecked }.keys.toSet()

                android.util.Log.d("NotificationSettingsFragment", "📅 Días seleccionados para ${hour}:${minute}: [${selectedDays.sorted()}]")

                if (selectedDays.isEmpty()) {
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
