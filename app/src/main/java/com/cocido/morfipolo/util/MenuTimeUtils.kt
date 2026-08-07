package com.cocido.morfipolo.util

import com.cocido.morfipolo.domain.model.Menu
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Ventana de votación real de un menú.
 *
 * Antes esto era un rango fijo (08:00-11:00 hora del dispositivo) igual para
 * todos los menús. El backend cambió: ahora cada menú define su propia
 * ventana vía start_time/end_time (instantes ISO-8601 en UTC) y los
 * cocineros la abren cuando quieren — en la práctica, desde ~21:00 del día
 * anterior hasta las 11:00 del día del menú. Un rango fijo en hora local ya
 * no alcanza: hay que comparar directamente contra esos instantes.
 *
 * El campo status del menú se probó no confiable como señal de "todavía
 * abierto": el backend lo deja en "open" varias horas después de que
 * end_time ya pasó (lo cierra un job en otro horario), así que NO se exige
 * status == "open" acá. Sí se respeta un status == "closed" explícito como
 * cierre anticipado, por si un admin lo fuerza antes de tiempo.
 */
object MenuTimeUtils {

    // Decisión de producto explícita: la selección cierra a las 11:00 (hora
    // del dispositivo) como máximo, sin importar lo que diga end_time del
    // backend. Si end_time es más temprano que las 11:00 ese día, se respeta
    // igual (cierre anticipado real); si es más tardío, se recorta a las 11:00.
    private const val HARD_CLOSE_HOUR = 11
    private const val HARD_CLOSE_MINUTE = 0

    private val isoFormats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'"
    ).map { pattern ->
        SimpleDateFormat(pattern, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
    }

    private fun parseInstant(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        for (format in isoFormats) {
            try {
                return format.parse(raw)?.time
            } catch (e: Exception) {
                // probar el siguiente formato
            }
        }
        return null
    }

    /**
     * end_time real del menú, recortado a las 11:00 (hora del dispositivo)
     * del mismo día calendario si el backend manda algo más tardío.
     */
    private fun effectiveEndMillis(rawEndMillis: Long): Long {
        val elevenAmSameDay = Calendar.getInstance().apply {
            timeInMillis = rawEndMillis
            set(Calendar.HOUR_OF_DAY, HARD_CLOSE_HOUR)
            set(Calendar.MINUTE, HARD_CLOSE_MINUTE)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return minOf(rawEndMillis, elevenAmSameDay)
    }

    /**
     * Instante real (ya recortado a las 11:00 como máximo) en que cierra la
     * selección de [menu], o null si end_time no se pudo parsear.
     */
    fun getEffectiveEndTimeMillis(menu: Menu): Long? {
        val rawEnd = parseInstant(menu.end_time) ?: return null
        return effectiveEndMillis(rawEnd)
    }

    /**
     * true si el instante actual cae dentro de [start_time, end_time_efectivo) del menú.
     */
    fun isWithinSelectionTime(menu: Menu): Boolean {
        if (menu.status == "draft" || menu.status == "closed") return false
        val start = parseInstant(menu.start_time) ?: return false
        val end = getEffectiveEndTimeMillis(menu) ?: return false
        val now = System.currentTimeMillis()
        return now in start until end
    }

    /**
     * true si [menu.date] es el día calendario de hoy (hora del dispositivo).
     * Uso puramente visual (badge "HOY" en Semanal, etc.) — ya no se usa para
     * decidir si se puede votar, porque el menú realmente abierto ahora mismo
     * puede tener fecha de mañana (ventana nocturna).
     */
    fun isMenuToday(menu: Menu): Boolean = isMenuOffsetFromToday(menu, 0)

    /**
     * true si [menu.date] es el día calendario de mañana. El menú realmente
     * abierto para votar durante la ventana nocturna (~21:00 en adelante)
     * tiene esta fecha, no la de hoy.
     */
    fun isMenuTomorrow(menu: Menu): Boolean = isMenuOffsetFromToday(menu, 1)

    private fun isMenuOffsetFromToday(menu: Menu, dayOffset: Int): Boolean {
        return try {
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_MONTH, dayOffset)
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
                menuCalendar.timeInMillis == target.timeInMillis
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
}
