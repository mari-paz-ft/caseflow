# Stack Tecnológica Permitida — CaseFlow

## 1. Backend (Kotlin)

- **Aplicações:** `caseflow-bff`, `auth-service` e `case-service`, cada uma com build Gradle próprio.
- **Linguagem / Runtime:** Kotlin 2.0.20 e Java 21 (Target JVM: 21).
- **Build Tool:** Gradle 8.10.2 com Kotlin DSL (`build.gradle.kts`) e Gradle Wrapper. Maven e `pom.xml` não são permitidos.
- **Framework:** Spring Boot 3.3.4.
  - `spring-boot-starter-web` — REST APIs e Controllers.
  - `spring-boot-starter-data-jpa` — persistência dos serviços `auth-service` e `case-service`; o BFF não tem banco.
  - `spring-boot-starter-validation` — Jakarta Validation.
  - `spring-boot-starter-security` — autorização e validação JWT.
  - `jackson-module-kotlin` — JSON idiomático.
  - SpringDoc OpenAPI 2.6.0 — somente no `case-service`.
- **Banco de Dados:** PostgreSQL 16 em runtime; H2 para testes. `auth-service` mantém conexão preparada, sem persistir contas no MVP.
- **Processamento Assíncrono:** Spring Scheduling sobre jobs persistidos em banco, sem broker externo.
- **Testes:** JUnit 5, Spring Boot Test, Kotlin Test e Spring Security Test conforme os manifests dos serviços.

---

## 2. Frontend (React)

- **Aplicação:** `caseflow-web` em `apps/frontend/caseflow-web`.
- **Linguagem:** TypeScript 5.x (manifest `^5.6.3`; versão resolvida no lockfile 5.9.3).
- **Framework / Runtime:** React 18.3.1.
- **Tooling / Bundler:** Vite 6.x (versão resolvida no lockfile 6.4.3).
- **Estilização:** Tailwind CSS 3.x (versão resolvida no lockfile 3.4.19).
- **Iconografia:** Lucide React 0.475.0.
- **Requisições:** Fetch API nativa com Bearer JWT e `Idempotency-Key` conforme o contrato.
- **Integração:** o frontend consome exclusivamente `caseflow-bff`; React Router e Axios não fazem parte da stack atual.
- **Testes:** runner nativo `node --test`.

---

## 3. Infraestrutura & DevOps

- **Docker & Docker Compose:** quatro aplicações e containers PostgreSQL separados para `auth-db` e `case-db`.
- **Comunicação entre containers:** nomes de serviço do Docker Compose; não utilizar `localhost` entre containers.
- **Persistência de documentos:** filesystem local em `./data/documents`, acessado por abstração de storage no `case-service`.
- **Controle de Versão:** Git com padrão de branches e Conventional Commits.
- **Segredos:** configuração por variáveis de ambiente; não hardcodear nem versionar segredos.

Não são dependências obrigatórias Kafka, RabbitMQ, Redis, Keycloak, provedor OIDC externo, S3/MinIO, Kubernetes, Service Mesh, Vault ou OpenTelemetry.
