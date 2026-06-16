package dio.budgeting.infrastructure.persistence.repository;

import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;
import dio.budgeting.domain.TransactionType;
import dio.budgeting.infrastructure.persistence.entity.TransactionEntity;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Adaptador JPA que implementa a porta de saída do domínio. */
@Repository
public class JpaTransactionRepository implements TransactionRepository {

    private final TransactionEntityRepository jpa;

    public JpaTransactionRepository(TransactionEntityRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Transaction save(Transaction transaction) {
        return jpa.save(TransactionEntity.from(transaction)).toDomain();
    }

    @Override
    public List<Transaction> findByPeriod(LocalDate from, LocalDate to) {
        return jpa.findByDateBetween(from, to).stream()
                .map(TransactionEntity::toDomain)
                .toList();
    }

    @Override
    public BigDecimal sumByCategory(String category, LocalDate from, LocalDate to) {
        // Por padrão, "quanto gastei" considera despesas.
        return jpa.sumByCategoryAndPeriod(category, TransactionType.EXPENSE, from, to);
    }
}
