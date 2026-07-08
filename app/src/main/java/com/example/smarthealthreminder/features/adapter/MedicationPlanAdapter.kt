package com.example.smarthealthreminder.features.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.data.local.entity.MedicationPlanEntity
import com.example.smarthealthreminder.features.util.RecurrenceHelper

class MedicationPlanAdapter : ListAdapter<MedicationPlanEntity, MedicationPlanAdapter.ViewHolder>(DiffCallback()) {

    private var onPlanClickListener: ((MedicationPlanEntity) -> Unit)? = null

    fun setOnPlanClickListener(listener: (MedicationPlanEntity) -> Unit) {
        onPlanClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medication_plan_chip, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tv_plan_name)
        private val tvDaysLeft: TextView = itemView.findViewById(R.id.tv_plan_days_left)

        fun bind(plan: MedicationPlanEntity) {
            tvName.text = plan.medicineName

            val endMillis = RecurrenceHelper.parseDateTimeMillis(plan.endDate, "23:59")
            val daysLeft = if (endMillis != null) {
                val diff = endMillis - System.currentTimeMillis()
                (diff / (24 * 60 * 60 * 1000L)).toInt().coerceAtLeast(0)
            } else 0

            tvDaysLeft.text = "$daysLeft days left"

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