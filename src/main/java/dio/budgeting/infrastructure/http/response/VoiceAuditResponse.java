package dio.budgeting.infrastructure.http.response;

import dio.budgeting.domain.VoiceAudit;

import java.time.LocalDateTime;

/** Resposta REST de um registro do histórico de comandos de voz. */
public record VoiceAuditResponse(
        String id,
        String transcript,
        String reply,
        LocalDateTime processedAt) {

    public static VoiceAuditResponse from(VoiceAudit a) {
        return new VoiceAuditResponse(
                a.getId().uuid().toString(),
                a.getTranscript(),
                a.getReply(),
                a.getProcessedAt());
    }
}
