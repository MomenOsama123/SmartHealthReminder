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
) {
    /** True if this day has ANY content that should show a dot */
    val hasContent: Boolean
        get() = hasEvents || hasNotes || hasReports || hasScheduleEntries
}

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

        // Empty padding cell
        if (day.dayNumber == 0) {
            holder.tvDay.text = ""
            holder.tvDay.background = null
            holder.tvDay.alpha = 0f
            holder.viewDot.visibility = View.INVISIBLE
            holder.itemView.isClickable = false
            return
        }

        holder.tvDay.text = day.dayNumber.toString()
        holder.tvDay.alpha = if (day.isCurrentMonth) 1f else 0.35f
        holder.itemView.isClickable = true

        val isSelected = day.date == selectedDate

        when {
            isSelected -> {
                // Filled white circle, primary-colored text
                holder.tvDay.setBackgroundResource(R.drawable.circle_white)
                holder.tvDay.setTextColor(ContextCompat.getColor(context, R.color.primary))
                holder.tvDay.typeface = Typeface.DEFAULT_BOLD
                holder.tvDay.paintFlags = 0
                // Hide dot when selected — the circle already highlights it
                holder.viewDot.visibility = View.INVISIBLE
            }
            day.isToday -> {
                // Today: no background, bold + underline in white
                holder.tvDay.background = null
                holder.tvDay.setTextColor(ContextCompat.getColor(context, R.color.white))
                holder.tvDay.typeface = Typeface.DEFAULT_BOLD
                holder.tvDay.paintFlags =
                    holder.tvDay.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
                holder.viewDot.visibility =
                    if (day.hasContent) View.VISIBLE else View.INVISIBLE
            }
            else -> {
                // Normal day
                holder.tvDay.background = null
                holder.tvDay.paintFlags = 0
                holder.tvDay.setTextColor(
                    ContextCompat.getColor(
                        context,
                        if (day.isCurrentMonth) R.color.white else R.color.text_on_primary
                    )
                )
                holder.tvDay.typeface = Typeface.DEFAULT
                holder.viewDot.visibility =
                    if (day.hasContent && day.isCurrentMonth) View.VISIBLE else View.INVISIBLE
            }
        }

        // Dot color: notes = soft yellow, everything else = white dot
        if (holder.viewDot.visibility == View.VISIBLE) {
            val dotColorRes = if (day.hasNotes && !day.hasEvents && !day.hasReports && !day.hasScheduleEntries)
                R.color.pending   // note-only days get a different dot
            else
                R.color.white
            holder.viewDot.backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(context, dotColorRes)
                )
        }

        holder.itemView.setOnClickListener {
            if (day.isCurrentMonth) {
                val old = selectedDate
                selectedDate = day.date
                val oldIndex = days.indexOfFirst { it.date == old }
                if (oldIndex != -1) notifyItemChanged(oldIndex)
                notifyItemChanged(position)
                onDayClick(day.date)
            }
        }
    }

    override fun getItemCount() = days.size

    fun setDays(newDays: List<CalendarDay>, selected: String) {
        days = newDays
        selectedDate = selected
        notifyDataSetChanged()
    }
}
