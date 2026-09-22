# Stack Tecnológica Permitida — CaseFlow

## 1. Backend (Kotlin)
- **Linguagem:** Kotlin 1.9+ / 2.0+ (Target JVM: Java 21 ou 17)
- **Build Tool:** Gradle (Kotlin DSL: `build.gradle.kts`)
- **Framework:** Spring Boot 3.3+
  - `spring-boot-starter-web` (REST APIs e Controllers)
  - `spring-boot-starter-data-jpa` (Persistência relacional com Hibernate)
  - `spring-boot-starter-validation` (Jakarta Validation)
  - `spring-boot-starter-security` (Controle de acesso, papéis e tokens)
  - `jackson-module-kotlin` (Serialização/deserialização JSON idiomática)
- **Banco de Dados:**
  - PostgreSQL 16+ (Perfil de produção / homologação)
  - H2 Database (Perfil local / testes integrados sem necessidade de container ativo)
- **Documentação da API:** SpringDoc OpenAPI 2.x (Swagger UI em `/swagger-ui.html`)
- **Testes:** JUnit 5, MockK ou Mockito-Kotlin, Spring Boot Test

---

## 2. Frontend (React)
- **Linguagem:** TypeScript 5.x
- **Framework / Runtime:** React 18.x / 19.x
- **Tooling / Bundler:** Vite 6.x / 5.x
- **Estilização:** Tailwind CSS 3.x
- **Iconografia:** Lucide React
- **Roteamento:** React Router DOM v6
- **Requisições:** Axios ou Fetch API com interceptors para tokens/CSRF e chave de idempotência

---

## 3. Infraestrutura & DevOps
- **Docker & Docker Compose:** Imagens multi-stage para backend e frontend + PostgreSQL
- **Controle de Versão:** Git com padrão de branches e Conventional Commits
