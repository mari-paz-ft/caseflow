# CaseFlow: Auditoria e Refatoração de Arquitetura

Plano para o grupo · atualizado em 03/10/2026

## Como ler este documento

Este documento traz duas versões do mesmo plano:

- **Versão mínima**: o que o grupo precisa entregar para cumprir a atividade.
- **Versão completa**: tudo o que a auditoria encontrou.

Comecem pela versão mínima. A completa é o que vale fazer se sobrar tempo.

---

## Parte 0: Contexto (comum às duas versões)

- **Atividade:** Trabalho 2 da disciplina Engenharia de Software 2.0 (prof. Luiz Real), "Atividade Prática Final: Auditoria e Refatoração de Arquitetura".
- **Projeto:** CaseFlow, o mesmo repositório do Trabalho 1, feito pelo mesmo grupo.
- **Base da refatoração:** a v1 entregue, que é o estado de `main` no GitHub (commit `9de3812`, com os commits de documentação depois da v1). A branch `refactoring` sai desse ponto.
- **Atenção, branch `feat/fase-14`:** existe no GitHub uma branch com uma reescrita do projeto em quatro aplicações (commit `ee400e8`, 160 arquivos alterados, com remoção de `api.ts` e `Navbar.tsx`). Ela não faz parte da v1 e não é a base deste plano. Se o grupo decidir seguir essa arquitetura, as tarefas de frontend (T16 a T19) precisam ser refeitas.
- **Prazos:** Trabalho 1 em **05/10/2026**. Refatoração em **15/10/2026**. Não deixar o Trabalho 1 para depois por causa desta atividade.
- **Entregáveis (confirmados em aula):**
  1. Código refatorado na branch `refactoring` do repositório.
  2. Arquivo com os prompts de modernização usados.
  3. Relatório de modernização, em PDF ou Markdown. A estrutura é livre e pode ser gerada com IA.
- **O que o professor vai olhar:** a branch e o relatório, para ver se um bate com o outro. Ele disse que o código não é a preocupação principal dele.
- **Vídeo:** o vídeo do Trabalho 1 não precisa falar de refatoração.

---

## Parte 1: Diagnóstico (resumo dos achados)

### Code smells e responsabilidades misturadas

| Onde | Problema |
|---|---|
| `CaseService` (backend) | Mistura regras de negócio, geração de protocolo e mapeamento de DTO. |
| `CaseDetail.tsx` (600 linhas) | Busca de dados, polling e três formulários no mesmo componente. |
| `App.tsx` (308 linhas) | Carregamento de dados, filtros, estatísticas e JSX na mesma função. |
| `services/api.ts` (754 linhas) | Dados de mock, cliente HTTP e 22 cópias do mesmo bloco de fallback. |

### Violação de padrões

| Onde | Problema |
|---|---|
| `CaseController` | Acessa `AppUserRepository` diretamente. É o exemplo de "lógica de banco dentro do Controller" citado no enunciado. |
| Login (`/auth/login`) | Não verifica a senha. Qualquer senha entra. Além disso, o seed grava a senha em texto puro no campo `passwordHash`. |
| `AnalysisEngineService` | Usa `!!` três vezes, contra o padrão declarado em `.ai/standards.md`. |
| `App.tsx` e `api.ts` | Usam `any` e casts, contra o `strict` do TypeScript. |
| Mapeamento de DTO | Parte dos endpoints monta DTO inline, parte usa `toDto()`. |

### Obsolescência, segurança e drift

| Onde | Problema |
|---|---|
| `build.gradle.kts` | Spring Boot 3.3.4. Confirmar na página oficial se a linha ainda tem suporte. |
| `backend/pom.xml` | Segundo build (Maven) que ninguém usa. O Dockerfile usa só Gradle. |
| Geração de protocolo | Sorteia 4 dígitos sem checar unicidade, apesar da coluna ser `UNIQUE`. |
| `DocumentService` | Calcula SHA-256 com `fold` e concatenação de String, em vez de `HexFormat`. |
| Código morto | Mais de 6 métodos de repositório sem chamador. Parâmetro `idempotencyKey` aceito e ignorado. |
| `.ai/standards.md` | Cita `CaseRequestService` (a classe real é `CaseService`), campo `code` (o real é `errorCode`) e hooks que não existem. |

### Deixados de fora de propósito (justificativa para o relatório)

- **Troca de usuário por cabeçalho `X-User-Email`:** o frontend depende dele em todas as chamadas. Corrigir de verdade exige implementar autenticação no frontend, o que é funcionalidade nova.
- **`alert` e `confirm` no frontend:** trocar por componentes de erro mudaria o comportamento.
- **`Thread.sleep` no motor assíncrono:** é uma simulação de processamento da atividade.
- **Console do H2 habilitado** e `frameOptions` desligado: necessário para a demonstração local, mas precisa de perfil separado para produção.
- **Fallback que devolve um PDF falso** quando o arquivo não existe no disco.
- **Seed com SHA-256 de tamanho inválido** e **entidades com listas mutáveis públicas**.

---

## Parte 2: Versão mínima

Objetivo: entregar os três itens com as intervenções de maior impacto e menor risco.

| Tarefa | O que faz | Por que entra na mínima |
|---|---|---|
| T0 | Cria a branch `refactoring` e o arquivo de prompts. | Base dos entregáveis. |
| T1 | Roda o detekt e salva o relatório antes das mudanças. | Evidência numérica do diagnóstico. |
| T2 | Testes de caracterização do `CaseService`. | Rede de segurança antes de refatorar. |
| T3 | Snapshot do JSON de history, notificações e login. | Prova que a API não mudou. |
| T4 | Extrai o `CaseMapper` do `CaseService`. | Corrige SRP na classe central de regras. |
| T9 | Extrai o `AuthService`, valida senha com BCrypt e tira o banco do Controller. | Corrige a falha mais grave e a violação de padrão citada no enunciado. |
| T10 | Remove código morto verificado por busca. | Simples e de baixo risco. |
| T20 | Escreve o relatório de modernização. | Entregável obrigatório. |
| T21 | Fecha o arquivo de prompts com os hashes dos commits. | Entregável obrigatório. |

**Critério de corte:** ficaram de fora as tarefas que mexem em build, frontend grande ou sem teste. Elas continuam na versão completa.

---

## Parte 3: Versão completa

Inclui a versão mínima e acrescenta as tarefas abaixo.

### Backend

| Tarefa | O que faz |
|---|---|
| T5 | Remove a duplicação de criação de `ProcessingJob` entre `submitCase` e `retryCase`. |
| T6 | Garante protocolo único, com tentativas limitadas, sem mudar o formato `CF-AAAAMMDD-NNNN`. |
| T7 | Remove os `!!` de `AnalysisEngineService`. |
| T8 | Padroniza o mapeamento de DTO de histórico e notificações com `toDto()`. |
| T11 | Troca o SHA-256 manual por `HexFormat` e a cadeia de `||` por `in` com conjunto. |
| T12 | Remove o `pom.xml` e corrige a documentação `.ai/` para refletir o código real. |
| T13 | Atualiza o Spring Boot para uma linha com suporte e o springdoc compatível. |
| T14 | Separa tipos misturados em arquivos próprios (`SecurityConfig.kt`, `SupportServices.kt`). |

### Frontend

| Tarefa | O que faz |
|---|---|
| T15 | Configura Vitest e React Testing Library (o frontend não tinha testes). |
| T16 | Separa `api.ts` em cliente HTTP, mock e fixtures, com um único helper de fallback. |
| T17 | Extrai `useCasesList` do `App.tsx`, tipa as notificações e remove os `any`. |
| T18 | Extrai `useCaseDetail` do `CaseDetail.tsx`. |
| T19 | Extrai `DocumentUploadForm`, `RetryModal` e `CaseHistoryTimeline`. |

### Documentação

Já estão incluídas nas tarefas T20 e T21 da versão mínima.

---

## Parte 4: Decisões que precisam do grupo

Antes de começar, o grupo precisa decidir:

0. **Base do trabalho:** seguir na v1 (plano válido como está) ou migrar para a branch `feat/fase-14` (exige refazer as tarefas de frontend e parte do backend).

E concordar com estas mudanças, porque afetam quem roda o projeto:

1. **Remover o `pom.xml`:** quem usa Maven localmente deixa de ter o build.
2. **Atualizar o Spring Boot (T13):** é a tarefa de maior risco de quebrar o build. Se não houver tempo para testar, pode ficar de fora sem prejuízo para os entregáveis.
3. **Corrigir a documentação `.ai/` (T12):** muda o que o grupo declarou como padrão. Vale alinhar antes.
4. **Login passa a exigir senha correta (T9):** a interface não é afetada, porque o frontend só usa o cabeçalho `X-User-Email`. Quem testar a API direto com login sem senha vai ver a diferença.
5. **Divisão de trabalho:** a definir pelo grupo. Sugestão: uma pessoa cuida do backend e outra do frontend, com T1 e T20 divididos.

---

## Parte 5: Como entregar

- Todo o trabalho fica local, na branch `refactoring`. **Não fazer `git push` sem combinar com o grupo.**
- Cada intervenção tem um commit próprio. O relatório cita o hash de cada um, porque o professor compara o relatório com a branch.
- Os prompts usados ficam em `docs/Prompts-Modernizacao-CaseFlow.md`, com uma entrada por tarefa, na ordem em que foram executadas.
- Antes de enviar, rodar `./gradlew test` (backend) e `npm test && npm run build` (frontend).

---

## Parte 6: Sugestões de prompt

Estes prompts são sugestões para usar no Claude Code ou em outro agente, uma tarefa por vez, na ordem da tabela. Os de refatoração terminam com a instrução exigida pelo enunciado.

**Atenção:** o professor compara o arquivo de prompts com a branch. Por isso, registre o prompt que vocês realmente usaram, não só o sugerido.

### Diagnóstico (exigido pelo enunciado)

> Aja como um Arquiteto de Software. Analise meu projeto e gere um relatório de dívida técnica, focando em violações de SOLID e acoplamento excessivo.

### Versão mínima

**T1 · Baseline com detekt**

> Adicione o plugin detekt ao build Gradle do backend com a configuração padrão. Rode `./gradlew detekt` e salve o relatório em markdown. Ao final, descreva brevemente o problema resolvido e a técnica aplicada.

**T2 · Testes de caracterização do CaseService**

> Escreva testes de caracterização para CaseService cobrindo createCase, submitCase (caminho feliz e reenvio idempotente), retryCase (exige ADMIN) e updateCase (conflito de versão). Os testes devem passar contra o código atual, sem alterá-lo. Ao final, descreva brevemente o problema resolvido e a técnica aplicada.

**T3 · Snapshot de contrato HTTP**

> Crie testes MockMvc que capturam o JSON atual de GET /bff/v1/cases/{id}/history, GET /bff/v1/notifications e POST /bff/v1/auth/login com a senha seedada correta, usando os dados do DataSeederService. Os testes devem passar contra o código atual. Ao final, descreva brevemente o problema resolvido e a técnica aplicada.

**T4 · Extrair CaseMapper**

> Aja como um Arquiteto de Software Kotlin/Spring. O método CaseService.toDto() mistura busca de dados, parsing de JSON e montagem de DTO. Extraia essa responsabilidade para uma classe CaseMapper (@Component), injetada em CaseService, mantendo o mesmo comportamento e a mesma saída. Ao final, descreva brevemente o problema resolvido e a técnica aplicada.

**T9 · AuthService e validação de senha**

> CaseController acessa AppUserRepository diretamente, o que é lógica de dados dentro do Controller, e o login não verifica senha. Extraia um AuthService com getCurrentUser() e login(), adicione PasswordEncoder BCrypt, faça o login validar a senha e faça o seeder gravar hashes. Não mexa no X-User-Email do HeaderAuthFilter. Ao final, descreva brevemente o problema resolvido e a técnica aplicada.

**T10 · Remover código morto**

> Remova os métodos de repositório e serviço que não têm nenhum chamador (verifique com busca em backend/src e frontend/src antes de apagar). Remova o parâmetro idempotencyKey que é aceito e ignorado, sem mudar a rota. Não remova /csrf nem /logout. Ao final, descreva brevemente o problema resolvido e a técnica aplicada.

**T20 · Relatório de modernização**

> Rode o detekt de novo e compare com o relatório inicial. Escreva o relatório de modernização com diagnóstico, plano de execução com o hash de cada commit, itens deixados de fora e recomendações futuras. Ao final, descreva brevemente o problema resolvido e a técnica aplicada.

### Versão completa (além da mínima)

**T5 · Duplicação de ProcessingJob (DRY)**

> O código de criação do ProcessingJob está duplicado entre submitCase e retryCase em CaseService. Extraia um método privado createProcessingJob(caseRequest) que devolve o job persistido, mantendo a ordem de execução. Ao final, descreva brevemente o problema resolvido e a técnica aplicada.

**T6 · Colisão de protocolo**

> generateProtocol() gera 4 dígitos aleatórios sem checar duplicidade contra uma coluna UNIQUE. Escreva primeiro um teste que force colisão e depois implemente tentativas limitadas, sem mudar o formato CF-AAAAMMDD-NNNN. Ao final, descreva brevemente o problema resolvido e a técnica aplicada.

**T7 · Remover `!!`**

> Os trechos com !! em AnalysisEngineService.executeAnalysis violam o padrão de null-safety do .ai/standards.md. Reescreva as checagens de vencimento com safe-call e when, sem !!, mantendo os mesmos reasonCodes. Ao final, descreva brevemente o problema resolvido e a técnica aplicada.

**T8 · DTO mapping padronizado**

> getHistory, getNotifications e markNotificationRead montam DTOs manualmente. Adicione toDto() em HistoryService e NotificationService e faça o Controller delegar, sem mudar o JSON. Ao final, descreva brevemente o problema resolvido e a técnica aplicada.

**T11 · Kotlin idiomático**

> Em DocumentService, troque o sha256 com fold por java.util.HexFormat, sem mudar o resultado. Em CaseService.submitCase, troque a cadeia de || por in com um conjunto. Ao final, descreva brevemente o problema resolvido e a técnica aplicada.

**T12 · Build duplicado e drift da documentação**

> Confirme que o Dockerfile e o .ai/tech-stack.md usam só Gradle e remova backend/pom.xml. Compare .ai/standards.md com o código real e corrija a documentação para refletir o código, não o contrário. Liste cada divergência. Ao final, descreva brevemente o problema resolvido e a técnica aplicada.

**T13 · Atualizar Spring Boot**

> Atualize o Spring Boot para a última linha 3.5.x com suporte OSS e o springdoc compatível. Não altere código de aplicação; se um teste quebrar, corrija só o que o upgrade exige. Ao final, descreva brevemente o problema resolvido e a técnica aplicada.

**T14 · Um tipo por arquivo**

> Separe SecurityConfig.kt em três arquivos e SupportServices.kt em dois, sem alterar código dentro das classes. Ao final, descreva brevemente o problema resolvido e a técnica aplicada.

**T15 · Vitest e React Testing Library**

> Configure Vitest com jsdom e React Testing Library no frontend, com um teste de fumaça do StatusBadge. Não altere componentes. Ao final, descreva brevemente o problema resolvido e a técnica aplicada.

**T16 · Separar api.ts**

> api.ts mistura dados de mock, cliente HTTP e adaptador de mock, com 22 métodos que repetem o mesmo fallback. Escreva testes do comportamento de fallback e depois separe em fixtures, mockAdapter e http, com um helper withFallback. Mantenha a assinatura pública do ApiService. Ao final, descreva brevemente o problema resolvido e a técnica aplicada.

**T17 · Hook de listagem no App.tsx**

> App.tsx mistura dados, filtros, estatísticas e JSX. Extraia um hook useCasesList(currentUser) e as funções puras filterCases e computeStats, testadas isoladamente. Remova os casts any e tipe notifications como NotificationItem[]. Não mude o JSX visível. Ao final, descreva brevemente o problema resolvido e a técnica aplicada.

**T18 · Hook useCaseDetail**

> CaseDetail.tsx mistura fetch, polling e três fluxos de formulário. Escreva testes do hook useCaseDetail e depois extraia o estado e os handlers para ele, sem mudar a lógica de polling. Ao final, descreva brevemente o problema resolvido e a técnica aplicada.

**T19 · Subcomponentes de CaseDetail**

> Extraia de CaseDetail.tsx os componentes DocumentUploadForm, RetryModal e CaseHistoryTimeline, recebendo dados por props, sem mudar markup nem classes Tailwind. Ao final, descreva brevemente o problema resolvido e a técnica aplicada.
