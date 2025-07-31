package com.proksi.kotodama.models

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserLibrary(
    val createdAt: Timestamp?, // Firebase Timestamp
    val image: String?,
    var isDefault: Boolean,
    var isGenerating: Boolean,
    val name: String,
    val soundUrl: String,
    val sound_sample_id: String,
    var text: String,
    var id:String,
    var type:String,
    var images:List<String>?

) : Parcelable {

    constructor() : this(null, "", true, false, "", "", "", "","","",null)


}
