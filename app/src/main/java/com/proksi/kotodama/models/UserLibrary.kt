package com.proksi.kotodama.models

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.Timestamp

data class UserLibrary(
    val createdAt: Timestamp?, // Firebase Timestamp
    val image: String,
    var isDefault: Boolean,
    var isGenerating: Boolean,
    val name: String,
    val soundUrl: String,
    val sound_sample_id: String,
    var text: String,
    var id:String
) : Parcelable {

    constructor() : this(null, "", true, false, "", "", "", "","")

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(createdAt?.seconds ?: 0L) // Firebase Timestamp'in seconds kısmını yazıyoruz
        dest.writeString(image)
        dest.writeByte(if (isDefault) 1 else 0)
        dest.writeByte(if (isGenerating) 1 else 0)
        dest.writeString(name)
        dest.writeString(soundUrl)
        dest.writeString(sound_sample_id)
        dest.writeString(text)
        dest.writeString(id)
    }

    companion object CREATOR : Parcelable.Creator<UserLibrary> {
        override fun createFromParcel(parcel: Parcel): UserLibrary {
            return UserLibrary(
                Timestamp(parcel.readLong(), 0), // Timestamp'i geri alıyoruz
                parcel.readString() ?: "",
                parcel.readByte() != 1.toByte(),
                parcel.readByte() != 0.toByte(),
                parcel.readString() ?: "",
                parcel.readString() ?: "",
                parcel.readString() ?: "",
                parcel.readString() ?: "",
                parcel.readString() ?: ""
            )
        }

        override fun newArray(size: Int): Array<UserLibrary?> {
            return arrayOfNulls(size)
        }
    }
}
