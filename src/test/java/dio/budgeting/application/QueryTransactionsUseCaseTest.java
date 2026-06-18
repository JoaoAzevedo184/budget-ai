package dio.budgeting.application;

import dio.budgeting.domain.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários da consulta agregada (a evolução do projeto).
 *
 * Foco: o use case NORMALIZA o input (trim + lowercase) antes de delegar ao
 * repositório, e repassa as datas sem alterá-las. Repositório mockado.
 */
@ExtendWith(MockitoExtension.class)
class QueryTransactionsUseCaseTest {

    @Mock
    private TransactionRepository repository;

    @InjectMocks
    private QueryTransactionsUseCase useCase;

    @Test
    void should_normalizeCategory_and_delegate_to_repository() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 30);

        // o repositório, ao receber a categoria já normalizada, devolve um total
        when(repository.sumByCategory(eq("transporte"), eq(from), eq(to)))
                .thenReturn(new BigDecimal("75.00"));

        // input com espaços e maiúsculas; deve ser normalizado para "transporte"
        BigDecimal total = useCase.sumByCategory("  Transporte ", from, to);

        assertThat(total).isEqualByComparingTo("75.00");
        // confirma que o repositório foi chamado com a categoria normalizada
        verify(repository).sumByCategory("transporte", from, to);
    }

    @Test
    void should_passNullCategory_through_when_inputIsNull() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 30);

        when(repository.sumByCategory(eq(null), eq(from), eq(to)))
                .thenReturn(BigDecimal.ZERO);

        BigDecimal total = useCase.sumByCategory(null, from, to);

        assertThat(total).isEqualByComparingTo("0");
        verify(repository).sumByCategory(null, from, to);
    }
}