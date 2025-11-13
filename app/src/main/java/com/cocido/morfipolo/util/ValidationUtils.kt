package com.cocido.morfipolo.util

object ValidationUtils {
    /**
     * Valida que el DNI tenga formato válido (7-8 dígitos numéricos)
     */
    fun isValidDni(dni: String): Boolean {
        val dniClean = dni.trim()
        return dniClean.length in 7..8 && dniClean.all { it.isDigit() }
    }
    
    /**
     * Valida formato de contraseña (mayúscula, minúscula, número, mínimo 8 caracteres)
     */
    fun isValidPassword(password: String): Boolean {
        val passwordPattern = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$")
        return passwordPattern.matches(password)
    }
}

