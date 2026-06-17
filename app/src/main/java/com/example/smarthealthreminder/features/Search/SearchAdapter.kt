package com.example.smarthealthreminder.features.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.data.local.entity.ReminderEntity

class SearchAdapter(
    private val onItemClick: (ReminderEntity) -> Unit
) : ListAdapter<ReminderEntity, SearchAdapter.SearchViewHolder>(ReminderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_reminder, parent, false)
        return SearchViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SearchViewHolder(
        itemView: View,
        private val onItemClick: (ReminderEntity) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val ivType: ImageView = itemView.findViewById(R.id.iv_reminder_type)
        private val tvName: TextView = itemView.findViewById(R.id.tv_reminder_name)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_reminder_time)

        fun bind(reminder: ReminderEntity) {
            // Updated to match ReminderEntity field: 'title'
            tvName.text = reminder.title

            // Using string resource for localization: "Due at %s"
            tvTime.text = itemView.context.getString(R.string.due_at, reminder.time ?: "--:--")

            // Corrected to match ReminderEntity field: 'category'
            ivType.setImageResource(getReminderIcon(reminder.category))

            itemView.setOnClickListener {
                onItemClick(reminder)
            }
        }

        private fun getReminderIcon(category: String?): Int {
            return when (category?.lowercase()) {
                "vitamins", "pill" -> R.drawable.ic_pill
                "hydration", "water" -> R.drawable.ic_water
                else -> R.drawable.ic_heart
            }
        }
    }

    class ReminderDiffCallback : DiffUtil.ItemCallback<ReminderEntity>() {
        override fun areItemsTheSame(oldItem: ReminderEntity, newItem: ReminderEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ReminderEntity, newItem: ReminderEntity): Boolean {
            return oldItem == newItem
        }
    }
}
