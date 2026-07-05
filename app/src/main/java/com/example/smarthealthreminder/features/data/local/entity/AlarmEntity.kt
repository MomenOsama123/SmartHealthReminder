package com.example.smarthealthreminder.features.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey
    val id: String,

    val label: String,
    val time: String,
    val amPm: String,
    val category: String,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "repeat_days")
    val repeatDays: String? = null,

    val sound: String? = null,

    @ColumnInfo(name = "vibration_enabled")
    val vibrationEnabled: Boolean = false,

    @ColumnInfo(name = "gradual_volume")
    val gradualVolume: Boolean = false,

    @ColumnInfo(name = "auto_snooze_minutes")
    val autoSnoozeMinutes: Int = 0,

    @ColumnInfo(name = "cognitive_lock_enabled")
    val cognitiveLockEnabled: Boolean = false,

    /** Tracks the last time this alarm fired: "Pending", "Completed", or "Snoozed" */
    @ColumnInfo(name = "last_triggered_status", defaultValue = "Pending")
    val lastTriggeredStatus: String = "Pending",

    @ColumnInfo(name = "last_snooze_minutes")
    var lastSnoozeMinutes: Int = 0,
)
