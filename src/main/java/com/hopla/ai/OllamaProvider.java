package com.hopla.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hopla.Completer;
import com.hopla.HopLa;
import okhttp3.Call;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.hopla.Constants.DEBUG_AI;
import static com.hopla.Utils.mapToJson;

public class OllamaProvider extends AIProvider {

    public OllamaProvider(LLMConfig config, LLMConfig.Provider providerConfig) {
        super(AIProviderType.OLLAMA, AIProviderType.OLLAMA.toString(), config, providerConfig);
    }

    @Override
    public void instruct(String prompt, StreamingCallback callback) throws IOException {

        if (providerConfig.quick_action_model == null || providerConfig.quick_action_model.isEmpty()) {
            throw new IOException("Ollama model undefined");
        }

        JsonObject jsonPayload = new JsonObject();
        jsonPayload.addProperty("model", providerConfig.quick_action_model);
        jsonPayload.addProperty("prompt", prompt);
        jsonPayload.addProperty("stream", true);
        jsonPayload.addProperty("keep_alive", "60m");


        if (!providerConfig.quick_action_system_prompt.isEmpty()) {
            jsonPayload.addProperty("system", providerConfig.quick_action_system_prompt);
        }
        if (!providerConfig.quick_action_params.isEmpty()) {
            jsonPayload.add("options", mapToJson(providerConfig.quick_action_params));
        }

        if (!providerConfig.quick_action_stops.isEmpty()) {
            JsonArray stopArray = new JsonArray();
            providerConfig.quick_action_stops.forEach(stopArray::add);
            if (jsonPayload.has("options")) {
                jsonPayload.get("options").getAsJsonObject().add("stop", stopArray);
            } else {
                JsonObject obj = new JsonObject();
                obj.add("stop", stopArray);
                jsonPayload.add("options", obj);
            }
        }
        String jsonString = gson.toJson(jsonPayload);

        if (DEBUG_AI) {
            HopLa.montoyaApi.logging().logToOutput("quick action request: " + jsonString);
        }

        RequestBody body = RequestBody.create(
                jsonString,
                JSON
        );

        Request.Builder builder = new Request.Builder().url(providerConfig.quick_action_endpoint);

        for (Map.Entry<String, Object> entry : providerConfig.headers.entrySet()) {
            builder.addHeader(entry.getKey(), String.valueOf(entry.getValue()));
        }

        Request request = builder.post(body).build();

        currentQuickActionCall = client.newCall(request);
        sendStreamingRequest(currentQuickActionCall, callback, false);
    }

    @Override
    public List<String> autoCompletion(Completer.CaretContext caretContext) throws IOException {

        List<String> completionParts = new ArrayList<>();
        if (providerConfig.completion_model == null || providerConfig.completion_model.isEmpty()) {
            throw new IOException("Ollama model undefined");
        }

        JsonObject jsonPayload = new JsonObject();
        jsonPayload.addProperty("model", providerConfig.completion_model);
        jsonPayload.addProperty("prompt", promptReplace(caretContext, providerConfig.completion_prompt));

        if (DEBUG_AI) {
            HopLa.montoyaApi.logging().logToOutput("Suggestion prompt: " + promptReplace(caretContext, providerConfig.completion_prompt));
        }


        jsonPayload.addProperty("stream", false);
        jsonPayload.addProperty("raw", true);
        jsonPayload.addProperty("keep_alive", "60m");


        if (!providerConfig.completion_system_prompt.isEmpty()) {
            jsonPayload.addProperty("system", promptReplace(caretContext, providerConfig.completion_system_prompt));
        }
        if (!providerConfig.completion_params.isEmpty()) {
            jsonPayload.add("options", mapToJson(providerConfig.completion_params));
        }

        if (!providerConfig.completion_stops.isEmpty()) {
            JsonArray stopArray = new JsonArray();
            providerConfig.completion_stops.forEach(stopArray::add);
            if (jsonPayload.has("options")) {
                jsonPayload.get("options").getAsJsonObject().add("stop", stopArray);
            } else {
                JsonObject obj = new JsonObject();
                obj.add("stop", stopArray);
                jsonPayload.add("options", obj);
            }
        }

        String jsonString = gson.toJson(jsonPayload);

        if (DEBUG_AI) {
            HopLa.montoyaApi.logging().logToOutput("Suggestion request: " + jsonString);
        }

        RequestBody body = RequestBody.create(
                jsonString,
                JSON
        );


        Request.Builder builder = new Request.Builder().url(providerConfig.completion_endpoint);

        for (Map.Entry<String, Object> entry : providerConfig.headers.entrySet()) {
            builder.addHeader(entry.getKey(), String.valueOf(entry.getValue()));
        }

        Request request = builder.post(body).build();

        if (currentQuickActionCall != null) {
            currentCompletionCall.cancel();
        }
        currentCompletionCall = client.newCall(request);

        try (Response response = currentCompletionCall.execute()) {
            if (!response.isSuccessful()) {
                throw new IOException(response.code() + "\n" + Objects.requireNonNull(response.body()).string());
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    Objects.requireNonNull(response.body()).byteStream(), StandardCharsets.UTF_8))
            ) {
                String line;
                while ((line = reader.readLine()) != null) {
                    JsonObject lineJson = gson.fromJson(line, JsonObject.class);
                    String responsePart = lineJson.get("response").getAsString();
                    if (responsePart.contains(" ")) {
                        completionParts.add(responsePart.split(" ")[0]);
                    }
                    completionParts.add(responsePart);

                    if (lineJson.get("done").getAsBoolean()) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            throw new IOException("Error : " + e.getMessage());
        }

        if (DEBUG_AI) {
            HopLa.montoyaApi.logging().logToOutput("AI suggestion: " + completionParts);
        }
        return completionParts;
    }

    @Override
    public void chat(AIChats.Chat chat, StreamingCallback callback) {
        JsonArray messages = new JsonArray();

        if (!providerConfig.chat_system_prompt.isEmpty()) {
            JsonObject userMessage = new JsonObject();
            userMessage.addProperty("role", AIChats.MessageRole.SYSTEM.toString());
            userMessage.addProperty("content", providerConfig.chat_system_prompt);
            messages.add(userMessage);
        }

        for (AIChats.Message message : chat.getMessages()) {
            JsonObject userMessage = new JsonObject();
            userMessage.addProperty("role", message.getRole().toString().toLowerCase());
            userMessage.addProperty("content", message.getContent());
            messages.add(userMessage);
        }

        JsonObject jsonPayload = new JsonObject();
        jsonPayload.addProperty("model", providerConfig.chat_model);
        jsonPayload.add("messages", messages);
        jsonPayload.addProperty("stream", true);
        jsonPayload.addProperty("keep_alive", "60m");

        if (!providerConfig.chat_params.isEmpty()) {
            jsonPayload.add("options", mapToJson(providerConfig.chat_params));
        }

        if (!providerConfig.chat_stops.isEmpty()) {
            JsonArray stopArray = new JsonArray();
            providerConfig.chat_stops.forEach(stopArray::add);
            if (jsonPayload.has("options")) {
                jsonPayload.get("options").getAsJsonObject().add("stop", stopArray);
            } else {
                JsonObject obj = new JsonObject();
                obj.add("stop", stopArray);
                jsonPayload.add("options", obj);
            }
        }

        String jsonString = gson.toJson(jsonPayload);
        RequestBody body = RequestBody.create(jsonString, JSON);

        Request.Builder builder = new Request.Builder().url(providerConfig.chat_endpoint);

        for (Map.Entry<String, Object> entry : providerConfig.headers.entrySet()) {
            builder.addHeader(entry.getKey(), String.valueOf(entry.getValue()));
        }

        Request request = builder.post(body).build();

        if (DEBUG_AI) {
            HopLa.montoyaApi.logging().logToOutput("AI chat request: " + jsonString);
        }

        currentChatcall = client.newCall(request);

        sendStreamingRequest(currentChatcall, callback, true);

    }

    private void sendStreamingRequest(Call call, StreamingCallback callback, Boolean isChat) {
        new Thread(() -> {
            try (Response response = call.execute()) {
                if (!response.isSuccessful()) {
                    callback.onError("AI API error : " + response.code() + "\n" + Objects.requireNonNull(response.body()).string());
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (Thread.currentThread().isInterrupted()) break;

                    JsonObject responseJson = gson.fromJson(line, JsonObject.class);

                    if (isChat) {
                        JsonObject messageObject = responseJson.getAsJsonObject("message");
                        if (DEBUG_AI) {
                            HopLa.montoyaApi.logging().logToOutput("AI streaming response: " + messageObject.get("content").getAsString());
                        }
                        callback.onData(messageObject.get("content").getAsString());
                    } else {
                        if (DEBUG_AI) {
                            HopLa.montoyaApi.logging().logToOutput("AI streaming response: " + responseJson.get("response").getAsString());
                        }
                        callback.onData(responseJson.get("response").getAsString());
                    }

                }
                callback.onDone();
            } catch (IOException ex) {
                callback.onError("Cancelled or error : " + ex.getMessage());
            } catch (Exception ex) {
                callback.onError("AI streaming error : " + ex.getMessage());
            }
        }).start();
    }
}