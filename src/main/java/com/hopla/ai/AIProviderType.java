package com.hopla.ai;

public enum AIProviderType {
    OLLAMA("Ollama"),
    GEMINI("Gemini"),
    OPENAI("OpenAI");

    private final String displayName;

    AIProviderType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}