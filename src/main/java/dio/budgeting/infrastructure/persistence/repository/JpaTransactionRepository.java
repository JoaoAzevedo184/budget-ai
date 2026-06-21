package dio.budgeting.infrastructure.persistence.repository;

import dio.budgeting.domain.CategoryTotal;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;
import dio.budgeting.domain.TransactionType;
import dio.budgeting.infrastructure.persistence.entity.TransactionEntity;
import org.springframework.data.domain.PageRequest;
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
        return sumByCategory(category, TransactionType.EXPENSE, from, to);
    }

    @Override
    public BigDecimal sumByCategory(String category, TransactionType type,
                                    LocalDate from, LocalDate to) {
        return jpa.sumByCategoryAndPeriod(category, type, from, to);
    }

    @Override
    public BigDecimal balance(LocalDate from, LocalDate to) {
        BigDecimal income = jpa.sumByTypeAndPeriod(TransactionType.INCOME, from, to);
        BigDecimal expense = jpa.sumByTypeAndPeriod(TransactionType.EXPENSE, from, to);
        return income.subtract(expense);
    }

    @Override
    public List<CategoryTotal> topCategories(TransactionType type, LocalDate from,
                                             LocalDate to, int limit) {
        return jpa.topCategories(type, from, to, PageRequest.of(0, limit)).stream()
                .map(p -> new CategoryTotal(p.getCategory(), p.getTotal()))
                .toList();
    }
}
