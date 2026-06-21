package dio.budgeting.infrastructure.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import dio.budgeting.application.CreateTransactionUseCase;
import dio.budgeting.application.QueryTransactionsUseCase;
import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.application.validation.ValidationException;
import dio.budgeting.infrastructure.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de slice da camada web (sem subir a aplicação inteira nem IA).
 *
 * Importa o SecurityConfig real (libera os endpoints) e mocka os casos de uso.
 * Valida o contrato REST e a tradução de erros pelo GlobalExceptionHandler
 * (ValidationException -> 422 + ProblemDetail com ruleCode).
 */
@WebMvcTest(TransactionController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreateTransactionUseCase createUseCase;

    @MockitoBean
    private QueryTransactionsUseCase queryUseCase;

    @Test
    void post_shouldReturn201_andNormalizedBody_whenValid() throws Exception {
        var output = new TransactionOutput(
                "f1c90000-0000-0000-0000-000000000000",
                "Mercado",
                new BigDecimal("45.00"),
                "EXPENSE",
                "mercado",
                LocalDate.of(2026, 6, 15));

        when(createUseCase.execute(anyString(), any(), any(), anyString(), any()))
                .thenReturn(output);

        var body = Map.of(
                "description", "Mercado",
                "amount", 45.00,
                "type", "EXPENSE",
                "category", "Mercado",
                "date", "2026-06-15");

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("mercado"))
                .andExpect(jsonPath("$.description").value("Mercado"))
                .andExpect(jsonPath("$.type").value("EXPENSE"));
    }

    @Test
    void post_shouldReturn422_fromBeanValidation_whenAmountIsZero() throws Exception {
        // Com @Valid no controller, o Bean Validation barra ANTES do use case.
        // A requisição com amount=0 nem chega ao TransactionValidator de domínio.
        var body = Map.of(
                "description", "Inválido",
                "amount", 0,
                "type", "EXPENSE",
                "category", "teste",
                "date", "2026-06-15");

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity())   // 422
                .andExpect(jsonPath("$.title").value("Requisição inválida"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("valor")));
    }

    @Test
    void getSummary_shouldReturnTotal() throws Exception {
        when(queryUseCase.sumByCategory(eq("mercado"), any(), any(), any()))
                .thenReturn(new BigDecimal("45.00"));

        mockMvc.perform(get("/api/transactions/summary")
                        .param("category", "mercado")
                        .param("from", "2026-06-01")
                        .param("to", "2026-06-30"))
                .andExpect(status().isOk())
                .andExpect(content().string("45.00"));
    }
}