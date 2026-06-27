package com.example.smarthealthreminder.features.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.model.TimelineItem
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip

class TimelineAdapter : ListAdapter<TimelineItem, TimelineAdapter.ViewHolder>(DiffCallback()) {

    private var actionListener: OnItemActionListener? = null

    interface OnItemActionListener {
        fun onItemClick(item: TimelineItem)
        fun onMarkDone(item: TimelineItem)
        fun onMarkMissed(item: TimelineItem)
        fun onDelete(item: TimelineItem)
    }

    fun setOnItemActionListener(listener: OnItemActionListener) {
        this.actionListener = listener
    }

    fun setItems(items: List<TimelineItem>) {
        submitList(items)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_timeline, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMonth: TextView = itemView.findViewById(R.id.tv_month)
        private val tvDay: TextView = itemView.findViewById(R.id.tv_day)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        private val tvDescription: TextView = itemView.findViewById(R.id.tv_description)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
        private val chipCategory: Chip = itemView.findViewById(R.id.chip_category)
        private val statusIndicator: View = itemView.findViewById(R.id.status_indicator)
        private val btnMoreActions: ImageView = itemView.findViewById(R.id.btn_more_actions)

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    showDetailDialog(getItem(position))
                }
            }
            btnMoreActions.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    showDetailDialog(getItem(position))
                }
            }
        }

        fun bind(item: TimelineItem) {
            tvMonth.text = item.month ?: ""
            tvDay.text = item.day ?: ""
            tvTitle.text = item.title ?: ""
            tvDescription.text = item.description ?: ""
            tvTime.text = item.time ?: ""
            chipCategory.text = item.category ?: "General"

            val status = item.status ?: "PENDING"
            tvStatus.text = status
            when (status.uppercase()) {
                "MISSED" -> {
                    tvStatus.setBackgroundResource(R.drawable.bg_rounded_card_red)
                    tvStatus.setTextColor(itemView.context.getColor(R.color.missed))
                    statusIndicator.backgroundTintList =
                        itemView.context.getColorStateList(R.color.missed)
                }
                "DONE", "COMPLETED" -> {
                    tvStatus.setBackgroundResource(R.drawable.bg_rounded_card_green)
                    tvStatus.setTextColor(itemView.context.getColor(R.color.success))
                    statusIndicator.backgroundTintList =
                        itemView.context.getColorStateList(R.color.success)
                }
                "SNOOZED" -> {
                    tvStatus.setBackgroundResource(R.drawable.bg_rounded_card)
                    tvStatus.setTextColor(itemView.context.getColor(R.color.pending))
                    statusIndicator.backgroundTintList =
                        itemView.context.getColorStateList(R.color.pending)
                }
                else -> {
                    tvStatus.setBackgroundResource(R.drawable.bg_rounded_card)
                    tvStatus.setTextColor(itemView.context.getColor(R.color.pending))
                    statusIndicator.backgroundTintList =
                        itemView.context.getColorStateList(R.color.pending)
                }
            }
        }

        private fun showDetailDialog(item: TimelineItem) {
            val context = itemView.context
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_reminder_detail, null)

            dialogView.findViewById<TextView>(R.id.tv_dialog_title).text = item.title ?: "Reminder"
            dialogView.findViewById<TextView>(R.id.tv_dialog_description).text =
                if (item.description.isNullOrBlank()) "No description provided." else item.description

            val dateText = if (!item.date.isNullOrBlank()) {
                "${item.month ?: ""} ${item.day ?: ""}, ${item.date}"
            } else {
                "${item.month ?: ""} ${item.day ?: ""}"
            }
            dialogView.findViewById<TextView>(R.id.tv_dialog_date).text = dateText
            dialogView.findViewById<TextView>(R.id.tv_dialog_time).text = item.time ?: "--:--"
            dialogView.findViewById<TextView>(R.id.tv_dialog_category).text = item.category ?: "General"

            val statusChip = dialogView.findViewById<Chip>(R.id.chip_dialog_status)
            val status = item.status ?: "PENDING"
            statusChip.text = status
            when (status.uppercase()) {
                "MISSED" -> {
                    statusChip.setTextColor(context.getColor(R.color.missed))
                    statusChip.chipBackgroundColor = context.getColorStateList(R.color.missed_light)
                }
                "DONE", "COMPLETED" -> {
                    statusChip.setTextColor(context.getColor(R.color.success))
                    statusChip.chipBackgroundColor = context.getColorStateList(R.color.success_light)
                }
                "SNOOZED" -> {
                    statusChip.setTextColor(context.getColor(R.color.pending))
                    statusChip.chipBackgroundColor = context.getColorStateList(R.color.surface_variant)
                }
                else -> {
                    statusChip.setTextColor(context.getColor(R.color.pending))
                    statusChip.chipBackgroundColor = context.getColorStateList(R.color.surface_variant)
                }
            }

            val dialog = AlertDialog.Builder(context)
                .setView(dialogView)
                .create()

            val btnMarkDone = dialogView.findViewById<MaterialButton>(R.id.btn_mark_done)
            val btnMarkMissed = dialogView.findViewById<MaterialButton>(R.id.btn_mark_missed)
            val btnDelete = dialogView.findViewById<MaterialButton>(R.id.btn_delete)

            when (status.uppercase()) {
                "COMPLETED", "DONE" -> {
                    btnMarkDone.visibility = View.GONE
                    btnMarkMissed.visibility = View.VISIBLE
                }
                "MISSED" -> {
                    btnMarkDone.visibility = View.VISIBLE
                    btnMarkMissed.visibility = View.GONE
                }
                else -> {
                    btnMarkDone.visibility = View.VISIBLE
                    btnMarkMissed.visibility = View.VISIBLE
                }
            }

            btnMarkDone.setOnClickListener {
                actionListener?.onMarkDone(item)
                dialog.dismiss()
            }

            btnMarkMissed.setOnClickListener {
                actionListener?.onMarkMissed(item)
                dialog.dismiss()
            }

            btnDelete.setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle("Delete Reminder")
                    .setMessage("Are you sure you want to delete \"${item.title ?: "this reminder"}\"?")
                    .setPositiveButton("Delete") { _, _ ->
                        actionListener?.onDelete(item)
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            dialog.show()
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<TimelineItem>() {
        override fun areItemsTheSame(oldItem: TimelineItem, newItem: TimelineItem): Boolean {
            return oldItem.id != null && oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TimelineItem, newItem: TimelineItem): Boolean {
            return oldItem == newItem
        }
    }
}
