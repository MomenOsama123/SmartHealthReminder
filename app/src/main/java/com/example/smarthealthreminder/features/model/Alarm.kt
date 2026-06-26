package com.example.smarthealthreminder.features.model

import android.os.Parcel
import android.os.Parcelable

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
    var cognitiveLockEnabled: Boolean = false,
    var status: String = "PENDING",
    var dosage: String? = null,
    var instructions: String? = null,
    var userId: Int = 0
) : Parcelable {

    constructor(parcel: Parcel) : this(
        id = parcel.readString(),
        label = parcel.readString(),
        time = parcel.readString(),
        amPm = parcel.readString(),
        category = parcel.readString(),
        isActive = parcel.readByte() != 0.toByte(),
        repeatDays = parcel.readString(),
        sound = parcel.readString(),
        vibrationEnabled = parcel.readByte() != 0.toByte(),
        gradualVolume = parcel.readByte() != 0.toByte(),
        autoSnoozeMinutes = parcel.readInt(),
        cognitiveLockEnabled = parcel.readByte() != 0.toByte(),
        status = parcel.readString() ?: "PENDING",
        dosage = parcel.readString(),
        instructions = parcel.readString(),
        userId = parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(label)
        parcel.writeString(time)
        parcel.writeString(amPm)
        parcel.writeString(category)
        parcel.writeByte(if (isActive) 1 else 0)
        parcel.writeString(repeatDays)
        parcel.writeString(sound)
        parcel.writeByte(if (vibrationEnabled) 1 else 0)
        parcel.writeByte(if (gradualVolume) 1 else 0)
        parcel.writeInt(autoSnoozeMinutes)
        parcel.writeByte(if (cognitiveLockEnabled) 1 else 0)
        parcel.writeString(status)
        parcel.writeString(dosage)
        parcel.writeString(instructions)
        parcel.writeInt(userId)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Alarm> {
        override fun createFromParcel(parcel: Parcel): Alarm = Alarm(parcel)
        override fun newArray(size: Int): Array<Alarm?> = arrayOfNulls(size)
    }

    // Convenience constructor without id
    constructor(label: String, time: String, amPm: String, category: String) : this() {
        this.label = label
        this.time = time
        this.amPm = amPm
        this.category = category
        this.isActive = true
    }
}
