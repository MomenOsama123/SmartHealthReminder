package com.example.smarthealthreminder.features.model


data class TimelineItem(
    var id: String? = null,
    var month: String? = null,
    var day: String? = null,
    var title: String? = null,
    var description: String? = null,
    var category: String? = null,
    var time: String? = null,
    var status: String? = null, // PENDING, DONE, MISSED
    var statusColor: Int = 0
) {
    constructor(month: String, day: String, title: String, description: String,
                category: String, time: String, status: String) : this() {
        this.month = month
        this.day = day
        this.title = title
        this.description = description
        this.category = category
        this.time = time
        this.status = status
    }
}