package com.doubleangels.nextdnsmanagement.sharedpreferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.doubleangels.nextdnsmanagement.sentry.SentryManager;

import java.io.File;

/**
 * A thread-safe utility class for managing encrypted SharedPreferences.
 * This class provides convenient static methods for reading and writing
 * preference values with encryption at rest.
 * All errors are captured using SentryManager.
 */
public class SharedPreferencesManager {

    // The name of the SharedPreferences file.
    private static final String PREF_NAME = "MyAppPreferences";

    // Key to track if migration from unencrypted to encrypted has been completed.
    private static final String MIGRATION_COMPLETE_KEY = "_encryption_migration_complete";

    // Set when encrypted storage could not be initialized.
    private static final String ENCRYPTION_FALLBACK_KEY = "_encryption_fallback_active";

    // A reference to the SharedPreferences object, backed by the application
    // context.
    private static SharedPreferences sharedPreferences;

    // Store only the application context to avoid leaking an Activity context.
    private static Context appContext;

    private static volatile boolean usingEncryptedStorage = false;

    // Tag for logging.
    private static final String TAG = "SharedPreferencesManager";

    /**
     * Private constructor to prevent instantiation.
     * This class only offers static methods.
     */
    private SharedPreferencesManager() {
        throw new UnsupportedOperationException("Cannot instantiate SharedPreferencesManager.");
    }

    public static boolean isInitialized() {
        return sharedPreferences != null;
    }

    public static boolean isUsingEncryptedStorage() {
        return usingEncryptedStorage;
    }

    /**
     * Initializes the encrypted SharedPreferences object using the application
     * context.
     * Must be called once, usually in an Application class or before using any
     * put/get methods.
     * This method will:
     * 1. Create or retrieve an encrypted SharedPreferences instance
     * 2. Migrate existing unencrypted preferences if needed
     * 3. Fall back to unencrypted preferences if encryption fails
     *
     * @param context The context used to create or retrieve the SharedPreferences.
     */
    public static synchronized void init(Context context) {
        if (sharedPreferences == null) {
            appContext = context.getApplicationContext();
            try {
                // Create master key for encryption.
                MasterKey masterKey = new MasterKey.Builder(appContext)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build();

                // Create encrypted SharedPreferences.
                sharedPreferences = EncryptedSharedPreferences.create(
                        appContext,
                        PREF_NAME,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
                usingEncryptedStorage = true;

                // Check if migration from unencrypted to encrypted is needed.
                if (!sharedPreferences.getBoolean(MIGRATION_COMPLETE_KEY, false)) {
                    migrateUnencryptedPreferences(context);
                }
            } catch (Exception e) {
                handleEncryptionFailure(e);
            }
        }
    }

    private static void handleEncryptionFailure(Exception e) {
        Log.e(TAG, "Failed to initialize encrypted SharedPreferences; using plaintext fallback", e);
        SentryManager.captureStaticException(
                e instanceof Exception ? (Exception) e : new Exception(e));

        sharedPreferences = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        usingEncryptedStorage = false;

        // App lock must not rely on plaintext storage; disable it until encryption works.
        sharedPreferences.edit()
                .putBoolean(ENCRYPTION_FALLBACK_KEY, true)
                .putBoolean("app_lock_enable", false)
                .apply();
    }

    /**
     * Migrates existing unencrypted SharedPreferences to encrypted storage.
     * This is a one-time operation that runs on first use of encrypted preferences.
     *
     * @param context The context used to access the unencrypted preferences.
     */
    private static void migrateUnencryptedPreferences(Context context) {
        try {
            // Check if unencrypted preferences file exists.
            File unencryptedFile = new File(
                    context.getApplicationInfo().dataDir + "/shared_prefs/" + PREF_NAME + ".xml");
            if (!unencryptedFile.exists()) {
                // No unencrypted preferences to migrate.
                sharedPreferences.edit().putBoolean(MIGRATION_COMPLETE_KEY, true).apply();
                return;
            }

            // Read from unencrypted preferences.
            SharedPreferences unencryptedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor encryptedEditor = sharedPreferences.edit();

            // Migrate all existing key-value pairs.
            boolean hasData = false;
            java.util.Map<String, ?> allPrefs = unencryptedPrefs.getAll();
            for (String key : allPrefs.keySet()) {
                Object value = allPrefs.get(key);
                if (value instanceof String) {
                    encryptedEditor.putString(key, (String) value);
                    hasData = true;
                } else if (value instanceof Boolean) {
                    encryptedEditor.putBoolean(key, (Boolean) value);
                    hasData = true;
                } else if (value instanceof Integer) {
                    encryptedEditor.putInt(key, (Integer) value);
                    hasData = true;
                } else if (value instanceof Long) {
                    encryptedEditor.putLong(key, (Long) value);
                    hasData = true;
                } else if (value instanceof Float) {
                    encryptedEditor.putFloat(key, (Float) value);
                    hasData = true;
                }
            }

            // Mark migration as complete only after a successful copy.
            encryptedEditor.putBoolean(MIGRATION_COMPLETE_KEY, true);
            encryptedEditor.apply();

            // Delete unencrypted preferences file after successful migration.
            if (hasData) {
                try {
                    boolean deleted = unencryptedFile.delete();
                    if (deleted) {
                        Log.d(TAG, "Successfully migrated and deleted unencrypted preferences");
                    } else {
                        Log.w(TAG, "Migration completed but failed to delete unencrypted file");
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Failed to delete unencrypted preferences file", e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during preference migration; will retry on next launch", e);
            SentryManager.captureStaticException(e);
        }
    }

    /**
     * Saves a String value into the SharedPreferences asynchronously.
     * If an error occurs, it is captured via SentryManager.
     *
     * @param key   The preference key under which the value is stored.
     * @param value The string value to store.
     */
    public static void putString(String key, String value) {
        checkInitialization();
        try {
            sharedPreferences.edit().putString(key, value).apply();
        } catch (Exception e) {
            SentryManager.captureStaticException(e);
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
            SentryManager.captureStaticException(e);
            return defaultValue;
        }
    }

    /**
     * Saves a boolean value into the SharedPreferences asynchronously.
     * If an error occurs, it is captured via SentryManager.
     *
     * @param key   The preference key under which the value is stored.
     * @param value The boolean value to store.
     */
    public static void putBoolean(String key, boolean value) {
        checkInitialization();
        try {
            sharedPreferences.edit().putBoolean(key, value).apply();
        } catch (Exception e) {
            SentryManager.captureStaticException(e);
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
            SentryManager.captureStaticException(e);
            return defaultValue;
        }
    }

    public static void putLong(String key, long value) {
        checkInitialization();
        try {
            sharedPreferences.edit().putLong(key, value).apply();
        } catch (Exception e) {
            SentryManager.captureStaticException(e);
        }
    }

    public static long getLong(String key, long defaultValue) {
        checkInitialization();
        try {
            return sharedPreferences.getLong(key, defaultValue);
        } catch (Exception e) {
            SentryManager.captureStaticException(e);
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
