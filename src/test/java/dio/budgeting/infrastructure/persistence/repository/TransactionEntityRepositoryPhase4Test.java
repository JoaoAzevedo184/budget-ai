package dio.budgeting.infrastructure.persistence.repository;

import dio.budgeting.domain.TransactionType;
import dio.budgeting.infrastructure.persistence.entity.TransactionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de persistência das queries adicionadas na Fase 4 (slice JPA, H2, sem rede).
 *
 * Cobre:
 *   - sumByTypeAndPeriod: soma por tipo (base do saldo), com COALESCE -> 0.
 *   - topCategories: agrupamento por categoria ordenado desc, respeitando o limite.
 */
@DataJpaTest
class TransactionEntityRepositoryPhase4Test {

    @Autowired
    private TransactionEntityRepository repository;

    private static final LocalDate D10 = LocalDate.of(2026, 6, 10);
    private static final LocalDate D15 = LocalDate.of(2026, 6, 15);
    private static final LocalDate FROM = LocalDate.of(2026, 6, 1);
    private static final LocalDate TO = LocalDate.of(2026, 6, 30);

    @BeforeEach
    void seed() {
        repository.deleteAll();
        // despesas
        repository.save(entity("mercado", TransactionType.EXPENSE, "20.00", D10));
        repository.save(entity("mercado", TransactionType.EXPENSE, "25.00", D15));   // mercado = 45
        repository.save(entity("transporte", TransactionType.EXPENSE, "30.00", D15)); // transporte = 30
        repository.save(entity("lazer", TransactionType.EXPENSE, "10.00", D15));      // lazer = 10
        // receitas
        repository.save(entity("salario", TransactionType.INCOME, "3000.00", D10));
        repository.save(entity("freela", TransactionType.INCOME, "500.00", D15));
    }

    // ── sumByTypeAndPeriod (base do balance) ─────────────────────────────────

    @Test
    void sumByType_shouldSumOnlyExpenses() {
        BigDecimal total = repository.sumByTypeAndPeriod(TransactionType.EXPENSE, FROM, TO);
        // 20 + 25 + 30 + 10 = 85
        assertThat(total).isEqualByComparingTo("85.00");
    }

    @Test
    void sumByType_shouldSumOnlyIncome() {
        BigDecimal total = repository.sumByTypeAndPeriod(TransactionType.INCOME, FROM, TO);
        // 3000 + 500 = 3500
        assertThat(total).isEqualByComparingTo("3500.00");
    }

    @Test
    void sumByType_shouldReturnZero_whenNoMatchInPeriod() {
        BigDecimal total = repository.sumByTypeAndPeriod(
                TransactionType.EXPENSE,
                LocalDate.of(2030, 1, 1), LocalDate.of(2030, 12, 31));
        // COALESCE garante 0 em vez de null
        assertThat(total).isEqualByComparingTo("0");
    }

    // ── topCategories ────────────────────────────────────────────────────────

    @Test
    void topCategories_shouldOrderExpensesDescByTotal() {
        var result = repository.topCategories(
                TransactionType.EXPENSE, FROM, TO, PageRequest.of(0, 10));

        // mercado (45) > transporte (30) > lazer (10)
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getCategory()).isEqualTo("mercado");
        assertThat(result.get(0).getTotal()).isEqualByComparingTo("45.00");
        assertThat(result.get(1).getCategory()).isEqualTo("transporte");
        assertThat(result.get(2).getCategory()).isEqualTo("lazer");
    }

    @Test
    void topCategories_shouldRespectLimit() {
        var result = repository.topCategories(
                TransactionType.EXPENSE, FROM, TO, PageRequest.of(0, 2));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(
                TransactionEntityRepository.CategoryTotalProjection::getCategory)
                .containsExactly("mercado", "transporte");
    }

    @Test
    void topCategories_shouldFilterByType() {
        var result = repository.topCategories(
                TransactionType.INCOME, FROM, TO, PageRequest.of(0, 10));

        // só categorias de receita: salario (3000) > freela (500)
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCategory()).isEqualTo("salario");
        assertThat(result.get(1).getCategory()).isEqualTo("freela");
    }

    @Test
    void topCategories_shouldReturnEmpty_whenNoMatch() {
        var result = repository.topCategories(
                TransactionType.EXPENSE,
                LocalDate.of(2030, 1, 1), LocalDate.of(2030, 12, 31),
                PageRequest.of(0, 10));

        assertThat(result).isEmpty();
    }

    private static TransactionEntity entity(String category, TransactionType type,
                                            String amount, LocalDate date) {
        return new TransactionEntity(
                UUID.randomUUID(),
                "desc-" + category,
                new BigDecimal(amount),
                type,
                category,
                date);
    }
}
