# 🗺️ Próximos passos — Budget AI

Roteiro para levar o projeto de "compila e roda" até uma entrega de portfólio
sólida. Ordenado por prioridade.

---

## Fase 0 — Destravar (obrigatório antes de tudo)

Aplique as correções de [`REVISAO_CODIGO.md`](REVISAO_CODIGO.md), na ordem:

- [ ] Apagar `infrastructure/ChatClientConfig.java` duplicado 🔴
- [ ] `./mvnw -B compile` passa
- [ ] (opcional, recomendado) mover `Transaction` para `domain`
- [ ] (opcional) limpar getters duplicados em `TransactionEntity`
- [ ] `./mvnw test` passa (validador verde)

---

## Fase 1 — Rodar de ponta a ponta

- [ ] Subir o Ollama: `ollama serve && ollama pull qwen2.5`
- [ ] `export BUDGET_AI_PROVIDER=ollama && ./mvnw spring-boot:run`
- [ ] Testar `/api/transactions` (POST) — valida domínio sem IA
- [ ] Testar `/api/ai/chat` com "registra um gasto de 30 reais de uber hoje"
- [ ] Conferir no H2 console (`/h2-console`) que a transação foi gravada
- [ ] Acessar `/swagger-ui.html` e ver os endpoints
- [ ] Capturar um print ou gif do fluxo funcionando → `docs/samples/`

> Dica: modelos pequenos às vezes erram o Tool Calling. Se a IA não chamar a
> ferramenta, teste `qwen2.5` (bom com tools) ou aumente a clareza do comando.

---

## Fase 2 — Fortalecer os testes (peso de portfólio)

Hoje só o validador tem teste unitário. Sugestões de cobertura, sem depender de IA:

- [ ] **`CreateTransactionUseCase`** com repositório mockado (Mockito): garante que
      valida antes de salvar e que normaliza a categoria.
- [ ] **`QueryTransactionsUseCase.sumByCategory`**: normalização do input + delega
      ao repositório.
- [ ] **Camada web** com `@WebMvcTest(TransactionController.class)`: testa o
      contrato REST e o `GlobalExceptionHandler` (ex.: POST com valor 0 → 422 +
      `ruleCode: RN-1`).
- [ ] **Persistência** com `@DataJpaTest`: testa `findByDateBetween` e
      `sumByCategoryAndPeriod` (incluindo o caso vazio → 0 via COALESCE).

Meta: cobrir os fluxos principais sem nenhuma chamada de rede, mantendo o CI verde.

---

## Fase 3 — Robustez e DX

- [ ] **Migrations com Flyway** em vez de `ddl-auto: update` (mais profissional;
      você já usa Flyway no study-rag).
- [ ] **Bean Validation** (`@Valid` + anotações no `TransactionRequest`) como
      primeira barreira no REST, complementando o `TransactionValidator` de domínio.
- [ ] **Perfil de teste** (`application-test.yml`) com H2 e sem IA, para isolar os
      testes de integração.
- [ ] Tratar exceções genéricas no `GlobalExceptionHandler` (400 para body
      malformado, 500 controlado).

---

## Fase 4 — Evoluções de produto (ideias do desafio)

- [ ] Nova tool: **saldo do mês** (receitas − despesas no período).
- [ ] Nova tool: **top categorias** por gasto.
- [ ] Suporte a `INCOME` no `sumByCategory` (hoje fixa EXPENSE) via parâmetro de
      tipo opcional.
- [ ] Endpoint de **auditoria**: histórico de comandos de voz processados.

---

## Fase 5 — Publicação

- [ ] Commits fatiados contando a história (ver `docs/SETUP_GITHUB.md`)
- [ ] Topics no repo: `spring-boot`, `spring-ai`, `java`, `tool-calling`,
      `ollama`, `clean-architecture`, `dio`
- [ ] Badge de CI no topo do README apontando para o repo correto
- [ ] Print/gif do fluxo em `docs/samples/` referenciado no README
- [ ] Conferir o checklist de entrega da DIO no `docs/SETUP_GITHUB.md`
- [ ] Colar o link do repositório no campo de entrega do desafio

---

## Possível Fase 6 — CD (quando quiser)

Você optou por só CI agora. Quando quiser evoluir:

- [ ] Job `docker` com `needs: build` que constrói a imagem
      (`./mvnw spring-boot:build-image`) e publica no GHCR.
- [ ] Tag por versão/commit e push em release.
- [ ] (avançado) Deploy num VPS/homelab via SSH — exige secrets e um destino.

O gancho natural é adicionar um segundo job no `ci.yml` existente.
