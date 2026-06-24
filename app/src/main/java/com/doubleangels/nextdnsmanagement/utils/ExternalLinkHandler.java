package com.doubleangels.nextdnsmanagement.utils;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebView;
import android.widget.Toast;

import com.doubleangels.nextdnsmanagement.R;

/**
 * Opens links in an external browser when one is available, otherwise falls back
 * to loading the URL in a WebView when one is provided.
 */
public final class ExternalLinkHandler {

    private ExternalLinkHandler() {
    }

    public static boolean isNextDnsHost(Uri uri) {
        if (uri == null) {
            return false;
        }
        String host = uri.getHost();
        if (host == null) {
            return false;
        }
        return host.equals("nextdns.io") || host.endsWith(".nextdns.io");
    }

    public static void openExternalLink(Context context, Uri uri) {
        openExternalLink(context, null, uri);
    }

    /**
     * @return {@code true} when the URL was handled externally or loaded as a fallback.
     */
    public static boolean openExternalLink(Context context, WebView webView, Uri uri) {
        if (uri == null) {
            return false;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        try {
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            if (loadInWebView(webView, uri)) {
                return true;
            }
            Toast.makeText(context, R.string.no_browser_found, Toast.LENGTH_LONG).show();
            return false;
        } catch (SecurityException e) {
            Toast.makeText(context, R.string.link_open_security_error, Toast.LENGTH_LONG)
                    .show();
            return loadInWebView(webView, uri);
        }
    }

    private static boolean loadInWebView(WebView webView, Uri uri) {
        if (webView == null || !isNextDnsHost(uri)) {
            return false;
        }
        webView.loadUrl(uri.toString());
        return true;
    }
}
