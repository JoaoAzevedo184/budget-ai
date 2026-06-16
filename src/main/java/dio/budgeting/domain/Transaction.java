package dio.budgeting.domain;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Modelo de domínio da transação financeira.
 *
 * Caminho B: valor em BigDecimal, categoria como texto livre (normalizada na
 * camada de aplicação), com tipo (INCOME/EXPENSE) e data.
 * Sem Lombok aqui de propósito — domínio puro, sem dependência de framework.
 */
@Getter
@Setter
public class Transaction {

    private final TransactionId id;
    private final String description;
    private final BigDecimal amount;
    private final TransactionType type;
    private final String category;
    private final LocalDate date;

    /** Construtor completo (usado ao reconstruir a partir da persistência). */
    public Transaction(TransactionId id, String description, BigDecimal amount,
                       TransactionType type, String category, LocalDate date) {
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.date = date;
    }

    /** Construtor de criação (gera um novo id). */
    public Transaction(String description, BigDecimal amount,
                       TransactionType type, String category, LocalDate date) {
        this(new TransactionId(), description, amount, type, category, date);
    }
}
