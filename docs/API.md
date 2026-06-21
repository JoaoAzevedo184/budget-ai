# Referência da API - Budget AI

Documentação dos endpoints REST. A documentação interativa (Swagger UI) fica em
`http://localhost:8080/swagger-ui.html` com a aplicação rodando.

## Convenções

- Base URL local: `http://localhost:8080`
- Datas no formato ISO `YYYY-MM-DD`
- Valores monetários em `BigDecimal` (ex.: `45.00`)
- Erros de validação retornam `422` com corpo `ProblemDetail` (RFC 7807)

## IA

### POST /api/ai/chat

Conversa com a IA por texto. A IA decide qual ferramenta chamar (Tool Calling).
Funciona com qualquer provedor.

Request:

```json
{ "message": "registra um gasto de 30 reais de uber hoje" }
```

Response 200:

```json
{ "reply": "Registrei R$ 30,00 em transporte hoje." }
```

### POST /api/ai/voice

Fluxo completo por voz: áudio -> transcrição -> IA -> TTS -> áudio. Requer um
profile com modelos de áudio (claude/gemini/nvidia/openrouter, que usam OpenAI).
No profile `ollama` retorna 501 Not Implemented.

- Consumes: `multipart/form-data`, campo `audio`
- Produces: `audio/mp3`

```bash
curl -X POST http://localhost:8080/api/ai/voice \
  -F "audio=@comando.mp3" --output resposta.mp3
```

### GET /api/ai/voice/history

Histórico dos comandos de voz processados, mais recentes primeiro.

- Query param opcional `limit` (padrão 20, máximo 100).

```bash
curl "http://localhost:8080/api/ai/voice/history?limit=10"
```

Response 200:

```json
[
  {
    "id": "f1c9...",
    "transcript": "gastei 30 reais de uber",
    "reply": "Registrei R$ 30,00 em transporte.",
    "processedAt": "2026-06-20T10:00:00"
  }
]
```

## Transações (REST direto)

### POST /api/transactions

Cria uma transação. Passa pelas validações RN-1..RN-5.

Request:

```json
{
  "description": "Mercado",
  "amount": 45.00,
  "type": "EXPENSE",
  "category": "Mercado",
  "date": "2026-06-15"
}
```

Response 201:

```json
{
  "id": "f1c9...",
  "description": "Mercado",
  "amount": 45.00,
  "type": "EXPENSE",
  "category": "mercado",
  "date": "2026-06-15"
}
```

Response 422 (ex.: RN-1, valor zero):

```json
{
  "type": "https://budget-ai/errors/validation",
  "title": "Transação inválida",
  "status": 422,
  "detail": "O valor da transação deve ser maior que zero.",
  "ruleCode": "RN-1"
}
```

### GET /api/transactions?from=&to=

Lista transações de um período.

```bash
curl "http://localhost:8080/api/transactions?from=2026-06-01&to=2026-06-30"
```

### GET /api/transactions/summary?category=&type=&from=&to=

Soma o total de uma categoria no período. O parâmetro `type` é opcional; quando
omitido, considera despesas (EXPENSE).

```bash
curl "http://localhost:8080/api/transactions/summary?category=mercado&from=2026-06-01&to=2026-06-30"
curl "http://localhost:8080/api/transactions/summary?category=salario&type=INCOME&from=2026-06-01&to=2026-06-30"
```

Response 200:
45.00

### GET /api/transactions/balance?from=&to=

Saldo do período: total de receitas menos total de despesas. Positivo quando
sobrou, negativo quando gastou mais do que recebeu.

```bash
curl "http://localhost:8080/api/transactions/balance?from=2026-06-01&to=2026-06-30"
```

Response 200:
1200.00

### GET /api/transactions/top-categories?type=&from=&to=&limit=

Categorias com maior total no período, da maior para a menor. `type` é opcional
(padrão EXPENSE) e `limit` é opcional (padrão 5).

```bash
curl "http://localhost:8080/api/transactions/top-categories?from=2026-06-01&to=2026-06-30&limit=3"
```

Response 200:

```json
[
  { "category": "mercado", "total": 100.00 },
  { "category": "transporte", "total": 60.00 }
]
```

## Regras de validação (referência)

| Código | Regra |
|---|---|
| RN-1 | valor deve ser maior que zero |
| RN-2 | descrição obrigatória, máx. 120 caracteres |
| RN-3 | data não pode ser futura |
| RN-4 | categoria obrigatória, normalizada (lowercase + trim) |
| RN-5 | tipo (INCOME/EXPENSE) obrigatório |

## Ferramentas expostas à IA (Tool Calling)

| Tool | Função |
|---|---|
| `create-transaction` | Registra uma transação |
| `list-transactions` | Lista por período |
| `sum-by-category` | Soma por categoria e período (tipo opcional) |
| `monthly-balance` | Saldo do período (receitas menos despesas) |
| `top-categories` | Categorias com maior total no período |