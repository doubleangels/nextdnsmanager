package com.doubleangels.nextdnsmanagement.protocol;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.Looper;
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
import com.doubleangels.nextdnsmanagement.network.HttpClients;
import com.doubleangels.nextdnsmanagement.sentry.SentryManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

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

    private final SentryManager sentryManager;
    private final OkHttpClient httpClient;

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private volatile Call activeDnsCheckCall;
    private final Handler dnsCheckHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingDnsCheck;
    private static final long DNS_CHECK_DEBOUNCE_MS = 300L;

    public VisualIndicator(Context context) {
        this.sentryManager = new SentryManager(context);
        this.httpClient = HttpClients.getDnsCheckClient();
    }

    public void initialize(Context context, LifecycleOwner lifecycleOwner, AppCompatActivity activity) {
        try {
            connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager == null) {
                sentryManager.captureMessage("ConnectivityManager is null.");
                return;
            }

            NetworkRequest networkRequest = new NetworkRequest.Builder().build();

            Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                sentryManager.captureMessage("No active network found.");
                update(null, activity, context);
            } else {
                LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
                update(linkProperties, activity, context);
            }

            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onLinkPropertiesChanged(@NonNull Network network, @NonNull LinkProperties linkProperties) {
                    super.onLinkPropertiesChanged(network, linkProperties);
                    if (connectivityManager == null) {
                        return;
                    }
                    update(linkProperties, activity, context);
                }

                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    ConnectivityManager manager = connectivityManager;
                    if (manager == null) {
                        return;
                    }
                    LinkProperties linkProperties = manager.getLinkProperties(network);
                    update(linkProperties, activity, context);
                }

                @Override
                public void onLost(@NonNull Network network) {
                    super.onLost(network);
                    update(null, activity, context);
                }
            };

            connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
            lifecycleOwner.getLifecycle().addObserver(new NetworkConnectivityObserver());
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
    }

    private class NetworkConnectivityObserver implements DefaultLifecycleObserver {
        @Override
        public void onDestroy(@NonNull LifecycleOwner owner) {
            try {
                if (pendingDnsCheck != null) {
                    dnsCheckHandler.removeCallbacks(pendingDnsCheck);
                    pendingDnsCheck = null;
                }
                if (activeDnsCheckCall != null) {
                    activeDnsCheckCall.cancel();
                    activeDnsCheckCall = null;
                }
                if (connectivityManager != null && networkCallback != null) {
                    connectivityManager.unregisterNetworkCallback(networkCallback);
                    networkCallback = null;
                }
            } catch (Exception e) {
                sentryManager.captureException(e);
            } finally {
                networkCallback = null;
            }
        }
    }

    public void update(@Nullable LinkProperties linkProperties, AppCompatActivity activity, Context context) {
        runOnUiThreadIfAlive(activity, () -> {
            try {
                if (linkProperties == null) {
                    setConnectionStatus(
                            activity.findViewById(R.id.connectionStatus),
                            R.drawable.failure,
                            R.color.red,
                            context);
                    checkInheritedDNS(context, activity);
                    return;
                }

                ImageView connectionStatus = activity.findViewById(R.id.connectionStatus);

                int statusDrawable = linkProperties.isPrivateDnsActive() ? R.drawable.success : R.drawable.failure;
                int statusColor = linkProperties.isPrivateDnsActive()
                        ? (linkProperties.getPrivateDnsServerName() != null
                                && linkProperties.getPrivateDnsServerName().contains("nextdns")
                                        ? R.color.green
                                        : R.color.yellow)
                        : R.color.red;

                setConnectionStatus(connectionStatus, statusDrawable, statusColor, context);
                checkInheritedDNS(context, activity);
            } catch (Exception e) {
                sentryManager.captureException(e);
            }
        });
    }

    public void checkInheritedDNS(Context context, AppCompatActivity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        if (pendingDnsCheck != null) {
            dnsCheckHandler.removeCallbacks(pendingDnsCheck);
        }

        pendingDnsCheck = () -> performInheritedDnsCheck(context, activity);
        dnsCheckHandler.postDelayed(pendingDnsCheck, DNS_CHECK_DEBOUNCE_MS);
    }

    private void performInheritedDnsCheck(Context context, AppCompatActivity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        Request request = new Request.Builder()
                .url("https://test.nextdns.io")
                .header("Accept", "application/json")
                .header("Cache-Control", "no-cache")
                .build();

        if (activeDnsCheckCall != null) {
            activeDnsCheckCall.cancel();
        }

        Call call = httpClient.newCall(request);
        activeDnsCheckCall = call;
        call.enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
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

                    JsonObject testResponse = JsonParser.parseString(response.body().string().trim())
                            .getAsJsonObject();

                    String nextDnsStatusKey = context.getString(R.string.nextdns_status);
                    String nextDnsProtocolKey = context.getString(R.string.nextdns_protocol);
                    String usingNextDnsStatusValue = context.getString(R.string.using_nextdns_status);
                    String[] secureProtocols = context.getResources().getStringArray(R.array.secure_protocols);

                    String nextDNSStatus = testResponse.getAsJsonPrimitive(nextDnsStatusKey).getAsString();
                    if (!usingNextDnsStatusValue.equalsIgnoreCase(nextDNSStatus)) {
                        response.close();
                        return;
                    }

                    String nextdnsProtocol = testResponse.getAsJsonPrimitive(nextDnsProtocolKey).getAsString();
                    boolean isSecure = false;
                    for (String secureProtocol : secureProtocols) {
                        if (secureProtocol.equals(nextdnsProtocol)) {
                            isSecure = true;
                            break;
                        }
                    }

                    final boolean secureResult = isSecure;
                    runOnUiThreadIfAlive(activity, () -> {
                        ImageView connectionStatus = activity.findViewById(R.id.connectionStatus);
                        if (connectionStatus != null) {
                            connectionStatus.setImageResource(secureResult ? R.drawable.success : R.drawable.failure);
                            connectionStatus.setColorFilter(
                                    ContextCompat.getColor(context, secureResult ? R.color.green : R.color.orange));
                        }
                    });
                    response.close();
                } catch (Exception e) {
                    catchNetworkErrors(e);
                } finally {
                    if (activeDnsCheckCall == call) {
                        activeDnsCheckCall = null;
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                catchNetworkErrors(e);
                if (activeDnsCheckCall == call) {
                    activeDnsCheckCall = null;
                }
            }
        });
    }

    private void setConnectionStatus(ImageView connectionStatus,
            int drawableResId,
            int colorResId,
            Context context) {
        if (connectionStatus == null) {
            return;
        }
        com.doubleangels.nextdnsmanagement.utils.ImageLoader.loadDrawable(context, connectionStatus, drawableResId);
        connectionStatus.setColorFilter(ContextCompat.getColor(context, colorResId));
    }

    private void runOnUiThreadIfAlive(AppCompatActivity activity, Runnable action) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        activity.runOnUiThread(() -> {
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                action.run();
            }
        });
    }

    private void catchNetworkErrors(@NonNull Exception e) {
        if (SentryManager.isIgnored(e)) {
            return;
        }
        sentryManager.captureException(e);
    }
}
