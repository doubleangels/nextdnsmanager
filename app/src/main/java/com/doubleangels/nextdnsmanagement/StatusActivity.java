package com.doubleangels.nextdnsmanagement;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowInsetsController;

import androidx.appcompat.app.AppCompatActivity;

import com.doubleangels.nextdnsmanagement.sentry.SentryInitializer;
import com.doubleangels.nextdnsmanagement.sentry.SentryManager;

import java.util.Locale;

/**
 * Activity for displaying status information. It initializes Sentry for error logging,
 * configures the status bar appearance based on the device's UI mode, and sets the appropriate locale.
 */
public class StatusActivity extends AppCompatActivity {

    // Instance of SentryManager for capturing errors.
    public SentryManager sentryManager;

    /**
     * Called when the activity is starting. Initializes Sentry and sets up the status bar appearance.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down,
     *                           this Bundle contains the data it most recently supplied. Otherwise, it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the layout for this activity.
        setContentView(R.layout.activity_status);

        // Initialize the SentryManager instance.
        sentryManager = new SentryManager(this);
        try {
            // Initialize Sentry if it is enabled.
            if (sentryManager.isEnabled()) {
                SentryInitializer.initialize(this);
            }
        } catch (Exception e) {
            // Capture any exception that occurs during Sentry initialization.
            sentryManager.captureException(e);
        }
        try {
            // Configure the status bar appearance.
            setupStatusBarForActivity();
        } catch (Exception e) {
            // Capture any exception that occurs during status bar setup.
            sentryManager.captureException(e);
        }
    }

    /**
     * Called when the activity is about to be destroyed. Releases the SentryManager instance.
     */
    @Override
    protected void onDestroy() {
        if (sentryManager != null) {
            // Clear the reference to SentryManager.
            sentryManager = null;
        }
        super.onDestroy();
    }

    /**
     * Attaches a new base context with locale settings based on the device configuration.
     *
     * @param newBase The new base context.
     */
    @Override
    protected void attachBaseContext(Context newBase) {
        // Retrieve the current configuration from the base context.
        Configuration config = newBase.getResources().getConfiguration();
        // Determine the new locale from the configuration or default if no locales are set.
        Locale newLocale = (!config.getLocales().isEmpty()) ? config.getLocales().get(0) : Locale.getDefault();
        // Create an override configuration with the new locale.
        Configuration overrideConfig = new Configuration(config);
        overrideConfig.setLocale(newLocale);
        // Create a localized context with the overridden configuration.
        Context localizedContext = newBase.createConfigurationContext(overrideConfig);
        super.attachBaseContext(localizedContext);
    }

    /**
     * Configures the status bar appearance based on the current UI mode (light or dark).
     * Uses WindowInsetsController on supported Android versions.
     */
    private void setupStatusBarForActivity() {
        // Check if the current Android version supports WindowInsetsController.
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            WindowInsetsController insetsController = getWindow().getInsetsController();
            if (insetsController != null) {
                // Determine if the device is in light theme mode.
                boolean isLightTheme = (getResources().getConfiguration().uiMode
                        & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO;
                // Adjust the appearance of system bars based on the theme.
                insetsController.setSystemBarsAppearance(
                        isLightTheme ? WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS : 0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
            }
        }
    }
}
