package com.example.smarthealthreminder.features.model

import android.os.Parcel
import android.os.Parcelable

data class Reminder(
    var id: String? = null,
    var userId: Int? = null,  // ✅ جديد
    var title: String? = null,
    var description: String? = null,
    var category: String? = null,
    var date: String? = null,
    var time: String? = null,
    var priority: String? = null,
    var status: String? = "Pending",
    var isRecurring: Boolean = false,
    var recurrenceType: String? = null,
    var vibrationEnabled: Boolean = false,
    var earlyNotification: Boolean = false,
    var earlyNotificationMinutes: Int = 0
) : Parcelable {

    constructor(parcel: Parcel) : this(
        id = parcel.readString(),
        userId = parcel.readInt(),  // ✅ جديد
        title = parcel.readString(),
        description = parcel.readString(),
        category = parcel.readString(),
        date = parcel.readString(),
        time = parcel.readString(),
        priority = parcel.readString(),
        status = parcel.readString(),
        isRecurring = parcel.readByte() != 0.toByte(),
        recurrenceType = parcel.readString(),
        vibrationEnabled = parcel.readByte() != 0.toByte(),
        earlyNotification = parcel.readByte() != 0.toByte(),
        earlyNotificationMinutes = parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeInt(userId ?: -1)  // ✅ جديد
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeString(category)
        parcel.writeString(date)
        parcel.writeString(time)
        parcel.writeString(priority)
        parcel.writeString(status)
        parcel.writeByte(if (isRecurring) 1 else 0)
        parcel.writeString(recurrenceType)
        parcel.writeByte(if (vibrationEnabled) 1 else 0)
        parcel.writeByte(if (earlyNotification) 1 else 0)
        parcel.writeInt(earlyNotificationMinutes)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Reminder> {
        override fun createFromParcel(parcel: Parcel): Reminder = Reminder(parcel)
        override fun newArray(size: Int): Array<Reminder?> = arrayOfNulls(size)
    }

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