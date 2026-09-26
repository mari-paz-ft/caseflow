# CaseFlow — Solicitações e Conferência Documental

> **Repositório da Prática:** Implementando a sua Arquitetura (Aulas 1 e 2 do curso *ai-driven-dev*).  
> **Stack:** Backend em **Kotlin** (Spring Boot 3) + Frontend em **React** (TypeScript + Vite + Tailwind CSS).

---

## 📌 Sumário
- [1. Visão Geral do Sistema](#1-visão-geral-do-sistema)
- [2. Estrutura do Repositório](#2-estrutura-do-repositório)
- [3. Governança e Context Engineering (.ai/)](#3-governança-e-context-engineering-ai)
- [4. Entregáveis da Prática](#4-entregáveis-da-prática)
  - [4.1 Prompt de Geração de Contexto](#41-prompt-de-geração-de-contexto)
  - [4.2 Prompt de Implementação](#42-prompt-de-implementação)
  - [4.3 Prompt de Evolução da Arquitetura (3 Microsserviços)](#43-prompt-de-evolução-da-arquitetura-3-microsserviços)
  - [4.4 Roteiro para Gravação do Vídeo de Demonstração](#44-roteiro-para-gravação-do-vídeo-de-demonstração)
- [5. Como Executar o Projeto](#5-como-executar-o-projeto)
  - [5.1 Executando o Frontend React (Demonstração Imediata)](#51-executando-o-frontend-react-demonstração-imediata)
  - [5.2 Executando o Backend Kotlin](#52-executando-o-backend-kotlin)
  - [5.3 Executando via Docker Compose](#53-executando-via-docker-compose)
- [6. Credenciais de Teste e Cenários Mockados](#6-credenciais-de-teste-e-cenários-mockados)
- [7. Verificação dos Critérios de Aceite](#7-verificação-dos-critérios-de-aceite)

---

## 1. Visão Geral do Sistema

O **CaseFlow** centraliza solicitações e a conferência automatizada de documentos comprobatórios através de um fluxo único, auditável e determinístico. 

### Principais Características
- **Backend em Kotlin com Spring Boot 3:** Arquitetura limpa em camadas (API, Aplicação, Domínio e Infraestrutura), tipagem estrita e null-safety nativa.
- **Frontend em React + Vite + TypeScript:** Interface moderna e responsiva com Tailwind CSS e alternador de perfil (USER / ADMIN) para validação rápida de permissões.
- **Dual Mode (Híbrido):** O frontend suporta execução autônoma instantânea com dados mockados em memória reativos ou conexão direta aos endpoints REST do backend Kotlin (`/bff/v1` e `/api/v1`).
- **Motor de Regras (`DOCUMENTAL_V1`):** Conferência automática de documentos obrigatórios (`IDENTIFICACAO` e `COMPROVANTE_ENDERECO`) e validade temporal declarada.
- **Rastreabilidade e Idempotência:** Histórico de auditoria de cada evento com atores (`USER`, `ADMIN`, `SYSTEM`) e proteção com `Idempotency-Key`.

---

## 2. Estrutura do Repositório

```bash
caseflow/
├── .ai/                                  # Diretório de Context Engineering
│   ├── standards.md                      # Padrões de código para Kotlin e React
│   ├── architecture.md                   # Decisões de arquitetura (ADRs) e diagramas
│   ├── tech-stack.md                     # Versões de tecnologias e bibliotecas permitidas
│   └── business-rules.md                 # Regras de negócio, status e permissões
├── .cursorrules                          # Regras de contexto para Cursor / IDEs
├── .geminirules                          # Regras de contexto para Antigravity / Gemini
├── docs/                                 # Documentação e prompts da prática
│   ├── 01-prompt-contexto.md             # Prompt 1: Geração da estrutura de contexto .ai/
│   ├── 02-prompt-implementacao.md        # Prompt 2: Implementação do MVP por agentes autônomos
│   ├── 03-prompt-arquitetura-microservicos.md # Prompt 3: Evolução para 3 microsserviços e backlog
│   └── CaseFlow-Arquitetura-Pratica.md   # Especificação arquitetural e requisitos da prática
├── backend/                              # Microsserviço Backend em Kotlin
│   ├── build.gradle.kts                  # Configuração Gradle com Kotlin DSL
│   ├── pom.xml                           # Configuração Maven alternativa
│   ├── gradlew                           # Gradle Wrapper
│   └── src/
│       ├── main/kotlin/com/caseflow/     # Código-fonte (controller, service, domain, repo)
│       └── test/kotlin/com/caseflow/     # Testes unitários do motor de regras
├── frontend/                             # Aplicação Frontend em React + TypeScript
│   ├── package.json
│   ├── vite.config.ts
│   ├── tailwind.config.js
│   └── src/                              # Componentes, serviços e páginas
├── docker-compose.yml                    # Orquestração completa de containers
└── README.md                             # Documentação principal
```

---

## 3. Governança e Context Engineering (`.ai/`)

Seguindo as boas práticas ensinadas na **Aula 2 (Context Engineering)**, o projeto adota uma "Bússola de Engenharia" para orientar qualquer assistente ou agente de IA:

| Arquivo | Finalidade |
| :--- | :--- |
| [`.ai/standards.md`](.ai/standards.md) | Convenções de nomenclatura, injeção de dependências por construtor, DTOs em data classes, padrões React e formato RFC 7807 para erros. |
| [`.ai/architecture.md`](.ai/architecture.md) | ADRs registrando o uso de Kotlin, executor assíncrono interno, separação entre rejeição de negócio e falha técnica, e idempotência. |
| [`.ai/tech-stack.md`](.ai/tech-stack.md) | Catálogo restrito de versões permitidas (Kotlin 2.x, Spring Boot 3.3, React 18/19, Tailwind CSS, PostgreSQL/H2). |
| [`.ai/business-rules.md`](.ai/business-rules.md) | Regras de validação de formulários, categorias documentais, ciclo de vida de status e matriz de permissões. |

---

## 4. Entregáveis da Prática

### 4.1 Prompt de Geração de Contexto
Disponível em [`docs/01-prompt-contexto.md`](docs/01-prompt-contexto.md).  
Instrui a IA a interpretar o documento de arquitetura (`CaseFlow-Arquitetura-Pratica.md`) e construir com precisão a estrutura `.ai/` com os 4 arquivos essenciais.

### 4.2 Prompt de Implementação
Disponível em [`docs/02-prompt-implementacao.md`](docs/02-prompt-implementacao.md).  
Prompt em formato de instrução para agente autônomo (ex: Google Antigravity) para ler `.ai/` e implementar o código completo do Backend Kotlin e Frontend React.

### 4.3 Prompt de Evolução da Arquitetura (3 Microsserviços)
Disponível em [`docs/03-prompt-arquitetura-microservicos.md`](docs/03-prompt-arquitetura-microservicos.md).  
Prompt para agentes autônomos atualizarem a governança e contexto em `.ai/` para a arquitetura-alvo com 3 microsserviços (BFF, serviço de negócio e serviço de autenticação OAuth2/OIDC) e gerarem o backlog persistente em `.ai/tasks.md`.

### 4.4 Roteiro para Gravação do Vídeo de Demonstração
*(Para a gravação do vídeo entre 3 e 10 minutos exigido na entrega)*:
1. **Introdução (1 min):** Apresentar a estrutura `.ai/` e demonstrar como os prompts guiaram o desenvolvimento do repositório no Google Antigravity.
2. **Visão do Solicitante (2-3 min):**
   - Acessar como Carlos Silva (`ROLE_USER`).
   - Criar uma nova solicitação em rascunho.
   - Anexar documento de `IDENTIFICACAO` e demonstrar o aviso de pendência do `COMPROVANTE_ENDERECO`.
   - Enviar a solicitação e observar a rejeição automática com motivo `FALTA_COMPROVANTE_ENDERECO`.
   - Criar uma solicitação com ambos os documentos válidos e observar a aprovação com veredito `APROVADA`.
3. **Visão do Administrador (2 min):**
   - Alternar para o perfil de Mariana Paz (`ROLE_ADMIN`).
   - Demonstrar a visão global de todas as solicitações do sistema.
   - Abrir a solicitação em `FALHA_TECNICA` e acionar o botão **Reprocessar Falha Técnica**.
   - Preencher a justificativa formal auditável e confirmar a execução.
4. **Comentários Finais (1 min):** Comentar sobre a eficácia do Context Engineering para mitigar AI Drift e garantir conformidade com as regras de negócio.

---

## 5. Como Executar o Projeto

### 5.1 Executando o Frontend React (Demonstração Imediata)
O frontend já possui todas as dependências instaladas e o build validado:

```bash
cd frontend
npm run dev
```

Acesse em seu navegador: **`http://localhost:5173`**

> **Dica:** O frontend possui dados pré-carregados e motor reativo integrado, permitindo demonstrar todas as funcionalidades (criação, anexo, envio, análise assíncrona, notificações e retry de admin) instantaneamente.

---

### 5.2 Executando o Backend Kotlin
Requer JDK 21 instalado:

```bash
cd backend
./gradlew bootRun
```
*(Ou usando Maven: `mvn spring-boot:run`)*

- **API Base:** `http://localhost:8080/bff/v1` e `/api/v1`
- **Swagger / OpenAPI UI:** `http://localhost:8080/swagger-ui.html`
- **Console H2 Database:** `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:case_db`, usuário: `sa`, senha em branco)

Para executar os testes automatizados do motor de regras:
```bash
cd backend
./gradlew test
```

---

### 5.3 Executando via Docker Compose
Para subir o banco PostgreSQL, o backend Kotlin e o frontend React simultaneamente:

```bash
docker-compose up --build
```

---

## 6. Credenciais de Teste e Cenários Mockados

O sistema conta com dois usuários pré-configurados e alternáveis com 1 clique na barra superior:

| Perfil | E-mail | Senha | Papel | Capacidades |
| :--- | :--- | :--- | :--- | :--- |
| **Carlos Silva** | `solicitante@caseflow.local` | `senha123` | `ROLE_USER` | Criação de rascunhos, anexo de arquivos, envio para análise e consulta às próprias solicitações. |
| **Mariana Paz** | `admin@caseflow.local` | `admin123` | `ROLE_ADMIN` | Acesso global para suporte, auditoria e reprocessamento exclusivo de casos em `FALHA_TECNICA` com justificativa. |

### Casos de Demonstração Pré-carregados:
1. `CF-20260922-1001` (**RASCUNHO**): Possui 1 documento anexado. Ideal para testar a inclusão de novos arquivos e o envio para conferência.
2. `CF-20260922-1002` (**APROVADA**): Possui Identificação e Comprovante de Residência válidos, aprovados na regra `DOCUMENTAL_V1`.
3. `CF-20260922-1003` (**REJEITADA**): Demonstra a recusa determinística com motivo `FALTA_COMPROVANTE_ENDERECO`.
4. `CF-20260922-1004` (**FALHA TÉCNICA**): Simulação de falha de infraestrutura. Permite ao Administrador testar o botão **Reprocessar Falha Técnica**.

---

## 7. Verificação dos Critérios de Aceite

| Critério de Aceite (SDD / Aula 1 & 2) | Implementação no CaseFlow |
| :--- | :--- |
| **Isolamento entre usuários** | Solicitante visualiza apenas seus protocolos; Administrador possui visão global auditada. |
| **Aprovação automática** | Rascunho com `IDENTIFICACAO` e `COMPROVANTE_ENDERECO` vigentes resulta em `APROVADA`. |
| **Rejeição com códigos de motivo** | Ausência de documento obrigatório ou validade vencida gera `REJEITADA` com tags claras (`FALTA_IDENTIFICACAO`, etc.). |
| **Idempotência** | Submissão e reprocessamento aceitam cabeçalho `Idempotency-Key` para evitar execuções duplicadas. |
| **Reprocessamento por Administrador** | Exclusivo para status `FALHA_TECNICA`, incrementa `processingRun` e exige justificativa formal de 10 a 500 caracteres. |
| **Rastreabilidade e Histórico** | Linha do tempo auditável de cada transição com ator (`USER`, `ADMIN`, `SYSTEM`) e data/hora. |
