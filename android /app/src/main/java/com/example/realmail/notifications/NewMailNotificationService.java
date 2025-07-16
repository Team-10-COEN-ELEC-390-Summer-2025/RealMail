package com.example.realmail.notifications;
/*
    * RealMail - A simple email client for Android
    * https://firebase.google.com/docs/cloud-messaging/android/client
 */
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.realmail.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class NewMailNotificationService extends FirebaseMessagingService {

    /**
     * Handles incoming Firebase Cloud Messaging (FCM) messages and displays a notification with the message's title and body.
     *
     * Extracts the notification title and body from the received RemoteMessage, creates a notification channel if required, and posts a high-priority notification to the user.
     *
     * @param remoteMessage the incoming FCM message containing notification data
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = null;
        String body = null;

        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        }

        String channelId = "default";
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create notification channel for Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Default Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_notification) // Replace with your icon
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        notificationManager.notify(0, builder.build());
    }

    /**
     * Called when a new Firebase Cloud Messaging registration token is generated.
     *
     * @param token the newly generated FCM registration token
     */
    @Override
    public void onNewToken(@NonNull String token) {
        Log.d("TAG", "Refreshed token: " + token);

    }
    // [END on_new_token]


}
