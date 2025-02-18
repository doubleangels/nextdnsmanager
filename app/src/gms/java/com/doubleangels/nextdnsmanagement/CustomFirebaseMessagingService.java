package com.doubleangels.nextdnsmanagement;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Custom Firebase Messaging Service to handle incoming FCM messages and display notifications.
 */
public class CustomFirebaseMessagingService extends FirebaseMessagingService {

    // Unique channel ID for notifications sent by this service
    private static final String CHANNEL_ID = "general";
    // Human-readable channel name for settings and system UI
    private static final String CHANNEL_NAME = "General";

    /**
     * Called when a message is received.
     * This method extracts the title and body from either the notification or data payload
     * and displays a notification to the user.
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Default values if no title or message body is provided
        String title = "This notification did not include a title.";
        String messageBody = "This notification did not include a message body.";

        // If the notification payload is not null, extract title and body
        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            messageBody = remoteMessage.getNotification().getBody();
        }

        // If the data payload has values, these can override the notification title/body
        // (often used when additional data is sent by the FCM server)
        if (!remoteMessage.getData().isEmpty()) {
            if (remoteMessage.getData().containsKey("title")) {
                title = remoteMessage.getData().get("title");
            }
            if (remoteMessage.getData().containsKey("message")) {
                messageBody = remoteMessage.getData().get("message");
            }
        }

        // Create an intent to launch MainActivity when the user taps on the notification
        Intent intent = new Intent(this, MainActivity.class);
        // Clears any existing instance of MainActivity before showing a new one
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Wrap the Intent in a PendingIntent so it can be triggered when notification is tapped
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        // Configure the notification, including icon, title, body, and launch intent
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // Replace with your own notification icon
                .setContentTitle(title)
                .setContentText(messageBody)
                .setAutoCancel(true) // Dismiss the notification when tapped
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        // Get the notification manager to show the notification
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create a NotificationChannel for Android O and above to display notifications properly
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
        );
        notificationManager.createNotificationChannel(channel);

        // Use a static notification ID or generate one dynamically
        int notificationId = 0;
        // Display the notification
        notificationManager.notify(notificationId, notificationBuilder.build());
    }

    /**
     * Called if the FCM registration token is updated. This usually happens when:
     * 1) The app is restored to a new device.
     * 2) The user uninstalls/reinstalls the app.
     * 3) The user clears app data.
     * Override this method to send the updated token to your server.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // You can send the new token to your server or save it locally.
    }
}
