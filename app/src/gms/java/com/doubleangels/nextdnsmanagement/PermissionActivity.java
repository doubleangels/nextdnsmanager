package com.doubleangels.nextdnsmanagement;

import android.content.Context;
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
 * Activity that displays the permissions requested by the application.
 * It initializes Sentry for error logging, requests notification permissions if
 * needed,
 * and displays the list of permissions using a RecyclerView.
 */
@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class PermissionActivity extends AppCompatActivity {

    // Notification permission constant
    private static final String POST_NOTIFICATIONS = android.Manifest.permission.POST_NOTIFICATIONS;
    // Sentry manager instance for capturing errors
    private SentryManager sentryManager;

    /**
     * Called when the activity is created. Initializes Sentry, sets up the status
     * bar,
     * requests notification permission if needed, and sets up the RecyclerView for
     * displaying permissions.
     *
     * @param savedInstanceState Bundle containing saved state data, if any.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the layout for this activity
        setContentView(R.layout.activity_permission);

        // Initialize the SentryManager
        sentryManager = new SentryManager(this);
        try {
            // Initialize Sentry if it is enabled
            if (sentryManager.isEnabled()) {
                SentryInitializer.initialize(this);
            }
            // Configure the status bar appearance
            setupStatusBarForActivity();
            // If notification permission is required, request it
            if (needsNotificationPermission()) {
                requestNotificationPermission();
            }
        } catch (Exception e) {
            sentryManager.captureException(e);
        }

        // Set up the RecyclerView to display the list of permissions
        RecyclerView recyclerView = findViewById(R.id.permissionRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        List<PermissionInfo> permissions = getPermissionsList(sentryManager);
        recyclerView.setAdapter(new PermissionsAdapter(permissions));
    }

    /**
     * Called when the activity is about to be destroyed. Cleans up the RecyclerView
     * adapter
     * and releases the SentryManager instance.
     */
    @Override
    protected void onDestroy() {
        // Remove the adapter from RecyclerView to free resources
        RecyclerView recyclerView = findViewById(R.id.permissionRecyclerView);
        if (recyclerView != null) {
            recyclerView.setAdapter(null);
        }
        // Release SentryManager
        if (sentryManager != null) {
            sentryManager = null;
        }
        super.onDestroy();
    }

    /**
     * Attaches a new base context with locale settings based on the device
     * configuration.
     *
     * @param newBase The new base context.
     */
    @Override
    protected void attachBaseContext(Context newBase) {
        // Retrieve the current configuration
        Configuration config = newBase.getResources().getConfiguration();
        // Get the primary locale or default locale if none exists
        Locale newLocale = (!config.getLocales().isEmpty()) ? config.getLocales().get(0) : Locale.getDefault();
        // Create a new configuration overriding the locale
        Configuration overrideConfig = new Configuration(config);
        overrideConfig.setLocale(newLocale);
        // Create a localized context with the override configuration
        Context localizedContext = newBase.createConfigurationContext(overrideConfig);
        super.attachBaseContext(localizedContext);
    }

    /**
     * Checks if the app needs to request the POST_NOTIFICATIONS permission.
     *
     * @return True if the permission is not granted, false otherwise.
     */
    private boolean needsNotificationPermission() {
        return checkSelfPermission(POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Requests the POST_NOTIFICATIONS permission.
     */
    private void requestNotificationPermission() {
        requestPermissions(new String[] { POST_NOTIFICATIONS }, 100);
    }

    /**
     * Called when the user responds to a permission request. If the notification
     * permission
     * was requested, refresh the permissions list.
     *
     * @param requestCode  The request code passed in requestPermissions().
     * @param permissions  The requested permissions.
     * @param grantResults The grant results for the corresponding permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            refreshPermissionsList();
        }
    }

    /**
     * Configures the status bar appearance based on the current UI mode (light or
     * dark).
     */
    private void setupStatusBarForActivity() {
        // Check if the Android version supports WindowInsetsController
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            WindowInsetsController insetsController = getWindow().getInsetsController();
            if (insetsController != null) {
                // Determine if the system is in light theme mode
                boolean isLightTheme = (getResources().getConfiguration().uiMode
                        & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO;
                // Set the appearance of the system bars based on the theme
                insetsController.setSystemBarsAppearance(
                        isLightTheme ? WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS : 0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
            }
        }
    }

    /**
     * Refreshes the permissions list displayed in the RecyclerView.
     */
    private void refreshPermissionsList() {
        try {
            RecyclerView recyclerView = findViewById(R.id.permissionRecyclerView);
            if (recyclerView != null) {
                List<PermissionInfo> permissions = getPermissionsList(sentryManager);
                recyclerView.setAdapter(new PermissionsAdapter(permissions));
            }
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
    }

    /**
     * Retrieves the list of permissions requested by the app.
     *
     * @param sentryManager SentryManager instance for capturing exceptions.
     * @return List of PermissionInfo objects representing the app's requested
     *         permissions.
     */
    private List<PermissionInfo> getPermissionsList(SentryManager sentryManager) {
        List<PermissionInfo> permissions = new ArrayList<>();
        try {
            // Retrieve package info with requested permissions
            PackageInfo packageInfo = getPackageManager().getPackageInfo(
                    getPackageName(),
                    PackageManager.GET_PERMISSIONS);
            // If there are any requested permissions, add them to the list
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
