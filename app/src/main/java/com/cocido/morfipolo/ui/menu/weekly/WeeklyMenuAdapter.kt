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

        private val dateFormat = SimpleDateFormat("d", Locale.getDefault())
        private val monthFormat = SimpleDateFormat("MMM", Locale("es", "AR"))
        private val dayNameFormat = SimpleDateFormat("EEEE", Locale("es", "AR"))
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
            binding.root.findViewById<TextView>(R.id.monthTextView)?.text = monthFormat.format(menuDate).uppercase(Locale("es", "AR"))
            binding.root.findViewById<TextView>(R.id.dayNameTextView)?.text = dayNameFormat.format(menuDate).replaceFirstChar { it.titlecase(Locale("es", "AR")) }
            
            // Configurar horario fijo: 08:00 - 11:00
            binding.timeRangeTextView.text = "08:00 - 11:00"
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
            val statusCard = binding.root.findViewById<com.google.android.material.card.MaterialCardView>(R.id.statusCard)
            statusCard.setCardBackgroundColor(
                androidx.core.content.ContextCompat.getColor(
                    binding.root.context,
                    if (isActuallyOpen) R.color.slate_100 else R.color.neutral_200
                )
            )
            binding.statusTextView.setTextColor(
                androidx.core.content.ContextCompat.getColor(
                    binding.root.context,
                    if (isActuallyOpen) R.color.slate_700 else R.color.neutral_600
                )
            )
            binding.statusSparklesIcon.visibility = if (isActuallyOpen) View.VISIBLE else View.GONE

            val dateBadgeCard = binding.root.findViewById<com.google.android.material.card.MaterialCardView>(R.id.dateBadgeCard)
            val monthText = binding.root.findViewById<TextView>(R.id.monthTextView)
            val dateText = binding.dateTextView
            when {
                isToday -> {
                    dateBadgeCard.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(binding.root.context, R.color.slate_700))
                    dateText.setTextColor(androidx.core.content.ContextCompat.getColor(binding.root.context, R.color.white))
                    monthText.setTextColor(androidx.core.content.ContextCompat.getColor(binding.root.context, R.color.white))
                }
                userVote != null -> {
                    dateBadgeCard.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(binding.root.context, R.color.emerald_600))
                    dateText.setTextColor(androidx.core.content.ContextCompat.getColor(binding.root.context, R.color.white))
                    monthText.setTextColor(androidx.core.content.ContextCompat.getColor(binding.root.context, R.color.white))
                }
                else -> {
                    dateBadgeCard.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(binding.root.context, R.color.white))
                    dateText.setTextColor(androidx.core.content.ContextCompat.getColor(binding.root.context, R.color.food_text_primary))
                    monthText.setTextColor(androidx.core.content.ContextCompat.getColor(binding.root.context, R.color.food_text_secondary))
                }
            }
            
            // Mostrar "Ya elegiste" si hay voto — Emerald 100 bg + Emerald 700 text
            val hasVote = userVote != null
            binding.alreadySelectedTextView.visibility = if (hasVote) View.VISIBLE else View.GONE
            val alreadySelectedCard = binding.root.findViewById<com.google.android.material.card.MaterialCardView>(R.id.alreadySelectedCard)
            alreadySelectedCard.visibility = if (hasVote) View.VISIBLE else View.GONE
            alreadySelectedCard.setCardBackgroundColor(
                androidx.core.content.ContextCompat.getColor(binding.root.context, R.color.emerald_100)
            )
            binding.alreadySelectedTextView.setTextColor(
                androidx.core.content.ContextCompat.getColor(binding.root.context, R.color.emerald_700)
            )
            
            val options = menu.getOptionsOrEmpty()
            val optionsContainer = binding.root.findViewById<android.widget.LinearLayout>(R.id.optionsContainer)
            optionsContainer?.removeAllViews()
            optionsContainer?.visibility = View.GONE
            binding.removeVoteButton.visibility = View.GONE

            val secondaryText = binding.root.findViewById<TextView>(R.id.menuSecondaryTextView)
            if (options.isNotEmpty()) {
                binding.menuDescriptionTextView.text = options.first().name
                val description = menu.description
                if (description.isNotBlank()) {
                    secondaryText.visibility = View.VISIBLE
                    secondaryText.text = description.uppercase(Locale("es", "AR"))
                } else {
                    secondaryText.visibility = View.GONE
                }
            } else {
                binding.menuDescriptionTextView.text = if (menu.description.isNotBlank()) menu.description else binding.root.context.getString(R.string.no_menu_available)
                secondaryText.visibility = View.GONE
            }

            val noteTextView = binding.root.findViewById<TextView>(R.id.statusNoteTextView)
            noteTextView.visibility = View.GONE

            val isPastMenu = menuDate.before(Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time) && !isToday

            if (userVote == null && !isPastMenu) {
                noteTextView.visibility = View.VISIBLE
                noteTextView.text = "Aún no seleccionaste este día"
                noteTextView.setTextColor(androidx.core.content.ContextCompat.getColor(binding.root.context, R.color.amber_600))
                noteTextView.setBackgroundResource(R.drawable.bg_note_amber)
                noteTextView.setTypeface(null, android.graphics.Typeface.NORMAL)
            } else if (userVote == null && isPastMenu) {
                noteTextView.visibility = View.VISIBLE
                noteTextView.text = "No se realizó selección"
                noteTextView.setTextColor(androidx.core.content.ContextCompat.getColor(binding.root.context, R.color.food_text_secondary))
                noteTextView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                noteTextView.setTypeface(null, android.graphics.Typeface.ITALIC)
            }

            binding.root.findViewById<View>(R.id.itemDivider).visibility =
                if (absoluteAdapterPosition == bindingAdapter?.itemCount?.minus(1)) View.GONE else View.VISIBLE
            
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

