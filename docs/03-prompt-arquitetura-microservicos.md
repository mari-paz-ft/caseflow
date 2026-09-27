# Prompt 3: Atualização da Arquitetura para 3 Serviços Backend + Frontend

Este prompt foi concebido para agentes autônomos revisarem a governança e o plano arquitetural do CaseFlow. Preserva o objetivo original de evoluir o contexto para microsserviços, mas usa a arquitetura já aprovada e implementada: caseflow-web, caseflow-bff, auth-service e case-service.

> **Escopo:** revisar documentação/contexto e registrar pendências. Alterações de código devem seguir uma fase autorizada; este prompt não autoriza novas funcionalidades.

---

```text
Atue como Arquiteto de Software Sênior no repositório CaseFlow.

## 1. Contexto e decisões existentes

Antes de escrever, leia integralmente os quatro arquivos `.ai/`, `docs/adr/001-mvp-architecture.md`,
`docs/mvp-scope.md`, `docs/CaseFlow-Arquitetura-Pratica.md`, `docs/api-contracts.md` e
`docs/compliance-matrix.md`. Consulte o código/configuração para separar requisito, decisão
aprovada, comportamento observado e débito conhecido.

A arquitetura física vigente tem quatro processos independentes:

  caseflow-web -> caseflow-bff -> auth-service
                             -> case-service

Decisões aprovadas que não devem ser reabertas:
- O navegador chama somente caseflow-bff; BFF chama auth-service e case-service por REST.
- BFF não tem banco próprio. auth-service mantém conexão PostgreSQL preparada, sem persistir
  contas no MVP; case-service é dono de case-db e dos arquivos em storage local.
- Credenciais não vazias são aceitas como demonstração; username contendo `admin` recebe ADMIN,
  os demais USER. JWT HMAC-SHA256 é real, com issuer `caseflow-auth-service` e subject UUID
  determinístico; BFF e case-service validam o token independentemente.
- O case-service usa jobs persistidos, scheduler com leases, recuperação após restart,
  `Idempotency-Key`, retries técnicos e notificações.
- PDF é o único formato aceito. A análise é determinística e não comprova autenticidade.

O baseline anterior em `backend/` e `frontend/`, inclusive Maven, fallback mock e identidade por
header, é histórico; não deve ser descrito como arquitetura-alvo ou reproduzido.

## 2. Antes de atualizar a governança

Compare `.ai/architecture.md`, `.ai/business-rules.md`, `.ai/tech-stack.md` e `.ai/standards.md`
com o ADR, o escopo e a matriz de aderência. Preserve decisões aceitas e a essência dos textos.
Não reescreva documentos inteiros para mudar uma regra pontual. Registre conflitos adicionais
com evidência e não invente uma solução de negócio.

## 3. Conteúdo a conferir

### 3.1 `.ai/architecture.md` — revisar sem reabrir decisões

- Manter diagrama Mermaid das quatro aplicações, auth-db, case-db e storage `./data/documents`.
- Registrar responsabilidades e portas locais/Compose: web 5173/80, BFF 8081, auth-service
  8082 local/8080 interno, case-service 8080 e bancos sem publicação no host.
- Explicitar que o navegador usa somente BFF, não há banco no BFF e os serviços internos usam DNS
  do Compose e REST síncrono.
- Documentar que credenciais são mockadas, JWT HMAC é real e BFF/case-service validam o token
  independentemente; não descreva sessão/cookie nem fluxo OAuth/OIDC.
- Descrever scheduler/leases no case-service, sem broker ou worker independente.

### 3.2 `.ai/tech-stack.md` — manter stack observada

- Kotlin 2.0.20, Java 21, Spring Boot 3.3.4, Spring Security/Data JPA e Gradle Kotlin DSL
  8.10.2 com wrappers.
- PostgreSQL 16 em runtime, H2 em testes; auth-service mantém conexão preparada sem usuários
  persistidos.
- React 18, TypeScript, Vite, Nginx e Docker Compose; frontend só consome BFF.
- Não presuma Maven nem acrescente OAuth Provider, broker, Redis, S3 ou dependências não aprovadas.

### 3.3 `.ai/standards.md` — preservar fronteiras

- Organizar cada backend em `domain`, `application`, `infrastructure` e `interfaces/rest`.
- Manter ports/adapters, DTOs próprios por aplicação, chamadas downstream fora de controllers,
  respostas de erro seguras e testes nos contratos/regras.
- Proibir identidade por `X-User-Email`, usuário padrão, token textual e autorização somente na UI.

### 3.4 `.ai/business-rules.md` — preservar regras verificadas

- USER acessa somente recursos próprios; ADMIN consulta global, mas não edita rascunhos alheios;
  notificações são exclusivas do destinatário.
- Casos aceitam somente `ANALISE_DOCUMENTAL`; títulos/descrições e versões seguem as validações.
- Documentos: PDF, MIME `application/pdf`, até 5 MiB, cabeçalho `%PDF-`, máximo três categorias;
  validade usa a data UTC do envio e submit exige pelo menos um documento `READY`.
- Diferenciar rejeição de negócio de falha técnica; documentar retry 10 s/30 s/terceira falha,
  recuperação por lease, notificação por tentativa e retry ADMIN com justificativa/idempotência.

### 3.5 Backlog e pendências

As Fases 0–14 já foram registradas como concluídas no plano atual. Não recrie `.ai/tasks.md` nem
reabra tarefas fechadas. Se houver uma nova solicitação de desenvolvimento, transforme apenas os
`PARTIAL` ainda aceitos da matriz em um plano novo, com evidência e critério de aceite claros.

### 3.6 Regras de ferramentas

- Os quatro arquivos `.ai/` são a fonte canônica deste projeto.
- Não altere regras de ferramentas externas nem adicione novos arquivos de configuração sem pedido explícito.

## 4. Restrições

- Este prompt orienta revisão de arquitetura/contexto, não autoriza mudanças de código, dependências,
  segurança ou banco sem uma fase aprovada.
- Não reintroduza OAuth2/OIDC, sessão/cookie no BFF, BFF DB, contas persistidas, acesso direto do
  navegador a auth-service/case-service, `X-User-Email`, fallback local ou PDF fabricado.
- Preserve documentos históricos como histórico; corrija somente afirmações apresentadas como
  atuais e divergentes do comportamento observado.

## 5. Verificação

- Conferir consistência de nomes das quatro aplicações, portas, endpoints, variáveis e paths de
  execução entre `.ai/`, README, arquitetura, contratos e Compose.
- Confirmar que diagramas Mermaid renderizam e que links relativos funcionam.
- Buscar instruções atuais incompatíveis; não contar referências históricas explicitamente
  rotuladas como decisões vigentes.
- Reportar arquivos alterados, evidências, status de cada regra crítica, limitações e débitos.
  Não declarar conformidade total se houver `FAIL` ou `PARTIAL` sem resolução/aceite.
```
