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
 * Teste de integração real do fluxo de voz (áudio -> transcrição -> IA -> TTS -> áudio).
 *
 * Não roda no CI: sobe a aplicação de verdade e chama provedores externos, então
 * exige credenciais e é disparado manualmente. Condições para rodar:
 *
 *   1. RUN_VOICE_IT=true (liga este teste).
 *   2. Um profile com áudio ativo:
 *      a) profile "openrouter": uma chave (OPENROUTER_API_KEY) cobre chat e voz.
 *         Exige crédito na conta (transcrição Whisper é paga) e a voz via
 *         OpenRouter é experimental (ver application-openrouter.yml).
 *      b) profile "gemini": GEMINI_API_KEY (chat) e OPENAI_API_KEY (áudio).
 *   3. Os arquivos src/test/resources/audio/recording-N.m4a presentes.
 *
 * Como rodar (exemplo, profile openrouter):
 *   export RUN_VOICE_IT=true
 *   export BUDGET_AI_PROVIDER=openrouter
 *   export OPENROUTER_API_KEY=sk-or-...
 *   ./mvnw test -Dtest=VoiceFlowIntegrationTest
 *
 * Erro 402 (Payment Required) indica falta de crédito no OpenRouter, não bug do
 * código. Erro 501 indica que o profile ativo não tem modelos de áudio.
 *
 * Como o conteúdo falado não é conhecido de antemão, o teste verifica apenas que
 * o fluxo completo respondeu com um MP3 não-vazio (HTTP 200 + corpo de áudio).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named = "RUN_VOICE_IT", matches = "true")
class VoiceFlowIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired(required = false)
    private org.springframework.boot.web.client.RestTemplateBuilder ignored; // apenas para o contexto

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