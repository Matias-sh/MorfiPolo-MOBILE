package com.cocido.morfipolo.ui.menu.weekly

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
            onRemoveVote = { voteId ->
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
                            android.widget.Toast.makeText(
                                requireContext(),
                                "Error al eliminar voto",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("WeeklyMenuFragment", "Error al eliminar voto", e)
                        android.widget.Toast.makeText(
                            requireContext(),
                            "Error al eliminar voto: ${e.message}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
        binding.menusRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.menusRecyclerView.adapter = adapter

        setupPullToRefresh()
        setupObservers()
        checkNetworkStatus()
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
                            state.message.contains("sesión", ignoreCase = true) || state.message.contains("session", ignoreCase = true) -> getString(R.string.error_session_expired)
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

    override fun onDestroyView() {
        super.onDestroyView()
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






