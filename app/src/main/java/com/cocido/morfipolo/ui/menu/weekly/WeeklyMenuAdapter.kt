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
import com.cocido.morfipolo.domain.model.MenuStatus
import java.text.SimpleDateFormat
import java.util.*

class WeeklyMenuAdapter : ListAdapter<Menu, WeeklyMenuAdapter.MenuViewHolder>(MenuDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = ItemWeeklyMenuBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MenuViewHolder(private val binding: ItemWeeklyMenuBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        fun bind(menu: Menu) {
            binding.dateTextView.text = dateFormat.format(menu.fecha)
            binding.menuDescriptionTextView.text = menu.descripcion
            binding.statusTextView.text = if (menu.estado == MenuStatus.ABIERTO) {
                binding.root.context.getString(R.string.open)
            } else {
                binding.root.context.getString(R.string.closed)
            }
            binding.statusTextView.setBackgroundResource(
                if (menu.estado == MenuStatus.ABIERTO) {
                    R.drawable.status_badge_green
                } else {
                    R.drawable.status_badge_red
                }
            )
        }
    }

    class MenuDiffCallback : DiffUtil.ItemCallback<Menu>() {
        override fun areItemsTheSame(oldItem: Menu, newItem: Menu): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Menu, newItem: Menu): Boolean {
            return oldItem == newItem
        }
    }
}

