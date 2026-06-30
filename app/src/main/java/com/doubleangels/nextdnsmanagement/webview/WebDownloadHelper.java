package com.doubleangels.nextdnsmanagement.webview;

import android.net.Uri;

/**
 * Validates WebView-initiated download URLs before handing them to DownloadManager.
 */
public final class WebDownloadHelper {

    private WebDownloadHelper() {
    }

    /**
     * Returns true when the URL uses a scheme supported by {@link android.app.DownloadManager}.
     */
    public static boolean isDownloadManagerSupportedUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        Uri uri = Uri.parse(url.trim());
        String scheme = uri.getScheme();
        return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    }
}
