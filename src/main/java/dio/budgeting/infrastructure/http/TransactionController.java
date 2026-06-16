package dio.budgeting.infrastructure.http;

import dio.budgeting.application.CreateTransactionUseCase;
import dio.budgeting.application.QueryTransactionsUseCase;
import dio.budgeting.infrastructure.http.request.TransactionRequest;
import dio.budgeting.infrastructure.http.response.TransactionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Endpoints REST diretos (sem IA) — úteis para validar o domínio. */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final CreateTransactionUseCase createUseCase;
    private final QueryTransactionsUseCase queryUseCase;

    public TransactionController(CreateTransactionUseCase createUseCase,
                                 QueryTransactionsUseCase queryUseCase) {
        this.createUseCase = createUseCase;
        this.queryUseCase = queryUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@RequestBody TransactionRequest req) {
        var output = createUseCase.execute(
                req.description(), req.amount(), req.type(), req.category(), req.date());
        return TransactionResponse.from(output);
    }

    @GetMapping
    public List<TransactionResponse> list(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        return queryUseCase.findByPeriod(from, to).stream()
                .map(TransactionResponse::from)
                .toList();
    }

    @GetMapping("/summary")
    public BigDecimal summary(
            @RequestParam String category,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        return queryUseCase.sumByCategory(category, from, to);
    }
}
