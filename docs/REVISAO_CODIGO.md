# 🔍 Revisão de código — erros e correções

Análise do estado atual do projeto. Itens marcados com 🔴 **impedem a compilação**
e devem ser corrigidos antes de qualquer outra coisa.

---

## 🔴 1. Classe `ChatClientConfig` duplicada (erro de compilação)

Existem **dois** arquivos com a mesma classe e o mesmo pacote
(`dio.budgeting.infrastructure.ai.ChatClientConfig`):

- `src/main/java/dio/budgeting/infrastructure/ChatClientConfig.java` — versão
  **antiga** (com `@Profile`/`@Qualifier`, `SYSTEM_PROMPT` inline, e referência a
  `BudgetTools` sem import correto). Está fisicamente na pasta errada.
- `src/main/java/dio/budgeting/infrastructure/ai/ChatClientConfig.java` — versão
  **correta** (injeta um único `ChatModel`, lê o prompt de `system-message.st`).

**Correção:** apague o arquivo antigo.

```bash
git rm src/main/java/dio/budgeting/infrastructure/ChatClientConfig.java
```

Mantenha apenas o de dentro de `infrastructure/ai/`.

---

## 🟡 2. `Transaction` no pacote `application` (fere a Clean Architecture)

O arquivo está em `application/Transaction.java` com
`package dio.budgeting.application`, mas é uma **entidade de domínio**. Isso cria
uma referência circular entre pacotes:

- `domain.TransactionRepository` importa `application.Transaction`
- `application.Transaction` importa `domain.TransactionId` e `domain.TransactionType`

Compila (o Java permite ciclo entre pacotes), mas **contradiz o próprio README**,
que lista a classe como `domain/Transaction.java` e afirma que o domínio não
depende de application. Para um projeto de portfólio que vende "Clean
Architecture", vale corrigir.

**Correção (mover para o domínio):**

1. Mova o arquivo para `src/main/java/dio/budgeting/domain/Transaction.java`.
2. Troque a primeira linha para `package dio.budgeting.domain;`.
3. **Remova** os imports que viram desnecessários (mesma package agora):
   `import dio.budgeting.domain.TransactionId;` e
   `import dio.budgeting.domain.TransactionType;`.
4. Atualize os imports em quem usava `application.Transaction`:
   - `domain/TransactionRepository.java`: remova
     `import dio.budgeting.domain.Transaction;` (agora é mesma package).
   - `application/output/TransactionOutput.java`: troque
     `import dio.budgeting.domain.Transaction;` por
     `import dio.budgeting.domain.Transaction;`.
   - `application/CreateTransactionUseCase.java`: adicione
     `import dio.budgeting.domain.Transaction;` (ele usa `new Transaction(...)`).
   - `infrastructure/persistence/entity/TransactionEntity.java`: troque
     `import dio.budgeting.domain.Transaction;` por
     `import dio.budgeting.domain.Transaction;`.
   - `infrastructure/persistence/repository/JpaTransactionRepository.java`: troque
     `import dio.budgeting.domain.Transaction;` por
     `import dio.budgeting.domain.Transaction;`.

> Se preferir **não** mexer agora (priorizar a entrega), o projeto funciona como
> está — mas então atualize o README para refletir que `Transaction` vive em
> `application`, para a documentação não mentir.

---

## 🟡 3. `TransactionEntity` — getters duplicados

A classe tem `@Getter`/`@Setter` (Lombok) **e** getters escritos à mão
(`getId()`, `getDescription()`, ...). Redundante. Não quebra a compilação (o
Lombok não recria métodos já existentes), mas é ruído.

**Correção (escolha uma):**
- Remova os getters manuais e mantenha só `@Getter @Setter`; **ou**
- Remova `@Getter @Setter` e mantenha os manuais (precisa de setters? a entidade
  só é construída via construtor + `from()`, então provavelmente não — pode tirar
  o Lombok daqui).

Recomendo manter `@Getter` e remover os manuais (menos código).

---

## 🟡 4. `docs/requests.http` desatualizado

Os exemplos 4 e 6 do arquivo usam o modelo antigo:
- exemplo 6 faz `GET /api/transactions` sem os parâmetros obrigatórios `from`/`to`;
- não há exemplo para `/summary`.

**Correção:** substitua pelos exemplos do arquivo `docs/API.md` (seção Transações),
que refletem os endpoints atuais.

---

## ✅ O que está correto

- `pom.xml`: dependências coerentes (Ollama + OpenAI starters, H2/Postgres,
  springdoc, security, validation). BOM do Spring AI importado certo.
- Profiles separados em `application-<provider>.yml`: escolha válida e legível.
- `SecurityConfig`: libera endpoints e Swagger — resolve o 401 do starter security.
- `AiController`: uso de `ObjectProvider` para áudio opcional é a abordagem certa
  (degrada para 501 quando não há modelo de áudio).
- `TransactionEntityRepository.sumByCategoryAndPeriod`: `COALESCE` evita retorno
  null. Correto.
- `TransactionValidatorTest`: cobre RN-1..RN-5, roda offline. Ótimo para o CI.
- CI (`.github/workflows/ci.yml`): build + testes sem secrets. Correto.

---

## Ordem sugerida de correção

1. 🔴 Apagar o `ChatClientConfig` duplicado (item 1) — **destrava a compilação**.
2. Rodar `./mvnw -B compile` e confirmar que compila.
3. 🟡 Mover `Transaction` para `domain` (item 2) e ajustar imports.
4. 🟡 Limpar getters duplicados (item 3).
5. 🟡 Atualizar `requests.http` (item 4).
6. `./mvnw test` — o teste do validador deve passar.
