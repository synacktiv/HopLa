package com.hopla.ai;

public class AIProviderFactory {
    public static AIProvider createProvider(AIProviderType type, LLMConfig config, LLMConfig.Provider providerConfig) {
        switch (type) {
            case GEMINI:
                return new GeminiProvider(config, providerConfig);
            case OLLAMA:
                return new OllamaProvider(config, providerConfig);
            case OPENAI:
                return new OpenAIProvider(config, providerConfig);
            default:
                throw new IllegalArgumentException("Provider unsupported: " + type);
        }
    }
}
