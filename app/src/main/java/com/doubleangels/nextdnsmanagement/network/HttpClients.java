package com.doubleangels.nextdnsmanagement.network;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;

/**
 * Shared OkHttp clients for the application.
 */
public final class HttpClients {

    private static final long TIMEOUT_SECONDS = 10L;
    private static volatile OkHttpClient dnsCheckClient;

    private HttpClients() {
    }

    public static OkHttpClient getDnsCheckClient() {
        if (dnsCheckClient == null) {
            synchronized (HttpClients.class) {
                if (dnsCheckClient == null) {
                    dnsCheckClient = new OkHttpClient.Builder()
                            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                            .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                            .addInterceptor(proxySafeInterceptor())
                            .build();
                }
            }
        }
        return dnsCheckClient;
    }

    private static Interceptor proxySafeInterceptor() {
        return chain -> {
            try {
                return chain.proceed(chain.request());
            } catch (IllegalArgumentException e) {
                throw new IOException("Proxy setup failed due to IllegalArgumentException", e);
            }
        };
    }
}
