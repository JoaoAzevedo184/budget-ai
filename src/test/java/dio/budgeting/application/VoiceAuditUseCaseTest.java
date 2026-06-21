package dio.budgeting.application;

import dio.budgeting.domain.VoiceAudit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários do caso de uso de auditoria de voz (Fase 4, item 4).
 *
 * Porta de persistência mockada. Cobre: o registro cria um VoiceAudit com a
 * transcrição/resposta dadas, e o histórico sanea o limite (default e teto).
 */
@ExtendWith(MockitoExtension.class)
class VoiceAuditUseCaseTest {

    @Mock
    private VoiceAudit.Repository repository;

    private VoiceAuditUseCase useCase() {
        return new VoiceAuditUseCase(repository);
    }

    @Test
    void should_record_command_with_transcript_and_reply() {
        when(repository.save(any(VoiceAudit.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        useCase().record("gastei 30 reais de uber", "Registrei R$ 30,00 em transporte.");

        ArgumentCaptor<VoiceAudit> captor = ArgumentCaptor.forClass(VoiceAudit.class);
        verify(repository).save(captor.capture());

        VoiceAudit saved = captor.getValue();
        assertThat(saved.getTranscript()).isEqualTo("gastei 30 reais de uber");
        assertThat(saved.getReply()).isEqualTo("Registrei R$ 30,00 em transporte.");
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getProcessedAt()).isNotNull();
    }

    @Test
    void should_useDefaultLimit_when_limitIsNullOrNonPositive() {
        when(repository.findRecent(anyInt())).thenReturn(List.of());

        useCase().history(null);
        useCase().history(0);
        useCase().history(-5);

        ArgumentCaptor<Integer> limit = ArgumentCaptor.forClass(Integer.class);
        verify(repository, org.mockito.Mockito.times(3)).findRecent(limit.capture());
        assertThat(limit.getAllValues()).containsExactly(20, 20, 20); // DEFAULT_LIMIT
    }

    @Test
    void should_capLimit_to_max() {
        when(repository.findRecent(anyInt())).thenReturn(List.of());

        useCase().history(500);

        verify(repository).findRecent(100); // MAX_LIMIT
    }

    @Test
    void should_passThrough_validLimit() {
        when(repository.findRecent(anyInt())).thenReturn(List.of());

        useCase().history(10);

        verify(repository).findRecent(10);
    }
}
