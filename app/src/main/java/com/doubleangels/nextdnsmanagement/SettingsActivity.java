package com.doubleangels.nextdnsmanagement;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import com.doubleangels.nextdnsmanagement.biometriclock.BiometricLock;
import com.doubleangels.nextdnsmanagement.sentry.SentryInitializer;
import com.doubleangels.nextdnsmanagement.sentry.SentryManager;
import com.doubleangels.nextdnsmanagement.sharedpreferences.SharedPreferencesManager;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    public SentryManager sentryManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize SentryManager
        sentryManager = new SentryManager(this);

        // Initialize SharedPreferencesManager
        try {
            SharedPreferencesManager.init(this);
        } catch (Exception e) {
            if (sentryManager != null) {
                sentryManager.captureException(e);
            }
        }

        // Log current preference values for debugging.
        try {
            assert sentryManager != null;
            sentryManager.captureMessage("SharedPreferences 'dark_mode' value: "
                    + SharedPreferencesManager.getString("dark_mode", "match"));
            sentryManager.captureMessage("SharedPreferences 'sentry_enable' value: "
                    + SharedPreferencesManager.getBoolean("sentry_enable", false));
        } catch (Exception e) {
            sentryManager.captureException(e);
        }

        // Initialize Sentry if enabled.
        try {
            if (sentryManager.isEnabled()) {
                SentryInitializer.initialize(this);
            }
        } catch (Exception e) {
            sentryManager.captureException(e);
        }

        // Apply language configuration.
        try {
            String appLocale = setupLanguageForActivity();
            sentryManager.captureMessage("Using locale: " + appLocale);
        } catch (Exception e) {
            sentryManager.captureException(e);
        }

        // Apply dark mode preference.
        try {
            setupDarkModeForActivity(SharedPreferencesManager.getString("dark_mode", "match"));
        } catch (Exception e) {
            sentryManager.captureException(e);
        }

        // Load the settings fragment.
        try {
            initializeViews();
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
    }

    /**
     * Applies the current locale to the base context and returns the language code.
     */
    private String setupLanguageForActivity() {
        Configuration config = getResources().getConfiguration();
        Locale appLocale = config.getLocales().get(0);
        Locale.setDefault(appLocale);

        Configuration newConfig = new Configuration(config);
        newConfig.setLocale(appLocale);

        new ContextThemeWrapper(getBaseContext(), R.style.AppTheme)
                .applyOverrideConfiguration(newConfig);

        return appLocale.getLanguage();
    }

    /**
     * Applies the dark mode setting (for pre-TIRAMISU devices).
     */
    private void setupDarkModeForActivity(String darkMode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            sentryManager.captureMessage("Dark mode setting: " + darkMode);
            if (darkMode.contains("match")) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            } else if (darkMode.contains("on")) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        }
    }

    /**
     * Loads the settings fragment into the activity.
     */
    private void initializeViews() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commitNow();
    }

    /**
     * The SettingsFragment displays and handles user preferences.
     */
    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            try {
                setPreferencesFromResource(R.xml.root_preferences, rootKey);
            } catch (Exception e) {
                new SentryManager(requireContext()).captureException(e);
            }

            try {
                SharedPreferencesManager.init(requireContext());
            } catch (Exception e) {
                new SentryManager(requireContext()).captureException(e);
            }

            try {
                setInitialSentryVisibility(SharedPreferencesManager.getBoolean("sentry_enable", false));
            } catch (Exception e) {
                new SentryManager(requireContext()).captureException(e);
            }

            try {
                // Retrieve references to specific preferences.
                SwitchPreference sentryEnablePreference = findPreference("sentry_enable");
                SwitchPreference appLockPreference = findPreference("app_lock_enable");
                ListPreference darkModePreference = findPreference("dark_mode");

                // Ensure app lock is only available if the device is configured for biometric authentication.
                final BiometricLock biometricLock = new BiometricLock((AppCompatActivity) requireContext());
                if (!biometricLock.canAuthenticate()) {
                    setPreferenceVisibility("applock", false);
                }

                if (sentryEnablePreference != null) {
                    setupSentryChangeListener(sentryEnablePreference);
                }
                if (appLockPreference != null) {
                    setupAppLockChangeListener(appLockPreference);
                }
                if (darkModePreference != null) {
                    setupDarkModeChangeListener(darkModePreference);
                }

                // Set up buttons for copying text or opening links.
                setupButton("whitelist_domain_1_button", R.string.whitelist_domain_1);
                setupButton("whitelist_domain_2_button", R.string.whitelist_domain_2);
                setupButton("sentry_info_button", R.string.sentry_info_url);
                setupButtonForIntent("author_button");
                setupButton("feedback_button", R.string.tally_feedback_url);
                setupButton("github_button", R.string.github_url);
                setupButton("github_issue_button", R.string.github_issues_url);
                setupButton("donation_button", R.string.donation_url);
                setupButton("tally_issues_button", R.string.tally_issues_url);
                setupButton("translate_button", R.string.tally_incorrect_translation_url);
                setupButton("privacy_policy_button", R.string.privacy_policy_url);
                setupButton("nextdns_privacy_policy_button", R.string.nextdns_privacy_policy_url);
                setupButton("nextdns_user_agreement_button", R.string.nextdns_user_agreement_url);
                setupButtonForIntent("permission_button");
                setupButton("version_button", R.string.versions_url);

                // Display the current app version name on the version preference.
                String versionName = BuildConfig.VERSION_NAME;
                Preference versionPreference = findPreference("version_button");
                if (versionPreference != null) {
                    versionPreference.setSummary(versionName);
                }
            } catch (Exception e) {
                new SentryManager(requireContext()).captureException(e);
            }
        }

        /**
         * Shows or hides the whitelist-related preferences based on Sentry enablement.
         */
        private void setInitialSentryVisibility(Boolean visibility) {
            setPreferenceVisibility("whitelist_domains", visibility);
            setPreferenceVisibility("whitelist_domain_1_button", visibility);
            setPreferenceVisibility("whitelist_domain_2_button", visibility);
        }

        /**
         * Sets the visibility of a preference by its key.
         */
        private void setPreferenceVisibility(String key, Boolean visibility) {
            Preference preference = findPreference(key);
            if (preference != null) {
                preference.setVisible(visibility);
            }
        }

        /**
         * Configures a button preference to copy text to the clipboard or open a URL.
         */
        private void setupButton(String buttonKey, int textResource) {
            Preference button = findPreference(buttonKey);
            if (button != null) {
                button.setOnPreferenceClickListener(preference -> {
                    try {
                        // For whitelist buttons, copy text to clipboard.
                        if ("whitelist_domain_1_button".equals(buttonKey) || "whitelist_domain_2_button".equals(buttonKey)) {
                            ClipboardManager clipboardManager = (ClipboardManager)
                                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                            CharSequence copiedText = getString(textResource);
                            ClipData copiedData = ClipData.newPlainText("text", copiedText);
                            clipboardManager.setPrimaryClip(copiedData);
                            Toast.makeText(getContext(), "Text copied!", Toast.LENGTH_SHORT).show();
                        } else {
                            // Otherwise, open the URL.
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(textResource)));
                            startActivity(intent);
                        }
                    } catch (Exception e) {
                        new SentryManager(requireContext()).captureException(e);
                    }
                    return true;
                });
            }
        }

        /**
         * Configures a button preference that navigates to another Activity within the app.
         */
        private void setupButtonForIntent(String buttonKey) {
            Preference button = findPreference(buttonKey);
            if (button != null) {
                button.setOnPreferenceClickListener(preference -> {
                    try {
                        if ("author_button".equals(buttonKey)) {
                            Intent intent = new Intent(getContext(), AuthorActivity.class);
                            startActivity(intent);
                        } else if ("permission_button".equals(buttonKey)) {
                            Intent intent = new Intent(getContext(), PermissionActivity.class);
                            startActivity(intent);
                        }
                    } catch (Exception e) {
                        new SentryManager(requireContext()).captureException(e);
                    }
                    return true;
                });
            }
        }

        /**
         * Sets up a change listener for dark mode settings.
         */
        private void setupDarkModeChangeListener(ListPreference setting) {
            setting.setOnPreferenceChangeListener((preference, newValue) -> {
                try {
                    new SentryManager(requireContext())
                            .captureMessage("Dark mode set to " + newValue.toString() + ".");
                    SharedPreferencesManager.putString("dark_mode", newValue.toString());
                } catch (Exception e) {
                    new SentryManager(requireContext()).captureException(e);
                }
                return true;
            });
        }

        /**
         * Sets up a change listener for app lock settings.
         */
        private void setupAppLockChangeListener(SwitchPreference setting) {
            setting.setOnPreferenceChangeListener((preference, newValue) -> {
                try {
                    new SentryManager(requireContext())
                            .captureMessage("App lock set to " + newValue.toString() + ".");
                    SharedPreferencesManager.putBoolean("app_lock_enable", (Boolean) newValue);
                } catch (Exception e) {
                    new SentryManager(requireContext()).captureException(e);
                }
                return true;
            });
        }

        /**
         * Sets up a change listener for the Sentry enable/disable setting.
         */
        private void setupSentryChangeListener(SwitchPreference switchPreference) {
            if (switchPreference != null) {
                switchPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                    try {
                        new SentryManager(requireContext())
                                .captureMessage("Sentry set to " + newValue.toString() + ".");
                        boolean isEnabled = (boolean) newValue;
                        SharedPreferencesManager.putBoolean("sentry_enable", isEnabled);
                        setPreferenceVisibility("whitelist_domains", isEnabled);
                        setPreferenceVisibility("whitelist_domain_1_button", isEnabled);
                        setPreferenceVisibility("whitelist_domain_2_button", isEnabled);
                    } catch (Exception e) {
                        new SentryManager(requireContext()).captureException(e);
                    }
                    return true;
                });
            }
        }
    }
}
