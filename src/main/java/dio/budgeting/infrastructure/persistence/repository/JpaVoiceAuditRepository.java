package dio.budgeting.infrastructure.persistence.repository;

import dio.budgeting.domain.VoiceAudit;
import dio.budgeting.infrastructure.persistence.entity.VoiceAuditEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Adaptador JPA da porta de auditoria de voz. */
@Repository
public class JpaVoiceAuditRepository implements VoiceAudit.Repository {

    private final VoiceAuditEntityRepository jpa;

    public JpaVoiceAuditRepository(VoiceAuditEntityRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public VoiceAudit save(VoiceAudit audit) {
        return jpa.save(VoiceAuditEntity.from(audit)).toDomain();
    }

    @Override
    public List<VoiceAudit> findRecent(int limit) {
        return jpa.findAllByOrderByProcessedAtDesc(PageRequest.of(0, limit)).stream()
                .map(VoiceAuditEntity::toDomain)
                .toList();
    }
}
