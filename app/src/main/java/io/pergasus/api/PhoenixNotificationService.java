/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.api;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import io.pergasus.BuildConfig;
import io.pergasus.R;
import io.pergasus.ui.HomeActivity;

public class PhoenixNotificationService extends FirebaseMessagingService {
	private static final String TAG = "PhoenixNotificationServ";
	
	@Override
	public void onMessageReceived(RemoteMessage remoteMessage) {
		super.onMessageReceived(remoteMessage);
		
		//Log sender
		createLogMessage("From: " + remoteMessage.getFrom());
		
		//Check state of the received notification
		if (remoteMessage.getData().isEmpty()) return;
		
		//Log data
		createLogMessage(remoteMessage.getData());
		String title = remoteMessage.getData().get("title");
		String body = remoteMessage.getData().get("body");
		String image = remoteMessage.getData().get("image");
		sendPurchaseNotification(title, body, image);
	}
	
	//Send notification
	private void sendPurchaseNotification(CharSequence title, CharSequence body, String image) {
		Intent intent = new Intent(this, HomeActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent
				.FLAG_ONE_SHOT);
		
		//Setup channel and sound for notification
		String channel = getString(R.string.default_notification_channel_id);
		Uri defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		
		//Build notification
		Builder builder = new Builder(this, channel)
				.setSmallIcon(getNotificationIcon())
				.setContentTitle(title)
				.setContentText(body)
				.setAutoCancel(true)
				.setSound(defaultUri)
				.setContentIntent(pendingIntent);
		
		//Init notification manager
		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		//Send notification
		if (manager != null) {
			manager.notify(2018, builder.build());
		}
		
	}
	
	//Get notification icon
	private static int getNotificationIcon() {
		//Use white icon for M+ and darker one for lower SDKs versions
		return R.drawable.ic_stat_ic_notification;
	}
	
	private static void createLogMessage(Object obj) {
		if (BuildConfig.DEBUG) {
			Log.d(TAG, obj.toString());
		}
	}
}
