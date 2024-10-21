package com.proksi.kotodama.models

import com.google.firebase.Timestamp

data class VoiceModel (
    val name:String,
    var id:String,
    val imageUrl:String,
    val createdAt: Timestamp,
    val model_name:String,
    val category: List<String>,
    val allTimeCounter: Int = 0,
    val weeklyCounter: Int = 0, // Sayısal bir değer olarak tanımladığınızdan emin olun
    val charUsedCount: Int = 0,
    val isClone: Boolean = false

){
    constructor() : this("", "", "", Timestamp.now(), "",listOf(),0,0,0,false,)
}