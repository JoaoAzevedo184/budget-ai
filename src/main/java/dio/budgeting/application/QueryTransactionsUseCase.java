package dio.budgeting.application;

import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.domain.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Casos de uso de consulta: listagem por período e agregação por categoria. */
@Service
public class QueryTransactionsUseCase {

    private final TransactionRepository repository;

    public QueryTransactionsUseCase(TransactionRepository repository) {
        this.repository = repository;
    }

    public List<TransactionOutput> findByPeriod(LocalDate from, LocalDate to) {
        return repository.findByPeriod(from, to).stream()
                .map(TransactionOutput::from)
                .toList();
    }

    /** ⭐ EVOLUÇÃO: soma total de uma categoria em um período. */
    public BigDecimal sumByCategory(String category, LocalDate from, LocalDate to) {
        String normalized = category == null ? null : category.trim().toLowerCase();
        return repository.sumByCategory(normalized, from, to);
    }
}
