package com.example.smarthealthreminder.features.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.model.Alarm

class AlarmAdapter : RecyclerView.Adapter<AlarmAdapter.ViewHolder>() {

    private val alarms = ArrayList<Alarm>()
    private var toggleListener: OnAlarmToggleListener? = null
    private var clickListener: OnAlarmClickListener? = null

    interface OnAlarmToggleListener {
        fun onToggle(alarm: Alarm, isActive: Boolean)
    }

    interface OnAlarmClickListener {
        fun onAlarmClick(alarm: Alarm)
    }

    fun setOnAlarmToggleListener(listener: OnAlarmToggleListener) {
        this.toggleListener = listener
    }

    fun setOnAlarmClickListener(listener: OnAlarmClickListener) {
        this.clickListener = listener
    }

    fun setAlarms(alarms: List<Alarm>) {
        this.alarms.clear()
        this.alarms.addAll(alarms)
        notifyDataSetChanged()
    }

    fun addAlarm(alarm: Alarm) {
        this.alarms.add(alarm)
        notifyItemInserted(alarms.size - 1)
    }

    fun updateAlarm(alarm: Alarm) {
        val index = alarms.indexOfFirst { it.id == alarm.id }
        if (index != -1) {
            alarms[index] = alarm
            notifyItemChanged(index)
        }
    }

    fun removeAlarm(alarmId: String) {
        val index = alarms.indexOfFirst { it.id == alarmId }
        if (index != -1) {
            alarms.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun getAlarms(): List<Alarm> = alarms.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alarm, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(alarms[position])
    }

    override fun getItemCount(): Int = alarms.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        private val tvAmPm: TextView = itemView.findViewById(R.id.tv_am_pm)
        private val tvRepeatDays: TextView = itemView.findViewById(R.id.tv_repeat_days)
        private val chipCategory: Chip = itemView.findViewById(R.id.chip_category)
        private val switchAlarm: SwitchCompat = itemView.findViewById(R.id.switch_alarm)

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    clickListener?.onAlarmClick(alarms[position])
                }
            }
        }

        fun bind(alarm: Alarm) {
            tvTime.text = alarm.time ?: "--:--"
            tvAmPm.text = alarm.amPm ?: ""
            tvRepeatDays.text = alarm.repeatDays ?: "No repeat"
            chipCategory.text = alarm.category ?: "General"
            switchAlarm.setOnCheckedChangeListener(null)
            switchAlarm.isChecked = alarm.isActive

            switchAlarm.setOnCheckedChangeListener { _, isChecked ->
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    alarms[position].isActive = isChecked
                    toggleListener?.onToggle(alarms[position], isChecked)
                }
            }
        }
    }
}
