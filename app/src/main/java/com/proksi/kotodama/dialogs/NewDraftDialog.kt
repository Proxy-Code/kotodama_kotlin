package com.proksi.kotodama.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.kotodama.tts.R
import com.proksi.kotodama.dataStore.DataStoreManager
import com.proksi.kotodama.viewmodel.StudioViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
@SuppressLint("MissingInflatedId")
class DialogNewDraft(
    context: Context,
    private val onCreateClick: (String) -> Unit // callback fonksiyonu
) : Dialog(context) {

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_new_draft)
        setCancelable(true)

        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.CENTER) // Ortaya alıyoruz
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val editText = findViewById<EditText>(R.id.inputField)
        val createButton = findViewById<TextView>(R.id.createBtn)


        createButton.setOnClickListener {
            val text = editText.text.toString()
            onCreateClick(text)
            dismiss()
        }
    }
}
