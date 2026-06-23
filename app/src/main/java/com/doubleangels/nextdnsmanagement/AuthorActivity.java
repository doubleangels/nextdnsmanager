package com.doubleangels.nextdnsmanagement;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.doubleangels.nextdnsmanagement.sentry.SentryManager;
import com.doubleangels.nextdnsmanagement.utils.ExternalLinkHandler;
import com.doubleangels.nextdnsmanagement.utils.StatusBarHelper;

import java.util.Locale;

/**
 * Activity displaying information about the author, including links to GitHub
 * and email.
 */
public class AuthorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_author);

        try {
            setupStatusBarForActivity();
        } catch (Exception e) {
            SentryManager.captureStaticException(e);
        }

        try {
            setupPersonalLinks();
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
        StatusBarHelper.apply(this);
    }

    private void setupPersonalLinks() {
        ImageView githubButton = findViewById(R.id.githubImageView);
        ImageView emailButton = findViewById(R.id.emailImageView);
        if (githubButton == null || emailButton == null) {
            return;
        }

        githubButton.setOnClickListener(view -> ExternalLinkHandler.openExternalLink(this,
                Uri.parse(getString(R.string.github_profile_url))));

        emailButton.setOnClickListener(view -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:jzgm2llm@addy.io"));
            try {
                startActivity(Intent.createChooser(emailIntent, getString(R.string.send_email)));
            } catch (android.content.ActivityNotFoundException e) {
                Toast.makeText(this, R.string.no_email_client_found, Toast.LENGTH_LONG).show();
            } catch (SecurityException e) {
                Toast.makeText(this, R.string.email_open_security_error,
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
