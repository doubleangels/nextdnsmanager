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

    private static final String KEY_FCM_TOKEN = "fcmToken";

    public static void initialize(Context context) {
        SentryManager sentryManager = new SentryManager(context);

        try {
            if (!SharedPreferencesManager.isInitialized()) {
                SharedPreferencesManager.init(context);
            }
        } catch (Exception e) {
            sentryManager.captureException(e);
            return;
        }

        try {
            FirebaseApp.initializeApp(context);
        } catch (Exception e) {
            sentryManager.captureException(e);
            return;
        }

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
                                restoreDefaultExceptionHandler(defaultHandler);
                                return;
                            }

                            String token = task.getResult();
                            if (token == null) {
                                sentryManager.captureMessage("FCM token is null");
                                restoreDefaultExceptionHandler(defaultHandler);
                                return;
                            }

                            sentryManager.captureMessage("FCM token retrieved successfully");
                            SharedPreferencesManager.putString(KEY_FCM_TOKEN, token);

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
                                        } finally {
                                            restoreDefaultExceptionHandler(defaultHandler);
                                        }
                                    });
                        } catch (Exception ex) {
                            sentryManager.captureException(ex);
                            restoreDefaultExceptionHandler(defaultHandler);
                        }
                    });
        } catch (Exception e) {
            sentryManager.captureException(e);
            restoreDefaultExceptionHandler(defaultHandler);
        }
    }

    private static void restoreDefaultExceptionHandler(Thread.UncaughtExceptionHandler defaultHandler) {
        if (defaultHandler != null) {
            Thread.setDefaultUncaughtExceptionHandler(defaultHandler);
        }
    }
}
