package com.proksi.kotodama

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.kotodama.tts.R
import com.proksi.kotodama.dataStore.DataStoreManager
import com.proksi.kotodama.models.RCPlacement
import com.proksi.kotodama.utils.PaywallUtility

abstract class BaseFragment : Fragment() {

    protected abstract val paywallComposeView: ComposeView

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

    protected fun showPaywall(placement: RCPlacement) {
        PaywallUtility.showPaywall(
            context = requireContext(),
            composeView = paywallComposeView,
            placement = placement,
            dataStoreManager = DataStoreManager,
            lifecycleScope = lifecycleScope
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        paywallComposeView.disposeComposition()
    }
}