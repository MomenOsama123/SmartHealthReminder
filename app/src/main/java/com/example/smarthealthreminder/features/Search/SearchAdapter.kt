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

class SearchAdapter(
    private val onItemClick: (SearchResult) -> Unit
) : ListAdapter<SearchResult, SearchAdapter.SearchViewHolder>(ReminderDiffCallback()) {

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
        private val onItemClick: (SearchResult) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val ivType: ImageView = itemView.findViewById(R.id.iv_reminder_type)
        private val tvName: TextView = itemView.findViewById(R.id.tv_reminder_name)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_reminder_time)
        private val tvResultType: TextView = itemView.findViewById(R.id.tv_result_type)

        fun bind(result: SearchResult) {
            tvName.text = result.title
            tvTime.text = itemView.context.getString(R.string.due_at, result.time.ifEmpty { "--:--" })
            ivType.setImageResource(getReminderIcon(result.category))
            
            tvResultType.text = when (result) {
                is SearchResult.Reminder -> "REMINDER"
                is SearchResult.Alarm -> "ALARM"
            }

            itemView.setOnClickListener {
                onItemClick(result)
            }
        }

        private fun getReminderIcon(category: String?): Int {
            return when (category?.lowercase()) {
                "vitamins", "pill", "medicine" -> R.drawable.ic_pill
                "hydration", "water" -> R.drawable.ic_water
                else -> R.drawable.ic_heart
            }
        }
    }

    class ReminderDiffCallback : DiffUtil.ItemCallback<SearchResult>() {
        override fun areItemsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean {
            return oldItem == newItem
        }
    }
}
