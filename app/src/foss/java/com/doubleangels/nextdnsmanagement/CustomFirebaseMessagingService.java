package com.doubleangels.nextdnsmanagement;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

/**
 * A simple Service implementation for Firebase Messaging.
 * <p>
 * This service currently does not perform any messaging functions and is set up to not restart if terminated.
 * </p>
 */
public class CustomFirebaseMessagingService extends Service {

    /**
     * Called when the service is started.
     * Returns START_NOT_STICKY so that the service is not recreated after being killed.
     *
     * @param intent  The Intent supplied to startService(Intent), as given.
     * @param flags   Additional data about this start request.
     * @param startId A unique integer representing this specific request to start.
     * @return The mode in which to continue running; START_NOT_STICKY indicates not to restart.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Return START_NOT_STICKY to prevent the service from restarting automatically.
        return START_NOT_STICKY;
    }

    /**
     * Called by the system when the service is first created.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        // Additional initialization can be done here if needed.
    }

    /**
     * This service does not support binding.
     *
     * @param intent The Intent that was used to bind to this service.
     * @return Always returns null since binding is not allowed.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // Binding is not implemented in this service.
        return null;
    }
}
