package dio.budgeting.infrastructure.http;

import dio.budgeting.application.CreateTransactionUseCase;
import dio.budgeting.application.QueryTransactionsUseCase;
import dio.budgeting.domain.CategoryTotal;
import dio.budgeting.domain.TransactionType;
import dio.budgeting.infrastructure.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice da camada web para os endpoints da Fase 4 (sem subir a app inteira, sem IA).
 *
 * Cobre o contrato REST de:
 *   - GET /api/transactions/summary?type=INCOME  (tipo opcional repassado ao use case)
 *   - GET /api/transactions/balance
 *   - GET /api/transactions/top-categories
 */
@WebMvcTest(TransactionController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class TransactionControllerPhase4Test {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateTransactionUseCase createUseCase;

    @MockitoBean
    private QueryTransactionsUseCase queryUseCase;

    @Test
    void summary_shouldPassIncomeType_whenProvided() throws Exception {
        when(queryUseCase.sumByCategory(eq("salario"), eq(TransactionType.INCOME), any(), any()))
                .thenReturn(new BigDecimal("3000.00"));

        mockMvc.perform(get("/api/transactions/summary")
                        .param("category", "salario")
                        .param("type", "INCOME")
                        .param("from", "2026-06-01")
                        .param("to", "2026-06-30"))
                .andExpect(status().isOk())
                .andExpect(content().string("3000.00"));

        verify(queryUseCase).sumByCategory(eq("salario"), eq(TransactionType.INCOME), any(), any());
    }

    @Test
    void summary_shouldPassNullType_whenOmitted() throws Exception {
        when(queryUseCase.sumByCategory(eq("mercado"), eq((TransactionType) null), any(), any()))
                .thenReturn(new BigDecimal("45.00"));

        mockMvc.perform(get("/api/transactions/summary")
                        .param("category", "mercado")
                        .param("from", "2026-06-01")
                        .param("to", "2026-06-30"))
                .andExpect(status().isOk())
                .andExpect(content().string("45.00"));

        verify(queryUseCase).sumByCategory(eq("mercado"), eq((TransactionType) null), any(), any());
    }

    @Test
    void balance_shouldReturnValue() throws Exception {
        when(queryUseCase.balance(any(), any())).thenReturn(new BigDecimal("1200.00"));

        mockMvc.perform(get("/api/transactions/balance")
                        .param("from", "2026-06-01")
                        .param("to", "2026-06-30"))
                .andExpect(status().isOk())
                .andExpect(content().string("1200.00"));
    }

    @Test
    void balance_shouldReturnNegative_whenSpentMoreThanEarned() throws Exception {
        when(queryUseCase.balance(any(), any())).thenReturn(new BigDecimal("-50.00"));

        mockMvc.perform(get("/api/transactions/balance")
                        .param("from", "2026-06-01")
                        .param("to", "2026-06-30"))
                .andExpect(status().isOk())
                .andExpect(content().string("-50.00"));
    }

    @Test
    void topCategories_shouldReturnList() throws Exception {
        when(queryUseCase.topCategories(any(), any(), any(), any()))
                .thenReturn(List.of(
                        new CategoryTotal("mercado", new BigDecimal("100.00")),
                        new CategoryTotal("transporte", new BigDecimal("60.00"))));

        mockMvc.perform(get("/api/transactions/top-categories")
                        .param("from", "2026-06-01")
                        .param("to", "2026-06-30")
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("mercado"))
                .andExpect(jsonPath("$[0].total").value(100.00))
                .andExpect(jsonPath("$[1].category").value("transporte"));
    }
}
