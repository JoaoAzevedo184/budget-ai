package dio.budgeting.infrastructure.persistence.repository;

import dio.budgeting.infrastructure.persistence.entity.VoiceAuditEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de persistência da auditoria de voz (Fase 4, item 4).
 *
 * Verifica a ordenação (mais recente primeiro) e o respeito ao limite (Pageable).
 */
@DataJpaTest
class VoiceAuditEntityRepositoryTest {

    @Autowired
    private VoiceAuditEntityRepository repository;

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 6, 20, 10, 0, 0);

    @BeforeEach
    void seed() {
        repository.deleteAll();
        // gravados fora de ordem de propósito; a query deve ordenar por processed_at desc
        repository.save(entity("comando antigo", "resposta antiga", BASE));               // mais antigo
        repository.save(entity("comando do meio", "resposta do meio", BASE.plusMinutes(5)));
        repository.save(entity("comando recente", "resposta recente", BASE.plusMinutes(10))); // mais novo
    }

    @Test
    void findRecent_shouldOrderByProcessedAtDesc() {
        List<VoiceAuditEntity> result =
                repository.findAllByOrderByProcessedAtDesc(PageRequest.of(0, 10));

        assertThat(result).extracting(VoiceAuditEntity::getTranscript)
                .containsExactly("comando recente", "comando do meio", "comando antigo");
    }

    @Test
    void findRecent_shouldRespectLimit() {
        List<VoiceAuditEntity> result =
                repository.findAllByOrderByProcessedAtDesc(PageRequest.of(0, 2));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTranscript()).isEqualTo("comando recente");
        assertThat(result.get(1).getTranscript()).isEqualTo("comando do meio");
    }

    @Test
    void findRecent_shouldReturnEmpty_whenNoRecords() {
        repository.deleteAll();

        List<VoiceAuditEntity> result =
                repository.findAllByOrderByProcessedAtDesc(PageRequest.of(0, 10));

        assertThat(result).isEmpty();
    }

    private static VoiceAuditEntity entity(String transcript, String reply, LocalDateTime at) {
        return new VoiceAuditEntity(UUID.randomUUID(), transcript, reply, at);
    }
}
