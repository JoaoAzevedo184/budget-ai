package dio.budgeting.application;

import dio.budgeting.domain.VoiceAudit;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Caso de uso de auditoria de comandos de voz.
 *
 * Registra cada comando processado (transcrição + resposta) e permite consultar
 * o histórico recente. A persistência é uma porta de saída ({@link VoiceAudit.Repository}).
 */
@Service
public class VoiceAuditUseCase {

    /** Limite padrão e máximo do histórico para evitar respostas gigantes. */
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final VoiceAudit.Repository repository;

    public VoiceAuditUseCase(VoiceAudit.Repository repository) {
        this.repository = repository;
    }

    /** Registra um comando de voz processado. Falhas aqui não devem quebrar o fluxo principal. */
    public VoiceAudit record(String transcript, String reply) {
        return repository.save(new VoiceAudit(transcript, reply));
    }

    /** Histórico recente (mais novo primeiro), com limite saneado. */
    public List<VoiceAudit> history(Integer limit) {
        int effective = (limit == null || limit <= 0) ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        return repository.findRecent(effective);
    }
}
