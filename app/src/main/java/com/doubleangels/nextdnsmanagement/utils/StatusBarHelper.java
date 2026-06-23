package com.doubleangels.nextdnsmanagement.utils;

import android.app.Activity;
import android.content.res.Configuration;
import android.view.View;
import android.view.WindowInsetsController;

import androidx.annotation.ColorRes;
import androidx.core.content.ContextCompat;

import com.doubleangels.nextdnsmanagement.R;

/**
 * Applies consistent status bar styling on API 32+ (the app minSdk).
 */
public final class StatusBarHelper {

    private StatusBarHelper() {
    }

    public static void apply(Activity activity) {
        apply(activity, R.color.main);
    }

    public static void apply(Activity activity, @ColorRes int statusBarColorRes) {
        View decorView = activity.getWindow().getDecorView();
        decorView.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setBackgroundColor(ContextCompat.getColor(activity, statusBarColorRes));
            return insets;
        });

        WindowInsetsController insetsController = activity.getWindow().getInsetsController();
        if (insetsController != null) {
            boolean isLightTheme = (activity.getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO;
            insetsController.setSystemBarsAppearance(
                    isLightTheme ? WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS : 0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
        }
    }
}
