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

public class AuthorActivity extends AppCompatActivity {

    public SentryManager sentryManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_author);

        sentryManager = new SentryManager(this);

        // Initialize Sentry with its own error handling.
        try {
            if (sentryManager.isEnabled()) {
                SentryInitializer.initialize(this);
            }
        } catch (Exception e) {
            sentryManager.captureException(e);
        }

        // Setup status bar appearance separately.
        try {
            setupStatusBarForActivity();
        } catch (Exception e) {
            sentryManager.captureException(e);
        }

        // Apply locale configuration.
        String appLocale;
        try {
            appLocale = setupLanguageForActivity();
            sentryManager.captureMessage("Using locale: " + appLocale);
        } catch (Exception e) {
            sentryManager.captureException(e);
        }

        // Setup personal links with granular error handling.
        try {
            setupPersonalLinks(sentryManager);
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
    }

    /**
     * Adjusts the status bar appearance to match the UI theme.
     */
    private void setupStatusBarForActivity() {
        // UPSIDE_DOWN_CAKE is a placeholder; adjust your version check as needed.
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            WindowInsetsController insetsController = getWindow().getInsetsController();
            if (insetsController != null) {
                boolean isLightTheme = (getResources().getConfiguration().uiMode
                        & Configuration.UI_MODE_NIGHT_MASK)
                        == Configuration.UI_MODE_NIGHT_NO;
                insetsController.setSystemBarsAppearance(
                        isLightTheme ? WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS : 0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
            }
        }
    }

    /**
     * Applies the current locale configuration.
     * Returns the language code for logging purposes.
     */
    private String setupLanguageForActivity() {
        Configuration config = getResources().getConfiguration();
        Locale appLocale = (!config.getLocales().isEmpty()) ? config.getLocales().get(0) : Locale.getDefault();
        Locale.setDefault(appLocale);

        // Create a new configuration with the selected locale.
        Configuration newConfig = new Configuration(config);
        newConfig.setLocale(appLocale);
        getResources().updateConfiguration(newConfig, getResources().getDisplayMetrics());

        return appLocale.getLanguage();
    }

    /**
     * Sets up click listeners on ImageViews to launch external intents.
     */
    public void setupPersonalLinks(SentryManager sentryManager) {
        ImageView githubButton = findViewById(R.id.githubImageView);
        ImageView emailButton = findViewById(R.id.emailImageView);
        ImageView websiteButton = findViewById(R.id.websiteImageView);

        // GitHub profile link.
        githubButton.setOnClickListener(view -> {
            String url = getString(R.string.github_profile_url);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            if (intent.resolveActivity(getPackageManager()) != null) {
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    sentryManager.captureException(e);
                }
            } else {
                sentryManager.captureMessage("No application found to handle GitHub URL: " + url);
            }
        });

        // Email composer.
        emailButton.setOnClickListener(view -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:nextdns@doubleangels.com"));
            if (emailIntent.resolveActivity(getPackageManager()) != null) {
                try {
                    startActivity(Intent.createChooser(emailIntent, "Send Email"));
                } catch (Exception e) {
                    sentryManager.captureException(e);
                }
            } else {
                sentryManager.captureMessage("No email application found");
            }
        });

        // Website link.
        websiteButton.setOnClickListener(view -> {
            String url = getString(R.string.author_url);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            if (intent.resolveActivity(getPackageManager()) != null) {
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    sentryManager.captureException(e);
                }
            } else {
                sentryManager.captureMessage("No application found to handle website URL: " + url);
            }
        });
    }
}
