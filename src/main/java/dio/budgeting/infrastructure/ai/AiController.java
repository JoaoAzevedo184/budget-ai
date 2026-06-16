package dio.budgeting.infrastructure.ai;

import dio.budgeting.infrastructure.http.request.ChatRequest;
import dio.budgeting.infrastructure.http.response.ChatResponse;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Endpoints de IA.
 *
 *  - POST /api/ai/chat  : texto -> IA (Tool Calling) -> texto. Funciona com qualquer provedor.
 *  - POST /api/ai/voice : áudio -> transcrição -> IA -> TTS -> áudio. Requer modelos de áudio
 *                         (OpenAI/Whisper+TTS); por isso são injetados como opcionais.
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final ChatClient chatClient;
    private final ObjectProvider<TranscriptionModel> transcriptionModel;
    private final ObjectProvider<TextToSpeechModel> textToSpeechModel;

    public AiController(ChatClient chatClient,
                        ObjectProvider<TranscriptionModel> transcriptionModel,
                        ObjectProvider<TextToSpeechModel> textToSpeechModel) {
        this.chatClient = chatClient;
        this.transcriptionModel = transcriptionModel;
        this.textToSpeechModel = textToSpeechModel;
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

        byte[] mp3 = tts.call(reply);
        var resource = new ByteArrayResource(mp3);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("resposta.mp3").build().toString())
                .body(resource);
    }
}
