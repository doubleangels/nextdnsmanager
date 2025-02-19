package com.doubleangels.nextdnsmanagement.sentry;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLException;

import io.sentry.Sentry;
import okhttp3.internal.http2.ConnectionShutdownException;

/**
 * A helper class to manage Sentry error logging.
 * It checks user preferences to see if Sentry is enabled before capturing messages and exceptions.
 */
public class SentryManager {

    // Android context, used for accessing SharedPreferences and other resources.
    private final Context context;

    // A tag for Android log statements.
    public String TAG = "DebugLogging";

    // Reference to the shared preferences where Sentry enable/disable setting is stored.
    public SharedPreferences sharedPreferences;

    // List of exception types to ignore when capturing exceptions.
    private static final List<Class<? extends Exception>> IGNORED_ERRORS = Arrays.asList(
            UnknownHostException.class,
            SocketTimeoutException.class,
            SocketException.class,
            SSLException.class,
            ConnectionShutdownException.class
    );

    /**
     * Constructor that initializes the SentryManager with an application or activity context.
     *
     * @param context The context used to access preferences and other application-level resources.
     */
    public SentryManager(Context context) {
        this.context = context;
    }

    /**
     * Captures an exception with Sentry. If Sentry is disabled (based on user preference) or if the exception
     * is one of the ignored types, logs the exception using Android's Log.e() but does not send it to Sentry.
     *
     * @param e The exception to capture or log.
     */
    public void captureException(Exception e) {
        if (isIgnored(e)) {
            Log.e(TAG, "Ignored error:", e);
            return;
        }

        if (isEnabled()) {
            // When enabled, capture the exception in Sentry for remote error tracking.
            Sentry.captureException(e);
            Log.e(TAG, "Got error:", e);
        } else {
            // If not enabled, just log the error locally.
            Log.e(TAG, "Got error:", e);
        }
    }

    /**
     * Logs a message as a breadcrumb in Sentry if enabled, or logs it using Log.d() otherwise.
     *
     * @param message The message to log or capture.
     */
    public void captureMessage(String message) {
        if (isEnabled()) {
            // If Sentry is enabled, add a breadcrumb (a trail of events leading to a crash or issue).
            Sentry.addBreadcrumb(message);
            Log.d(TAG, message);
        } else {
            // Otherwise, just log it using the standard Android log.
            Log.d(TAG, message);
        }
    }

    /**
     * Checks SharedPreferences to see if Sentry is enabled.
     * This preference ("sentry_enable") is assumed to be managed elsewhere, e.g., in a settings UI.
     *
     * @return true if the user has enabled Sentry, false otherwise.
     */
    public boolean isEnabled() {
        // Acquire a reference to the default shared preferences.
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        // Return the current state of the "sentry_enable" preference (false by default if not set).
        return sharedPreferences.getBoolean("sentry_enable", false);
    }

    /**
     * Checks whether the exception is an instance of any ignored error types.
     *
     * @param e The exception to check.
     * @return true if the exception should be ignored; false otherwise.
     */
    private boolean isIgnored(Exception e) {
        for (Class<? extends Exception> ignored : IGNORED_ERRORS) {
            if (ignored.isInstance(e)) {
                return true;
            }
        }
        return false;
    }
}
