package dio.budgeting.application.validation;

import dio.budgeting.domain.TransactionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Validações executadas antes de persistir uma transação.
 *
 * A IA é uma fonte de entrada não-determinística: um comando mal interpretado
 * poderia gerar dados inválidos. Este validador protege o domínio independente
 * da origem do comando (REST ou Tool Calling).
 *
 * Regras de negócio:
 *   RN-1: valor deve ser maior que zero
 *   RN-2: descrição obrigatória, máx. 120 caracteres
 *   RN-3: data não pode ser futura
 *   RN-4: categoria obrigatória, normalizada (lowercase, trim)
 *   RN-5: tipo obrigatório
 */
@Component
public class TransactionValidator {

    private static final int MAX_DESCRIPTION_LENGTH = 120;

    /** Valida e retorna a categoria normalizada. Lança {@link ValidationException} se inválido. */
    public String validateAndNormalizeCategory(String description,
                                               BigDecimal amount,
                                               TransactionType type,
                                               String category,
                                               LocalDate date) {
        // RN-1
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("RN-1", "O valor da transação deve ser maior que zero.");
        }
        // RN-2
        if (description == null || description.isBlank()) {
            throw new ValidationException("RN-2", "A descrição é obrigatória.");
        }
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new ValidationException("RN-2",
                    "A descrição não pode exceder " + MAX_DESCRIPTION_LENGTH + " caracteres.");
        }
        // RN-5
        if (type == null) {
            throw new ValidationException("RN-5", "O tipo (INCOME/EXPENSE) é obrigatório.");
        }
        // RN-3
        if (date == null || date.isAfter(LocalDate.now())) {
            throw new ValidationException("RN-3", "A data não pode ser futura.");
        }
        // RN-4
        if (category == null || category.isBlank()) {
            throw new ValidationException("RN-4", "A categoria é obrigatória.");
        }
        return category.trim().toLowerCase();
    }
}
