package com.doubleangels.nextdnsmanagement;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.ComponentCallbacks2;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.LifecycleOwner;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.doubleangels.nextdnsmanagement.firebasemessaging.MessagingInitializer;
import com.doubleangels.nextdnsmanagement.protocol.VisualIndicator;
import com.doubleangels.nextdnsmanagement.sentry.SentryInitializer;
import com.doubleangels.nextdnsmanagement.sentry.SentryManager;
import com.doubleangels.nextdnsmanagement.sharedpreferences.SharedPreferencesManager;

import java.util.Locale;

/**
 * Main Activity class that handles initialization of the UI, WebView, and various settings
 * like dark mode, locale, etc. Also contains logic for handling low-memory events
 * and navigation from the options menu.
 */
public class MainActivity extends AppCompatActivity {

    // Main WebView for displaying content
    private WebView webView;
    // Indicates whether dark mode is currently enabled
    private Boolean darkModeEnabled = false;
    // Indicates if the WebView is initialized to avoid re-initialization
    private Boolean isWebViewInitialized = false;
    // Used to save/restore WebView state across configuration changes
    private Bundle webViewState = null;

    /**
     * Saves the current state of the activity, including the WebView state and dark mode setting.
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) {
            Bundle webViewBundle = new Bundle();
            // Save the current state of the WebView
            webView.saveState(webViewBundle);
            outState.putBundle("webViewState", webViewBundle);
        }
        // Save the current dark mode state
        outState.putBoolean("darkModeEnabled", darkModeEnabled);
    }

    /**
     * Called when the activity is first created. Restores saved state if available,
     * initializes necessary components, and sets up the UI (toolbar, WebView, dark mode, etc).
     */
    @SuppressLint("WrongThread")
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Restore saved instance state if present (e.g., after rotation)
        if (savedInstanceState != null) {
            webViewState = savedInstanceState.getBundle("webViewState");
            darkModeEnabled = savedInstanceState.getBoolean("darkModeEnabled");
        }

        // Set the content view for this activity
        setContentView(R.layout.activity_main);

        // Initialize Sentry manager for error tracking and logging
        SentryManager sentryManager = new SentryManager(this);

        // Initialize Firebase Messaging for push notifications
        MessagingInitializer.initialize(this);

        // Initialize SharedPreferences manager for storing/retrieving settings
        SharedPreferencesManager.init(this);

        try {
            // Enable and initialize Sentry if allowed
            if (sentryManager.isEnabled()) {
                sentryManager.captureMessage("Sentry is enabled for NextDNS Manager.");
                SentryInitializer.initialize(this);
            }

            // Set up the status bar with custom insets, etc.
            setupStatusBarForActivity();

            // Set up the toolbar (action bar) and hide the title
            setupToolbarForActivity();

            // Apply the language settings from the device/app configuration
            String appLocale = setupLanguageForActivity();
            sentryManager.captureMessage("Using locale: " + appLocale);

            // Determine and apply dark mode preference
            setupDarkModeForActivity(
                sentryManager,
                SharedPreferencesManager.getString("dark_mode", "match")
            );

            // Set up any additional UI indicators
            setupVisualIndicatorForActivity(sentryManager, this);

            // Initialize the WebView, load the main URL
            setupWebViewForActivity(getString(R.string.main_url));
        } catch (Exception e) {
            // Capture any exceptions in Sentry for analysis
            sentryManager.captureException(e);
        }
    }

    /**
     * Called when the activity is destroyed. Cleans up the WebView to free resources.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanupWebView();
    }

    /**
     * Called when the app is running low on memory. If the WebView is initialized,
     * destroy it to free resources.
     */
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (!isWebViewInitialized) {
            return;
        }
        cleanupWebView();
    }

    /**
     * Called when the app should trim memory. If the trim level is high enough,
     * the WebView is destroyed to free up resources.
     */
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            if (!isWebViewInitialized) {
                return;
            }
            cleanupWebView();
        }
    }

    /**
     * Called when the activity is paused. Pauses the WebView as well (if initialized)
     * to avoid unnecessary resource usage.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
        }
    }

    /**
     * Called when the activity is resumed. Resumes the WebView if it was previously initialized.
     * If not initialized, sets up a new WebView instance.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
        } else if (!isWebViewInitialized) {
            setupWebViewForActivity(getString(R.string.main_url));
        }
    }

    /**
     * Example setup method for the status bar, applying custom insets if applicable.
     * This is potentially for newer Android versions.
     */
    private void setupStatusBarForActivity() {
        // UPSIDE_DOWN_CAKE is a placeholder. Replace with appropriate version checks if needed.
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Window window = getWindow();
            // Set a listener for applying window insets (to modify background, etc.)
            window.getDecorView().setOnApplyWindowInsetsListener((view, insets) -> {
                // Set a background color for the view
                view.setBackgroundColor(getResources().getColor(R.color.main));
                return insets;
            });
        }
    }

    /**
     * Sets up the toolbar by hiding the default title and adding a click listener
     * to an icon that navigates to a status activity.
     */
    private void setupToolbarForActivity() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Hide the activity title in the action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }

        // Set up an ImageView to act as a clickable "connection status" icon
        ImageView imageView = findViewById(R.id.connectionStatus);
        imageView.setOnClickListener(v -> startActivity(new Intent(this, StatusActivity.class)));
    }

    /**
     * Reads the current language from the system configuration and applies it.
     * Returns the language code for logging or debugging.
     */
    private String setupLanguageForActivity() {
        Configuration config = getResources().getConfiguration();

        // Retrieve the first locale from the configuration
        Locale appLocale = config.getLocales().get(0);
        // Set the default locale
        Locale.setDefault(appLocale);

        // Create a new config object and set its locale
        Configuration newConfig = new Configuration(config);
        newConfig.setLocale(appLocale);

        // Apply the new configuration
        new ContextThemeWrapper(getBaseContext(), R.style.AppTheme).applyOverrideConfiguration(newConfig);

        // Return the language code, e.g., "en" or "fr"
        return appLocale.getLanguage();
    }

    /**
     * Reads the user's dark mode preference from SharedPreferences and sets the appropriate mode.
     * Logs any changes or actions to Sentry for debugging.
     */
    private void setupDarkModeForActivity(SentryManager sentryManager, String darkMode) {
        switch (darkMode) {
            case "match":
                // Follow the system's light/dark setting
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                updateDarkModeState();
                sentryManager.captureMessage("Dark mode set to follow system.");
                break;
            case "on":
                // Always use dark mode
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                darkModeEnabled = true;
                sentryManager.captureMessage("Dark mode set to on.");
                break;
            case "disabled":
                // Disable dark mode if the SDK version doesn't support it
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                darkModeEnabled = false;
                sentryManager.captureMessage("Dark mode is disabled due to SDK version.");
                break;
            case "off":
            default:
                // Default to light mode
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                darkModeEnabled = false;
                sentryManager.captureMessage("Dark mode set to off.");
                break;
        }
    }

    /**
     * Checks the system configuration to update the darkModeEnabled flag.
     * This is helpful when following the system night mode setting.
     */
    private void updateDarkModeState() {
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        switch (nightModeFlags) {
            case Configuration.UI_MODE_NIGHT_YES:
                darkModeEnabled = true;
                break;
            case Configuration.UI_MODE_NIGHT_NO:
                darkModeEnabled = false;
                break;
        }
    }

    /**
     * Sets up a visual indicator (e.g., a connectivity indicator or loading status)
     * that operates alongside this activity's lifecycle.
     */
    private void setupVisualIndicatorForActivity(SentryManager sentryManager, LifecycleOwner lifecycleOwner) {
        try {
            // VisualIndicator might manage overlays or UI elements to show status
            new VisualIndicator(this).initialize(this, lifecycleOwner, this);
        } catch (Exception e) {
            // Capture any initialization errors
            sentryManager.captureException(e);
        }
    }

    /**
     * Cleans up the WebView by loading a blank page, removing all views, and destroying the WebView.
     * This frees up memory and resources.
     */
    private void cleanupWebView() {
        if (webView != null) {
            try {
                webView.loadUrl("about:blank");
                webView.removeAllViews();
                webView.destroy();
            } finally {
                webView = null;
                isWebViewInitialized = false;
            }
        }
    }

    /**
     * Initializes and configures the WebView, including JavaScript, DOM storage,
     * caching, and download handling. Optionally restores a previously saved WebView state.
     *
     * @param url The URL to load if there is no saved WebView state.
     */
    @SuppressLint("SetJavaScriptEnabled")
    public void setupWebViewForActivity(String url) {
        // Find the WebView in the layout
        webView = findViewById(R.id.webView);

        // Restore any previously saved state
        if (webViewState != null) {
            webView.restoreState(webViewState);
        } else {
            // If there is no saved state, simply load the main URL
            webView.loadUrl(url);
        }

        // Configure various WebView settings
        WebSettings webViewSettings = webView.getSettings();
        webViewSettings.setJavaScriptEnabled(true);
        webViewSettings.setDomStorageEnabled(true);
        webViewSettings.setDatabaseEnabled(true);
        webViewSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webViewSettings.setAllowFileAccess(false);
        webViewSettings.setAllowContentAccess(false);

        // Set a custom WebViewClient to handle events like page loading finished
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView webView, String url) {
                // Enable cookies
                CookieManager.getInstance().setAcceptCookie(true);
                CookieManager.getInstance().acceptCookie();
                // Flush cookies to ensure persistence
                CookieManager.getInstance().flush();
            }
        });

        // Enable or disable algorithmic darkening for WebView if dark mode is active
        if (Boolean.TRUE.equals(darkModeEnabled)) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.getSettings(), true);
            }
        }

        // Set up the Download Manager for handling file downloads in the WebView
        setupDownloadManagerForActivity();

        // If we have no saved state, (re)load the URL now that settings are done
        webView.loadUrl(url);

        // Mark that the WebView is fully initialized
        isWebViewInitialized = true;
    }

    /**
     * Sets up a listener to handle file downloads from within the WebView using the system
     * DownloadManager. It saves files to the app's external files directory by default.
     */
    private void setupDownloadManagerForActivity() {
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            // Create a DownloadManager request for the file URL
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url.trim()));
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            // Set download destination to external files directory (Downloads)
            request.setDestinationInExternalFilesDir(
                this,
                Environment.DIRECTORY_DOWNLOADS,
                "NextDNS-Configuration.mobileconfig"
            );

            // Enqueue the download request
            DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (downloadManager != null) {
                downloadManager.enqueue(request);
            }
            // Show a toast indicating the download has started
            Toast.makeText(getApplicationContext(), "Downloading file!", Toast.LENGTH_LONG).show();
        });
    }

    /**
     * Helper method to start a new activity based on the given class.
     *
     * @param targetClass The Activity class to start.
     */
    private void startIntent(Class<?> targetClass) {
        Intent intent = new Intent(this, targetClass);
        startActivity(intent);
    }

    /**
     * Initializes the options menu from a resource.
     *
     * @param menu The menu to inflate into the activity.
     * @return true if the menu was created successfully.
     */
    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Handles option item selections, such as going back, refreshing, or opening various settings.
     *
     * @param item The selected menu item.
     * @return true if the event was handled successfully.
     */
    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.back:
                if (webView == null) {
                    // Re-set up the WebView if it's null
                    setupWebViewForActivity(getString(R.string.main_url));
                } else {
                    // Go back in the WebView history
                    webView.goBack();
                }
                break;
            case R.id.refreshNextDNS:
                if (webView == null) {
                    // Re-set up the WebView if it's null
                    setupWebViewForActivity(getString(R.string.main_url));
                } else {
                    // Reload the current WebView page
                    webView.reload();
                }
                break;
            case R.id.pingNextDNS:
                // Start PingActivity
                startIntent(PingActivity.class);
                break;
            case R.id.returnHome:
                if (webView == null) {
                    // Re-set up the WebView if it's null
                    setupWebViewForActivity(getString(R.string.main_url));
                } else {
                    // Navigate to the main URL in the WebView
                    webView.loadUrl(getString(R.string.main_url));
                }
                break;
            case R.id.privateDNS:
                // Launch system settings for Private DNS
                startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                break;
            case R.id.settings:
                // Launch the app's custom SettingsActivity
                startIntent(SettingsActivity.class);
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
