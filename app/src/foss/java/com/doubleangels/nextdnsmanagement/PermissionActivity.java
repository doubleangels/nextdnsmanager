package com.doubleangels.nextdnsmanagement;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.doubleangels.nextdnsmanagement.adaptors.PermissionsAdapter;
import com.doubleangels.nextdnsmanagement.sentry.SentryInitializer;
import com.doubleangels.nextdnsmanagement.sentry.SentryManager;
import com.doubleangels.nextdnsmanagement.utils.InsetsHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Activity that displays the list of permissions requested by the application.
 */
@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class PermissionActivity extends BaseActivity {

    private SentryManager sentryManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission);
        setupInsets();

        sentryManager = new SentryManager(this);
        try {
            if (sentryManager.isEnabled()) {
                SentryInitializer.initialize(this);
            }
        } catch (Exception e) {
            sentryManager.captureException(e);
        }

        RecyclerView recyclerView = findViewById(R.id.permissionRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        List<PermissionInfo> permissions = getPermissionsList(sentryManager);
        recyclerView.setAdapter(new PermissionsAdapter(permissions));
    }

    @Override
    protected void onDestroy() {
        RecyclerView recyclerView = findViewById(R.id.permissionRecyclerView);
        if (recyclerView != null) {
            recyclerView.setAdapter(null);
        }
        if (sentryManager != null) {
            sentryManager = null;
        }
        super.onDestroy();
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

    private void setupInsets() {
        View root = findViewById(R.id.root);
        InsetsHelper.installOnRoot(root);
        InsetsHelper.applySystemBarPadding(root);
    }

    private List<PermissionInfo> getPermissionsList(SentryManager sentryManager) {
        List<PermissionInfo> permissions = new ArrayList<>();
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(
                    getPackageName(),
                    PackageManager.GET_PERMISSIONS);
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
            sentryManager.captureException(e);
        }
        return permissions;
    }
}
