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

    private var currentQuery: String = ""

    fun updateQuery(query: String) {
        currentQuery = query
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_reminder, parent, false)
        return SearchViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        holder.bind(getItem(position), currentQuery)
    }

    class SearchViewHolder(
        itemView: View,
        private val onItemClick: (SearchResult) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val ivType: ImageView = itemView.findViewById(R.id.iv_reminder_type)
        private val tvName: TextView = itemView.findViewById(R.id.tv_reminder_name)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_reminder_time)
        private val tvDate: TextView = itemView.findViewById(R.id.tv_reminder_date)
        private val tvDot: TextView = itemView.findViewById(R.id.tv_dot_separator)
        private val tvResultType: TextView = itemView.findViewById(R.id.tv_result_type)

        fun bind(result: SearchResult, query: String) {
            // Highlighting Logic
            val title = result.title
            if (query.isNotEmpty() && title.contains(query, ignoreCase = true)) {
                val spannable = android.text.SpannableString(title)
                val start = title.lowercase().indexOf(query.lowercase())
                val end = start + query.length
                spannable.setSpan(
                    android.text.style.ForegroundColorSpan(androidx.core.content.ContextCompat.getColor(itemView.context, R.color.primary)),
                    start, end, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                tvName.text = spannable
            } else {
                tvName.text = title
            }

            tvTime.text = itemView.context.getString(R.string.due_at, result.time.ifEmpty { "--:--" })
            
            // Date Visibility
            if (result.date.isNotEmpty()) {
                tvDate.text = result.date
                tvDate.visibility = View.VISIBLE
                tvDot.visibility = View.VISIBLE
            } else {
                tvDate.visibility = View.GONE
                tvDot.visibility = View.GONE
            }

            ivType.setImageResource(getReminderIcon(result.category))
            
            val typeStr = when (result) {
                is SearchResult.Reminder -> "REMINDER"
                is SearchResult.Alarm -> "ALARM"
            }
            tvResultType.text = if (result.category.isNotEmpty()) "$typeStr | ${result.category.uppercase()}" else typeStr

            itemView.setOnClickListener {
                onItemClick(result)
            }
        }

        private fun getReminderIcon(category: String?): Int {
            return when (category?.lowercase()) {
                "medicine" -> R.drawable.ic_medicine
                "appointment" -> R.drawable.ic_appointment
                "task" -> R.drawable.ic_task
                "custom" -> R.drawable.ic_custom
                "vitamins", "pill" -> R.drawable.ic_pill
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
