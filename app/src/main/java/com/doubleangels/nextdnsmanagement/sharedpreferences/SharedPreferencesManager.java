package com.doubleangels.nextdnsmanagement.sharedpreferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import com.doubleangels.nextdnsmanagement.sentry.SentryManager;

/**
 * A thread-safe utility class for managing SharedPreferences.
 * This class provides convenient static methods for reading and writing
 * preference values.
 * All errors are captured using SentryManager.
 */
public class SharedPreferencesManager {

    // The name of the SharedPreferences file
    private static final String PREF_NAME = "MyAppPreferences";

    // A reference to the SharedPreferences object, backed by the application
    // context
    private static SharedPreferences sharedPreferences;

    // Store only the application context to avoid leaking an Activity context
    private static Context appContext;

    /**
     * Private constructor to prevent instantiation.
     * This class only offers static methods.
     */
    private SharedPreferencesManager() {
        throw new UnsupportedOperationException("Cannot instantiate SharedPreferencesManager.");
    }

    /**
     * Initializes the SharedPreferences object using the application context.
     * Must be called once, usually in an Application class or before using any
     * put/get methods.
     *
     * @param context The context used to create or retrieve the SharedPreferences.
     */
    public static synchronized void init(Context context) {
        if (sharedPreferences == null) {
            appContext = context.getApplicationContext();
            sharedPreferences = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
    }

    /**
     * Saves a String value into the SharedPreferences, committing the change
     * immediately.
     * If an error occurs, it is captured via SentryManager.
     *
     * @param key   The preference key under which the value is stored.
     * @param value The string value to store.
     */
    @SuppressLint("ApplySharedPref")
    public static void putString(String key, String value) {
        checkInitialization();
        try {
            sharedPreferences.edit().putString(key, value).commit();
        } catch (Exception e) {
            new SentryManager(appContext).captureException(e);
        }
    }

    /**
     * Retrieves a String value from the SharedPreferences.
     * If the key is not found or an error occurs, returns the provided default
     * value.
     *
     * @param key          The preference key to look for.
     * @param defaultValue The default value to return if not found.
     * @return The stored string value or the default if not found.
     */
    public static String getString(String key, String defaultValue) {
        checkInitialization();
        try {
            return sharedPreferences.getString(key, defaultValue);
        } catch (Exception e) {
            new SentryManager(appContext).captureException(e);
            return defaultValue;
        }
    }

    /**
     * Saves a boolean value into the SharedPreferences, committing the change
     * immediately.
     * If an error occurs, it is captured via SentryManager.
     *
     * @param key   The preference key under which the value is stored.
     * @param value The boolean value to store.
     */
    @SuppressLint("ApplySharedPref")
    public static void putBoolean(String key, boolean value) {
        checkInitialization();
        try {
            sharedPreferences.edit().putBoolean(key, value).commit();
        } catch (Exception e) {
            new SentryManager(appContext).captureException(e);
        }
    }

    /**
     * Retrieves a boolean value from the SharedPreferences.
     * If the key is not found or an error occurs, returns the provided default
     * value.
     *
     * @param key          The preference key to look for.
     * @param defaultValue The default value to return if not found.
     * @return The stored boolean value or the default if not found.
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        checkInitialization();
        try {
            return sharedPreferences.getBoolean(key, defaultValue);
        } catch (Exception e) {
            new SentryManager(appContext).captureException(e);
            return defaultValue;
        }
    }

    /**
     * A helper method that ensures the SharedPreferences has been initialized.
     * Throws an exception if init() has not been called beforehand.
     */
    private static void checkInitialization() {
        if (sharedPreferences == null) {
            throw new IllegalStateException(
                    "SharedPreferencesManager is not initialized. Call init() before using it.");
        }
    }
}
