package dio.budgeting.domain;

import java.util.UUID;

/** Identificador forte (strong-typed id) de um registro de auditoria de voz. */
public record VoiceAuditId(UUID uuid) {
    public VoiceAuditId() {
        this(UUID.randomUUID());
    }
}
