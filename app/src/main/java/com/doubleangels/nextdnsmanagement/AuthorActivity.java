package com.doubleangels.nextdnsmanagement;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowInsetsController;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.doubleangels.nextdnsmanagement.sentry.SentryInitializer;
import com.doubleangels.nextdnsmanagement.sentry.SentryManager;

import java.util.Locale;

/**
 * An Activity that displays author-related information and links (GitHub, email, website).
 * It also initializes Sentry if enabled, adjusts system bar appearance based on theme,
 * and applies the app's current locale.
 */
public class AuthorActivity extends AppCompatActivity {

    // SentryManager is used to conditionally send error logs/messages to Sentry.
    public SentryManager sentryManager;

    /**
     * Called when the activity is first created. 
     * Initializes Sentry if enabled, sets up status bar appearance, applies language settings,
     * and configures clickable image links (GitHub, Email, Website).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_author);

        // Initialize the SentryManager
        sentryManager = new SentryManager(this);

        try {
            // If Sentry is enabled, initialize it for error tracking and diagnostics
            if (sentryManager.isEnabled()) {
                SentryInitializer.initialize(this);
            }

            // Adjust the status bar appearance (e.g., light/dark status bar icons)
            setupStatusBarForActivity();

            // Apply the user’s locale configuration
            String appLocale = setupLanguageForActivity();
            sentryManager.captureMessage("Using locale: " + appLocale);

            // Initialize the clickable image links for external sites/emails
            setupPersonalLinks(sentryManager);
        } catch (Exception e) {
            // Log any caught exceptions in Sentry if enabled
            sentryManager.captureException(e);
        }
    }

    /**
     * Sets up the status bar’s appearance. On newer versions of Android,
     * this can toggle light/dark icon styles to match the UI theme.
     */
    private void setupStatusBarForActivity() {
        // UPSIDE_DOWN_CAKE is a placeholder; adjust the version check as necessary.
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            WindowInsetsController insetsController = getWindow().getInsetsController();
            if (insetsController != null) {
                // Determine if we are in a light theme (e.g., day mode)
                boolean isLightTheme = (getResources().getConfiguration().uiMode 
                        & Configuration.UI_MODE_NIGHT_MASK) 
                        == Configuration.UI_MODE_NIGHT_NO;

                // Set system bar appearance based on whether it’s a light or dark theme.
                insetsController.setSystemBarsAppearance(
                        isLightTheme ? WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS : 0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
            }
        }
    }

    /**
     * Applies the current locale settings from the device or app config.
     * Returns the language code (e.g., "en", "fr") for logging purposes.
     *
     * @return A string representing the current language in use.
     */
    private String setupLanguageForActivity() {
        // Get the current configuration
        Configuration config = getResources().getConfiguration();
        // Obtain the first locale from that configuration
        Locale appLocale = config.getLocales().get(0);
        // Set the default locale to this app’s locale
        Locale.setDefault(appLocale);

        // Create a new configuration based on the current one, and set the locale
        Configuration newConfig = new Configuration(config);
        newConfig.setLocale(appLocale);

        // Return the language code (e.g., "en")
        return appLocale.getLanguage();
    }

    /**
     * Sets up ImageView click listeners to launch external intents:
     * - GitHub profile
     * - Email composer with a preset address
     * - Website URL
     */
    public void setupPersonalLinks(SentryManager sentryManager) {
        try {
            // Retrieve references to the ImageView buttons in the layout
            ImageView githubButton = findViewById(R.id.githubImageView);
            ImageView emailButton = findViewById(R.id.emailImageView);
            ImageView websiteButton = findViewById(R.id.websiteImageView);

            // Open GitHub profile on click
            githubButton.setOnClickListener(view -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, 
                        Uri.parse(getString(R.string.github_profile_url)));
                startActivity(intent);
            });

            // Open email app with the preset email address on click
            emailButton.setOnClickListener(view -> {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(Uri.parse("mailto:nextdns@doubleangels.com"));
                startActivity(Intent.createChooser(emailIntent, "Send Email"));
            });

            // Open author’s website on click
            websiteButton.setOnClickListener(view -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, 
                        Uri.parse(getString(R.string.author_url)));
                startActivity(intent);
            });
        } catch (Exception e) {
            // Log exceptions if any occur during setup
            sentryManager.captureException(e);
        }
    }
}
