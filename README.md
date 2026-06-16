# 🎙️ Budget AI — Assistente Financeiro por Voz com Spring AI

API inteligente em **Spring Boot 3 + Spring AI** que recebe comandos de **voz**, transcreve o áudio, interpreta a intenção com um modelo de linguagem e executa **funções reais** da aplicação (criar e consultar transações financeiras) via **Tool Calling**.

> Projeto desenvolvido para o **Desafio de Projeto Spring AI** da [DIO](https://www.dio.me/) (trilha NTT DATA — Backend Java com Spring AI). Construído do zero, reaproveitando a arquitetura em camadas proposta pelo expert Thiago Poiani.

---

## 📋 Sumário

- [O que o projeto faz](#-o-que-o-projeto-faz)
- [Fluxo principal](#-fluxo-principal)
- [Arquitetura](#-arquitetura)
- [Stack / tecnologias](#-stack--tecnologias)
- [Provedores de IA suportados](#-provedores-de-ia-suportados)
- [Minhas evoluções](#-minhas-evoluções-o-diferencial-da-entrega)
- [Como executar](#-como-executar)
- [Como testar o fluxo principal](#-como-testar-o-fluxo-principal)
- [Endpoints REST](#-endpoints-rest)
- [O que aprendi](#-o-que-aprendi)

---

## 🎯 O que o projeto faz

O **Budget AI** é uma API de orçamento pessoal controlada por voz. Em vez de preencher formulários, a pessoa **fala** um comando como *"registra um gasto de 45 reais no mercado hoje"* ou *"quanto eu gastei esse mês?"*, e a aplicação:

1. transcreve a fala em texto;
2. entende a intenção usando um LLM;
3. chama a função correta da aplicação (criar transação, consultar saldo, etc.);
4. devolve uma resposta em texto e/ou áudio.

O foco **não é só usar IA**, e sim conectar IA a uma aplicação real **sem furar os limites de domínio e casos de uso** — a IA orquestra, mas quem executa a regra de negócio é o código.

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
                                                   │ (criar/consultar  │
                                                   │   transação)      │
                                                   └─────────┬─────────┘
                                                             │
                                       ┌─────────────────────┼─────────────────────┐
                                       ▼                                           ▼
                                ┌────────────┐                          ┌──────────────────┐
                                │ Repositório│                          │ TextToSpeechModel│
                                │   (JPA)    │                          │  → resposta MP3  │
                                └────────────┘                          └──────────────────┘
```

1. **Cliente** envia um arquivo de áudio (`POST /api/ai/voice`).
2. **Transcrição** converte o áudio em texto (speech-to-text).
3. **ChatClient** recebe o texto e decide qual ferramenta usar (Tool Calling).
4. **Caso de uso** persiste ou consulta dados de transação.
5. Resposta final é convertida em áudio (text-to-speech) e/ou texto.

---

## 🏛️ Arquitetura

Arquitetura em camadas (DDD + Clean Architecture). A IA é apenas mais um *driver* que entra pela camada de infraestrutura — exatamente como o REST.

```
src/main/java/dio/budgeting/
├── domain/            # Modelo de domínio + contrato de repositório (regras puras)
│   ├── Transaction.java
│   ├── TransactionType.java
│   └── TransactionRepository.java
├── application/       # Casos de uso usados POR REST E PELA IA
│   ├── CreateTransactionUseCase.java
│   ├── QueryTransactionsUseCase.java
│   └── validation/    # ✅ MINHA EVOLUÇÃO: validações antes de salvar
│       └── TransactionValidator.java
└── infrastructure/    # Adaptadores: HTTP, JPA, e a "cola" da IA
    ├── ai/
    │   ├── BudgetTools.java        # @Tool expostas ao modelo
    │   ├── ChatClientConfig.java   # multi-provider (Ollama/Claude/Gemini/NVIDIA)
    │   └── VoiceController.java
    ├── rest/
    │   └── TransactionController.java
    └── persistence/
        ├── TransactionEntity.java
        └── JpaTransactionRepository.java
```

**Princípio-chave:** o mesmo caso de uso (`CreateTransactionUseCase`) é chamado tanto pelo `TransactionController` (REST) quanto pelo `BudgetTools` (Tool Calling da IA). A regra de negócio não é duplicada.

---

## 🧰 Stack / tecnologias

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 3.5 |
| IA | Spring AI 1.0 |
| Build | Gradle (Kotlin DSL) |
| Persistência | Spring Data JPA + H2 (dev) / PostgreSQL (compose) |
| IA local | Ollama (homelab) |
| IA cloud | Claude (Anthropic), Gemini (Google), NVIDIA NIM |
| Testes | JUnit 5 + Mockito + Spring Boot Test |
| Container | Docker Compose |

---

## 🤖 Provedores de IA suportados

A aplicação foi desenhada para ser **provider-agnostic**. Em desenvolvimento eu uso **Ollama local** (sem custo, offline, rodando no meu homelab); para qualidade de produção, qualquer provedor *OpenAI-compatible* pode ser plugado só trocando `base-url`, `api-key` e `model`.

| Provedor | Papel | Como é configurado |
|---|---|---|
| **Ollama** | Dev / testes locais | Starter Ollama nativo, `base-url=http://localhost:11434` |
| **Claude** | Expert (raciocínio / Tool Calling) | via endpoint OpenAI-compatible |
| **Gemini** | Expert (multimodal) | via endpoint OpenAI-compatible do Gemini |
| **NVIDIA NIM** | Expert (inferência acelerada) | via endpoint OpenAI-compatible (`integrate.api.nvidia.com`) |

> ⚠️ **Nota técnica importante:** quando os *starters* de OpenAI e Ollama coexistem, ambos registram um bean `ChatModel` e o Spring quebra por ambiguidade. A solução adotada aqui é usar **`@Profile`** + **`@Qualifier`** para selecionar o provedor por ambiente — veja `ChatClientConfig.java`. Você ativa o provedor pela variável `BUDGET_AI_PROVIDER` (`ollama` | `claude` | `gemini` | `nvidia`).

---

## ⭐ Minhas evoluções (o diferencial da entrega)

A partir do projeto base, implementei **duas melhorias**:

### 1. Nova ferramenta no Tool Calling — consulta por categoria e período

Adicionei a tool `sumByCategory`, que permite perguntas como *"quanto gastei com transporte essa semana?"*. O modelo identifica a categoria e o intervalo de datas a partir da fala e chama a função, que agrega as transações no banco.

```java
@Tool(description = "Soma o total gasto em uma categoria dentro de um período")
public BigDecimal sumByCategory(
        @ToolParam(description = "Categoria, ex: mercado, transporte") String category,
        @ToolParam(description = "Data inicial (YYYY-MM-DD)") LocalDate from,
        @ToolParam(description = "Data final (YYYY-MM-DD)") LocalDate to) {
    return queryUseCase.sumByCategory(category, from, to);
}
```

### 2. Validações antes de salvar uma transação

Criei a camada `TransactionValidator`, que roda **antes da persistência** e protege o domínio mesmo quando o comando vem da IA (que pode interpretar valores errados). Regras implementadas:

- **RN-1:** valor da transação deve ser **maior que zero**.
- **RN-2:** descrição é **obrigatória** e tem no máximo 120 caracteres.
- **RN-3:** data não pode ser **futura**.
- **RN-4:** categoria é normalizada (lowercase, sem espaços nas bordas).
- **RN-5:** tipo (`INCOME`/`EXPENSE`) é obrigatório.

Erros de validação retornam um **contrato de erro consistente** (`ProblemDetail`, RFC 7807), tanto no REST quanto quando a IA tenta executar a tool.

> Por que isso importa: a IA é **não-determinística**. Sem essa barreira, um comando mal interpretado poderia gravar lixo no banco. As validações garantem que o domínio permaneça íntegro independentemente da origem do comando.

---

## ▶️ Como executar

### Pré-requisitos

- Java 21
- Docker (opcional, para PostgreSQL)
- [Ollama](https://ollama.com/) rodando localmente (para o modo dev)

### 1. Subir o Ollama e baixar um modelo com suporte a tools

```bash
ollama serve
ollama pull qwen2.5        # modelo com bom suporte a Tool Calling
ollama pull llama3.2       # alternativa mais leve
```

### 2. Configurar o provedor

Para **desenvolvimento local (Ollama)** — nenhuma chave necessária:

```bash
export BUDGET_AI_PROVIDER=ollama
./gradlew bootRun
```

Para usar um **expert na nuvem** (ex.: Gemini):

```bash
export BUDGET_AI_PROVIDER=gemini
export GEMINI_API_KEY="sua_chave_aqui"
./gradlew bootRun
```

Variáveis equivalentes: `CLAUDE_API_KEY`, `NVIDIA_API_KEY`.

### 3. Rodar os testes

```bash
./gradlew test
```

---

## 🧪 Como testar o fluxo principal

### Via voz (fluxo completo)

```bash
curl -X POST http://localhost:8080/api/ai/voice \
  -F "audio=@comando.mp3" \
  --output resposta.mp3
```

Exemplos de comando falado:
- *"Registrar gasto de 45 reais no mercado hoje"*
- *"Quanto eu gastei com transporte esse mês?"*
- *"Qual o meu saldo?"*

### Via texto (para testar a IA sem gravar áudio)

```bash
curl -X POST http://localhost:8080/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "registra um gasto de 30 reais de uber hoje"}'
```

### Via REST direto (sem IA — útil para validar o domínio)

```bash
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"description":"Mercado","amount":45.00,"type":"EXPENSE","category":"mercado","date":"2026-06-15"}'
```

Veja exemplos prontos em [`docs/requests.http`](docs/requests.http).

---

## 🌐 Endpoints REST

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/ai/voice` | Recebe áudio, processa e devolve áudio |
| `POST` | `/api/ai/chat` | Recebe texto, processa via IA, devolve texto |
| `POST` | `/api/transactions` | Cria transação (com validações) |
| `GET` | `/api/transactions` | Lista transações |
| `GET` | `/api/transactions/summary` | Resumo por categoria/período |

Documentação interativa (Swagger): `http://localhost:8080/swagger-ui.html`

---

## 💡 O que aprendi

- **Tool Calling não é mágica.** O modelo só decide *qual* função chamar e com *quais* argumentos — quem executa e valida é o seu código. Isso reforça a importância de manter casos de uso bem definidos.
- **IA é uma fonte de entrada não confiável.** Tratei os argumentos vindos do LLM com a mesma desconfiança que trataria input de um usuário anônimo na internet — daí a camada de validação.
- **Abstração de provider vale muito.** Conseguir desenvolver de graça com Ollama local e plugar um expert na nuvem só trocando configuração mostra o poder do `ChatModel` como interface portável.
- **Ambiguidade de beans** entre starters foi um problema real e me ensinou a usar `@Profile`/`@Qualifier` de forma intencional.
- **Separar transcrição, raciocínio e síntese de voz** deixa cada parte testável de forma isolada.

---

## 📄 Licença

MIT — veja [LICENSE](LICENSE).

---

<p align="center">
  Feito por <a href="https://github.com/JoaoAzevedo184">João Victor Azevedo de Sena</a> ·
  Desafio Spring AI · DIO / NTT DATA
</p>
