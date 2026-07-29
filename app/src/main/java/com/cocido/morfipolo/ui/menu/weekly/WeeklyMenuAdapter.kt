package com.cocido.morfipolo.ui.menu.weekly

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cocido.morfipolo.R
import com.cocido.morfipolo.databinding.ItemWeeklyMenuBinding
import com.cocido.morfipolo.domain.model.Menu
import com.cocido.morfipolo.util.MenuTimeUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * Elegir/cambiar opción se hace siempre desde la pantalla "Hoy" (única fuente
 * de verdad para votar), acá solo se navega a ella. Simplifica el spec de
 * diseño y elimina el riesgo de doble-tap que tenía el picker inline viejo.
 */
class WeeklyMenuAdapter(
    private val onChangeSelection: (WeeklyMenuItem) -> Unit = {}
) : ListAdapter<WeeklyMenuItem, WeeklyMenuAdapter.MenuViewHolder>(MenuDiffCallback()) {

    private val expandedIds = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = ItemWeeklyMenuBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MenuViewHolder(binding, onChangeSelection, expandedIds) { id ->
            if (!expandedIds.add(id)) expandedIds.remove(id)
            notifyItemChanged(currentList.indexOfFirst { it.menu.id == id })
        }
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MenuViewHolder(
        private val binding: ItemWeeklyMenuBinding,
        private val onChangeSelection: (WeeklyMenuItem) -> Unit,
        private val expandedIds: MutableSet<String>,
        private val onToggle: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dayNameFormat = SimpleDateFormat("EEEE", Locale("es", "AR"))
        private val dayShortFormat = SimpleDateFormat("EEE", Locale("es", "AR"))
        private val dayNumFormat = SimpleDateFormat("d", Locale("es", "AR"))

        fun bind(item: WeeklyMenuItem) {
            val menu = item.menu
            val userVote = item.userVote
            val context = binding.root.context

            val menuDate = try {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(menu.date) ?: Date()
            } catch (e: Exception) {
                Date()
            }

            val isToday = MenuTimeUtils.isMenuToday(menu)
            val isWithinTime = MenuTimeUtils.isWithinSelectionTime(menu)
            val isActuallyOpen = menu.status == "open" && isToday && isWithinTime
            val hasVoted = userVote != null
            val hasOptions = menu.getOptionsOrEmpty().isNotEmpty()

            binding.dayNumTextView.text = dayNumFormat.format(menuDate)
            binding.dayShortTextView.text = dayShortFormat.format(menuDate).replaceFirstChar { it.uppercase() }
            binding.dayNameTextView.text = dayNameFormat.format(menuDate).replaceFirstChar { it.uppercase() }
            binding.todayBadge.visibility = if (isToday) View.VISIBLE else View.GONE
            binding.menuDescriptionTextView.text = if (hasOptions) {
                userVote?.option?.name ?: menu.description.ifBlank { menu.getOptionsOrEmpty().joinToString(" · ") { it.name } }
            } else {
                context.getString(R.string.no_menu_available)
            }

            // Chip + badge + detalle según estado real (no el mock de 5 estados fijos del spec)
            val (chipBg, chipText, chipLabel, detail) = when {
                hasVoted -> Quad(
                    R.drawable.chip_chosen, R.color.chip_chosen_text, R.string.chosen,
                    context.getString(R.string.your_choice_today) + ": " + userVote!!.option.name
                )
                isActuallyOpen -> Quad(
                    R.drawable.chip_pending, R.color.chip_pending_text, R.string.pending,
                    context.getString(R.string.choose_hint)
                )
                !hasOptions -> Quad(
                    R.drawable.chip_nomenu, R.color.chip_nomenu_text, R.string.no_menu_chip,
                    context.getString(R.string.no_menu_available)
                )
                else -> Quad(
                    R.drawable.chip_closed, R.color.chip_closed_text, R.string.closed,
                    context.getString(R.string.no_choice_today)
                )
            }
            binding.statusTextView.setBackgroundResource(chipBg)
            binding.statusTextView.setTextColor(androidx.core.content.ContextCompat.getColor(context, chipText))
            binding.statusTextView.text = context.getString(chipLabel)
            binding.dayBadge.background.setTint(androidx.core.content.ContextCompat.getColor(context, chipText))

            binding.detailTextView.text = detail

            val showChangeAction = isActuallyOpen
            binding.changeSelectionAction.visibility = if (showChangeAction) View.VISIBLE else View.GONE
            binding.changeSelectionAction.text = context.getString(
                if (hasVoted) R.string.change_choice else R.string.choose_action
            )
            binding.changeSelectionAction.setOnClickListener { onChangeSelection(item) }

            val isExpanded = expandedIds.contains(menu.id)
            binding.expandedContent.visibility = if (isExpanded) View.VISIBLE else View.GONE
            binding.dayHeader.setOnClickListener { onToggle(menu.id) }
        }
    }

    private data class Quad(val chipBg: Int, val chipTextColor: Int, val chipLabel: Int, val detail: String)

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
