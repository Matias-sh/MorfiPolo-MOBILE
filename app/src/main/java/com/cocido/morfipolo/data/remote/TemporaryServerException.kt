package com.cocido.morfipolo.data.remote

/**
 * Excepción para errores temporales del servidor.
 * Se usa cuando el servidor devuelve errores 5xx que no deberían
 * causar cierre de sesión ni redirección al login.
 */
class TemporaryServerException(message: String) : Exception(message)




