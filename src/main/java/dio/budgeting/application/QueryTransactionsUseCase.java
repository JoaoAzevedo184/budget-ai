package dio.budgeting.application;

import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.domain.CategoryTotal;
import dio.budgeting.domain.TransactionRepository;
import dio.budgeting.domain.TransactionType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Casos de uso de consulta: listagem por período, agregações e saldo. */
@Service
public class QueryTransactionsUseCase {

    /** Quantidade padrão de categorias retornadas quando o cliente não informa. */
    private static final int DEFAULT_TOP_LIMIT = 5;

    private final TransactionRepository repository;

    public QueryTransactionsUseCase(TransactionRepository repository) {
        this.repository = repository;
    }

    public List<TransactionOutput> findByPeriod(LocalDate from, LocalDate to) {
        return repository.findByPeriod(from, to).stream()
                .map(TransactionOutput::from)
                .toList();
    }

    /**
     * Soma total de uma categoria em um período.
     * Mantido por compatibilidade — considera despesas (EXPENSE).
     */
    public BigDecimal sumByCategory(String category, LocalDate from, LocalDate to) {
        return sumByCategory(category, TransactionType.EXPENSE, from, to);
    }

    /**
     * Soma total de uma categoria filtrando pelo tipo.
     * Quando {@code type} é null, assume EXPENSE (comportamento histórico).
     */
    public BigDecimal sumByCategory(String category, TransactionType type,
                                    LocalDate from, LocalDate to) {
        String normalized = category == null ? null : category.trim().toLowerCase();
        TransactionType effectiveType = type == null ? TransactionType.EXPENSE : type;
        return repository.sumByCategory(normalized, effectiveType, from, to);
    }

    /** Saldo do período: receitas menos despesas. */
    public BigDecimal balance(LocalDate from, LocalDate to) {
        return repository.balance(from, to);
    }

    /** Top categorias por total no período. */
    public List<CategoryTotal> topCategories(TransactionType type, LocalDate from,
                                             LocalDate to, Integer limit) {
        TransactionType effectiveType = type == null ? TransactionType.EXPENSE : type;
        int effectiveLimit = (limit == null || limit <= 0) ? DEFAULT_TOP_LIMIT : limit;
        return repository.topCategories(effectiveType, from, to, effectiveLimit);
    }
}
