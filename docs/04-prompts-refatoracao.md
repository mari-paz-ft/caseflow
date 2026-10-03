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
| **T20** | Relatório de execução da trilha mínima | `d1d9c0d` | Concluído; métricas finais bloqueadas pela falta de JDK |
| **T21** | Fechamento do arquivo de prompts com histórico completo | `c0f3cad` | Concluído |

As tarefas T7–T8 e T11–T19 da trilha completa ainda estão pendentes. T13 é opcional no plano.

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
- **Commit:** `d1d9c0d`
- **Instrução executada:** Consolidar diagnóstico, hashes de commits, escopo preservado e estado das verificações no relatório de modernização.
- **Problema & técnica:** Rastreabilidade entre implementação e entregável. A medição pós-refatoração do Detekt e os testes ficaram registrados como pendentes porque o ambiente não possui Java Runtime.

### Tarefa T21 · Fechamento do registro de prompts
- **Commit:** `c0f3cad` (a atualização deste índice com o hash ocorre no commit de fechamento seguinte; um commit não pode conter o próprio hash).
- **Instrução executada:** Sincronizar os estados e hashes efetivos de T0–T20, documentando as etapas não executadas da trilha completa.
- **Problema & técnica:** Fechamento do audit trail sem atribuir hashes fictícios nem declarar métricas não medidas.

### Tarefa T5 · Eliminar duplicação de `ProcessingJob`
- **Commit:** `8f28cd0`
- **Instrução executada:** Extrair a criação do `ProcessingJob` duplicada entre `submitCase` e `retryCase` para `createProcessingJob(caseRequest)`, preservando a ordem e o objeto persistido.
- **Problema & técnica:** Centralização da construção e persistência do job em método privado reutilizado pelos dois fluxos.

### Tarefa T6 · Colisão de protocolo
- **Commit:** `4a2b6b0`
- **Instrução executada:** Verificar a unicidade de cada protocolo gerado, repetir no máximo cinco vezes mantendo `CF-AAAAMMDD-NNNN` e falhar com código explícito se todas colidirem.
- **Problema & técnica:** Prevenção de colisão com a restrição única do banco usando consulta derivada `existsByProtocol` e retry limitado. Foram adicionados testes para colisão recuperável e esgotamento; a execução depende de JDK.

---
