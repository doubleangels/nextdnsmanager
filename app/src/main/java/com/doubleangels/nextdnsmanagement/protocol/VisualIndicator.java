package com.doubleangels.nextdnsmanagement.protocol;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkRequest;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.doubleangels.nextdnsmanagement.R;
import com.doubleangels.nextdnsmanagement.sentry.SentryManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.Arrays;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * A class that monitors and visually indicates the network status, specifically
 * whether NextDNS is being used securely. It listens for changes in network
 * properties and updates a UI element (ImageView) accordingly.
 */
public class VisualIndicator {

    // Used for capturing exceptions or logs to Sentry
    private final SentryManager sentryManager;

    // OkHttpClient instance for making network calls (to check NextDNS status,
    // etc)
    private final OkHttpClient httpClient;

    // Manages network connections and can register callbacks for connectivity
    // changes
    private ConnectivityManager connectivityManager;

    // Callback to track changes in network properties (e.g., Private DNS settings)
    private ConnectivityManager.NetworkCallback networkCallback;

    /**
     * Constructor that initializes SentryManager and OkHttpClient.
     *
     * @param context The application context, used for initializing Sentry and
     *                OkHttpClient.
     */
    public VisualIndicator(Context context) {
        this.sentryManager = new SentryManager(context);
        this.httpClient = new OkHttpClient();
    }

    /**
     * Initializes the network monitoring and sets up a callback to listen for
     * changes in network properties. Also registers a lifecycle observer to
     * unregister the callback when the provided lifecycleOwner is destroyed.
     *
     * @param context        The application or activity context.
     * @param lifecycleOwner The lifecycle owner (e.g., an Activity or Fragment)
     *                       used to automatically unregister the network callback
     *                       upon destruction.
     * @param activity       The Activity where the UI update (ImageView) is
     *                       handled.
     */
    public void initialize(Context context, LifecycleOwner lifecycleOwner, AppCompatActivity activity) {
        try {
            // Obtain the system's ConnectivityManager
            connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager == null) {
                sentryManager.captureMessage("ConnectivityManager is null.");
                return;
            }

            // Build a generic NetworkRequest to monitor all networks
            NetworkRequest networkRequest = new NetworkRequest.Builder().build();

            // Get the currently active network (if any)
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                sentryManager.captureMessage("No active network found.");
                return;
            }

            // Obtain the network's LinkProperties, which include DNS and other connection
            // details
            LinkProperties linkProperties = connectivityManager.getLinkProperties(network);

            // Update the UI immediately based on the current link properties
            update(linkProperties, activity, context);

            // Define the callback to listen for LinkProperties changes
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onLinkPropertiesChanged(@NonNull Network network, @NonNull LinkProperties linkProperties) {
                    super.onLinkPropertiesChanged(network, linkProperties);
                    update(linkProperties, activity, context);
                }
            };

            // Register the callback to listen for changes in the network's properties
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback);

            // Add an observer to remove the network callback when the lifecycleOwner is
            // destroyed
            lifecycleOwner.getLifecycle().addObserver(new NetworkConnectivityObserver());
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
    }

    /**
     * A LifecycleObserver to handle the cleanup of the network callback when
     * the parent lifecycle is destroyed.
     */
    private class NetworkConnectivityObserver implements DefaultLifecycleObserver {
        @Override
        public void onDestroy(@NonNull LifecycleOwner owner) {
            try {
                if (connectivityManager != null && networkCallback != null) {
                    connectivityManager.unregisterNetworkCallback(networkCallback);
                    networkCallback = null; // Clear reference
                }
            } catch (Exception e) {
                sentryManager.captureException(e);
            } finally {
                // Ensure cleanup even if exception occurs
                connectivityManager = null;
                networkCallback = null;
            }
        }
    }

    /**
     * Updates the UI (e.g., an ImageView) to reflect the current network status.
     * If Private DNS is active, we attempt further checks to see if NextDNS is
     * being used.
     *
     * @param linkProperties The current network's LinkProperties, or null if none.
     * @param activity       The Activity containing the ImageView to update.
     * @param context        The Context used for resource access.
     */
    public void update(@Nullable LinkProperties linkProperties, AppCompatActivity activity, Context context) {
        try {
            if (linkProperties == null) {
                // No valid network. Show a 'failure' icon in red
                setConnectionStatus(
                        activity.findViewById(R.id.connectionStatus),
                        R.drawable.failure,
                        R.color.red,
                        context);
                // Perform a more detailed check for NextDNS usage
                checkInheritedDNS(context, activity);
                return;
            }

            // Find the status icon View
            ImageView connectionStatus = activity.findViewById(R.id.connectionStatus);

            // Determine which icon/color to show based on Private DNS activity
            int statusDrawable = linkProperties.isPrivateDnsActive() ? R.drawable.success : R.drawable.failure;
            int statusColor = linkProperties.isPrivateDnsActive()
                    ? (linkProperties.getPrivateDnsServerName() != null
                            && linkProperties.getPrivateDnsServerName().contains("nextdns")
                                    ? R.color.green
                                    : R.color.yellow)
                    : R.color.red;

            // Update the image resource and color filter
            setConnectionStatus(connectionStatus, statusDrawable, statusColor, context);

            // Perform a more detailed check for NextDNS usage
            checkInheritedDNS(context, activity);

        } catch (Exception e) {
            // Capture any unexpected errors in Sentry
            sentryManager.captureException(e);
        }
    }

    /**
     * Makes a request to "https://test.nextdns.io" to confirm if the device is
     * actually using NextDNS. If the response indicates NextDNS usage with a
     * secure protocol, we color the icon green, otherwise orange or failure icon.
     *
     * @param context  The Context used for resources and string lookups.
     * @param activity The Activity whose ImageView we update to reflect status.
     */
    public void checkInheritedDNS(Context context, AppCompatActivity activity) {
        // Build a request to NextDNS test endpoint
        Request request = new Request.Builder()
                .url("https://test.nextdns.io")
                .header("Accept", "application/json")
                .header("Cache-Control", "no-cache")
                .build();

        // Execute the request asynchronously
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    // If the response is not successful, capture the failure
                    if (!response.isSuccessful()) {
                        sentryManager.captureMessage("Response was not successful.");
                        response.close();
                        return;
                    }
                    if (response.body() == null) {
                        sentryManager.captureMessage("Response body is null.");
                        response.close();
                        return;
                    }
                    // Parse the JSON response
                    JsonObject testResponse = JsonParser.parseString(response.body().string().trim())
                            .getAsJsonObject();

                    // Obtain relevant string keys from resources
                    String nextDnsStatusKey = context.getString(R.string.nextdns_status);
                    String nextDnsProtocolKey = context.getString(R.string.nextdns_protocol);
                    String usingNextDnsStatusValue = context.getString(R.string.using_nextdns_status);
                    String[] secureProtocols = context.getResources().getStringArray(R.array.secure_protocols);

                    // Check if the device is using NextDNS
                    String nextDNSStatus = testResponse.getAsJsonPrimitive(nextDnsStatusKey).getAsString();
                    if (!usingNextDnsStatusValue.equalsIgnoreCase(nextDNSStatus)) {
                        response.close();
                        return;
                    }

                    // Determine which protocol is used and if it is deemed secure
                    String nextdnsProtocol = testResponse.getAsJsonPrimitive(nextDnsProtocolKey).getAsString();
                    boolean isSecure = Arrays.asList(secureProtocols).contains(nextdnsProtocol);

                    // Update the icon with either green for secure, or failure/orange otherwise
                    ImageView connectionStatus = activity.findViewById(R.id.connectionStatus);
                    if (connectionStatus != null) {
                        connectionStatus.setImageResource(isSecure ? R.drawable.success : R.drawable.failure);
                        connectionStatus.setColorFilter(
                                ContextCompat.getColor(context, isSecure ? R.color.green : R.color.orange));
                    }
                    response.close();
                } catch (Exception e) {
                    // Capture network or parsing related errors in Sentry
                    catchNetworkErrors(e);
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Handle network failures by capturing the exception in Sentry
                catchNetworkErrors(e);
            }
        });
    }

    /**
     * Helper method to update the connection status icon and its color.
     * Uses optimized image loading for better performance and memory management.
     *
     * @param connectionStatus The ImageView to update.
     * @param drawableResId    The drawable resource (icon) to set.
     * @param colorResId       The color resource used to tint the icon.
     * @param context          The Context to retrieve the color resource.
     */
    private void setConnectionStatus(ImageView connectionStatus,
            int drawableResId,
            int colorResId,
            Context context) {
        // Use optimized image loading
        com.doubleangels.nextdnsmanagement.utils.ImageLoader.loadDrawable(context, connectionStatus, drawableResId);
        connectionStatus.setColorFilter(ContextCompat.getColor(context, colorResId));
    }

    /**
     * Handles various network-related exceptions, capturing them in Sentry.
     *
     * @param e The caught exception.
     */
    private void catchNetworkErrors(@NonNull Exception e) {
        sentryManager.captureException(e);
    }
}
