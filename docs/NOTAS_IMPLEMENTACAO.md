# Notas de implementação - Caminho B

Este documento registra as decisões tomadas ao migrar do scaffold inicial
(Spring Initializr + stubs) para o **Caminho B**: modelo de domínio novo com
`BigDecimal`, categoria como texto livre, `TransactionType` e `LocalDate`.

## Arquivos a SUBSTITUIR / REMOVER do scaffold antigo

Se você partiu do scaffold anterior (com os `.properties` e os stubs antigos),
faça esta limpeza para não ter classes duplicadas ou conflitantes:

- **Remova** os cinco arquivos de config antigos:
  `application.properties`, `application-ollama.properties`,
  `application-claude.properties`, `application-gemini.properties`,
  `application-nvidia.properties`.
  → substituídos por um único `src/main/resources/application.yml`.

- **Remova** os stubs antigos que estavam em pastas erradas:
  `application/TransactionValidator.java`,
  `application/ValidationException.java`,
  `infrastructure/BudgetTools.java`,
  `infrastructure/ChatClientConfig.java`.
  → recriados nos pacotes corretos (`application/validation/`, `infrastructure/ai/`).

- **Remova** a classe `BudgetingApplication.java` antiga se existir;
  o entrypoint agora é `BudgetAiApplication.java`.

## Decisões do pom.xml

- **Adicionado** `spring-ai-starter-model-openai` (o scaffold só tinha Ollama).
  É ele que fornece o `openAiChatModel` usado pelos perfis cloud e os modelos
  de áudio (Whisper/TTS).
- **Removido** `spring-ai-spring-boot-docker-compose` — esse artifactId não
  existe no Spring AI 1.1.x e quebrava o build. O `spring-boot-docker-compose`
  padrão já cobre o necessário.
- **Removido Lombok** — nenhuma classe do Caminho B usa Lombok (domínio puro
  com getters explícitos). Se quiser reintroduzir, volte a dependência e o
  bloco de `annotationProcessorPaths` no compiler plugin.
- `groupId` ajustado de `com.dio` para `dio` para casar com `package dio.budgeting`.

## Ambiguidade de ChatModel (Ollama + OpenAI no classpath)

Com os dois starters presentes, cada profile no `application.yml` fixa o modelo
ativo:
- profile `ollama`: `spring.ai.model.chat: ollama`
- profiles cloud:   `spring.ai.model.chat: openai` (+ `audio.*: openai`)

Assim o `ChatClientConfig` injeta um único `ChatModel` sem `@Qualifier`.
Confirme essa propriedade na versão 1.1.8 (o nome da chave de seleção de modelo
variou entre releases do Spring AI); se necessário, troque por `@Qualifier`
no bean do `ChatClientConfig`.

## Áudio

- `/api/ai/voice` injeta `TranscriptionModel` e `TextToSpeechModel` como
  `ObjectProvider` (opcionais). No profile `ollama` eles não existem → o
  endpoint retorna `501 Not Implemented`. Use `/api/ai/chat`.
- Nos profiles cloud, o áudio usa OpenAI (requer `OPENAI_API_KEY`), mesmo que o
  chat seja Claude/Gemini/NVIDIA.

## Pontos para validar ao compilar pela primeira vez

1. `./mvnw -q compile` — baixa deps e confirma que tudo compila.
2. `./mvnw test` — o `TransactionValidatorTest` roda offline e cobre RN-1..RN-5.
3. Suba o Ollama, `export BUDGET_AI_PROVIDER=ollama`, `./mvnw spring-boot:run`,
   e teste `/api/ai/chat`.
4. Acesse `/swagger-ui.html` para ver os endpoints.

## Checklist de entrega DIO

- [ ] Projeto compila (`./mvnw test` verde)
- [ ] README explica: o que faz, como rodar, melhoria, stack, como testar, aprendizado
- [ ] Pelo menos um print/gif do fluxo funcionando em `docs/samples/`
- [ ] Repositório público com topics
- [ ] Link colado no campo de entrega do desafio
