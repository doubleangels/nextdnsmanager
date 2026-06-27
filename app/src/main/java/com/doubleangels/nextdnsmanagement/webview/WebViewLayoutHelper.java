package com.doubleangels.nextdnsmanagement.webview;

import android.view.ViewGroup;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.doubleangels.nextdnsmanagement.R;

/**
 * Ensures the main WebView exists in the activity layout after render-process recovery.
 */
public final class WebViewLayoutHelper {

    private WebViewLayoutHelper() {
    }

    /**
     * Returns the layout WebView, creating and attaching a new instance when the previous
     * one was destroyed during render-process recovery.
     */
    public static WebView findOrCreateWebView(AppCompatActivity activity,
            SwipeRefreshLayout swipeRefreshLayout) {
        WebView webView = activity.findViewById(R.id.webView);
        if (webView != null) {
            return webView;
        }

        SwipeRefreshLayout container = swipeRefreshLayout != null
                ? swipeRefreshLayout
                : activity.findViewById(R.id.swipeRefreshLayout);
        if (container == null) {
            return null;
        }

        webView = new WebView(activity);
        webView.setId(R.id.webView);
        container.addView(webView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        return webView;
    }
}
