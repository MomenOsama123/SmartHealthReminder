package com.example.smarthealthreminder.features.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.model.Reminder

class WelcomeReminderAdapter : RecyclerView.Adapter<WelcomeReminderAdapter.ViewHolder>() {

    private val reminders = mutableListOf<Reminder>()
    private var onReminderClickListener: ((Reminder) -> Unit)? = null

    fun setReminders(newReminders: List<Reminder>) {
        reminders.clear()
        reminders.addAll(newReminders)
        notifyDataSetChanged()
    }

    fun setOnReminderClickListener(listener: (Reminder) -> Unit) {
        onReminderClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_welcome_reminder, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(reminders[position])
    }

    override fun getItemCount(): Int = reminders.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.iv_reminder_icon)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_reminder_title)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_reminder_time)
        private val ivStatus: ImageView = itemView.findViewById(R.id.iv_status_icon)

        fun bind(reminder: Reminder) {
            tvTitle.text = reminder.title
            tvTime.text = itemView.context.getString(R.string.due_at, reminder.time)
            
            // Set icon based on category
            when (reminder.category?.lowercase()) {
                "vitamins", "meds", "medicine" -> {
                    ivIcon.setImageResource(R.drawable.ic_pill)
                    ivIcon.backgroundTintList = itemView.context.getColorStateList(R.color.medicine_light)
                }
                "hydration", "water" -> {
                    ivIcon.setImageResource(R.drawable.ic_water)
                    ivIcon.backgroundTintList = itemView.context.getColorStateList(R.color.bg_light_blue)
                }
                else -> {
                    ivIcon.setImageResource(R.drawable.ic_heart)
                    ivIcon.backgroundTintList = itemView.context.getColorStateList(R.color.success_light)
                }
            }

            // Set status icon
            if (reminder.status == "Completed") {
                ivStatus.setImageResource(R.drawable.ic_check_circle)
                ivStatus.imageTintList = itemView.context.getColorStateList(R.color.success)
            } else {
                ivStatus.setImageResource(R.drawable.ic_circle_outline)
                ivStatus.imageTintList = itemView.context.getColorStateList(R.color.divider)
            }

            itemView.setOnClickListener {
                onReminderClickListener?.invoke(reminder)
            }
        }
    }
}
