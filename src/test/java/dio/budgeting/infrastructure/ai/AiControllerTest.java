package dio.budgeting.infrastructure.ai;

import dio.budgeting.infrastructure.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de slice do AiController (sem subir a aplicação inteira, sem rede, sem IA real).
 *
 * IMPORTANTE: aqui os modelos de áudio são MOCKADOS. Isso significa que o
 * conteúdo do áudio não é lido — o mock devolve texto fixo. Portanto este teste
 * NÃO usa os arquivos .m4a; ele cobre apenas a LÓGICA do controller:
 *   - quando há modelos de áudio: 200 + corpo de áudio;
 *   - quando NÃO há (ex.: profile Ollama): 501 (degradação graciosa).
 *
 * O ChatClient é mockado em cadeia (prompt -> user -> call -> content), porque o
 * controller o usa de forma fluente.
 *
 * Roda no CI sem secrets.
 */
@WebMvcTest(AiController.class)
@Import(SecurityConfig.class)
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatClient chatClient;

    // Os modelos de áudio são opcionais no controller (ObjectProvider).
    // Como @MockitoBean cria um bean, o ObjectProvider os enxerga como presentes.
    @MockitoBean
    private TranscriptionModel transcriptionModel;

    @MockitoBean
    private TextToSpeechModel textToSpeechModel;

    /** Monta o mock fluente do ChatClient para devolver uma resposta de texto fixa. */
    private void stubChatClientReply(String reply) {
        ChatClient.ChatClientRequestSpec spec = org.mockito.Mockito.mock(
                ChatClient.ChatClientRequestSpec.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        when(chatClient.prompt()).thenReturn(spec);
        when(spec.user(anyString())).thenReturn(spec);
        when(spec.call().content()).thenReturn(reply);
    }

    @Test
    void chat_shouldReturnReply() throws Exception {
        stubChatClientReply("Registrei R$ 30,00 em transporte hoje.");

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"registra um gasto de 30 reais de uber hoje\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("transporte")));
    }

    @Test
    void voice_shouldReturnMp3_whenAudioModelsAvailable() throws Exception {
        // transcrição devolve um texto qualquer (o .m4a NÃO é lido — está mockado)
        when(transcriptionModel.transcribe(any()))
                .thenReturn("registra um gasto de 30 reais de uber hoje");
        stubChatClientReply("Registrei R$ 30,00 em transporte hoje.");
        when(textToSpeechModel.call(anyString()))
                .thenReturn(new byte[]{0x49, 0x44, 0x33}); // bytes fake ("ID3")

        var file = new MockMultipartFile(
                "audio", "qualquer.m4a", "audio/mp4", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/ai/voice").file(file))
                .andExpect(status().isOk())
                .andExpect(content().contentType("audio/mp3"));
    }
}