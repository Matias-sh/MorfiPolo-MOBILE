package com.cocido.morfipolo.ui.menu.weekly

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.cocido.morfipolo.MorfipoloApplication
import com.cocido.morfipolo.R
import com.cocido.morfipolo.databinding.FragmentWeeklyMenuBinding
import com.cocido.morfipolo.domain.model.Menu
import com.cocido.morfipolo.util.NetworkUtils
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class WeeklyMenuFragment : Fragment() {

    private var _binding: FragmentWeeklyMenuBinding? = null
    private val binding get() = _binding!!

    private var infoBannerHideJob: kotlinx.coroutines.Job? = null
    
    // BroadcastReceiver para escuchar actualizaciones del menú
    private val menuUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.cocido.morfipolo.MENU_UPDATED") {
                android.util.Log.d("WeeklyMenuFragment", "📱 Recibido broadcast de actualización de menú")
                // Recargar menús semanales
                viewModel.loadWeeklyMenus()
            }
        }
    }

    private val viewModel: WeeklyMenuViewModel by viewModels {
        val app = requireActivity().application as MorfipoloApplication
        WeeklyMenuViewModelFactory(
            app.menuRepository,
            app.authManager,
            app.voteRepository,
            app.sessionManager
        )
    }

    private lateinit var adapter: WeeklyMenuAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWeeklyMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = WeeklyMenuAdapter(
            onMenuClick = { item ->
                // Navegar al menú del día con la fecha seleccionada
                android.util.Log.d("WeeklyMenuFragment", "Navegando al menú del día: ${item.menu.date}")
                val bundle = Bundle().apply {
                    putString("menuDate", item.menu.date)
                }
                findNavController().navigate(R.id.action_weeklyMenuFragment_to_dailyMenuFragment, bundle)
            },
            onRemoveVote = { voteId, errorMessage ->
                // Eliminar voto y recargar menús
                lifecycleScope.launch {
                    try {
                        val app = requireActivity().application as MorfipoloApplication
                        val result = app.voteRepository.deleteVote(voteId)
                        if (result.isSuccess) {
                            android.util.Log.d("WeeklyMenuFragment", "Voto eliminado exitosamente")
                            viewModel.loadWeeklyMenus()
                        } else {
                            android.util.Log.e("WeeklyMenuFragment", "Error al eliminar voto")
                            val exception = result.exceptionOrNull()
                            val message = exception?.message ?: ""
                            
                            // Verificar si es un error de sesión expirada
                            if (exception is com.cocido.morfipolo.data.remote.SessionExpiredException ||
                                message.contains("sesión", ignoreCase = true) || 
                                message.contains("session", ignoreCase = true)) {
                                navigateToLogin()
                                return@launch
                            }
                            
                            // Verificar si es un error de horario cerrado
                            if (message.contains("cerrado", ignoreCase = true) || 
                                message.contains("horario", ignoreCase = true) || 
                                message.contains("time", ignoreCase = true) ||
                                errorMessage != null) {
                                val infoMessage = errorMessage ?: "El menú está cerrado. No puedes quitar votos fuera del horario de selección (08:00 - 11:00)."
                                showInfoBanner(infoMessage)
                            } else {
                                showInfoBanner("Error al eliminar voto")
                            }
                        }
                    } catch (e: com.cocido.morfipolo.data.remote.SessionExpiredException) {
                        android.util.Log.w("WeeklyMenuFragment", "Sesión expirada al eliminar voto")
                        navigateToLogin()
                    } catch (e: Exception) {
                        android.util.Log.e("WeeklyMenuFragment", "Error al eliminar voto", e)
                        val message = e.message ?: ""
                        if (message.contains("sesión", ignoreCase = true) || 
                            message.contains("session", ignoreCase = true)) {
                            navigateToLogin()
                        } else if (message.contains("cerrado", ignoreCase = true) || 
                            message.contains("horario", ignoreCase = true) || 
                            message.contains("time", ignoreCase = true)) {
                            showInfoBanner("El menú está cerrado. No puedes quitar votos fuera del horario de selección (08:00 - 11:00).")
                        } else {
                            showInfoBanner("Error al eliminar voto: ${e.message}")
                        }
                    }
                }
            },
            onSelectOption = { menuId, optionId, errorMessage ->
                // Seleccionar opción y recargar menús
                lifecycleScope.launch {
                    try {
                        val app = requireActivity().application as MorfipoloApplication
                        val userId = app.sessionManager.getCurrentUserId()
                        if (userId != null) {
                            val result = app.voteRepository.createVoteOrReplace(optionId, menuId, userId)
                            if (result.isSuccess) {
                                android.util.Log.d("WeeklyMenuFragment", "Opción seleccionada exitosamente")
                                viewModel.loadWeeklyMenus()
                            } else {
                                android.util.Log.e("WeeklyMenuFragment", "Error al seleccionar opción")
                                val exception = result.exceptionOrNull()
                                val message = exception?.message ?: ""
                                
                                // Verificar si es un error de sesión expirada
                                if (exception is com.cocido.morfipolo.data.remote.SessionExpiredException ||
                                    message.contains("sesión", ignoreCase = true) || 
                                    message.contains("session", ignoreCase = true)) {
                                    navigateToLogin()
                                    return@launch
                                }
                                
                                // Verificar si es un error de horario cerrado
                                if (message.contains("cerrado", ignoreCase = true) || 
                                    message.contains("horario", ignoreCase = true) || 
                                    message.contains("time", ignoreCase = true) ||
                                    errorMessage != null) {
                                    val infoMessage = errorMessage ?: "El menú está cerrado. No puedes agregar votos fuera del horario de selección (08:00 - 11:00)."
                                    showInfoBanner(infoMessage)
                                } else {
                                    showInfoBanner("Error al seleccionar opción")
                                }
                            }
                        }
                    } catch (e: com.cocido.morfipolo.data.remote.SessionExpiredException) {
                        android.util.Log.w("WeeklyMenuFragment", "Sesión expirada al seleccionar opción")
                        navigateToLogin()
                    } catch (e: Exception) {
                        android.util.Log.e("WeeklyMenuFragment", "Error al seleccionar opción", e)
                        val message = e.message ?: ""
                        if (message.contains("sesión", ignoreCase = true) || 
                            message.contains("session", ignoreCase = true)) {
                            navigateToLogin()
                        } else if (message.contains("cerrado", ignoreCase = true) || 
                            message.contains("horario", ignoreCase = true) || 
                            message.contains("time", ignoreCase = true)) {
                            showInfoBanner("El menú está cerrado. No puedes agregar votos fuera del horario de selección (08:00 - 11:00).")
                        } else {
                            showInfoBanner("Error al seleccionar opción: ${e.message}")
                        }
                    }
                }
            }
        )
        binding.menusRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.menusRecyclerView.adapter = adapter

        setupPullToRefresh()
        setupObservers()
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
        
        viewModel.loadWeeklyMenus()
    }
    
    private fun setupPullToRefresh() {
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.nonna_brown_primary,
            R.color.nonna_accent_warm,
            R.color.nonna_success
        )
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadWeeklyMenus()
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
            // Observar cuando la sesión expira
            viewModel.sessionExpired.collect { expired ->
                if (expired) {
                    android.util.Log.w("WeeklyMenuFragment", "Sesión expirada, redirigiendo al login")
                    navigateToLogin()
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.swipeRefreshLayout.isRefreshing = false
                
                when (state) {
                    is WeeklyMenuUiState.Loading -> {
                        if (!binding.swipeRefreshLayout.isRefreshing) {
                            binding.progressBar.visibility = View.VISIBLE
                        }
                        binding.menusRecyclerView.visibility = View.GONE
                        binding.emptyStateLayout.visibility = View.GONE
                        android.util.Log.d("WeeklyMenuFragment", "Cargando menús...")
                        checkNetworkStatus()
                    }
                    is WeeklyMenuUiState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.offlineIndicator.visibility = View.GONE
                        
                        if (state.menus.isEmpty()) {
                            binding.menusRecyclerView.visibility = View.GONE
                            binding.emptyStateLayout.visibility = View.VISIBLE
                        } else {
                            binding.menusRecyclerView.visibility = View.VISIBLE
                            binding.emptyStateLayout.visibility = View.GONE
                            android.util.Log.d("WeeklyMenuFragment", "Menús cargados exitosamente: ${state.menus.size}")
                            adapter.submitList(state.menus) {
                                android.util.Log.d("WeeklyMenuFragment", "Adapter actualizado con ${state.menus.size} menús")
                            }
                        }
                    }
                    is WeeklyMenuUiState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.menusRecyclerView.visibility = View.GONE
                        binding.emptyStateLayout.visibility = View.GONE
                        checkNetworkStatus()
                        
                        android.util.Log.e("WeeklyMenuFragment", "Error al cargar menús: ${state.message}")
                        
                        val errorMessage = when {
                            !NetworkUtils.isNetworkAvailable(requireContext()) -> getString(R.string.error_no_connection)
                            state.message.contains("sesión", ignoreCase = true) || state.message.contains("session", ignoreCase = true) -> {
                                // Si es error de sesión expirada, redirigir al login
                                navigateToLogin()
                                return@collect
                            }
                            else -> state.message
                        }
                        
                        showErrorWithRetry(errorMessage) {
                            viewModel.loadWeeklyMenus()
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

    private fun showInfoBanner(message: String) {
        // Cancelar trabajo anterior si existe
        infoBannerHideJob?.cancel()
        
        binding.infoBannerText.text = message
        binding.infoBannerIcon.setImageResource(android.R.drawable.ic_dialog_info)
        binding.infoBanner.setCardBackgroundColor(resources.getColor(R.color.nonna_accent_warm, null))
        
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

class WeeklyMenuViewModelFactory(
    private val menuRepository: com.cocido.morfipolo.data.repository.MenuRepository,
    private val authManager: com.cocido.morfipolo.data.remote.AuthManager,
    private val voteRepository: com.cocido.morfipolo.data.repository.VoteRepository,
    private val sessionManager: com.cocido.morfipolo.data.local.preferences.SessionManager
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WeeklyMenuViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WeeklyMenuViewModel(menuRepository, authManager, voteRepository, sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}






