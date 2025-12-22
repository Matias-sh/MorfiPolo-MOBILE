package com.cocido.morfipolo.domain.model

import java.io.Serializable

/**
 * Modelo para representar una notificación configurable por el usuario.
 * Similar a las alarmas del reloj de Android.
 */
data class CustomNotification(
    val id: String, // ID único para identificar la notificación
    val hour: Int, // Hora (0-23)
    val minute: Int, // Minuto (0-59)
    val isEnabled: Boolean, // Si está activada o no
    val daysOfWeek: Set<Int> = setOf(1, 2, 3, 4, 5) // Días de la semana (1=Lunes, 7=Domingo). Por defecto lunes a viernes
) : Serializable {
    
    companion object {
        const val MONDAY = 1
        const val TUESDAY = 2
        const val WEDNESDAY = 3
        const val THURSDAY = 4
        const val FRIDAY = 5
        const val SATURDAY = 6
        const val SUNDAY = 7
        
        /**
         * Genera un ID único basado en la hora y minuto.
         */
        fun generateId(hour: Int, minute: Int): String {
            return "notification_${hour}_${minute}"
        }
        
        /**
         * Obtiene el nombre del día de la semana.
         */
        fun getDayName(dayOfWeek: Int): String {
            return when (dayOfWeek) {
                MONDAY -> "Lun"
                TUESDAY -> "Mar"
                WEDNESDAY -> "Mié"
                THURSDAY -> "Jue"
                FRIDAY -> "Vie"
                SATURDAY -> "Sáb"
                SUNDAY -> "Dom"
                else -> ""
            }
        }
        
        /**
         * Obtiene el nombre completo del día de la semana.
         */
        fun getDayNameFull(dayOfWeek: Int): String {
            return when (dayOfWeek) {
                MONDAY -> "Lunes"
                TUESDAY -> "Martes"
                WEDNESDAY -> "Miércoles"
                THURSDAY -> "Jueves"
                FRIDAY -> "Viernes"
                SATURDAY -> "Sábado"
                SUNDAY -> "Domingo"
                else -> ""
            }
        }
    }
    
    /**
     * Formatea la hora en formato 12 horas (ej: "9:00 AM" o "2:30 PM").
     */
    fun getFormattedTime(): String {
        val period = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format("%d:%02d %s", displayHour, minute, period)
    }
    
    /**
     * Formatea la hora en formato 24 horas (ej: "09:00" o "14:30").
     */
    fun getFormattedTime24(): String {
        return String.format("%02d:%02d", hour, minute)
    }
    
    /**
     * Obtiene los días de la semana formateados (ej: "Lun, Mar, Mié").
     */
    fun getFormattedDays(): String {
        if (daysOfWeek.isEmpty()) return "Nunca"
        if (daysOfWeek.size == 7) return "Todos los días"
        if (daysOfWeek == setOf(1, 2, 3, 4, 5)) return "Lun - Vie"
        if (daysOfWeek == setOf(6, 7)) return "Sáb - Dom"
        
        return daysOfWeek.sorted().joinToString(", ") { getDayName(it) }
    }
    
    /**
     * Verifica si la notificación está programada para un día específico.
     */
    fun isScheduledForDay(dayOfWeek: Int): Boolean {
        return daysOfWeek.contains(dayOfWeek)
    }
}
