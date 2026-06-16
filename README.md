# 🎙️ Budget AI — Assistente Financeiro por Voz com Spring AI

API inteligente em **Spring Boot 3.5 + Spring AI** que recebe comandos de **voz**, transcreve o áudio, interpreta a intenção com um modelo de linguagem e executa **funções reais** da aplicação (criar e consultar transações financeiras) via **Tool Calling**.

> Desenvolvido para o **Desafio de Projeto Spring AI** da [DIO](https://www.dio.me/) (trilha NTT DATA — Backend Java com Spring AI). Construído do zero, reaproveitando a arquitetura em camadas proposta pelo expert Thiago Poiani.

---

## 📋 Sumário

- [O que o projeto faz](#-o-que-o-projeto-faz)
- [Fluxo principal](#-fluxo-principal)
- [Arquitetura](#-arquitetura)
- [Stack / tecnologias](#-stack--tecnologias)
- [Provedores de IA](#-provedores-de-ia-suportados)
- [Minhas evoluções](#-minhas-evoluções-o-diferencial-da-entrega)
- [Como executar](#-como-executar)
- [Como testar](#-como-testar-o-fluxo-principal)
- [Endpoints](#-endpoints-rest)
- [O que aprendi](#-o-que-aprendi)

---

## 🎯 O que o projeto faz

O **Budget AI** é uma API de orçamento pessoal controlada por voz. Em vez de preencher formulários, a pessoa **fala** um comando como *"registra um gasto de 45 reais no mercado hoje"* ou *"quanto eu gastei esse mês?"*, e a aplicação:

1. transcreve a fala em texto;
2. entende a intenção usando um LLM;
3. chama a função correta da aplicação (criar transação, consultar, somar por categoria);
4. devolve uma resposta em texto e/ou áudio.

O foco **não é só usar IA**, e sim conectar IA a uma aplicação real **sem furar os limites de domínio e casos de uso** — a IA orquestra, mas quem executa e valida a regra de negócio é o código.

---

## 🔄 Fluxo principal

```
┌──────────┐   áudio    ┌────────────────────┐   texto   ┌───────────────┐
│  Cliente │ ─────────► │ TranscriptionModel │ ──────►   │  ChatClient   │
└──────────┘            └────────────────────┘           │  (LLM + Tools)│
                                                         └───────┬───────┘
                                                                 │ tool call
                                                                 ▼
                                                       ┌───────────────────┐
                                                       │  Casos de uso     │
                                                       │ (criar/consultar/ │
                                                       │  somar)           │
                                                       └─────────┬─────────┘
                                                                 │
                                       ┌─────────────────────────┼───────────────────────┐
                                       ▼                                                 ▼
                                ┌────────────┐                              ┌──────────────────┐
                                │ Repositório│                              │ TextToSpeechModel│
                                │   (JPA)    │                              │  → resposta MP3  │
                                └────────────┘                              └──────────────────┘
```

---

## 🏛️ Arquitetura

Arquitetura em camadas (DDD + Clean Architecture). A IA é apenas mais um *driver* que entra pela camada de infraestrutura — exatamente como o REST.

```
src/main/java/dio/budgeting/
├── domain/                         # Regras puras, sem framework
│   ├── Transaction.java            #   valor BigDecimal, type, category, date
│   ├── TransactionType.java        #   INCOME | EXPENSE
│   ├── TransactionId.java          #   strong-typed id (UUID)
│   └── TransactionRepository.java  #   porta de saída
├── application/                    # Casos de uso (REST + IA usam os mesmos)
│   ├── CreateTransactionUseCase.java
│   ├── QueryTransactionsUseCase.java
│   ├── output/TransactionOutput.java
│   └── validation/                 # ✅ EVOLUÇÃO: validações antes de salvar
│       ├── TransactionValidator.java
│       └── ValidationException.java
└── infrastructure/
    ├── SecurityConfig.java         # libera endpoints + Swagger
    ├── ai/
    │   ├── BudgetTools.java        # @Tool expostas ao modelo
    │   ├── ChatClientConfig.java   # monta o ChatClient com tools + prompt
    │   └── AiController.java       # /api/ai/chat e /api/ai/voice
    ├── http/
    │   ├── TransactionController.java       # /api/transactions
    │   ├── GlobalExceptionHandler.java      # ProblemDetail (RFC 7807)
    │   ├── request/  response/
    └── persistence/
        ├── entity/TransactionEntity.java
        └── repository/ (JpaTransactionRepository, TransactionEntityRepository)
```

**Princípio-chave:** o mesmo `CreateTransactionUseCase` é chamado pelo `TransactionController` (REST) e pelo `BudgetTools` (Tool Calling). A regra de negócio não é duplicada.

---

## 🧰 Stack / tecnologias

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 3.5.15 |
| IA | Spring AI 1.1.8 |
| Build | Maven (wrapper incluído) |
| Persistência | Spring Data JPA + H2 (dev) / PostgreSQL (compose) |
| IA local | Ollama (homelab) |
| IA cloud | Claude (Anthropic), Gemini (Google), NVIDIA NIM |
| Áudio | OpenAI Whisper (STT) + TTS |
| Docs | springdoc-openapi (Swagger UI) |
| Testes | JUnit 5 + AssertJ + Spring Boot Test |
| Container | Docker Compose |

---

## 🤖 Provedores de IA suportados

A aplicação é **provider-agnostic**. Em desenvolvimento uso **Ollama local** (sem custo, offline, no meu homelab); para produção, qualquer provedor *OpenAI-compatible* é plugado só trocando `base-url`, `api-key` e `model`. O provedor é escolhido pelo profile ativo via `BUDGET_AI_PROVIDER`.

| Provedor | Profile | Papel | Áudio (voz) |
|---|---|---|---|
| **Ollama** | `ollama` (default) | Dev / testes locais | ❌ (use `/api/ai/chat`) |
| **Claude** | `claude` | Expert — raciocínio / Tool Calling | ✅ via OpenAI |
| **Gemini** | `gemini` | Expert — multimodal | ✅ via OpenAI |
| **NVIDIA NIM** | `nvidia` | Expert — inferência acelerada | ✅ via OpenAI |

> ⚠️ **Nota técnica:** os provedores cloud falam o protocolo **OpenAI-compatible**, então uso o starter OpenAI apontando `base-url` para o endpoint de cada um. Como Ollama e OpenAI coexistem no classpath, cada profile fixa explicitamente o `ChatModel` ativo (`spring.ai.model.chat`) para evitar ambiguidade de bean. Voz (Whisper/TTS) é coberta pela OpenAI; em modo Ollama puro, `/api/ai/voice` responde `501` e você usa o endpoint de texto.

---

## ⭐ Minhas evoluções (o diferencial da entrega)

### 1. Nova ferramenta no Tool Calling — `sumByCategory`

Permite perguntas como *"quanto gastei com transporte essa semana?"*. O modelo extrai categoria e intervalo de datas da fala e chama a função, que agrega no banco via query JPA com `SUM` + `COALESCE` (retorna 0 em vez de null).

### 2. Validações antes de salvar — `TransactionValidator`

Roda **antes da persistência**, protegendo o domínio mesmo quando o comando vem da IA (não-determinística):

| Regra | Descrição |
|---|---|
| RN-1 | valor deve ser maior que zero |
| RN-2 | descrição obrigatória, máx. 120 caracteres |
| RN-3 | data não pode ser futura |
| RN-4 | categoria obrigatória, normalizada (lowercase + trim) |
| RN-5 | tipo (INCOME/EXPENSE) obrigatório |

Erros retornam um contrato consistente (`ProblemDetail`, RFC 7807) com o `ruleCode`, tanto no REST quanto via tool.

---

## ▶️ Como executar

### Pré-requisitos
- Java 21
- [Ollama](https://ollama.com/) rodando localmente (modo dev)
- Docker (opcional, para PostgreSQL)

### 1. Subir o Ollama e baixar um modelo com suporte a tools
```bash
ollama serve
ollama pull qwen2.5
```

### 2. Rodar (Ollama, padrão — sem chave)
```bash
export BUDGET_AI_PROVIDER=ollama
./mvnw spring-boot:run
```

### 3. Rodar com expert na nuvem (ex.: Gemini)
```bash
export BUDGET_AI_PROVIDER=gemini
export GEMINI_API_KEY="sua_chave"
export OPENAI_API_KEY="sua_chave_openai"   # necessária só para voz
./mvnw spring-boot:run
```
Equivalentes: `CLAUDE_API_KEY`, `NVIDIA_API_KEY`.

### 4. Testes
```bash
./mvnw test
```

---

## 🧪 Como testar o fluxo principal

**IA por texto (Tool Calling, qualquer provedor):**
```bash
curl -X POST http://localhost:8080/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "registra um gasto de 30 reais de uber hoje"}'
```

**REST direto (valida o domínio):**
```bash
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"description":"Mercado","amount":45.00,"type":"EXPENSE","category":"mercado","date":"2026-06-15"}'
```

**Voz (perfil cloud com áudio):**
```bash
curl -X POST http://localhost:8080/api/ai/voice \
  -F "audio=@comando.mp3" --output resposta.mp3
```

Exemplos prontos em [`docs/requests.http`](docs/requests.http).

---

## 🌐 Endpoints REST

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/ai/chat` | Texto → IA → texto |
| `POST` | `/api/ai/voice` | Áudio → IA → áudio (perfil cloud) |
| `POST` | `/api/transactions` | Cria transação (com validações) |
| `GET` | `/api/transactions?from=&to=` | Lista por período |
| `GET` | `/api/transactions/summary?category=&from=&to=` | Soma por categoria |

Swagger: `http://localhost:8080/swagger-ui.html`

---

## 💡 O que aprendi

- **Tool Calling não é mágica.** O modelo só decide *qual* função chamar e com *quais* argumentos — quem executa e valida é o código. Manter casos de uso bem definidos é o que torna isso seguro.
- **IA é entrada não confiável.** Tratei os argumentos vindos do LLM com a mesma desconfiança de um input anônimo da internet — daí a camada de validação.
- **Abstração de provider vale muito.** Desenvolver de graça com Ollama local e plugar um expert na nuvem só trocando configuração mostra o poder do `ChatModel` como interface portável.
- **Coexistência de starters** (Ollama + OpenAI) exige fixar o `ChatModel` ativo por profile para evitar ambiguidade de bean.
- **Separar transcrição, raciocínio e síntese de voz** deixa cada parte testável isoladamente.

---

## 📄 Licença
MIT — veja [LICENSE](LICENSE).

---

<p align="center">
  Feito por <a href="https://github.com/JoaoAzevedo184">João Victor Azevedo de Sena</a> ·
  Desafio Spring AI · DIO / NTT DATA
</p>
