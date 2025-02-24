package com.doubleangels.nextdnsmanagement.webview;

import android.app.Activity;
import android.content.Context;
import android.webkit.JavascriptInterface;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.doubleangels.nextdnsmanagement.sentry.SentryManager;

public class WebAppInterface {
    private final Context context;
    private final SwipeRefreshLayout swipeRefreshLayout;

    public WebAppInterface(Context context, SwipeRefreshLayout swipeRefreshLayout) {
        this.context = context;
        this.swipeRefreshLayout = swipeRefreshLayout;
    }

    /**
     * Called from JavaScript to enable or disable the swipe refresh.
     */
    @JavascriptInterface
    public void setSwipeRefreshEnabled(final boolean enabled) {
        try {
            if (context instanceof Activity) {
                ((Activity) context).runOnUiThread(() -> swipeRefreshLayout.setEnabled(enabled));
            } else {
                new SentryManager(context).captureMessage("Context is not an Activity instance. Cannot update swipe refresh.");
            }
        } catch (Exception e) {
            new SentryManager(context).captureException(e);
        }
    }
}
