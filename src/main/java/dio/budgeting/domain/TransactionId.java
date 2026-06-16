package dio.budgeting.domain;

import java.util.UUID;

/** Identificador forte (strong-typed id) de uma transação. */
public record TransactionId(UUID uuid) {
    public TransactionId() {
        this(UUID.randomUUID());
    }
}
