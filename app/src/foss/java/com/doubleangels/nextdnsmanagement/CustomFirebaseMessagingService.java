package com.doubleangels.nextdnsmanagement;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class CustomFirebaseMessagingService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // No push notifications processing in the foss flavor.
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize alternative push systems here if needed,
        // or simply leave it as a no-op.
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
