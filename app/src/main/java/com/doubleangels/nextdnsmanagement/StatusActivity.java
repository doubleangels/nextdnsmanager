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

        // Initialize Sentry with its own error handling.
        try {
            if (sentryManager.isEnabled()) {
                SentryInitializer.initialize(this);
            }
        } catch (Exception e) {
            sentryManager.captureException(e);
        }

        // Customize the status bar appearance.
        try {
            setupStatusBarForActivity();
        } catch (Exception e) {
            sentryManager.captureException(e);
        }

        // Apply locale configuration.
        try {
            String appLocale = setupLanguageForActivity();
            sentryManager.captureMessage("Using locale: " + appLocale);
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
    }

    /**
     * Adjusts the system bars' appearance based on the current theme.
     */
    private void setupStatusBarForActivity() {
        // Replace UPSIDE_DOWN_CAKE with an appropriate API level check.
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            WindowInsetsController insetsController = getWindow().getInsetsController();
            if (insetsController != null) {
                boolean isLightTheme = (getResources().getConfiguration().uiMode
                        & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO;
                insetsController.setSystemBarsAppearance(
                        isLightTheme ? WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS : 0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
            }
        }
    }

    /**
     * Configures the language settings based on the current locale.
     *
     * @return The language code (e.g., "en") representing the active locale.
     */
    private String setupLanguageForActivity() {
        Configuration config = getResources().getConfiguration();
        Locale appLocale = config.getLocales().get(0);
        Locale.setDefault(appLocale);

        Configuration newConfig = new Configuration(config);
        newConfig.setLocale(appLocale);

        // Override the base context's configuration to use the new locale.
        new ContextThemeWrapper(getBaseContext(), R.style.AppTheme)
                .applyOverrideConfiguration(newConfig);

        return appLocale.getLanguage();
    }
}
