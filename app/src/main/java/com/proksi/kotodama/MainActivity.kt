package com.proksi.kotodama

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.proksi.kotodama.fragments.LibraryFragment
import com.kotodama.tts.R
import com.kotodama.tts.databinding.ActivityMainBinding
import com.proksi.kotodama.dataStore.DataStoreManager
import com.proksi.kotodama.fragments.SettingsFragment
import kotlinx.coroutines.launch
import com.appsflyer.AppsFlyerLib


class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var dataStoreManager: DataStoreManager
    private var isSubscribed: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_Kotodama)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        dataStoreManager = DataStoreManager


        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController= navHostFragment.navController

        binding.bottomNavigationView.setupWithNavController(navController)
        binding.bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.home_bottom -> navController.navigate(R.id.homeFragment)
                R.id.cover_bottom -> {
                    navController.navigate(R.id.libraryFragment)
                }
                R.id.settings_bottom -> navController.navigate(R.id.settingsFragment)
            }
            true
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.voiceLabFormatFragment -> binding.bottomNavigationView.visibility = View.GONE
                R.id.voiceLabNameFragment -> binding.bottomNavigationView.visibility = View.GONE
                R.id.voiceLabPhotoFragment -> binding.bottomNavigationView.visibility = View.GONE
                R.id.recordVoiceFragment -> binding.bottomNavigationView.visibility = View.GONE
                R.id.customizeFragment -> binding.bottomNavigationView.visibility = View.GONE
                R.id.voiceLabLoadingFragment -> binding.bottomNavigationView.visibility = View.GONE

                else -> binding.bottomNavigationView.visibility = View.VISIBLE
            }
            when (destination.id) {
                R.id.homeFragment -> binding.bottomNavigationView.menu.findItem(R.id.home_bottom).isChecked =
                    true

                R.id.libraryFragment -> binding.bottomNavigationView.menu.findItem(
                    R.id.cover_bottom
                ).isChecked = true

                R.id.settingsFragment -> binding.bottomNavigationView.menu.findItem(R.id.settings_bottom).isChecked =
                    true
            }

        }


        val appsFlyer = AppsFlyerLib.getInstance()
        val apiKey = getString(R.string.appsflyer_api_key)
        appsFlyer.init(apiKey, null, this)
        appsFlyer.start(this)

    }

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val currentFragment = navHostFragment.childFragmentManager.primaryNavigationFragment

        if (currentFragment is LibraryFragment) {
            navController.navigate(R.id.homeFragment)
        } else if (currentFragment is SettingsFragment) {
            navController.navigate(R.id.homeFragment)
        }else{
            super.onBackPressed()
        }
    }

    fun setBottomNavigationVisibility(isVisible: Boolean) {
        if (isVisible) {
            binding.bottomNavigationView.visibility = View.VISIBLE
        } else {
            binding.bottomNavigationView.visibility = View.GONE
        }
    }


}
