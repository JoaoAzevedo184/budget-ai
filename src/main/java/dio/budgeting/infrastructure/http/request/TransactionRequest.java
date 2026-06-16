package dio.budgeting.infrastructure.http.request;

import dio.budgeting.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Corpo da requisição para criar uma transação via REST. */
public record TransactionRequest(
        String description,
        BigDecimal amount,
        TransactionType type,
        String category,
        LocalDate date) {
}
