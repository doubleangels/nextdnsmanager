package com.doubleangels.nextdnsmanagement;

import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.WindowInsetsController;

import androidx.appcompat.app.AppCompatActivity;

import com.doubleangels.nextdnsmanagement.sentry.SentryInitializer;
import com.doubleangels.nextdnsmanagement.sentry.SentryManager;

import java.util.Locale;

public class StatusActivity extends AppCompatActivity {

    public SentryManager sentryManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);
        sentryManager = new SentryManager(this);
        try {package com.doubleangels.nextdnsmanagement;

import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.WindowInsetsController;

import androidx.appcompat.app.AppCompatActivity;

import com.doubleangels.nextdnsmanagement.sentry.SentryInitializer;
import com.doubleangels.nextdnsmanagement.sentry.SentryManager;

import java.util.Locale;

/**
 * A simple activity that displays current status information about the app or device
 * (if desired) and integrates with Sentry for error logging and localization.
 */
public class StatusActivity extends AppCompatActivity {

    // Manages logging to Sentry if the user has enabled it
    public SentryManager sentryManager;

    /**
     * Called when the activity is first created. Sets the content view, 
     * initializes Sentry (if enabled), configures the status bar, and 
     * applies the current locale.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);

        // Initialize the SentryManager for conditional logging/tracking
        sentryManager = new SentryManager(this);

        try {
            // Check if Sentry is enabled. If true, initialize it for error tracking.
            if (sentryManager.isEnabled()) {
                SentryInitializer.initialize(this);
            }

            // Customize the status bar appearance based on theme (light or dark)
            setupStatusBarForActivity();

            // Apply the user's or system's locale settings, and log which language is in use
            String appLocale = setupLanguageForActivity();
            sentryManager.captureMessage("Using locale: " + appLocale);

        } catch (Exception e) {
            // Capture any exceptions in Sentry if enabled, or log them locally
            sentryManager.captureException(e);
        }
    }

    /**
     * Adjusts the appearance of system bars (e.g., status bar icons) for newer Android versions.
     * It sets light or dark icons according to whether the current theme is in night mode or not.
     */
    private void setupStatusBarForActivity() {
        // Replace UPSIDE_DOWN_CAKE with the appropriate version check as needed
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            WindowInsetsController insetsController = getWindow().getInsetsController();
            if (insetsController != null) {
                // Determine if the system is in light (day) theme or night theme
                boolean isLightTheme = (getResources().getConfiguration().uiMode 
                        & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO;

                // If light theme, set light icons, otherwise revert to default/dark icons
                insetsController.setSystemBarsAppearance(
                        isLightTheme ? WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS : 0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
            }
        }
    }

    /**
     * Reads the current configuration to obtain the active locale, then overrides
     * the base context's locale setting with this value. This ensures the UI is 
     * displayed in the correct language. Returns the language code (e.g., "en").
     *
     * @return The language code representing the current locale in use.
     */
    private String setupLanguageForActivity() {
        // Get the current configuration from the system
        Configuration config = getResources().getConfiguration();

        // Retrieve the first locale from the configuration
        Locale appLocale = config.getLocales().get(0);
        // Set the default locale to match
        Locale.setDefault(appLocale);

        // Create a new configuration and set its locale
        Configuration newConfig = new Configuration(config);
        newConfig.setLocale(appLocale);

        // Apply the new locale configuration via a ContextThemeWrapper
        new ContextThemeWrapper(getBaseContext(), R.style.AppTheme).applyOverrideConfiguration(newConfig);

        // Return the language code, useful for debugging/logging
        return appLocale.getLanguage();
    }
}

            if (sentryManager.isEnabled()) {
                SentryInitializer.initialize(this);
            }
            setupStatusBarForActivity();
            String appLocale = setupLanguageForActivity();
            sentryManager.captureMessage("Using locale: " + appLocale);
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
    }

    private void setupStatusBarForActivity() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            WindowInsetsController insetsController = getWindow().getInsetsController();
            if (insetsController != null) {
                boolean isLightTheme = (getResources().getConfiguration().uiMode &
                        android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                        android.content.res.Configuration.UI_MODE_NIGHT_NO;
                insetsController.setSystemBarsAppearance(
                        isLightTheme ? WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS : 0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
            }
        }
    }

    private String setupLanguageForActivity() {
        Configuration config = getResources().getConfiguration();
        Locale appLocale = config.getLocales().get(0);
        Locale.setDefault(appLocale);
        Configuration newConfig = new Configuration(config);
        newConfig.setLocale(appLocale);
        new ContextThemeWrapper(getBaseContext(), R.style.AppTheme).applyOverrideConfiguration(newConfig);
        return appLocale.getLanguage();
    }
}
