package com.proksi.kotodama.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.proksi.kotodama.models.ConversationModel
import com.proksi.kotodama.models.DraftFileModel
import com.proksi.kotodama.models.VoiceModel

class StudioViewModel: ViewModel() {

    private val _studioId = MutableLiveData<String>()
    val studioId: LiveData<String> = _studioId

    private val _text = MutableLiveData<String>()
    val text: LiveData<String> = _text

    private val _conversation = MutableLiveData<ConversationModel>()
    val conversation: LiveData<ConversationModel> = _conversation

    private val _draft = MutableLiveData<DraftFileModel>()
    val draft: LiveData<DraftFileModel> = _draft

    private val _voice = MutableLiveData<VoiceModel>()
    val voice: LiveData<VoiceModel> = _voice

    private val _draftNames = mutableSetOf<String>()
    val draftNames: Set<String> get() = _draftNames

    fun setDraftNames(names: List<String>) {
        _draftNames.clear()
        _draftNames.addAll(names)
    }

    fun setStudioId(item:String) {
        _studioId.value = item
    }

    fun getStudioId(): String? {
        return _studioId.value
    }

    fun setConversation(item:ConversationModel) {
        _conversation.value = item
    }

    fun getConversation(): ConversationModel? {
        return _conversation.value
    }

    fun setDraft(item:DraftFileModel) {
        _draft.value = item
    }

    fun getDraft(): DraftFileModel? {
        return _draft.value
    }

    fun setVoice(item:VoiceModel) {
        _voice.value = item
    }

    fun getVoice(): VoiceModel? {
        return _voice.value
    }

    fun setText(item:String) {
        _text.value = item
    }

    fun getText(): String? {
        return _text.value
    }





}