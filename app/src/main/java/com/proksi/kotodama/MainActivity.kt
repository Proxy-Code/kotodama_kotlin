package com.proksi.kotodama

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
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


        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController= navHostFragment.navController


        askNotificationPermission()
        setupBottomMenu()



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

        MyFirebaseMessagingService.getToken()


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

    private fun setTabUnselected(tab: LinearLayout, icon: ImageView, text: TextView) {
        icon.setColorFilter(ContextCompat.getColor(this, R.color.unselected_nav), PorterDuff.Mode.SRC_IN)
        text.setTextColor(ContextCompat.getColor(this, R.color.unselected_nav))
    }

    private fun setTabSelected(tab: LinearLayout, icon: ImageView, text: TextView) {
        icon.setColorFilter(ContextCompat.getColor(this, R.color.main_purple), PorterDuff.Mode.SRC_IN)
        text.setTextColor(ContextCompat.getColor(this, R.color.main_purple))
    }
    private fun resetTabSelection(binding: ActivityMainBinding) {
        setTabUnselected(binding.navHome,binding.navHomeImg,binding.navHomeText)
        setTabUnselected(binding.navLibrary, binding.navLibraryImg, binding.navLibraryText)
        setTabUnselected(binding.navSettings, binding.navSettingsImg, binding.navSettingsText)
    }
    private fun setupBottomMenu() {
        navController.addOnDestinationChangedListener {_, destination, _ ->
            resetTabSelection(binding)
            when (destination.id) {
                R.id.homeFragment -> setTabSelected(
                    binding.navHome,
                    binding.navHomeImg,
                    binding.navHomeText
                )

                R.id.libraryFragment -> setTabSelected(
                    binding.navLibrary,
                    binding.navLibraryImg,
                    binding.navLibraryText
                )

                R.id.settingsFragment-> setTabSelected(
                    binding.navSettings,
                    binding.navSettingsImg,
                    binding.navSettingsText
                )
            }

            binding.navHome.setOnClickListener {
                navController.navigate(R.id.homeFragment)}

            binding.navLibrary.setOnClickListener {
                navController.navigate(R.id.libraryFragment)}

            binding.navSettings.setOnClickListener {
                navController.navigate(R.id.settingsFragment)}

            binding.bottomNavigationView.visibility = if (
                destination.id == R.id.voiceLabFormatFragment ||
                destination.id == R.id.voiceLabNameFragment ||
                destination.id == R.id.voiceLabPhotoFragment||
                destination.id == R.id.voiceLabLoadingFragment ||
                destination.id == R.id.recordVoiceFragment ||
                destination.id == R.id.customizeFragment ||
                destination.id == R.id.referFragment  ||
                destination.id == R.id.languageFragment
            ) View.GONE else View.VISIBLE



        }
    }


}
