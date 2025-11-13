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
            
            // Configurar horario
            val startTime = try {
                menu.start_time.split("T")[1].substring(0, 5) // HH:mm
            } catch (e: Exception) {
                "08:00"
            }
            val endTime = try {
                menu.end_time.split("T")[1].substring(0, 5) // HH:mm
            } catch (e: Exception) {
                "11:00"
            }
            binding.timeRangeTextView.text = binding.root.context.getString(
                R.string.selection_time,
                startTime,
                endTime
            )
            binding.timeRangeTextView.visibility = View.VISIBLE
            
            // Configurar estado
            val statusText = when (menu.status) {
                "open" -> binding.root.context.getString(R.string.open)
                "closed" -> binding.root.context.getString(R.string.closed)
                else -> menu.status
            }
            binding.statusTextView.text = statusText
            binding.statusTextView.setBackgroundResource(
                if (menu.status == "open") {
                    R.drawable.status_badge_green
                } else {
                    R.drawable.status_badge_red
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
            
            // Configurar botón "Quitar elección" si hay voto y el menú está abierto
            if (userVote != null && menu.status == "open") {
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

