package com.cocido.morfipolo.ui.menu.weekly

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cocido.morfipolo.MorfipoloApplication
import com.cocido.morfipolo.R
import com.cocido.morfipolo.databinding.FragmentWeeklyMenuBinding
import com.cocido.morfipolo.domain.model.Menu
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
                // Cuando se hace clic en un menú, navegar al menú del día con esa fecha
                android.util.Log.d("WeeklyMenuFragment", "Menú clickeado: ${item.menu.date}")
                // Por ahora solo logueamos, pero podrías navegar al DailyMenuFragment con esta fecha
                // TODO: Implementar navegación al menú del día seleccionado
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

        setupObservers()
        viewModel.loadWeeklyMenus()
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is WeeklyMenuUiState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.menusRecyclerView.visibility = View.GONE
                        android.util.Log.d("WeeklyMenuFragment", "Cargando menús...")
                    }
                    is WeeklyMenuUiState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.menusRecyclerView.visibility = View.VISIBLE
                        android.util.Log.d("WeeklyMenuFragment", "Menús cargados exitosamente: ${state.menus.size}")
                        adapter.submitList(state.menus) {
                            android.util.Log.d("WeeklyMenuFragment", "Adapter actualizado con ${state.menus.size} menús")
                        }
                    }
                    is WeeklyMenuUiState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.menusRecyclerView.visibility = View.GONE
                        android.util.Log.e("WeeklyMenuFragment", "Error al cargar menús: ${state.message}")
                        // Mostrar mensaje de error al usuario
                        android.widget.Toast.makeText(
                            requireContext(),
                            state.message,
                            android.widget.Toast.LENGTH_LONG
                        ).show()
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






