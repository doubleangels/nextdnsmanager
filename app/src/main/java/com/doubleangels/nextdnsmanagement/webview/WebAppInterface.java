package com.doubleangels.nextdnsmanagement.webview;

import android.app.Activity;
import android.content.Context;
import android.webkit.JavascriptInterface;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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
        ((Activity) context).runOnUiThread(() -> swipeRefreshLayout.setEnabled(enabled));
    }
}