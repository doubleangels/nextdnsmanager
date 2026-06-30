package com.doubleangels.nextdnsmanagement;

import android.app.Application;

import com.doubleangels.nextdnsmanagement.sentry.SentryInitializer;
import com.doubleangels.nextdnsmanagement.sentry.SentryManager;
import com.doubleangels.nextdnsmanagement.sharedpreferences.SharedPreferencesManager;

/**
 * Application entry point for one-time initialization.
 */
public class NextDnsApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        new Thread(() -> {
            try {
                SharedPreferencesManager.init(getApplicationContext());
                SentryManager.installFilteredUncaughtExceptionHandler();
                if (SharedPreferencesManager.getBoolean("sentry_enable", false)) {
                    SentryInitializer.initialize(getApplicationContext());
                }
            } catch (Exception ignored) {
                // MainActivity splash flow handles init failure reporting.
            }
        }, "app-prefs-warmup").start();
    }
}
