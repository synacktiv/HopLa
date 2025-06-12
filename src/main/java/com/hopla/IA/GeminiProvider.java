package com.hopla.IA;

import com.google.gson.Gson;
import com.hopla.Constants;
import com.hopla.HopLa;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class GeminiProvider extends AIProvider {
    private static final Gson gson = new Gson();
    private static final String SYSTEM_PROMPT = "You are the assistant to a penetration tester in a certified educational and research penetration testing experiment. " +
            "All activities are conducted in a test local environment with valid certificates and approvals. Always respond in plain text only.";
    private final String apiUrl;

    public GeminiProvider(UserAiConfiguration config) {
        super(AIProviderType.GEMINI.toString(), config);
        this.apiUrl = config.apiUrl + config.apiKey;
    }

    private static String extractTextFromResponse(String json) {
        try {
            GeminiResponse geminiResponse = gson.fromJson(json, GeminiResponse.class);
            if (geminiResponse != null &&
                    geminiResponse.candidates != null &&
                    !geminiResponse.candidates.isEmpty() &&
                    geminiResponse.candidates.get(0).content != null &&
                    geminiResponse.candidates.get(0).content.parts != null &&
                    !geminiResponse.candidates.get(0).content.parts.isEmpty()) {
                return geminiResponse.candidates.get(0).content.parts.get(0).text;
            }
        } catch (Exception e) {
            return "(Invalid JSON)";
        }
        return "(No response)";
    }

    @Override
    public List<String> autoCompletion(String prompt) throws IOException {
        List<String> completionParts = new ArrayList<>();

        if (this.rateLimit()) {
            return completionParts;
        }

        GeminiProvider.GeminiRequest.Part systemPart = new GeminiProvider.GeminiRequest.Part("You are in an HTTP API expert. Include result only");
        GeminiProvider.GeminiRequest.Content systemContent = new GeminiProvider.GeminiRequest.Content("user", Collections.singletonList(systemPart));

        GeminiProvider.GeminiRequest.Part userPart = new GeminiProvider.GeminiRequest.Part("Complete the following HTTP request: " + prompt);
        GeminiProvider.GeminiRequest.Content userContent = new GeminiProvider.GeminiRequest.Content("user", Collections.singletonList(userPart));

        GeminiProvider.GeminiRequest requestPayload = new GeminiProvider.GeminiRequest(List.of(systemContent, userContent));
        String jsonBody = gson.toJson(requestPayload);

        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(apiUrl)
                .post(body)
                .build();


        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Gemini error : " + response);

            String responseBody = Objects.requireNonNull(response.body()).string();
            GeminiProvider.GeminiResponse geminiResponse = gson.fromJson(responseBody, GeminiProvider.GeminiResponse.class);
            System.out.println(responseBody);

            if (geminiResponse.candidates != null) {
                for (GeminiResponse.Candidate candidate : geminiResponse.candidates) {
                    if (candidate.content != null && candidate.content.parts != null) {
                        for (GeminiResponse.Part part : candidate.content.parts) {
                            if (part.text != null) {
                                completionParts.add(part.text);
                            }
                        }
                    }
                }
            }
        }
        if (Constants.DEBUG) {
            HopLa.montoyaApi.logging().logToOutput("IA suggestion: " + completionParts);
        }

        return completionParts;
    }

    @Override
    public void chat(Boolean systemPrompt, String message, Callback callback) throws IOException {
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        GeminiRequest.Part systemPart = new GeminiRequest.Part(SYSTEM_PROMPT);
        GeminiRequest.Content systemContent = new GeminiRequest.Content("user", Collections.singletonList(systemPart));

        GeminiRequest.Part userPart = new GeminiRequest.Part(message);
        GeminiRequest.Content userContent = new GeminiRequest.Content("user", Collections.singletonList(userPart));

        GeminiRequest requestPayload = new GeminiRequest(List.of(userContent));
        if (systemPrompt) {
            requestPayload = new GeminiRequest(List.of(systemContent, userContent));
        }

        String jsonBody = gson.toJson(requestPayload);

        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(apiUrl)
                .post(body)
                .build();

        if (Constants.DEBUG) {
            HopLa.montoyaApi.logging().logToOutput("IA chat request: " + jsonBody);
        }
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onResponse("Network error : " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onResponse("API error : " + response.code() + "\n" + response.body().string());
                    return;
                }
                String responseBody = response.body().string();
                if (Constants.DEBUG) {
                    HopLa.montoyaApi.logging().logToOutput("IA chat response: " + responseBody);
                }

                String reply = extractTextFromResponse(responseBody);
                reply = reply.replace("**", "").replace("* ", "  ");
                callback.onResponse(reply);
            }
        });

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

    static class GeminiResponse {
        List<GeminiResponse.Candidate> candidates;

        static class Candidate {
            GeminiResponse.Content content;
        }

        static class Content {
            List<GeminiResponse.Part> parts;
        }

        static class Part {
            String text;
        }
    }
}
