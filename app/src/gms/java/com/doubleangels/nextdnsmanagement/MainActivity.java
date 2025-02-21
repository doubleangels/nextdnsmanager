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
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
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

import jp.wasabeef.blurry.Blurry;

/**
 * Main Activity class that handles initialization of the UI, WebView, and various settings
 * such as dark mode and locale. It also includes logic for handling low-memory events,
 * biometric re-authentication with a timeout, and a blurry overlay until the user authenticates.
 */
public class MainActivity extends AppCompatActivity {

    // Main WebView for displaying web content.
    private WebView webView;
    // SwipeRefreshLayout that wraps the WebView for pull-to-refresh functionality.
    private SwipeRefreshLayout swipeRefreshLayout;
    // Flag indicating whether dark mode is enabled.
    private Boolean darkModeEnabled = false;
    // Flag to avoid re-initializing the WebView if already initialized.
    private Boolean isWebViewInitialized = false;
    // Bundle to store and restore the WebView state across configuration changes.
    private Bundle webViewState = null;

    // Biometric authentication timeout in milliseconds (2 minutes)
    private static final long AUTH_TIMEOUT_MS = 2 * 60 * 1000;
    // Timestamp (in ms) of the last successful authentication.
    private long lastAuthenticatedTime = 0;

    /**
     * Saves the current state of the activity, including the WebView state and dark mode flag.
     *
     * @param outState Bundle in which to place saved state data.
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) {
            Bundle webViewBundle = new Bundle();
            // Save the current state of the WebView.
            webView.saveState(webViewBundle);
            outState.putBundle("webViewState", webViewBundle);
        }
        // Save the current dark mode state.
        outState.putBoolean("darkModeEnabled", darkModeEnabled);
    }

    /**
     * Called when the activity is created. Restores saved state if available, sets up the UI
     * (toolbar, WebView, dark mode, etc.), applies the blur overlay, and initializes necessary services.
     *
     * @param savedInstanceState The saved state of the activity, if any.
     */
    @SuppressLint({"WrongThread", "SetJavaScriptEnabled"})
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Restore saved state if available (e.g., after rotation).
        if (savedInstanceState != null) {
            webViewState = savedInstanceState.getBundle("webViewState");
            darkModeEnabled = savedInstanceState.getBoolean("darkModeEnabled");
        }

        // Set the content view for this activity.
        setContentView(R.layout.activity_main);

        // Apply the blurry overlay immediately to the SwipeRefreshLayout (web content container)
        showBlurryOverlay();

        // Initialize Sentry manager for error tracking and logging.
        SentryManager sentryManager = new SentryManager(this);

        // Initialize Firebase Messaging for push notifications.
        MessagingInitializer.initialize(this);

        // Initialize SharedPreferences manager for storing/retrieving settings.
        SharedPreferencesManager.init(this);

        try {
            // If Sentry is enabled, capture a startup message and initialize Sentry.
            if (sentryManager.isEnabled()) {
                sentryManager.captureMessage("Sentry is enabled for NextDNS Manager.");
                SentryInitializer.initialize(this);
            }

            // Set up the status bar with custom insets and background color.
            setupStatusBarForActivity();

            // Set up the toolbar (action bar) and hide its default title.
            setupToolbarForActivity();

            // Apply the device's language settings and capture the current locale.
            String appLocale = setupLanguageForActivity();
            sentryManager.captureMessage("Using locale: " + appLocale);

            // Determine and apply the dark mode preference.
            setupDarkModeForActivity(sentryManager, SharedPreferencesManager.getString("dark_mode", "match"));

            // Set up any additional UI indicators.
            setupVisualIndicatorForActivity(sentryManager, this);

            // Set up swipe-to-refresh functionality.
            setupSwipeToRefreshForActivity();

            // Request POST_NOTIFICATIONS permission if not granted already.
            if (ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(this, new String[]{POST_NOTIFICATIONS}, 1);
            }

            // Initialize the WebView and load the main URL.
            setupWebViewForActivity(getString(R.string.main_url));
        } catch (Exception e) {
            // Capture any exceptions in Sentry for analysis.
            sentryManager.captureException(e);
        }
    }

    /**
     * Called when the activity is destroyed. Cleans up the WebView to free resources.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        webView.destroy();
    }

    /**
     * Called when the activity is paused. Pauses the WebView to avoid unnecessary resource usage.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
        }
    }

    /**
     * Called when the activity is resumed. Resumes the WebView if previously initialized, and handles
     * biometric authentication with a timeout to determine if the user needs to re-authenticate.
     * Also includes a fallback reload if the WebView fails to load.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
        } else if (!isWebViewInitialized) {
            setupWebViewForActivity(getString(R.string.main_url));
        }

        SharedPreferencesManager.init(this);

        if (SharedPreferencesManager.getBoolean("app_lock_enable", true)) {
            // App lock enabled, so proceed with biometric authentication
            if (shouldAuthenticate()) {
                hideToolbarButtons();
                invalidateOptionsMenu();

                final BiometricLock biometricLock = new BiometricLock(this);
                if (biometricLock.canAuthenticate()) {
                    showBlurryOverlay();
                    biometricLock.showPrompt(
                            "Unlock",
                            "Authenticate to access and change your settings.",
                            "",
                            new BiometricLock.BiometricLockCallback() {
                                @Override
                                public void onAuthenticationSucceeded() {
                                    removeBlurryOverlay();
                                    if (webView != null) {
                                        webView.animate().alpha(1f).setDuration(300).start();
                                    }
                                    lastAuthenticatedTime = System.currentTimeMillis();
                                    showToolbarButtons();
                                    invalidateOptionsMenu();
                                }

                                @Override
                                public void onAuthenticationError(String error) {
                                    removeBlurryOverlay();
                                    Toast.makeText(MainActivity.this, "Authentication error!", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onAuthenticationFailed() {
                                    Toast.makeText(MainActivity.this, "Authentication failed. Please try again.", Toast.LENGTH_SHORT).show();
                                }
                            }
                    );
                } else {
                    lastAuthenticatedTime = System.currentTimeMillis();
                    showToolbarButtons();
                    invalidateOptionsMenu();
                }
            } else {
                showToolbarButtons();
                invalidateOptionsMenu();
            }

            if (webView != null && webView.getProgress() == 0) {
                webView.postDelayed(() -> {
                    if (webView.getProgress() == 0) {
                        Log.d("WebView", "Fallback reload initiated");
                        webView.reload();
                    }
                }, 300);
            }
        } else {
            // App lock is disabled, so bypass biometric authentication.
            // Immediately reveal the UI and remove any overlays.
            removeBlurryOverlay();
            if (webView != null) {
                webView.setAlpha(1f);
            }
            showToolbarButtons();
            invalidateOptionsMenu();
        }
    }

    /**
     * Determines whether biometric authentication is required based on the timeout.
     *
     * @return true if the elapsed time since last authentication exceeds the allowed timeout.
     */
    private boolean shouldAuthenticate() {
        return System.currentTimeMillis() - lastAuthenticatedTime > AUTH_TIMEOUT_MS;
    }

    /**
     * Applies a blurry overlay over the SwipeRefreshLayout containing the WebView.
     * If the container's dimensions are not available yet, delays and retries.
     */
    private void showBlurryOverlay() {
        final ViewGroup container = findViewById(R.id.swipeRefreshLayout);
        // Post a runnable to execute after the layout pass.
        container.post(() -> {
            // If the container has no valid dimensions, try again after a short delay.
            if (container.getWidth() == 0 || container.getHeight() == 0) {
                container.postDelayed(this::showBlurryOverlay, 100);
                return;
            }
            try {
                // Get the tint color from resources.
                int tintColor = ContextCompat.getColor(this, R.color.blur_tint);
                // Apply the blur with the specified radius, sampling, and tint color.
                Blurry.with(this)
                        .radius(10)
                        .sampling(2)
                        .color(tintColor)
                        .onto(container);
            } catch (NullPointerException e) {
                // Log any exceptions that occur while applying the blur.
                Log.d("Blurry", "There was an error while applying a blur effect: " + e);
            }
        });
    }

    /**
     * Removes the blurry overlay from the SwipeRefreshLayout.
     */
    private void removeBlurryOverlay() {
        ViewGroup container = findViewById(R.id.swipeRefreshLayout);
        Blurry.delete(container);
    }

    /**
     * Sets up the status bar by applying a custom background color.
     * This method may be updated to handle newer Android versions as needed.
     */
    private void setupStatusBarForActivity() {
        // Check if the Android version is higher than a placeholder version.
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Window window = getWindow();
            // Set a listener to apply window insets and modify the background color.
            window.getDecorView().setOnApplyWindowInsetsListener((view, insets) -> {
                view.setBackgroundColor(getResources().getColor(R.color.main));
                return insets;
            });
        }
    }

    /**
     * Sets up the toolbar by hiding the default title and adding a click listener to an icon
     * that navigates to the StatusActivity.
     */
    private void setupToolbarForActivity() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        // Set the toolbar as the ActionBar.
        setSupportActionBar(toolbar);

        // Hide the default title in the ActionBar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }

        // Set up the connection status icon to navigate to the StatusActivity when clicked.
        ImageView imageView = findViewById(R.id.connectionStatus);
        imageView.setOnClickListener(v -> startActivity(new Intent(this, StatusActivity.class)));
    }

    /**
     * Reads the current language from the system configuration, applies it, and returns the language code.
     *
     * @return The language code (e.g., "en" or "fr").
     */
    private String setupLanguageForActivity() {
        // Retrieve the configuration and the first locale.
        Configuration config = getResources().getConfiguration();
        Locale appLocale = config.getLocales().get(0);
        // Set the default locale.
        Locale.setDefault(appLocale);

        // Create a new configuration and set its locale.
        Configuration newConfig = new Configuration(config);
        newConfig.setLocale(appLocale);

        // Apply the new configuration using a ContextThemeWrapper.
        new ContextThemeWrapper(getBaseContext(), R.style.AppTheme).applyOverrideConfiguration(newConfig);

        return appLocale.getLanguage();
    }

    /**
     * Reads the user's dark mode preference from SharedPreferences and applies the appropriate mode.
     * Also logs any changes or actions to Sentry for debugging.
     *
     * @param sentryManager The SentryManager instance for logging.
     * @param darkMode      The dark mode preference (e.g., "match", "on", "off").
     */
    private void setupDarkModeForActivity(SentryManager sentryManager, String darkMode) {
        switch (darkMode) {
            case "match":
                // Follow the system's light/dark setting.
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                updateDarkModeState();
                sentryManager.captureMessage("Dark mode set to follow system.");
                break;
            case "on":
                // Always use dark mode.
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                darkModeEnabled = true;
                sentryManager.captureMessage("Dark mode set to on.");
                break;
            case "disabled":
                // Disable dark mode if the SDK version doesn't support it.
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                darkModeEnabled = false;
                sentryManager.captureMessage("Dark mode is disabled due to SDK version.");
                break;
            case "off":
            default:
                // Default to light mode.
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                darkModeEnabled = false;
                sentryManager.captureMessage("Dark mode set to off.");
                break;
        }
    }

    /**
     * Updates the darkModeEnabled flag based on the current system configuration.
     * This method helps determine whether dark mode is active.
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
     * Sets up a visual indicator (e.g., connectivity or loading status) that operates alongside
     * this activity's lifecycle.
     *
     * @param sentryManager  The SentryManager instance for logging any exceptions.
     * @param lifecycleOwner The LifecycleOwner (usually the activity itself).
     */
    private void setupVisualIndicatorForActivity(SentryManager sentryManager, LifecycleOwner lifecycleOwner) {
        try {
            // Initialize and start the visual indicator.
            new VisualIndicator(this).initialize(this, lifecycleOwner, this);
        } catch (Exception e) {
            // Log any exceptions that occur during initialization.
            sentryManager.captureException(e);
        }
    }

    /**
     * Initializes and configures the WebView, including JavaScript, DOM storage, caching,
     * and download handling. Optionally restores a previously saved WebView state.
     *
     * @param url The URL to load if there is no saved WebView state.
     */
    @SuppressLint("SetJavaScriptEnabled")
    public void setupWebViewForActivity(String url) {
        // Find the WebView in the layout.
        webView = findViewById(R.id.webView);

        // Hide the WebView until authentication is complete.
        webView.setAlpha(0f);

        // Restore the saved WebView state if available; otherwise, load the provided URL.
        if (webViewState != null) {
            webView.restoreState(webViewState);
        } else {
            // Load the URL only once if no saved state exists.
            webView.loadUrl(url);
        }

        // Configure various settings for the WebView.
        WebSettings webViewSettings = webView.getSettings();
        webViewSettings.setJavaScriptEnabled(true);
        webViewSettings.setDomStorageEnabled(true);
        webViewSettings.setDatabaseEnabled(true);
        webViewSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webViewSettings.setAllowFileAccess(false);
        webViewSettings.setAllowContentAccess(false);

        // Set a custom WebViewClient to handle events such as page loading finished.
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView webView, String url) {
                // Enable cookies and flush them for persistence.
                CookieManager.getInstance().setAcceptCookie(true);
                CookieManager.getInstance().acceptCookie();
                CookieManager.getInstance().flush();

                // Inject JavaScript to enable swipe-to-refresh functionality.
                String js =
                        "setInterval(function() {" +
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
                webView.evaluateJavascript(js, null);
            }
        });
        webView.setWebChromeClient(new WebChromeClient());

        // Enable algorithmic darkening for the WebView if dark mode is active and supported.
        if (Boolean.TRUE.equals(darkModeEnabled)) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.getSettings(), true);
            }
        }

        // Set up the Download Manager for file downloads in the WebView.
        setupDownloadManagerForActivity();

        // Do not call loadUrl(url) again here since the URL is already loaded above.
        // Mark the WebView as fully initialized.
        isWebViewInitialized = true;
    }

    /**
     * Initializes and configures the SwipeRefreshLayout and WebView.
     * Enables JavaScript, attaches a JavaScript interface for native-to-JS communication,
     * and sets up a refresh listener to reload the WebView when swiped.
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void setupSwipeToRefreshForActivity() {
        // Find the SwipeRefreshLayout and WebView by their IDs.
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        webView = findViewById(R.id.webView);

        // Enable JavaScript on the WebView and add the custom JavaScript interface.
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new WebAppInterface(this, swipeRefreshLayout), "AndroidInterface");

        // Set up the swipe-to-refresh listener to reload the WebView and stop the refresh animation.
        swipeRefreshLayout.setOnRefreshListener(() -> {
            webView.reload();
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    /**
     * Sets up a listener to handle file downloads from within the WebView using DownloadManager.
     * Downloads are saved to the app's external files directory (Downloads).
     */
    private void setupDownloadManagerForActivity() {
        // Set the download listener on the WebView.
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            // Create a DownloadManager request for the file URL.
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url.trim()));
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            // Set the download destination to the external files directory.
            request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, "NextDNS-Configuration.mobileconfig");

            // Get the system's DownloadManager and enqueue the request.
            DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (downloadManager != null) {
                downloadManager.enqueue(request);
            }
            // Show a toast to indicate that the download has started.
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
     * Hides all child views (such as buttons) within the toolbar.
     */
    private void hideToolbarButtons() {
        // Get the toolbar by its ID.
        Toolbar toolbar = findViewById(R.id.toolbar);
        // Loop through all the child views in the toolbar.
        for (int i = 0; i < toolbar.getChildCount(); i++) {
            // Hide each child view.
            toolbar.getChildAt(i).setVisibility(android.view.View.GONE);
        }
    }

    /**
     * Shows all child views (such as buttons) within the toolbar.
     */
    private void showToolbarButtons() {
        // Get the toolbar by its ID.
        Toolbar toolbar = findViewById(R.id.toolbar);
        // Loop through all the child views in the toolbar.
        for (int i = 0; i < toolbar.getChildCount(); i++) {
            // Make each child view visible.
            toolbar.getChildAt(i).setVisibility(android.view.View.VISIBLE);
        }
    }

    /**
     * Prepares the options menu by updating the visibility of menu items based on the authentication status.
     *
     * @param menu The options menu to be prepared.
     * @return true after the menu has been prepared.
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Determine if the user is authenticated by checking if the elapsed time since the last authentication
        // is within the allowed authentication timeout.
        boolean isAuthenticated = System.currentTimeMillis() - lastAuthenticatedTime <= AUTH_TIMEOUT_MS;
        // Loop through each menu item and set its visibility accordingly.
        for (int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setVisible(isAuthenticated);
        }
        return super.onPrepareOptionsMenu(menu);
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
     * Handles option item selections, such as navigating back, refreshing the page,
     * or opening various settings.
     *
     * @param item The selected menu item.
     * @return true if the event was handled successfully.
     */
    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.back:
                // If the WebView is null, reinitialize it; otherwise, navigate back in history.
                if (webView == null) {
                    setupWebViewForActivity(getString(R.string.main_url));
                } else {
                    webView.goBack();
                }
                break;
            case R.id.refreshNextDNS:
                // If the WebView is null, reinitialize it; otherwise, reload the current page.
                if (webView == null) {
                    setupWebViewForActivity(getString(R.string.main_url));
                } else {
                    webView.reload();
                }
                break;
            case R.id.pingNextDNS:
                // Start the PingActivity.
                startIntent(PingActivity.class);
                break;
            case R.id.returnHome:
                // If the WebView is null, reinitialize it; otherwise, load the main URL.
                if (webView == null) {
                    setupWebViewForActivity(getString(R.string.main_url));
                } else {
                    webView.loadUrl(getString(R.string.main_url));
                }
                break;
            case R.id.privateDNS:
                // Launch the system settings for Private DNS.
                startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                break;
            case R.id.settings:
                // Launch the app's custom SettingsActivity.
                startIntent(SettingsActivity.class);
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
