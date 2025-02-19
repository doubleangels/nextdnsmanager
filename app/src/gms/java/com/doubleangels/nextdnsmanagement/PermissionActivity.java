package com.doubleangels.nextdnsmanagement;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowInsetsController;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.doubleangels.nextdnsmanagement.adaptors.PermissionsAdapter;
import com.doubleangels.nextdnsmanagement.sentry.SentryInitializer;
import com.doubleangels.nextdnsmanagement.sentry.SentryManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * An activity that displays a list of requested permissions and handles the
 * POST_NOTIFICATIONS permission request flow on Android Tiramisu and above.
 */
@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class PermissionActivity extends AppCompatActivity {

    // A request code for prompting the user to grant notification permission
    private static final int REQUEST_POST_NOTIFICATIONS = 100;

    // The Android runtime permission for posting notifications
    private static final String POST_NOTIFICATIONS = android.Manifest.permission.POST_NOTIFICATIONS;

    // A reference to SentryManager for capturing or logging messages and exceptions
    private SentryManager sentryManager;

    /**
     * Called when the activity is created. Initializes Sentry if enabled, sets up the system bars,
     * applies the locale configuration, and checks for notification permission needs.
     * It also sets up the RecyclerView for displaying permission info.
     *
     * @param savedInstanceState The saved instance state, if any.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission);

        // Instantiate SentryManager for logging or error tracking
        sentryManager = new SentryManager(this);
        try {
            // Initialize Sentry only if the user has enabled it in preferences
            if (sentryManager.isEnabled()) {
                SentryInitializer.initialize(this);
            }

            // Adjust the status bar (light/dark icons) based on the current theme
            setupStatusBarForActivity();

            // Apply the locale settings (language preferences) and log which language is being used
            String appLocale = setupLanguageForActivity();
            sentryManager.captureMessage("Using locale: " + appLocale);

            // Check if we need to prompt the user for notification permission
            if (needsNotificationPermission()) {
                requestNotificationPermission();
            }
        } catch (Exception e) {
            // Capture any exceptions with Sentry (if enabled) or log them locally
            sentryManager.captureException(e);
        }

        // Set up the RecyclerView to show a list of this app's declared permissions
        RecyclerView recyclerView = findViewById(R.id.permissionRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Retrieve a list of requested permissions and set the adapter
        List<PermissionInfo> permissions = getPermissionsList(sentryManager);
        recyclerView.setAdapter(new PermissionsAdapter(permissions));
    }

    /**
     * Determines if the POST_NOTIFICATIONS permission has been granted or not.
     *
     * @return true if the permission is not granted, false otherwise.
     */
    private boolean needsNotificationPermission() {
        return checkSelfPermission(POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Requests the POST_NOTIFICATIONS permission from the user with the designated request code.
     */
    private void requestNotificationPermission() {
        requestPermissions(new String[]{POST_NOTIFICATIONS}, REQUEST_POST_NOTIFICATIONS);
    }

    /**
     * Callback for the result from requesting permissions.
     * If the request code matches, we refresh the permission list to reflect any changes.
     *
     * @param requestCode  The code used to request the permission.
     * @param permissions  The requested permissions.
     * @param grantResults The grant results for the corresponding permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_POST_NOTIFICATIONS) {
            // Refresh the displayed permission list in the RecyclerView
            refreshPermissionsList();
        }
    }

    /**
     * Configures the status bar appearance (light or dark icons) on devices
     * newer than a certain API level.
     */
    private void setupStatusBarForActivity() {
        // The UPSIDE_DOWN_CAKE constant is a placeholder; adjust for real version checks as needed.
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            WindowInsetsController insetsController = getWindow().getInsetsController();
            if (insetsController != null) {
                // Check if the current system theme is light or dark
                boolean isLightTheme = (getResources().getConfiguration().uiMode
                        & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO;

                // Update the system bar icons to appear correctly in light or dark theme
                insetsController.setSystemBarsAppearance(
                        isLightTheme
                                ? WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                                : 0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
            }
        }
    }

    /**
     * Applies the current device locale to the configuration and returns the language code.
     *
     * @return A string representing the current locale's language (e.g., "en").
     */
    private String setupLanguageForActivity() {
        // Retrieve the default locale
        Locale appLocale = Locale.getDefault();
        Locale.setDefault(appLocale);

        // Apply the locale to the current configuration
        Configuration config = getResources().getConfiguration();
        config.setLocale(appLocale);

        // Update the configuration in the current resources
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        // Return the language code (e.g., "en" or "fr")
        return appLocale.getLanguage();
    }

    /**
     * Refreshes the RecyclerView by fetching the updated list of requested permissions
     * and resetting the adapter.
     */
    private void refreshPermissionsList() {
        RecyclerView recyclerView = findViewById(R.id.permissionRecyclerView);
        if (recyclerView != null) {
            List<PermissionInfo> permissions = getPermissionsList(sentryManager);
            recyclerView.setAdapter(new PermissionsAdapter(permissions));
        }
    }

    /**
     * Retrieves the list of permissions requested by this app from the PackageInfo.
     * Captures exceptions with Sentry if they occur (e.g., if package info not found).
     *
     * @param sentryManager A reference to SentryManager for capturing/logging exceptions.
     * @return A list of PermissionInfo objects declared by this app.
     */
    private List<PermissionInfo> getPermissionsList(SentryManager sentryManager) {
        List<PermissionInfo> permissions = new ArrayList<>();
        try {
            // Get package info including requested permissions
            PackageInfo packageInfo = getPackageManager().getPackageInfo(
                    getPackageName(),
                    PackageManager.GET_PERMISSIONS
            );

            // If permissions are found, convert each permission name to PermissionInfo
            if (packageInfo.requestedPermissions != null) {
                for (String permission : packageInfo.requestedPermissions) {
                    PermissionInfo permissionInfo = getPackageManager().getPermissionInfo(permission, 0);
                    permissions.add(permissionInfo);
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            sentryManager.captureException(e);
        }
        return permissions;
    }
}
