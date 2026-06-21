package dio.budgeting.infrastructure.ai;

import dio.budgeting.application.CreateTransactionUseCase;
import dio.budgeting.application.QueryTransactionsUseCase;
import dio.budgeting.application.output.CategoryTotalOutput;
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

    // Agregação por categoria e período, com tipo opcional (EXPENSE como padrão).
    @Tool(name = "sum-by-category",
          description = "Soma o total de uma categoria dentro de um período. "
                  + "Use type=EXPENSE para gastos (padrão) ou type=INCOME para receitas.")
    public BigDecimal sumByCategory(
            @ToolParam(description = "Categoria, ex: mercado, transporte") String category,
            @ToolParam(description = "Tipo: EXPENSE (gasto, padrão) ou INCOME (receita). "
                    + "Pode ser omitido para gastos.", required = false) TransactionType type,
            @ToolParam(description = "Data inicial (YYYY-MM-DD)") LocalDate from,
            @ToolParam(description = "Data final (YYYY-MM-DD)") LocalDate to) {
        return queryUseCase.sumByCategory(category, type, from, to);
    }

    // Saldo do período: receitas menos despesas.
    @Tool(name = "monthly-balance",
          description = "Calcula o saldo de um período: total de receitas menos total de despesas. "
                  + "Positivo significa que sobrou; negativo significa que gastou mais do que recebeu.")
    public BigDecimal monthlyBalance(
            @ToolParam(description = "Data inicial (YYYY-MM-DD)") LocalDate from,
            @ToolParam(description = "Data final (YYYY-MM-DD)") LocalDate to) {
        return queryUseCase.balance(from, to);
    }

    // Top categorias por total no período.
    @Tool(name = "top-categories",
          description = "Lista as categorias com maior total em um período, da maior para a menor. "
                  + "Use type=EXPENSE (padrão) para gastos ou type=INCOME para receitas.")
    public List<CategoryTotalOutput> topCategories(
            @ToolParam(description = "Tipo: EXPENSE (padrão) ou INCOME. Pode ser omitido.",
                    required = false) TransactionType type,
            @ToolParam(description = "Data inicial (YYYY-MM-DD)") LocalDate from,
            @ToolParam(description = "Data final (YYYY-MM-DD)") LocalDate to,
            @ToolParam(description = "Quantas categorias retornar (padrão 5)",
                    required = false) Integer limit) {
        return queryUseCase.topCategories(type, from, to, limit).stream()
                .map(CategoryTotalOutput::from)
                .toList();
    }
}
