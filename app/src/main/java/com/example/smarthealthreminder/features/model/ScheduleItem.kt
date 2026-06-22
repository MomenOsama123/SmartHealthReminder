package com.example.smarthealthreminder.features.model

data class ScheduleItem(
    val id: String = "",
    val title: String,
    val date: String,
    val time: String,
    val category: String = "General",
    val priority: String = "NORMAL",
    val status: String = "Pending",
    val earlyNotification: Boolean = false,
    val earlyNotificationMinutes: Int = 0,
    val isAlarm: Boolean = false,
    val itemType: String = TYPE_REMINDER
) {
    companion object {
        const val TYPE_REMINDER = "reminder"
        const val TYPE_ALARM = "alarm"
        const val TYPE_SCHEDULE_ENTRY = "schedule_entry"
        const val TYPE_REPORT = "report"
        const val TYPE_NOTE = "note"
    }
}
