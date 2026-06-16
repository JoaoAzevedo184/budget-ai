package dio.budgeting.application.validation;

/** Exceção de validação de regra de negócio; carrega o código da regra (ex: RN-1). */
public class ValidationException extends RuntimeException {

    private final String ruleCode;

    public ValidationException(String ruleCode, String message) {
        super(message);
        this.ruleCode = ruleCode;
    }

    public String getRuleCode() {
        return ruleCode;
    }
}
