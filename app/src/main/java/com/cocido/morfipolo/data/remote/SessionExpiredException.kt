package com.cocido.morfipolo.data.remote

/**
 * Excepción lanzada cuando la sesión del usuario ha expirado
 * y necesita iniciar sesión nuevamente.
 */
class SessionExpiredException(message: String = "Sesión expirada. Por favor, inicia sesión nuevamente.") : Exception(message)

