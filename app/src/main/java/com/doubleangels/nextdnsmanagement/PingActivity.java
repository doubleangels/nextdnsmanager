package com.doubleangels.nextdnsmanagement;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.WindowInsetsController;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import com.doubleangels.nextdnsmanagement.sentry.SentryInitializer;
import com.doubleangels.nextdnsmanagement.sentry.SentryManager;

import java.util.Locale;

/**
 * An Activity that loads two WebViews with specified URLs (e.g., for ping tests).
 * It also integrates Sentry error tracking, applies language settings, and 
 * adjusts system bar appearance for light/dark modes.
 */
public class PingActivity extends AppCompatActivity {

    // SentryManager instance for capturing logs and errors (if enabled by user preference)
    public SentryManager sentryManager;

    // Two separate WebView components for displaying different URLs simultaneously
    public WebView webView;
    public WebView webView2;

    /**
     * Called when the activity is first created. Sets up layout, initializes Sentry if enabled,
     * applies system bar styling and language settings, and configures two WebViews for loading URLs.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ping);

        // Initialize a SentryManager for handling logs/exceptions
        sentryManager = new SentryManager(this);

        try {
            // If Sentry is enabled (based on user preference), initialize the Sentry SDK
            if (sentryManager.isEnabled()) {
                SentryInitializer.initialize(this);
            }

            // Customize status bar icons (light/dark) for newer Android versions
            setupStatusBarForActivity();

            // Apply and capture the current locale setting
            String appLocale = setupLanguageForActivity();
            sentryManager.captureMessage("Using locale: " + appLocale);

            // Configure two WebViews with designated URLs
            setupWebViewForActivity(getString(R.string.ping_url), getString(R.string.test_url));
        } catch (Exception e) {
            // Capture any exceptions in Sentry (if enabled) or log them
            sentryManager.captureException(e);
        }
    }

    /**
     * Called when the activity is destroyed. Removes views from the WebView and destroys it 
     * to free resources and avoid memory leaks.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        webView.removeAllViews();
        webView.destroy();
    }

    /**
     * Sets up the status bar's appearance (light or dark icons) if the device runs on a newer Android version.
     */
    private void setupStatusBarForActivity() {
        // Replace UPSIDE_DOWN_CAKE with the appropriate API level check in real implementations
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            WindowInsetsController insetsController = getWindow().getInsetsController();
            if (insetsController != null) {
                // Determine whether the current theme is in night mode or not
                boolean isLightTheme = (getResources().getConfiguration().uiMode &
                        Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO;

                // Apply light or default status bar appearance to match the theme
                insetsController.setSystemBarsAppearance(
                        isLightTheme ? WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS : 0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
            }
        }
    }

    /**
     * Sets the app's language based on the current configuration. This ensures 
     * the app resources (strings, layouts, etc.) match the selected locale.
     *
     * @return The language code (e.g., "en") representing the current locale.
     */
    private String setupLanguageForActivity() {
        // Retrieve the current Configuration
        Configuration config = getResources().getConfiguration();

        // Get the first locale from the configuration
        Locale appLocale = config.getLocales().get(0);
        Locale.setDefault(appLocale);

        // Create a new Configuration for overriding the locale
        Configuration newConfig = new Configuration(config);
        newConfig.setLocale(appLocale);

        // Apply the override configuration using a ContextThemeWrapper
        new ContextThemeWrapper(getBaseContext(), R.style.AppTheme)
                .applyOverrideConfiguration(newConfig);

        // Return the language code for logging
        return appLocale.getLanguage();
    }

    /**
     * Sets up both WebViews in the activity layout with provided URLs.
     *
     * @param url1 The first URL to load (e.g., ping test).
     * @param url2 The second URL to load (e.g., additional test page).
     */
    @SuppressLint("SetJavaScriptEnabled")
    public void setupWebViewForActivity(String url1, String url2) {
        // Find the WebViews from the layout
        webView = findViewById(R.id.webView);
        webView2 = findViewById(R.id.webView2);

        // Initialize each WebView with common settings
        setupWebView(webView);
        setupWebView(webView2);

        // Load the desired URLs
        webView.loadUrl(url1);
        webView2.loadUrl(url2);
    }

    /**
     * Configures an individual WebView with JavaScript and storage settings, 
     * and sets a simple WebViewClient.
     *
     * @param webView A WebView instance to configure.
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(WebView webView) {
        // Get the WebView's settings and enable commonly used features
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);

        // Prevent loading pages in external browser by setting a WebViewClient
        webView.setWebViewClient(new WebViewClient());
    }
}
