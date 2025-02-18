package com.doubleangels.nextdnsmanagement;

import android.annotation.SuppressLint;
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

import com.doubleangels.nextdnsmanagement.sentry.SentryInitializer;
import com.doubleangels.nextdnsmanagement.sentry.SentryManager;
import com.doubleangels.nextdnsmanagement.sharedpreferences.SharedPreferencesManager;

import java.util.Locale;

/**
 * SettingsActivity is responsible for displaying and handling various user preferences,
 * such as dark mode, Sentry error-logging enable/disable, and links to external resources.
 */
public class SettingsActivity extends AppCompatActivity {

    // Used for capturing and sending messages/exceptions to Sentry.
    public SentryManager sentryManager;

    /**
     * Called when the activity is created.
     * Initializes the SentryManager, sets up dark mode (if applicable),
     * and loads the PreferencesFragment for in-app settings.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize Sentry Manager
        sentryManager = new SentryManager(this);

        // Initialize our custom shared preferences manager
        SharedPreferencesManager.init(this);

        // Capture debug messages showing current preference values
        sentryManager.captureMessage("SharedPreferences 'dark_mode' value: " 
                + SharedPreferencesManager.getString("dark_mode", "match"));
        sentryManager.captureMessage("SharedPreferences 'sentry_enable' value: " 
                + SharedPreferencesManager.getBoolean("sentry_enable", false));

        try {
            // If Sentry is enabled in user preferences, initialize it
            if (sentryManager.isEnabled()) {
                SentryInitializer.initialize(this);
            }

            // Apply the current locale, capturing which locale we end up using
            String appLocale = setupLanguageForActivity();
            sentryManager.captureMessage("Using locale: " + appLocale);

            // Apply or restore the user's dark mode preference
            setupDarkModeForActivity(SharedPreferencesManager.getString("dark_mode", "match"));

            // Load the PreferenceFragment that defines all of the user-facing settings
            initializeViews();
        } catch (Exception e) {
            // Capture any exceptions to Sentry (if enabled) or log them locally
            sentryManager.captureException(e);
        }
    }

    /**
     * Applies the currently configured locale to the context and returns the language code.
     *
     * @return The language code string (e.g., "en", "fr") for logging/debugging.
     */
    private String setupLanguageForActivity() {
        Configuration config = getResources().getConfiguration();
        Locale appLocale = config.getLocales().get(0);
        // Set the default locale to our chosen locale
        Locale.setDefault(appLocale);

        // Create a new config reflecting our chosen locale, then apply it
        Configuration newConfig = new Configuration(config);
        newConfig.setLocale(appLocale);
        new ContextThemeWrapper(getBaseContext(), R.style.AppTheme).applyOverrideConfiguration(newConfig);

        return appLocale.getLanguage();
    }

    /**
     * Applies the dark mode setting if the device is running below TIRAMISU (Android 13).
     * For TIRAMISU and above, this code block can be adjusted or removed
     * depending on how you want to handle user preferences.
     *
     * @param darkMode The string value representing the user's dark mode preference.
     */
    private void setupDarkModeForActivity(String darkMode) {
        // Only apply manually for pre-TIRAMISU devices
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            sentryManager.captureMessage("Dark mode setting: " + darkMode);

            if (darkMode.contains("match")) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            } else if (darkMode.contains("on")) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                // "off" or any other value defaults to NO
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        }
    }

    /**
     * Replaces the main activity view with a PreferenceFragment that displays settings
     * defined in root_preferences.xml.
     */
    private void initializeViews() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commitNow();
    }

    /**
     * SettingsFragment implements the user preferences UI using AndroidX Preference APIs.
     * It includes toggles for Sentry, dark mode selection, and clickable links.
     */
    public static class SettingsFragment extends PreferenceFragmentCompat {

        /**
         * Called to initialize the preference hierarchy from an XML resource.
         *
         * @param savedInstanceState The saved instance state of this fragment.
         * @param rootKey            If non-null, this preference fragment should be rooted
         *                           at the PreferenceScreen with this key.
         */
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            // Load the preferences from XML
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            // Initialize SharedPreferencesManager for accessing preference values
            SharedPreferencesManager.init(requireContext());

            // Show or hide certain settings based on whether Sentry is currently enabled
            setInitialSentryVisibility(SharedPreferencesManager.getBoolean("sentry_enable", false));

            // Retrieve references to specific preferences
            SwitchPreference sentryEnablePreference = findPreference("sentry_enable");
            ListPreference darkModePreference = findPreference("dark_mode");

            // Attach listeners to handle changes in Sentry or dark mode settings
            if (sentryEnablePreference != null) {
                setupSentryChangeListener(sentryEnablePreference);
            }
            if (darkModePreference != null) {
                setupDarkModeChangeListener(darkModePreference);
            }

            // Set up various buttons that either open external links or copy text to the clipboard
            setupButton("whitelist_domain_1_button", R.string.whitelist_domain_1);
            setupButton("whitelist_domain_2_button", R.string.whitelist_domain_2);
            setupButton("sentry_info_button", R.string.sentry_info_url);
            setupButtonForIntent("author_button");
            setupButton("feedback_button", R.string.feedback_url);
            setupButton("github_button", R.string.github_url);
            setupButton("github_issue_button", R.string.github_issues_url);
            setupButton("donation_button", R.string.donation_url);
            setupButton("translate_button", R.string.translate_url);
            setupButton("privacy_policy_button", R.string.privacy_policy_url);
            setupButton("nextdns_privacy_policy_button", R.string.nextdns_privacy_policy_url);
            setupButton("nextdns_user_agreement_button", R.string.nextdns_user_agreement_url);
            setupButtonForIntent("permission_button");
            setupButton("version_button", R.string.versions_url);

            // Display the current app version name on the "version_button" preference summary
            String versionName = BuildConfig.VERSION_NAME;
            Preference versionPreference = findPreference("version_button");
            if (versionPreference != null) {
                versionPreference.setSummary(versionName);
            }
        }

        /**
         * Shows or hides the "whitelist_domains" category, plus its child preferences,
         * based on whether Sentry is enabled. 
         * 
         * @param visibility True if Sentry is enabled; false otherwise.
         */
        private void setInitialSentryVisibility(Boolean visibility) {
            setPreferenceVisibility("whitelist_domains", visibility);
            setPreferenceVisibility("whitelist_domain_1_button", visibility);
            setPreferenceVisibility("whitelist_domain_2_button", visibility);
        }

        /**
         * Configures a specific preference button to either copy text to the clipboard 
         * or open an external URL when clicked.
         *
         * @param buttonKey   The key identifying the preference in root_preferences.xml.
         * @param textResource The string resource ID (URL or text) to copy/open.
         */
        private void setupButton(String buttonKey, int textResource) {
            Preference button = findPreference(buttonKey);
            assert button != null;
            button.setOnPreferenceClickListener(preference -> {
                // Handle copying text for certain keys
                if ("whitelist_domain_1_button".equals(buttonKey) || "whitelist_domain_2_button".equals(buttonKey)) {
                    ClipboardManager clipboardManager = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    CharSequence copiedText = getString(textResource);
                    ClipData copiedData = ClipData.newPlainText("text", copiedText);
                    clipboardManager.setPrimaryClip(copiedData);
                    Toast.makeText(getContext(), "Text copied!", Toast.LENGTH_SHORT).show();
                } else {
                    // Otherwise, open a URL in a browser
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(textResource)));
                    startActivity(intent);
                }
                return true;
            });
        }

        /**
         * Configures a button (preference) that navigates to another Activity within the app
         * rather than opening a URL or copying text.
         *
         * @param buttonKey The key identifying the preference in root_preferences.xml.
         */
        private void setupButtonForIntent(String buttonKey) {
            Preference button = findPreference(buttonKey);
            assert button != null;
            button.setOnPreferenceClickListener(preference -> {
                if ("author_button".equals(buttonKey)) {
                    // Navigate to the AuthorActivity
                    Intent intent = new Intent(getContext(), AuthorActivity.class);
                    startActivity(intent);
                }
                if ("permission_button".equals(buttonKey)) {
                    // Navigate to the PermissionActivity
                    Intent intent = new Intent(getContext(), PermissionActivity.class);
                    startActivity(intent);
                }
                return true;
            });
        }

        /**
         * Sets up a change listener for the "dark_mode" ListPreference. When a user picks 
         * a new value, it logs the choice to Sentry and persists the setting with SharedPreferences.
         *
         * @param setting The ListPreference that allows users to choose a dark mode strategy.
         */
        private void setupDarkModeChangeListener(ListPreference setting) {
            setting.setOnPreferenceChangeListener((preference, newValue) -> {
                new SentryManager(requireContext()).captureMessage("Dark mode set to " + newValue.toString() + ".");
                SharedPreferencesManager.putString("dark_mode", newValue.toString());
                return true;
            });
        }

        /**
         * Sets up a change listener for the Sentry enable/disable SwitchPreference. This 
         * toggles the visibility of certain preferences (e.g., whitelisting) when switched on/off.
         *
         * @param switchPreference The SwitchPreference that enables or disables Sentry.
         */
        private void setupSentryChangeListener(SwitchPreference switchPreference) {
            if (switchPreference != null) {
                switchPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                    new SentryManager(requireContext()).captureMessage("Sentry set to " + newValue.toString() + ".");
                    boolean isEnabled = (boolean) newValue;
                    SharedPreferencesManager.putBoolean("sentry_enable", isEnabled);
                    
                    // Hide or show whitelist-related preferences depending on the value
                    setPreferenceVisibility("whitelist_domains", isEnabled);
                    setPreferenceVisibility("whitelist_domain_1_button", isEnabled);
                    setPreferenceVisibility("whitelist_domain_2_button", isEnabled);
                    return true;
                });
            }
        }

        /**
         * Sets the visibility of a preference (by key) in this fragment. 
         * Useful for dynamically hiding or showing settings based on app logic.
         *
         * @param key        The preference key to show or hide.
         * @param visibility True to show, false to hide.
         */
        private void setPreferenceVisibility(String key, Boolean visibility) {
            Preference preference = findPreference(key);
            if (preference != null) {
                preference.setVisible(visibility);
            }
        }
    }
}
