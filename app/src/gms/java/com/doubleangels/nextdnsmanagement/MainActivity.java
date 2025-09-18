package com.doubleangels.nextdnsmanagement;

import static android.Manifest.permission.POST_NOTIFICATIONS;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.doubleangels.nextdnsmanagement.biometriclock.BiometricLock;
import com.doubleangels.nextdnsmanagement.firebasemessaging.MessagingInitializer;
import com.doubleangels.nextdnsmanagement.protocol.VisualIndicator;
import com.doubleangels.nextdnsmanagement.sentry.SentryInitializer;
import com.doubleangels.nextdnsmanagement.sentry.SentryManager;
import com.doubleangels.nextdnsmanagement.sharedpreferences.SharedPreferencesManager;
import com.doubleangels.nextdnsmanagement.webview.WebAppInterface;

import java.util.Locale;

/**
 * Main Activity class that handles initialization of the UI, WebView, and
 * various settings
 * such as dark mode, locale, biometric re-authentication, and notification
 * permission checks.
 */
public class MainActivity extends AppCompatActivity {

    // Main WebView for displaying web content.
    private WebView webView;
    // SwipeRefreshLayout wrapping the WebView to enable pull-to-refresh
    // functionality.
    private SwipeRefreshLayout swipeRefreshLayout;
    // Flag indicating whether dark mode is enabled.
    private Boolean darkModeEnabled = false;
    // Flag to avoid re-initializing the WebView if it has already been set up.
    private Boolean isWebViewInitialized = false;
    // Bundle used to store and restore the WebView state across configuration
    // changes.
    private Bundle webViewState = null;
    // Biometric authentication timeout in milliseconds (5 minutes).
    private static final long AUTH_TIMEOUT_MS = 5 * 60 * 1000;
    // Timestamp (in ms) of the last successful biometric authentication.
    private long lastAuthenticatedTime = 0;

    // Blur overlay view to hide content during biometric authentication.
    private View blurOverlay;
    // SentryManager instance for error logging.
    private SentryManager sentryManager;

    /**
     * Saves the current state of the activity, including the WebView state and dark
     * mode flag.
     *
     * @param outState Bundle in which to save the activity state.
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        try {
            // Save the WebView state if it exists.
            if (webView != null) {
                Bundle webViewBundle = new Bundle();
                webView.saveState(webViewBundle);
                outState.putBundle("webViewState", webViewBundle);
            }
            // Save the dark mode flag.
            outState.putBoolean("darkModeEnabled", darkModeEnabled);
        } catch (Exception e) {
            SentryManager.captureStaticException(e);
        }
    }

    /**
     * Called when the activity is created.
     * Initializes Sentry, messaging, UI components, language, dark mode, visual
     * indicator,
     * swipe-to-refresh, and the WebView.
     *
     * @param savedInstanceState Bundle containing the activity's previously saved
     *                           state.
     */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Restore previously saved state if available.
        if (savedInstanceState != null) {
            webViewState = savedInstanceState.getBundle("webViewState");
            darkModeEnabled = savedInstanceState.getBoolean("darkModeEnabled");
        }
        setContentView(R.layout.activity_main);

        // Initialize blur overlay
        blurOverlay = findViewById(R.id.blurOverlay);

        // Initialize Sentry for error logging, Firebase Messaging, and
        // SharedPreferences.
        sentryManager = new SentryManager(this);
        MessagingInitializer.initialize(this);
        SharedPreferencesManager.init(this);

        try {
            if (sentryManager.isEnabled()) {
                sentryManager.captureMessage("Sentry is enabled for NextDNS Manager.");
                SentryInitializer.initialize(this);
            }
        } catch (Exception e) {
            sentryManager.captureException(e);
        }

        // Setup UI components and configurations.
        try {
            setupStatusBarForActivity();
            setupToolbarForActivity();
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
        try {
            String appLocale = setupLanguageForActivity();
            sentryManager.captureMessage("Using locale: " + appLocale);
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
        try {
            setupDarkModeForActivity(sentryManager, SharedPreferencesManager.getString("dark_mode", "match"));
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
        try {
            setupVisualIndicatorForActivity(sentryManager, this);
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
        try {
            setupSwipeToRefreshForActivity();
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
        try {
            // Setup the WebView with the main URL.
            setupWebViewForActivity(getString(R.string.main_url));
        } catch (Exception e) {
            sentryManager.captureException(e);
        }

        // Check biometric authentication on app start
        if (SharedPreferencesManager.getBoolean("app_lock_enable", true)) {
            if (shouldAuthenticate()) {
                showBiometricPrompt();
            }
        }
    }

    /**
     * Called when the activity is destroyed.
     * Cleans up the WebView and SwipeRefreshLayout to avoid memory leaks.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            webViewState = null;
            // Remove WebView from its parent if attached.
            if (webView.getParent() != null) {
                ((ViewGroup) webView.getParent()).removeView(webView);
            }
            if (webView != null) {
                // Remove JavaScript interfaces and clients.
                webView.removeJavascriptInterface("SwipeToRefreshInterface");
                webView.setWebViewClient(new WebViewClient());
                webView.setWebChromeClient(new WebChromeClient());
                webView.setDownloadListener(null);
                // Destroy the WebView.
                webView.destroy();
                webView = null;
            }
            if (swipeRefreshLayout != null) {
                // Remove refresh listener.
                swipeRefreshLayout.setOnRefreshListener(null);
                swipeRefreshLayout = null;
            }
        } catch (Exception e) {
            SentryManager.captureStaticException(e);
        } finally {
            webView = null;
        }
    }

    /**
     * Pauses the WebView when the activity is paused.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
        }
    }

    /**
     * Handles results from other activities to ensure biometric authentication is
     * maintained.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Check biometric authentication when returning from other activities
        if (SharedPreferencesManager.getBoolean("app_lock_enable", true)) {
            if (shouldAuthenticate()) {
                showBiometricPrompt();
            }
        }
    }

    /**
     * Resumes the WebView and triggers biometric authentication if needed.
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Resume WebView if it exists; otherwise, initialize it.
        if (webView != null) {
            webView.onResume();
        } else if (!isWebViewInitialized) {
            setupWebViewForActivity(getString(R.string.main_url));
        }
        SharedPreferencesManager.init(this);
        // Refresh dark mode settings when returning from settings
        setupDarkModeForActivity(sentryManager, SharedPreferencesManager.getString("dark_mode", "match"));
        // Check if app lock is enabled and if biometric authentication is needed.
        if (SharedPreferencesManager.getBoolean("app_lock_enable", true)) {
            if (shouldAuthenticate()) {
                // Show biometric prompt immediately - don't wait for WebView state
                showBiometricPrompt();
            }
        }
    }

    /**
     * Determines whether biometric authentication is required based on the timeout.
     *
     * @return true if the elapsed time since last authentication exceeds the
     *         timeout.
     */
    private boolean shouldAuthenticate() {
        return System.currentTimeMillis() - lastAuthenticatedTime > AUTH_TIMEOUT_MS;
    }

    /**
     * Configures the status bar for the activity.
     * For newer SDK versions, sets the background color using an
     * OnApplyWindowInsetsListener.
     */
    private void setupStatusBarForActivity() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            getWindow().getDecorView().setOnApplyWindowInsetsListener((view, insets) -> {
                view.setBackgroundColor(ContextCompat.getColor(this, R.color.main));
                return insets;
            });
        }
    }

    /**
     * Sets up the toolbar for the activity.
     * Configures the action bar and assigns a click listener to the connection
     * status icon.
     */
    private void setupToolbarForActivity() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Disable default title display.
            actionBar.setDisplayShowTitleEnabled(false);
        }
        // Launch StatusActivity when the connection status icon is clicked.
        ImageView imageView = findViewById(R.id.connectionStatus);
        imageView.setOnClickListener(v -> startActivity(new Intent(this, StatusActivity.class)));
    }

    /**
     * Configures the language settings for the activity.
     * Applies the current locale to the application's configuration.
     *
     * @return The language code of the current locale.
     */
    private String setupLanguageForActivity() {
        Configuration config = getResources().getConfiguration();
        Locale appLocale = config.getLocales().get(0);
        Locale.setDefault(appLocale);
        Configuration newConfig = new Configuration(config);
        newConfig.setLocale(appLocale);
        new ContextThemeWrapper(getBaseContext(), R.style.AppTheme).applyOverrideConfiguration(newConfig);
        return appLocale.getLanguage();
    }

    /**
     * Sets up dark mode based on user preference.
     * Configures the AppCompatDelegate accordingly and logs the setting.
     *
     * @param sentryManager SentryManager instance for logging.
     * @param darkMode      Dark mode setting ("match", "on", "disabled", "off").
     */
    private void setupDarkModeForActivity(SentryManager sentryManager, String darkMode) {
        switch (darkMode) {
            case "match":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                updateDarkModeState();
                sentryManager.captureMessage("Dark mode set to follow system.");
                break;
            case "on":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                darkModeEnabled = true;
                sentryManager.captureMessage("Dark mode set to on.");
                break;
            case "disabled":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                darkModeEnabled = false;
                sentryManager.captureMessage("Dark mode is disabled due to SDK version.");
                break;
            case "off":
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                darkModeEnabled = false;
                sentryManager.captureMessage("Dark mode set to off.");
                break;
        }
    }

    /**
     * Updates the internal dark mode flag based on the current UI configuration.
     */
    private void updateDarkModeState() {
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        darkModeEnabled = (nightModeFlags == Configuration.UI_MODE_NIGHT_YES);
    }

    /**
     * Initializes a visual indicator (e.g., a network or loading indicator) using
     * the VisualIndicator class.
     *
     * @param sentryManager  SentryManager instance for logging.
     * @param lifecycleOwner LifecycleOwner to manage the indicator's lifecycle.
     */
    private void setupVisualIndicatorForActivity(SentryManager sentryManager, LifecycleOwner lifecycleOwner) {
        try {
            new VisualIndicator(this).initialize(this, lifecycleOwner, this);
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
    }

    /**
     * Configures and loads the WebView with the specified URL.
     * Restores saved state if dark mode is enabled and state exists.
     *
     * @param url The URL to load in the WebView.
     */
    @SuppressLint("SetJavaScriptEnabled")
    public void setupWebViewForActivity(String url) {
        webView = findViewById(R.id.webView);
        try {
            // Load URL directly if dark mode is disabled; otherwise, try to restore state.
            if (!darkModeEnabled) {
                webViewState = null;
                webView.loadUrl(url);
            } else {
                if (webViewState != null) {
                    webView.restoreState(webViewState);
                } else {
                    webView.loadUrl(url);
                }
            }
        } catch (Exception e) {
            SentryManager.captureStaticException(e);
        }
        // Configure WebView settings.
        WebSettings webViewSettings = webView.getSettings();
        webViewSettings.setJavaScriptEnabled(true);
        webViewSettings.setDomStorageEnabled(true);
        webViewSettings.setDatabaseEnabled(true);
        webViewSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webViewSettings.setAllowFileAccess(false);
        webViewSettings.setAllowContentAccess(false);

        // Set a custom WebViewClient to handle page events.
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                try {
                    // Enable and flush cookies.
                    CookieManager.getInstance().setAcceptCookie(true);
                    CookieManager.getInstance().acceptCookie();
                    CookieManager.getInstance().flush();
                    // Inject JavaScript to monitor modal dialogs and disable/enable swipe refresh.
                    String js = "setInterval(function() {" +
                            "   var modal = document.querySelector('.modal-dialog.modal-lg.modal-dialog-scrollable');" +
                            "   if (modal) {" +
                            "       if (!modal.getAttribute('data-listeners-attached')) {" +
                            "           modal.setAttribute('data-listeners-attached', 'true');" +
                            "           modal.addEventListener('touchstart', function(){" +
                            "               AndroidInterface.setSwipeRefreshEnabled(false);" +
                            "           });" +
                            "           modal.addEventListener('touchend', function(){" +
                            "               AndroidInterface.setSwipeRefreshEnabled(true);" +
                            "           });" +
                            "       }" +
                            "   }" +
                            "}, 500);";
                    view.evaluateJavascript(js, null);
                } catch (Exception e) {
                    SentryManager.captureStaticException(e);
                }
            }
        });

        // Set a WebChromeClient to handle JavaScript dialogs and progress updates.
        webView.setWebChromeClient(new WebChromeClient());

        // Enable algorithmic darkening if dark mode is enabled and supported.
        if (Boolean.TRUE.equals(darkModeEnabled)) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.getSettings(), true);
            }
        }
        // Setup file download handling.
        setupDownloadManagerForActivity();
        isWebViewInitialized = true;
    }

    /**
     * Configures the SwipeRefreshLayout to allow pull-to-refresh on the WebView.
     * Also adds a JavaScript interface to control swipe refresh behavior from web
     * content.
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void setupSwipeToRefreshForActivity() {
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        // Add a JavaScript interface for controlling swipe refresh.
        webView.addJavascriptInterface(new WebAppInterface(this, swipeRefreshLayout), "AndroidInterface");
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Reload the WebView when the user swipes to refresh.
            webView.reload();
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    /**
     * Configures the download manager to handle file downloads initiated within the
     * WebView.
     * Downloads are saved to the app's external downloads directory.
     */
    private void setupDownloadManagerForActivity() {
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            try {
                // Create a new download request.
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url.trim()));
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS,
                        "NextDNS-Configuration.mobileconfig");
                DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                if (downloadManager != null) {
                    // Enqueue the download request.
                    downloadManager.enqueue(request);
                } else {
                    throw new Exception("DownloadManager is null");
                }
                Toast.makeText(getApplicationContext(), "Downloading file!", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                SentryManager.captureStaticException(e);
                Toast.makeText(MainActivity.this, "Download failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Shows the blur overlay to hide WebView content during biometric
     * authentication.
     */
    private void showBlurOverlay() {
        if (blurOverlay != null) {
            // Set overlay color based on current theme
            int overlayColor;
            if (darkModeEnabled) {
                // Dark mode: #212529
                overlayColor = android.graphics.Color.parseColor("#212529");
            } else {
                // Light mode: #007bff
                overlayColor = android.graphics.Color.parseColor("#007bff");
            }

            blurOverlay.setBackgroundColor(overlayColor);
            blurOverlay.setVisibility(View.VISIBLE);
            blurOverlay.setScaleX(0.95f);
            blurOverlay.setScaleY(0.95f);
            blurOverlay.animate()
                    .alpha(1.0f)
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(300)
                    .start();
        }
    }

    /**
     * Hides the blur overlay after biometric authentication is complete.
     */
    private void hideBlurOverlay() {
        if (blurOverlay != null) {
            blurOverlay.animate()
                    .alpha(0.0f)
                    .scaleX(1.05f)
                    .scaleY(1.05f)
                    .setDuration(300)
                    .withEndAction(() -> {
                        blurOverlay.setVisibility(View.GONE);
                        blurOverlay.setScaleX(1.0f);
                        blurOverlay.setScaleY(1.0f);
                    })
                    .start();
        }
    }

    /**
     * Displays a biometric prompt to the user if authentication is available.
     * On successful authentication, updates the last authentication time and, if
     * necessary,
     * requests notification permission.
     */
    private void showBiometricPrompt() {
        final BiometricLock biometricLock = new BiometricLock(this);
        if (biometricLock.canAuthenticate()) {
            // Show blur overlay to hide WebView content
            showBlurOverlay();

            biometricLock.showPrompt(
                    getString(R.string.unlock_title),
                    getString(R.string.unlock_description),
                    "",
                    new BiometricLock.BiometricLockCallback() {
                        @Override
                        public void onAuthenticationSucceeded() {
                            // Update the last authenticated time.
                            lastAuthenticatedTime = System.currentTimeMillis();
                            // Hide blur overlay on successful authentication
                            hideBlurOverlay();
                            // Check for notification permission on supported devices.
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                if (ContextCompat.checkSelfPermission(MainActivity.this,
                                        POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
                                    ActivityCompat.requestPermissions(MainActivity.this,
                                            new String[] { POST_NOTIFICATIONS },
                                            2);
                                }
                            }
                        }

                        @Override
                        public void onAuthenticationError(String error) {
                            // Hide blur overlay before finishing
                            hideBlurOverlay();
                            // Finish the activity on authentication error.
                            finish();
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            // Hide blur overlay before finishing
                            hideBlurOverlay();
                            // Finish the activity on authentication failure.
                            finish();
                        }
                    });
        }
    }

    /**
     * Inflates the options menu.
     *
     * @param menu The menu in which items are placed.
     * @return true for the menu to be displayed.
     */
    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Handles selections from the options menu.
     *
     * @param item The selected menu item.
     * @return The result of the menu item selection.
     */
    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Determine action based on selected menu item.
        switch (item.getItemId()) {
            case R.id.back:
                // Navigate back in the WebView history.
                if (webView == null) {
                    setupWebViewForActivity(getString(R.string.main_url));
                } else {
                    webView.goBack();
                }
                break;
            case R.id.refreshNextDNS:
                // Reload the WebView.
                if (webView == null) {
                    setupWebViewForActivity(getString(R.string.main_url));
                } else {
                    webView.reload();
                }
                break;
            case R.id.pingNextDNS:
                // Launch the PingActivity.
                startActivity(new Intent(this, PingActivity.class));
                break;
            case R.id.returnHome:
                // Load the main URL in the WebView.
                if (webView == null) {
                    setupWebViewForActivity(getString(R.string.main_url));
                } else {
                    webView.loadUrl(getString(R.string.main_url));
                }
                break;
            case R.id.privateDNS:
                // Open wireless settings.
                startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                break;
            case R.id.settings:
                // Launch the SettingsActivity.
                startActivity(new Intent(this, SettingsActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
