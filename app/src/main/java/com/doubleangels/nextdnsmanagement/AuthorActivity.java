package com.doubleangels.nextdnsmanagement;

import android.content.Context;
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
 * Activity displaying information about the author, including links to GitHub, email, and website.
 * It also initializes Sentry for error logging and adjusts UI elements such as the status bar.
 */
public class AuthorActivity extends AppCompatActivity {

    // Sentry manager instance for error capturing.
    public SentryManager sentryManager;

    /**
     * Called when the activity is starting. This is where most initialization should go.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down,
     *                           this Bundle contains the data it most recently supplied. Otherwise it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the UI layout for this activity.
        setContentView(R.layout.activity_author);

        // Initialize SentryManager to handle error logging.
        sentryManager = new SentryManager(this);

        // Try to initialize Sentry if it is enabled.
        try {
            if (sentryManager.isEnabled()) {
                SentryInitializer.initialize(this);
            }
        } catch (Exception e) {
            // Capture and log any exception that occurs during Sentry initialization.
            sentryManager.captureException(e);
        }

        // Setup the status bar appearance.
        try {
            setupStatusBarForActivity();
        } catch (Exception e) {
            // Capture and log any exception that occurs while setting up the status bar.
            sentryManager.captureException(e);
        }

        // Setup the personal links (GitHub, email, website) for the activity.
        try {
            setupPersonalLinks(sentryManager);
        } catch (Exception e) {
            // Capture and log any exception that occurs while setting up personal links.
            sentryManager.captureException(e);
        }
    }

    /**
     * Called when the activity is about to be destroyed.
     * Releases resources and clears the SentryManager.
     */
    @Override
    protected void onDestroy() {
        if (sentryManager != null) {
            // Release the reference to SentryManager.
            sentryManager = null;
        }
        super.onDestroy();
    }

    /**
     * Attach a new base context to the ContextWrapper. This method applies localization
     * based on the device configuration.
     *
     * @param newBase The new base context for this ContextWrapper.
     */
    @Override
    protected void attachBaseContext(Context newBase) {
        // Get current configuration from the base context.
        Configuration config = newBase.getResources().getConfiguration();
        // Retrieve the first locale from the configuration or use default locale.
        Locale newLocale = (!config.getLocales().isEmpty()) ? config.getLocales().get(0) : Locale.getDefault();
        // Create a new configuration with the desired locale.
        Configuration overrideConfig = new Configuration(config);
        overrideConfig.setLocale(newLocale);
        // Create a localized context using the new configuration.
        Context localizedContext = newBase.createConfigurationContext(overrideConfig);
        super.attachBaseContext(localizedContext);
    }

    /**
     * Configures the status bar appearance based on the device's theme (light/dark).
     * Uses the WindowInsetsController to adjust the appearance on supported Android versions.
     */
    private void setupStatusBarForActivity() {
        // Check if the OS version supports the new insets controller.
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            WindowInsetsController insetsController = getWindow().getInsetsController();
            if (insetsController != null) {
                // Determine if the current UI mode is light theme.
                boolean isLightTheme = (getResources().getConfiguration().uiMode
                        & Configuration.UI_MODE_NIGHT_MASK)
                        == Configuration.UI_MODE_NIGHT_NO;
                // Set the appearance of system bars based on the theme.
                insetsController.setSystemBarsAppearance(
                        isLightTheme ? WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS : 0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
            }
        }
    }

    /**
     * Initializes and sets up personal link buttons (GitHub, email, website) in the UI.
     * Each button launches an intent when clicked.
     *
     * @param sentryManager Instance used for capturing exceptions during intent launches.
     */
    public void setupPersonalLinks(SentryManager sentryManager) {
        // Find the ImageView buttons by their ID.
        ImageView githubButton = findViewById(R.id.githubImageView);
        ImageView emailButton = findViewById(R.id.emailImageView);
        ImageView websiteButton = findViewById(R.id.websiteImageView);

        // Set click listener for GitHub button.
        githubButton.setOnClickListener(view -> {
            // Get the GitHub profile URL from resources.
            String url = getString(R.string.github_profile_url);
            // Create an intent to view the URL.
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            try {
                // Start the activity to view the GitHub profile.
                startActivity(intent);
            } catch (Exception e) {
                // Capture and log any exception that occurs when launching the intent.
                sentryManager.captureException(e);
            }
        });

        // Set click listener for email button.
        emailButton.setOnClickListener(view -> {
            // Create an intent for sending an email.
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:nextdns@doubleangels.com"));
            try {
                // Start an email chooser activity.
                startActivity(Intent.createChooser(emailIntent, "Send Email"));
            } catch (Exception e) {
                // Capture and log any exception that occurs when launching the email intent.
                sentryManager.captureException(e);
            }
        });

        // Set click listener for website button.
        websiteButton.setOnClickListener(view -> {
            // Get the author's website URL from resources.
            String url = getString(R.string.author_url);
            // Create an intent to view the website.
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            try {
                // Start the activity to view the website.
                startActivity(intent);
            } catch (Exception e) {
                // Capture and log any exception that occurs when launching the website intent.
                sentryManager.captureException(e);
            }
        });
    }
}
