package dio.budgeting.infrastructure.persistence.repository;

import dio.budgeting.infrastructure.persistence.entity.VoiceAuditEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VoiceAuditEntityRepository extends JpaRepository<VoiceAuditEntity, UUID> {

    /** Histórico mais recente primeiro; o limite vem via Pageable. */
    List<VoiceAuditEntity> findAllByOrderByProcessedAtDesc(Pageable pageable);
}
