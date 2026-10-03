# Prompts de Modernização e Refatoração — CaseFlow

**Atividade:** Trabalho 2 da disciplina Engenharia de Software 2.0 (Prof. Luiz Real)  
**Projeto:** CaseFlow  
**Branch de Trabalho:** `refactoring`  
**Base do Repositório:** commit `9de3812223b11650f724c862c2d39a3519110320`  

Este documento registra os prompts efetivamente utilizados para conduzir a auditoria e as refatorações arquiteturais do projeto CaseFlow, correlacionando cada instrução ao respectivo hash de commit na branch `refactoring`.

---

## Índice de Tarefas e Rastreabilidade de Commits

| Tarefa | Descrição | Commit | Status |
| :---: | :--- | :---: | :---: |
| **T0** | Setup da branch e inicialização do arquivo de prompts | `47c498d` | Concluído |
| **T1** | Baseline numérico com Detekt no backend | `1d06338` | Concluído |
| **T2** | Testes de caracterização do `CaseService` | `650d140` | Concluído |
| **T3** | Snapshot de contrato HTTP com MockMvc | `a35cc0d` | Concluído |
| **T4** | Extração do `CaseMapper` (`@Component`) | `f10598e` | Concluído |
| **T9** | Extração de `AuthService` com BCrypt e desacoplamento do Controller | `9b03930`, `5285ecc` | Concluído |
| **T10** | Remoção de código morto comprovado e parâmetro ignorado | `c35a8ff` | Concluído |
| **T5** | Eliminar duplicação de criação do `ProcessingJob` | `8f28cd0` | Concluído |
| **T6** | Garantir unicidade do protocolo com tentativas limitadas | `4a2b6b0` | Implementado; testes adicionados, execução pendente por falta de Java |
| **T7** | Remover `!!` de `AnalysisEngineService` | `24f9316` | Concluído |
| **T8** | Delegar mapeamento de histórico e notificações aos serviços | `7548e80`, `da329fd` | Concluído; snapshots ajustados |
| **T11** | Aplicar construções idiomáticas Kotlin (`HexFormat`, `in`) | `e24acf5` | Concluído |
| **T12** | Remover build Maven duplicado e alinhar documentação | `826a1bd` | Concluído |
| **T14** | Separar tipos de segurança e serviços de suporte | `13233e3` | Concluído |
| **T15** | Configurar Vitest e teste de fumaça do `StatusBadge` | `1fa1190` | Concluído; teste e build passaram |
| **T16** | Modularizar cliente de API e fallback local | `fce3a4a` | Concluído; 4 testes e build passaram |
| **T17** | Extrair hook `useCasesList` e tipar notificações | `93a1dd9`, `4e75400` | Concluído; 7 testes e build passaram |
| **T18** | Extrair hook `useCaseDetail` | `e2b2c32` | Concluído; 9 testes e build passaram |
| **T19** | Extrair subcomponentes de `CaseDetail` | `198cc77` | Concluído; 9 testes e build passaram |
| **T20** | Relatório final da trilha completa | `d1d9c0d`, `fb0315c`, `066e1bf` | Relatório atualizado; métricas finais bloqueadas pela falta de JDK |
| **T21** | Fechamento do arquivo de prompts com histórico completo | `c0f3cad`, `f5d96b2` | Concluído |

T13 é a única tarefa planejada ainda não executada; é opcional e foi diferida para evitar atualização de dependências sem validação backend disponível.

---

## Registro Detalhado dos Prompts Executados

### Tarefa T0 · Setup da Branch e Inicialização do Arquivo de Prompts
- **Commit:** `47c498d`
- **Prompt Utilizado:**
  > Inicialize a governança de refatoração: valide a branch `refactoring` a partir de `main` e crie o arquivo `docs/04-prompts-refatoracao.md` estruturando o tracking de cada prompt, problema resolvido e técnica aplicada conforme exigido no plano de auditoria.
- **Problema Resolvido & Técnica Aplicada:**
  - **Problema:** Ausência de rastreabilidade formal entre as decisões de engenharia, os prompts submetidos ao agente de IA e os commits atômicos exigidos na avaliação da disciplina.
  - **Técnica Aplicada:** Setup de documentação de governança e rastreabilidade (*Audit Trail*), isolando a trilha de refatoração na branch dedicada `refactoring`.

---

### Tarefa T1 · Baseline com Detekt
- **Commit:** `1d06338`
- **Prompt Utilizado:**
  > Adicione o plugin detekt ao build Gradle do backend com a configuração padrão. Rode `./gradlew detekt` e salve o relatório em markdown. Ao final, descreva brevemente o problema resolvido e a técnica aplicada.
- **Problema Resolvido & Técnica Aplicada:**
  - **Problema:** Ausência de baseline estático e numérico de métricas de qualidade de código (complexidade ciclomática, *code smells*, *force unwraps* `!!` e violações de estilo) antes do início da refatoração.
  - **Técnica Aplicada:** Configuração do plugin Detekt 1.23.6 no `backend/build.gradle.kts` e consolidação do relatório inicial `docs/detekt-baseline.md` (44 violações catalogadas: 1 `ComplexMethod` McCabe 20, 3 `UnsafeCallOnNullableType` `!!`, 1 `TooManyFunctions`, 6 `MatchingDeclarationName`, 19 `WildcardImport`, 14 `MagicNumber`).

### Tarefa T2 · Caracterização de `CaseService`
- **Commit:** `650d140`
- **Instrução executada:** Cobrir `createCase`, envio feliz e idempotente, autorização de retry administrativo e conflito de versão sem alterar a implementação de produção.
- **Problema & técnica:** Testes unitários de caracterização com repositórios e serviços simulados, preservando o comportamento observável antes da refatoração.

### Tarefa T3 · Snapshot do contrato HTTP
- **Commit:** `a35cc0d`
- **Instrução executada:** Capturar os contratos de histórico, notificações e login por testes MockMvc, usando os formatos e credenciais seedados.
- **Problema & técnica:** Proteção da estrutura JSON e dos códigos HTTP em endpoints consumidos pelo frontend.

### Tarefa T4 · Extração de `CaseMapper`
- **Commit:** `f10598e`
- **Instrução executada:** Extrair o mapeamento, consulta do resultado e parsing JSON de `CaseService` para um componente `CaseMapper`, preservando a saída.
- **Problema & técnica:** Aplicação de SRP por delegação do mapeamento a componente dedicado.

### Tarefa T9 · `AuthService`, BCrypt e desacoplamento
- **Commits:** `9b03930`, `5285ecc`
- **Instrução executada:** Extrair `AuthService` com `getCurrentUser()` e `login()`, validar senha com `PasswordEncoder` BCrypt e armazenar hashes BCrypt no seed, preservando `X-User-Email`.
- **Problema & técnica:** Retirada do acesso a `AppUserRepository` do Controller e validação explícita da senha; testes de serviço cobrem senha correta e incorreta.

### Tarefa T10 · Código morto e parâmetro ignorado
- **Commit:** `c35a8ff`
- **Instrução executada:** Buscar usos no backend e frontend, remover consultas sem chamadores e deixar de passar `idempotencyKey` ignorado ao serviço, mantendo a rota e o cabeçalho HTTP.
- **Problema & técnica:** Redução de API interna não utilizada com verificação estática de referências.

### Tarefa T20 · Relatório final da trilha mínima
- **Commits:** `d1d9c0d`, `fb0315c`
- **Instrução executada:** Consolidar diagnóstico, hashes de commits, escopo preservado e estado das verificações da trilha completa no relatório de modernização.
- **Problema & técnica:** Rastreabilidade entre implementação e entregável. O frontend foi validado (9 testes e build); o Detekt final e a suíte backend seguem pendentes porque o ambiente não tem Java Runtime.

### Tarefa T21 · Fechamento do registro de prompts
- **Commits:** `c0f3cad`, `f5d96b2` (o hash desta atualização será registrado no commit de fechamento subsequente, pois um commit não pode incluir o próprio hash).
- **Instrução executada:** Sincronizar os estados e hashes efetivos de T0–T20, documentando T13 como opcional diferida e as métricas backend ainda não medidas.
- **Problema & técnica:** Fechamento do audit trail sem atribuir hashes fictícios nem declarar métricas não medidas. O hash do último commit será registrado em commit subsequente, pois um commit não pode conter o próprio hash.

### Tarefa T5 · Eliminar duplicação de `ProcessingJob`
- **Commit:** `8f28cd0`
- **Instrução executada:** Extrair a criação do `ProcessingJob` duplicada entre `submitCase` e `retryCase` para `createProcessingJob(caseRequest)`, preservando a ordem e o objeto persistido.
- **Problema & técnica:** Centralização da construção e persistência do job em método privado reutilizado pelos dois fluxos.

### Tarefa T6 · Colisão de protocolo
- **Commit:** `4a2b6b0`
- **Instrução executada:** Verificar a unicidade de cada protocolo gerado, repetir no máximo cinco vezes mantendo `CF-AAAAMMDD-NNNN` e falhar com código explícito se todas colidirem.
- **Problema & técnica:** Prevenção de colisão com a restrição única do banco usando consulta derivada `existsByProtocol` e retry limitado. Foram adicionados testes para colisão recuperável e esgotamento; a execução depende de JDK.

### Tarefa T7 · Remover `!!`
- **Commit:** `24f9316`
- **Instrução executada:** Substituir os três force unwraps de datas em `executeAnalysis` por safe calls e comparação explícita com `true`, preservando os mesmos `reasonCodes`.
- **Problema & técnica:** Remoção de caminhos que podiam lançar `NullPointerException` por desembrulho forçado de valores anuláveis.

### Tarefa T8 · Mapeamento DTO dos serviços de suporte
- **Commits:** `7548e80`, `da329fd`
- **Instrução executada:** Adicionar `toDto()` a `HistoryService` e `NotificationService` e fazer o Controller delegar o mapeamento, preservando os campos JSON.
- **Problema & técnica:** Retirada da montagem repetida de DTOs do Controller, com contrato coberto pelos snapshots T3 e stubs ajustados para delegação.

### Tarefa T11 · Kotlin idiomático
- **Commit:** `e24acf5`
- **Instrução executada:** Substituir o `fold` manual do SHA-256 por `HexFormat` e a cadeia de condições de estado por membership em conjunto.
- **Problema & técnica:** Uso de APIs padrão para conversão hexadecimal e checagem de pertencimento, mantendo o formato do hash e os estados aceitos.

### Tarefa T12 · Build duplicado e documentação de arquitetura
- **Commit:** `826a1bd`
- **Instrução executada:** Confirmar que Docker e stack usam Gradle, remover o `backend/pom.xml` e corrigir divergências de nomes, códigos de erro e idempotência nos documentos `.ai/`.
- **Problema & técnica:** Remoção de um segundo build não usado e sincronização da documentação com o código real.

### Tarefa T14 · Um tipo por arquivo
- **Commit:** `13233e3`
- **Instrução executada:** Separar `CurrentUserContext`, `HeaderAuthFilter`, `HistoryService` e `NotificationService` em arquivos dedicados sem alterar suas responsabilidades.
- **Problema & técnica:** Redução de declarações de nível superior agrupadas e melhor localização dos tipos por nome de arquivo.

### Tarefa T15 · Vitest e React Testing Library
- **Commit:** `1fa1190`
- **Instrução executada:** Configurar Vitest com jsdom e React Testing Library e adicionar um teste de fumaça ao `StatusBadge`, sem alterar o componente.
- **Problema & técnica:** Criação da primeira infraestrutura de testes de UI. `npm test` passou (1 teste) e `npm run build` passou.

### Tarefa T16 · Modularizar `api.ts`
- **Commit:** `fce3a4a`
- **Instrução executada:** Separar dados iniciais (`fixtures.ts`), armazenamento mock (`mockAdapter.ts`), cliente/fallback HTTP (`httpClient.ts`) e manter a assinatura pública de `ApiService`.
- **Problema & técnica:** Extração de responsabilidades e centralização do fallback num helper testável. `npm test` passou (4 testes) e `npm run build` passou.

### Tarefa T17 · Hook de listagem no `App.tsx`
- **Commits:** `93a1dd9`, `4e75400`
- **Instrução executada:** Extrair o hook `useCasesList(currentUser)`, as funções puras `filterCases` e `computeStats`, tipar `NotificationItem[]` e substituir o cast `any` por parsing do filtro de status.
- **Problema & técnica:** Separação de carregamento, filtros e estatísticas da tela; testes unitários cobrem filtro, métricas e parsing. `npm test` passou (7 testes) e `npm run build` passou. O adaptador mock agora também tolera ambientes sem `localStorage`.

### Tarefa T18 · Hook `useCaseDetail`
- **Commit:** `e2b2c32`
- **Instrução executada:** Extrair estado, fetch, polling e mutations de documentos, submissão e retry para `useCaseDetail`, mantendo o intervalo de polling de 2 segundos.
- **Problema & técnica:** Separação da lógica de estado e efeitos do JSX. Testes cobrem carregamento e polling; `npm test` passou (9 testes) e `npm run build` passou.

### Tarefa T19 · Subcomponentes de `CaseDetail`
- **Commit:** `198cc77`
- **Instrução executada:** Extrair `DocumentUploadForm`, `RetryModal` e `CaseHistoryTimeline`, passando os dados e handlers por props e preservando markup e classes.
- **Problema & técnica:** Redução da responsabilidade visual concentrada em `CaseDetail`; `npm test` passou (9 testes) e `npm run build` passou.

---
