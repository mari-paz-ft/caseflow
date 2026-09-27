# ADR 001 — Arquitetura do MVP CaseFlow

- **Status:** Aceito como baseline para execução das Fases 0–14
- **Data:** 2026-09-26
- **Escopo:** arquitetura-alvo do MVP; não descreve o estado já implementado no repositório

## Contexto

O repositório atual contém uma aplicação React e um backend Spring Boot que reúne API, autenticação e domínio de casos. Os documentos de arquitetura anteriores propõem decisões diferentes, incluindo acesso do navegador ao serviço de autenticação, OAuth2/OIDC, sessão no BFF e outro arranjo de persistência. Essas propostas não são a arquitetura-alvo aprovada por este ADR.

O MVP será evoluído em fases e não tem como objetivo ser production-ready. Um comportamento interno pode ser mockado quando explicitamente identificado, mas não se pode remover limites entre aplicações nem simular as chamadas entre elas como chamadas locais à mesma aplicação.

## Decisão

A arquitetura física obrigatória contém quatro aplicações independentes e executáveis:

```text
caseflow-web
      |
      v
caseflow-bff
     / \\
    v   v
auth-service   case-service
```

Fluxo de comunicação:

```text
caseflow-web -> caseflow-bff -> auth-service
                            -> case-service
```

O navegador chama somente o BFF. O BFF chama auth-service e case-service por REST/HTTP síncrono. auth-service e case-service não são acessados diretamente pelo frontend. O case-service executa o processamento assíncrono por scheduler interno e jobs persistidos; não há broker ou worker independente no MVP.

### Responsabilidades e persistência

| Aplicação | Responsabilidades no MVP | Persistência |
| --- | --- | --- |
| `caseflow-web` | Apresentação, interação, estado da interface e consumo exclusivo da API do BFF. | Nenhum banco de domínio; não mantém mock funcional que desvie chamadas do BFF. |
| `caseflow-bff` | API de entrada para o frontend, validação de JWT, integração com serviços, composição e transformação explícita de DTOs, propagação de headers e tratamento de erros downstream. Não acessa o banco do case-service. | Sem banco próprio no MVP. |
| `auth-service` | Login mockado, emissão de JWT real, determinação de roles e contratos de identidade. | Conexão PostgreSQL preparada; não é necessário persistir usuários/credenciais no MVP. |
| `case-service` | Domínio de casos, documentos, submissões, regras, análise, notificações, autorização por recurso, idempotência e jobs assíncronos recuperáveis. | PostgreSQL em runtime; H2 nos testes. |

Cada aplicação possui seu próprio processo, configuração e contrato. A implantação local deverá materializar os bancos de auth e case como componentes separados. Não há banco no BFF.

### Autenticação e segurança

- O auth-service aceita qualquer username e password não vazios como autenticação demonstrativa; não há verificação contra uma base real de credenciais.
- Para a demonstração, username contendo `admin` recebe role `ADMIN`; os demais recebem `USER`.
- Apesar da autenticação mockada, o token é JWT real, assinado e validado, com no mínimo `sub`, `roles`, `iat`, `exp` e `iss`.
- O issuer é `caseflow-auth-service`. A chave HMAC é fornecida por configuração/variável de ambiente; segredo de produção não pode ser hardcoded.
- BFF e case-service validam JWT nos limites sob sua responsabilidade. O case-service não confia somente na origem da requisição ou no fato de ela vir do BFF.
- Identidade por `X-User-Email`, fallback para usuário padrão e tokens fictícios não são mecanismos aceitos na arquitetura-alvo.

### Comunicação e processamento

- Comunicação síncrona BFF → auth-service/case-service por REST/HTTP.
- Processamento de análise assíncrono, dentro do case-service, por scheduler e jobs persistidos em PostgreSQL.
- `Idempotency-Key`, `Authorization` e `Correlation-Id` (quando disponível) são preservados pelo BFF conforme os contratos de cada operação.
- Erros downstream são traduzidos para respostas seguras e consistentes; stack traces e detalhes internos não são expostos ao frontend.

### Tecnologias e limites

A implementação segue as tecnologias aprovadas em `.ai/tech-stack.md` depois de sua atualização na Fase 1: Kotlin, Spring Boot, Gradle Kotlin DSL, Spring Security, Spring Data JPA, PostgreSQL em runtime, H2 em testes, React, TypeScript, Docker e Docker Compose. Não se introduzem Kafka, RabbitMQ, Redis, Keycloak, OAuth Provider externo, MinIO, S3, Kubernetes, Service Mesh, Vault ou OpenTelemetry nesta execução.

## Consequências

- O frontend depende de um único contrato HTTP público, o BFF; os serviços de identidade e domínio permanecem substituíveis sem acoplamento do navegador.
- São necessários três backends independentes e dois bancos de serviço na infraestrutura local, ainda que alguns comportamentos internos sejam mockados.
- O mock de credenciais reduz a garantia de identidade e existe somente para demonstração/testes do MVP; não representa autenticação apropriada para produção.
- A validação de JWT é real e independente da autenticação mockada. O segredo HMAC precisa ser configurado em cada ambiente sem ser publicado no repositório.
- O processamento e os registros de idempotência permanecem no case-service e sobrevivem a reinícios por persistência.
- Esta decisão substitui explicitamente as propostas legadas incompatíveis. README e `docs/CaseFlow-Arquitetura-Pratica.md` serão alinhados integralmente na Fase 12; até lá, este ADR é a decisão de baseline do MVP e os documentos legados não devem ser interpretados como decisões vigentes.

## Estado do código no início da execução

Este ADR documenta a arquitetura-alvo, não afirma que ela já foi implementada. No baseline da Fase 0, há um backend monolítico em `backend/`, frontend em `frontend/`, um Compose com frontend/backend/um PostgreSQL, autenticação por header/token mock, fallback em memória no frontend e processamento `@Async`. BFF e auth-service independentes, validação JWT real, scheduler persistido e topologia com quatro aplicações ainda são entregáveis de fases posteriores.
