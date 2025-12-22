package com.cocido.morfipolo.ui.notifications

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cocido.morfipolo.databinding.ItemNotificationBinding
import com.cocido.morfipolo.domain.model.CustomNotification

class NotificationAdapter(
    private val onNotificationClick: (CustomNotification) -> Unit,
    private val onToggleChanged: (CustomNotification, Boolean) -> Unit
) : ListAdapter<CustomNotification, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NotificationViewHolder(
        private val binding: ItemNotificationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: CustomNotification) {
            binding.timeTextView.text = notification.getFormattedTime()
            binding.daysTextView.text = notification.getFormattedDays()
            binding.enabledSwitch.isChecked = notification.isEnabled

            // Evitar llamadas infinitas al cambiar el switch
            binding.enabledSwitch.setOnCheckedChangeListener(null)
            binding.enabledSwitch.isChecked = notification.isEnabled
            binding.enabledSwitch.setOnCheckedChangeListener { _, isChecked ->
                onToggleChanged(notification, isChecked)
            }

            binding.root.setOnClickListener {
                onNotificationClick(notification)
            }
        }
    }

    class NotificationDiffCallback : DiffUtil.ItemCallback<CustomNotification>() {
        override fun areItemsTheSame(oldItem: CustomNotification, newItem: CustomNotification): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CustomNotification, newItem: CustomNotification): Boolean {
            return oldItem == newItem
        }
    }
}
