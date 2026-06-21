# Arquitetura - Budget AI

Documento técnico da arquitetura. Para visão geral e instruções de uso, veja o
[README](../README.md).

## Visão em camadas

O projeto segue **DDD em camadas + Clean Architecture**. A regra fundamental é a
**direção das dependências**: camadas de fora dependem das de dentro, nunca o
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
Modelo de negócio puro, sem framework. Contém:
- `Transaction` — entidade de domínio (valor `BigDecimal`, `type`, `category`, `date`)
- `TransactionType` — enum INCOME/EXPENSE
- `TransactionId` — identificador forte (UUID encapsulado)
- `TransactionRepository` — **porta de saída** (interface que a infra implementa)

### application
Orquestra o domínio para resolver casos de uso. É **a mesma porta de entrada para
REST e para a IA**:
- `CreateTransactionUseCase` — valida (via `TransactionValidator`) e persiste
- `QueryTransactionsUseCase` — lista por período e soma por categoria
- `validation/` — `TransactionValidator` (RN-1..RN-5) e `ValidationException`
- `output/TransactionOutput` — DTO de saída

### infrastructure
Adaptadores que conectam o mundo externo aos casos de uso:
- `http/` — controllers REST, DTOs de request/response, `GlobalExceptionHandler`
- `ai/` — `BudgetTools` (@Tool), `ChatClientConfig`, `AiController`
- `persistence/` — `TransactionEntity`, repositórios JPA (adaptador da porta)
- `SecurityConfig` — libera endpoints e Swagger

## O princípio central do projeto

> O mesmo `CreateTransactionUseCase` é chamado **tanto** pelo `TransactionController`
> (REST) **quanto** pelo `BudgetTools` (Tool Calling da IA).

Isso significa que a validação e a regra de negócio acontecem **uma única vez**, no
caso de uso — não importa se o comando veio de um JSON REST ou de um comando de voz
interpretado pelo LLM. A IA é tratada como só mais um "driver" de entrada, no mesmo
nível do HTTP.

## Fluxo de voz (ponta a ponta)

```
áudio.mp3
  │
  ▼  TranscriptionModel (Whisper)
"gastei 45 reais no mercado hoje"   (texto)
  │
  ▼  ChatClient + Tool Calling
LLM decide chamar create-transaction(description, amount, type, category, date)
  │
  ▼  BudgetTools → CreateTransactionUseCase
TransactionValidator valida (RN-1..RN-5) → repository.save()
  │
  ▼  resposta textual da IA
"Registrei R$ 45,00 em mercado."
  │
  ▼  TextToSpeechModel (TTS)
resposta.mp3
```

## Tratamento de erros

`ValidationException` carrega um `ruleCode` (ex.: `RN-1`). O
`GlobalExceptionHandler` traduz isso para um `ProblemDetail` (RFC 7807) com status
422, incluindo o `ruleCode` como propriedade. Esse contrato vale tanto para o REST
quanto para erros propagados durante a execução de uma tool.

## Seleção de provedor de IA

O provedor é escolhido pelo **profile ativo** (`BUDGET_AI_PROVIDER`):

| Profile | ChatModel ativo | Áudio |
|---|---|---|
| `ollama` | `ollamaChatModel` | — (501 em /voice) |
| `claude` | `openAiChatModel` → api.anthropic.com | OpenAI |
| `gemini` | `openAiChatModel` → generativelanguage.googleapis.com | OpenAI |
| `nvidia` | `openAiChatModel` → integrate.api.nvidia.com | OpenAI |

Como os starters Ollama e OpenAI coexistem no classpath, cada profile fixa
`spring.ai.model.chat` para evitar ambiguidade de bean ao injetar um único
`ChatModel` em `ChatClientConfig`.
