# Prompt 2: Implementação do MVP (Executável por Agentes)

Este prompt foi concebido para ser executado por agentes autônomos de desenvolvimento (como Google Antigravity, Claude Code ou Cursor Composer) para implementar e validar o MVP do CaseFlow com quatro aplicações independentes.

---

```text
Atue como Desenvolvedor Fullstack Sênior executando em modo agente no repositório CaseFlow.

Você irá implementar o MVP de Solicitações e Conferência Documental conforme os quatro arquivos
`.ai/` e o ADR 001. Antes de escrever código, leia `.ai/architecture.md`, `.ai/business-rules.md`,
`.ai/tech-stack.md`, `.ai/standards.md`, `docs/adr/001-mvp-architecture.md` e `docs/mvp-scope.md`.

A estrutura executável é:
- `apps/frontend/caseflow-web` — React, TypeScript e Vite.
- `apps/backend/caseflow-bff` — entrada pública e clients REST downstream.
- `apps/backend/auth-service` — autenticação demonstrativa e JWT.
- `apps/backend/case-service` — domínio documental, persistência e scheduler.

1. BACKEND (Kotlin + Spring Boot)
   - Mantenha cada backend como processo e build independente, usando Gradle Kotlin DSL e Java 21.
   - O auth-service aceita username e senha não vazios sem persistir contas. Username contendo
     `admin` recebe ADMIN; os demais recebem USER. Emitir JWT HMAC real com subject UUID
     determinístico, roles, iat, exp e iss `caseflow-auth-service`.
   - O BFF valida JWT, expõe a API pública do navegador, chama auth-service e case-service por
     clients REST e mantém DTOs/mappings próprios. Não adicione banco ao BFF.
   - O case-service valida JWT independentemente, aplica autorização por recurso e possui
     PostgreSQL em runtime, H2 em testes, histórico e notificações persistidas.
   - Implemente casos, documentos, resultados e transições conforme `.ai/business-rules.md`.
     O tipo aceito é `ANALISE_DOCUMENTAL`; valide título e descrição e use `version` nas escritas.
   - Aceite PDFs apenas com MIME `application/pdf`, tamanho real máximo de 5 MiB e assinatura
     básica `%PDF-`. Use DocumentStoragePort e storage local; valide tamanho/hash ao ler e jamais
     fabrique conteúdo quando o arquivo estiver ausente.
   - Submit e retry exigem `Idempotency-Key`; replay equivalente retorna a resposta original e
     contexto divergente resulta em 409, sem job duplicado.
   - Persista ProcessingJob antes do aceite. Use scheduler com lock/lease e recuperação após
     restart. Retry técnico: primeira falha +10 s, segunda +30 s, terceira termina em
     `FALHA_TECNICA`; rejeição de negócio não agenda retry. Notifique o titular em cada falha.
   - Retry ADMIN é permitido apenas em `FALHA_TECNICA` e exige justificativa de 10 a 500 caracteres.

2. FRONTEND (React + TypeScript + Vite)
   - Crie uma interface responsiva com login, lista/detalhe, criação, documentos, submit,
     resultado, histórico e notificações.
   - A camada de API chama exclusivamente o BFF, envia `Authorization: Bearer`, mantém JWT em
     memória e conserva `Idempotency-Key` por intenção do usuário.
   - Não implemente alternador local de usuário, endpoints diretos de auth-service/case-service ou
     fallback em memória para mascarar respostas HTTP 4xx/5xx.
   - Apresente estados claros de carregamento, erro, vazio e sucesso.

3. INFRAESTRUTURA, SCRIPTS E DOCUMENTAÇÃO
   - Compose deve subir caseflow-web, caseflow-bff, auth-service, case-service, auth-db e case-db.
     Use DNS interno entre serviços; publique somente web e BFF. Persista bancos em volumes
     separados e documentos em `./data/documents`.
   - Segredos vêm do ambiente/`.env` local ignorado pelo Git. Nunca grave valores no repositório.
   - Não adicione Maven, broker, Redis, OAuth Provider externo, S3, OCR ou dependência de teste
     não aprovada em `.ai/tech-stack.md`.
   - Atualize README e contratos existentes somente onde execução, porta, endpoint ou comportamento
     mudou. Preserve a estrutura original dos Markdown e não substitua o conteúdo inteiro sem
     necessidade.
   - Teste os serviços com Gradle Wrapper, rode `npm test`/`npm run build` no frontend e execute
     Compose/E2E conforme o gate autorizado. Não remova volumes para limpar dados de teste.

Não introduza OAuth2/OIDC, sessão/cookie no BFF, contas persistidas, usuário padrão, `X-User-Email`,
PDF de fallback, broker ou worker externo. Diferencie rejeição de negócio de falha técnica; não
exponha stack trace, segredo, JWT ou conteúdo privado. Informe evidências e débitos sem afirmar
que uma regra passou quando não foi testada.
```
