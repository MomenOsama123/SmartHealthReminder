package com.example.smarthealthreminder.features.model


data class Reminder(
    var id: String? = null,
    var title: String? = null,
    var description: String? = null,
    var category: String? = null, // Medicine, Appointment, Task, Custom
    var date: String? = null,
    var time: String? = null,
    var priority: String? = null, // Low, Medium, High
    var status: String? = "Pending", // Pending, Completed, Missed
    var isRecurring: Boolean = false,
    var recurrenceType: String? = null,
    var vibrationEnabled: Boolean = false,
    var earlyNotification: Boolean = false,
    var earlyNotificationMinutes: Int = 0
) {
    constructor(title: String, description: String, category: String,
                date: String, time: String, priority: String) : this() {
        this.title = title
        this.description = description
        this.category = category
        this.date = date
        this.time = time
        this.priority = priority
        this.status = "Pending"
    }
}