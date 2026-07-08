package com.example.smarthealthreminder.features.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.data.local.entity.MedicationPlanEntity
import com.example.smarthealthreminder.features.util.RecurrenceHelper
import com.google.android.material.chip.Chip
import android.widget.TextView

class MedicationPlansListAdapter :
    ListAdapter<MedicationPlanEntity, MedicationPlansListAdapter.ViewHolder>(DiffCallback()) {

    private var onPlanClickListener: ((MedicationPlanEntity) -> Unit)? = null

    fun setOnPlanClickListener(listener: (MedicationPlanEntity) -> Unit) {
        onPlanClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medication_plan_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tv_row_plan_name)
        private val tvSubtitle: TextView = itemView.findViewById(R.id.tv_row_plan_subtitle)
        private val chipStatus: Chip = itemView.findViewById(R.id.chip_row_plan_status)

        fun bind(plan: MedicationPlanEntity) {
            tvName.text = plan.medicineName

            val today = RecurrenceHelper.getTodayString()
            val isFinished = !plan.isActive || plan.endDate < today

            val endMillis = RecurrenceHelper.parseDateTimeMillis(plan.endDate, "23:59")
            val daysLeft = if (endMillis != null) {
                val diff = endMillis - System.currentTimeMillis()
                (diff / (24 * 60 * 60 * 1000L)).toInt().coerceAtLeast(0)
            } else 0

            tvSubtitle.text = if (isFinished) {
                "${plan.timesPerDay}x daily · Finished"
            } else {
                "${plan.timesPerDay}x daily · $daysLeft days left"
            }

            if (isFinished) {
                chipStatus.text = "Finished"
                chipStatus.setTextColor(itemView.context.getColor(R.color.text_secondary))
                chipStatus.chipBackgroundColor = itemView.context.getColorStateList(R.color.surface_variant)
            } else {
                chipStatus.text = "Active"
                chipStatus.setTextColor(itemView.context.getColor(R.color.success))
                chipStatus.chipBackgroundColor = itemView.context.getColorStateList(R.color.success_light)
            }

            itemView.setOnClickListener {
                onPlanClickListener?.invoke(plan)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<MedicationPlanEntity>() {
        override fun areItemsTheSame(oldItem: MedicationPlanEntity, newItem: MedicationPlanEntity) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: MedicationPlanEntity, newItem: MedicationPlanEntity) =
            oldItem == newItem
    }
}