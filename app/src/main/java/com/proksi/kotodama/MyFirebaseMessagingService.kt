package com.proksi.kotodama

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kotodama.tts.R

class MyFirebaseMessagingService : FirebaseMessagingService(){
    val channelId="notification_channel"
    val channelName="com.kotodama.tts"

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("onNewToken", "Refreshed token: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        if(message.notification != null){
            generateNotification(message.notification!!.title!!,message.notification!!.body!!)
        }
    }

    @SuppressLint("RemoteViewLayout")
    fun getRemoteView(title: String, message: String) : RemoteViews {
        val remoteViews = RemoteViews("com.kotodama.tts",R.layout.notification)

        remoteViews.setTextViewText(R.id.msgTitle,title)
        remoteViews.setTextViewText(R.id.msgDesc,message)
        remoteViews.setImageViewResource(R.id.app_logo,R.drawable.icon_kota)

        return remoteViews

    }

    fun generateNotification(title:String,message:String){
        val intent = Intent(this,MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(this,0,intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)

        //CHANNEL ID, CHANNEL NAME
        var builder = NotificationCompat.Builder(applicationContext,channelId)
            .setSmallIcon(R.drawable.icon_kota)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(1000,1000,1000,1000))
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)

        builder = builder.setContent(getRemoteView(title,message))

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(0,builder.build())

    }

    companion object {
        fun getToken() {
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.w("FCM_TOKEN", "Token alma başarısız", task.exception)
                        return@addOnCompleteListener
                    }
                    val token = task.result
                    Log.d("FCM_TOKEN", "FCM Token: $token")
                }
        }
    }


}