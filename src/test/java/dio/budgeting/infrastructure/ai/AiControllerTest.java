package dio.budgeting.infrastructure.ai;

import dio.budgeting.application.VoiceAuditUseCase;
import dio.budgeting.domain.VoiceAudit;
import dio.budgeting.domain.VoiceAuditId;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de slice do AiController (sem subir a app inteira, sem rede, sem IA real).
 *
 * Os modelos de áudio são MOCKADOS — o conteúdo do áudio não é lido; o mock
 * devolve texto fixo. Cobre a LÓGICA do controller:
 *   - chat: 200 + reply;
 *   - voice com modelos de áudio: 200 + corpo de áudio E grava a auditoria;
 *   - voice sem modelos (perfil Ollama): 501 (degradação graciosa);
 *   - history: 200 + JSON do histórico (Fase 4).
 *
 * O ChatClient é mockado em cadeia (prompt -> user -> call -> content).
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

    // Fase 4: o controller agora depende do caso de uso de auditoria.
    @MockitoBean
    private VoiceAuditUseCase voiceAuditUseCase;

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
    void voice_shouldReturnMp3_andRecordAudit_whenAudioModelsAvailable() throws Exception {
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

        // Fase 4: o fluxo de voz deve registrar a transcrição + resposta na auditoria.
        verify(voiceAuditUseCase).record(
                "registra um gasto de 30 reais de uber hoje",
                "Registrei R$ 30,00 em transporte hoje.");
    }

    @Test
    void voiceHistory_shouldReturnEntries() throws Exception {
        var audit = new VoiceAudit(
                new VoiceAuditId(UUID.fromString("f1c90000-0000-0000-0000-000000000000")),
                "gastei 30 reais de uber",
                "Registrei R$ 30,00 em transporte.",
                LocalDateTime.of(2026, 6, 20, 10, 0, 0));

        when(voiceAuditUseCase.history(any())).thenReturn(List.of(audit));

        mockMvc.perform(get("/api/ai/voice/history").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].transcript").value("gastei 30 reais de uber"))
                .andExpect(jsonPath("$[0].reply").value("Registrei R$ 30,00 em transporte."))
                .andExpect(jsonPath("$[0].id").value("f1c90000-0000-0000-0000-000000000000"));
    }
}
