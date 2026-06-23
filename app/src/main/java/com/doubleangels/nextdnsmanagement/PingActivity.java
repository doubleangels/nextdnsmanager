package com.doubleangels.nextdnsmanagement;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.WindowInsetsController;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import com.doubleangels.nextdnsmanagement.sentry.SentryInitializer;
import com.doubleangels.nextdnsmanagement.sentry.SentryManager;
import com.doubleangels.nextdnsmanagement.utils.ExternalLinkHandler;

import java.util.Locale;

/**
 * Activity that handles ping functionality by loading two web pages in separate
 * WebViews.
 */
public class PingActivity extends AppCompatActivity {

    public SentryManager sentryManager;
    public WebView webView;
    public WebView webView2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ping);

        getWindow().setFlags(
                android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

        sentryManager = new SentryManager(this);
        try {
            if (sentryManager.isEnabled()) {
                SentryInitializer.initialize(this);
            }
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
        try {
            setupStatusBarForActivity();
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
        try {
            setupWebViewsForActivity(getString(R.string.ping_url), getString(R.string.test_url));
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
        }
        if (webView2 != null) {
            webView2.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
        }
        if (webView2 != null) {
            webView2.onResume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (webView != null) {
                if (webView.getParent() != null) {
                    ((ViewGroup) webView.getParent()).removeView(webView);
                }
                webView.setWebViewClient(new WebViewClient());
                webView.clearCache(true);
                webView.clearHistory();
                webView.clearFormData();
                webView.destroy();
                webView = null;
            }
            if (webView2 != null) {
                if (webView2.getParent() != null) {
                    ((ViewGroup) webView2.getParent()).removeView(webView2);
                }
                webView2.setWebViewClient(new WebViewClient());
                webView2.clearCache(true);
                webView2.clearHistory();
                webView2.clearFormData();
                webView2.destroy();
                webView2 = null;
            }
            sentryManager = null;
        } catch (Exception e) {
            SentryManager.captureStaticException(e);
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        Configuration config = newBase.getResources().getConfiguration();
        Locale newLocale = (!config.getLocales().isEmpty()) ? config.getLocales().get(0) : Locale.getDefault();
        Configuration overrideConfig = new Configuration(config);
        overrideConfig.setLocale(newLocale);
        Context localizedContext = newBase.createConfigurationContext(overrideConfig);
        super.attachBaseContext(localizedContext);
    }

    private void setupStatusBarForActivity() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            WindowInsetsController insetsController = getWindow().getInsetsController();
            if (insetsController != null) {
                boolean isLightTheme = (getResources().getConfiguration().uiMode &
                        Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO;
                insetsController.setSystemBarsAppearance(
                        isLightTheme ? WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS : 0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void setupWebViewsForActivity(String url1, String url2) {
        try {
            webView = findViewById(R.id.webView);
            webView2 = findViewById(R.id.webView2);
            if (webView == null || webView2 == null) {
                throw new IllegalStateException("Ping WebViews not found in layout");
            }
            setupWebView(webView, url1);
            setupWebView(webView2, url2);
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(WebView targetWebView, String url) {
        WebSettings settings = targetWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        targetWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (uri == null) {
                    return false;
                }
                if (ExternalLinkHandler.isNextDnsHost(uri)) {
                    return false;
                }
                return ExternalLinkHandler.openExternalLink(view.getContext(), view, uri);
            }
        });
        targetWebView.loadUrl(url);
    }
}
