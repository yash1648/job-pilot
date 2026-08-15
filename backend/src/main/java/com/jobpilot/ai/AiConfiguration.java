package com.jobpilot.ai;

import com.jobpilot.ai.provider.fake.FakeAiService;
import com.jobpilot.ai.provider.fake.FakeEmbeddingService;
import com.jobpilot.ai.provider.fake.FakeVisionService;
import com.jobpilot.ai.provider.ollama.OllamaAiService;
import com.jobpilot.ai.provider.ollama.OllamaClient;
import com.jobpilot.ai.provider.ollama.OllamaEmbeddingService;
import com.jobpilot.ai.provider.ollama.OllamaVisionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Selects the AI provider implementation from {@code jobpilot.ai.provider}
 * (doc 06 §1, doc 26 §3). Only classes inside {@code com.jobpilot.ai..} may
 * depend on {@code ai.provider..} — enforced by ModuleBoundaryTest.
 */
@Configuration
public class AiConfiguration {

    @Configuration
    @ConditionalOnProperty(name = "jobpilot.ai.provider", havingValue = "fake")
    static class FakeProviderConfig {
        @Bean
        FakeAiService aiService() {
            return new FakeAiService();
        }

        @Bean
        FakeEmbeddingService embeddingService() {
            return new FakeEmbeddingService();
        }

        @Bean
        FakeVisionService visionService() {
            return new FakeVisionService();
        }
    }

    @Configuration
    @ConditionalOnProperty(name = "jobpilot.ai.provider", havingValue = "ollama")
    static class OllamaProviderConfig {
        @Bean
        OllamaClient ollamaClient(
                @org.springframework.beans.factory.annotation.Value("${jobpilot.ai.ollama.base-url:http://localhost:11434}") String baseUrl) {
            return new OllamaClient(baseUrl);
        }

        @Bean
        OllamaAiService aiService(ModelRouter router, OllamaClient client) {
            return new OllamaAiService(router, client);
        }

        @Bean
        OllamaEmbeddingService embeddingService(ModelRouter router, OllamaClient client) {
            return new OllamaEmbeddingService(router, client);
        }

        @Bean
        OllamaVisionService visionService(ModelRouter router, OllamaClient client) {
            return new OllamaVisionService(router, client);
        }
    }
}
