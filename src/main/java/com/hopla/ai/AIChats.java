package com.hopla.ai;


import com.hopla.HopLa;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.inspector.TagInspector;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.hopla.Constants.PREFERENCE_AI_CHATS;
import static com.hopla.Utils.alert;


public class AIChats {
    private final Yaml yaml;
    public Chats chats = new Chats();

    public AIChats() {
        var loaderoptions = new LoaderOptions();
        TagInspector taginspector =
                tag -> tag.getClassName().equals(Chats.class.getName());
        loaderoptions.setTagInspector(taginspector);
        this.yaml = new Yaml(new Constructor(Chats.class, loaderoptions));
        load();
    }

    public List<Chat> getChats() {
        return chats.chats;
    }

    public void load() {
        String config = HopLa.montoyaApi.persistence().preferences().getString(PREFERENCE_AI_CHATS);
        if (config == null || config.isEmpty()) {
            return;
        }
        try {
            this.chats = yaml.load(config);
            if (this.chats == null) {
                chats = new Chats();
            }
            HopLa.montoyaApi.logging().logToOutput("AI Chats loaded");
        } catch (Exception e) {
            HopLa.montoyaApi.logging().logToError("Failed to load AI chats:  " + e.getMessage() + "\nAll chats reset");
            alert("Failed to load AI chats:  " + e.getMessage() + "\nAll chats reset");
            chats = new Chats();
            save();
        }
    }

    public void save() {
        String output = yaml.dump(chats);
        HopLa.montoyaApi.persistence().preferences().setString(PREFERENCE_AI_CHATS, output);
    }

    public enum MessageRole {
        USER,
        ASSISTANT,
        SYSTEM
    }

    public static class Chats {
        public List<Chat> chats;

        public Chats() {
            this.chats = new ArrayList<>();
        }
    }

    public static class Chat {
        public String timestamp;
        public List<Message> messages;

        public Chat(String timestamp, List<Message> messages) {
            this.timestamp = timestamp;
            this.messages = messages;
        }

        public Chat() {
            this.messages = new ArrayList<>();
            this.timestamp = LocalDateTime.now().toString();
        }

        public void addMessage(Message message) {
            messages.add(message);
        }


        public List<Message> getMessages() {
            return messages;
        }

        public Message getLastMessage() {
            if (messages.isEmpty()) return null;
            return messages.getLast();
        }

        public Message getLastUserMessage() {
            if (messages.isEmpty() || messages.size() < 2) return null;
            return messages.get(messages.size() - 2);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Message msg : messages) {
                sb.append(msg).append("\n");
            }
            return sb.toString();
        }
    }

    public static class Message {
        public MessageRole role;
        public String content;

        public Message() {
        }

        public Message(MessageRole role, String content) {
            this.role = role;
            this.content = content;
        }

        public MessageRole getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }

        public void appendContent(String content) {
            this.content += content;
        }

        @Override
        public String toString() {
            return "**" + role + ":** " + content + "\n";

        }
    }
}
