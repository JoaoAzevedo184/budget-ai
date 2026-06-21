# CI - Integração Contínua (GitHub Actions)

O pipeline está em [`.github/workflows/ci.yml`](../.github/workflows/ci.yml).

## O que ele faz

A cada **push na `main`** e em **todo Pull Request**:

1. faz checkout do código;
2. instala o JDK 21 (Temurin) com cache do `~/.m2`;
3. roda `./mvnw -B verify` (compila + testes + empacota);
4. publica o relatório de testes JUnit como um *check* no PR;
5. anexa os relatórios do Surefire como artefato (7 dias).

Builds antigos do mesmo branch/PR são cancelados automaticamente quando chega
um push novo (bloco `concurrency`).

## Por que não precisa de secrets nem de IA

- O `TransactionValidatorTest` cobre as regras RN-1..RN-5 e roda **offline**.
- O `BudgetAiApplicationTests` (`@SpringBootTest`) está condicionado a
  `RUN_CONTEXT_TEST=true`. O CI **não** define essa variável, então o teste de
  contexto é ignorado — assim o pipeline passa sem Ollama nem chaves de API.

Se um dia você quiser rodar o teste de contexto no CI, suba um Ollama como
service container e adicione `env: RUN_CONTEXT_TEST: 'true'` no step de build.

## Pré-requisito: Maven Wrapper versionado

O workflow chama `./mvnw`. Para funcionar no runner, estes arquivos (gerados
pelo Spring Initializr) **precisam estar commitados** na raiz do repositório:

```
mvnw
mvnw.cmd
.mvn/wrapper/maven-wrapper.properties
```

Confirme que o seu `.gitignore` não os ignora. Se você apagou o wrapper, gere
de novo com:

```bash
mvn -N wrapper:wrapper
```

## Rodando localmente o mesmo que o CI roda

```bash
./mvnw -B verify
```

Se passar local, passa no CI (mesma JDK, mesmos testes).
