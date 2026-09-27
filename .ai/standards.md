# Padrões de Código e Convenções — CaseFlow

## 1. Diretrizes Gerais

- Todo código deve ser legível, autocontido e seguir boas práticas de Clean Architecture e SOLID.
- Separar responsabilidades entre camadas: API (Controller/DTO), Aplicação (Service/UseCases), Domínio (Entidades/Regras/Enums) e Infraestrutura (Repositories/Adapters).
- Toda comunicação com IA deve consultar os arquivos de contexto em `.ai/` antes de propor alterações.
- Preservar os limites entre `caseflow-web`, `caseflow-bff`, `auth-service` e `case-service`; cada backend é independente e dono dos próprios contratos/dependências.
- Funcionalidades novas ou alteradas exigem cobertura unitária de 100%; testes de integração/E2E complementam, mas não substituem asserções de comportamento.
- Divergências entre a implementação e as regras devem ser corrigidas na implementação; não alterar regras de negócio para justificar o código existente.
- Não compartilhar DTOs de domínio entre BFF, auth-service e case-service.

---

## 2. Backend — Kotlin & Spring Boot

### Convenções de Nomenclatura
- **Classes e Interfaces:** `PascalCase` (ex: `CaseRequestService`, `ProcessingJobRepository`).
- **Métodos e Variáveis:** `camelCase` (ex: `submitCase`, `rulesVersion`).
- **Enums e Constantes:** `UPPER_SNAKE_CASE` (ex: `CaseStatus.RASCUNHO`, `MAX_ATTACHMENTS = 3`).
- **DTOs / Payloads:** Kotlin `data class` com validações via Jakarta Bean Validation (`@NotBlank`, `@Size`, etc.).

### Padrões Arquiteturais e de Código
- **Injeção de Dependências:** sempre por construtor (`constructor injection`), sem `@Autowired` em campos.
- **Null Safety:** aproveitar o sistema de tipos do Kotlin (`Type?` vs `Type`). Evitar `!!`; usar `?:`, `checkNotNull` ou `requireNotNull` quando apropriado.
- **Imutabilidade:** preferir `val` a `var` e coleções imutáveis (`List`, `Set`, `Map`).
- **Tratamento de Exceções:**
  - Exceções de negócio devem utilizar hierarquia própria de domínio/aplicação.
  - Exceções REST devem ser tratadas por `@RestControllerAdvice`, retornando `ProblemDetail` RFC 7807 ou contrato equivalente com `code`, `message`, `timestamp` e `traceId`.
- **Idempotência:** rotas de mutação crítica definidas em `business-rules.md` exigem `Idempotency-Key` e tratam repetições de forma consistente.
- **DTO Mapping:** usar conversões explícitas (`toDto()`, `toEntity()`) por métodos de extensão ou construtores dedicados.
- **Persistência:** entidades JPA e repositórios pertencem à infraestrutura; casos de uso e regras de domínio não dependem diretamente dos detalhes do banco.
- **BFF:** controllers não fazem chamadas HTTP downstream diretamente; usar clients/adapters dedicados e DTOs próprios. O BFF não acessa os bancos de outros serviços.
- **Processamento Assíncrono:** processamento documental parte de `ProcessingJob` persistido e scheduler interno com lease/recuperação; não depender de `@Async` após a requisição.
- **Armazenamento de Documentos:** acessar filesystem por porta de storage; regras de domínio/aplicação não dependem de `java.nio.file`.
- **Segurança:** propagar `Authorization: Bearer <jwt>` e validar o token nos limites exigidos. Não substituir JWT por headers de identidade.

### Testes

- Priorizar testes de autenticação/JWT, autorização entre usuários, regras documentais, idempotência, scheduler/recuperação, retry, storage e contratos do BFF.
- Usar H2 para testes de persistência quando não houver necessidade explícita de validar comportamento específico do PostgreSQL.
- Não adicionar dependências de teste sem aprovação em `tech-stack.md`.

---

## 3. Frontend — React & TypeScript

### Convenções de Nomenclatura
- **Componentes:** `PascalCase` (ex: `CaseList.tsx`, `DocumentUploader.tsx`, `StatusBadge.tsx`).
- **Hooks:** prefixo `use` em `camelCase` (ex: `useCases.ts`, `useAuth.ts`).
- **Tipos / Interfaces:** `PascalCase` (ex: `CaseRequest`, `CaseDocument`, `UserRole`).
- **Constantes:** `UPPER_SNAKE_CASE` ou `camelCase` agrupado.

### Padrões de Código
- **Componentes Funcionais:** usar exclusivamente funções com TypeScript tipado explicitamente.
- **Estilização:** Tailwind CSS para design system moderno, responsivo e limpo.
- **Ícones:** Lucide React para iconografia consistente e acessível.
- **Estado e Efeitos:** separar lógica de visualização de comunicação com a API e estado assíncrono; extrair hooks quando houver responsabilidade suficiente.
- **Tratamento de Feedback:** exibir estados claros de `loading`, `error`, `empty` e confirmações de sucesso.
- **Integração:** todas as requisições reais do frontend usam exclusivamente o `caseflow-bff`; o JWT fica em memória e `Idempotency-Key` é enviada quando exigida.
- **Erros e Mocks:** propagar erros HTTP 4xx/5xx sem transformá-los em sucesso. Não manter fallback mock funcional no frontend; mocks de teste ficam atrás dos limites de serviço apropriados.

---

## 4. Git & Commits

Usar Conventional Commits:

- `feat:` Nova funcionalidade.
- `fix:` Correção de bug.
- `docs:` Alterações em documentação.
- `refactor:` Refatoração sem alteração de comportamento.
- `test:` Inclusão ou ajuste de testes.
