package com.example.smarthealthreminder.features.model

import android.os.Parcel
import android.os.Parcelable

data class TimelineItem(
    var id: String? = null,
    var month: String? = null,
    var day: String? = null,
    var date: String? = null,
    var title: String? = null,
    var description: String? = null,
    var category: String? = null,
    var time: String? = null,
    var status: String? = null,
    var statusColor: Int = 0
) : Parcelable {

    constructor(parcel: Parcel) : this(
        id = parcel.readString(),
        month = parcel.readString(),
        day = parcel.readString(),
        date = parcel.readString(),
        title = parcel.readString(),
        description = parcel.readString(),
        category = parcel.readString(),
        time = parcel.readString(),
        status = parcel.readString(),
        statusColor = parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(month)
        parcel.writeString(day)
        parcel.writeString(date)
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeString(category)
        parcel.writeString(time)
        parcel.writeString(status)
        parcel.writeInt(statusColor)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<TimelineItem> {
        override fun createFromParcel(parcel: Parcel): TimelineItem = TimelineItem(parcel)
        override fun newArray(size: Int): Array<TimelineItem?> = arrayOfNulls(size)
    }

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
