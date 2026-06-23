package com.doubleangels.nextdnsmanagement;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.doubleangels.nextdnsmanagement.adaptors.PermissionsAdapter;
import com.doubleangels.nextdnsmanagement.sentry.SentryInitializer;
import com.doubleangels.nextdnsmanagement.sentry.SentryManager;
import com.doubleangels.nextdnsmanagement.utils.StatusBarHelper;

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
     * Configures the status bar appearance based on the current UI mode (light or
     * dark).
     */
    private void setupStatusBarForActivity() {
        StatusBarHelper.apply(this);
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
                    try {
                        PermissionInfo permissionInfo = getPackageManager().getPermissionInfo(permission, 0);
                        permissions.add(permissionInfo);
                    } catch (PackageManager.NameNotFoundException ignored) {
                        // Ignore permissions not found on older Android versions
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            // Capture exception if the package name is not found
            sentryManager.captureException(e);
        }
        return permissions;
    }
}
