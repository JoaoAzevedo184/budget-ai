package dio.budgeting.application;

import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.application.validation.TransactionValidator;
import dio.budgeting.domain.TransactionRepository;
import dio.budgeting.domain.TransactionType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Caso de uso de criação de transação.
 *
 * Chamado tanto pelo REST quanto pela tool da IA. Valida ANTES de persistir
 * (ver {@link TransactionValidator}) — a regra de negócio não depende da origem.
 */
@Service
public class CreateTransactionUseCase {

    private final TransactionRepository repository;
    private final TransactionValidator validator;

    public CreateTransactionUseCase(TransactionRepository repository,
                                    TransactionValidator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    public TransactionOutput execute(String description,
                                     BigDecimal amount,
                                     TransactionType type,
                                     String category,
                                     LocalDate date) {
        String normalizedCategory =
                validator.validateAndNormalizeCategory(description, amount, type, category, date);

        Transaction saved = repository.save(
                new Transaction(description, amount, type, normalizedCategory, date));

        return TransactionOutput.from(saved);
    }
}
