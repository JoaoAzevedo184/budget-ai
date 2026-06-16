package dio.budgeting.infrastructure.ai;

import dio.budgeting.application.CreateTransactionUseCase;
import dio.budgeting.application.QueryTransactionsUseCase;
import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.domain.TransactionType;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Ferramentas (Tool Calling) expostas ao modelo de linguagem.
 *
 * Cada método @Tool delega para um caso de uso da camada application — a IA
 * decide QUAL função chamar e com quais argumentos, mas a regra de negócio e a
 * validação acontecem no código, não no modelo.
 */
@Component
public class BudgetTools {

    private final CreateTransactionUseCase createUseCase;
    private final QueryTransactionsUseCase queryUseCase;

    public BudgetTools(CreateTransactionUseCase createUseCase,
                       QueryTransactionsUseCase queryUseCase) {
        this.createUseCase = createUseCase;
        this.queryUseCase = queryUseCase;
    }

    @Tool(name = "create-transaction",
          description = "Registra uma nova transação financeira (receita ou despesa)")
    public TransactionOutput createTransaction(
            @ToolParam(description = "Descrição curta da transação") String description,
            @ToolParam(description = "Valor em reais, sempre positivo") BigDecimal amount,
            @ToolParam(description = "Tipo: INCOME (receita) ou EXPENSE (despesa)") TransactionType type,
            @ToolParam(description = "Categoria, ex: mercado, transporte, salário") String category,
            @ToolParam(description = "Data da transação no formato YYYY-MM-DD") LocalDate date) {
        return createUseCase.execute(description, amount, type, category, date);
    }

    @Tool(name = "list-transactions",
          description = "Lista as transações de um período")
    public List<TransactionOutput> listTransactions(
            @ToolParam(description = "Data inicial (YYYY-MM-DD)") LocalDate from,
            @ToolParam(description = "Data final (YYYY-MM-DD)") LocalDate to) {
        return queryUseCase.findByPeriod(from, to);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // ⭐ EVOLUÇÃO: nova tool de agregação por categoria + período
    // Permite perguntas como "quanto gastei com transporte essa semana?"
    // ──────────────────────────────────────────────────────────────────────────
    @Tool(name = "sum-by-category",
          description = "Soma o total gasto em uma categoria dentro de um período")
    public BigDecimal sumByCategory(
            @ToolParam(description = "Categoria, ex: mercado, transporte") String category,
            @ToolParam(description = "Data inicial (YYYY-MM-DD)") LocalDate from,
            @ToolParam(description = "Data final (YYYY-MM-DD)") LocalDate to) {
        return queryUseCase.sumByCategory(category, from, to);
    }
}
