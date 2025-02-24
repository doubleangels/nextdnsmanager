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

    // Request code for prompting notification permission.
    private static final int REQUEST_POST_NOTIFICATIONS = 100;
    // Runtime permission for posting notifications.
    private static final String POST_NOTIFICATIONS = android.Manifest.permission.POST_NOTIFICATIONS;

    // SentryManager for capturing messages and exceptions.
    private SentryManager sentryManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission);

        // Initialize SentryManager.
        sentryManager = new SentryManager(this);
        try {
            // Initialize Sentry if enabled.
            if (sentryManager.isEnabled()) {
                SentryInitializer.initialize(this);
            }
            // Adjust the status bar appearance.
            setupStatusBarForActivity();

            // Apply locale settings and capture which language is in use.
            String appLocale = setupLanguageForActivity();
            sentryManager.captureMessage("Using locale: " + appLocale);

            // Check if notification permission is needed.
            if (needsNotificationPermission()) {
                requestNotificationPermission();
            }
        } catch (Exception e) {
            sentryManager.captureException(e);
        }

        // Set up the RecyclerView to display the list of permissions.
        RecyclerView recyclerView = findViewById(R.id.permissionRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        List<PermissionInfo> permissions = getPermissionsList(sentryManager);
        recyclerView.setAdapter(new PermissionsAdapter(permissions));
    }

    /**
     * Determines if the POST_NOTIFICATIONS permission has been granted.
     *
     * @return true if permission is not granted, false otherwise.
     */
    private boolean needsNotificationPermission() {
        return checkSelfPermission(POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Requests the POST_NOTIFICATIONS permission from the user.
     */
    private void requestNotificationPermission() {
        requestPermissions(new String[]{POST_NOTIFICATIONS}, REQUEST_POST_NOTIFICATIONS);
    }

    /**
     * Callback for the result from requesting permissions.
     * Refreshes the permission list if the notification permission was requested.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_POST_NOTIFICATIONS) {
            refreshPermissionsList();
        }
    }

    /**
     * Configures the status bar appearance (light/dark icons) based on the current theme.
     */
    private void setupStatusBarForActivity() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            WindowInsetsController insetsController = getWindow().getInsetsController();
            if (insetsController != null) {
                boolean isLightTheme = (getResources().getConfiguration().uiMode
                        & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO;
                insetsController.setSystemBarsAppearance(
                        isLightTheme ? WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS : 0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
            }
        }
    }

    /**
     * Applies the current device locale to the configuration and returns the language code.
     *
     * @return The language code (e.g., "en").
     */
    private String setupLanguageForActivity() {
        Locale appLocale = Locale.getDefault();
        Locale.setDefault(appLocale);
        Configuration config = getResources().getConfiguration();
        config.setLocale(appLocale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        return appLocale.getLanguage();
    }

    /**
     * Refreshes the RecyclerView by retrieving the updated permissions list.
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
     * Retrieves the list of permissions declared in this app.
     *
     * @param sentryManager SentryManager instance for capturing exceptions.
     * @return A list of PermissionInfo objects.
     */
    private List<PermissionInfo> getPermissionsList(SentryManager sentryManager) {
        List<PermissionInfo> permissions = new ArrayList<>();
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(
                    getPackageName(),
                    PackageManager.GET_PERMISSIONS
            );
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
