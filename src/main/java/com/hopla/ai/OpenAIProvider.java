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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.hopla.Constants.DEBUG_AI;

public class OpenAIProvider extends AIProvider {
    public OpenAIProvider(LLMConfig config, LLMConfig.Provider providerConfig) {
        super(AIProviderType.OPENAI, AIProviderType.OPENAI.toString(), config, providerConfig);
    }


    // No fim on openai
    @Override
    public List<String> autoCompletion(Completer.CaretContext caretContext) throws IOException {
        return new ArrayList<>();
    }

    @Override
    public void instruct(String prompt, StreamingCallback callback) throws IOException {

        if (providerConfig.quick_action_model == null || providerConfig.quick_action_model.isEmpty()) {
            throw new IOException("OpenAI model undefined");
        }

        JsonArray messages = new JsonArray();

        if (!providerConfig.quick_action_system_prompt.isEmpty()) {
            JsonObject userMessage = new JsonObject();
            userMessage.addProperty("role", AIChats.MessageRole.SYSTEM.toString());
            userMessage.addProperty("content", providerConfig.chat_system_prompt);
            messages.add(userMessage);
        }

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", AIChats.MessageRole.USER.toString());
        userMessage.addProperty("content", prompt);
        messages.add(userMessage);

        JsonObject jsonPayload = new JsonObject();
        jsonPayload.addProperty("model", providerConfig.quick_action_model);
        jsonPayload.add("messages", messages);
        jsonPayload.addProperty("stream", true);

        String jsonString = gson.toJson(jsonPayload);
        RequestBody body = RequestBody.create(jsonString, JSON);
        Request.Builder builder = new Request.Builder().url(providerConfig.quick_action_endpoint);

        for (Map.Entry<String, Object> entry : providerConfig.headers.entrySet()) {
            builder.addHeader(entry.getKey(), String.valueOf(entry.getValue()));
        }

        Request request = builder.post(body).build();

        currentChatcall = client.newCall(request);

        sendStreamingRequest(currentQuickActionCall, callback);
    }

    @Override
    public void chat(AIChats.Chat chat, StreamingCallback callback) throws IOException {
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

        String jsonString = gson.toJson(jsonPayload);
        RequestBody body = RequestBody.create(jsonString, JSON);

        Request.Builder builder = new Request.Builder().url(providerConfig.chat_endpoint);

        if (DEBUG_AI) {
            HopLa.montoyaApi.logging().logToOutput("AI chat request: " + jsonString);
        }

        for (Map.Entry<String, Object> entry : providerConfig.headers.entrySet()) {
            builder.addHeader(entry.getKey(), String.valueOf(entry.getValue()));
        }

        Request request = builder.post(body).build();

        currentChatcall = client.newCall(request);

        sendStreamingRequest(currentChatcall, callback);

    }

    private void sendStreamingRequest(Call call, StreamingCallback callback) {
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

                    JsonArray choices = responseJson.getAsJsonArray("choices");

                    if (choices != null && !choices.isEmpty()) {
                        String content = choices.get(0).getAsJsonObject().get("delta").getAsJsonObject().get("content").getAsString();
                        if (DEBUG_AI) {
                            HopLa.montoyaApi.logging().logToOutput("AI chat streaming response: " + content);
                        }
                        callback.onData(content);
                    }
                }
                callback.onDone();
            } catch (IOException ex) {
                callback.onError("Cancelled or error : " + ex.getMessage());
            } catch (Exception ex) {
                callback.onError("AI chat error : " + ex.getMessage());
            }
        }).start();
    }

}
