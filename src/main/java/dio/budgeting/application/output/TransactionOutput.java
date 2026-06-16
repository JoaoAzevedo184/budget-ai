package dio.budgeting.application.output;

import dio.budgeting.application.Transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/** DTO de saída de uma transação (usado por REST e pela IA). */
public record TransactionOutput(
        String id,
        String description,
        BigDecimal amount,
        String type,
        String category,
        LocalDate date) {

    public static TransactionOutput from(Transaction t) {
        return new TransactionOutput(
                t.getId().uuid().toString(),
                t.getDescription(),
                t.getAmount().setScale(2, RoundingMode.HALF_UP),
                t.getType().name(),
                t.getCategory(),
                t.getDate());
    }
}
