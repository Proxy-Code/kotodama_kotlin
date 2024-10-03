package com.proksi.kotodama.models

import com.google.firebase.Timestamp

data class VoiceModel (
    val name:String,
    var id:String,
    val imageUrl:String,
    val createdAt: Timestamp,
    val model_name:String,
    val category: List<String>,
    val allTimeCounter: Number,
    val weeklyCounter: Number,
    val charUsedCount: Number
){
    constructor() : this("", "", "", Timestamp.now(), "",listOf(),0,0,0)
}