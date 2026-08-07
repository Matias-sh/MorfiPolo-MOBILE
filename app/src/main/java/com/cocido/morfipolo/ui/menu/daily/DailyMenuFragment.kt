package com.cocido.morfipolo.ui.menu.daily

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.cocido.morfipolo.MorfipoloApplication
import com.cocido.morfipolo.R
import com.cocido.morfipolo.databinding.FragmentDailyMenuBinding
import com.cocido.morfipolo.util.NetworkUtils
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

    // Formato largo del spec: "Lunes 28 de julio"
    private val dateFormat = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "AR"))
    private val dateFormatApi = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val localHourFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    private fun formatDisplayDate(date: Date): String {
        return dateFormat.format(date).replaceFirstChar { it.uppercase() }
    }

    /**
     * Hora de cierre efectiva del menú en hora local — la real (end_time) ya
     * recortada a las 11:00 como máximo por MenuTimeUtils, sin importar lo
     * que mande el backend. Si no se puede resolver, "11:00" es el valor
     * esperado en la práctica.
     */
    private fun formatMenuEndTime(menu: com.cocido.morfipolo.domain.model.Menu): String {
        val effectiveEnd = com.cocido.morfipolo.util.MenuTimeUtils.getEffectiveEndTimeMillis(menu)
        return if (effectiveEnd != null) localHourFormat.format(Date(effectiveEnd)) else "11:00"
    }

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

        // Insets: correr el header debajo de la barra de estado
        ViewCompat.setOnApplyWindowInsetsListener(binding.headerCaption) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val layoutParams = v.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            layoutParams?.let {
                val originalMarginTop = 24 // space_2xl del XML
                val marginTopInPx = (originalMarginTop * resources.displayMetrics.density).toInt()
                it.topMargin = marginTopInPx + systemBars.top
                v.layoutParams = it
            }
            insets
        }

        // Configurar insets para el ScrollView - aplicar padding inferior para evitar solapamiento con la barra de navegación
        ViewCompat.setOnApplyWindowInsetsListener(binding.scrollView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Obtener insets de navegación (incluye barra de navegación del sistema)
            val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            // Calcular altura de la BottomNavigationView de la app (aproximadamente 80dp + insets)
            val bottomNavHeightDp = 80f
            val bottomNavHeightPx = (bottomNavHeightDp * resources.displayMetrics.density).toInt()
            // Padding total = insets del sistema + altura de la barra de navegación de la app + margen extra
            val totalBottomPadding = navigationBars.bottom + bottomNavHeightPx + (16 * resources.displayMetrics.density).toInt()
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, totalBottomPadding)
            insets
        }

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
        loadMenuFromArguments()
    }
    
    override fun onResume() {
        super.onResume()
        // Si no hay argumentos de fecha (viene de la barra de navegación, no del menú semanal),
        // resolver de nuevo cuál es el menú vigente. No alcanza con comparar contra "hoy": el
        // backend abre la votación de mañana desde ~21:00, así que el menú vigente puede
        // cambiar de identidad (de hoy a mañana) sin que el usuario navegue a ningún lado —
        // solo por haber pasado esa hora mientras la app estaba en segundo plano.
        val menuDateArg = arguments?.getString("menuDate", "") ?: ""
        if (menuDateArg.isEmpty()) {
            android.util.Log.d("DailyMenuFragment", "🔄 Resolviendo menú vigente desde barra de navegación")
            viewModel.loadActiveMenu(showLoading = false)
        } else {
            // Hay argumentos, pero si la fecha es diferente a hoy y ya se cargó,
            // limpiar los argumentos para que la próxima vez muestre hoy
            val argDate = try {
                dateFormatApi.parse(menuDateArg)
            } catch (e: Exception) {
                null
            }
            val today = getTodayDate()
            if (argDate != null && !isSameDate(argDate, today)) {
                // La fecha del argumento no es hoy, limpiar argumentos después de cargar
                // para que la próxima vez que se vuelva desde la barra de navegación muestre hoy
                android.util.Log.d("DailyMenuFragment", "🧹 Limpiando argumentos de fecha antigua")
                arguments?.remove("menuDate")
            }
        }
    }
    
    private fun loadMenuFromArguments() {
        val menuDateArg = arguments?.getString("menuDate", "") ?: ""
        if (menuDateArg.isNotEmpty()) {
            val dateToLoad = try {
                dateFormatApi.parse(menuDateArg) ?: getTodayDate()
            } catch (e: Exception) {
                getTodayDate()
            }
            viewModel.loadMenuForDate(dateToLoad)
        } else {
            // Sin fecha explícita: resolver cuál es el menú vigente para votar
            // ahora (puede ser el de mañana si ya se abrió la ventana nocturna).
            viewModel.loadActiveMenu()
        }
    }
    
    private fun isSameDate(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
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
            // Si hay argumentos, mantener la fecha del argumento. Si no, volver a
            // resolver el menú vigente (puede haber cambiado a partir de ~21:00).
            val menuDateArg = arguments?.getString("menuDate", "") ?: ""
            if (menuDateArg.isNotEmpty()) {
                val dateToLoad = try {
                    dateFormatApi.parse(menuDateArg) ?: getTodayDate()
                } catch (e: Exception) {
                    getTodayDate()
                }
                viewModel.loadMenuForDate(dateToLoad)
            } else {
                viewModel.loadActiveMenu(showLoading = false)
            }
        }
    }
    
    private fun checkNetworkStatus() {
        val isOnline = NetworkUtils.isNetworkAvailable(requireContext())
        binding.offlineIndicator.visibility = if (!isOnline) View.VISIBLE else View.GONE
    }

    private fun setupObservers() {
        // viewLifecycleOwner (no el Fragment) evita colectores duplicados si
        // onCreateView/onViewCreated se vuelven a ejecutar sobre la misma
        // instancia de Fragment (back stack) sin pasar por onDestroy real.
        viewLifecycleOwner.lifecycleScope.launch {
            // Observar cuando la sesión expira
            viewModel.sessionExpired.collect { expired ->
                if (expired) {
                    android.util.Log.w("DailyMenuFragment", "Sesión expirada, redirigiendo al login")
                    navigateToLogin()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                _binding?.let { currentBinding ->
                    currentBinding.swipeRefreshLayout.isRefreshing = false
                    
                    when (state) {
                        is DailyMenuUiState.Loading -> {
                            if (!currentBinding.swipeRefreshLayout.isRefreshing) {
                                currentBinding.progressBar.visibility = View.VISIBLE
                            }
                            currentBinding.optionsContainer.visibility = View.GONE
                            currentBinding.optionsTitle.visibility = View.GONE
                            currentBinding.optionsHint.visibility = View.GONE
                            // Mostrar información genérica mientras carga
                            val today = Date()
                            currentBinding.dateTextView.text = formatDisplayDate(today)
                            currentBinding.dateTextView.visibility = View.VISIBLE
                            currentBinding.timeRangeTextView.text = getString(R.string.selection_time, "11:00")
                            currentBinding.timeRangeTextView.visibility = View.VISIBLE
                            currentBinding.statusContainer.visibility = View.GONE
                            checkNetworkStatus()
                        }
                        is DailyMenuUiState.Success -> {
                            currentBinding.progressBar.visibility = View.GONE
                            currentBinding.optionsContainer.visibility = View.VISIBLE
                            // El card siempre está visible, no se oculta
                            // currentBinding.offlineIndicator.visibility = View.GONE
                            
                            updateUI(state)
                        }
                        is DailyMenuUiState.Error -> {
                            currentBinding.progressBar.visibility = View.GONE
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
                                state.message.contains("horario", ignoreCase = true) ||
                                state.message.contains("cerrado", ignoreCase = true) ||
                                state.message.contains("eliminar el voto", ignoreCase = true) ||
                                state.message.contains("votar", ignoreCase = true) -> {
                                    // Error de horario - mostrar banner informativo sin reintentar
                                    showInfoBanner(state.message)
                                }
                                else -> {
                                    // Error real de carga (conexión o servidor) - estado inline con Reintentar,
                                    // como en el spec, en vez de un snackbar que se pierde.
                                    showLoadErrorState()
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
        binding.dateTextView.text = formatDisplayDate(menuDate)

        // Chip de estado: Abierto (verde suave) / Cerrado (neutral, nunca rojo)
        val isActuallyOpen = state.isActuallyOpen
        if (isActuallyOpen) {
            binding.statusTextView.text = getString(R.string.open)
            binding.statusTextView.setBackgroundResource(R.drawable.chip_open)
            binding.statusTextView.setTextColor(
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.chip_open_text)
            )
        } else {
            binding.statusTextView.text = getString(R.string.closed)
            binding.statusTextView.setBackgroundResource(R.drawable.chip_closed)
            binding.statusTextView.setTextColor(
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.chip_closed_text)
            )
        }

        // Subtítulo contextual según estado y elección
        val menuEndTime = formatMenuEndTime(menu)
        binding.timeRangeTextView.text = when {
            isActuallyOpen && state.userVote != null -> getString(R.string.selection_change_until, menuEndTime)
            isActuallyOpen -> getString(R.string.selection_time, menuEndTime)
            else -> getString(R.string.selection_closed_at, menuEndTime)
        }

        // El chip "Ya elegiste" del header queda oculto: la card de confirmación lo comunica
        binding.alreadySelectedTextView.visibility = View.GONE

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

    private fun showLoadErrorState() {
        binding.dateTextView.visibility = View.GONE
        binding.timeRangeTextView.visibility = View.GONE
        binding.statusContainer.visibility = View.GONE
        binding.optionsTitle.visibility = View.GONE
        binding.optionsHint.visibility = View.GONE

        binding.optionsContainer.removeAllViews()
        binding.optionsContainer.visibility = View.VISIBLE
        addEmptyState(
            icon = "!",
            title = getString(R.string.menu_load_error_title),
            subtitle = getString(R.string.menu_load_error_hint),
            isError = true,
            onRetry = { viewModel.loadMenuForDate(viewModel.getCurrentDate()) }
        )
    }

    private fun showNoMenuMessage() {
        val today = Date()
        binding.dateTextView.text = formatDisplayDate(today)
        binding.dateTextView.visibility = View.VISIBLE
        binding.timeRangeTextView.visibility = View.GONE
        binding.statusContainer.visibility = View.GONE
        binding.optionsTitle.visibility = View.GONE
        binding.optionsHint.visibility = View.GONE

        binding.optionsContainer.removeAllViews()
        binding.optionsContainer.visibility = View.VISIBLE
        addEmptyState(
            icon = "···",
            title = getString(R.string.no_menu_available),
            subtitle = getString(R.string.no_menu_available_hint)
        )
    }

    /**
     * Infla el empty state reutilizable dentro de optionsContainer.
     * [onRetry] agrega el botón Reintentar (para el estado de error).
     */
    private fun addEmptyState(
        icon: String,
        title: String,
        subtitle: String,
        isError: Boolean = false,
        onRetry: (() -> Unit)? = null
    ) {
        val emptyView = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_empty_state, binding.optionsContainer, false)
        val iconCircle = emptyView.findViewById<android.widget.FrameLayout>(R.id.emptyIconCircle)
        val iconText = emptyView.findViewById<android.widget.TextView>(R.id.emptyIconText)
        iconText.text = icon
        emptyView.findViewById<android.widget.TextView>(R.id.emptyTitle).text = title
        emptyView.findViewById<android.widget.TextView>(R.id.emptySubtitle).text = subtitle
        if (isError) {
            // mutate() antes de setTint(): empty_state_circle es un drawable compartido
            // por varias pantallas (Hoy, Semanal, Recordatorios); sin mutate() el tinte
            // se filtra a las demás.
            iconCircle.background.mutate().setTint(
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.chip_error_bg)
            )
            iconText.setTextColor(
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.md_error)
            )
        }
        val retryButton = emptyView.findViewById<com.google.android.material.button.MaterialButton>(R.id.emptyRetryButton)
        if (onRetry != null) {
            retryButton.visibility = View.VISIBLE
            retryButton.setOnClickListener { onRetry() }
        }
        binding.optionsContainer.addView(emptyView)
    }

    private fun confirmRemoveVote() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirm_remove_vote_title)
            .setMessage(R.string.confirm_remove_vote_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.confirm) { _, _ -> viewModel.deleteVote() }
            .show()
    }
    
    private fun displayMenuOptions(
        menu: com.cocido.morfipolo.domain.model.Menu,
        userVote: com.cocido.morfipolo.domain.model.Vote?,
        isWithinTime: Boolean,
        isMenuOpen: Boolean
    ) {
        binding.optionsContainer.removeAllViews()
        binding.optionsContainer.visibility = View.VISIBLE

        val options = menu.getOptionsOrEmpty()
        val canVote = isWithinTime && isMenuOpen
        val hasVoted = userVote != null

        // Título/hint de "Elegí tu menú" solo cuando hay lista para elegir
        val showChooseHeader = canVote && !hasVoted && options.isNotEmpty()
        binding.optionsTitle.visibility = if (showChooseHeader) View.VISIBLE else View.GONE
        binding.optionsHint.visibility = if (showChooseHeader) View.VISIBLE else View.GONE

        if (options.isEmpty()) {
            if (!menu.description.isNullOrBlank()) {
                // Menú sin opciones estructuradas: mostrar la descripción en una card simple
                val descriptionView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_menu_option, binding.optionsContainer, false)
                descriptionView.findViewById<android.widget.TextView>(R.id.optionNameTextView).text = menu.description
                descriptionView.findViewById<android.view.View>(R.id.selectedIndicator).visibility = View.GONE
                descriptionView.isClickable = false
                binding.optionsContainer.addView(descriptionView)
            } else {
                addEmptyState(
                    icon = "···",
                    title = getString(R.string.no_menu_available),
                    subtitle = getString(R.string.no_menu_available_hint)
                )
            }
            return
        }

        if (hasVoted) {
            // Card de confirmación "Tu elección de hoy" / "Tu menú de hoy"
            val confirmedView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_selection_confirmed, binding.optionsContainer, false)
            val card = confirmedView as com.google.android.material.card.MaterialCardView
            val label = confirmedView.findViewById<android.widget.TextView>(R.id.selectionLabel)
            val name = confirmedView.findViewById<android.widget.TextView>(R.id.selectionName)
            val hint = confirmedView.findViewById<android.widget.TextView>(R.id.selectionHint)
            val checkBadge = confirmedView.findViewById<android.widget.FrameLayout>(R.id.checkBadge)
            val removeAction = confirmedView.findViewById<android.widget.TextView>(R.id.removeSelectionAction)

            name.text = userVote!!.option.name

            val menuEndTime = formatMenuEndTime(menu)
            if (canVote) {
                label.setText(R.string.your_choice_today)
                hint.text = getString(R.string.selection_change_until, menuEndTime)
                removeAction.visibility = View.VISIBLE
                removeAction.setOnClickListener { confirmRemoveVote() }
            } else {
                // Variante cerrada: neutral beige, check apagado, sin acciones
                label.setText(R.string.your_menu_today)
                hint.text = getString(R.string.selection_closed_at, menuEndTime)
                card.setCardBackgroundColor(
                    androidx.core.content.ContextCompat.getColor(requireContext(), R.color.md_surface_container_high)
                )
                // mutate(): circle_check_badge es compartido con la variante abierta.
                checkBadge.background.mutate().setTint(
                    androidx.core.content.ContextCompat.getColor(requireContext(), R.color.md_text_muted)
                )
                removeAction.visibility = View.GONE
            }
            binding.optionsContainer.addView(confirmedView)

            if (canVote && options.size > 1) {
                // Sección "Otras opciones" para cambiar la elección
                val otherTitle = android.widget.TextView(requireContext()).apply {
                    text = getString(R.string.other_options)
                    textSize = 13f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(
                        androidx.core.content.ContextCompat.getColor(requireContext(), R.color.md_on_surface_variant)
                    )
                    setPadding(0, 0, 0, (8 * resources.displayMetrics.density).toInt())
                }
                binding.optionsContainer.addView(otherTitle)

                options.filter { it.id != userVote.option.id }.forEach { option ->
                    val optionView = LayoutInflater.from(requireContext())
                        .inflate(R.layout.item_menu_option, binding.optionsContainer, false)
                    optionView.findViewById<android.widget.TextView>(R.id.optionNameTextView).text = option.name
                    optionView.findViewById<android.view.View>(R.id.selectedIndicator).visibility = View.GONE
                    optionView.findViewById<android.widget.TextView>(R.id.optionActionTextView).visibility = View.VISIBLE
                    optionView.setOnClickListener { viewModel.selectOption(option.id) }
                    binding.optionsContainer.addView(optionView)
                }
            }
            return
        }

        if (canVote) {
            // Abierto y sin elegir: lista de opciones con radio, toda la card clickeable
            options.forEach { option ->
                val optionView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_menu_option, binding.optionsContainer, false)
                optionView.findViewById<android.widget.TextView>(R.id.optionNameTextView).text = option.name
                optionView.setOnClickListener { viewModel.selectOption(option.id) }
                binding.optionsContainer.addView(optionView)
            }
        } else {
            // Cerrado sin elección: empty state del spec
            addEmptyState(
                icon = "–",
                title = getString(R.string.no_choice_today),
                subtitle = getString(R.string.no_choice_today_hint, formatMenuEndTime(menu))
            )
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

