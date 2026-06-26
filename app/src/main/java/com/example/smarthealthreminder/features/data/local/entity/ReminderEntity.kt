package com.example.smarthealthreminder.features.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey
    val id: String,

    val title: String,
    val description: String? = null,
    val category: String? = null,
    val date: String? = null,
    val time: String? = null,
    val priority: String? = null,
    val status: String = "Pending",
    val totalQuantity: Int = 0,
    val dosagePerTime: Int = 1,
    val lowStockThreshold: Int = 3,

    @ColumnInfo(name = "is_recurring")
    val isRecurring: Boolean = false,

    @ColumnInfo(name = "recurrence_type")
    val recurrenceType: String? = null,

    @ColumnInfo(name = "vibration_enabled")
    val vibrationEnabled: Boolean = false,

    @ColumnInfo(name = "early_notification")
    val earlyNotification: Boolean = false,

    @ColumnInfo(name = "early_notification_minutes")
    val earlyNotificationMinutes: Int = 0
)
