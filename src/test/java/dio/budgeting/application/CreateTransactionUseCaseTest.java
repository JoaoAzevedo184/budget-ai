package dio.budgeting.application;

import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.application.validation.TransactionValidator;
import dio.budgeting.application.validation.ValidationException;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários do caso de uso de criação.
 *
 * Usa um TransactionValidator REAL (a regra é simples e determinística) e um
 * repositório MOCKADO (Mockito), sem rede nem Spring. Garante que:
 *   - valida ANTES de salvar (input inválido nunca chega ao repositório);
 *   - a categoria é normalizada (lowercase + trim) antes da persistência.
 */
@ExtendWith(MockitoExtension.class)
class CreateTransactionUseCaseTest {

    @Mock
    private dio.budgeting.domain.TransactionRepository repository;

    // validator real: queremos exercitar a normalização e as RNs de verdade
    private final TransactionValidator validator = new TransactionValidator();

    private CreateTransactionUseCase useCase;

    private CreateTransactionUseCase useCase() {
        if (useCase == null) {
            useCase = new CreateTransactionUseCase(repository, validator);
        }
        return useCase;
    }

    @Test
    void should_normalizeCategory_and_save_when_inputIsValid() {
        // o repositório devolve a própria transação recebida (eco)
        when(repository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionOutput output = useCase().execute(
                "Uber",
                new BigDecimal("30.00"),
                TransactionType.EXPENSE,
                "  Transporte ",   // com espaços e maiúscula de propósito
                LocalDate.now());

        // a saída reflete a categoria normalizada
        assertThat(output.category()).isEqualTo("transporte");
        assertThat(output.description()).isEqualTo("Uber");
        assertThat(output.type()).isEqualTo("EXPENSE");

        // e o que foi efetivamente salvo também está normalizado
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCategory()).isEqualTo("transporte");
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("30.00");
    }

    @Test
    void should_notSave_when_amountIsZero() {
        // RN-1 deve barrar ANTES de qualquer save
        assertThatThrownBy(() -> useCase().execute(
                "Inválido",
                BigDecimal.ZERO,
                TransactionType.EXPENSE,
                "teste",
                LocalDate.now()))
                .isInstanceOf(ValidationException.class)
                .extracting("ruleCode").isEqualTo("RN-1");

        verify(repository, never()).save(any());
    }

    @Test
    void should_notSave_when_dateIsFuture() {
        // RN-3 deve barrar ANTES de qualquer save
        assertThatThrownBy(() -> useCase().execute(
                "Futuro",
                new BigDecimal("10.00"),
                TransactionType.EXPENSE,
                "teste",
                LocalDate.now().plusDays(1)))
                .isInstanceOf(ValidationException.class)
                .extracting("ruleCode").isEqualTo("RN-3");

        verify(repository, never()).save(any());
    }
}