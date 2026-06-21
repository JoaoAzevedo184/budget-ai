package dio.budgeting.domain;

import java.math.BigDecimal;

/**
 * Resultado de agregação por categoria (categoria + total somado).
 * Usado pela consulta de "top categorias".
 */
public record CategoryTotal(String category, BigDecimal total) {
}
