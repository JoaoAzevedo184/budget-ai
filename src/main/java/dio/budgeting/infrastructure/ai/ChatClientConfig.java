package dio.budgeting.infrastructure.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Configuração do ChatClient.
 *
 * Estratégia simples e robusta: injetamos UM bean {@link ChatModel}. Qual bean
 * é esse depende do profile/dependências ativas:
 *   - profile "ollama"  -> spring-ai-starter-model-ollama fornece ollamaChatModel
 *   - profiles cloud    -> spring-ai-starter-model-openai fornece openAiChatModel
 *                          (Claude/Gemini/NVIDIA via base-url OpenAI-compatible)
 *
 * Importante: para evitar ambiguidade de bean, mantenha apenas UM starter de chat
 * ativo por execução. Com os dois starters no classpath ao mesmo tempo, use
 * @Qualifier aqui para desambiguar (ver README, seção multi-provider).
 */
@Configuration
public class ChatClientConfig {

    @Bean
    ChatClient chatClient(ChatModel chatModel,
                          BudgetTools budgetTools,
                          @Value("classpath:prompts/system-message.st") Resource systemPrompt)
            throws IOException {
        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt.getContentAsString(StandardCharsets.UTF_8))
                .defaultTools(budgetTools)
                .build();
    }
}
