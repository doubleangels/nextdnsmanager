package com.doubleangels.nextdnsmanagement;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import com.doubleangels.nextdnsmanagement.geckoruntime.GeckoRuntimeSingleton;
import com.doubleangels.nextdnsmanagement.protocoltest.VisualIndicator;
import com.doubleangels.nextdnsmanagement.sentrymanager.SentryManager;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;

import java.util.Locale;
import java.util.Objects;

import io.sentry.android.core.SentryAndroid;

public class PingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ping);
        SentryManager sentryManager = new SentryManager(this);
        SharedPreferences sharedPreferences = getSharedPreferences("preferences", MODE_PRIVATE);
        try {
            if (sentryManager.isSentryEnabled()) {
                SentryAndroid.init(this, options -> {
                    options.setDsn("https://8b52cc2148b94716a69c9a4f0c0b4513@o244019.ingest.us.sentry.io/6270764");
                    options.setEnableTracing(true);
                    options.setAttachScreenshot(true);
                    options.setAttachViewHierarchy(true);
                    options.setTracesSampleRate(1.0);
                    options.setEnableAppStartProfiling(true);
                    options.setAnrEnabled(true);
                });
            }
            setupToolbar();
            setupLanguage();
            setupDarkMode(sharedPreferences);
            setupVisualIndicator(sentryManager);
            GeckoView view = findViewById(R.id.geckoView);
            GeckoSession session = new GeckoSession();
            GeckoRuntime runtime = GeckoRuntimeSingleton.getInstance();
            session.setContentDelegate(new GeckoSession.ContentDelegate() {});
            runtime.getSettings().setAllowInsecureConnections(GeckoRuntimeSettings.HTTPS_ONLY);
            runtime.getSettings().setAutomaticFontSizeAdjustment(true);
            session.open(runtime);
            view.setSession(session);
            session.loadUri(getString(R.string.ping_url));
        } catch (Exception e) {
            sentryManager.captureExceptionIfEnabled(e);
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
    }

    private void setupLanguage() {
        String appLocaleString = getResources().getConfiguration().getLocales().get(0).toString();
        String appLocaleStringResult = appLocaleString.split("_")[0];
        Locale appLocale = Locale.forLanguageTag(appLocaleStringResult);
        Locale.setDefault(appLocale);
        Configuration appConfig = new Configuration();
        appConfig.locale = appLocale;
        getResources().updateConfiguration(appConfig, getResources().getDisplayMetrics());
    }

    private void setupDarkMode(SharedPreferences sharedPreferences) {
        String darkModeOverride = sharedPreferences.getString("darkmode_override", "match");
        if (darkModeOverride.contains("match")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        } else if (darkModeOverride.contains("on")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void setupVisualIndicator(SentryManager sentryManager) {
        try {
            VisualIndicator visualIndicator = new VisualIndicator(this);
            visualIndicator.initiateVisualIndicator(this, getApplicationContext());
        } catch (Exception e) {
            sentryManager.captureExceptionIfEnabled(e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_back_only, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.back) {
            Intent mainIntent = new Intent(this, MainActivity.class);
            startActivity(mainIntent);
        }
        return super.onContextItemSelected(item);
    }
}
