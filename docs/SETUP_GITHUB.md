# 🚀 Como subir este projeto no GitHub

Guia rápido para criar o repositório e publicar. Substitua `budget-ai` pelo nome
que preferir (sugestões: `budget-ai`, `spring-ai-budget`, `voice-budget-ai`).

## 1. Criar o repositório no GitHub

Pelo site: **New repository** → nome `budget-ai`, descrição
*"Assistente financeiro por voz com Spring Boot + Spring AI (Tool Calling + validações)"*,
**público**, sem README/.gitignore/LICENSE (já estão neste projeto).

Ou pela CLI (`gh`):

```bash
gh repo create budget-ai --public \
  --description "Assistente financeiro por voz com Spring Boot + Spring AI" \
  --source=. --remote=origin
```

## 2. Inicializar o git e publicar

Dentro da pasta do projeto:

```bash
git init
git add .
git commit -m "feat: projeto base Budget AI com Spring AI (voz + tool calling)"
git branch -M main
git remote add origin https://github.com/JoaoAzevedo184/budget-ai.git
git push -u origin main
```

## 3. Commits sugeridos para mostrar evolução (bom para portfólio)

Em vez de um único commit gigante, divida para evidenciar seu processo:

```bash
git commit -m "feat: estrutura base em camadas (domain/application/infrastructure)"
git commit -m "feat: integração Spring AI com ChatClient e transcrição de áudio"
git commit -m "feat: tool calling para criar e listar transações"
git commit -m "feat: nova tool sumByCategory (consulta agregada por categoria/período)"
git commit -m "feat: validações antes de salvar (RN-1 a RN-5)"
git commit -m "feat: suporte multi-provider (Ollama/Claude/Gemini/NVIDIA)"
git commit -m "test: cobertura dos fluxos principais"
git commit -m "docs: README completo e exemplos de requisição"
```

## 4. Caprichar no perfil do repo

- Adicione **topics**: `spring-boot`, `spring-ai`, `java`, `tool-calling`,
  `ollama`, `llm`, `clean-architecture`, `dio`.
- Marque o README como destaque.
- Se gravar um gif/print do fluxo de voz funcionando, coloque em `docs/samples/`
  e referencie no README — mostra que **roda de verdade**.

## 5. Entrega na DIO

No campo de entrega do desafio, cole o link:
`https://github.com/JoaoAzevedo184/budget-ai`

E garanta que o README responde claramente:
- [x] O que o projeto faz
- [x] Como executar
- [x] Qual melhoria você implementou
- [x] Tecnologias usadas
- [x] Como testar o fluxo principal
- [x] O que você aprendeu
