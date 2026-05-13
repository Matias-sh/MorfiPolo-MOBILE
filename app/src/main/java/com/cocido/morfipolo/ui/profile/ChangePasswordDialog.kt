package com.cocido.morfipolo.ui.profile

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.cocido.morfipolo.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class ChangePasswordDialog(
    private val onConfirm: (current: String, new: String, confirm: String) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_change_password, null)

        val currentPasswordLayout = dialogView.findViewById<TextInputLayout>(R.id.currentPasswordLayout)
        val newPasswordLayout = dialogView.findViewById<TextInputLayout>(R.id.newPasswordLayout)
        val confirmPasswordLayout = dialogView.findViewById<TextInputLayout>(R.id.confirmPasswordLayout)
        val currentPasswordEditText = dialogView.findViewById<TextInputEditText>(R.id.currentPasswordEditText)
        val newPasswordEditText = dialogView.findViewById<TextInputEditText>(R.id.newPasswordEditText)
        val confirmPasswordEditText = dialogView.findViewById<TextInputEditText>(R.id.confirmPasswordEditText)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.change_password))
            .setView(dialogView)
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.save), null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                currentPasswordLayout.error = null
                newPasswordLayout.error = null
                confirmPasswordLayout.error = null

                val currentPassword = currentPasswordEditText.text?.toString().orEmpty()
                val newPassword = newPasswordEditText.text?.toString().orEmpty()
                val confirmPassword = confirmPasswordEditText.text?.toString().orEmpty()

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
                    onConfirm(currentPassword, newPassword, confirmPassword)
                    dialog.dismiss()
                }
            }
        }

        return dialog
    }

    private fun isPasswordValid(password: String): Boolean {
        val passwordPattern = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$")
        return passwordPattern.matches(password)
    }
}
