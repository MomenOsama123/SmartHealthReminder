package com.example.smarthealthreminder.features.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.model.ScheduleItem

class ScheduleAdapter(
    private val items: MutableList<ScheduleItem> = mutableListOf()
) : RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder>() {

    class ScheduleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tv_title)
        val date: TextView = view.findViewById(R.id.tv_date)
        val time: TextView = view.findViewById(R.id.tv_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_schedule, parent, false)
        return ScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        val item = items[position]

        holder.title.text = item.title
        holder.date.text = item.date
        holder.time.text = item.time
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<ScheduleItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}