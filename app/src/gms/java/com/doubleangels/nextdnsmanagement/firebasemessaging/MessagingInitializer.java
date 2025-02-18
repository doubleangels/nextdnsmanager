package com.doubleangels.nextdnsmanagement.firebasemessaging;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

/**
 * Initializes Firebase in the given context, retrieves the FCM token,
 * and subscribes the device to a default topic ("general").
 */
public class MessagingInitializer {

    // Tag for logging
    private static final String TAG = "MessagingInitializer";

    // Name of the shared preferences file
    private static final String PREFS_NAME = "MyAppPreferences";

    // Key for storing the FCM token in shared preferences
    private static final String KEY_FCM_TOKEN = "fcmToken";

    /**
     * Initializes Firebase, retrieves the device's FCM registration token,
     * stores it in shared preferences, and subscribes the device to the "general" topic.
     *
     * @param context The context from which this method is called (e.g., an Application or Activity).
     */
    public static void initialize(Context context) {
        // Initialize Firebase (Required before using Firebase services).
        FirebaseApp.initializeApp(context);

        // Retrieve the FCM registration token asynchronously.
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        // If token retrieval fails, log the exception and return.
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    // Get the token result from the task
                    String token = task.getResult();
                    if (token == null) {
                        // If the token is null, something went wrong.
                        Log.w(TAG, "FCM token is null");
                        return;
                    }

                    // Log the retrieved token for debugging purposes.
                    Log.d(TAG, "FCM Token retrieved: " + token);

                    // Store the token in SharedPreferences for future use (e.g., sending to server).
                    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    prefs.edit().putString(KEY_FCM_TOKEN, token).apply();

                    // Subscribe the user to the "general" topic (e.g., for sending broadcast notifications).
                    FirebaseMessaging.getInstance().subscribeToTopic("general")
                            .addOnCompleteListener(task1 -> {
                                // Check if subscription was successful.
                                if (!task1.isSuccessful()) {
                                    Log.w(TAG, "Topic subscription failed", task1.getException());
                                } else {
                                    Log.d(TAG, "Subscribed to topic 'general'");
                                }
                            });
                });
    }
}
