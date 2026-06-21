package dio.budgeting.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Contrato de repositório (porta de saída do domínio). */
public interface TransactionRepository {

    Transaction save(Transaction transaction);

    List<Transaction> findByPeriod(LocalDate from, LocalDate to);

    /**
     * Soma o total de uma categoria dentro de um período (inclusivo).
     * Mantido por compatibilidade — soma despesas (EXPENSE).
     */
    BigDecimal sumByCategory(String category, LocalDate from, LocalDate to);

    /**
     * Soma o total de uma categoria filtrando pelo tipo informado
     * (INCOME ou EXPENSE) dentro de um período.
     */
    BigDecimal sumByCategory(String category, TransactionType type, LocalDate from, LocalDate to);

    /**
     * Saldo do período = soma(INCOME) menos soma(EXPENSE).
     * Positivo quando sobrou; negativo quando gastou mais do que recebeu.
     */
    BigDecimal balance(LocalDate from, LocalDate to);

    /**
     * Top categorias por total no período, para um tipo, ordenadas
     * do maior para o menor, limitadas a {@code limit} resultados.
     */
    List<CategoryTotal> topCategories(TransactionType type, LocalDate from, LocalDate to, int limit);
}
