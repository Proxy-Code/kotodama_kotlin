package com.proksi.kotodama.models

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.kotodama.tts.R


class TTSSwitch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var isTTSMode = true

    private lateinit var ttsButton: TextView
    private lateinit var stsButton: TextView
    private lateinit var selectedIndicator: View

    var onModeChanged: ((Boolean) -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_tts_switch, this, true)
        initViews()
        setupClickListeners()
        updateUI()
    }

    private fun initViews() {
        ttsButton = findViewById(R.id.ttsButton)
        stsButton = findViewById(R.id.stsButton)
        selectedIndicator = findViewById(R.id.selectedIndicator)
    }

    private fun setupClickListeners() {
        ttsButton.setOnClickListener {
            switchToTTS()
        }

        stsButton.setOnClickListener {
            switchToSTS()
        }
    }

    private fun switchToTTS() {
        if (!isTTSMode) {
            isTTSMode = true
            updateUI()
            onModeChanged?.invoke(true)
        }
    }

    private fun switchToSTS() {
        if (isTTSMode) {
            isTTSMode = false
            updateUI()
            onModeChanged?.invoke(false)
        }
    }

    private fun updateUI() {
        if (isTTSMode) {
            // TTS modu aktif
            ttsButton.setTextColor(ContextCompat.getColor(context, android.R.color.white))
            stsButton.setTextColor(ContextCompat.getColor(context, R.color.black))

            // Indicator'ı TTS butonuna taşı
            val params = selectedIndicator.layoutParams as LayoutParams
            params.startToStart = ttsButton.id
            params.endToEnd = ttsButton.id
            selectedIndicator.layoutParams = params

        } else {
            // STS modu aktif
            stsButton.setTextColor(ContextCompat.getColor(context, android.R.color.white))
            ttsButton.setTextColor(ContextCompat.getColor(context, R.color.black))

            // Indicator'ı STS butonuna taşı
            val params = selectedIndicator.layoutParams as LayoutParams
            params.startToStart = stsButton.id
            params.endToEnd = stsButton.id
            selectedIndicator.layoutParams = params
        }

        selectedIndicator.requestLayout()
    }

    fun setMode(isTTS: Boolean) {
        if (isTTSMode != isTTS) {
            isTTSMode = isTTS
            updateUI()
        }
    }

    fun getCurrentMode(): Boolean = isTTSMode
}