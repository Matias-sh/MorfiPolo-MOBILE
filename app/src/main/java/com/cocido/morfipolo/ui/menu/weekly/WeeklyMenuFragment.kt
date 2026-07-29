package com.cocido.morfipolo.ui.menu.weekly

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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.cocido.morfipolo.MorfipoloApplication
import com.cocido.morfipolo.R
import com.cocido.morfipolo.databinding.FragmentWeeklyMenuBinding
import com.cocido.morfipolo.util.NetworkUtils
import kotlinx.coroutines.launch

class WeeklyMenuFragment : Fragment() {

    private var _binding: FragmentWeeklyMenuBinding? = null
    private val binding get() = _binding!!

    private var hasLoadedOnce = false

    // BroadcastReceiver para escuchar actualizaciones del menú
    private val menuUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.cocido.morfipolo.MENU_UPDATED") {
                android.util.Log.d("WeeklyMenuFragment", "📱 Recibido broadcast de actualización de menú")
                viewModel.loadWeeklyMenus(forceReload = true)
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

        // Insets: correr el header debajo de la barra de estado
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

        ViewCompat.setOnApplyWindowInsetsListener(binding.menusRecyclerView) { v, insets ->
            val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val bottomNavHeightDp = 80f
            val bottomNavHeightPx = (bottomNavHeightDp * resources.displayMetrics.density).toInt()
            val totalBottomPadding = navigationBars.bottom + bottomNavHeightPx + (16 * resources.displayMetrics.density).toInt()
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, totalBottomPadding)
            insets
        }

        // Elegir/cambiar elección siempre navega a "Hoy": es la única pantalla que
        // vota, evitando el picker duplicado que tenía acá el diseño anterior.
        adapter = WeeklyMenuAdapter(
            onChangeSelection = { item ->
                val bundle = Bundle().apply { putString("menuDate", item.menu.date) }
                findNavController().navigate(R.id.action_weeklyMenuFragment_to_dailyMenuFragment, bundle)
            }
        )
        binding.menusRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.menusRecyclerView.adapter = adapter

        setupPullToRefresh()
        setupObservers()
        checkNetworkStatus()

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
        hasLoadedOnce = true
    }

    override fun onResume() {
        super.onResume()
        // El usuario puede haber votado en "Hoy" y vuelto acá por atrás: el
        // caché de 5 min de este ViewModel podría mostrar el estado viejo.
        if (hasLoadedOnce) {
            viewModel.loadWeeklyMenus(forceReload = true)
        }
    }

    private fun setupPullToRefresh() {
        binding.swipeRefreshLayout.setColorSchemeResources(R.color.md_primary)
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadWeeklyMenus(forceReload = true)
        }
    }

    private fun checkNetworkStatus() {
        // Sin banner dedicado en esta pantalla: los errores de red van al
        // estado de error inline (showLoadErrorState).
        NetworkUtils.isNetworkAvailable(requireContext())
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sessionExpired.collect { expired ->
                if (expired) {
                    android.util.Log.w("WeeklyMenuFragment", "Sesión expirada, redirigiendo al login")
                    navigateToLogin()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                _binding?.let { currentBinding ->
                    currentBinding.swipeRefreshLayout.isRefreshing = false

                    when (state) {
                        is WeeklyMenuUiState.Loading -> {
                            if (!currentBinding.swipeRefreshLayout.isRefreshing) {
                                currentBinding.progressBar.visibility = View.VISIBLE
                            }
                            currentBinding.menusRecyclerView.visibility = View.GONE
                            currentBinding.emptyStateLayout.visibility = View.GONE
                        }
                        is WeeklyMenuUiState.Success -> {
                            currentBinding.progressBar.visibility = View.GONE

                            if (state.menus.isEmpty()) {
                                currentBinding.menusRecyclerView.visibility = View.GONE
                                showEmptyState(
                                    icon = "···",
                                    title = getString(R.string.empty_state_title),
                                    subtitle = getString(R.string.empty_state_message)
                                )
                            } else {
                                currentBinding.menusRecyclerView.visibility = View.VISIBLE
                                currentBinding.emptyStateLayout.visibility = View.GONE
                                adapter.submitList(state.menus)
                            }
                        }
                        is WeeklyMenuUiState.Error -> {
                            currentBinding.progressBar.visibility = View.GONE
                            currentBinding.menusRecyclerView.visibility = View.GONE

                            when {
                                state.message.contains("sesión", ignoreCase = true) ||
                                state.message.contains("session", ignoreCase = true) -> {
                                    navigateToLogin()
                                    return@collect
                                }
                                else -> showLoadErrorState()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showEmptyState(icon: String, title: String, subtitle: String, onRetry: (() -> Unit)? = null) {
        binding.emptyStateLayout.visibility = View.VISIBLE
        binding.emptyIconText.text = icon
        binding.emptyTitle.text = title
        binding.emptySubtitle.text = subtitle
        if (onRetry != null) {
            binding.emptyRetryButton.visibility = View.VISIBLE
            binding.emptyRetryButton.setOnClickListener { onRetry() }
        } else {
            binding.emptyRetryButton.visibility = View.GONE
        }
    }

    private fun showLoadErrorState() {
        showEmptyState(
            icon = "!",
            title = getString(R.string.menu_load_error_title),
            subtitle = getString(R.string.menu_load_error_hint),
            onRetry = { viewModel.loadWeeklyMenus(forceReload = true) }
        )
    }

    private fun navigateToLogin() {
        val intent = android.content.Intent(requireContext(), com.cocido.morfipolo.ui.login.LoginActivity::class.java)
        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            requireContext().unregisterReceiver(menuUpdateReceiver)
        } catch (e: Exception) {
            // El receiver puede no estar registrado, ignorar
        }
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
