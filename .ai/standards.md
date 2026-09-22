# Padrões de Código e Convenções — CaseFlow

## 1. Diretrizes Gerais
- Todo código deve ser legível, autocontido e seguir boas práticas de Clean Architecture e SOLID.
- Separação clara de responsabilidades entre camadas: API (Controller/DTO), Aplicação (Service/UseCases), Domínio (Entidades/Regras/Enums) e Infraestrutura (Repositories/Adapters).
- Toda comunicação com IA deve consultar os arquivos de contexto em `.ai/` antes de propor alterações.

---

## 2. Backend — Kotlin & Spring Boot

### Convenções de Nomenclatura
- **Classes e Interfaces:** `PascalCase` (ex: `CaseRequestService`, `ProcessingJobRepository`).
- **Métodos e Variáveis:** `camelCase` (ex: `submitCase`, `rulesVersion`).
- **Enums e Constantes:** `UPPER_SNAKE_CASE` (ex: `CaseStatus.RASCUNHO`, `MAX_ATTACHMENTS = 3`).
- **DTOs / Payloads:** Records ou Kotlin `data class` com validações via Jakarta Bean Validation (`@NotBlank`, `@Size`, etc.).

### Padrões Arquiteturais e de Código
- **Injeção de Dependências:** Sempre por construtor (`constructor injection`), dispensando `@Autowired` direto em campos.
- **Null Safety:** Aproveitar ao máximo o sistema de tipos do Kotlin (`Type?` vs `Type`). Evitar `!!` (double bang operator); usar `?:` (Elvis operator) ou `checkNotNull`/`requireNotNull`.
- **Imutabilidade:** Preferir `val` a `var` e coleções imutáveis (`List`, `Set`, `Map`).
- **Tratamento de Exceções:** 
  - Exceções de negócio devem estender uma classe base de domínio (ex: `CaseFlowException`, `ResourceNotFoundException`, `ConflictException`).
  - Todas as exceções devem ser interceptadas e tratadas por `@RestControllerAdvice` (`GlobalExceptionHandler`), retornando payload padronizado RFC 7807 (`ProblemDetail`) ou `ApiError` com `code`, `message`, `timestamp` e `traceId`.
- **Idempotência:** Rotas de mutação crítica (`/submit`, `/retry`) exigem cabeçalho `Idempotency-Key` e tratam repetições de forma consistente.
- **DTO Mapping:** Métodos de extensão ou construtores de conversão (`toDto()`, `toEntity()`) explícitos.

---

## 3. Frontend — React & TypeScript

### Convenções de Nomenclatura
- **Componentes:** `PascalCase` (ex: `CaseList.tsx`, `DocumentUploader.tsx`, `StatusBadge.tsx`).
- **Hooks:** prefixo `use` em `camelCase` (ex: `useCases.ts`, `useAuth.ts`).
- **Tipos / Interfaces:** `PascalCase` (ex: `CaseRequest`, `CaseDocument`, `UserRole`).
- **Constantes:** `UPPER_SNAKE_CASE` ou `camelCase` agrupado.

### Padrões de Código
- **Componentes Funcionais:** Usar exclusivamente funções com TypeScript tipado explicitamente.
- **Estilização:** Tailwind CSS para design system moderno, responsivo e limpo.
- **Ícones:** Lucide React para iconografia consistente e acessível.
- **Estado e Efeitos:** Separar lógica de visualização (custom hooks para comunicação com API e gerenciamento de estado assíncrono).
- **Tratamento de Feedback:** Exibir estados claros de `loading`, `error`, `empty` e confirmações de sucesso.
- **Mock Service:** Manter adaptador mockável para permitir execução autônoma local sem dependência externa obrigatória durante demonstrações.

---

## 4. Git & Commits
- Padrão Conventional Commits:
  - `feat:` Nova funcionalidade
  - `fix:` Correção de bug
  - `docs:` Alterações em documentação
  - `refactor:` Refatoração sem alteração de comportamento
  - `test:` Inclusão ou ajuste de testes
