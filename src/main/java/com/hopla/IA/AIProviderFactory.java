package com.hopla.IA;

public class AIProviderFactory {
    public static AIProvider createProvider(AIProviderType type, UserAiConfiguration config) {
        switch (type) {
            case GEMINI:
                return new GeminiProvider(config);
            case OLLAMA:
                return new OllamaProvider(config);
            default:
                throw new IllegalArgumentException("Provider unsupported: " + type);
        }
    }
}
