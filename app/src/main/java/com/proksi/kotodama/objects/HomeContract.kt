package com.proksi.kotodama.objects

// HomeContract.kt
data class VoiceUI(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val isClone: Boolean
)

data class HomeUiState(
    val isLoading: Boolean = false,
    val isSubscribed: Boolean = false,
    val isStsMode: Boolean = false,
    val text: String = "",
    val languageCode: String = "en",
    val languageName: String = "English",
    val selectedVoice: VoiceUI? = null,
    val voices: List<VoiceUI> = emptyList(),
    val remainingChars: Int = 150,
    val tokenCounter: Int = 3,
    val remainingRights: Int? = null,
    val showSlotPaywall: Boolean = false
)

sealed interface HomeIntent {
    data class TextChanged(val text: String): HomeIntent
    data class ToggleMode(val sts: Boolean): HomeIntent
    data class VoiceSelected(val voice: VoiceUI): HomeIntent
    data class UploadPicked(val uri: android.net.Uri): HomeIntent
    data object CreateTapped: HomeIntent
    data object DoneTapped: HomeIntent
    data object DeleteTapped: HomeIntent
    data object OpenReferTapped: HomeIntent
    data object ShowPaywallHome: HomeIntent
    data object LoadMoreVoices: HomeIntent
}
