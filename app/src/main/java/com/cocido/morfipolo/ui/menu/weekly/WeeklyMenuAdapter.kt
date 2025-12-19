package com.cocido.morfipolo.ui.menu.weekly

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
    private val onRemoveVote: (String, String?) -> Unit = { _, _ -> }, // voteId, errorMessage
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
        private val onRemoveVote: (String, String?) -> Unit,
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
            
            // Configurar horario fijo: 08:00 - 11:00
            binding.timeRangeTextView.text = binding.root.context.getString(
                R.string.selection_time,
                "08:00",
                "11:00"
            )
            binding.timeRangeTextView.visibility = View.VISIBLE
            
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
                
                // Mostrar descripción del menú
                binding.menuDescriptionTextView.text = menu.description
                
                // Agregar opciones con botones
                options.forEachIndexed { index, option ->
                    val optionView = LayoutInflater.from(binding.root.context)
                        .inflate(R.layout.item_menu_option, optionsContainer, false)
                    
                    val optionNameTextView = optionView.findViewById<TextView>(R.id.optionNameTextView)
                    val optionButton = optionView.findViewById<com.google.android.material.button.MaterialButton>(R.id.optionButton)
                    val selectedIndicator = optionView.findViewById<android.widget.ImageView>(R.id.selectedIndicator)
                    val statusMessageTextView = optionView.findViewById<TextView>(R.id.statusMessageTextView)
                    
                    // Nombre de la opción
                    optionNameTextView.text = if (options.size > 1) {
                        "Opción ${index + 1}: ${option.name}"
                    } else {
                        option.name
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
                        
                        // Mantener indicador si está seleccionado
                        selectedIndicator.visibility = if (isSelected) View.VISIBLE else View.GONE
                        
                    } else {
                        // ABIERTO
                        statusMessageTextView.visibility = View.GONE
                        optionButton.visibility = View.VISIBLE
                        
                        if (isSelected) {
                            // Opción seleccionada -> Botón rojo con texto negro ("Quitar elección")
                            optionButton.text = binding.root.context.getString(R.string.remove_selection)
                            optionButton.setIconResource(android.R.drawable.ic_menu_delete)
                            
                            optionButton.backgroundTintList = android.content.res.ColorStateList.valueOf(
                                androidx.core.content.ContextCompat.getColor(binding.root.context, R.color.food_status_error)
                            )
                            optionButton.setTextColor(androidx.core.content.ContextCompat.getColor(binding.root.context, R.color.black))
                            optionButton.iconTint = android.content.res.ColorStateList.valueOf(
                                androidx.core.content.ContextCompat.getColor(binding.root.context, R.color.black)
                            )
                            
                            selectedIndicator.visibility = View.VISIBLE
                            
                            optionButton.setOnClickListener {
                                onRemoveVote(userVote!!.id, null)
                            }
                        } else {
                            // Opción no seleccionada -> Botón verde ("Elegir esta opción")
                            optionButton.text = binding.root.context.getString(R.string.choose_option)
                            optionButton.setIconResource(android.R.drawable.ic_menu_add)
                            
                            optionButton.backgroundTintList = android.content.res.ColorStateList.valueOf(
                                androidx.core.content.ContextCompat.getColor(binding.root.context, R.color.food_secondary_green)
                            )
                            optionButton.setTextColor(androidx.core.content.ContextCompat.getColor(binding.root.context, R.color.white))
                            optionButton.iconTint = android.content.res.ColorStateList.valueOf(
                                androidx.core.content.ContextCompat.getColor(binding.root.context, R.color.white)
                            )
                            
                            selectedIndicator.visibility = View.GONE
                            
                            optionButton.setOnClickListener {
                                onSelectOption(menu.id, option.id, null)
                            }
                        }
                    }
                    
                    optionsContainer?.addView(optionView)
                }
            } else {
                binding.menuDescriptionTextView.text = binding.root.context.getString(R.string.no_menu_available)
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

