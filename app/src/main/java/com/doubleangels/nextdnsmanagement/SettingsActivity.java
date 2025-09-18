package com.doubleangels.nextdnsmanagement;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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

/**
 * Activity for application settings. It initializes shared preferences, sets up
 * error logging (Sentry),
 * configures dark mode settings, and loads the SettingsFragment.
 */
public class SettingsActivity extends AppCompatActivity {

    // Sentry manager instance for capturing errors.
    public SentryManager sentryManager;

    /**
     * Called when the activity is created. Sets up shared preferences, error
     * logging,
     * dark mode configuration, and initializes the settings views.
     *
     * @param savedInstanceState Bundle containing saved state data, if any.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the layout for this activity.
        setContentView(R.layout.activity_settings);

        // Initialize the SentryManager for error logging.
        sentryManager = new SentryManager(this);
        try {
            // Initialize shared preferences.
            SharedPreferencesManager.init(this);
        } catch (Exception e) {
            if (sentryManager != null) {
                sentryManager.captureException(e);
            }
        }
        try {
            // Log the current settings for dark mode and Sentry enable flag.
            assert sentryManager != null;
            sentryManager.captureMessage("SharedPreferences 'dark_mode' value: "
                    + SharedPreferencesManager.getString("dark_mode", "match"));
            sentryManager.captureMessage("SharedPreferences 'sentry_enable' value: "
                    + SharedPreferencesManager.getBoolean("sentry_enable", false));
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
        try {
            // Initialize Sentry if it is enabled.
            if (sentryManager.isEnabled()) {
                SentryInitializer.initialize(this);
            }
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
        try {
            // Set up dark mode configuration based on saved preferences.
            setupDarkModeForActivity(SharedPreferencesManager.getString("dark_mode", "match"));
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
        try {
            // Initialize the settings views (fragment).
            initializeViews();
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
    }

    /**
     * Attaches a new base context with locale settings based on device
     * configuration.
     *
     * @param newBase The new base context.
     */
    @Override
    protected void attachBaseContext(Context newBase) {
        // Retrieve the current configuration.
        Configuration config = newBase.getResources().getConfiguration();
        // Get the primary locale from the configuration, or default if empty.
        Locale newLocale = (!config.getLocales().isEmpty()) ? config.getLocales().get(0) : Locale.getDefault();
        // Create a new configuration overriding the locale.
        Configuration overrideConfig = new Configuration(config);
        overrideConfig.setLocale(newLocale);
        // Create a localized context with the new configuration.
        Context localizedContext = newBase.createConfigurationContext(overrideConfig);
        super.attachBaseContext(localizedContext);
    }

    /**
     * Configures the dark mode settings for the activity.
     *
     * @param darkMode The dark mode setting value from shared preferences.
     */
    private void setupDarkModeForActivity(String darkMode) {
        // Only set dark mode for Android versions below TIRAMISU.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            sentryManager.captureMessage("Dark mode setting: " + darkMode);
            // Set the night mode based on the darkMode string.
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
     * Initializes the settings fragment view.
     */
    private void initializeViews() {
        // Replace the container with the SettingsFragment.
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commitNow();
    }

    /**
     * Fragment for displaying and handling settings preferences.
     */
    public static class SettingsFragment extends PreferenceFragmentCompat {

        /**
         * Called during fragment creation to initialize the preference hierarchy from
         * an XML resource.
         *
         * @param savedInstanceState Saved state, if any.
         * @param rootKey            The key of the preference hierarchy.
         */
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            try {
                // Load preferences from the XML resource.
                setPreferencesFromResource(R.xml.root_preferences, rootKey);
            } catch (Exception e) {
                new SentryManager(requireContext()).captureException(e);
            }
            try {
                // Initialize shared preferences for the fragment.
                SharedPreferencesManager.init(requireContext());
            } catch (Exception e) {
                new SentryManager(requireContext()).captureException(e);
            }
            try {
                // Set the initial visibility of Sentry-related preferences.
                setInitialSentryVisibility(SharedPreferencesManager.getBoolean("sentry_enable", false));
            } catch (Exception e) {
                new SentryManager(requireContext()).captureException(e);
            }
            try {
                // Retrieve references to various preferences.
                SwitchPreference sentryEnablePreference = findPreference("sentry_enable");
                SwitchPreference appLockPreference = findPreference("app_lock_enable");
                ListPreference darkModePreference = findPreference("dark_mode");
                final BiometricLock biometricLock = new BiometricLock((AppCompatActivity) requireContext());

                // Hide app lock preferences if biometric authentication is not available.
                if (!biometricLock.canAuthenticate()) {
                    setPreferenceVisibility("applock", false);
                }
                // Set up change listeners for each preference.
                if (sentryEnablePreference != null) {
                    setupSentryChangeListener(sentryEnablePreference);
                }
                if (appLockPreference != null) {
                    setupAppLockChangeListener(appLockPreference);
                }
                if (darkModePreference != null) {
                    setupDarkModeChangeListener(darkModePreference);
                }
                // Setup buttons that perform various actions.
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
                // Set the version name as the summary for the version preference.
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
         * Sets the initial visibility of Sentry-related preferences.
         *
         * @param visibility The visibility flag for Sentry preferences.
         */
        private void setInitialSentryVisibility(Boolean visibility) {
            setPreferenceVisibility("whitelist_domains", visibility);
            setPreferenceVisibility("whitelist_domain_1_button", visibility);
            setPreferenceVisibility("whitelist_domain_2_button", visibility);
        }

        /**
         * Sets the visibility for a given preference key.
         *
         * @param key        The key of the preference.
         * @param visibility The visibility flag.
         */
        private void setPreferenceVisibility(String key, Boolean visibility) {
            Preference preference = findPreference(key);
            if (preference != null) {
                preference.setVisible(visibility);
            }
        }

        /**
         * Sets up a button preference that performs an action when clicked.
         *
         * @param buttonKey    The key for the button preference.
         * @param textResource The text resource to use for the action.
         */
        private void setupButton(String buttonKey, int textResource) {
            Preference button = findPreference(buttonKey);
            if (button != null) {
                button.setOnPreferenceClickListener(preference -> {
                    try {
                        // If the button is for whitelisting a domain, copy the text to clipboard.
                        if ("whitelist_domain_1_button".equals(buttonKey)
                                || "whitelist_domain_2_button".equals(buttonKey)) {
                            ClipboardManager clipboardManager = (ClipboardManager) requireContext()
                                    .getSystemService(Context.CLIPBOARD_SERVICE);
                            CharSequence copiedText = getString(textResource);
                            ClipData copiedData = ClipData.newPlainText("text", copiedText);
                            clipboardManager.setPrimaryClip(copiedData);
                            Toast.makeText(getContext(), "Text copied!", Toast.LENGTH_SHORT).show();
                        } else {
                            // Otherwise, open the URL in a browser.
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
         * Sets up a button preference that starts a new activity based on its key.
         *
         * @param buttonKey The key for the button preference.
         */
        private void setupButtonForIntent(String buttonKey) {
            Preference button = findPreference(buttonKey);
            if (button != null) {
                button.setOnPreferenceClickListener(preference -> {
                    try {
                        // Launch the appropriate activity based on the button key.
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
         * Sets up a listener to handle changes to the dark mode preference.
         *
         * @param setting The dark mode list preference.
         */
        private void setupDarkModeChangeListener(ListPreference setting) {
            setting.setOnPreferenceChangeListener((preference, newValue) -> {
                try {
                    // Log and save the new dark mode setting.
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
         * Sets up a listener to handle changes to the app lock preference.
         * Requires biometric authentication to disable app lock for security.
         *
         * @param setting The app lock switch preference.
         */
        private void setupAppLockChangeListener(SwitchPreference setting) {
            setting.setOnPreferenceChangeListener((preference, newValue) -> {
                try {
                    boolean newValueBoolean = (Boolean) newValue;
                    boolean currentValue = SharedPreferencesManager.getBoolean("app_lock_enable", true);

                    // If trying to disable app lock, require biometric authentication
                    if (currentValue && !newValueBoolean) {
                        final BiometricLock biometricLock = new BiometricLock((AppCompatActivity) requireContext());
                        if (biometricLock.canAuthenticate()) {
                            biometricLock.showPrompt(
                                    "Disable App Lock",
                                    "Authenticate to disable app lock",
                                    "Use biometric or device credentials to confirm this action",
                                    new BiometricLock.BiometricLockCallback() {
                                        @Override
                                        public void onAuthenticationSucceeded() {
                                            // Authentication successful, allow the change
                                            SharedPreferencesManager.putBoolean("app_lock_enable", false);
                                            new SentryManager(requireContext())
                                                    .captureMessage(
                                                            "App lock disabled after biometric authentication.");
                                        }

                                        @Override
                                        public void onAuthenticationError(String error) {
                                            // Authentication failed, revert the change
                                            setting.setChecked(true);
                                            new SentryManager(requireContext())
                                                    .captureMessage(
                                                            "App lock disable failed - authentication error: " + error);
                                        }

                                        @Override
                                        public void onAuthenticationFailed() {
                                            // Authentication failed, revert the change
                                            setting.setChecked(true);
                                            new SentryManager(requireContext())
                                                    .captureMessage("App lock disable failed - authentication failed");
                                        }
                                    });
                            return false; // Don't apply the change yet
                        } else {
                            // No biometric available, don't allow disabling
                            setting.setChecked(true);
                            new SentryManager(requireContext())
                                    .captureMessage("Cannot disable app lock - biometric authentication not available");
                            return false;
                        }
                    } else {
                        // Enabling app lock or other changes don't require authentication
                        SharedPreferencesManager.putBoolean("app_lock_enable", newValueBoolean);
                        new SentryManager(requireContext())
                                .captureMessage("App lock set to " + newValue + ".");
                    }
                } catch (Exception e) {
                    new SentryManager(requireContext()).captureException(e);
                }
                return true;
            });
        }

        /**
         * Sets up a listener to handle changes to the Sentry preference.
         *
         * @param switchPreference The Sentry switch preference.
         */
        private void setupSentryChangeListener(SwitchPreference switchPreference) {
            if (switchPreference != null) {
                switchPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                    try {
                        // Log and update the Sentry preference.
                        new SentryManager(requireContext())
                                .captureMessage("Sentry set to " + newValue.toString() + ".");
                        boolean isEnabled = (boolean) newValue;
                        SharedPreferencesManager.putBoolean("sentry_enable", isEnabled);
                        // Update visibility for Sentry-related preferences.
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
