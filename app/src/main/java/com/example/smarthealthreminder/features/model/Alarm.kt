package com.example.smarthealthreminder.features.model

data class Alarm(
    var id: String? = null,
    var label: String? = null,
    var time: String? = null,
    var amPm: String? = null,
    var category: String? = null,
    var isActive: Boolean = true,
    var repeatDays: String? = null,
    var sound: String? = null,
    var vibrationEnabled: Boolean = false,
    var gradualVolume: Boolean = false,
    var autoSnoozeMinutes: Int = 0,
    var cognitiveLockEnabled: Boolean = false
) {
    constructor(label: String, time: String, amPm: String, category: String) : this() {
        this.label = label
        this.time = time
        this.amPm = amPm
        this.category = category
        this.isActive = true
    }
}