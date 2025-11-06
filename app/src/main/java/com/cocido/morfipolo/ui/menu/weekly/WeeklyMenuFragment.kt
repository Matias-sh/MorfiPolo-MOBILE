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
        WeeklyMenuViewModelFactory(
            (requireActivity().application as MorfipoloApplication).menuRepository
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

        adapter = WeeklyMenuAdapter()
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
                    }
                    is WeeklyMenuUiState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.menusRecyclerView.visibility = View.VISIBLE
                        adapter.submitList(state.menus)
                    }
                    is WeeklyMenuUiState.Error -> {
                        binding.progressBar.visibility = View.GONE
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
    private val menuRepository: com.cocido.morfipolo.data.repository.MenuRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WeeklyMenuViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WeeklyMenuViewModel(menuRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


