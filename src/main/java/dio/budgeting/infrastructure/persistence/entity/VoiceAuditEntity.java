package dio.budgeting.infrastructure.persistence.entity;

import dio.budgeting.domain.VoiceAudit;
import dio.budgeting.domain.VoiceAuditId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "voice_audit")
public class VoiceAuditEntity {

    @Id
    private UUID id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String transcript;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reply;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    protected VoiceAuditEntity() {
        // exigido pelo JPA
    }

    public VoiceAuditEntity(UUID id, String transcript, String reply, LocalDateTime processedAt) {
        this.id = id;
        this.transcript = transcript;
        this.reply = reply;
        this.processedAt = processedAt;
    }

    public static VoiceAuditEntity from(VoiceAudit a) {
        return new VoiceAuditEntity(
                a.getId().uuid(),
                a.getTranscript(),
                a.getReply(),
                a.getProcessedAt());
    }

    public VoiceAudit toDomain() {
        return new VoiceAudit(new VoiceAuditId(id), transcript, reply, processedAt);
    }

    public UUID getId() {
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
}
