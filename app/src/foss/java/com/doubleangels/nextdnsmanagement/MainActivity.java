package com.doubleangels.nextdnsmanagement;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
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
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.doubleangels.nextdnsmanagement.biometriclock.BiometricLock;
import com.doubleangels.nextdnsmanagement.protocol.VisualIndicator;
import com.doubleangels.nextdnsmanagement.sentry.SentryInitializer;
import com.doubleangels.nextdnsmanagement.sentry.SentryManager;
import com.doubleangels.nextdnsmanagement.sharedpreferences.SharedPreferencesManager;
import com.doubleangels.nextdnsmanagement.webview.WebAppInterface;

import java.util.Locale;

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

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        try {
            if (webView != null) {
                Bundle webViewBundle = new Bundle();
                webView.saveState(webViewBundle);
                outState.putBundle("webViewState", webViewBundle);
            }
            outState.putBoolean("darkModeEnabled", darkModeEnabled);
        } catch (Exception e) {
            // Log and capture any exception during state save.
            SentryManager.captureStaticException(e);
        }
    }

    @SuppressLint({"WrongThread", "SetJavaScriptEnabled"})
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Restore saved state if available.
        if (savedInstanceState != null) {
            webViewState = savedInstanceState.getBundle("webViewState");
            darkModeEnabled = savedInstanceState.getBoolean("darkModeEnabled");
        }

        setContentView(R.layout.activity_main);

        // Initialize Sentry manager.
        final SentryManager sentryManager = new SentryManager(this);
        SharedPreferencesManager.init(this);

        try {
            if (sentryManager.isEnabled()) {
                sentryManager.captureMessage("Sentry is enabled for NextDNS Manager.");
                SentryInitializer.initialize(this);
            }
        } catch (Exception e) {
            sentryManager.captureException(e);
        }

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
            setupWebViewForActivity(getString(R.string.main_url));
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webView != null) {
            try {
                webView.destroy();
            } catch (Exception e) {
                SentryManager.captureStaticException(e);
            } finally {
                webView = null;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
        }
    }

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
            if (shouldAuthenticate()) {
                final BiometricLock biometricLock = new BiometricLock(this);
                if (biometricLock.canAuthenticate()) {
                    biometricLock.showPrompt(
                            getString(R.string.unlock_title),
                            getString(R.string.unlock_description),
                            "",
                            new BiometricLock.BiometricLockCallback() {
                                @Override
                                public void onAuthenticationSucceeded() {
                                    if (webView != null) {
                                        webView.animate().alpha(1f).setDuration(300).start();
                                    }
                                    lastAuthenticatedTime = System.currentTimeMillis();
                                    invalidateOptionsMenu();
                                }

                                @Override
                                public void onAuthenticationError(String error) {
                                    finish();
                                }

                                @Override
                                public void onAuthenticationFailed() {
                                    finish();
                                }
                            }
                    );
                }
            }
        }
    }

    private boolean shouldAuthenticate() {
        return System.currentTimeMillis() - lastAuthenticatedTime > AUTH_TIMEOUT_MS;
    }

    private void setupStatusBarForActivity() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Window window = getWindow();
            window.getDecorView().setOnApplyWindowInsetsListener((view, insets) -> {
                view.setBackgroundColor(getResources().getColor(R.color.main));
                return insets;
            });
        }
    }

    private void setupToolbarForActivity() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }
        ImageView imageView = findViewById(R.id.connectionStatus);
        imageView.setOnClickListener(v -> startActivity(new Intent(this, StatusActivity.class)));
    }

    private String setupLanguageForActivity() {
        Configuration config = getResources().getConfiguration();
        Locale appLocale = config.getLocales().get(0);
        Locale.setDefault(appLocale);
        Configuration newConfig = new Configuration(config);
        newConfig.setLocale(appLocale);
        new ContextThemeWrapper(getBaseContext(), R.style.AppTheme).applyOverrideConfiguration(newConfig);
        return appLocale.getLanguage();
    }

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

    private void updateDarkModeState() {
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        darkModeEnabled = (nightModeFlags == Configuration.UI_MODE_NIGHT_YES);
    }

    private void setupVisualIndicatorForActivity(SentryManager sentryManager, LifecycleOwner lifecycleOwner) {
        try {
            new VisualIndicator(this).initialize(this, lifecycleOwner, this);
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void setupWebViewForActivity(String url) {
        webView = findViewById(R.id.webView);
        try {
            if (webViewState != null) {
                webView.restoreState(webViewState);
            } else {
                webView.loadUrl(url);
            }
        } catch (Exception e) {
            SentryManager.captureStaticException(e);
        }

        WebSettings webViewSettings = webView.getSettings();
        webViewSettings.setJavaScriptEnabled(true);
        webViewSettings.setDomStorageEnabled(true);
        webViewSettings.setDatabaseEnabled(true);
        webViewSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webViewSettings.setAllowFileAccess(false);
        webViewSettings.setAllowContentAccess(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                try {
                    CookieManager.getInstance().setAcceptCookie(true);
                    CookieManager.getInstance().acceptCookie();
                    CookieManager.getInstance().flush();
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
                    webView.evaluateJavascript(js, null);
                } catch (Exception e) {
                    SentryManager.captureStaticException(e);
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                SentryManager.captureStaticException(new Exception("WebView error: " + error.getDescription()));
                super.onReceivedError(view, request, error);
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                SentryManager.captureStaticException(new Exception("HTTP error: " + errorResponse.getStatusCode()));
                super.onReceivedHttpError(view, request, errorResponse);
            }
        });
        webView.setWebChromeClient(new WebChromeClient());

        if (Boolean.TRUE.equals(darkModeEnabled)) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.getSettings(), true);
            }
        }

        setupDownloadManagerForActivity();
        isWebViewInitialized = true;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupSwipeToRefreshForActivity() {
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new WebAppInterface(this, swipeRefreshLayout), "AndroidInterface");
        swipeRefreshLayout.setOnRefreshListener(() -> {
            webView.reload();
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void setupDownloadManagerForActivity() {
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            try {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url.trim()));
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, "NextDNS-Configuration.mobileconfig");
                DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                if (downloadManager != null) {
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

    private void startIntent(Class<?> targetClass) {
        Intent intent = new Intent(this, targetClass);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.back:
                if (webView == null) {
                    setupWebViewForActivity(getString(R.string.main_url));
                } else {
                    webView.goBack();
                }
                break;
            case R.id.refreshNextDNS:
                if (webView == null) {
                    setupWebViewForActivity(getString(R.string.main_url));
                } else {
                    webView.reload();
                }
                break;
            case R.id.pingNextDNS:
                startIntent(PingActivity.class);
                break;
            case R.id.returnHome:
                if (webView == null) {
                    setupWebViewForActivity(getString(R.string.main_url));
                } else {
                    webView.loadUrl(getString(R.string.main_url));
                }
                break;
            case R.id.privateDNS:
                startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                break;
            case R.id.settings:
                startIntent(SettingsActivity.class);
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
