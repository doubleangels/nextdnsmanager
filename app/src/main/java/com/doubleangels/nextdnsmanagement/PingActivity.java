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

public class PingActivity extends AppCompatActivity {

    public SentryManager sentryManager;
    public WebView webView;
    public WebView webView2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ping);

        sentryManager = new SentryManager(this);

        // Initialize Sentry separately.
        try {
            if (sentryManager.isEnabled()) {
                SentryInitializer.initialize(this);
            }
        } catch (Exception e) {
            sentryManager.captureException(e);
        }

        // Setup status bar appearance.
        try {
            setupStatusBarForActivity();
        } catch (Exception e) {
            sentryManager.captureException(e);
        }

        // Apply language configuration.
        String appLocale;
        try {
            appLocale = setupLanguageForActivity();
            sentryManager.captureMessage("Using locale: " + appLocale);
        } catch (Exception e) {
            sentryManager.captureException(e);
        }

        // Configure the WebViews with the designated URLs.
        try {
            setupWebViewForActivity(getString(R.string.ping_url), getString(R.string.test_url));
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
    }

    @Override
    protected void onDestroy() {
        try {
            if (webView != null) {
                webView.removeAllViews();
                webView.destroy();
            }
            if (webView2 != null) {
                webView2.removeAllViews();
                webView2.destroy();
            }
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
        super.onDestroy();
    }

    /**
     * Adjusts the status bar's appearance based on the current theme.
     */
    private void setupStatusBarForActivity() {
        // Replace UPSIDE_DOWN_CAKE with an appropriate API level check.
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            WindowInsetsController insetsController = getWindow().getInsetsController();
            if (insetsController != null) {
                boolean isLightTheme = (getResources().getConfiguration().uiMode &
                        Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO;
                insetsController.setSystemBarsAppearance(
                        isLightTheme ? WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS : 0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
            }
        }
    }

    /**
     * Sets up the language configuration for the activity.
     *
     * @return The language code (e.g., "en") representing the current locale.
     */
    private String setupLanguageForActivity() {
        Configuration config = getResources().getConfiguration();
        Locale appLocale = (!config.getLocales().isEmpty()) ? config.getLocales().get(0) : Locale.getDefault();
        Locale.setDefault(appLocale);

        Configuration newConfig = new Configuration(config);
        newConfig.setLocale(appLocale);

        // Apply the new configuration using a ContextThemeWrapper.
        new ContextThemeWrapper(getBaseContext(), R.style.AppTheme)
                .applyOverrideConfiguration(newConfig);

        return appLocale.getLanguage();
    }

    /**
     * Configures both WebViews with the provided URLs.
     *
     * @param url1 The first URL to load.
     * @param url2 The second URL to load.
     */
    @SuppressLint("SetJavaScriptEnabled")
    public void setupWebViewForActivity(String url1, String url2) {
        try {
            webView = findViewById(R.id.webView);
            webView2 = findViewById(R.id.webView2);

            setupWebView(webView);
            setupWebView(webView2);

            webView.loadUrl(url1);
            webView2.loadUrl(url2);
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
    }

    /**
     * Configures an individual WebView with necessary settings and a custom WebViewClient
     * that logs loading errors.
     *
     * @param webView The WebView instance to configure.
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(WebView webView) {
        try {
            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);
            settings.setCacheMode(WebSettings.LOAD_DEFAULT);
            settings.setAllowFileAccess(false);
            settings.setAllowContentAccess(false);

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    super.onReceivedError(view, errorCode, description, failingUrl);
                    if (sentryManager != null) {
                        sentryManager.captureMessage("Error loading URL " + failingUrl + ": " + description);
                    }
                }
            });
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
    }
}
