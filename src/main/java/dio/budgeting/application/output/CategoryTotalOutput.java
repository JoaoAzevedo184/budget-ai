package dio.budgeting.application.output;

import dio.budgeting.domain.CategoryTotal;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** DTO de saída de uma agregação por categoria (usado por REST e pela IA). */
public record CategoryTotalOutput(String category, BigDecimal total) {

    public static CategoryTotalOutput from(CategoryTotal c) {
        BigDecimal value = c.total() == null ? BigDecimal.ZERO : c.total();
        return new CategoryTotalOutput(c.category(), value.setScale(2, RoundingMode.HALF_UP));
    }
}
