# Prompt 2: Implementação do MVP (Executável por Agentes)

Este prompt foi concebido para ser executado por agentes autônomos de desenvolvimento (como o Google Antigravity, Claude Code ou Cursor Composer) para implementar de ponta a ponta o MVP do CaseFlow com backend em Kotlin e frontend em React.

---

```text
Atue como um Desenvolvedor Fullstack Sênior executando em modo agente.

Você irá implementar o MVP do sistema CaseFlow (Solicitações e Conferência Documental) com Backend em Kotlin (Spring Boot 3) e Frontend em React (TypeScript + Vite + Tailwind CSS).

Antes de iniciar a escrita de código, leia atentamente todos os arquivos em .ai/ (.ai/standards.md, .ai/architecture.md, .ai/tech-stack.md, .ai/business-rules.md) e siga estritamente todas as diretrizes definidas.

Objetivos de Implementação:

1. BACKEND (Kotlin + Spring Boot 3):
   - Estrutura em camadas: controller (API/DTO), service (Regras de negócio e Jobs), domain (Entidades JPA, Enums, Exceptions) e repository.
   - Entidades: CaseRequest, CaseDocument, ProcessingJob, ProcessingResult, CaseHistory, Notification, AppUser, UserRole.
   - Enums: CaseStatus (RASCUNHO, ENVIADA, PROCESSANDO, APROVADA, REJEITADA, FALHA_TECNICA), DocumentCategory (IDENTIFICACAO, COMPROVANTE_ENDERECO, COMPLEMENTAR).
   - Motor de Regras: Validar presença obrigatória de IDENTIFICACAO e COMPROVANTE_ENDERECO, validade não expirada, e gerar resultado determinístico com códigos de motivo (ex: FALTA_IDENTIFICACAO, FALTA_COMPROVANTE_ENDERECO, DOCUMENTO_VENCIDO).
   - Suporte a reprocessamento por ADMIN em FALHA_TECNICA com justificativa.
   - Idempotência suportada no envio e retry com cabeçalho Idempotency-Key.
   - Dados iniciais mockados/seed no bootstrap:
     * Usuário Solicitante (solicitante@caseflow.local / senha123)
     * Usuário Administrador (admin@caseflow.local / admin123)
     * Exemplos pré-carregados de solicitações em diferentes estados (Rascunho, Aprovada, Rejeitada com motivos, Falha Técnica).
   - Perfil H2 pronto para execução local sem dependências de containers obrigatórias.

2. FRONTEND (React + TypeScript + Vite + Tailwind CSS):
   - Interface limpa, responsiva e intuitiva inspirada em plataformas corporativas modernas.
   - Alternador de Usuário (Simulação de Login): Permite alternar rapidamente entre o perfil do Solicitante (USER) e do Administrador (ADMIN) para testar os critérios de aceite e isolamento de dados.
   - Funcionalidades das telas:
     * Listagem de solicitações com filtros por status e busca.
     * Criação de novo rascunho (título e descrição com contadores e validações).
     * Gestão de documentos: Anexar com categoria e data de validade, pré-visualizar/baixar, remover.
     * Envio para análise com confirmação e alerta de categorias pendentes.
     * Visualização detalhada do resultado: Cartão de aprovação/rejeição com tags de códigos de motivo e data.
     * Linha do tempo de histórico rastreável com atores (USER, ADMIN, SYSTEM).
     * Ação de reprocessamento visível apenas para ADMIN quando o caso estiver em FALHA_TECNICA com campo de justificativa.
     * Central de Notificações com badge de não lidas e marcação como lida.
   - Provedor Híbrido: O frontend deve conter uma camada de serviço que pode se comunicar com a API REST real do Spring Boot e também fornecer fallback mock completo em memória para demonstração instantânea via `npm run dev`.

3. INFRAESTRUTURA E SCRIPTS:
   - docker-compose.yml para subir toda a stack opcionalmente em containers.
   - README.md abrangente com instruções passo a passo para executar o backend e o frontend, além do roteiro de teste do MVP cobrindo todos os critérios de aceite.

Entregue o código completo, compilável e executável localmente.
```
