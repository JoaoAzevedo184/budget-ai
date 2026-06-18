package dio.budgeting.infrastructure.http;

import dio.budgeting.application.validation.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.stream.Collectors;

/**
 * Tradutor de exceções para um contrato de erro consistente (ProblemDetail, RFC 7807).
 *
 * Camadas de erro:
 *   - ValidationException (domínio, RN-1..RN-5) -> 422 + ruleCode
 *   - MethodArgumentNotValidException (Bean Validation @Valid) -> 422 + campos
 *   - HttpMessageNotReadableException (JSON malformado) -> 400
 *   - Exception (qualquer outra não tratada) -> 500 controlado
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Regra de negócio do domínio (vale para REST e para a tool da IA). */
    @ExceptionHandler(ValidationException.class)
    public ProblemDetail handleValidation(ValidationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Transação inválida");
        problem.setType(URI.create("https://budget-ai/errors/validation"));
        problem.setProperty("ruleCode", ex.getRuleCode());
        return problem;
    }

    /** Bean Validation (@Valid) falhou na borda HTTP — primeira barreira. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleBeanValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                detail.isBlank() ? "Requisição inválida." : detail);
        problem.setTitle("Requisição inválida");
        problem.setType(URI.create("https://budget-ai/errors/bad-request"));
        return problem;
    }

    /** JSON malformado / corpo ilegível -> 400. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadable(HttpMessageNotReadableException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Corpo da requisição malformado ou ilegível.");
        problem.setTitle("Requisição malformada");
        problem.setType(URI.create("https://budget-ai/errors/malformed"));
        return problem;
    }

    /** Rede de segurança: qualquer exceção não prevista -> 500 controlado. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro inesperado ao processar a requisição.");
        problem.setTitle("Erro interno");
        problem.setType(URI.create("https://budget-ai/errors/internal"));
        // Não expõe a mensagem da exceção ao cliente (evita vazar detalhes internos).
        return problem;
    }
}