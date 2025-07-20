package com.hopla.ai;

import java.net.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LLMConfig {
    public String shortcut_ai_chat;
    public String shortcut_quick_action;
    public int autocompletion_min_chars = 1;
    public Map<AIProviderType, Provider> providers;
    public Defaults defaults;
    public List<Prompt> prompts = new ArrayList<>();
    public List<QuickAction> quick_actions = new ArrayList<>();

    public static class ProxyConfig {
        public boolean enabled = false;
        public String host = "127.0.0.1";
        public int port = 8080;
        public Proxy.Type type = Proxy.Type.DIRECT;
        public String username = "";
        public String password = "";

        public String toString() {
            return this.type + " " + this.host + " " + this.port;
        }
    }

    public static class Provider {
        public String name;
        public boolean enabled;
        public String chat_model;
        public String completion_model;
        public String quick_action_model;
        public String chat_system_prompt = "";
        public String completion_system_prompt = "";
        public String quick_action_system_prompt = "";
        public String api_key;
        public String chat_endpoint;
        public String completion_endpoint;
        public String quick_action_endpoint;
        public String completion_prompt;
        public Map<String, Object> headers = new HashMap<>();
        public Map<String, Object> completion_params = new HashMap<>();
        public Map<String, Object> chat_params = new HashMap<>();
        public Map<String, Object> quick_action_params = new HashMap<>();
        public List<String> completion_stops = new ArrayList<>();
        public List<String> chat_stops = new ArrayList<>();
        public List<String> quick_action_stops = new ArrayList<>();
        public ProxyConfig proxy = new ProxyConfig();
    }

    public static class Defaults {
        public String chat_provider;
        public String completion_provider;
        public String quick_action_provider;
        public int timeout_sec;
    }

    public static class Prompt {
        public String name;
        public String description;
        public String content;

        @Override
        public String toString() {
            return name + ": " + description;
        }
    }

    public static class QuickAction {
        public String name;
        public String description;
        public String content;

        @Override
        public String toString() {
            return name + ": " + description;
        }
    }

}