package dio.budgeting.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Contrato de repositório (porta de saída do domínio). */
public interface TransactionRepository {

    Transaction save(Transaction transaction);

    List<Transaction> findByPeriod(LocalDate from, LocalDate to);

    /** Soma o total de uma categoria dentro de um período (inclusivo). */
    BigDecimal sumByCategory(String category, LocalDate from, LocalDate to);
}
