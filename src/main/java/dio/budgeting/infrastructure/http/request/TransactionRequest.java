package dio.budgeting.infrastructure.http.request;

import dio.budgeting.domain.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Corpo da requisição para criar uma transação via REST.
 *
 * Bean Validation atua como PRIMEIRA barreira (formato/presença) na borda HTTP.
 * A regra de negócio autoritativa continua no TransactionValidator de domínio
 * (RN-1..RN-5), que protege o domínio mesmo quando a entrada vem da IA — onde
 * não há Bean Validation. Por isso as duas camadas coexistem de propósito.
 */
public record TransactionRequest(

        @NotBlank(message = "A descrição é obrigatória.")
        @Size(max = 120, message = "A descrição não pode exceder 120 caracteres.")
        String description,

        @NotNull(message = "O valor é obrigatório.")
        @DecimalMin(value = "0.0", inclusive = false,
                message = "O valor deve ser maior que zero.")
        BigDecimal amount,

        @NotNull(message = "O tipo (INCOME/EXPENSE) é obrigatório.")
        TransactionType type,

        @NotBlank(message = "A categoria é obrigatória.")
        String category,

        @NotNull(message = "A data é obrigatória.")
        @PastOrPresent(message = "A data não pode ser futura.")
        LocalDate date) {
}