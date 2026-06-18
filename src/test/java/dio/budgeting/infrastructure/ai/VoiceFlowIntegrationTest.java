package dio.budgeting.infrastructure.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de INTEGRAÇÃO REAL do fluxo de voz (áudio -> transcrição -> IA -> TTS -> áudio).
 *
 * ⚠️ NÃO roda no CI. Sobe a aplicação de verdade e bate em provedores externos,
 * então exige credenciais e é disparado manualmente. Condições para rodar:
 *
 *   1. RUN_VOICE_IT=true              (liga este teste)
 *   2. Um profile com áudio ativo. Duas opções:
 *      a) profile "openrouter": UMA chave (OPENROUTER_API_KEY) cobre chat + voz.
 *         ⚠️ exige CRÉDITO na conta OpenRouter (transcrição Whisper é paga);
 *         ⚠️ voz via OpenRouter é experimental (ver application-openrouter.yml).
 *      b) profile "gemini": GEMINI_API_KEY (chat) + OPENAI_API_KEY (áudio).
 *   3. Os arquivos src/test/resources/audio/recording-N.m4a presentes.
 *
 * Como rodar (exemplo, profile openrouter):
 *   export RUN_VOICE_IT=true
 *   export BUDGET_AI_PROVIDER=openrouter
 *   export OPENROUTER_API_KEY=sk-or-...
 *   ./mvnw test -Dtest=VoiceFlowIntegrationTest
 *
 * Se vier erro 402 (Payment Required), é falta de crédito no OpenRouter, não
 * bug do código. Se vier 501, o profile ativo não tem modelos de áudio.
 *
 * NOTA sobre asserção: como o conteúdo falado nos áudios não é conhecido de
 * antemão, o teste verifica apenas que o fluxo completo respondeu com um MP3
 * não-vazio (HTTP 200 + corpo de áudio). Quando você souber o que cada áudio
 * diz, dá para fortalecer: transcrever, criar a transação esperada e conferir
 * no banco via /api/transactions.
 *
 * Se o provedor de áudio não estiver configurado, o endpoint responde 501 — e
 * o teste falha de propósito, sinalizando que falta a credencial de áudio.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named = "RUN_VOICE_IT", matches = "true")
class VoiceFlowIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired(required = false)
    private org.springframework.boot.web.client.RestTemplateBuilder ignored; // só p/ contexto

    @Test
    void voice_shouldReturnNonEmptyMp3_forFirstRecording() {
        Resource audio = new ClassPathResource("audio/recording-1.m4a");
        assertThat(audio.exists())
                .as("coloque os .m4a em src/test/resources/audio/")
                .isTrue();

        RestClient client = RestClient.create("http://localhost:" + port);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("audio", audio);

        ResponseEntity<byte[]> response = client.post()
                .uri("/api/ai/voice")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .toEntity(byte[].class);

        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("se vier 501, falta OPENAI_API_KEY (áudio); se 5xx, ver log do provedor")
                .isTrue();
        assertThat(response.getBody())
                .as("o TTS deve devolver um MP3 não-vazio")
                .isNotNull()
                .isNotEmpty();
    }
}