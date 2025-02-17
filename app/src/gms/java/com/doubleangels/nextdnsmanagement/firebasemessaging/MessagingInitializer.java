package com.doubleangels.nextdnsmanagement.firebasemessaging;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

/**
 * A helper class that:
 * 1. Initializes Firebase.
 * 2. Retrieves the FCM token.
 * 3. Stores the token locally.
 * 4. Subscribes the device to a topic for push notifications.
 */
public class MessagingInitializer {
    private static final String TAG = "MessagingInitializer";
    private static final String PREFS_NAME = "MyAppPreferences";
    private static final String KEY_FCM_TOKEN = "fcmToken";

    /**
     * Call this method (e.g., from your Application or Activity onCreate) to set up push notifications.
     *
     * @param context the application context
     */
    public static void initialize(Context context) {
        // 1. Initialize Firebase
        FirebaseApp.initializeApp(context);

        // 2. Retrieve the FCM token asynchronously
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    // Get the new FCM token
                    String token = task.getResult();
                    if (token == null) {
                        Log.w(TAG, "FCM token is null");
                        return;
                    }

                    Log.d(TAG, "FCM Token retrieved: " + token);

                    // 3. Store the token in SharedPreferences for future use
                    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    prefs.edit().putString(KEY_FCM_TOKEN, token).apply();

                    // 4. Optionally subscribe to a topic (e.g., "general") for push notifications
                    FirebaseMessaging.getInstance().subscribeToTopic("general")
                            .addOnCompleteListener(task1 -> {
                                if (!task1.isSuccessful()) {
                                    Log.w(TAG, "Topic subscription failed", task1.getException());
                                } else {
                                    Log.d(TAG, "Subscribed to topic 'general'");
                                }
                            });
                });
    }
}