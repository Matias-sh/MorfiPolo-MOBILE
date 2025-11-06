package com.cocido.morfipolo.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cocido.morfipolo.MorfipoloApplication
import com.cocido.morfipolo.R
import com.cocido.morfipolo.databinding.ActivityLoginBinding
import com.cocido.morfipolo.ui.main.MainActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels {
        LoginViewModelFactory((application as MorfipoloApplication).userRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupListeners()

        // Verificar si ya está logueado
        if ((application as MorfipoloApplication).userRepository.isLoggedIn()) {
            navigateToMain()
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is LoginUiState.Idle -> {
                        binding.progressBar.visibility = android.view.View.GONE
                        binding.loginButton.isEnabled = true
                    }
                    is LoginUiState.Loading -> {
                        binding.progressBar.visibility = android.view.View.VISIBLE
                        binding.loginButton.isEnabled = false
                    }
                    is LoginUiState.Success -> {
                        binding.progressBar.visibility = android.view.View.GONE
                        binding.loginButton.isEnabled = true
                        navigateToMain()
                    }
                    is LoginUiState.Error -> {
                        binding.progressBar.visibility = android.view.View.GONE
                        binding.loginButton.isEnabled = true
                        Toast.makeText(this@LoginActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.loginButton.setOnClickListener {
            val dni = binding.dniEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            viewModel.login(dni, password)
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

class LoginViewModelFactory(private val userRepository: com.cocido.morfipolo.data.repository.UserRepository) :
    androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


