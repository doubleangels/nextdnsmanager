package com.doubleangels.nextdnsmanagement.firebasemessaging;

import android.content.Context;

import com.doubleangels.nextdnsmanagement.sentry.SentryManager;
import com.doubleangels.nextdnsmanagement.sharedpreferences.SharedPreferencesManager;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

/**
 * Initializes Firebase in the given context, retrieves the FCM token,
 * and subscribes the device to a default topic ("general").
 */
public class MessagingInitializer {

    // Key for storing the FCM token in shared preferences
    private static final String KEY_FCM_TOKEN = "fcmToken";

    /**
     * Initializes Firebase, retrieves the device's FCM registration token,
     * stores it in shared preferences, and subscribes the device to the "general"
     * topic.
     *
     * @param context The context from which this method is called (e.g., an
     *                Application or Activity).
     */
    public static void initialize(Context context) {
        SentryManager sentryManager = new SentryManager(context);

        // Initialize SharedPreferencesManager
        try {
            SharedPreferencesManager.init(context);
        } catch (Exception e) {
            sentryManager.captureException(e);
            return;
        }

        // Initialize Firebase (Required before using Firebase services)
        try {
            FirebaseApp.initializeApp(context);
        } catch (Exception e) {
            sentryManager.captureException(e);
            return;
        }

        // Retrieve the FCM registration token asynchronously
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    try {
                        if (!task.isSuccessful()) {
                            if (task.getException() != null) {
                                sentryManager.captureException(task.getException());
                            } else {
                                sentryManager.captureMessage("Fetching FCM registration token failed");
                            }
                            return;
                        }

                        // Get the token result from the task
                        String token = task.getResult();
                        if (token == null) {
                            sentryManager.captureMessage("FCM token is null");
                            return;
                        }

                        sentryManager.captureMessage("FCM Token retrieved: " + token);

                        // Store the token in SharedPreferences for future use
                        SharedPreferencesManager.putString(KEY_FCM_TOKEN, token);

                        // Subscribe the user to the "general" topic
                        FirebaseMessaging.getInstance().subscribeToTopic("general")
                                .addOnCompleteListener(task1 -> {
                                    try {
                                        if (!task1.isSuccessful()) {
                                            if (task1.getException() != null) {
                                                sentryManager.captureException(task1.getException());
                                            } else {
                                                sentryManager.captureMessage("Topic subscription failed");
                                            }
                                        } else {
                                            sentryManager.captureMessage("Subscribed to topic 'general'");
                                        }
                                    } catch (Exception ex) {
                                        sentryManager.captureException(ex);
                                    }
                                });
                    } catch (Exception ex) {
                        sentryManager.captureException(ex);
                    }
                });
    }
}
