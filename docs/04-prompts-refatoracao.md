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
| **T0** | Setup da branch e inicialização do arquivo de prompts | `cd91ef2` | Concluído |
| **T1** | Baseline numérico com Detekt no backend | *(pendente)* | A iniciar |
| **T2** | Testes de caracterização do `CaseService` | *(pendente)* | A iniciar |
| **T3** | Snapshot de contrato HTTP com MockMvc | *(pendente)* | A iniciar |
| **T4** | Extração do `CaseMapper` (`@Component`) | *(pendente)* | A iniciar |
| **T9** | Extração de `AuthService` com BCrypt e desacoplamento do Controller | *(pendente)* | A iniciar |
| **T10** | Remoção de código morto comprovado e parâmetro ignorado | *(pendente)* | A iniciar |
| **T20** | Relatório de modernização final com métricas comparativas | *(pendente)* | A iniciar |
| **T21** | Fechamento do arquivo de prompts com histórico completo | *(pendente)* | A iniciar |

---

## Registro Detalhado dos Prompts Executados

### Tarefa T0 · Setup da Branch e Inicialização do Arquivo de Prompts
- **Commit:** `cd91ef2`
- **Prompt Utilizado:**
  > Inicialize a governança de refatoração: valide a branch `refactoring` a partir de `main` e crie o arquivo `docs/04-prompts-refatoracao.md` estruturando o tracking de cada prompt, problema resolvido e técnica aplicada conforme exigido no plano de auditoria.
- **Problema Resolvido & Técnica Aplicada:**
  - **Problema:** Ausência de rastreabilidade formal entre as decisões de engenharia, os prompts submetidos ao agente de IA e os commits atômicos exigidos na avaliação da disciplina.
  - **Técnica Aplicada:** Setup de documentação de governança e rastreabilidade (*Audit Trail*), isolando a trilha de refatoração na branch dedicada `refactoring`.

---
