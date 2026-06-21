package dio.budgeting.application;

import dio.budgeting.domain.CategoryTotal;
import dio.budgeting.domain.TransactionRepository;
import dio.budgeting.domain.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários da camada de consulta (a evolução do projeto + Fase 4).
 *
 * Foco: o use case NORMALIZA o input (trim + lowercase), aplica os defaults de
 * tipo/limite e delega ao repositório. Repositório mockado — sem rede, sem Spring.
 */
@ExtendWith(MockitoExtension.class)
class QueryTransactionsUseCaseTest {

    @Mock
    private TransactionRepository repository;

    @InjectMocks
    private QueryTransactionsUseCase useCase;

    private static final LocalDate FROM = LocalDate.of(2026, 6, 1);
    private static final LocalDate TO = LocalDate.of(2026, 6, 30);

    // ── sumByCategory (compatibilidade: sem tipo => EXPENSE) ─────────────────

    @Test
    void should_normalizeCategory_and_delegate_as_expense_when_noTypeGiven() {
        when(repository.sumByCategory(eq("transporte"), eq(TransactionType.EXPENSE), eq(FROM), eq(TO)))
                .thenReturn(new BigDecimal("75.00"));

        // input com espaços e maiúsculas; deve ser normalizado para "transporte"
        BigDecimal total = useCase.sumByCategory("  Transporte ", FROM, TO);

        assertThat(total).isEqualByComparingTo("75.00");
        verify(repository).sumByCategory("transporte", TransactionType.EXPENSE, FROM, TO);
    }

    @Test
    void should_passNullCategory_through_when_inputIsNull() {
        when(repository.sumByCategory(eq(null), eq(TransactionType.EXPENSE), eq(FROM), eq(TO)))
                .thenReturn(BigDecimal.ZERO);

        BigDecimal total = useCase.sumByCategory(null, FROM, TO);

        assertThat(total).isEqualByComparingTo("0");
        verify(repository).sumByCategory(null, TransactionType.EXPENSE, FROM, TO);
    }

    // ── sumByCategory com tipo (Fase 4, item 3) ──────────────────────────────

    @Test
    void should_useIncome_when_typeIsIncome() {
        when(repository.sumByCategory(eq("salario"), eq(TransactionType.INCOME), eq(FROM), eq(TO)))
                .thenReturn(new BigDecimal("3000.00"));

        BigDecimal total = useCase.sumByCategory(" Salario ", TransactionType.INCOME, FROM, TO);

        assertThat(total).isEqualByComparingTo("3000.00");
        verify(repository).sumByCategory("salario", TransactionType.INCOME, FROM, TO);
    }

    @Test
    void should_defaultToExpense_when_typeIsNull() {
        when(repository.sumByCategory(eq("mercado"), eq(TransactionType.EXPENSE), eq(FROM), eq(TO)))
                .thenReturn(new BigDecimal("45.00"));

        BigDecimal total = useCase.sumByCategory("mercado", null, FROM, TO);

        assertThat(total).isEqualByComparingTo("45.00");
        verify(repository).sumByCategory("mercado", TransactionType.EXPENSE, FROM, TO);
    }

    // ── balance (Fase 4, item 1) ─────────────────────────────────────────────

    @Test
    void should_delegateBalance_to_repository() {
        when(repository.balance(FROM, TO)).thenReturn(new BigDecimal("1200.00"));

        BigDecimal balance = useCase.balance(FROM, TO);

        assertThat(balance).isEqualByComparingTo("1200.00");
        verify(repository).balance(FROM, TO);
    }

    // ── topCategories (Fase 4, item 2) ───────────────────────────────────────

    @Test
    void should_applyDefaultTypeAndLimit_when_omitted() {
        // a porta recebe um int primitivo no último argumento -> anyInt()
        when(repository.topCategories(any(), any(), any(), anyInt()))
                .thenReturn(List.of());

        useCase.topCategories(null, FROM, TO, null);

        ArgumentCaptor<TransactionType> type = ArgumentCaptor.forClass(TransactionType.class);
        ArgumentCaptor<Integer> limit = ArgumentCaptor.forClass(Integer.class);
        verify(repository).topCategories(type.capture(), eq(FROM), eq(TO), limit.capture());

        assertThat(type.getValue()).isEqualTo(TransactionType.EXPENSE); // default
        assertThat(limit.getValue()).isEqualTo(5);                      // DEFAULT_TOP_LIMIT
    }

    @Test
    void should_sanitizeNonPositiveLimit_toDefault() {
        when(repository.topCategories(any(), any(), any(), anyInt()))
                .thenReturn(List.of());

        useCase.topCategories(TransactionType.INCOME, FROM, TO, 0);

        ArgumentCaptor<Integer> limit = ArgumentCaptor.forClass(Integer.class);
        verify(repository).topCategories(eq(TransactionType.INCOME), eq(FROM), eq(TO), limit.capture());
        assertThat(limit.getValue()).isEqualTo(5);
    }

    @Test
    void should_honorExplicitLimit_and_returnResults() {
        var top = List.of(
                new CategoryTotal("mercado", new BigDecimal("100.00")),
                new CategoryTotal("transporte", new BigDecimal("60.00")));
        when(repository.topCategories(eq(TransactionType.EXPENSE), eq(FROM), eq(TO), eq(2)))
                .thenReturn(top);

        List<CategoryTotal> result = useCase.topCategories(TransactionType.EXPENSE, FROM, TO, 2);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).category()).isEqualTo("mercado");
        verify(repository).topCategories(TransactionType.EXPENSE, FROM, TO, 2);
    }
}
