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
  - [5.1 Executando o Frontend React](#51-executando-o-frontend-react)
  - [5.2 Executando os Serviços Backend Kotlin](#52-executando-os-serviços-backend-kotlin)
  - [5.3 Executando via Docker Compose](#53-executando-via-docker-compose)
- [6. Credenciais de Teste e Cenários Mockados](#6-credenciais-de-teste-e-cenários-mockados)
- [7. Verificação dos Critérios de Aceite](#7-verificação-dos-critérios-de-aceite)

---

## 1. Visão Geral do Sistema

O **CaseFlow** centraliza solicitações e a conferência automatizada de documentos comprobatórios através de um fluxo único, auditável e determinístico.

### Principais Características
- **Quatro aplicações independentes:** `caseflow-web`, `caseflow-bff`, `auth-service` e `case-service`, com responsabilidades e processos separados.
- **Frontend em React + Vite + TypeScript:** interface responsiva que consome exclusivamente as rotas públicas do BFF; não há alternador local de perfil nem fallback de negócio em memória.
- **Backend em Kotlin com Spring Boot 3:** serviços independentes para BFF, autenticação e domínio documental, com persistência PostgreSQL separada para auth e case.
- **Autenticação demonstrativa com JWT real:** credenciais não vazias são aceitas pelo auth-service; username contendo `admin` recebe papel `ADMIN` e os demais `USER`. Os serviços validam assinatura, emissor e expiração do token.
- **Motor de Regras (`DOCUMENTAL_V1`):** conferência determinística das categorias obrigatórias (`IDENTIFICACAO` e `COMPROVANTE_ENDERECO`) e validade declarada; não comprova autenticidade ou conteúdo material.
- **Processamento durável:** scheduler e jobs persistidos no case-service, com recuperação após restart, retries técnicos e proteção por `Idempotency-Key`.

---

## 2. Estrutura do Repositório

```bash
caseflow/
├── .ai/                                      # Configuração canônica do projeto
├── AGENTS.md                                 # Índice genérico dos arquivos de configuração
├── .cursorrules                              # Referências às configurações .ai/ para Cursor
├── .geminirules                              # Referências às configurações .ai/ para Gemini
├── apps/
│   ├── backend/
│   │   ├── auth-service/                     # Login demonstrativo e JWT
│   │   ├── case-service/                     # Domínio, PostgreSQL, storage e scheduler
│   │   └── caseflow-bff/                     # API pública e clients downstream
│   └── frontend/
│       └── caseflow-web/                     # Interface React + TypeScript
├── docs/
│   ├── 01-prompt-contexto.md
│   ├── 02-prompt-implementacao.md
│   ├── 03-prompt-arquitetura-microservicos.md
│   ├── adr/                                  # Decisões de arquitetura
│   ├── api-contracts.md                      # Contratos HTTP
│   ├── CaseFlow-Arquitetura-Pratica.md       # Arquitetura e fluxos
│   ├── compliance-matrix.md                  # Aderência às regras críticas
│   ├── mvp-scope.md                          # Escopo, mocks e limitações
│   └── ui-acceptance-checklist.md             # Checklist visual manual
├── docker-compose.yml                        # Quatro apps e dois bancos PostgreSQL
└── README.md                                 # Documentação principal
```

---

## 3. Governança e Context Engineering (`.ai/`)

Seguindo as boas práticas ensinadas na **Aula 2 (Context Engineering)**, o projeto adota uma "Bússola de Engenharia" para orientar qualquer assistente ou agente de IA:

| Arquivo | Finalidade |
| :--- | :--- |
| [`.ai/standards.md`](.ai/standards.md) | Convenções de nomenclatura, injeção por construtor, DTOs em data classes, padrões React e erros seguros. |
| [`.ai/architecture.md`](.ai/architecture.md) | Decisões sobre quatro aplicações, executor interno persistido, segurança e idempotência. |
| [`.ai/tech-stack.md`](.ai/tech-stack.md) | Catálogo restrito de Kotlin 2.x, Spring Boot 3.3, React 18, Tailwind CSS, PostgreSQL e H2. |
| [`.ai/business-rules.md`](.ai/business-rules.md) | Validação de formulários, categorias documentais, ciclo de vida de status e matriz de permissões. |

O arquivo [`AGENTS.md`](AGENTS.md) é um índice genérico sem regras próprias. `.cursorrules` e `.geminirules` apontam diretamente para os quatro arquivos `.ai/` e não mantêm cópias das regras. A configuração canônica permanece na pasta `.ai/`.

As decisões de baseline estão em [`docs/adr/001-mvp-architecture.md`](docs/adr/001-mvp-architecture.md); o escopo observado, mocks e itens fora do MVP estão em [`docs/mvp-scope.md`](docs/mvp-scope.md).

---

## 4. Entregáveis da Prática

### 4.1 Prompt de Geração de Contexto
Disponível em [`docs/01-prompt-contexto.md`](docs/01-prompt-contexto.md).  
Instrui a IA a interpretar o documento de arquitetura (`CaseFlow-Arquitetura-Pratica.md`) e construir com precisão a estrutura `.ai/` com os 4 arquivos essenciais.

### 4.2 Prompt de Implementação
Disponível em [`docs/02-prompt-implementacao.md`](docs/02-prompt-implementacao.md).  
Prompt em formato de instrução para agente autônomo (ex: Google Antigravity) ler `.ai/` e implementar o código do Backend Kotlin e Frontend React nas quatro aplicações independentes, preservando os contratos reais entre elas.

### 4.3 Prompt de Evolução da Arquitetura (3 Microsserviços)
Disponível em [`docs/03-prompt-arquitetura-microservicos.md`](docs/03-prompt-arquitetura-microservicos.md).  
Prompt para agentes autônomos atualizarem a governança e o contexto `.ai/` para três serviços backend (`caseflow-bff`, `case-service` e `auth-service`) e o frontend independente `caseflow-web`. A autenticação aceita credenciais demonstrativas e usa JWT Bearer real, sem sessão no BFF.

Os contratos HTTP estão em [`docs/api-contracts.md`](docs/api-contracts.md) e a rastreabilidade das regras está em [`docs/compliance-matrix.md`](docs/compliance-matrix.md).

### 4.4 Roteiro para Gravação do Vídeo de Demonstração
*(Para a gravação do vídeo entre 3 e 10 minutos exigido na entrega)*:
1. **Introdução (1 min):** apresentar a estrutura `.ai/` e demonstrar como os prompts orientam as quatro aplicações independentes.
2. **Visão do Solicitante (2–3 min):**
   - Entrar como `solicitante@caseflow.local` com qualquer senha não vazia.
   - Criar uma nova solicitação em rascunho.
   - Anexar documento `IDENTIFICACAO` e demonstrar a pendência de `COMPROVANTE_ENDERECO`.
   - Enviar a solicitação e observar a rejeição automática com motivo `FALTA_COMPROVANTE_ENDERECO`.
   - Criar uma solicitação com ambos os documentos válidos e observar a aprovação `APROVADA`.
3. **Visão do Administrador (2 min):**
   - Entrar como `admin@caseflow.local` com qualquer senha não vazia.
   - Demonstrar a visão global das solicitações.
   - Abrir uma solicitação em `FALHA_TECNICA` e acionar **Reprocessar Falha Técnica**.
   - Preencher justificativa formal de 10 a 500 caracteres e confirmar.
4. **Comentários Finais (1 min):** comentar sobre Context Engineering, JWT validado nas fronteiras e recuperação dos jobs após restart.

---

## 5. Como Executar o Projeto

### 5.1 Executando o Frontend React

O frontend consome apenas o BFF. Para executá-lo isoladamente, inicie auth-service, case-service e caseflow-bff com as dependências PostgreSQL configuradas.

```bash
cd apps/frontend/caseflow-web
npm ci
npm test
npm run build
npm run dev
```

Acesse em seu navegador: **`http://localhost:5173`**. O proxy Vite encaminha `/api/v1` e `/bff/v1` para o BFF em `localhost:8081`; erros HTTP são apresentados à UI, sem fallback que os transforme em sucesso.

### 5.2 Executando os Serviços Backend Kotlin

Requer JDK 21 instalado e PostgreSQL configurado. Execute cada serviço em um terminal separado; o build backend usa somente Gradle Kotlin DSL.

```bash
cd apps/backend/auth-service
./gradlew bootRun
```

```bash
cd apps/backend/case-service
./gradlew bootRun
```

```bash
cd apps/backend/caseflow-bff
./gradlew bootRun
```

Portas locais padrão: auth-service `8082`, case-service `8080` e caseflow-bff `8081`. Configure `AUTH_DB_URL`, `AUTH_DB_USERNAME`, `AUTH_DB_PASSWORD`, `CASE_DB_URL`, `CASE_DB_USERNAME`, `CASE_DB_PASSWORD` e `CASEFLOW_JWT_SECRET` conforme o ambiente. Os testes backend usam H2; não há Console H2 de runtime.

Para executar testes e gerar o JAR, entre no diretório de cada serviço:

```bash
cd apps/backend/auth-service
./gradlew clean test
./gradlew bootJar
```

```bash
cd apps/backend/case-service
./gradlew clean test
./gradlew bootJar
```

```bash
cd apps/backend/caseflow-bff
./gradlew clean test
./gradlew bootJar
```

### 5.3 Executando via Docker Compose

Crie um `.env` local na raiz (ignorado pelo Git) ou exporte as variáveis necessárias:

```dotenv
CASEFLOW_JWT_SECRET=<segredo local com pelo menos 32 bytes>
AUTH_DB_PASSWORD=<senha local para auth-db>
CASE_DB_PASSWORD=<senha local para case-db>
```

Substitua os marcadores por valores locais reais; não versione nem compartilhe os valores. `AUTH_DB_USERNAME` e `CASE_DB_USERNAME` são opcionais e usam `caseflow` por padrão.

```bash
docker compose config --quiet
docker compose up --build -d
docker compose ps
```

O Compose executa `caseflow-web`, `caseflow-bff`, `auth-service`, `case-service`, `auth-db` e `case-db`. A interface fica em `http://localhost:5173`; o BFF é publicado em `http://localhost:8081`. Auth-service, case-service e PostgreSQL ficam na rede interna. Documentos persistem no bind mount `./data/documents`; os bancos usam volumes nomeados separados.

Com a stack saudável, o teste E2E pode ser executado em outro terminal:

```bash
cd apps/frontend/caseflow-web
npm run test:e2e
```

O teste reinicia o case-service e grava casos, documentos e notificações nos volumes persistentes; esses dados não são removidos automaticamente.

Para parar sem apagar dados persistidos:

```bash
docker compose down
```

Não use `docker compose down -v` se quiser preservar volumes. O antigo volume monolítico `pgdata` não é migrado automaticamente para `case-db` e não é removido pelo Compose atual.

---

## 6. Credenciais de Teste e Cenários Mockados

O auth-service não persiste contas no MVP. Qualquer username e senha não vazios são aceitos; username contendo `admin` recebe `ADMIN` e os demais recebem `USER`. Apesar da autenticação de credenciais ser demonstrativa, os JWTs são assinados e validados de verdade.

| Exemplo de username | Senha | Papel | Capacidades |
| :--- | :--- | :--- | :--- |
| `solicitante@caseflow.local` | Qualquer valor não vazio | `ROLE_USER` | Criação de rascunhos, anexos, envio e consulta às próprias solicitações de demonstração. |
| `admin@caseflow.local` | Qualquer valor não vazio | `ROLE_ADMIN` | Consulta global e retry exclusivo de casos em `FALHA_TECNICA`, com justificativa. |

### Casos de Demonstração Pré-carregados
1. `CF-20260922-1001` (**RASCUNHO**): rascunho com metadados de documento.
2. `CF-20260922-1002` (**APROVADA**): identificação e comprovante de endereço válidos.
3. `CF-20260922-1003` (**REJEITADA**): demonstra o motivo `FALTA_COMPROVANTE_ENDERECO`.
4. `CF-20260922-1004` (**FALHA TÉCNICA**): permite demonstrar os retries e o reprocessamento administrativo.

Alguns registros seed têm metadados sem arquivo físico correspondente; sua leitura falha tecnicamente e não fabrica conteúdo PDF.

---

## 7. Verificação dos Critérios de Aceite

| Critério de Aceite (SDD / Aula 1 & 2) | Implementação no CaseFlow |
| :--- | :--- |
| **Isolamento entre usuários** | Solicitante visualiza apenas seus protocolos; Administrador possui visão global auditada. |
| **Aprovação automática** | Rascunho com `IDENTIFICACAO` e `COMPROVANTE_ENDERECO` prontos e vigentes resulta em `APROVADA`. |
| **Rejeição com códigos de motivo** | Ausência de documento obrigatório ou validade vencida gera `REJEITADA` com códigos claros (`FALTA_IDENTIFICACAO`, etc.). |
| **Idempotência** | Submit e retry exigem `Idempotency-Key`; replay equivalente não duplica jobs. |
| **Reprocessamento por Administrador** | Exclusivo para casos em `FALHA_TECNICA`, incrementa `processingRun` e exige justificativa de 10 a 500 caracteres. |
| **Recuperação após restart** | Jobs persistidos são retomados pelo scheduler do case-service com lease. |
| **Rastreabilidade e Histórico** | Linha do tempo registra ator (`USER`, `ADMIN`, `SYSTEM`) e data/hora; notificações são internas. |
