package com.doubleangels.nextdnsmanagement;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.WindowInsetsController;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import com.doubleangels.nextdnsmanagement.sentry.SentryInitializer;
import com.doubleangels.nextdnsmanagement.sentry.SentryManager;

import java.util.Locale;

/**
 * Activity that handles ping functionality by loading two web pages in separate
 * WebViews.
 * It initializes Sentry for error logging, sets up the status bar appearance,
 * and configures the WebViews.
 */
public class PingActivity extends AppCompatActivity {

    // Sentry manager instance for error capturing.
    public SentryManager sentryManager;
    // First WebView instance for displaying the primary URL.
    public WebView webView;
    // Second WebView instance for displaying the secondary URL.
    public WebView webView2;

    /**
     * Called when the activity is created. Initializes Sentry, configures the
     * status bar,
     * and sets up the WebViews with the provided URLs.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down,
     *                           this Bundle contains the data it most recently
     *                           supplied. Otherwise, it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the layout for this activity.
        setContentView(R.layout.activity_ping);

        // Initialize the SentryManager for error logging.
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
            // Capture any exception during the status bar setup.
            sentryManager.captureException(e);
        }
        try {
            // Setup the two WebViews with their respective URLs.
            setupWebViewsForActivity(getString(R.string.ping_url), getString(R.string.test_url));
        } catch (Exception e) {
            // Capture any exception during WebView setup.
            sentryManager.captureException(e);
        }
    }

    /**
     * Called when the activity is about to be destroyed.
     * Cleans up WebView resources by removing them from their parent and destroying
     * them.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            // Remove and destroy the first WebView.
            if (webView != null) {
                if (webView.getParent() != null) {
                    ((ViewGroup) webView.getParent()).removeView(webView);
                }
                webView.setWebViewClient(new WebViewClient());
                webView.destroy();
                webView = null;
            }
            // Remove and destroy the second WebView.
            if (webView2 != null) {
                if (webView2.getParent() != null) {
                    ((ViewGroup) webView2.getParent()).removeView(webView2);
                }
                webView2.setWebViewClient(new WebViewClient());
                webView2.destroy();
                webView2 = null;
            }
        } catch (Exception e) {
            // Capture any exception using a static method.
            SentryManager.captureStaticException(e);
        } finally {
            // Ensure webView is set to null.
            webView = null;
        }
    }

    /**
     * Attaches a new base context with the locale configured based on device
     * settings.
     *
     * @param newBase The new base context.
     */
    @Override
    protected void attachBaseContext(Context newBase) {
        // Retrieve current configuration.
        Configuration config = newBase.getResources().getConfiguration();
        // Determine the new locale from the configuration or use the default locale.
        Locale newLocale = (!config.getLocales().isEmpty()) ? config.getLocales().get(0) : Locale.getDefault();
        // Create an override configuration with the new locale.
        Configuration overrideConfig = new Configuration(config);
        overrideConfig.setLocale(newLocale);
        // Create a localized context based on the new configuration.
        Context localizedContext = newBase.createConfigurationContext(overrideConfig);
        super.attachBaseContext(localizedContext);
    }

    /**
     * Configures the status bar appearance based on the current UI theme (light or
     * dark).
     * Uses the WindowInsetsController on supported Android versions.
     */
    private void setupStatusBarForActivity() {
        // Check if the OS version supports WindowInsetsController.
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            WindowInsetsController insetsController = getWindow().getInsetsController();
            if (insetsController != null) {
                // Check if the current UI mode is light theme.
                boolean isLightTheme = (getResources().getConfiguration().uiMode &
                        Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO;
                // Adjust the system bars appearance based on the theme.
                insetsController.setSystemBarsAppearance(
                        isLightTheme ? WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS : 0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
            }
        }
    }

    /**
     * Configures and loads two WebViews with the given URLs.
     *
     * @param url1 URL to be loaded in the first WebView.
     * @param url2 URL to be loaded in the second WebView.
     */
    @SuppressLint("SetJavaScriptEnabled")
    public void setupWebViewsForActivity(String url1, String url2) {
        try {
            // Retrieve WebView instances from the layout.
            webView = findViewById(R.id.webView);
            webView2 = findViewById(R.id.webView2);
            // Configure each WebView.
            setupWebView(webView);
            setupWebView(webView2);
            // Load URLs into the respective WebViews.
            webView.loadUrl(url1);
            webView2.loadUrl(url2);
        } catch (Exception e) {
            // Capture any exception that occurs during WebView setup.
            sentryManager.captureException(e);
        }
    }

    /**
     * Configures a single WebView's settings for optimal performance and security.
     *
     * @param webView The WebView to configure.
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(WebView webView) {
        try {
            // Retrieve WebSettings from the WebView.
            WebSettings settings = webView.getSettings();
            // Enable JavaScript execution.
            settings.setJavaScriptEnabled(true);
            // Enable DOM storage.
            settings.setDomStorageEnabled(true);
            // Enable database storage.
            settings.setDatabaseEnabled(true);
            // Set cache mode to load default content.
            settings.setCacheMode(WebSettings.LOAD_DEFAULT);
            // Disable file and content access for enhanced security.
            settings.setAllowFileAccess(false);
            settings.setAllowContentAccess(false);
            // Set a WebViewClient to handle page navigation.
            webView.setWebViewClient(new WebViewClient());
        } catch (Exception e) {
            // Capture any exception that occurs during WebView configuration.
            sentryManager.captureException(e);
        }
    }
}
