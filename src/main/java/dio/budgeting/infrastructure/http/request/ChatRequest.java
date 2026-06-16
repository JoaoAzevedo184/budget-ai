package dio.budgeting.infrastructure.http.request;

/** Corpo da requisição para conversar com a IA por texto. */
public record ChatRequest(String message) {
}
