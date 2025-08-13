package com.hopla.ai;

public enum AIProviderType {
    OLLAMA("Ollama"),
    GEMINI("Gemini"),
    OPENAI("OpenAI"),
    BURP("Burp");

    private final String displayName;

    AIProviderType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}