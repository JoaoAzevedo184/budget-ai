package dio.budgeting.infrastructure.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuração multi-provider do ChatClient.
 *
 * Problema resolvido: quando os starters de OpenAI e Ollama coexistem, ambos
 * registram um bean {@link ChatModel}, e o Spring quebra por ambiguidade. Aqui
 * usamos {@link Profile} + {@link Qualifier} para escolher o provedor por ambiente.
 *
 * Ative o provedor com a variável BUDGET_AI_PROVIDER (mapeada para spring.profiles.active):
 *   - ollama  -> dev local (homelab), sem custo
 *   - claude  -> Anthropic via endpoint OpenAI-compatible
 *   - gemini  -> Google Gemini via endpoint OpenAI-compatible
 *   - nvidia  -> NVIDIA NIM via endpoint OpenAI-compatible
 *
 * Para todos os provedores "expert", usamos o OpenAiChatModel apontando o base-url
 * para o endpoint compatível de cada um — assim o código da aplicação não muda.
 */
@Configuration
public class ChatClientConfig {

    private static final String SYSTEM_PROMPT = """
        Você é um assistente financeiro. Sua função é interpretar comandos do usuário
        e chamar as ferramentas disponíveis para criar ou consultar transações.
        Nunca invente valores. Se faltar informação essencial (valor, tipo ou descrição),
        peça esclarecimento em vez de chamar a ferramenta.
        Responda de forma curta e objetiva, em português.
        """;

    /** Perfil de desenvolvimento: Ollama local. */
    @Bean
    @Profile("ollama")
    ChatClient ollamaChatClient(
            @Qualifier("ollamaChatModel") ChatModel chatModel,
            BudgetTools budgetTools) {
        return ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(budgetTools)
                .build();
    }

    /** Perfis de produção: Claude / Gemini / NVIDIA via OpenAI-compatible. */
    @Bean
    @Profile({"claude", "gemini", "nvidia"})
    ChatClient cloudChatClient(
            @Qualifier("openAiChatModel") ChatModel chatModel,
            BudgetTools budgetTools) {
        return ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(budgetTools)
                .build();
    }
}
