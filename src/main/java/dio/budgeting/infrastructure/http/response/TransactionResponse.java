package dio.budgeting.infrastructure.http.response;

import dio.budgeting.application.output.TransactionOutput;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Resposta REST de uma transação. */
public record TransactionResponse(
        String id,
        String description,
        BigDecimal amount,
        String type,
        String category,
        LocalDate date) {

    public static TransactionResponse from(TransactionOutput o) {
        return new TransactionResponse(
                o.id(), o.description(), o.amount(), o.type(), o.category(), o.date());
    }
}
