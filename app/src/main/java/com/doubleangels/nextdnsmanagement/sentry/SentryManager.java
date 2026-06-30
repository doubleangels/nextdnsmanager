package com.doubleangels.nextdnsmanagement.sentry;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.util.Log;

import com.doubleangels.nextdnsmanagement.sharedpreferences.SharedPreferencesManager;

import java.lang.ref.WeakReference;

import java.io.EOFException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

import io.sentry.Sentry;
import io.sentry.SentryOptions;
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
            ActivityNotFoundException.class,
            ConnectException.class,
            EOFException.class,
            SecurityException.class,
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
    }

    /**
     * Transient Firebase Messaging / Installations errors that reflect device or
     * Play Services state, not app bugs.
     */
    private static final List<String> TRANSIENT_FIREBASE_MESSAGING_ERRORS = Arrays.asList(
            "SERVICE_NOT_AVAILABLE",
            "FIS_AUTH_ERROR",
            "MISSING_INSTANCEID_SERVICE",
            "Firebase Installations Service is unavailable",
            "TIMEOUT");

    /**
     * Transient network errors from OkHttp or connectivity checks — not app bugs.
     */
    private static final List<String> TRANSIENT_NETWORK_ERROR_MESSAGES = Arrays.asList(
            "canceled",
            "cancel",
            "port out of range",
            "exhausted all routes",
            "failed to connect",
            "timeout",
            "unexpected end of stream",
            "stream was reset",
            "connection reset",
            "broken pipe",
            "connection closed",
            "socket closed",
            "settings preface not received",
            "authenticate with proxy",
            "read timed out",
            "connection closed");

    /**
     * Checks whether the provided throwable (including its cause chain) matches an
     * ignored error type.
     * <p>
     * This static method is available for both static and instance contexts.
     * </p>
     *
     * @param throwable The throwable to check.
     * @return true if the throwable should be ignored; false otherwise.
     */
    public static boolean isIgnored(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            for (Class<? extends Exception> ignored : IGNORED_ERRORS) {
                if (ignored.isInstance(current)) {
                    return true;
                }
            }
            if (isTransientFirebaseMessagingError(current)) {
                return true;
            }
            if (isTransientNetworkError(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean isTransientFirebaseMessagingError(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null) {
            return false;
        }
        for (String error : TRANSIENT_FIREBASE_MESSAGING_ERRORS) {
            if (message.contains(error)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTransientNetworkError(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase();
        for (String pattern : TRANSIENT_NETWORK_ERROR_MESSAGES) {
            if (lower.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Registers ignored exception types with the Sentry SDK so auto-captured errors
     * (e.g. from OkHttp instrumentation) are dropped before transmission.
     */
    public static void registerIgnoredExceptionTypes(SentryOptions options) {
        for (Class<? extends Exception> ignored : IGNORED_ERRORS) {
            options.addIgnoredExceptionForType(ignored);
        }
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

        if (isSentryEnabledStatic()) {
            Sentry.captureException(e);
        }
        Log.e(TAG, "Got error:", e);
    }

    private static boolean isSentryEnabledStatic() {
        if (!SharedPreferencesManager.isInitialized()) {
            return false;
        }
        try {
            return SharedPreferencesManager.getBoolean("sentry_enable", false);
        } catch (Exception e) {
            Log.e(TAG, "Error checking Sentry enable status", e);
            return false;
        }
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
        if (context == null || !SharedPreferencesManager.isInitialized()) {
            return false;
        }
        try {
            return SharedPreferencesManager.getBoolean("sentry_enable", false);
        } catch (Exception e) {
            Log.e(TAG, "Error checking Sentry enable status", e);
            return false;
        }
    }
}