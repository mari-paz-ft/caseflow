# Prompt 3: Atualização da Arquitetura para 3 Microserviços + Backlog Persistente

Este prompt foi concebido para ser executado por agentes autônomos de desenvolvimento (Claude Code, Cursor Composer, Google Antigravity, Codex etc.). Ele atualiza **somente os arquivos de contexto** em `.ai/` (e as regras dos agentes) para a arquitetura com BFF + serviço de negócio + serviço de autenticação, e cria um backlog persistente de tarefas para a migração do código.

> **Escopo:** apenas documentação/contexto. **Não altere código** em `backend/`, `frontend/`, `docker-compose.yml` nem `docs/`. As mudanças de código entram como tarefas em `.ai/tasks.md`.

---

```text
Atue como Arquiteto de Software Sênior executando em modo agente no repositório CaseFlow.

## 1. Contexto

Os arquivos em .ai/ (architecture.md, tech-stack.md, standards.md, business-rules.md) descrevem
hoje um MONÓLITO ("caseflow-backend", Kotlin + Spring Boot 3) e um frontend React com modo mock.
O documento docs/CaseFlow-Arquitetura-Pratica.md já define o desenho-alvo: frontend + BFF +
dois microserviços (negócio e autenticação), com OAuth2/OIDC, sessão no BFF, bancos lógicos
separados e executor interno durável. Use esse documento como FONTE PRINCIPAL: copie e adapte
seus diagramas (seções 4, 5 e 8), tabela de rotas (seção 6) e critérios de aceite (seção 9),
sem reinventar.

Decisões já tomadas (não reabra):
- Três serviços backend, todos em Kotlin 2.x + Spring Boot 3.x:
  caseflow-bff, case-service (negócio) e auth-service (identidade).
- Autenticação: OAuth2/OIDC (Authorization Code + PKCE) com Spring Authorization Server no
  auth-service; o BFF mantém os tokens no servidor (Spring Session JDBC) e o navegador recebe
  apenas cookie de sessão HttpOnly; o case-service é resource server e valida JWT via JWKS.
- Backlog persistente em .ai/tasks.md, lido e atualizado por qualquer agente.

Divergências encontradas na validação do código atual (viram tarefas no backlog):
- backend/src/main/kotlin/com/caseflow/service/DocumentService.kt importa
  com.caseflow.domain.model.RoleName, mas o enum está em com.caseflow.domain.enums → build quebra.
- Existem build.gradle.kts e pom.xml duplicados (Dockerfile usa Gradle).
- Executor assíncrono: processJobAsync é chamado DENTRO da transação de submit/retry (antes do
  commit); não existe poller @Scheduled para recuperar jobs SCHEDULED/RUNNING após reinício;
  campos attemptCount/availableAt/leaseUntil/leaseToken não são usados; markTechnicalFailure
  roda na mesma transação que pode estar rollback-only; perfil padrão é H2 em memória.
- Falha técnica só é simulada por "[SIMULAR_FALHA]"; o motor não lê arquivos nem confere sha256;
  getDocumentContent devolve um PDF falso quando o arquivo não existe.
- Idempotency-Key é opcional (required=false) e ignorada no CaseService; retry repetido devolve 409;
  o frontend gera a chave do retry com Date.now().
- SecurityConfig usa anyRequest().permitAll(); identidade via header X-User-Email com fallback
  para usuário padrão; /auth/login ignora a senha; NotificationService lança
  IllegalArgumentException/IllegalStateException (vira 500 em vez de 404/403).
- frontend/src/services/api.ts cai no mock para QUALQUER resposta não-2xx (mascara 403/409);
  não há updateCase via PUT no cliente real.
- Uploads em ./uploads sem volume no docker-compose.yml.

## 2. Antes de escrever

Leia integralmente: .ai/architecture.md, .ai/tech-stack.md, .ai/standards.md,
.ai/business-rules.md, .cursorrules, .geminirules e docs/CaseFlow-Arquitetura-Pratica.md.
Escreva tudo em português, mantendo o estilo e a formatação atuais dos arquivos.

## 3. Arquivos a alterar

### 3.1 .ai/architecture.md — REESCREVER

a) Visão geral + diagrama Mermaid (flowchart) com: caseflow-web (React), caseflow-bff,
   case-service, auth-service, bff_db, case_db, auth_db e "Arquivos privados (volume/S3)".
   Arestas: WEB→BFF (HTTPS + cookie de sessão); WEB→AUTH (navegação para login);
   BFF→AUTH (OAuth2/OIDC); BFF→CORE (REST + access token); CORE-.->AUTH (JWKS);
   BFF→bff_db; AUTH→auth_db; CORE→case_db; CORE→Arquivos.
   Tabela de responsabilidades por componente.

b) Tabela de serviços:
   | Serviço      | Porta local | Prefixo                                  | Banco   | Papel Spring Security                 |
   | caseflow-bff | 8080        | /bff/v1 (público)                        | bff_db  | oauth2-client + Spring Session JDBC   |
   | case-service | 8081        | /api/v1 (rede interna)                   | case_db | oauth2-resource-server (JWT via JWKS) |
   | auth-service | 9000        | /oauth2/*, /.well-known/*, /login        | auth_db | Spring Authorization Server           |

c) Mapa de rotas BFF → core: rotas de negócio /bff/v1/<sufixo> → /api/v1/<sufixo> com o mesmo
   sufixo (tabela da seção 6 do docs). /csrf, /me e /logout pertencem ao BFF. Login inicia em
   /oauth2/authorization/caseflow com callback /login/oauth2/code/caseflow (ambos no BFF).

d) ADRs (formato atual: Status, Contexto, Decisão, Consequência):
   - ADR 001 — Kotlin 2.x + Spring Boot 3.x nos três serviços (manter e estender).
   - ADR 002 (novo) — Decomposição em BFF + case-service + auth-service. Executor de análise é
     módulo interno do case-service; sem worker separado e sem broker no MVP.
   - ADR 003 (novo) — Autenticação OAuth2/OIDC: Authorization Code + PKCE; tokens só no BFF;
     cookie HttpOnly, Secure (em HTTPS), SameSite=Lax; CSRF em escritas no BFF; case-service
     valida assinatura, iss, aud, exp e claim "roles"; é PROIBIDO confiar em headers de
     identidade (ex.: X-User-Email). Logout invalida sessão e solicita encerramento ao auth.
   - ADR 004 (novo) — Banco por serviço: bff_db, case_db, auth_db (um PostgreSQL local,
     credenciais distintas); sem FK/consulta entre bancos; owner_subject/recipient_subject e
     atores humanos do histórico guardam o "sub" do token; ator automático = SYSTEM;
     BFF não acessa case_db. Migrações com Flyway (sem ddl-auto: update).
   - ADR 005 — Executor interno durável (antigo ADR 002, refinado): job persistido na mesma
     transação do envio; disparo somente após commit; poller @Scheduled obtém jobs com
     concessão (lease_until/lease_token) — executor antigo não sobrescreve execução atual;
     até 3 tentativas totais com espera de 10 s e 30 s; esgotadas → FALHA_TECNICA; rejeição
     de negócio não gera nova tentativa; reinício não perde jobs pendentes.
   - ADR 006 — Separação regra de negócio × falha técnica (antigo ADR 003): APROVADA/REJEITADA
     como hoje; motor lê cada arquivo e confere sha256; erro de I/O, arquivo ausente ou hash
     divergente → FALHA_TECNICA; nunca retornar conteúdo simulado. Retry por ADMIN em
     POST /cases/{id}/retry.
   - ADR 007 — Frontend desacoplado com mock híbrido (antigo ADR 004): o front fala SOMENTE
     com /bff/v1; fallback para mock apenas quando o BFF está offline/erro de rede; respostas
     HTTP de erro (4xx/5xx) são propagadas para a UI, nunca mascaradas.
   - ADR 008 — Idempotência (antigo ADR 005): Idempotency-Key obrigatória em submit e retry
     (ausente → 400); registro em case_db (chave, ator, rota, hash do payload, status e corpo da
     resposta); mesma chave + mesmo payload → resposta original; mesma chave + payload
     diferente → 409; BFF repassa o header sem alterar; chave gerada pelo front uma vez por
     intenção do usuário.
   - ADR 009 (novo) — Arquivos privados: armazenados pelo case-service em volume persistente
     (S3 privado na evolução); upload/download passam pelo BFF em streaming; banco guarda só
     metadados e storage_key; toda rota de documento verifica pertencimento à solicitação.
   - ADR 010 (novo) — Comunicação entre serviços: REST síncrono BFF→core com RestClient e token
     relay; propagação de X-Request-Id como traceId; timeouts explícitos; core indisponível →
     503 no BFF; erros do core repassados preservando code e traceId.

e) Diagramas de sequência Mermaid: login OIDC (seção 8.1 do docs) e envio/processamento
   (seção 8.3 do docs). Diagrama de estados (seção 8.4).

### 3.2 .ai/tech-stack.md — ATUALIZAR
- Seção Backend: base comum (Kotlin 2.x, JVM 21, Spring Boot 3.3+, Gradle Kotlin DSL APENAS —
  sem Maven/pom.xml, jackson-module-kotlin, Flyway, Actuator) + dependências por serviço:
  * auth-service: spring-boot-starter-oauth2-authorization-server, data-jpa, security, flyway.
  * caseflow-bff: spring-boot-starter-oauth2-client, spring-session-jdbc, web (RestClient),
    security, flyway.
  * case-service: spring-boot-starter-oauth2-resource-server, web, data-jpa, validation,
    flyway, springdoc-openapi 2.x.
- Banco: PostgreSQL 16+ com três bancos lógicos; H2 apenas para testes.
- Testes: JUnit 5, MockK ou Mockito-Kotlin, Spring Boot Test, spring-security-test,
  Testcontainers (opcional).
- Frontend: fetch/axios com credenciais de cookie, cabeçalho CSRF e Idempotency-Key
  (remover menção a "tokens" no navegador).
- Infra: Docker Compose com 4 aplicações + PostgreSQL; script de init criando bff_db, case_db,
  auth_db com usuários distintos; volume para uploads do case-service; apenas BFF (via nginx do
  front) e auth (página de login) expostos ao navegador.

### 3.3 .ai/standards.md — AJUSTAR
- Nova seção "Estrutura por serviço": diretórios caseflow-bff/, case-service/, auth-service/;
  pacote raiz com.caseflow.bff | com.caseflow.core | com.caseflow.auth; camadas
  api / application / domain / infrastructure.
- Nova seção "Comunicação entre serviços": só o BFF é público; navegador nunca chama o core;
  BFF pode adaptar DTOs do core, sem módulo de domínio compartilhado; erros do core repassados
  com code e traceId.
- Segurança: autorização por recurso (autor/ADMIN) no case-service; @PreAuthorize em rotas
  administrativas; proibido identidade por header customizado; exceções de acesso sempre como
  ForbiddenException/ResourceNotFoundException (nunca IllegalState/IllegalArgument → 500).
- Git: Conventional Commits com escopo do serviço, ex.: feat(case-service): ..., fix(bff): ...

### 3.4 .ai/business-rules.md — AJUSTES PONTUAIS
- §2: papéis vêm do claim "roles" do token emitido pelo auth-service; contas provisionadas no
  auth-service (sem cadastro público).
- §6, Critério 3: a regra de validade vale para QUALQUER documento com validUntil, inclusive
  COMPLEMENTAR; documento sem validUntil é considerado vigente; referência = data UTC do envio.
- §7: tentativas automáticas (3 no total, espera 10 s e 30 s) antes de FALHA_TECNICA.

### 3.5 .ai/tasks.md — CRIAR (backlog persistente)
Cabeçalho com instruções para agentes:
- Ler este arquivo antes de qualquer trabalho de código.
- Ao concluir uma tarefa: marcar [x] e anotar data (YYYY-MM-DD) e hash do commit.
- Não iniciar tarefa com dependências pendentes; uma tarefa por commit sempre que possível.
Formato de cada item:
  - [ ] **Txx** — <título> · `<serviço>` · deps: Tyy · Aceite: <critério verificável>

Fases e tarefas (detalhe cada critério de aceite):
Fase 0 — Correções imediatas no código atual
  T01 corrigir import de RoleName em DocumentService.kt (backend compila: ./gradlew build)
  T02 remover backend/pom.xml
  T03 volume de uploads no docker-compose.yml
Fase 1 — Estrutura
  T04 renomear backend/ → case-service/ (pacote com.caseflow.core)
  T05 esqueleto caseflow-bff/ (Gradle KTS, Dockerfile)
  T06 esqueleto auth-service/ (Gradle KTS, Dockerfile)
  T07 docker-compose com 4 apps + script init dos 3 bancos
  T08 Flyway em cada serviço; remover ddl-auto: update
Fase 2 — auth-service
  T09 Authorization Server + cliente caseflow-bff (Authorization Code + PKCE)
  T10 entidades APP_USER/ROLE/USER_ROLE + seed solicitante@caseflow.local e admin@caseflow.local
      (senhas BCrypt, credenciais fora do repositório via env)
  T11 claim "roles" e audience "case-service" no access token
  T12 página de login servida pelo auth
Fase 3 — caseflow-bff
  T13 login/callback OIDC + Spring Session JDBC em bff_db
  T14 GET /csrf, GET /me, POST /logout
  T15 proxy /bff/v1/cases/** e /bff/v1/notifications/** → core com token relay, repassando
      Idempotency-Key, X-Case-Version e X-Request-Id
  T16 upload multipart e download em streaming
  T17 mapeamento de erros do core + 503 quando indisponível
Fase 4 — case-service
  T18 resource server JWT; remover HeaderAuthFilter, usuário padrão e /auth/login
  T19 remover AppUser do core; usar "sub" e "roles" do JWT
  T20 idempotência persistida (IdempotencyRecord) em submit/retry; header obrigatório
  T21 executor: disparo após commit, poller @Scheduled com lease, 3 tentativas, recuperação
      após reinício, falha técnica em transação REQUIRES_NEW; remover Thread.sleep
  T22 motor lê arquivo e confere sha256; remover PDF falso de getDocumentContent
  T23 NotificationService lança ResourceNotFoundException/ForbiddenException
  T24 paginação page/size (padrão 20, máx. 100) em /cases, /history, /notifications
Fase 5 — Frontend
  T25 login por redirect ao BFF; remover seletor de usuário via X-User-Email
  T26 cabeçalho CSRF em escritas
  T27 fallback para mock somente em erro de rede
  T28 updateCase via PUT /bff/v1/cases/{id}
  T29 Idempotency-Key estável por intenção no retry (UUID gerado ao abrir o modal)
Fase 6 — Qualidade e documentação
  T30 teste de isolamento entre dois usuários
  T31 teste de idempotência (mesma chave não cria novo job; payload diferente → 409)
  T32 teste de recuperação após reinício (job SCHEDULED persistido é processado)
  T33 teste de falha técnica por arquivo ausente/hash divergente
  T34 README atualizado (subida com Compose, usuários de demo, roteiro dos critérios de aceite)
Ao final, seção "Critérios de aceite do MVP" copiada da seção 9 de
docs/CaseFlow-Arquitetura-Pratica.md.

### 3.6 .cursorrules e .geminirules — ATUALIZAR
- Adicionar .ai/tasks.md à lista de leitura obrigatória com a regra:
  "Consulte o backlog antes de codificar e atualize o status da tarefa ao concluí-la."
- Trocar a regra de backend única por: "Três serviços Kotlin/Spring Boot 3 — caseflow-bff,
  case-service e auth-service — conforme .ai/architecture.md."
- Manter as demais regras (isolamento de USER, Idempotency-Key, erro padronizado com code/message).

## 4. Restrições
- Não modificar código-fonte, Dockerfiles, docker-compose.yml, README.md nem docs/.
- Não inventar versões de bibliotecas além das já citadas; não adicionar broker, IA ou OCR.
- Nomes de serviços, portas, prefixos e bancos devem ser IDÊNTICOS em todos os arquivos.
- Não apresentar a arquitetura-alvo como já implementada: deixe claro em architecture.md que o
  código atual ainda é monolítico e que a migração segue .ai/tasks.md.

## 5. Verificação (execute e reporte)
1. Todos os blocos Mermaid renderizam (ex.: npx -y @mermaid-js/mermaid-cli em arquivo temporário,
   ou pré-visualização Markdown).
2. grep de consistência: "caseflow-bff", "case-service", "auth-service", "bff_db", "case_db",
   "auth_db", "8080", "8081", "9000", "/bff/v1", "/api/v1" aparecem de forma coerente;
   "X-User-Email" só aparece como prática proibida ou em tarefa de remoção; "caseflow-backend"
   não aparece como estado-alvo.
3. Cada ADR tem ao menos uma tarefa correspondente em .ai/tasks.md; cada tarefa cita o serviço.
4. git diff --stat mostra alterações apenas em .ai/, .cursorrules e .geminirules.
5. Commit: "docs(ai): atualizar arquitetura para BFF, case-service e auth-service e criar backlog".
```
