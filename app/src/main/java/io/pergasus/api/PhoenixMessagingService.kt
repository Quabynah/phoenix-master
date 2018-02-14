/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.api

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.pergasus.R

/** Handling [FirebaseMessagingService] */
class PhoenixMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(p0: RemoteMessage?) {
        val title = p0?.notification?.title //Message Title
        val body = p0?.notification?.body   //Message Body
        val clickAction = p0?.notification?.clickAction   //Message Click action
        val dataMessage = p0?.data?.get("message") //Get data from notification
        val dataFrom = p0?.data?.get("from_user_id") //Get data from notification

        val builder = NotificationCompat.Builder(this, getString(R.string.default_notification_channel_id))
                .setSmallIcon(R.drawable.ic_stat_ic_notification)
                .setContentTitle(title)
                .setContentText(body)

        val intent = Intent(clickAction)
        intent.putExtra("message", dataMessage)
        intent.putExtra("from_user_id", dataFrom)

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        builder.setContentIntent(pendingIntent)

        val notificationID = System.currentTimeMillis().toInt()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationID, builder.build())

    }

}