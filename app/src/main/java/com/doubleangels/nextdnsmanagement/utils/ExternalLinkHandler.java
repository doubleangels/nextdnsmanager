package com.doubleangels.nextdnsmanagement.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.webkit.WebView;
import android.widget.Toast;

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
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        PackageManager packageManager = context.getPackageManager();
        if (intent.resolveActivity(packageManager) != null) {
            try {
                context.startActivity(intent);
                return true;
            } catch (ActivityNotFoundException ignored) {
                // resolveActivity can succeed while startActivity still fails on some devices.
            } catch (SecurityException e) {
                Toast.makeText(context, "Unable to open link due to security restrictions.", Toast.LENGTH_LONG)
                        .show();
                return loadInWebView(webView, uri);
            }
        }

        Toast.makeText(context, "No browser found to open link.", Toast.LENGTH_LONG).show();
        return false;
    }

    private static boolean loadInWebView(WebView webView, Uri uri) {
        if (webView == null || !isNextDnsHost(uri)) {
            return false;
        }
        webView.loadUrl(uri.toString());
        return true;
    }
}
