package com.hopla.IA;


import com.google.gson.Gson;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.hopla.Utils.alert;
import static com.hopla.Utils.success;

public abstract class AIProvider {
    protected static Gson gson = new Gson();
    protected String providerName;
    protected UserAiConfiguration config;
    protected OkHttpClient client;
    MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final long COOLDOWN_PERIOD_MS = 2000;
    private final AtomicLong lastInvocationTime = new AtomicLong(0);


    public AIProvider(String name, UserAiConfiguration config) {
        this.providerName = name;
        this.config = config;
        this.buildClient();
    }

    private void buildClient() {
        if (config.proxyType == Proxy.Type.DIRECT) {
            client = new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(120, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS).build();
        } else {
            client = new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(120, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .proxy(new Proxy(config.proxyType, new InetSocketAddress(config.proxyUrl, config.proxyPort)))
                    .build();
        }
    }
    public Boolean rateLimit(){
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastInvocationTime.get() > COOLDOWN_PERIOD_MS) {
            lastInvocationTime.set(currentTime);
            return false;
        } else {
            return true;
        }
    }
    public void testCommunication() {
        try {
            this.chat(false, "Hello World!", e -> {
            });
            success("Setup OK !");
        } catch (Exception e) {
            alert("AI Invalid configuration: " + e.getMessage());
        }

    }

    public abstract List<String> autoCompletion(String prompt) throws IOException;

    public abstract void chat(Boolean systemPrompt, String message, Callback callback) throws IOException;

    public interface Callback {
        void onResponse(String response);
    }
}