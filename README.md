# 🎙️ Budget AI - Assistente Financeiro por Voz com Spring AI

API em Spring Boot 3.5 + Spring AI que recebe comandos de voz, transcreve o
áudio, interpreta a intenção com um modelo de linguagem e executa funções da
aplicação (criar e consultar transações financeiras) via Tool Calling.

Desenvolvido para o Desafio de Projeto Spring AI da [DIO](https://www.dio.me/),
reaproveitando a arquitetura em camadas proposta pelo expert Thiago Poiani.

## Sumário

- [O que o projeto faz](#o-que-o-projeto-faz)
- [Arquitetura](#arquitetura)
- [Stack](#stack)
- [Provedores de IA](#provedores-de-ia)
- [Funcionalidades](#funcionalidades)
- [Como executar](#como-executar)
- [Como testar](#como-testar)
- [Endpoints](#endpoints)
- [Testes automatizados](#testes-automatizados)

## O que o projeto faz

Budget AI é uma API de orçamento pessoal controlada por voz. A pessoa fala um
comando como "registra um gasto de 45 reais no mercado hoje" ou "quanto gastei
esse mês?", e a aplicação:

1. transcreve a fala em texto;
2. interpreta a intenção usando um LLM;
3. chama a função correta (criar transação, listar, somar por categoria, saldo,
   top categorias);
4. devolve uma resposta em texto e/ou áudio.

A IA orquestra qual função chamar e com quais argumentos, mas quem executa e
valida a regra de negócio é o código. Os argumentos vindos do modelo passam
pela mesma validação de domínio aplicada à entrada REST.

## Arquitetura

Arquitetura em camadas (DDD + Clean Architecture). A IA entra pela camada de
infraestrutura como um driver, no mesmo nível do REST.

```
src/main/java/dio/budgeting/
├── domain/                         # Regras puras, sem framework
│   ├── Transaction.java
│   ├── TransactionType.java        # INCOME | EXPENSE
│   ├── TransactionId.java
│   ├── TransactionRepository.java  # porta de saída
│   ├── CategoryTotal.java          # agregação categoria + total
│   ├── VoiceAudit.java             # auditoria + porta de saída aninhada
│   └── VoiceAuditId.java
├── application/                    # Casos de uso (REST e IA usam os mesmos)
│   ├── CreateTransactionUseCase.java
│   ├── QueryTransactionsUseCase.java
│   ├── VoiceAuditUseCase.java
│   ├── output/
│   └── validation/                 # TransactionValidator (RN-1..RN-5)
└── infrastructure/
├── SecurityConfig.java
├── ai/                         # BudgetTools, ChatClientConfig, AiController
├── http/                       # controllers, DTOs, GlobalExceptionHandler
└── persistence/                # entidades e adaptadores JPA
```
O mesmo `CreateTransactionUseCase` é chamado pelo `TransactionController` (REST)
e pelo `BudgetTools` (Tool Calling). A regra de negócio não é duplicada.
Detalhes em [docs/ARQUITETURA.md](docs/ARQUITETURA.md).

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 3.5.15 |
| IA | Spring AI 1.1.8 |
| Build | Maven (wrapper incluído) |
| Persistência | Spring Data JPA + H2 (dev) / PostgreSQL (compose) |
| Migrations | Flyway |
| IA local | Ollama |
| IA cloud | Claude, Gemini, NVIDIA NIM, OpenRouter |
| Áudio | OpenAI Whisper (STT) + TTS |
| Docs | springdoc-openapi (Swagger UI) |
| Testes | JUnit 5 + AssertJ + Mockito + Spring Boot Test |
| Container | Docker Compose |

## Provedores de IA

A aplicação é provider-agnostic. Em desenvolvimento usa Ollama local (sem custo,
offline); para produção, qualquer provedor OpenAI-compatible é plugado trocando
`base-url`, `api-key` e `model`. O provedor é escolhido pelo profile ativo via
`BUDGET_AI_PROVIDER`.

| Provedor | Profile | Papel | Áudio |
|---|---|---|---|
| Ollama | `ollama` (default) | Dev / testes locais | Não (use `/api/ai/chat`) |
| Claude | `claude` | Raciocínio / Tool Calling | Via OpenAI |
| Gemini | `gemini` | Multimodal | Via OpenAI |
| NVIDIA NIM | `nvidia` | Inferência acelerada | Via OpenAI |
| OpenRouter | `openrouter` | Chat (voz experimental) | Via OpenAI |

Os provedores cloud usam o protocolo OpenAI-compatible, então o starter OpenAI
aponta `base-url` para o endpoint de cada um. Como Ollama e OpenAI coexistem no
classpath, cada profile fixa o `ChatModel` ativo (`spring.ai.model.chat`) para
evitar ambiguidade de bean. Voz (Whisper/TTS) é coberta pela OpenAI; no profile
Ollama, `/api/ai/voice` responde 501.

## Funcionalidades

### Tool Calling

| Tool | Função |
|---|---|
| `create-transaction` | Registra uma transação (receita ou despesa) |
| `list-transactions` | Lista transações de um período |
| `sum-by-category` | Soma por categoria e período, com tipo opcional |
| `monthly-balance` | Saldo do período (receitas menos despesas) |
| `top-categories` | Categorias com maior total no período |

### Validação de domínio

O `TransactionValidator` roda antes da persistência, protegendo o domínio mesmo
quando o comando vem da IA:

| Regra | Descrição |
|---|---|
| RN-1 | valor deve ser maior que zero |
| RN-2 | descrição obrigatória, máx. 120 caracteres |
| RN-3 | data não pode ser futura |
| RN-4 | categoria obrigatória, normalizada (lowercase + trim) |
| RN-5 | tipo (INCOME/EXPENSE) obrigatório |

Erros retornam `ProblemDetail` (RFC 7807) com o `ruleCode`, tanto no REST quanto
via tool.

### Auditoria de voz

Cada comando de voz processado é registrado (transcrição + resposta) na tabela
`voice_audit`. O histórico fica disponível em `GET /api/ai/voice/history`. A
gravação é defensiva: falha na auditoria não interrompe a resposta de áudio.

## Como executar

### Pré-requisitos

- Java 21
- [Ollama](https://ollama.com/) rodando localmente (modo dev)
- Docker (opcional, para PostgreSQL)

### 1. Subir o Ollama e baixar um modelo com suporte a tools

```bash
ollama serve
ollama pull qwen2.5
```

### 2. Rodar (Ollama, padrão, sem chave)

```bash
export BUDGET_AI_PROVIDER=ollama
./mvnw spring-boot:run
```

### 3. Rodar com provedor cloud (exemplo: Gemini)

```bash
export BUDGET_AI_PROVIDER=gemini
export GEMINI_API_KEY="sua_chave"
export OPENAI_API_KEY="sua_chave_openai"   # necessária apenas para voz
./mvnw spring-boot:run
```

Equivalentes: `CLAUDE_API_KEY`, `NVIDIA_API_KEY`, `OPENROUTER_API_KEY`.

### 4. Testes

```bash
./mvnw test
```

## Como testar

IA por texto (Tool Calling, qualquer provedor):

```bash
curl -X POST http://localhost:8080/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "registra um gasto de 30 reais de uber hoje"}'
```

REST direto (valida o domínio):

```bash
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"description":"Mercado","amount":45.00,"type":"EXPENSE","category":"mercado","date":"2026-06-15"}'
```

Voz (profile cloud com áudio):

```bash
curl -X POST http://localhost:8080/api/ai/voice \
  -F "audio=@comando.mp3" --output resposta.mp3
```

Collection do Postman em
[docs/Collection/Budget-AI.postman_collection.json](docs/Collection/Budget-AI.postman_collection.json).

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/ai/chat` | Texto -> IA -> texto |
| `POST` | `/api/ai/voice` | Áudio -> IA -> áudio (profile cloud) |
| `GET` | `/api/ai/voice/history?limit=` | Histórico de comandos de voz |
| `POST` | `/api/transactions` | Cria transação (com validações) |
| `GET` | `/api/transactions?from=&to=` | Lista por período |
| `GET` | `/api/transactions/summary?category=&type=&from=&to=` | Soma por categoria |
| `GET` | `/api/transactions/balance?from=&to=` | Saldo do período |
| `GET` | `/api/transactions/top-categories?type=&from=&to=&limit=` | Top categorias |

Swagger: `http://localhost:8080/swagger-ui.html`. Referência completa em
[docs/API.md](docs/API.md).

## Testes automatizados

Cobertura sem dependência de rede ou IA real:

- unitários dos casos de uso (`CreateTransactionUseCase`,
  `QueryTransactionsUseCase`, `VoiceAuditUseCase`) com repositório mockado;
- unitários do `TransactionValidator` (RN-1..RN-5);
- slice web (`@WebMvcTest`) dos controllers e do `GlobalExceptionHandler`;
- slice de persistência (`@DataJpaTest`) das queries JPA.

O teste de contexto (`@SpringBootTest`) e o de integração de voz são
condicionais (`RUN_CONTEXT_TEST`, `RUN_VOICE_IT`), para não exigir
infraestrutura no CI. Detalhes em [docs/CI.md](docs/CI.md).

## Licença

MIT, veja [LICENSE](LICENSE).

Feito por [João Victor Azevedo de Sena](https://github.com/JoaoAzevedo184) -
Desafio Spring AI - DIO / NTT DATA