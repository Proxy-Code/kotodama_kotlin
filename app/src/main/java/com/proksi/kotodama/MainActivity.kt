package com.proksi.kotodama

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.proksi.kotodama.databinding.ActivityMainBinding
import com.proksi.kotodama.fragments.FilesFragment
import com.proksi.kotodama.fragments.SettingsFragment

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_Kotodama)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController= navHostFragment.navController

        binding.bottomNavigationView.setupWithNavController(navController)
        binding.bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.home_bottom -> navController.navigate(R.id.homeFragment)
                R.id.cover_bottom -> {
                    navController.navigate(R.id.nullFilesFragment)
                }
                R.id.settings_bottom -> navController.navigate(R.id.settingsFragment)
            }
            true
        }

    }

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val currentFragment = navHostFragment.childFragmentManager.primaryNavigationFragment

        if (currentFragment is FilesFragment) {
            navController.navigate(R.id.homeFragment)
        } else if (currentFragment is SettingsFragment) {
            navController.navigate(R.id.homeFragment)
        }else{
            super.onBackPressed()
        }
    }
}
