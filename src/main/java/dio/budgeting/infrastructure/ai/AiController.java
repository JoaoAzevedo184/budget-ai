package dio.budgeting.infrastructure.ai;

import dio.budgeting.application.VoiceAuditUseCase;
import dio.budgeting.infrastructure.http.request.ChatRequest;
import dio.budgeting.infrastructure.http.response.ChatResponse;
import dio.budgeting.infrastructure.http.response.VoiceAuditResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Endpoints de IA.
 *
 *  - POST /api/ai/chat          : texto -> IA (Tool Calling) -> texto. Qualquer provedor.
 *  - POST /api/ai/voice         : áudio -> transcrição -> IA -> TTS -> áudio. Requer áudio.
 *  - GET  /api/ai/voice/history : histórico dos comandos de voz processados.
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    private final ChatClient chatClient;
    private final ObjectProvider<TranscriptionModel> transcriptionModel;
    private final ObjectProvider<TextToSpeechModel> textToSpeechModel;
    private final VoiceAuditUseCase voiceAuditUseCase;

    public AiController(ChatClient chatClient,
                        ObjectProvider<TranscriptionModel> transcriptionModel,
                        ObjectProvider<TextToSpeechModel> textToSpeechModel,
                        VoiceAuditUseCase voiceAuditUseCase) {
        this.chatClient = chatClient;
        this.transcriptionModel = transcriptionModel;
        this.textToSpeechModel = textToSpeechModel;
        this.voiceAuditUseCase = voiceAuditUseCase;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String reply = chatClient.prompt()
                .user(request.message())
                .call()
                .content();
        return new ChatResponse(reply);
    }

    @PostMapping(value = "/voice",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = "audio/mp3")
    public ResponseEntity<Resource> voice(@RequestParam("audio") MultipartFile audio) {
        TranscriptionModel stt = transcriptionModel.getIfAvailable();
        TextToSpeechModel tts = textToSpeechModel.getIfAvailable();
        if (stt == null || tts == null) {
            // Provedor ativo não tem áudio (ex.: Ollama puro). Use /api/ai/chat.
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }

        String userMessage = stt.transcribe(audio.getResource());
        String reply = chatClient.prompt().user(userMessage).call().content();

        // Registra o comando de voz. Falha aqui não deve quebrar a resposta de áudio.
        try {
            voiceAuditUseCase.record(userMessage, reply);
        } catch (Exception e) {
            log.warn("Falha ao registrar auditoria do comando de voz: {}", e.getMessage());
        }

        byte[] mp3 = tts.call(reply);
        var resource = new ByteArrayResource(mp3);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("resposta.mp3").build().toString())
                .body(resource);
    }

    /** Histórico dos comandos de voz processados (mais recentes primeiro). */
    @GetMapping("/voice/history")
    public List<VoiceAuditResponse> voiceHistory(
            @RequestParam(name = "limit", required = false) Integer limit) {
        return voiceAuditUseCase.history(limit).stream()
                .map(VoiceAuditResponse::from)
                .toList();
    }
}
