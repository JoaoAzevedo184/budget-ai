package dio.budgeting.infrastructure.persistence.repository;

import dio.budgeting.domain.TransactionType;
import dio.budgeting.infrastructure.persistence.entity.TransactionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de persistência (slice JPA, banco H2 em memória, sem rede nem IA).
 *
 * @DataJpaTest sobe apenas a camada de dados, então o Spring AI não é
 * autoconfigurado aqui. Cobre o findByDateBetween e o sumByCategoryAndPeriod,
 * incluindo o caso vazio (COALESCE -> 0 em vez de null).
 */
@DataJpaTest
class TransactionEntityRepositoryTest {

    @Autowired
    private TransactionEntityRepository repository;

    private static final LocalDate D10 = LocalDate.of(2026, 6, 10);
    private static final LocalDate D15 = LocalDate.of(2026, 6, 15);
    private static final LocalDate D20 = LocalDate.of(2026, 6, 20);

    @BeforeEach
    void seed() {
        repository.deleteAll();
        repository.save(entity("mercado", TransactionType.EXPENSE, "20.00", D10));
        repository.save(entity("mercado", TransactionType.EXPENSE, "25.00", D15));
        repository.save(entity("transporte", TransactionType.EXPENSE, "30.00", D15));
        // uma receita na mesma categoria não deve entrar na soma de despesas
        repository.save(entity("mercado", TransactionType.INCOME, "100.00", D15));
    }

    @Test
    void findByDateBetween_shouldReturnRecordsInsidePeriod() {
        // os 4 registros do seed estão em D10 e D15, ambos dentro de [01, 16]
        List<TransactionEntity> found =
                repository.findByDateBetween(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 16));

        assertThat(found).hasSize(4);
    }

    @Test
    void findByDateBetween_shouldExcludeOutsidePeriod() {
        repository.save(entity("lazer", TransactionType.EXPENSE, "50.00", D20));

        List<TransactionEntity> found =
                repository.findByDateBetween(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 16));

        // o registro de D20 (lazer) fica de fora
        assertThat(found).extracting(TransactionEntity::getCategory)
                .doesNotContain("lazer");
    }

    @Test
    void sumByCategoryAndPeriod_shouldSumOnlyExpensesOfCategory() {
        BigDecimal total = repository.sumByCategoryAndPeriod(
                "mercado", TransactionType.EXPENSE,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        // 20.00 + 25.00 = 45.00 (a receita de 100 é IGNORADA por ser INCOME)
        assertThat(total).isEqualByComparingTo("45.00");
    }

    @Test
    void sumByCategoryAndPeriod_shouldReturnZero_whenNoMatch() {
        BigDecimal total = repository.sumByCategoryAndPeriod(
                "inexistente", TransactionType.EXPENSE,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        // COALESCE garante 0 em vez de null
        assertThat(total).isEqualByComparingTo("0");
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