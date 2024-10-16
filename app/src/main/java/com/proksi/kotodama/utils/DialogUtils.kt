package com.proksi.kotodama.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.kotodama.app.R
import com.proksi.kotodama.dataStore.DataStoreManager
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.purchaseWith
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DialogUtils {

    private lateinit var mostPackage: Package
    private lateinit var normalPackage: Package
    private lateinit var lifetimePackage: Package
    private lateinit var tenKPackage: Package
    private lateinit var fiftyKPackage: Package
    private lateinit var hundredKPackage: Package
    private lateinit var finalPackage: Package
    private lateinit var mostPopularButton: TextView
    private lateinit var normalPlanButton: TextView
    private lateinit var lifetimeButton: TextView
    private lateinit var getPremiumButton: TextView
    private lateinit var mostPopularPrice: TextView
    private lateinit var normalPlanPrice: TextView
    private lateinit var lifetimePrice: TextView
    private lateinit var tenKPrice: TextView
    private lateinit var fiftyKPrice: TextView
    private lateinit var hundredKPrice: TextView
    private lateinit var tenKTitle: TextView
    private lateinit var fiftyKTitle: TextView
    private lateinit var hundredKTitle: TextView
    private var isPremium:Boolean = true
    private var finalPrice: String = ""
    private var packageType: String = ""

    fun showPremiumDialogBox(context: Context, viewLifecycleOwner: LifecycleOwner, lifecycleScope: CoroutineScope,dataStoreManager: DataStoreManager) {

        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.dialog_premium)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()

        isPremium=true

        getPremiumButton = dialog.findViewById(R.id.getPremiumButton)
        lifetimeButton = dialog.findViewById(R.id.textView151)
        normalPlanButton = dialog.findViewById(R.id.textView152)
        mostPopularButton = dialog.findViewById(R.id.textView15)
        lifetimePrice = dialog.findViewById(R.id.lifetimePrice)
        normalPlanPrice = dialog.findViewById(R.id.normalPrice)
        mostPopularPrice = dialog.findViewById(R.id.mostPrice)
        val lifetimeLayout: ConstraintLayout = dialog.findViewById(R.id.lifetimeLayout)
        val normalLayout: ConstraintLayout = dialog.findViewById(R.id.normalLayout)
        val mostLayout: ConstraintLayout = dialog.findViewById(R.id.mostLayout)

        val layouts = listOf(lifetimeLayout, normalLayout, mostLayout)

        fun resetBackgrounds() {
            layouts.forEach { layout ->
                layout.setBackgroundResource(R.drawable.radius14_bg_white)
            }
        }
        fun setOnClickListener(layout: ConstraintLayout, type: String) {
            layout.setOnClickListener {
                resetBackgrounds()
                layout.setBackgroundResource(R.drawable.radius14_bg_white_purple)
                packageType = type
            }
        }

        setOnClickListener(lifetimeLayout, "lifetime")
        setOnClickListener(normalLayout, "normal")
        setOnClickListener(mostLayout, "most")



        fetchAndDisplayOfferings(viewLifecycleOwner)

        val closeBtnPremium = dialog.findViewById<ImageView>(R.id.closeButton)
        closeBtnPremium.setOnClickListener {
            dialog.dismiss()
            showFinalOffer(context)
        }

        dialog.findViewById<TextView>(R.id.getPremiumButton).setOnClickListener{
            selectPackage(context, packageType , lifecycleScope, dataStoreManager)
        }
    }

    fun showAddCharacterDialogBox(context: Context, viewLifecycleOwner: LifecycleOwner){
        val dialog = BottomSheetDialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setDismissWithAnimation(true)
        dialog.setContentView(R.layout.dialog_add_character)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()

        isPremium = false

        val tenKLayout: ConstraintLayout? = dialog.findViewById(R.id.onbinLayout)
        val fiftyKLayout: ConstraintLayout? = dialog.findViewById(R.id.ellibinLayout)
        val hundredKLayout: ConstraintLayout? = dialog.findViewById(R.id.yuzbinLayout)

        tenKPrice = dialog.findViewById<TextView>(R.id.onbinPrice)!!
        fiftyKPrice = dialog.findViewById<TextView>(R.id.ellibinPrice)!!
        hundredKPrice = dialog.findViewById<TextView>(R.id.yuzbinPrice)!!

        tenKTitle = dialog.findViewById<TextView>(R.id.numberOnbin)!!
        fiftyKTitle = dialog.findViewById<TextView>(R.id.numberEllibin)!!
        hundredKTitle = dialog.findViewById<TextView>(R.id.numberyuzbin)!!

        val layouts = listOf( tenKLayout, fiftyKLayout, hundredKLayout)
        fun resetBackgrounds() {
            layouts.forEach { layout ->
                layout?.setBackgroundResource(R.drawable.radius14_bg_white)
            }
        }

        layouts.forEach { layout ->
            layout?.setOnClickListener {
                resetBackgrounds()
                layout.setBackgroundResource(R.drawable.radius14_bg_white_purple)
            }
        }

        fetchAndDisplayOfferings(viewLifecycleOwner)


    }

    @SuppressLint("SetTextI18n")
    fun showFinalOffer(context: Context) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.dialog_final_offer)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)


        val title = dialog.findViewById<TextView>(R.id.finalTitle)
        val paint = title.paint
        val width = paint.measureText(title.text.toString())
        title.paint.shader = LinearGradient(
            0f, 0f, width, title.textSize, intArrayOf(
                Color.parseColor("#7100E2"),
                Color.parseColor("#8E05C2")
            ), null, Shader.TileMode.REPEAT
        )
        dialog.show()

        val price = dialog.findViewById<TextView>(R.id.pricetextView)
        price.text = "Lifetime/${finalPrice}"


        val closeBtn = dialog.findViewById<ImageView>(R.id.closeButton)
        closeBtn.setOnClickListener {
            dialog.dismiss()
        }
    }

    fun hideKeyboard(activity: Activity) {
        val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        val currentFocus = activity.currentFocus ?: View(activity)
        imm.hideSoftInputFromWindow(currentFocus.windowToken, 0)
    }

    fun fetchAndDisplayOfferings(lifecycleOwner: LifecycleOwner) {
        Purchases.sharedInstance.getOfferings(object : ReceiveOfferingsCallback {
            override fun onReceived(offerings: Offerings) {
                val currentOfferings = offerings.current
                if (currentOfferings != null) {
                    val availablePackages = currentOfferings.availablePackages

                    lifecycleOwner.lifecycleScope.launch {
                        for (pkg in availablePackages) {
                            if (isPremium){
                                when (pkg.product.id) {
                                    "subscription_annual:subs" -> {
                                        normalPackage = pkg
                                        normalPlanButton.text = pkg.product.title.split("(")[0].trim()

                                        normalPlanPrice.text = pkg.product.price.formatted
                                    }

                                    "life_time_offer" -> {
                                        lifetimePackage = pkg
                                        lifetimeButton.text = pkg.product.title.split("(")[0].trim()
                                        lifetimePrice.text = pkg.product.price.formatted
                                    }

                                    "subscription_weekly:subs" -> {
                                        mostPackage = pkg
                                        mostPopularButton.text = pkg.product.title.split("(")[0].trim()
                                        mostPopularPrice.text = pkg.product.price.formatted
                                    }

                                    "life_time_final" -> {
                                        finalPackage = pkg
                                        finalPrice = pkg.product.price.formatted
                                    }
                                }
                            } else{
                                when (pkg.product.id) {

                                    "add_character_100k" -> {
                                        hundredKPackage = pkg
                                        hundredKPrice.text = pkg.product.price.formatted
                                        hundredKTitle.text = pkg.product.title.split("(")[0].trim()
                                    }
                                    "add_character_10k" -> {
                                        tenKPackage = pkg
                                        tenKPrice.text = pkg.product.price.formatted
                                        tenKTitle.text = pkg.product.title.split("(")[0].trim()
                                    }
                                    "add_character_50k" -> {
                                        fiftyKPackage = pkg
                                        fiftyKPrice.text = pkg.product.price.formatted
                                        fiftyKTitle.text = pkg.product.title.split("(")[0].trim()
                                    }
                                }
                            }

                            getPremiumButton.isEnabled = true

                        }
                    }
                } else {
                    Log.d("Offerings", "No offerings available")
                }
            }

            override fun onError(error: PurchasesError) {
                Log.e("Offerings", "Error fetching offerings: ${error.message}")
            }
        })
    }

    private fun selectPackage(context: Context, packageType: String, lifecycleScope: CoroutineScope, dataStoreManager: DataStoreManager) {
        Log.d("selected packagedat", "Selecting package: $packageType")

        when (packageType) {
            "normal" -> {
                handleSelectedPackage(context,lifecycleScope,normalPackage,dataStoreManager)
            }
//            "lifetime" -> {
//                handleSelectedPackage(lifetimePackage)
//            }
            "most" -> {
                handleSelectedPackage(context,lifecycleScope,mostPackage,dataStoreManager)
            }
        }
    }

    private fun handleSelectedPackage(context: Context,lifecycleScope: CoroutineScope,selectedPackage: Package,dataStoreManager: DataStoreManager) {
        Log.d("TAG", selectedPackage.identifier)
        Purchases.sharedInstance.purchaseWith(
            PurchaseParams.Builder(context as Activity, selectedPackage).build(),
            onError = { error, userCancelled ->
                Log.e("PurchaseError", "Purchase failed: $error, userCancelled: $userCancelled")

            },
            onSuccess = { storeTransaction, customerInfo ->

                // Check and log the specific entitlement
                val entitlement = customerInfo.entitlements["subscription"]
                if (entitlement != null) {
                    Log.d("Entitlement", "Entitlement details: $entitlement")
                } else {
                    Log.d("Entitlement", "Entitlement not found")
                }

                if (entitlement?.isActive == true) {
//                    val eventValues = HashMap<String, Any>()
//                    eventValues.put(AFInAppEventParameterName.CONTENT_ID, selectedPackage.product.id)
//                    eventValues.put(AFInAppEventParameterName.CONTENT_TYPE, selectedPackage.product.price)
//                    eventValues.put(AFInAppEventParameterName.REVENUE, selectedPackage.product.price.amountMicros / 1_000_000)
//
//                    eventValues.put(AFInAppEventParameterName.CURRENCY,selectedPackage.product.price.currencyCode)

                    lifecycleScope.launch {
                        dataStoreManager.saveSubscriptionStatus(context as Activity, true)

                    }


                } else {
                    Log.d("onsuccessde", "Entitlement is not active")
                }
            }
        )
    }

}

