package com.example.smarthealthreminder.features.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.model.TimelineItem
import com.google.android.material.chip.Chip

class TimelineAdapter : RecyclerView.Adapter<TimelineAdapter.ViewHolder>() {

    private val items = ArrayList<TimelineItem>()
    private var listener: OnItemClickListener? = null

    interface OnItemClickListener {
        fun onItemClick(item: TimelineItem)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    fun setItems(items: List<TimelineItem>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    fun addItem(item: TimelineItem) {
        this.items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun updateItem(item: TimelineItem) {
        val index = items.indexOfFirst { it.id == item.id }
        if (index != -1) {
            items[index] = item
            notifyItemChanged(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_timeline, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMonth: TextView = itemView.findViewById(R.id.tv_month)
        private val tvDay: TextView = itemView.findViewById(R.id.tv_day)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        private val tvDescription: TextView = itemView.findViewById(R.id.tv_description)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
        private val chipCategory: Chip = itemView.findViewById(R.id.chip_category)
        private val statusIndicator: View = itemView.findViewById(R.id.status_indicator)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener?.onItemClick(items[position])
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

            // Set status
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
                else -> {
                    tvStatus.setBackgroundResource(R.drawable.bg_rounded_card)
                    tvStatus.setTextColor(itemView.context.getColor(R.color.pending))
                    statusIndicator.backgroundTintList =
                        itemView.context.getColorStateList(R.color.pending)
                }
            }
        }
    }
}
