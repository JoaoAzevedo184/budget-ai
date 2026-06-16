package dio.budgeting.infrastructure.persistence.entity;

import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionId;
import dio.budgeting.domain.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "transactions")
public class TransactionEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false)
    private String category;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate date;

    protected TransactionEntity() {
        // exigido pelo JPA
    }

    public TransactionEntity(UUID id, String description, BigDecimal amount,
                             TransactionType type, String category, LocalDate date) {
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.date = date;
    }

    public static TransactionEntity from(Transaction t) {
        return new TransactionEntity(
                t.getId().uuid(),
                t.getDescription(),
                t.getAmount(),
                t.getType(),
                t.getCategory(),
                t.getDate());
    }

    public Transaction toDomain() {
        return new Transaction(
                new TransactionId(id),
                description,
                amount,
                type,
                category,
                date);
    }
}
