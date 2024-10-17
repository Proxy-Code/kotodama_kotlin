package com.proksi.kotodama

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import com.kotodama.tts.R

open class BaseFragment : Fragment() {

    open fun configureStatusBar(isLightStatusBar: Boolean = true, statusBarColor: Int? = null) {
        activity?.window?.apply {
            this.statusBarColor = statusBarColor ?: ContextCompat.getColor(requireContext(), R.color.main_bg)
            WindowInsetsControllerCompat(this, decorView).apply {
                isAppearanceLightStatusBars = isLightStatusBar
                show(WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureStatusBar()
    }
}