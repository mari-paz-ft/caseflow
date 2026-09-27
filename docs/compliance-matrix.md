# Matriz de aderência — CaseFlow MVP

**Data da revisão:** 26/09/2026

**Escopo:** regras críticas dos quatro arquivos `.ai/`, ADR 001 e evidências do worktree validado. O status registra o que foi observado/testado; não é uma certificação de prontidão para produção.

## Legenda

- **OK** — implementação e evidência de teste/runtime identificadas.
- **PARTIAL** — implementação observada, mas a cobertura automatizada ou a garantia concorrente é incompleta.
- **FAIL** — divergência demonstrável em relação à regra/fonte.
- **MOCKED** — comportamento deliberadamente mockado no MVP, sem remover a fronteira do serviço.
- **OUT OF SCOPE** — explicitamente excluído do MVP.

Não há regra crítica marcada como desconhecida; os `FAIL` e `PARTIAL` abaixo são débitos explícitos.

## Arquitetura, identidade e autorização

| Regra crítica | Fonte | Implementação observada | Evidência | Status |
| --- | --- | --- | --- | --- |
| Quatro aplicações independentes; web usa somente o BFF | `.ai/architecture.md` §§2–5 | Compose tem web/BFF/auth/case; Nginx e `ApiService` apontam ao BFF | `apps/frontend/caseflow-web/tests/api.test.mjs`; `npm run test:e2e` via Nginx; seis healthchecks Compose | OK |
| BFF sem banco próprio; downstream por clients e DTOs BFF | `.ai/architecture.md` §3 | BFF mantém `AuthServiceClient`, `CaseServiceClient` e DTOs próprios; sem dependência JPA | `BffIntegrationTest`; stack web→BFF→serviços | OK |
| Credenciais são mockadas; JWT é real | `.ai/business-rules.md` §2; ADR 001 | username/password não vazios; `admin` determina role; HMAC-SHA256 e claims assinados | `AuthServiceJwtTest` (6); `auth-service` valida token assinado | MOCKED |
| Claims, issuer, expiração, TTL e segredo por ambiente | `.ai/architecture.md` §4 | `sub`, roles, `iat`, `exp`, `iss=caseflow-auth-service`; TTL 3600; segredo exigido | `AuthServiceJwtTest`; configuração dos três backends e Compose | OK |
| `sub` UUID determinístico e migração dos sujeitos legados | `.ai/business-rules.md` §2 | Derivação UUID no auth-service; migração de casos/notificações no case-service | `AuthServiceJwtTest`; `CaseAuthorizationTest` migra sem apagar registros | OK |
| Autorização por ownership e papel; ADMIN consulta, mas não edita rascunho alheio | `.ai/business-rules.md` §2 | `CaseAuthorizationPolicy` aplicado em casos/documentos/notificações | `CaseAuthorizationTest` (7); E2E USER B recebe 403 e ADMIN recebe 200 | OK |
| Notificações são visíveis/marcáveis somente pelo destinatário | `.ai/business-rules.md` §9 | Controller consulta pelo `recipientSubject`; serviço valida destinatário na marcação | `CaseAuthorizationTest` verifica que USER não lista notificação alheia | OK |
| Retry administrativo requer ADMIN e caso em `FALHA_TECNICA`, com justificativa | `.ai/business-rules.md` §§2,8 | Policy ADMIN, validação 10–500, estado verificado antes da criação de job | `IdempotencyTest` (9); `npm run test:e2e` executa retry e replay ADMIN | OK |

## Regras de caso, documentos e resultado

| Regra crítica | Fonte | Implementação observada | Evidência | Status |
| --- | --- | --- | --- | --- |
| Tipo `ANALISE_DOCUMENTAL`, título 5–120, descrição 20–2.000 | `.ai/business-rules.md` §4 | Bean Validation no case-service e BFF | `CaseRulesTest` (2) valida tipo e limites | OK |
| Versão impede sobrescrita concorrente de rascunho | `.ai/business-rules.md` §4 | `@Version` e comparação explícita em update/submit | `CaseAuthorizationTest`: duas PUTs concorrentes com a mesma versão produzem uma resposta 200, uma 409, uma única atualização de versão e um único evento de histórico | OK |
| Submit exige ao menos um documento `READY`; categoria obrigatória ausente pode ser rejeitada pela análise | `.ai/business-rules.md` §§5–6 | Submit recusa lista vazia ou somente documentos não prontos; análise produz códigos de pendência | `IdempotencyTest` verifica estado pendente; `AnalysisEngineServiceTest` e E2E verificam rejeição | OK |
| Até três documentos ativos, no máximo um por categoria | `.ai/business-rules.md` §5 | Upload substitui documento da mesma categoria e valida o conjunto das categorias | `DocumentStorageTest`: aceita as três categorias distintas, substitui a categoria existente, mantém somente três anexos ativos e remove o arquivo substituído | OK |
| PDF-only, MIME, limite real de 5 MiB e assinatura básica `%PDF-` | `.ai/business-rules.md` §5 | `PdfDocumentRules` valida MIME, bytes e cabeçalho | `DocumentStorageTest`: PDF válido, >5 MiB, MIME, assinatura, arquivo ausente, ownership, substituição/categorias e tamanho/hash divergentes | OK |
| Data de referência da validade usa UTC do envio | `.ai/business-rules.md` §§5–6 | Submit grava `submittedAt` em UTC; motor usa esse dia e fallback UTC | `IdempotencyTest` altera timezone padrão e verifica timestamp; análise testa documento vencido | OK |
| Arquivo ausente/ilegível/integridade divergente é falha técnica; nunca fabricar bytes PDF | `.ai/business-rules.md` §5 | Storage local lê bytes reais e compara assinatura, tamanho e SHA-256; falha entra em retry técnico | `DocumentStorageTest` e `AnalysisEngineServiceTest` cobrem arquivo ausente e divergência independente de tamanho/hash, tratadas como falha técnica | OK |
| Regras determinísticas aprovam com categorias válidas e rejeitam por pendência/validade | `.ai/business-rules.md` §6 | `AnalysisEngineService` calcula decisão e `reasonCodes` | `AnalysisEngineServiceTest` (5); E2E verifica `APROVADA` e `REJEITADA` | OK |

## Idempotência, jobs e notificações

| Regra crítica | Fonte | Implementação observada | Evidência | Status |
| --- | --- | --- | --- | --- |
| Submit/retry exigem `Idempotency-Key`, persistem contexto e resposta; replay não duplica job | `.ai/business-rules.md` §7 | `IdempotencyRecord`, hash contextual e constraint única por operação/chave | `IdempotencyTest` cobre replay sequencial/concorrente; E2E verifica `processingRun` estável no replay | OK |
| Mesmo key com contexto diferente conflita | `.ai/business-rules.md` §7 | Hash de ator, recurso, operação e payload; resposta 409 | `IdempotencyTest` cobre submit e retry | OK |
| Estados/job fields persistidos e lease impede claim simultâneo | `.ai/business-rules.md` §8 | Job inclui state/count/availability/lease/token/error/timestamps; adapter consulta candidatos com `PESSIMISTIC_WRITE` | `ProcessingJobRecoveryTest`: duas threads/transações concorrentes no H2 reivindicam um único job, token e evento de início | OK |
| Job pendente/lease expirado recupera após restart | `.ai/business-rules.md` §8 | Poller retoma `SCHEDULED` e `RUNNING` com lease expirado | `ProcessingJobRecoveryTest`; E2E reinicia case-service com job técnico agendado | OK |
| Retry técnico: +10 s, +30 s, terceira falha termina `FAILED`/`FALHA_TECNICA`; rejeição não incrementa | `.ai/business-rules.md` §8 | `TechnicalRetryPolicy`; erro transitório reagenda, decisão de negócio conclui job | `ProcessingJobRetryTest` (2); `AnalysisEngineServiceTest`; E2E atinge falha terminal | OK |
| Toda falha técnica notifica titular, inclusive retries automáticos | `.ai/business-rules.md` §9 | `markTechnicalFailure` cria notificação por tentativa | `ProcessingJobRetryTest` conta uma por falha; E2E observa ao menos três | OK |
| Resultado final também gera notificação ao titular | `.ai/business-rules.md` §9 | `AnalysisEngineService` chama NotificationService após decisão | `AnalysisEngineServiceTest` verifica a chamada exata uma vez para resultado aprovado e rejeitado e nenhuma interação adicional | OK |

## Contratos, frontend e operação

| Regra crítica | Fonte | Implementação observada | Evidência | Status |
| --- | --- | --- | --- | --- |
| BFF propaga Authorization, Idempotency-Key e Correlation-Id; erros downstream seguros | `.ai/architecture.md` §3; `.ai/standards.md` §4 | Clients REST dedicados, relay e BffExceptionHandler; mensagem 5xx saneada e resposta com traceId | `BffIntegrationTest` (5) | OK |
| Frontend envia Bearer, usa rotas BFF, mantém chave de submit e não mascara 4xx/5xx | `.ai/standards.md` §5 | `ApiService` usa login `/api/v1/auth/login` e negócios `/bff/v1`; erros lançados | `apps/frontend/caseflow-web/tests/api.test.mjs` (7), build e E2E pela origem Nginx | OK |
| BFF não expõe serviços/database; auth/case DB e volumes separados | `.ai/architecture.md` §§3,6 | Compose possui `auth-db`/`case-db`, volumes próprios, storage local, nomes DNS internos | `docker compose config`; seis containers saudáveis via `docker compose ps` | OK |
| Credenciais/segredos não são versionados; Compose exige secret JWT e senhas de DB | ADR 001; `.ai/architecture.md` §3 | `${CASEFLOW_JWT_SECRET:?}`, `AUTH_DB_PASSWORD`, `CASE_DB_PASSWORD`; `.env` ignorado | configuração Compose; nenhum valor de segredo na matriz/repositório | OK |
| Tecnologias proibidas não são adicionadas | `.ai/tech-stack.md` | Nenhum broker, Redis, OIDC externo, S3, OCR ou nova dependência de teste | Gradle/Compose/manifests e busca estática | OK |

## Divergências de arquitetura e lacunas conhecidas

| Regra crítica | Fonte | Evidência da divergência | Status |
| --- | --- | --- | --- |
| Domínio não deve depender de JPA/Spring/HTTP | `.ai/standards.md` §2 | Modelos e exceções de domínio são Kotlin puro; handler e DTO de erro HTTP estão em `interfaces/rest` | `ArchitectureBoundariesTest` verifica imports proibidos; `GlobalExceptionHandlerTest` cobre tradução de erros HTTP | OK |
| Portas de persistência devem preservar direção de dependências | `.ai/standards.md` §§2–3 | `apps/backend/case-service/src/main/kotlin/com/caseflow/application/port/Repositories.kt` declara contratos Kotlin; entidades, queries e adapters Spring Data estão em `infrastructure/persistence` | `ArchitectureBoundariesTest` verifica que portas não importam framework nem camadas externas | OK |
| Caso de uso de upload não deve depender do transporte HTTP | `.ai/standards.md` §§2–3 | `CaseController` converte multipart em `UploadDocumentCommand`; `DocumentService` recebe metadados e bytes | Compilação/testes do case-service; `DocumentService` não importa MultipartFile ou Spring Web | OK |
| Reivindicação/idempotência sob concorrência real | `.ai/business-rules.md` §§7–8 | Locks pessimistas e constraint única são exercitados por transações concorrentes no H2 | `ProcessingJobRecoveryTest` prova claim único; `IdempotencyTest` prova replay e conflito tipado para corrida da mesma chave em recursos diferentes; testes H2 não equivalem a teste de carga multi-instância PostgreSQL | OK |
| Testes específicos de versão, limite/substituição de categorias e integridade corrompida | `.ai/business-rules.md` §§4–5 | Cenários cobertos em testes concorrentes e de storage/análise | `CaseAuthorizationTest`, `DocumentStorageTest` e `AnalysisEngineServiceTest` cobrem versão, categorias/substituição e tamanho/hash divergentes | OK |

## Tratamento de erros

| Regra crítica | Fonte | Implementação observada | Evidência | Status |
| --- | --- | --- | --- | --- |
| Erro inesperado do case-service não expõe detalhes internos | `.ai/standards.md` §3 | `GlobalExceptionHandler` retorna mensagem genérica sem `Exception.message` | `GlobalExceptionHandlerTest` (1) | OK |

## Regras complementares e limites aprovados

| Regra crítica | Fonte | Implementação observada | Evidência | Status |
| --- | --- | --- | --- | --- |
| Estados `RASCUNHO`→`ENVIADA`→`PROCESSANDO`→decisão final; rejeição não reenvia o mesmo caso | `.ai/business-rules.md` §3 | `CaseService` e `AnalysisEngineService` aplicam transições; atualização de não-rascunhos conflita | `AnalysisEngineServiceTest`; E2E aprovação/rejeição | OK |
| Replay idempotente não duplica histórico/notificações além do job | `.ai/business-rules.md` §7, §9 | Caminho de replay retorna resposta antes de novas transições/efeitos | `IdempotencyTest` verifica um job e um evento de histórico para replay de submit/retry; quantidade de notificações no replay não tem asserção específica | PARTIAL |
| Notificações permanecem internas, sem e-mail/SMS ou integração externa | `.ai/business-rules.md` §9 | Persistência/consulta no case-service; nenhuma integração externa aprovada | `NotificationService`; dependências e Compose | OUT OF SCOPE (integrações externas) |
| OCR/IA, comprovação de autenticidade e aprovação humana não fazem parte do resultado | `.ai/architecture.md` §7; `docs/mvp-scope.md` | Motor determinístico usa regras documentais e integridade/disponibilidade de arquivos | `AnalysisEngineServiceTest`; limites explicitados no README | OUT OF SCOPE |

## Validação independente de JWT

| Regra crítica | Fonte | Implementação observada | Evidência | Status |
| --- | --- | --- | --- | --- |
| BFF e case-service validam JWT independentemente | `.ai/architecture.md` §4; `.ai/business-rules.md` §2 | `BffJwtVerifier` e `HmacJwtVerifier` validam assinatura, issuer, sujeito, expiração e roles | `BffIntegrationTest` e `CaseAuthorizationTest` negam token ausente/adulterado em cada fronteira | OK |

## Stack e build

| Regra crítica | Fonte | Implementação observada | Evidência | Status |
| --- | --- | --- | --- | --- |
| Kotlin, Java 21, Spring Boot, Gradle Kotlin DSL; PostgreSQL runtime/H2 testes; React/TypeScript | `.ai/tech-stack.md` | Wrappers e manifests fixam a stack por serviço | Gradle `clean test` nos três backends, `npm ci`, `npm test`, build Vite e Compose rebuild | OK |

## Experiência visual

| Regra crítica | Fonte | Implementação observada | Evidência | Status |
| --- | --- | --- | --- | --- |
| UI apresenta loading, erro, vazio e sucesso de forma perceptível | `.ai/standards.md` §5 | `App`, `CaseDetail` e `LoginForm` implementam os estados; checklist manual reprodutível em `docs/ui-acceptance-checklist.md` | Ainda aguardando execução e evidência manual no navegador | PARTIAL |

## Conclusão da revisão

As divergências arquiteturais de domínio/persistência foram corrigidas e verificadas por testes. A cobertura de concorrência usa transações H2 e não substitui teste de carga com múltiplas instâncias PostgreSQL. O replay idempotente retorna antes de repetir efeitos, mas não há asserção específica de quantidade para histórico/notificações; esse cenário permanece `PARTIAL`. O checklist visual manual também aguarda execução registrada no navegador; nenhum resultado visual foi presumido.
