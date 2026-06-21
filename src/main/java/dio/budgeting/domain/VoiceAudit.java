package dio.budgeting.domain;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Registro de auditoria de um comando de voz processado.
 * Modelo de domínio puro (sem framework).
 */
public class VoiceAudit {

    private final VoiceAuditId id;
    private final String transcript;   // texto transcrito do áudio
    private final String reply;        // resposta da IA
    private final LocalDateTime processedAt;

    public VoiceAudit(VoiceAuditId id, String transcript, String reply, LocalDateTime processedAt) {
        this.id = id;
        this.transcript = transcript;
        this.reply = reply;
        this.processedAt = processedAt;
    }

    /** Construtor de criação (gera id e timestamp). */
    public VoiceAudit(String transcript, String reply) {
        this(new VoiceAuditId(), transcript, reply, LocalDateTime.now());
    }

    public VoiceAuditId getId() {
        return id;
    }

    public String getTranscript() {
        return transcript;
    }

    public String getReply() {
        return reply;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    /** Porta de saída: persistência do histórico de comandos de voz. */
    public interface Repository {
        VoiceAudit save(VoiceAudit audit);

        /** Histórico mais recente primeiro, limitado a {@code limit} registros. */
        List<VoiceAudit> findRecent(int limit);
    }
}
