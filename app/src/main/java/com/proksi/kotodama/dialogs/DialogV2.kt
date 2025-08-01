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

@SuppressLint("MissingInflatedId")
class DialogV2(
    context: Context,
) : Dialog(context) {

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_v2_info)
        setCancelable(true)

        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.CENTER) // Ortaya alıyoruz
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val closeButton= findViewById<TextView>(R.id.close_btn)

        closeButton.setOnClickListener {
            dismiss()
        }


    }
}
