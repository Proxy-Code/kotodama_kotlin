package com.proksi.kotodama.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.ColorDrawable
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.kotodama.tts.R
import com.proksi.kotodama.dataStore.DataStoreManager
import com.proksi.kotodama.objects.EventLogger
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.purchaseWith
import com.revenuecat.purchases.restorePurchasesWith
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DialogUtils {

    private lateinit var mostPackage: Package
    private lateinit var normalPackage: Package
    private lateinit var lifetimePackage: Package
    private lateinit var tenKPackage: Package
    private lateinit var fiftyKPackage: Package
    private lateinit var hundredKPackage: Package
    private lateinit var finalPackage: Package
    private lateinit var slotPackage: Package
    private lateinit var normalPlanButton: TextView
//    private lateinit var getPremiumButton: TextView
    private lateinit var mostPopularPrice: TextView
    private lateinit var normalPlanPrice: TextView
    private lateinit var lifetimePrice: TextView
    private lateinit var tenKPrice: TextView
    private lateinit var fiftyKPrice: TextView
    private lateinit var hundredKPrice: TextView
    private lateinit var slotPrice: TextView
    private lateinit var tenKTitle: TextView
    private lateinit var fiftyKTitle: TextView
    private lateinit var textView15: TextView
    private lateinit var hundredKTitle: TextView
    private var isPremium:Boolean = true
    private var isSlot: Boolean = false
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

        EventLogger.logEvent(context, "paywall_second_screen_shown")


        isPremium=true
        isSlot = false

        val getPremiumButton:TextView = dialog.findViewById(R.id.getPremiumButton)
        normalPlanButton = dialog.findViewById(R.id.textView152)
        normalPlanPrice = dialog.findViewById(R.id.normalPrice)
        mostPopularPrice = dialog.findViewById(R.id.mostPrice)
        textView15 = dialog.findViewById(R.id.textView15)
        val restoreButtonLayout:RelativeLayout = dialog.findViewById(R.id.restoreButtonLayout)
        val normalLayout: ConstraintLayout = dialog.findViewById(R.id.normalLayout)
        val mostLayout: ConstraintLayout = dialog.findViewById(R.id.mostLayout)
        val progressBar: ProgressBar = dialog.findViewById(R.id.restoreProgressBar)
        val restoreText: TextView = dialog.findViewById(R.id.restoreBtn)

        val layouts = listOf( normalLayout, mostLayout)



        fun resetBackgrounds() {
            layouts.forEach { layout ->
                layout.setBackgroundResource(R.drawable.radius14_bg_white)
            }
        }

        Log.d("TAG", "showPremiumDialogBox: $dialog")
        fun setOnClickListener(layout: ConstraintLayout, type: String) {
            Log.d("aaaaaa", "setOnClickListener: set onclikck")
            layout.setOnClickListener {
                resetBackgrounds()
                layout.setBackgroundResource(R.drawable.radius14_bg_white_purple)
                packageType = type
            }
        }

        restoreButtonLayout.setOnClickListener{
            restorePurchases(context, viewLifecycleOwner, lifecycleScope, dataStoreManager, dialog, progressBar, restoreText)
            showLoadingState(true, progressBar,restoreText)
        }

        setOnClickListener(normalLayout, "normal")
        setOnClickListener(mostLayout, "most")

        fetchAndDisplayOfferings(viewLifecycleOwner, getPremiumButton,dataStoreManager,lifecycleScope,context)

        val closeBtnPremium = dialog.findViewById<ImageView>(R.id.closeButton)
        closeBtnPremium.setOnClickListener {
            dialog.dismiss()
            showFinalOffer(context, lifecycleScope, dataStoreManager)
        }

        dialog.findViewById<TextView>(R.id.getPremiumButton).setOnClickListener{
            EventLogger.logEvent(context, "paywallSecond_getPremium_click")
            selectPackage(context, packageType, lifecycleScope, dataStoreManager, dialog)
        }
    }

    fun showAddCharacterDialogBox(context: Context, viewLifecycleOwner: LifecycleOwner,lifecycleScope: CoroutineScope,dataStoreManager: DataStoreManager){
        val dialog = BottomSheetDialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setDismissWithAnimation(true)
        dialog.setContentView(R.layout.dialog_add_character)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()

        isPremium = false
        isSlot = false

        val tenKLayout: ConstraintLayout? = dialog.findViewById(R.id.onbinLayout)
        val fiftyKLayout: ConstraintLayout? = dialog.findViewById(R.id.ellibinLayout)
        val hundredKLayout: ConstraintLayout? = dialog.findViewById(R.id.yuzbinLayout)
        val getPremiumButton: TextView? = dialog.findViewById(R.id.buyButton)

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


        fun setOnClickListener(layout: ConstraintLayout, type: String) {
            EventLogger.logEvent(context, "paywallCredit_buy_click")
            layout.setOnClickListener {
                resetBackgrounds()
                layout.setBackgroundResource(R.drawable.radius14_bg_white_purple)
                packageType = type
            }
        }

        if (tenKLayout != null) {
            setOnClickListener(tenKLayout, "10k")
        }
        if (fiftyKLayout != null) {
            setOnClickListener(fiftyKLayout, "50k")
        }
        if (hundredKLayout != null) {
            setOnClickListener(hundredKLayout, "100k")
        }

        dialog.findViewById<TextView>(R.id.buyButton)!!.setOnClickListener{
            selectPackage(context, packageType, lifecycleScope, dataStoreManager, dialog)
        }

        if (getPremiumButton != null) {
            fetchAndDisplayOfferings(viewLifecycleOwner, getPremiumButton,dataStoreManager,lifecycleScope,context)
        }



    }

    fun showAddSlotDialogBox(context: Context, viewLifecycleOwner: LifecycleOwner,lifecycleScope: CoroutineScope,dataStoreManager: DataStoreManager){
        val dialog = BottomSheetDialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setDismissWithAnimation(true)
        dialog.setContentView(R.layout.dialog_add_slot)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()

        isPremium = false
        isSlot = true

        slotPrice = dialog.findViewById<TextView>(R.id.priceText)!!
        val getPremiumButton: TextView? = dialog.findViewById(R.id.buyButton)

        EventLogger.logEvent(context, "paywall_credit_screen_shown")

        dialog.findViewById<TextView>(R.id.buyButton)!!.setOnClickListener{
            EventLogger.logEvent(context, "paywallClone_buy_click")
            selectPackage(context, "slot", lifecycleScope, dataStoreManager, dialog)
        }

        if (getPremiumButton != null) {
            fetchAndDisplayOfferings(viewLifecycleOwner, getPremiumButton, dataStoreManager,lifecycleScope,context)
        }

    }

    @SuppressLint("SetTextI18n")
    fun showFinalOffer(
        context: Context,
        lifecycleScope: CoroutineScope,
        dataStoreManager: DataStoreManager
    ) {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val lastOfferKey = "lastOfferKey"
        val storedValue = sharedPreferences.getLong(lastOfferKey, 0)
        val currentTime = System.currentTimeMillis() / 1000
        val timeDiff = currentTime - storedValue

        if (timeDiff > 300) {
            return
        }
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
        EventLogger.logEvent(context, "paywall_final_screen_shown")


        dialog.findViewById<TextView>(R.id.priceOldTextView).paintFlags = Paint.STRIKE_THRU_TEXT_FLAG

        val price = dialog.findViewById<TextView>(R.id.pricetextView)
        val timer = dialog.findViewById<TextView>(R.id.timer)
        price.text = finalPrice

        packageType = "final"


        val remainingTime = (300 - timeDiff).coerceAtLeast(0)

        if (remainingTime > 0) {
            object : CountDownTimer(remainingTime * 1000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val seconds = millisUntilFinished / 1000
                    val minutes = seconds / 60
                    val remainingSeconds = seconds % 60
                    timer.text = String.format("%02d:%02d", minutes, remainingSeconds)
                }

                override fun onFinish() {
                    timer.text = "00:00"
                }
            }.start()
        } else {
            timer.text = "00:00"
        }

        val closeBtn = dialog.findViewById<ImageView>(R.id.closeButton)
        closeBtn.setOnClickListener {
            dialog.dismiss()
        }

        dialog.findViewById<TextView>(R.id.getPremiumButton).setOnClickListener{
            EventLogger.logEvent(context, "paywallFinal_getPremium_click")
            selectPackage(context, packageType , lifecycleScope, dataStoreManager,dialog)
        }

    }

    fun hideKeyboard(activity: Activity) {
        val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        val currentFocus = activity.currentFocus ?: View(activity)
        imm.hideSoftInputFromWindow(currentFocus.windowToken, 0)
    }

    fun fetchAndDisplayOfferings(lifecycleOwner: LifecycleOwner,
                                 getPremiumButton: TextView,
                                 dataStoreManager:DataStoreManager,
                                 lifecycleScope: CoroutineScope,
                                 context: Context) {
        lifecycleScope.launch {
            val planType = dataStoreManager.getPlanType(context).first()


            Purchases.sharedInstance.getOfferings(object : ReceiveOfferingsCallback {
                @SuppressLint("SetTextI18n")
                override fun onReceived(offerings: Offerings) {
                    val currentOfferings = offerings.current
                    if (currentOfferings != null) {
                        val availablePackages = currentOfferings.availablePackages

                        lifecycleOwner.lifecycleScope.launch {
                            for (pkg in availablePackages) {
                                Log.d("onemli", "${pkg.product.id} $planType")

                                if (isPremium) {
                                    when (pkg.product.id) {

                                        "subscription_annual:subs" -> {
                                            normalPackage = pkg
                                         //       textView15.text = pkg.product.title.split("(")[0].trim()
                                                mostPopularPrice.text = pkg.product.price.formatted
                                        }

                                        "subscription_weekly:subs" -> {
                                            mostPackage = pkg
                                            normalPlanPrice.text = pkg.product.price.formatted //weekly
                                        }

                                        "subscription_final_offer:subs" -> {
                                            finalPackage = pkg
                                            finalPrice = pkg.product.price.formatted
                                        }
                                    }
                                } else {
                                    if (isSlot) {
                                        when ((pkg.product.id)) {
                                            "kt_voice_slot" -> {
                                                slotPackage = pkg
                                                slotPrice.text =
                                                    "One time / ${pkg.product.price.formatted}"
                                            }
                                        }
                                    } else {
                                        when (pkg.product.id) {

                                            "add_character_100k" -> {
                                                hundredKPackage = pkg
                                                hundredKPrice.text = pkg.product.price.formatted
                                                hundredKTitle.text =
                                                    pkg.product.title.split("(")[0].trim()
                                            }

                                            "add_character_10k" -> {
                                                tenKPackage = pkg
                                                tenKPrice.text = pkg.product.price.formatted
                                                tenKTitle.text =
                                                    pkg.product.title.split("(")[0].trim()
                                            }

                                            "add_character_50k" -> {
                                                fiftyKPackage = pkg
                                                fiftyKPrice.text = pkg.product.price.formatted
                                                fiftyKTitle.text =
                                                    pkg.product.title.split("(")[0].trim()
                                            }

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
    }

    private fun selectPackage(
        context: Context,
        packageType: String,
        lifecycleScope: CoroutineScope,
        dataStoreManager: DataStoreManager,
        dialog: Dialog
    ) {
        Log.d("aaaaaa", packageType)

        when (packageType) {
            "most" -> {
                 handleSelectedPackage(context,lifecycleScope,normalPackage,dataStoreManager,dialog) //yearly
            }

            "normal" -> {
                handleSelectedPackage(context,lifecycleScope,mostPackage,dataStoreManager,dialog)
            }
            "10k" -> {
                handleSelectedPackage(
                    context,
                    lifecycleScope,
                    tenKPackage,
                    dataStoreManager,
                    dialog
                )
            }
            "50k" -> {
                handleSelectedPackage(
                    context,
                    lifecycleScope,
                    fiftyKPackage,
                    dataStoreManager,
                    dialog
                )
            }
            "100k" -> {
                handleSelectedPackage(
                    context,
                    lifecycleScope,
                    hundredKPackage,
                    dataStoreManager,
                    dialog
                )
            }
            "final" -> {
                handleSelectedPackage(
                    context,
                    lifecycleScope,
                    finalPackage,
                    dataStoreManager,
                    dialog
                )
            }
            "slot" -> {
                handleSelectedPackage(
                    context,
                    lifecycleScope,
                    slotPackage,
                    dataStoreManager,
                    dialog
                )
            }
        }
    }

    private fun handleSelectedPackage(
        context: Context,
        lifecycleScope: CoroutineScope,
        selectedPackage: Package,
        dataStoreManager: DataStoreManager,
        dialog: Dialog
    ) {

        Purchases.sharedInstance.purchaseWith(
            PurchaseParams.Builder(context as Activity, selectedPackage).build(),
            onError = { error, userCancelled ->
                Log.e("PurchaseError", "Purchase failed: $error, userCancelled: $userCancelled")
                (context as? Activity)?.runOnUiThread {
                    dialog.dismiss()
                }
            },
            onSuccess = { storeTransaction, customerInfo ->

                // Check and log the specific entitlement
                val entitlement = customerInfo.entitlements["Subscription"]
                if (entitlement != null) {
                    Log.d("Entitlement", "Entitlement details: $entitlement")
                } else {
                    Log.d("Entitlement", "Entitlement not found")
                }

                if (entitlement?.isActive == true) {

//                    lifecycleScope.launch {
//                        dataStoreManager.saveSubscriptionStatus(context as Activity, true)
//                        (context as? Activity)?.runOnUiThread {
//                            dialog.dismiss()
//                        }
//                    }
                    lifecycleScope.launch {
                        dataStoreManager.saveSubscriptionStatus(context, true)
                        dialog.dismiss()
                    }

                } else {
                    Log.d("onsuccessde", "Entitlement is not active")
                    (context as? Activity)?.runOnUiThread {
                        dialog.dismiss()
                    }

                }
            }
        )
    }

    private fun restorePurchases(
        context: Context,
        viewLifecycleOwner: LifecycleOwner,
        lifecycleScope: CoroutineScope,
        dataStoreManager: DataStoreManager,
        dialog: Dialog,
        progressBar: ProgressBar,
        restoreText: TextView
    ) {
        Purchases.sharedInstance.restorePurchasesWith(
            onSuccess = { customerInfo ->
                val entitlement = customerInfo.entitlements["Subscription"]
                if (entitlement?.isActive == true) {

                    Log.d("Restore", "Subscription restored successfully")

                    lifecycleScope.launch {
                        dataStoreManager.saveSubscriptionStatus(context, true)
                        dialog.dismiss()
                    }
                    showLoadingState(false, progressBar, restoreText)

                } else {
                    Log.d("Restore", "No active subscription found")
                    showNoSubscriptionDialog(context)
                    showLoadingState(false, progressBar, restoreText)
                }
            },
            onError = { error ->
                Log.e("Restore", "Error restoring purchases: ${error.message}")
                Toast.makeText(context, "Error restoring purchases: ${error.message}", Toast.LENGTH_LONG).show()
                showLoadingState(false, progressBar, restoreText)
            }
        )
    }

    private fun showLoadingState(
        isLoading: Boolean,
        progressBar: ProgressBar,
        restoreText: TextView
    ) {
        if (isLoading) {
            restoreText.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
        } else {
            restoreText.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
        }
    }

    private fun showNoSubscriptionDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("Subscription Not Found")
            .setMessage("We couldn't find active subscription. Contact us if problem continues.")
            .setPositiveButton("Okay") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

}

