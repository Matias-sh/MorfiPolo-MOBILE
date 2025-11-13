package com.cocido.morfipolo.ui.profile

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.core.widget.doOnTextChanged
import com.google.android.material.textfield.TextInputLayout
import com.cocido.morfipolo.MorfipoloApplication
import com.cocido.morfipolo.R
import com.cocido.morfipolo.databinding.FragmentProfileBinding
import com.cocido.morfipolo.ui.login.LoginActivity
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels {
        val app = requireActivity().application as MorfipoloApplication
        ProfileViewModelFactory(
            app.userRepository,
            app.authManager
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupListeners()
        viewModel.loadUser()
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is ProfileUiState.Loading -> {
                        // Show loading if needed
                    }
                    is ProfileUiState.Success -> {
                        val fullName = "${state.user.name} ${state.user.lastName}"
                        binding.nameTextView.text = fullName
                        binding.avatarTextView.text = state.user.name.firstOrNull()?.toString() ?: "U"
                    }
                    is ProfileUiState.Error -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.passwordChangeState.collect { state ->
                when (state) {
                    is PasswordChangeState.Idle -> {}
                    is PasswordChangeState.Success -> {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.password_changed_success),
                            Toast.LENGTH_SHORT
                        ).show()
                        viewModel.resetPasswordChangeState()
                    }
                    is PasswordChangeState.Error -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetPasswordChangeState()
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.changePasswordButton.setOnClickListener {
            showChangePasswordDialog()
        }

        binding.logoutButton.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_change_password, null)

        val currentPasswordLayout = dialogView.findViewById<TextInputLayout>(R.id.currentPasswordLayout)
        val newPasswordLayout = dialogView.findViewById<TextInputLayout>(R.id.newPasswordLayout)
        val confirmPasswordLayout = dialogView.findViewById<TextInputLayout>(R.id.confirmPasswordLayout)

        val currentPasswordEditText = currentPasswordLayout.editText
        val newPasswordEditText = newPasswordLayout.editText
        val confirmPasswordEditText = confirmPasswordLayout.editText

        newPasswordEditText?.doOnTextChanged { text, _, _, _ ->
            if (newPasswordLayout.error != null) {
                val isValid = isPasswordValid(text?.toString().orEmpty())
                newPasswordLayout.error = if (isValid) null else getString(R.string.password_requirements_error)
            }
        }

        confirmPasswordEditText?.doOnTextChanged { _, _, _, _ ->
            confirmPasswordLayout.error = null
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.change_password))
            .setView(dialogView)
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.save), null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                currentPasswordLayout.error = null
                newPasswordLayout.error = null
                confirmPasswordLayout.error = null

                val currentPassword = currentPasswordEditText?.text?.toString().orEmpty()
                val newPassword = newPasswordEditText?.text?.toString().orEmpty()
                val confirmPassword = confirmPasswordEditText?.text?.toString().orEmpty()

                var isValid = true

                if (currentPassword.isBlank()) {
                    currentPasswordLayout.error = getString(R.string.password_current_required)
                    isValid = false
                }

                if (!isPasswordValid(newPassword)) {
                    newPasswordLayout.error = getString(R.string.password_requirements_error)
                    isValid = false
                }

                if (newPassword != confirmPassword) {
                    confirmPasswordLayout.error = getString(R.string.passwords_not_match)
                    isValid = false
                }

                if (isValid) {
                    viewModel.changePassword(currentPassword, newPassword)
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.logout))
            .setMessage("¿Estás seguro que deseas cerrar sesión?")
            .setPositiveButton(getString(R.string.logout)) { _, _ ->
                viewModel.logout()
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun isPasswordValid(password: String): Boolean {
        val passwordPattern = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$")
        return passwordPattern.matches(password)
    }
}

class ProfileViewModelFactory(
    private val userRepository: com.cocido.morfipolo.data.repository.UserRepository,
    private val authManager: com.cocido.morfipolo.data.remote.AuthManager
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(userRepository, authManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}




