package com.cocido.morfipolo.ui.menu.weekly

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cocido.morfipolo.R
import com.cocido.morfipolo.databinding.ItemWeeklyMenuBinding
import com.cocido.morfipolo.domain.model.Menu
import com.cocido.morfipolo.ui.menu.weekly.WeeklyMenuItem
import java.text.SimpleDateFormat
import java.util.*

class WeeklyMenuAdapter(
    private val onMenuClick: (WeeklyMenuItem) -> Unit = {},
    private val onRemoveVote: (String, String, String?) -> Unit = { _, _, _ -> }, // voteId, menuId, errorMessage
    private val onSelectOption: (String, String, String?) -> Unit = { _, _, _ -> } // menuId, optionId, errorMessage
) : ListAdapter<WeeklyMenuItem, WeeklyMenuAdapter.MenuViewHolder>(MenuDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = ItemWeeklyMenuBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MenuViewHolder(binding, onMenuClick, onRemoveVote, onSelectOption)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MenuViewHolder(
        private val binding: ItemWeeklyMenuBinding,
        private val onMenuClick: (WeeklyMenuItem) -> Unit,
        private val onRemoveVote: (String, String, String?) -> Unit,
        private val onSelectOption: (String, String, String?) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        
        private fun isWithinSelectionTime(menu: Menu): Boolean {
            if (menu.status != "open") return false
            
            return try {
                // Horario fijo: 08:00 - 11:00
                val now = Calendar.getInstance()
                val currentHour = now.get(Calendar.HOUR_OF_DAY)
                val currentMinute = now.get(Calendar.MINUTE)

                val startHour = 8
                val startMin = 0
                val endHour = 11
                val endMin = 0

                val currentTimeInMinutes = currentHour * 60 + currentMinute
                val startTimeInMinutes = startHour * 60 + startMin
                val endTimeInMinutes = endHour * 60 + endMin

                currentTimeInMinutes >= startTimeInMinutes && currentTimeInMinutes < endTimeInMinutes
            } catch (e: Exception) {
                false
            }
        }
        
        private fun isMenuToday(menu: Menu): Boolean {
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

        fun bind(item: WeeklyMenuItem) {
            val menu = item.menu
            val userVote = item.userVote
            
            // Convertir date string a Date
            val menuDate = try {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(menu.date) ?: Date()
            } catch (e: Exception) {
                Date()
            }
            
            // Configurar fecha
            binding.dateTextView.text = dateFormat.format(menuDate)
            
            // En vista semanal no mostramos el texto "MENÚ DEL DÍA"
            binding.menuDescriptionTextView.visibility = View.GONE
            
            // Configurar estado - validar si realmente está abierto según el horario (08:00 - 11:00) y si es el menú de hoy
            val isToday = isMenuToday(menu)
            val isWithinTime = isWithinSelectionTime(menu)
            val isActuallyOpen = menu.status == "open" && isWithinTime && isToday
            val statusText = when {
                isActuallyOpen -> binding.root.context.getString(R.string.open)
                menu.status == "closed" -> binding.root.context.getString(R.string.closed)
                else -> binding.root.context.getString(R.string.closed) // Si pasó el horario o no es hoy, mostrar cerrado
            }
            binding.statusTextView.text = statusText
            binding.statusTextView.setBackgroundResource(
                if (isActuallyOpen) {
                    R.drawable.badge_success_modern
                } else {
                    R.drawable.badge_error_modern
                }
            )
            
            // Mostrar "Ya elegiste" si hay voto
            binding.alreadySelectedTextView.visibility = if (userVote != null) {
                View.VISIBLE
            } else {
                View.GONE
            }
            
            // Mostrar opciones del menú con botones de selección
            val options = menu.getOptionsOrEmpty()
            val optionsContainer = binding.root.findViewById<android.widget.LinearLayout>(R.id.optionsContainer)
            
            if (options.isNotEmpty()) {
                // Limpiar opciones anteriores
                optionsContainer?.removeAllViews()
                optionsContainer?.visibility = View.VISIBLE
                
                // Agregar opciones con botones
                options.forEachIndexed { index, option ->
                    val optionView = LayoutInflater.from(binding.root.context)
                        .inflate(R.layout.item_menu_option, optionsContainer, false)
                    
                    val optionNameTextView = optionView.findViewById<TextView>(R.id.optionNameTextView)
                    val optionButton = optionView.findViewById<com.google.android.material.button.MaterialButton>(R.id.optionButton)
                    val statusMessageTextView = optionView.findViewById<TextView>(R.id.statusMessageTextView)
                    
                    // Nombre de la opción (normalizado para mejorar legibilidad)
                    val normalizedOptionName = option.name
                        .replace(Regex("\\s*,\\s*"), ", ")
                        .replace(Regex("\\s+"), " ")
                        .trim()
                    optionNameTextView.text = if (options.size > 1) {
                        "Opción ${index + 1} · $normalizedOptionName"
                    } else {
                        normalizedOptionName
                    }
                    
                    // Verificar si esta opción está seleccionada
                    val isSelected = userVote?.option?.id == option.id
                    val hasVotedAny = userVote != null
                    
                    // Lógica de visualización (igual que DailyMenuFragment)
                    if (!isActuallyOpen) {
                        // CERRADO / FUERA DE HORARIO
                        optionButton.visibility = View.GONE
                        statusMessageTextView.visibility = View.VISIBLE
                        
                        if (isSelected) {
                            // Usuario votó ESTA opción
                            statusMessageTextView.text = "Usted ya seleccionó esta opción"
                            statusMessageTextView.setTextColor(androidx.core.content.ContextCompat.getColor(binding.root.context, R.color.food_secondary_green))
                            statusMessageTextView.setTypeface(null, android.graphics.Typeface.BOLD)
                        } else if (!hasVotedAny) {
                            // Usuario NO votó nada hoy
                            statusMessageTextView.text = "Usted no seleccionó una opción el día de hoy"
                            statusMessageTextView.setTextColor(androidx.core.content.ContextCompat.getColor(binding.root.context, R.color.food_status_error))
                            statusMessageTextView.setTypeface(null, android.graphics.Typeface.NORMAL)
                        } else {
                            // Usuario votó OTRA opción
                            statusMessageTextView.text = "No seleccionada"
                            statusMessageTextView.setTextColor(androidx.core.content.ContextCompat.getColor(binding.root.context, R.color.food_text_secondary))
                            statusMessageTextView.setTypeface(null, android.graphics.Typeface.NORMAL)
                        }
                        
                    } else {
                        // ABIERTO
                        statusMessageTextView.visibility = View.GONE
                        optionButton.visibility = View.VISIBLE
                        
                        if (isSelected) {
                            // Opción seleccionada -> Botón rojo sin icono y texto blanco ("Quitar elección")
                            optionButton.text = binding.root.context.getString(R.string.remove_selection)
                            optionButton.icon = null
                            
                            optionButton.backgroundTintList = AppCompatResources.getColorStateList(binding.root.context, R.color.vote_button_negative_tint)
                            optionButton.rippleColor = android.content.res.ColorStateList.valueOf(
                                androidx.core.content.ContextCompat.getColor(binding.root.context, R.color.food_ripple)
                            )
                            optionButton.setTextColor(androidx.core.content.ContextCompat.getColor(binding.root.context, R.color.white))
                            
                            optionButton.setOnClickListener {
                                onRemoveVote(userVote!!.id, menu.id, null)
                            }
                        } else {
                            // Opción no seleccionada -> Botón verde ("Elegir esta opción")
                            optionButton.text = binding.root.context.getString(R.string.choose_option)
                            optionButton.icon = null
                            
                            optionButton.backgroundTintList = AppCompatResources.getColorStateList(binding.root.context, R.color.vote_button_positive_tint)
                            optionButton.rippleColor = android.content.res.ColorStateList.valueOf(
                                androidx.core.content.ContextCompat.getColor(binding.root.context, R.color.food_ripple)
                            )
                            optionButton.setTextColor(androidx.core.content.ContextCompat.getColor(binding.root.context, R.color.white))
                            
                            optionButton.setOnClickListener {
                                onSelectOption(menu.id, option.id, null)
                            }
                        }
                    }
                    
                    optionsContainer?.addView(optionView)
                }
            } else {
                binding.menuDescriptionTextView.visibility = View.GONE
                optionsContainer?.visibility = View.GONE
            }
            
            // Ocultar botón "Quitar elección" antiguo (ya no se usa, se maneja en cada opción)
            binding.removeVoteButton.visibility = View.GONE
            
            // Configurar click listener
            binding.root.setOnClickListener {
                onMenuClick(item)
            }
            
            android.util.Log.d("WeeklyMenuAdapter", "Menú bindeado: ${menu.date} - ${options.size} opciones - Voto: ${if (userVote != null) "Sí" else "No"}")
        }
    }

    class MenuDiffCallback : DiffUtil.ItemCallback<WeeklyMenuItem>() {
        override fun areItemsTheSame(oldItem: WeeklyMenuItem, newItem: WeeklyMenuItem): Boolean {
            return oldItem.menu.id == newItem.menu.id
        }

        override fun areContentsTheSame(oldItem: WeeklyMenuItem, newItem: WeeklyMenuItem): Boolean {
            return oldItem.menu.id == newItem.menu.id && 
                   oldItem.userVote?.id == newItem.userVote?.id
        }
    }
}

