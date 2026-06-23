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

        // Workaround for known Firebase Messaging issues where transient Play Services
        // errors throw on an internal background thread and would otherwise crash the app.
        Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            if (SentryManager.isIgnored(throwable)) {
                sentryManager.captureMessage(
                        "Caught and ignored transient Firebase Messaging error from background thread.");
                return;
            }

            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            }
        });

        try {
            // Retrieve the FCM registration token asynchronously
            FirebaseMessaging.getInstance().getToken()
                    .addOnCompleteListener(task -> {
                        try {
                            if (!task.isSuccessful()) {
                                Exception tokenError = task.getException();
                                if (tokenError != null) {
                                    if (SentryManager.isIgnored(tokenError)) {
                                        sentryManager.captureMessage(
                                                "Transient Firebase Messaging error while fetching FCM token.");
                                    } else {
                                        sentryManager.captureException(tokenError);
                                    }
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
                                                Exception topicError = task1.getException();
                                                if (topicError != null) {
                                                    if (SentryManager.isIgnored(topicError)) {
                                                        sentryManager.captureMessage(
                                                                "Transient Firebase Messaging error while subscribing to topic.");
                                                    } else {
                                                        sentryManager.captureException(topicError);
                                                    }
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
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
    }
}
