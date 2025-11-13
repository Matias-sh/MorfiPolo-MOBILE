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
    private val onRemoveVote: (String) -> Unit = {} // voteId
) : ListAdapter<WeeklyMenuItem, WeeklyMenuAdapter.MenuViewHolder>(MenuDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = ItemWeeklyMenuBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MenuViewHolder(binding, onMenuClick, onRemoveVote)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MenuViewHolder(
        private val binding: ItemWeeklyMenuBinding,
        private val onMenuClick: (WeeklyMenuItem) -> Unit,
        private val onRemoveVote: (String) -> Unit
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
            
            // Configurar estado - validar si realmente está abierto según el horario (08:00 - 11:00)
            val isWithinTime = isWithinSelectionTime(menu)
            val isActuallyOpen = menu.status == "open" && isWithinTime
            val statusText = when {
                isActuallyOpen -> binding.root.context.getString(R.string.open)
                menu.status == "closed" -> binding.root.context.getString(R.string.closed)
                else -> binding.root.context.getString(R.string.closed) // Si pasó el horario, mostrar cerrado
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
            
            // Mostrar opciones del menú (no la descripción)
            val options = menu.getOptionsOrEmpty()
            if (options.isNotEmpty()) {
                // Mostrar todas las opciones separadas por comas, o solo la primera si hay una
                val optionsText = if (options.size == 1) {
                    options[0].name
                } else {
                    options.joinToString(", ") { it.name }
                }
                binding.menuDescriptionTextView.text = optionsText
            } else {
                binding.menuDescriptionTextView.text = binding.root.context.getString(R.string.no_menu_available)
            }
            
            // Configurar botón "Quitar elección" si hay voto y el menú está realmente abierto
            if (userVote != null && isActuallyOpen) {
                binding.removeVoteButton.visibility = View.VISIBLE
                binding.removeVoteButton.setOnClickListener {
                    onRemoveVote(userVote.id)
                }
            } else {
                binding.removeVoteButton.visibility = View.GONE
            }
            
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

