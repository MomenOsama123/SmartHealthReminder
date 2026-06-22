package com.example.smarthealthreminder.features.adapter

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthealthreminder.R
import java.text.SimpleDateFormat
import java.util.*

data class CalendarDay(
    val date: String,
    val dayNumber: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val hasEvents: Boolean,
    val hasNotes: Boolean = false,
    val hasReports: Boolean = false,
    val hasScheduleEntries: Boolean = false
)

class CalendarDayAdapter(
    private val onDayClick: (String) -> Unit
) : RecyclerView.Adapter<CalendarDayAdapter.ViewHolder>() {

    private var days = listOf<CalendarDay>()
    private var selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDay: TextView = view.findViewById(R.id.tv_day_number)
        val viewDot: View = view.findViewById(R.id.view_dot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val day = days[position]
        val context = holder.itemView.context

        if (day.dayNumber == 0) {
            holder.tvDay.text = ""
            holder.tvDay.background = null
            holder.viewDot.visibility = View.INVISIBLE
            return
        }

        holder.tvDay.text = day.dayNumber.toString()

        when {
            day.date == selectedDate -> {
                holder.tvDay.setBackgroundResource(R.drawable.circle_white)
                holder.tvDay.setTextColor(ContextCompat.getColor(context, R.color.primary))
                holder.tvDay.typeface = Typeface.DEFAULT_BOLD
            }
            day.isToday -> {
                holder.tvDay.background = null
                holder.tvDay.setTextColor(ContextCompat.getColor(context, R.color.white))
                holder.tvDay.typeface = Typeface.DEFAULT_BOLD
                holder.tvDay.paintFlags = holder.tvDay.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
            }
            !day.isCurrentMonth -> {
                holder.tvDay.background = null
                holder.tvDay.paintFlags = 0
                holder.tvDay.setTextColor(ContextCompat.getColor(context, R.color.primary_light))
                holder.tvDay.typeface = Typeface.DEFAULT
            }
            else -> {
                holder.tvDay.background = null
                holder.tvDay.paintFlags = 0
                holder.tvDay.setTextColor(ContextCompat.getColor(context, R.color.white))
                holder.tvDay.typeface = Typeface.DEFAULT
            }
        }

        holder.viewDot.visibility =
            if (day.hasEvents && day.date != selectedDate) View.VISIBLE else View.INVISIBLE

        holder.itemView.setOnClickListener {
            if (day.dayNumber != 0 && day.isCurrentMonth) {
                val old = selectedDate
                selectedDate = day.date
                val oldIndex = days.indexOfFirst { it.date == old }
                if (oldIndex != -1) notifyItemChanged(oldIndex)
                notifyItemChanged(position)
                onDayClick(day.date)
            }
        }
        val dotColor = when {
            day.hasEvents -> R.color.primary
            day.hasReports -> R.color.urgent
            day.hasNotes -> R.color.pending
            else -> R.color.primary
        }
        holder.viewDot.backgroundTintList =
            android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(holder.itemView.context, dotColor)
            )
    }

    override fun getItemCount() = days.size

    fun setDays(newDays: List<CalendarDay>, selected: String) {
        days = newDays
        selectedDate = selected
        notifyDataSetChanged()
    }
}