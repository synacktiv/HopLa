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
import java.util.*;

import static com.hopla.Constants.DEBUG_AI;

public class GeminiProvider extends AIProvider {

    public GeminiProvider(LLMConfig config, LLMConfig.Provider providerConfig) {
        super(AIProviderType.GEMINI, AIProviderType.GEMINI.toString(), config, providerConfig);
    }


    // No fim on gemini
    @Override
    public List<String> autoCompletion(Completer.CaretContext caretContext) throws IOException {
        return new ArrayList<>();
    }

    @Override
    public void instruct(String prompt, StreamingCallback callback) throws IOException {
        GeminiRequest geminiRequest = new GeminiRequest(new ArrayList<>());


        if (!providerConfig.quick_action_system_prompt.isEmpty()) {
            geminiRequest.contents.add(
                    new GeminiRequest.Content(
                            AIChats.MessageRole.SYSTEM.toString(),
                            Collections.singletonList(new GeminiRequest.Part(providerConfig.chat_system_prompt))
                    )
            );
        }

        geminiRequest.contents.add(
                new GeminiRequest.Content(
                        AIChats.MessageRole.USER.toString(),
                        Collections.singletonList(new GeminiRequest.Part(prompt))
                )
        );

        String jsonString = gson.toJson(geminiRequest);
        RequestBody body = RequestBody.create(jsonString, JSON);

        Request.Builder builder = new Request.Builder().url(providerConfig.quick_action_endpoint.replace("@model", providerConfig.quick_action_model).replace("@key", providerConfig.api_key));

        for (Map.Entry<String, Object> entry : providerConfig.headers.entrySet()) {
            builder.addHeader(entry.getKey(), String.valueOf(entry.getValue()));
        }

        Request request = builder.post(body).build();

        currentChatcall = client.newCall(request);

        sendStreamingRequest(currentQuickActionCall, callback);
    }

    @Override
    public void chat(AIChats.Chat chat, StreamingCallback callback) throws IOException {

        GeminiRequest geminiRequest = new GeminiRequest(new ArrayList<>());


        if (!providerConfig.chat_system_prompt.isEmpty()) {
            geminiRequest.contents.add(
                    new GeminiRequest.Content(
                            AIChats.MessageRole.SYSTEM.toString(),
                            Collections.singletonList(new GeminiRequest.Part(providerConfig.chat_system_prompt))
                    )
            );
        }

        for (AIChats.Message message : chat.getMessages().subList(0, chat.getMessages().size() - 1)) {
            geminiRequest.contents.add(
                    new GeminiRequest.Content(
                            AIChats.MessageRole.USER.toString(),
                            Collections.singletonList(new GeminiRequest.Part(message.getContent()))
                    )
            );
        }

        String jsonString = gson.toJson(geminiRequest);
        RequestBody body = RequestBody.create(jsonString, JSON);

        Request.Builder builder = new Request.Builder().url(providerConfig.chat_endpoint.replace("@model", providerConfig.chat_model).replace("@key", providerConfig.api_key));

        for (Map.Entry<String, Object> entry : providerConfig.headers.entrySet()) {
            builder.addHeader(entry.getKey(), String.valueOf(entry.getValue()));
        }

        Request request = builder.post(body).build();
        if (DEBUG_AI) {
            HopLa.montoyaApi.logging().logToOutput("AI chat request: " + jsonString);
        }

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

                    if (!line.startsWith("data: ")) continue;

                    String jsonLine = line.substring("data: ".length());
                    if (jsonLine.isBlank()) continue;

                    JsonObject responseJson = gson.fromJson(jsonLine, JsonObject.class);
                    JsonArray candidates = responseJson.getAsJsonArray("candidates");
                    if (candidates != null && !candidates.isEmpty()) {
                        JsonObject content = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
                        JsonArray parts = content.getAsJsonArray("parts");
                        if (parts != null && !parts.isEmpty()) {
                            String text = parts.get(0).getAsJsonObject().get("text").getAsString();
                            if (DEBUG_AI) {
                                HopLa.montoyaApi.logging().logToOutput("AI chat streaming response: " + text);
                            }
                            callback.onData(text);
                        }
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

    static class GeminiRequest {
        List<GeminiRequest.Content> contents;

        GeminiRequest(List<GeminiRequest.Content> contents) {
            this.contents = contents;
        }

        static class Content {
            String role;
            List<GeminiRequest.Part> parts;

            Content(String role, List<GeminiRequest.Part> parts) {
                this.role = role;
                this.parts = parts;
            }
        }


        static class Part {
            String text;

            Part(String text) {
                this.text = text;
            }
        }
    }
}
