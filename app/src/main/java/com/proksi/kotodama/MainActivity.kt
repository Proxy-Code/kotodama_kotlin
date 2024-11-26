package com.proksi.kotodama

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.proksi.kotodama.fragments.LibraryFragment
import com.kotodama.tts.R
import com.kotodama.tts.databinding.ActivityMainBinding
import com.proksi.kotodama.dataStore.DataStoreManager
import com.proksi.kotodama.fragments.SettingsFragment
import com.appsflyer.AppsFlyerLib
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging

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

        askNotificationPermission()

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
                R.id.referFragment -> binding.bottomNavigationView.visibility = View.GONE

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

        val sharedPreferences: SharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val lastOfferKey = "lastOfferKey"

        if (!sharedPreferences.contains(lastOfferKey)) {
            val editor = sharedPreferences.edit()
            editor.putLong(lastOfferKey, System.currentTimeMillis() / 1000)
            editor.apply()
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

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,android.Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }else{
            FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("FIREBASE", "Fetching FCM registration token failed", task.exception)
                    return@OnCompleteListener
                }

                val token = task.result
                Log.d("token", "askNotificationPermission: $token")
            })
        }
    }
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("FIREBASE", "Fetching FCM registration token failed", task.exception)
                    return@OnCompleteListener
                }

                val token = task.result
                Log.d("token", "askNotificationPermission: $token")
            })
        } else {
            // TODO: Inform user that that your app will not show notifications.
        }
    }


}
