package dio.budgeting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Teste de contexto. Requer um provedor de chat ativo (Ollama por padrão),
 * por isso é condicional para não quebrar CI sem infraestrutura.
 * Habilite definindo RUN_CONTEXT_TEST=true com o Ollama rodando.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "RUN_CONTEXT_TEST", matches = "true")
class BudgetAiApplicationTests {

    @Test
    void contextLoads() {
    }
}
