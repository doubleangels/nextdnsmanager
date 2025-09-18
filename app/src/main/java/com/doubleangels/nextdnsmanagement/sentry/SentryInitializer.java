package com.doubleangels.nextdnsmanagement.sentry;

import android.content.Context;

import com.doubleangels.nextdnsmanagement.BuildConfig;

import io.sentry.android.core.SentryAndroid;

/**
 * A utility class for initializing Sentry in the application. It sets various
 * configuration options
 * for better error monitoring, performance tracing, and diagnostic
 * capabilities.
 */
public class SentryInitializer {

    /**
     * Initializes Sentry on a separate thread, providing DSN (Data Source Name),
     * release version,
     * and other advanced configuration options for capturing and tracking
     * application performance
     * and errors.
     *
     * @param context The application or activity context needed for Sentry's
     *                initialization.
     */
    public static void initialize(Context context) {
        new Thread(() -> SentryAndroid.init(context, options -> {
            // The DSN (Data Source Name) for your Sentry project:
            // This tells Sentry where to send the error data
            options.setDsn("https://8b52cc2148b94716a69c9a4f0c0b4513@o244019.ingest.us.sentry.io/6270764");

            // Set the release version in Sentry to match the app's version name
            // Useful for tracking errors that occur across different app releases
            options.setRelease(BuildConfig.VERSION_NAME);

            // Enable all auto-breadcrumbs. This helps capture UI events,
            // network calls, and more to provide context around errors
            options.enableAllAutoBreadcrumbs(true);

            // Attach a screenshot of the user's view hierarchy at the time of the error
            // This helps visualize the state of the UI when the crash or error occurred
            options.setAttachScreenshot(true);

            // Attach the full view hierarchy for debugging layout or view-related issues
            options.setAttachViewHierarchy(true);

            // Sets the sample rate for performance tracing (transactions)
            // 1.0 means all transactions are captured
            options.setTracesSampleRate(1.0);

            // Enables application start profiling, which measures cold/warm start
            // performance
            options.setEnableAppStartProfiling(true);

            // Enables ANR (Application Not Responding) detection to capture when the app
            // has blocked the UI thread for too long
            options.setAnrEnabled(true);

            // Allows collection of additional context such as environment info or other
            // device data
            options.setCollectAdditionalContext(true);

            // Enables frames tracking for performance monitoring
            // Tracks dropped or slow frames to measure UI responsiveness
            options.setEnableFramesTracking(true);

            // This is set again (likely redundant, but ensures it's explicitly enabled)
            options.setEnableAppStartProfiling(true);

            // Check if the device is rooted, adding extra context for debugging certain
            // crashes
            // that may be more likely on rooted devices
            options.setEnableRootCheck(true);

        })).start(); // Start the thread, so the initialization does not block the main thread
    }
}
