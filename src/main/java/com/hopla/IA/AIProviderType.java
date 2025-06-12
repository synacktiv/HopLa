package com.hopla.IA;

public enum AIProviderType {
    OLLAMA("Ollama"),
    GEMINI("Gemini");

    private final String displayName;

    AIProviderType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}