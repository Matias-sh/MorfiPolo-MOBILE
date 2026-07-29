package com.cocido.morfipolo.util

import com.cocido.morfipolo.domain.model.Menu
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Ventana de votación única (08:00-11:00, hora del dispositivo) y chequeo de
 * "es el menú de hoy". Antes vivía triplicada en DailyMenuViewModel,
 * WeeklyMenuAdapter y MenuWidgetProvider — consolidada acá para que un
 * cambio futuro (feriados, DST, horario de servidor) se haga en un solo lugar.
 */
object MenuTimeUtils {

    const val START_HOUR = 8
    const val START_MINUTE = 0
    const val END_HOUR = 11
    const val END_MINUTE = 0

    fun isWithinSelectionTime(menu: Menu): Boolean {
        if (menu.status != "open") return false
        return try {
            val now = Calendar.getInstance()
            val currentTimeInMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
            val startTimeInMinutes = START_HOUR * 60 + START_MINUTE
            val endTimeInMinutes = END_HOUR * 60 + END_MINUTE
            currentTimeInMinutes in startTimeInMinutes until endTimeInMinutes
        } catch (e: Exception) {
            false
        }
    }

    fun isMenuToday(menu: Menu): Boolean {
        return try {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val menuDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(menu.date)
            menuDate?.let {
                val menuCalendar = Calendar.getInstance().apply {
                    time = it
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                menuCalendar.timeInMillis == today.timeInMillis
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
}
