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
 * Activity that displays the list of permissions requested by the application.
 * <p>
 * This activity initializes Sentry for error logging, sets up the status bar
 * appearance,
 * and populates a RecyclerView with the application's permissions.
 * </p>
 */
@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class PermissionActivity extends AppCompatActivity {

    // SentryManager instance for capturing exceptions and logging messages
    private SentryManager sentryManager;

    /**
     * Called when the activity is created.
     * <p>
     * Initializes Sentry, sets up the status bar, and configures the RecyclerView
     * to display permissions.
     * </p>
     *
     * @param savedInstanceState Bundle containing the activity's previously saved
     *                           state, if any.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the activity's layout
        setContentView(R.layout.activity_permission);

        // Initialize SentryManager for error tracking
        sentryManager = new SentryManager(this);
        try {
            // If Sentry is enabled, initialize it
            if (sentryManager.isEnabled()) {
                SentryInitializer.initialize(this);
            }
            // Setup the status bar appearance
            setupStatusBarForActivity();
        } catch (Exception e) {
            // Capture any exceptions during initialization
            sentryManager.captureException(e);
        }

        // Setup the RecyclerView for displaying the list of permissions
        RecyclerView recyclerView = findViewById(R.id.permissionRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        // Retrieve the permissions list and set the adapter
        List<PermissionInfo> permissions = getPermissionsList(sentryManager);
        recyclerView.setAdapter(new PermissionsAdapter(permissions));
    }

    /**
     * Called when the activity is about to be destroyed.
     * <p>
     * Cleans up resources by removing the adapter from the RecyclerView and
     * releasing the SentryManager instance.
     * </p>
     */
    @Override
    protected void onDestroy() {
        // Remove the adapter to release RecyclerView resources
        RecyclerView recyclerView = findViewById(R.id.permissionRecyclerView);
        if (recyclerView != null) {
            recyclerView.setAdapter(null);
        }
        // Release the SentryManager instance
        if (sentryManager != null) {
            sentryManager = null;
        }
        super.onDestroy();
    }

    /**
     * Attaches a new base context with the locale configured based on the device
     * settings.
     *
     * @param newBase The new base context.
     */
    @Override
    protected void attachBaseContext(Context newBase) {
        // Get the current configuration
        Configuration config = newBase.getResources().getConfiguration();
        // Determine the primary locale from the configuration, or use the default
        // locale
        Locale newLocale = (!config.getLocales().isEmpty()) ? config.getLocales().get(0) : Locale.getDefault();
        // Create a new configuration with the desired locale
        Configuration overrideConfig = new Configuration(config);
        overrideConfig.setLocale(newLocale);
        // Create a localized context using the override configuration
        Context localizedContext = newBase.createConfigurationContext(overrideConfig);
        super.attachBaseContext(localizedContext);
    }

    /**
     * Called when the user responds to a permission request.
     * <p>
     * If the notification permission was requested, the permissions list is
     * refreshed.
     * </p>
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
        // Check if the notification permission request was handled
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
                // Determine if the device is using a light theme
                boolean isLightTheme = (getResources().getConfiguration().uiMode
                        & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO;
                // Set system bars appearance based on the theme
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
                // Retrieve updated permissions list and set a new adapter
                List<PermissionInfo> permissions = getPermissionsList(sentryManager);
                recyclerView.setAdapter(new PermissionsAdapter(permissions));
            }
        } catch (Exception e) {
            // Capture any exceptions that occur during the refresh
            sentryManager.captureException(e);
        }
    }

    /**
     * Retrieves the list of permissions requested by the application.
     *
     * @param sentryManager SentryManager instance for capturing exceptions.
     * @return List of PermissionInfo objects representing the app's requested
     *         permissions.
     */
    private List<PermissionInfo> getPermissionsList(SentryManager sentryManager) {
        List<PermissionInfo> permissions = new ArrayList<>();
        try {
            // Get the package info including the requested permissions
            PackageInfo packageInfo = getPackageManager().getPackageInfo(
                    getPackageName(),
                    PackageManager.GET_PERMISSIONS);
            // If requested permissions are present, iterate and add them to the list
            if (packageInfo.requestedPermissions != null) {
                for (String permission : packageInfo.requestedPermissions) {
                    PermissionInfo permissionInfo = getPackageManager().getPermissionInfo(permission, 0);
                    permissions.add(permissionInfo);
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            // Capture exception if the package name is not found
            sentryManager.captureException(e);
        }
        return permissions;
    }
}
