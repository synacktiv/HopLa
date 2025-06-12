package com.hopla.IA;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hopla.Constants;
import com.hopla.HopLa;
import okhttp3.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OllamaProvider extends AIProvider {
    private String BASE_PROMPT = "You are an expert in HTTP APIs. Based on the following partial HTTP request, autocomplete without explaination. Do not use Markdown, code fences, or formatting like.\n ";

    public OllamaProvider(UserAiConfiguration config) {
        super(AIProviderType.OLLAMA.toString(), config);
    }

    @Override
    public List<String> autoCompletion(String prompt) throws IOException {

        //MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        List<String> completionParts = new ArrayList<>();
        if (config.model == null || config.model.isEmpty()) {
            throw new IOException("Ollama model undefined");
        }

        if (this.rateLimit()) {
            return completionParts;
        }

        JsonObject jsonPayload = new JsonObject();
        jsonPayload.addProperty("model", config.model);
        jsonPayload.addProperty("prompt", BASE_PROMPT + "<|fim_begin|>"+ prompt + "<|fim_hole|><|fim_end|>"); //BASE_PROMPT +
                jsonPayload.addProperty("stream", false);
        jsonPayload.addProperty("temperature", 0.0);
        jsonPayload.addProperty("top_p", 0.95);
        jsonPayload.addProperty("num_predict", 50);
        JsonArray stopArray = new JsonArray();
        stopArray.add("\n\n");
        stopArray.add("\r\n\r\n");
        stopArray.add("HTTP/");
        stopArray.add("<|fim_begin|>");
        stopArray.add("<|fim_hole|>");
        stopArray.add("<|fim_end|>");

        jsonPayload.add("stop", stopArray);
        String jsonString = gson.toJson(jsonPayload);

        RequestBody body = RequestBody.create(
                jsonString,
                JSON
        );

        Request request = new Request.Builder()
                .url(removeTrailingSlash(config.apiUrl) + "/api/generate")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Error : " + response);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    Objects.requireNonNull(response.body()).byteStream(), StandardCharsets.UTF_8))
            ) {
                String line;
                while ((line = reader.readLine()) != null) {
                    JsonObject lineJson = gson.fromJson(line, JsonObject.class);

                    String responsePart = lineJson.get("response").getAsString();
                    completionParts.add(responsePart);

                    if (lineJson.get("done").getAsBoolean()) {
                        break;
                    }
                }
            }
        }
        if (Constants.DEBUG) {
            HopLa.montoyaApi.logging().logToOutput("IA suggestion: " + completionParts);
        }
        return completionParts;
    }

    public static String removeTrailingSlash(String url) {
        if (url != null && url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
    @Override
    public void chat(Boolean systemPrompt, String message, Callback callback) throws IOException {
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", message);

        JsonArray messages = new JsonArray();
        messages.add(userMessage);

        JsonObject jsonPayload = new JsonObject();
        jsonPayload.addProperty("model", config.model);
        jsonPayload.add("messages", messages);
        jsonPayload.addProperty("stream", false);

        String jsonString = gson.toJson(jsonPayload);
        RequestBody body = RequestBody.create(jsonString, JSON);
        Request request = new Request.Builder().url(removeTrailingSlash(config.apiUrl) + "/api/chat").post(body).build();

        if (Constants.DEBUG) {
            HopLa.montoyaApi.logging().logToOutput("IA chat request: " + jsonString);
        }

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onResponse("Network error : " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onResponse("API error : " + response.code() + "\n" + Objects.requireNonNull(response.body()).string());
                    return;
                }

                String responseBody = Objects.requireNonNull(response.body()).string();
                if (Constants.DEBUG) {
                    HopLa.montoyaApi.logging().logToOutput("IA chat response: " + responseBody);
                }

                JsonObject responseJson = gson.fromJson(responseBody, JsonObject.class);



                JsonObject messageObject = responseJson.getAsJsonObject("message");
                callback.onResponse(messageObject.get("content").getAsString());
            }
        });
    }
}