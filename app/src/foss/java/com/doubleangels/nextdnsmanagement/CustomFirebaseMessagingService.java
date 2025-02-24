package com.doubleangels.nextdnsmanagement;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

/**
 * A basic Service class named "CustomFirebaseMessagingService."
 */
public class CustomFirebaseMessagingService extends Service {

    /**
     * Called each time the service is started with an Intent.
     * Returning START_NOT_STICKY means the system will NOT recreate the service
     * if it is killed while there are no start commands pending.
     *
     * @param intent  The Intent used to start the service.
     * @param flags   Additional data about the start request.
     * @param startId A unique integer representing this specific request to start.
     * @return The mode in which the system will handle if this service’s process is killed.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Perform any necessary work here (e.g., background tasks, message handling).
        return START_NOT_STICKY;
    }

    /**
     * Called by the system when the service is first created.
     * This is where you can do one-time setup before the service runs.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize resources or perform any setup needed by this service.
    }

    /**
     * Called when another component wants to bind with the service by calling bindService().
     * Returning null indicates this service is not designed to be bound; it's started as needed.
     *
     * @param intent The Intent that was used to bind to this service.
     * @return An IBinder for client binding, or null if binding is not allowed.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // For a bound service, you would return a binder interface here.
        return null;
    }
}