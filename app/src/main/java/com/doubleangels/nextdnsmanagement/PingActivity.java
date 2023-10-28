// Import statements for required libraries and classes.
package com.doubleangels.nextdnsmanagement;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import java.util.Objects;

import io.sentry.ITransaction;
import io.sentry.Sentry;

// Definition of the PingActivity class, which extends AppCompatActivity.
public class PingActivity extends AppCompatActivity {
    private final DarkModeHandler darkModeHandler = new DarkModeHandler();
    private WebView webView;

    // Method called when the activity is created.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ITransaction pingCreateTransaction = Sentry.startTransaction("ping_onCreate()", "PingActivity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ping);

        try {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

            initializeViews(sharedPreferences);
            setVisualIndicator();
            setClickListeners();
            provisionWebView(getString(R.string.ping_url));
        } catch (Exception e) {
            Sentry.captureException(e);
        } finally {
            pingCreateTransaction.finish();
        }
    }

    // Method to initialize views and preferences.
    private void initializeViews(SharedPreferences sharedPreferences) {
        boolean darkNavigation = sharedPreferences.getBoolean(SettingsActivity.DARK_NAVIGATION, false);
        setWindowAndToolbar(darkNavigation);
    }

    // Method to set window and toolbar styles based on dark navigation.
    private void setWindowAndToolbar(boolean isDark) {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        int statusBarColor;
        int toolbarColor;

        if (isDark) {
            statusBarColor = ContextCompat.getColor(this, R.color.darkgray);
            toolbarColor = ContextCompat.getColor(this, R.color.darkgray);
        } else {
            statusBarColor = ContextCompat.getColor(this, R.color.blue);
            toolbarColor = ContextCompat.getColor(this, R.color.blue);
        }

        window.setStatusBarColor(statusBarColor);
        window.setNavigationBarColor(statusBarColor);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        toolbar.setBackgroundColor(toolbarColor);
        Sentry.setTag("dark_navigation", String.valueOf(isDark));
    }

    // Method to set up a visual indicator.
    private void setVisualIndicator() {
        VisualIndicator visualIndicator = new VisualIndicator();
        visualIndicator.initiateVisualIndicator(this, getApplicationContext());
    }

    // Method to set click listeners, e.g., for status icon.
    private void setClickListeners() {
        ImageView statusIcon = findViewById(R.id.connectionStatus);
        statusIcon.setOnClickListener(v -> {
            Intent helpIntent = new Intent(v.getContext(), HelpActivity.class);
            startActivity(helpIntent);
        });
    }

    // Method to create the options menu.
    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_back_only, menu);
        return true;
    }

    // Method called when the activity is resumed.
    @Override
    protected void onResume() {
        super.onResume();
        darkModeHandler.handleDarkMode(this);
    }

    // Method to provision the WebView with the provided URL.
    @SuppressLint("SetJavaScriptEnabled")
    @SuppressWarnings("unused")
    public void provisionWebView(String url) {
        ITransaction provisionWebViewTransaction = Sentry.startTransaction("ping_provisionWebView()", "PingActivity");
        try {
            if (webView == null) {
                webView = findViewById(R.id.mWebview);
                setupWebViewSettings();
            }
            webView.loadUrl(url);
        } catch (Exception e) {
            Sentry.captureException(e);
        } finally {
            provisionWebViewTransaction.finish();
        }
    }

    // Method to configure WebView settings.
    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebViewSettings() {
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);
    }

    // Method to handle menu item selection, e.g., back button.
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.back) {
            Intent mainIntent = new Intent(this, MainActivity.class);
            startActivity(mainIntent);
        }
        return super.onContextItemSelected(item);
    }
}