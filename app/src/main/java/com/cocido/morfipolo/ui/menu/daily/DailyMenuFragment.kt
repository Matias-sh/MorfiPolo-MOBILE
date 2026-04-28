package com.cocido.morfipolo.ui.menu.daily

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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

    private var infoBannerHideJob: kotlinx.coroutines.Job? = null
    
    // BroadcastReceiver para escuchar actualizaciones del menú
    private val menuUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.cocido.morfipolo.MENU_UPDATED") {
                android.util.Log.d("DailyMenuFragment", "📱 Recibido broadcast de actualización de menú")
                // Recargar el menú del día actual
                viewModel.loadMenuForDate(viewModel.getCurrentDate())
            }
        }
    }

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
        
        // Registrar BroadcastReceiver para escuchar actualizaciones del menú
        // RECEIVER_NOT_EXPORTED porque solo escuchamos broadcasts internos de nuestra app
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(
                menuUpdateReceiver,
                IntentFilter("com.cocido.morfipolo.MENU_UPDATED"),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            requireContext().registerReceiver(
                menuUpdateReceiver,
                IntentFilter("com.cocido.morfipolo.MENU_UPDATED")
            )
        }
        
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
            R.color.comedor_brown_primary,
            R.color.comedor_accent_warm,
            R.color.comedor_success
        )
        binding.swipeRefreshLayout.setOnRefreshListener {
            val currentDate = viewModel.getCurrentDate()
            viewModel.loadMenuForDate(currentDate)
        }
    }
    
    private fun checkNetworkStatus() {
        val isOnline = NetworkUtils.isNetworkAvailable(requireContext())
        // binding.offlineIndicator.visibility = if (!isOnline) View.VISIBLE else View.GONE
    }
    
    private var currentSnackbar: Snackbar? = null
    
    private fun showErrorWithRetry(message: String, retryAction: () -> Unit) {
        // Ocultar snackbar anterior si existe
        currentSnackbar?.dismiss()
        
        // Usar duración de 4 segundos
        val snackbar = Snackbar.make(binding.root, message, 4000)
        snackbar.setAction(getString(R.string.error_retry)) {
            retryAction()
        }
        snackbar.setActionTextColor(resources.getColor(R.color.comedor_brown_primary, null))
        snackbar.addCallback(object : Snackbar.Callback() {
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                currentSnackbar = null
            }
        })
        currentSnackbar = snackbar
        snackbar.show()
    }
    
    private fun showTemporaryMessage(message: String) {
        // Ocultar snackbar anterior si existe
        currentSnackbar?.dismiss()
        
        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
        snackbar.addCallback(object : Snackbar.Callback() {
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                currentSnackbar = null
            }
        })
        currentSnackbar = snackbar
        snackbar.show()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch {
                    // Observar cuando la sesión expira
                    viewModel.sessionExpired.collect { expired ->
                        if (expired) {
                            android.util.Log.w("DailyMenuFragment", "Sesión expirada, redirigiendo al login")
                            navigateToLogin()
                        }
                    }
                }

                launch {
                    viewModel.uiState.collect { state ->
                        binding.swipeRefreshLayout.isRefreshing = false

                        when (state) {
                            is DailyMenuUiState.Loading -> {
                                if (!binding.swipeRefreshLayout.isRefreshing) {
                                    binding.progressBar.visibility = View.VISIBLE
                                }
                                binding.optionsContainer.visibility = View.GONE
                                // Mostrar información genérica mientras carga
                                val today = Date()
                                binding.dateTextView.text = dateFormat.format(today)
                                binding.dateTextView.visibility = View.VISIBLE
                                binding.timeRangeTextView.text = "Horario para elegir: 08:00 - 11:00"
                                binding.timeRangeTextView.visibility = View.VISIBLE
                                binding.statusContainer.visibility = View.GONE
                                // El card siempre está visible, no se oculta
                                checkNetworkStatus()
                            }
                            is DailyMenuUiState.Success -> {
                                binding.progressBar.visibility = View.GONE
                                binding.optionsContainer.visibility = View.VISIBLE
                                // El card siempre está visible, no se oculta
                                // binding.offlineIndicator.visibility = View.GONE

                                updateUI(state)
                            }
                            is DailyMenuUiState.Error -> {
                                binding.progressBar.visibility = View.GONE
                                checkNetworkStatus()

                                // Detectar tipo de error
                                when {
                                    state.message.contains("sesión", ignoreCase = true) ||
                                        state.message.contains("session", ignoreCase = true) -> {
                                        // Error de sesión expirada, redirigir al login
                                        navigateToLogin()
                                        return@collect
                                    }
                                    state.message.contains("No hay menú disponible", ignoreCase = true) ||
                                        state.message.contains("no hay menu", ignoreCase = true) -> {
                                        // No hay menú - mostrar mensaje en el área de opciones en lugar del snackbar
                                        showNoMenuMessage()
                                    }
                                    state.message.contains("08:00", ignoreCase = true) ||
                                        state.message.contains("11:00", ignoreCase = true) ||
                                        state.message.contains("horario", ignoreCase = true) ||
                                        state.message.contains("cerrado", ignoreCase = true) ||
                                        state.message.contains("eliminar el voto", ignoreCase = true) ||
                                        state.message.contains("votar", ignoreCase = true) -> {
                                        // Error de horario - mostrar banner informativo sin reintentar
                                        showInfoBanner(state.message)
                                    }
                                    !NetworkUtils.isNetworkAvailable(requireContext()) -> {
                                        // Error de conexión - mostrar con reintentar
                                        showErrorWithRetry(getString(R.string.error_no_connection)) {
                                            viewModel.loadMenuForDate(viewModel.getCurrentDate())
                                        }
                                    }
                                    else -> {
                                        // Otros errores - mostrar con reintentar
                                        showErrorWithRetry(state.message) {
                                            viewModel.loadMenuForDate(viewModel.getCurrentDate())
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    private fun navigateToLogin() {
        val intent = android.content.Intent(requireContext(), com.cocido.morfipolo.ui.login.LoginActivity::class.java)
        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    private fun updateUI(state: DailyMenuUiState.Success) {
        val menu = state.menu

        // Mostrar/ocultar banner informativo
        if (state.infoMessage != null) {
            showInfoBanner(state.infoMessage)
        } else {
            hideInfoBanner()
        }

        // Mostrar elementos cuando hay menú disponible
        binding.dateTextView.visibility = View.VISIBLE
        binding.timeRangeTextView.visibility = View.VISIBLE
        binding.statusContainer.visibility = View.VISIBLE
        
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

        // Estado - usar el valor calculado en el ViewModel
        val isActuallyOpen = state.isActuallyOpen
        val statusText = when {
            isActuallyOpen -> getString(R.string.open)
            menu.status == "closed" -> getString(R.string.closed)
            else -> getString(R.string.closed) // Si pasó el horario o no es hoy, mostrar cerrado
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

        // El card siempre muestra "Menú de hoy" (estático en el XML)
        // No se modifica el texto del card
        
        // Mostrar todas las opciones del menú (reutilizar isActuallyOpen ya calculado arriba)
        displayMenuOptions(menu, state.userVote, state.isWithinTime, isActuallyOpen)
    }
    
    private fun showInfoBanner(message: String) {
        // Cancelar trabajo anterior si existe
        infoBannerHideJob?.cancel()
        
        binding.infoBannerText.text = message
        binding.infoBannerIcon.setImageResource(android.R.drawable.ic_dialog_info)
        binding.infoBanner.setCardBackgroundColor(resources.getColor(R.color.comedor_accent_warm, null))
        
        // Mostrar con animación suave
        if (binding.infoBanner.visibility != View.VISIBLE) {
            binding.infoBanner.alpha = 0f
            binding.infoBanner.visibility = View.VISIBLE
            binding.infoBanner.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
        }
        
        // Ocultar automáticamente después de 5 segundos
        infoBannerHideJob = lifecycleScope.launch {
            kotlinx.coroutines.delay(5000) // 5 segundos
            hideInfoBanner()
        }
    }
    
    private fun hideInfoBanner() {
        // Cancelar trabajo de ocultación si existe
        infoBannerHideJob?.cancel()
        infoBannerHideJob = null
        
        if (binding.infoBanner.visibility == View.VISIBLE) {
            binding.infoBanner.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    binding.infoBanner.visibility = View.GONE
                }
                .start()
        }
    }

    private fun showNoMenuMessage() {
        // Mostrar información genérica pero real cuando no hay menú
        // Fecha de HOY (no estática incorrecta)
        val today = Date()
        binding.dateTextView.text = dateFormat.format(today)
        binding.dateTextView.visibility = View.VISIBLE
        
        // Horario estándar
        binding.timeRangeTextView.text = "Horario para elegir: 08:00 - 11:00"
        binding.timeRangeTextView.visibility = View.VISIBLE
        
        // Ocultar estado "Abierto/Cerrado" ya que no hay menú de referencia
        binding.statusContainer.visibility = View.GONE
        
        // Limpiar opciones anteriores
        binding.optionsContainer.removeAllViews()
        
        // Crear un card elegante para el mensaje
        val noMenuCard = com.google.android.material.card.MaterialCardView(requireContext()).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 16)
            }
            cardElevation = 4f
            radius = 16f
            setCardBackgroundColor(resources.getColor(R.color.food_background_card, null))
            strokeWidth = 0
        }
        
        // Contenedor interno con padding
        val cardContent = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(32, 48, 32, 48)
        }
        
        // Función helper para convertir dp a px
        fun dpToPx(dp: Int): Int {
            return (dp * resources.displayMetrics.density).toInt()
        }
        
        // Icono
        val iconView = android.widget.ImageView(requireContext()).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                dpToPx(80),
                dpToPx(80)
            ).apply {
                bottomMargin = dpToPx(16)
            }
            setImageResource(android.R.drawable.ic_dialog_info)
            imageTintList = android.content.res.ColorStateList.valueOf(
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.food_text_secondary)
            )
            alpha = 0.6f
        }
        
        // Texto principal
        val titleText = android.widget.TextView(requireContext()).apply {
            text = "No hay menú disponible"
            textSize = 18f
            setTextColor(resources.getColor(R.color.food_text_primary, null))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(8)
            }
        }
        
        // Texto secundario
        val subtitleText = android.widget.TextView(requireContext()).apply {
            text = "No hay menú para el día de hoy"
            textSize = 14f
            setTextColor(resources.getColor(R.color.food_text_secondary, null))
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        // Agregar elementos al card
        cardContent.addView(iconView)
        cardContent.addView(titleText)
        cardContent.addView(subtitleText)
        noMenuCard.addView(cardContent)
        
        binding.optionsContainer.visibility = View.VISIBLE
        binding.optionsContainer.addView(noMenuCard)
        
        // El card siempre está visible, no se oculta
    }
    
    private fun displayMenuOptions(
        menu: com.cocido.morfipolo.domain.model.Menu,
        userVote: com.cocido.morfipolo.domain.model.Vote?,
        isWithinTime: Boolean,
        isMenuOpen: Boolean
    ) {
        // Limpiar opciones anteriores
        binding.optionsContainer.removeAllViews()
        
        // El card siempre está visible, no necesita lógica de visibilidad
        
        val options = menu.getOptionsOrEmpty()
        
        if (options.isEmpty()) {
            // Si no hay opciones pero hay descripción, mostrar la descripción como información
            if (menu.description.isNotBlank()) {
                val descriptionCard = com.google.android.material.card.MaterialCardView(requireContext()).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, 12)
                    }
                    cardElevation = 2f
                    radius = 12f
                    setCardBackgroundColor(resources.getColor(R.color.food_background_card, null))
                    strokeWidth = 1
                    strokeColor = resources.getColor(R.color.food_divider, null)
                }
                
                val cardContent = android.widget.LinearLayout(requireContext()).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    setPadding(16, 16, 16, 16)
                }
                
                val descriptionText = android.widget.TextView(requireContext()).apply {
                    text = menu.description
                    textSize = 15f
                    setTextColor(resources.getColor(R.color.food_text_primary, null))
                    gravity = android.view.Gravity.START
                }
                
                cardContent.addView(descriptionText)
                descriptionCard.addView(cardContent)
                binding.optionsContainer.addView(descriptionCard)
            } else {
                // Si no hay opciones ni descripción, mostrar mensaje
                val noOptionsTextView = android.widget.TextView(requireContext()).apply {
                    text = getString(R.string.no_menu_available)
                    textSize = 14f
                    setTextColor(resources.getColor(R.color.text_secondary, null))
                    gravity = android.view.Gravity.CENTER
                    setPadding(0, 16, 0, 16)
                }
                binding.optionsContainer.addView(noOptionsTextView)
            }
            return
        }
        
        // Crear una vista para cada opción
        options.forEachIndexed { index, option ->
            val optionView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_menu_option, binding.optionsContainer, false)
            
            val optionNameTextView = optionView.findViewById<android.widget.TextView>(R.id.optionNameTextView)
            val optionButton = optionView.findViewById<com.google.android.material.button.MaterialButton>(R.id.optionButton)
            val statusMessageTextView = optionView.findViewById<android.widget.TextView>(R.id.statusMessageTextView)
            
            // Nombre de la opción (normalizado para mejorar legibilidad)
            val normalizedOptionName = option.name
                .replace(Regex("\\s*,\\s*"), ", ")
                .replace(Regex("\\s+"), " ")
                .trim()
            optionNameTextView.text = if (menu.getOptionsOrEmpty().size > 1) {
                "Opción ${index + 1} · $normalizedOptionName"
            } else {
                normalizedOptionName
            }
            
            val canVote = isWithinTime && isMenuOpen
            val isSelected = userVote?.option?.id == option.id
            val hasVotedAny = userVote != null
            
            if (!canVote) {
                // Estado CERRADO / FUERA DE HORARIO
                optionButton.visibility = View.GONE
                statusMessageTextView.visibility = View.VISIBLE
                
                if (isSelected) {
                    // El usuario votó ESTA opción
                    statusMessageTextView.text = "Usted ya seleccionó esta opción"
                    statusMessageTextView.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.food_secondary_green))
                    statusMessageTextView.setTypeface(null, android.graphics.Typeface.BOLD)
                } else if (!hasVotedAny) {
                    // El usuario NO votó nada hoy
                    statusMessageTextView.text = "Usted no seleccionó una opción el día de hoy"
                    statusMessageTextView.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.food_status_error))
                    statusMessageTextView.setTypeface(null, android.graphics.Typeface.NORMAL)
                } else {
                    // votó OTRA opción
                     statusMessageTextView.text = "No seleccionada"
                     statusMessageTextView.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.food_text_secondary))
                     statusMessageTextView.setTypeface(null, android.graphics.Typeface.NORMAL)
                }
                
            } else {
                // Estado ABIERTO / PENDIENTE
                statusMessageTextView.visibility = View.GONE
                optionButton.visibility = View.VISIBLE
                
                if (isSelected) {
                    // Opción seleccionada -> Botón rojo "Quitar"
                    optionButton.text = getString(R.string.remove_selection)
                    optionButton.icon = null
                    optionButton.backgroundTintList = AppCompatResources.getColorStateList(requireContext(), R.color.vote_button_negative_tint)
                    optionButton.rippleColor = android.content.res.ColorStateList.valueOf(
                        androidx.core.content.ContextCompat.getColor(requireContext(), R.color.food_ripple)
                    )
                    optionButton.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white))
                    
                    optionButton.setOnClickListener {
                        if (canVote) viewModel.deleteVote()
                    }
                } else {
                    // Opción no seleccionada -> Botón verde "Elegir"
                    optionButton.text = getString(R.string.choose_option)
                    optionButton.icon = null
                    optionButton.backgroundTintList = AppCompatResources.getColorStateList(requireContext(), R.color.vote_button_positive_tint)
                    optionButton.rippleColor = android.content.res.ColorStateList.valueOf(
                        androidx.core.content.ContextCompat.getColor(requireContext(), R.color.food_ripple)
                    )
                    optionButton.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white))
                    
                    optionButton.setOnClickListener {
                        if (canVote) viewModel.selectOption(option.id)
                    }
                }
            }
            
            binding.optionsContainer.addView(optionView)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        // Desregistrar BroadcastReceiver
        try {
            requireContext().unregisterReceiver(menuUpdateReceiver)
        } catch (e: Exception) {
            // El receiver puede no estar registrado, ignorar
        }
        // Cancelar trabajo de ocultación del banner
        infoBannerHideJob?.cancel()
        infoBannerHideJob = null
        // Ocultar snackbar si existe
        currentSnackbar?.dismiss()
        currentSnackbar = null
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

