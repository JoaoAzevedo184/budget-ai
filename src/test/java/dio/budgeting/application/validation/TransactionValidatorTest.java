package dio.budgeting.application.validation;

import dio.budgeting.domain.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testes unitários das regras RN-1..RN-5. Não dependem de rede nem do Spring. */
class TransactionValidatorTest {

    private final TransactionValidator validator = new TransactionValidator();

    private static final String DESC = "Mercado";
    private static final BigDecimal VALUE = new BigDecimal("45.00");
    private static final TransactionType TYPE = TransactionType.EXPENSE;
    private static final String CATEGORY = "  Mercado ";
    private static final LocalDate TODAY = LocalDate.now();

    @Test
    void should_normalizeCategory_when_inputIsValid() {
        String result = validator.validateAndNormalizeCategory(DESC, VALUE, TYPE, CATEGORY, TODAY);
        assertThat(result).isEqualTo("mercado"); // trim + lowercase (RN-4)
    }

    @Test
    void should_throwRN1_when_amountIsZeroOrNegative() {
        assertThatThrownBy(() ->
                validator.validateAndNormalizeCategory(DESC, BigDecimal.ZERO, TYPE, CATEGORY, TODAY))
                .isInstanceOf(ValidationException.class)
                .extracting("ruleCode").isEqualTo("RN-1");
    }

    @Test
    void should_throwRN2_when_descriptionIsBlank() {
        assertThatThrownBy(() ->
                validator.validateAndNormalizeCategory("  ", VALUE, TYPE, CATEGORY, TODAY))
                .isInstanceOf(ValidationException.class)
                .extracting("ruleCode").isEqualTo("RN-2");
    }

    @Test
    void should_throwRN2_when_descriptionTooLong() {
        String longDesc = "x".repeat(121);
        assertThatThrownBy(() ->
                validator.validateAndNormalizeCategory(longDesc, VALUE, TYPE, CATEGORY, TODAY))
                .isInstanceOf(ValidationException.class)
                .extracting("ruleCode").isEqualTo("RN-2");
    }

    @Test
    void should_throwRN3_when_dateIsFuture() {
        assertThatThrownBy(() ->
                validator.validateAndNormalizeCategory(DESC, VALUE, TYPE, CATEGORY, TODAY.plusDays(1)))
                .isInstanceOf(ValidationException.class)
                .extracting("ruleCode").isEqualTo("RN-3");
    }

    @Test
    void should_throwRN5_when_typeIsNull() {
        assertThatThrownBy(() ->
                validator.validateAndNormalizeCategory(DESC, VALUE, null, CATEGORY, TODAY))
                .isInstanceOf(ValidationException.class)
                .extracting("ruleCode").isEqualTo("RN-5");
    }

    @Test
    void should_throwRN4_when_categoryIsBlank() {
        assertThatThrownBy(() ->
                validator.validateAndNormalizeCategory(DESC, VALUE, TYPE, "  ", TODAY))
                .isInstanceOf(ValidationException.class)
                .extracting("ruleCode").isEqualTo("RN-4");
    }
}
