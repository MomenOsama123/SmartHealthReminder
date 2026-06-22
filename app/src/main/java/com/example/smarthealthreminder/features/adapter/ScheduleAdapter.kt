package com.example.smarthealthreminder.features.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.model.ScheduleItem

class ScheduleAdapter : ListAdapter<ScheduleItem, ScheduleAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val priorityBar: View = view.findViewById(R.id.view_priority_bar)
        val title: TextView = view.findViewById(R.id.tv_title)
        val date: TextView = view.findViewById(R.id.tv_date)
        val time: TextView = view.findViewById(R.id.tv_time)
        val status: TextView = view.findViewById(R.id.tv_status)
        val category: TextView = view.findViewById(R.id.tv_category)
        val earlyNotifIcon: ImageView = view.findViewById(R.id.iv_early_notification)
        val earlyNotifText: TextView = view.findViewById(R.id.tv_early_notification)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_schedule, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = currentList[position]

        holder.title.text = item.title
        holder.date.text = item.date
        holder.time.text = item.time
        holder.category.text = when (item.itemType) {
            ScheduleItem.TYPE_ALARM -> "⏰ Alarm"
            ScheduleItem.TYPE_SCHEDULE_ENTRY -> "📅 Schedule"
            ScheduleItem.TYPE_REPORT -> "📊 Report"
            ScheduleItem.TYPE_NOTE -> "📝 Note"
            else -> item.category
        }

        // Priority bar color based on item type
        val priorityColor = when (item.itemType) {
            ScheduleItem.TYPE_ALARM -> Color.parseColor("#FF9800")
            ScheduleItem.TYPE_SCHEDULE_ENTRY -> Color.parseColor("#9C27B0")
            ScheduleItem.TYPE_REPORT -> Color.parseColor("#E91E63")
            ScheduleItem.TYPE_NOTE -> Color.parseColor("#4CAF50")
            else -> when (item.priority.uppercase()) {
                "HIGH", "URGENT" -> Color.parseColor("#FF5252")
                "MEDIUM" -> Color.parseColor("#FF9800")
                "LOW" -> Color.parseColor("#4CAF50")
                else -> Color.parseColor("#2196F3")
            }
        }
        holder.priorityBar.setBackgroundColor(priorityColor)

        // Status badge
        val (statusText, statusColor) = when (item.status.uppercase()) {
            "DONE", "COMPLETED" -> "Done" to Color.parseColor("#4CAF50")
            "MISSED" -> "Missed" to Color.parseColor("#FF5252")
            else -> "Pending" to Color.parseColor("#FF9800")
        }
        holder.status.text = statusText
        holder.status.setBackgroundColor(statusColor)

        // Early notification (only for reminders)
        if (item.earlyNotification && item.earlyNotificationMinutes > 0 && item.itemType == ScheduleItem.TYPE_REMINDER) {
            holder.earlyNotifIcon.visibility = View.VISIBLE
            holder.earlyNotifText.visibility = View.VISIBLE
            holder.earlyNotifText.text = "${item.earlyNotificationMinutes} min early"
        } else {
            holder.earlyNotifIcon.visibility = View.GONE
            holder.earlyNotifText.visibility = View.GONE
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ScheduleItem>() {
        override fun areItemsTheSame(a: ScheduleItem, b: ScheduleItem) = a.id == b.id
        override fun areContentsTheSame(a: ScheduleItem, b: ScheduleItem) = a == b
    }
}