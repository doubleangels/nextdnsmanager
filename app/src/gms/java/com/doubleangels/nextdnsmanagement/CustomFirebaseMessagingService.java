package com.doubleangels.nextdnsmanagement;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.doubleangels.nextdnsmanagement.sentry.SentryManager;
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

    private SentryManager sentryManager;

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize SentryManager for capturing exceptions and messages.
        sentryManager = new SentryManager(this);
    }

    /**
     * Called when a message is received.
     * This method extracts the title and body from either the notification or data payload
     * and displays a notification to the user.
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        try {
            // Default values if no title or message body is provided.
            String title = "This notification did not include a title.";
            String messageBody = "This notification did not include a message body.";

            // Extract title and message from the notification payload if available.
            try {
                if (remoteMessage.getNotification() != null) {
                    if (remoteMessage.getNotification().getTitle() != null) {
                        title = remoteMessage.getNotification().getTitle();
                    }
                    if (remoteMessage.getNotification().getBody() != null) {
                        messageBody = remoteMessage.getNotification().getBody();
                    }
                }
            } catch (Exception e) {
                sentryManager.captureException(e);
            }

            // Override with values from the data payload if present.
            try {
                if (!remoteMessage.getData().isEmpty()) {
                    if (remoteMessage.getData().containsKey("title")) {
                        title = remoteMessage.getData().get("title");
                    }
                    if (remoteMessage.getData().containsKey("message")) {
                        messageBody = remoteMessage.getData().get("message");
                    }
                }
            } catch (Exception e) {
                sentryManager.captureException(e);
            }

            // Create an intent to launch MainActivity when the notification is tapped.
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
            );

            // Configure the notification.
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification) // Replace with your own notification icon.
                    .setContentTitle(title)
                    .setContentText(messageBody)
                    .setAutoCancel(true) // Dismiss the notification when tapped.
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent);

            // Get the NotificationManager.
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                // Create a NotificationChannel for Android O and above.
                try {
                    NotificationChannel channel = new NotificationChannel(
                            CHANNEL_ID,
                            CHANNEL_NAME,
                            NotificationManager.IMPORTANCE_HIGH
                    );
                    notificationManager.createNotificationChannel(channel);
                } catch (Exception e) {
                    sentryManager.captureException(e);
                }

                // Display the notification.
                try {
                    int notificationId = 0; // Use a static or dynamically generated ID as needed.
                    notificationManager.notify(notificationId, notificationBuilder.build());
                } catch (Exception e) {
                    sentryManager.captureException(e);
                }
            } else {
                sentryManager.captureMessage("NotificationManager is null");
            }
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
    }

    /**
     * Called if the FCM registration token is updated. Override this method to send the updated token to your server.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        try {
            super.onNewToken(token);
            // You can send the new token to your server or save it locally.
            sentryManager.captureMessage("New FCM token: " + token);
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
    }
}
