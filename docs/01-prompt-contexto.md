# Prompt 1: Geração de Contexto (.ai/)

Este prompt foi desenvolvido para ser executado no assistente de IA (ex: ChatGPT, Claude, Antigravity) para analisar o documento de arquitetura (`CaseFlow-Arquitetura-Pratica.md`) e gerar automaticamente a estrutura canônica de contexto do projeto.

---

```text
Atue como um Engenheiro de Software Sênior e Especialista em Context Engineering.

Com base nas diretrizes do documento de arquitetura "CaseFlow-Arquitetura-Pratica.md", crie a estrutura de governança de contexto para desenvolvimento assistido por IA dentro do diretório .ai/:

.ai/
├── standards.md        # Convenções de código e estilo (Kotlin e React)
├── architecture.md     # Decisões de alto nível (ADRs) e diagramas
├── tech-stack.md       # Versões e bibliotecas permitidas
└── business-rules.md   # Lógica de negócio, permissões e domínio

Requisitos para a geração:
1. Em "standards.md":
   - Defina as convenções de código para o Backend em Kotlin com Spring Boot 3 (nomenclatura, injeção por construtor, imutabilidade, null-safety, DTOs em data classes, GlobalExceptionHandler com ProblemDetail, idempotência).
   - Defina as convenções para o Frontend em React com TypeScript (componentes funcionais tipados, Tailwind CSS, Lucide icons, tratamento de estados de loading e erro).

2. Em "architecture.md":
   - Registre as decisões de arquitetura (ADRs) essenciais: Backend Kotlin Spring Boot, desacoplamento de microserviços e BFF, processamento assíncrono interno durável com PostgreSQL/H2, separação estrita entre Rejeição de Negócio e Falha Técnica, e idempotência com Idempotency-Key.
   - Inclua diagrama Mermaid de arquitetura do sistema e camadas.

3. Em "tech-stack.md":
   - Especifique a stack autorizada: Kotlin 2.x, Spring Boot 3.3+, Spring Data JPA, Spring Security, SpringDoc OpenAPI, PostgreSQL 16 / H2, React 18/19, TypeScript 5, Vite, Tailwind CSS.

4. Em "business-rules.md":
   - Sintetize a matriz de permissões (USER vs ADMIN).
   - Especifique o ciclo de vida dos status (RASCUNHO -> ENVIADA -> PROCESSANDO -> APROVADA / REJEITADA / FALHA_TECNICA).
   - Descreva as regras de documentos (máximo 3, categorias IDENTIFICACAO, COMPROVANTE_ENDERECO e COMPLEMENTAR, limite 5MB, validade não vencida em relação ao envio UTC).
   - Detalhe a regra de reprocessamento por ADMIN em FALHA_TECNICA com justificativa obrigatória.

Crie os arquivos diretamente na pasta .ai/ na raiz do repositório.
```
