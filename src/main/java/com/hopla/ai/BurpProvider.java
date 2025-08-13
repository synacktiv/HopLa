package com.hopla.ai;

import burp.api.montoya.ai.chat.Message;
import burp.api.montoya.ai.chat.PromptOptions;
import burp.api.montoya.ai.chat.PromptResponse;
import com.hopla.Completer;
import com.hopla.Constants;
import com.hopla.HopLa;
import com.hopla.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.hopla.Constants.DEBUG_AI;

public class BurpProvider extends AIProvider {

    public BurpProvider(LLMConfig config, LLMConfig.Provider providerConfig) {
        super(AIProviderType.BURP, AIProviderType.BURP.toString(), config, providerConfig);
    }

    @Override
    public void instruct(String prompt, StreamingCallback callback) throws IOException {
        if (!HopLa.montoyaApi.ai().isEnabled() && !Constants.EXTERNAL_AI) {
            Utils.alert(Constants.ERROR_BURP_AI_DISABLED);
            return;
        }

        List<Message> messages = new ArrayList<>();
        PromptOptions options = PromptOptions.promptOptions();

        if (!providerConfig.quick_action_system_prompt.isEmpty()) {
            messages.add(Message.systemMessage(providerConfig.quick_action_system_prompt));
        }

        if (!providerConfig.quick_action_params.isEmpty() && providerConfig.quick_action_params.containsKey("temperature")) {
            options = options.withTemperature((double) providerConfig.quick_action_params.get("temperature"));
        }
        messages.add(Message.userMessage(prompt));

        sendRequest(messages, options, callback);
    }

    @Override
    public List<String> autoCompletion(Completer.CaretContext caretContext) throws IOException {
        return new ArrayList<>();
    }

    @Override
    public void chat(AIChats.Chat chat, StreamingCallback callback) {
        if (!HopLa.montoyaApi.ai().isEnabled() && !Constants.EXTERNAL_AI) {
            Utils.alert(Constants.ERROR_BURP_AI_DISABLED);
            return;
        }

        List<Message> messages = new ArrayList<>();

        PromptOptions options = PromptOptions.promptOptions();

        if (!providerConfig.chat_system_prompt.isEmpty()) {
            messages.add(Message.systemMessage(providerConfig.chat_system_prompt));
        }

        if (!providerConfig.chat_params.isEmpty() && providerConfig.chat_params.containsKey("temperature")) {
            options = options.withTemperature((double) providerConfig.chat_params.get("temperature"));
        }


        for (AIChats.Message message : chat.getMessages().subList(0, chat.getMessages().size() - 1)) {

            if (message.role == AIChats.MessageRole.USER) {
                messages.add(Message.userMessage(message.getContent()));
            }
            if (message.role == AIChats.MessageRole.ASSISTANT) {
                messages.add(Message.assistantMessage(message.getContent()));
            }
            if (message.role == AIChats.MessageRole.SYSTEM) {
                messages.add(Message.systemMessage(message.getContent()));
            }
        }
        sendRequest(messages, options, callback);
    }

    private void sendRequest(List<Message> messages, PromptOptions options, StreamingCallback callback) {
        new Thread(() -> {
            try {
                PromptResponse response = HopLa.montoyaApi.ai().prompt().execute(options, messages.toArray(new Message[0]));
                if (DEBUG_AI) {
                    HopLa.montoyaApi.logging().logToOutput("AI chat streaming response: " + response.content());
                }
                callback.onData(response.content());
                callback.onDone();
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }

        }).start();
    }
}