package com.doubleangels.nextdnsmanagement.sentry;

import android.content.Context;
import android.util.Log;

import com.doubleangels.nextdnsmanagement.sharedpreferences.SharedPreferencesManager;

import java.lang.ref.WeakReference;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

import io.sentry.Sentry;
import okhttp3.internal.http2.ConnectionShutdownException;

/**
 * A helper class to manage Sentry error logging.
 * It checks user preferences to see if Sentry is enabled before capturing
 * messages and exceptions.
 */
public class SentryManager {

    /** Tag for logging purposes. */
    private static final String TAG = "SentryManager";

    /**
     * List of exception types to ignore when capturing exceptions.
     * These exceptions will not be sent to Sentry.
     */
    private static final List<Class<? extends Exception>> IGNORED_ERRORS = Arrays.asList(
            UnknownHostException.class,
            SocketTimeoutException.class,
            SocketException.class,
            SSLException.class,
            SSLHandshakeException.class,
            ConnectionShutdownException.class);

    /** Android context used for accessing SharedPreferences and other resources. */
    private final WeakReference<Context> contextRef;

    /**
     * Constructs a new SentryManager instance.
     *
     * @param context The context used to access preferences and other
     *                application-level resources.
     */
    public SentryManager(Context context) {
        this.contextRef = new WeakReference<>(context);
        // Ensure SharedPreferencesManager is initialized
        if (context != null) {
            try {
                SharedPreferencesManager.init(context);
            } catch (Exception e) {
                // If initialization fails, log but don't throw
                Log.e(TAG, "Failed to initialize SharedPreferencesManager", e);
            }
        }
    }

    /**
     * Checks whether the provided exception is an instance of any ignored error
     * types.
     * <p>
     * This static method is available for both static and instance contexts.
     * </p>
     *
     * @param e The exception to check.
     * @return true if the exception should be ignored; false otherwise.
     */
    public static boolean isIgnored(Exception e) {
        for (Class<? extends Exception> ignored : IGNORED_ERRORS) {
            if (ignored.isInstance(e)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Captures an exception using Sentry.
     * <p>
     * If the exception is one of the ignored types or Sentry is disabled in user
     * preferences,
     * the exception is logged locally using Log.e() rather than being sent to
     * Sentry.
     * </p>
     *
     * @param e The exception to capture or log.
     */
    public void captureException(Exception e) {
        if (SentryManager.isIgnored(e)) {
            Log.e(TAG, "Ignored error:", e);
            return;
        }

        if (isEnabled()) {
            // When enabled, capture the exception in Sentry for remote error tracking
            Sentry.captureException(e);
            Log.e(TAG, "Got error:", e);
        } else {
            // If not enabled, just log the error locally
            Log.e(TAG, "Got error:", e);
        }
    }

    /**
     * Captures an exception using Sentry in a static context.
     * <p>
     * If the exception is one of the ignored types, the error is logged locally
     * using Log.e()
     * without sending it to Sentry. This method assumes Sentry is enabled or uses a
     * default behavior.
     * </p>
     *
     * @param e The exception to capture or log.
     */
    public static void captureStaticException(Exception e) {
        if (isIgnored(e)) {
            Log.e(TAG, "Ignored error:", e);
            return;
        }

        // In a static context, we assume Sentry is enabled
        Sentry.captureException(e);
        Log.e(TAG, "Got error:", e);
    }

    /**
     * Logs a message as a breadcrumb in Sentry if enabled,
     * or logs it using Log.d() otherwise.
     *
     * @param message The message to log or capture.
     */
    public void captureMessage(String message) {
        if (isEnabled()) {
            // If Sentry is enabled, add a breadcrumb (a trail of events leading to a crash
            // or issue)
            Sentry.addBreadcrumb(message);
            Log.d(TAG, message);
        } else {
            // Otherwise, log the message locally
            Log.d(TAG, message);
        }
    }

    /**
     * Checks SharedPreferences to determine whether Sentry is enabled.
     * <p>
     * The preference ("sentry_enable") is assumed to be managed elsewhere, for
     * example, in a settings UI.
     * </p>
     *
     * @return true if Sentry is enabled by the user; false otherwise.
     */
    public boolean isEnabled() {
        Context context = contextRef.get();
        if (context == null) {
            return false; // Context has been garbage collected
        }
        try {
            // Ensure SharedPreferencesManager is initialized
            SharedPreferencesManager.init(context);
            return SharedPreferencesManager.getBoolean("sentry_enable", false);
        } catch (Exception e) {
            // If there's an error accessing preferences, default to disabled
            Log.e(TAG, "Error checking Sentry enable status", e);
            return false;
        }
    }
}