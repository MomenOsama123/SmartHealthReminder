package com.example.smarthealthreminder.features.search

import com.example.smarthealthreminder.features.data.local.entity.AlarmEntity
import com.example.smarthealthreminder.features.data.local.entity.ReminderEntity

sealed class SearchResult {
    data class Reminder(val entity: ReminderEntity) : SearchResult()
    data class Alarm(val entity: AlarmEntity) : SearchResult()

    val id: String
        get() = when (this) {
            is Reminder -> entity.id
            is Alarm -> entity.id
        }

    val title: String
        get() = when (this) {
            is Reminder -> entity.title ?: ""
            is Alarm -> entity.label ?: ""
        }

    val time: String
        get() = when (this) {
            is Reminder -> entity.time ?: ""
            is Alarm -> entity.time ?: ""
        }

    val category: String
        get() = when (this) {
            is Reminder -> entity.category ?: ""
            is Alarm -> entity.category ?: ""
        }
}
