# Arquitetura - Budget AI

Documento técnico da arquitetura. Para visão geral e instruções de uso, veja o
[README](../README.md).

## Visão em camadas

O projeto segue DDD em camadas + Clean Architecture. A regra fundamental é a
direção das dependências: camadas externas dependem das internas, nunca o
contrário.

```
        ┌──────────────────────────────────────────────┐
        │              infrastructure                  │
        │  (HTTP, JPA, IA/ChatClient, Security)        │
        │   ┌──────────────────────────────────────┐   │
        │   │            application               │   │
        │   │   (casos de uso, validação, DTOs)    │   │
        │   │   ┌──────────────────────────────┐   │   │
        │   │   │           domain             │   │   │
        │   │   │  (Transaction, Type, Id,     │   │   │
        │   │   │   Repository - porta)        │   │   │
        │   │   └──────────────────────────────┘   │   │
        │   └──────────────────────────────────────┘   │
        └──────────────────────────────────────────────┘

   domain      ← não depende de ninguém
   application ← depende só de domain
   infrastructure ← depende de application e domain
```

## Papéis por camada

### domain

Modelo de negócio puro, sem framework:

- `Transaction` - entidade de domínio (valor `BigDecimal`, `type`, `category`, `date`)
- `TransactionType` - enum INCOME/EXPENSE
- `TransactionId`, `VoiceAuditId` - identificadores fortes (UUID encapsulado)
- `CategoryTotal` - resultado de agregação por categoria
- `TransactionRepository` - porta de saída de transações
- `VoiceAudit` - registro de auditoria de voz, com a porta `VoiceAudit.Repository` aninhada

### application

Orquestra o domínio para resolver casos de uso. É a mesma porta de entrada para
REST e para a IA:

- `CreateTransactionUseCase` - valida (via `TransactionValidator`) e persiste
- `QueryTransactionsUseCase` - listagem, soma por categoria, saldo e top categorias
- `VoiceAuditUseCase` - registro e consulta do histórico de comandos de voz
- `validation/` - `TransactionValidator` (RN-1..RN-5) e `ValidationException`
- `output/` - DTOs de saída (`TransactionOutput`, `CategoryTotalOutput`)

### infrastructure

Adaptadores que conectam o mundo externo aos casos de uso:

- `http/` - controllers REST, DTOs de request/response, `GlobalExceptionHandler`
- `ai/` - `BudgetTools` (@Tool), `ChatClientConfig`, `AiController`
- `persistence/` - entidades JPA e adaptadores das portas
- `SecurityConfig` - libera endpoints e Swagger

## Princípio central

O mesmo `CreateTransactionUseCase` é chamado tanto pelo `TransactionController`
(REST) quanto pelo `BudgetTools` (Tool Calling da IA). A validação e a regra de
negócio acontecem uma única vez, no caso de uso, independente de o comando vir
de um JSON REST ou de um comando de voz interpretado pelo LLM. A IA é tratada
como mais um driver de entrada, no mesmo nível do HTTP.

## Fluxo de voz (ponta a ponta)

```
áudio.mp3

-> TranscriptionModel (Whisper)        -> texto

-> ChatClient + Tool Calling           -> LLM decide a tool e os argumentos

-> BudgetTools -> CreateTransactionUseCase

-> TransactionValidator (RN-1..RN-5) -> repository.save()

-> resposta textual da IA

-> VoiceAuditUseCase.record(transcript, reply)   (auditoria, defensiva)

-> TextToSpeechModel (TTS)             -> resposta.mp3


```
A gravação da auditoria ocorre dentro de um try/catch: uma falha ao registrar
não interrompe a resposta de áudio ao cliente.

## Tratamento de erros

`ValidationException` carrega um `ruleCode` (ex.: RN-1). O
`GlobalExceptionHandler` traduz isso para um `ProblemDetail` (RFC 7807) com
status 422, incluindo o `ruleCode` como propriedade. O handler também cobre:

- `MethodArgumentNotValidException` (Bean Validation @Valid) -> 422 com os campos
- `HttpMessageNotReadableException` (JSON malformado) -> 400
- `Exception` (não prevista) -> 500 controlado, sem expor detalhes internos

Esse contrato vale tanto para o REST quanto para erros propagados durante a
execução de uma tool.

## Persistência e migrations

O schema é criado pelo Flyway; o Hibernate apenas valida (`ddl-auto: validate`).

| Migration | Tabela | Conteúdo |
|---|---|---|
| V1 | `transactions` | id, description, amount, type, category, transaction_date + índices |
| V2 | `voice_audit` | id, transcript, reply, processed_at + índice por data |

As queries agregadas usam `COALESCE(SUM(...), 0)` para retornar 0 em vez de
null quando não há registros. O top categorias usa projeção JPA com `Pageable`
para limitar resultados.

## Seleção de provedor de IA

O provedor é escolhido pelo profile ativo (`BUDGET_AI_PROVIDER`):

| Profile | ChatModel ativo | Áudio |
|---|---|---|
| `ollama` | `ollamaChatModel` | 501 em /voice |
| `claude` | `openAiChatModel` -> api.anthropic.com | OpenAI |
| `gemini` | `openAiChatModel` -> generativelanguage.googleapis.com | OpenAI |
| `nvidia` | `openAiChatModel` -> integrate.api.nvidia.com | OpenAI |
| `openrouter` | `openAiChatModel` -> openrouter.ai | OpenAI (experimental) |

Como os starters Ollama e OpenAI coexistem no classpath, cada profile fixa
`spring.ai.model.chat` para evitar ambiguidade de bean ao injetar um único
`ChatModel` em `ChatClientConfig`.