package com.proksi.kotodama

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService

class FirebaseClass : FirebaseMessagingService() {
    override fun onNewToken(token: String){
        Log.d("token", "Refreshed token: $token")


    }
}