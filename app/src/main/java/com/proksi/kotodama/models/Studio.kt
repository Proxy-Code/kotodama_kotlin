package com.proksi.kotodama.models

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class DraftFileModel(
    val id: String,
    val createdAt: Timestamp?,
    val exportUrl: String,
    val isGenerating: Boolean,
    val name: String,
    val libraryTaskId: String,
    val processedAt: Timestamp?,
    val conversation: List<ConversationModel>?
) : Parcelable


@Parcelize
data class ConversationModel(
    var id: String="",
    val createdAt: Timestamp? = null,
    val imageUrl: String? = null,
    var isGenerating: Boolean = false,
    val order: Int = 0,
    val processedAt: Timestamp? = null,
    val requestIds: List<String> = emptyList(),
    val soundUrl: String? = null,
    val soundSampleId: String? = null,
    val text: String? = null
) : Parcelable


