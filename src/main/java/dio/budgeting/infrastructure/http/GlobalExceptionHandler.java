package dio.budgeting.infrastructure.http;

import dio.budgeting.application.validation.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/**
 * Tradutor de exceções de domínio para um contrato de erro consistente
 * (ProblemDetail, RFC 7807). Vale para REST e para erros propagados pela tool.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ProblemDetail handleValidation(ValidationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Transação inválida");
        problem.setType(URI.create("https://budget-ai/errors/validation"));
        problem.setProperty("ruleCode", ex.getRuleCode());
        return problem;
    }
}
