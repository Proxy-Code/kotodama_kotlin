package com.proksi.kotodama.models

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class VoiceModel (
    val name:String,
    var id:String,
    val imageUrl:String,
    val createdAt: Timestamp,
    val model_name:String,
    val category: List<String>,
    val allTimeCounter: Int = 0,
    val weeklyCounter: Int = 0,
    val charUsedCount: Int = 0,
    val isClone: Boolean = false

): Parcelable {
    constructor() : this("", "", "", Timestamp.now(), "",listOf(),0,0,0,false,)
}