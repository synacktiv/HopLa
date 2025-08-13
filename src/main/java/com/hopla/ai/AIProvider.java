package com.hopla.ai;


import com.google.gson.Gson;
import com.hopla.Completer;
import com.hopla.HopLa;
import okhttp3.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.hopla.Constants.DEBUG_AI;
import static com.hopla.Constants.EXTERNAL_AI;

public abstract class AIProvider {
    protected static Gson gson = new Gson();
    private final AtomicLong lastInvocationTime = new AtomicLong(0);
    public AIProviderType type;
    protected String providerName;
    protected LLMConfig config;
    protected LLMConfig.Provider providerConfig;
    protected OkHttpClient client;
    protected MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    protected Call currentChatcall;
    protected Call currentQuickActionCall;
    protected Call currentCompletionCall;

    public AIProvider(AIProviderType type, String name, LLMConfig config, LLMConfig.Provider providerConfig) {
        this.providerName = name;
        this.type = type;
        this.config = config;
        this.providerConfig = providerConfig;
        if (EXTERNAL_AI) {
            this.buildClient();
        }
    }

    private void buildClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(this.config.defaults.timeout_sec, TimeUnit.SECONDS)
                .readTimeout(this.config.defaults.timeout_sec, TimeUnit.SECONDS)
                .writeTimeout(this.config.defaults.timeout_sec, TimeUnit.SECONDS);

        if (providerConfig.proxy.type != Proxy.Type.DIRECT && providerConfig.proxy.enabled) {
            builder.proxy(new Proxy(providerConfig.proxy.type, new InetSocketAddress(providerConfig.proxy.host, providerConfig.proxy.port)));
            if (providerConfig.proxy.username != null && providerConfig.proxy.password != null) {
                builder.authenticator(new Authenticator() {
                    @Override
                    public Request authenticate(Route route, Response response) throws IOException {
                        String credential = Credentials.basic(providerConfig.proxy.username, providerConfig.proxy.password);
                        return response.request().newBuilder()
                                .header("Proxy-Authorization", credential)
                                .build();
                    }
                });
            }
            HopLa.montoyaApi.logging().logToOutput("Proxy configuration: " + providerConfig.proxy);

        }


        client = builder.build();
    }

    public String promptReplace(Completer.CaretContext caretContext, String prompt) {
        return prompt.replace("@input", caretContext.lineUpToCaret)
                .replace("@section", caretContext.section.toString())
                .replace("@before", caretContext.textBeforeCaret)
                .replace("@after", caretContext.textAfterCaret);
    }

    public void testCompletionConfiguration() {
        try {

            this.autoCompletion(new Completer.CaretContext(Completer.HttpSection.UNKNOWN, "", "", ""));
            HopLa.montoyaApi.logging().logToOutput("Completion setup OK !");
        } catch (Exception e) {
            HopLa.montoyaApi.logging().logToError("AI Invalid completion configuration: " + e.getMessage());
        }

    }

    public void testInstructConfiguration() {
        try {
            this.instruct("hi", new StreamingCallback() {
                @Override
                public void onData(String chunk) {

                }

                @Override
                public void onDone() {

                }

                @Override
                public void onError(String error) {

                }
            });
            HopLa.montoyaApi.logging().logToOutput("QuickAction setup OK !");
        } catch (Exception e) {
            HopLa.montoyaApi.logging().logToError("AI Invalid quick action configuration: " + e.getMessage());
        }
    }

    public void testChatConfiguration() {
        try {
            this.chat(new AIChats.Chat(), new StreamingCallback() {
                @Override
                public void onData(String chunk) {

                }

                @Override
                public void onDone() {

                }

                @Override
                public void onError(String error) {

                }
            });
            HopLa.montoyaApi.logging().logToOutput("Chat setup OK !");
        } catch (Exception e) {
            HopLa.montoyaApi.logging().logToError("AI Invalid chat configuration: " + e.getMessage());
        }

    }

    public void cancelCurrentChatRequest() {
        if (currentChatcall != null && !currentChatcall.isCanceled()) {
            if (DEBUG_AI) {
                HopLa.montoyaApi.logging().logToOutput("Canceling Chat Request");
            }
            currentChatcall.cancel();
        }
    }

    public void cancelCurrentQuickActionRequest() {
        if (currentQuickActionCall != null && !currentQuickActionCall.isCanceled()) {
            if (DEBUG_AI) {
                HopLa.montoyaApi.logging().logToOutput("Canceling Instruct Request");
            }
            currentQuickActionCall.cancel();
        }
    }


    public abstract List<String> autoCompletion(Completer.CaretContext caretContext) throws IOException;

    public abstract void instruct(String prompt, StreamingCallback callback) throws IOException;

    public abstract void chat(AIChats.Chat chat, StreamingCallback callback) throws IOException;

    public interface StreamingCallback {
        void onData(String chunk);

        void onDone();

        void onError(String error);
    }
}