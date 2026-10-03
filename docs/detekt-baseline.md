# Relatório de Linha de Base (Baseline) — Detekt

**Projeto:** CaseFlow (Backend Kotlin)  
**Ferramenta:** Detekt 1.23.6  
**Data da Execução:** 03/10/2026  
**Commit de Base:** `47c498d` (v1 antes das intervenções de refatoração)  

---

## 1. Sumário Executivo de Violações

| Regra Detekt | Categoria | Severidade | Quantidade | Relação com o Plano |
| :--- | :--- | :---: | :---: | :--- |
| **ComplexMethod** | `complexity` | Alta | 1 | `AnalysisEngineService.executeAnalysis` (McCabe = 20) |
| **UnsafeCallOnNullableType (`!!`)** | `potential-bugs` | Alta | 3 | `AnalysisEngineService.kt` (Linhas 76, 86, 94) — Alvo de **T7** |
| **TooManyFunctions** | `complexity` | Média | 1 | `CaseController.kt` (12 funções públicas) — Alvo de **T9** |
| **MatchingDeclarationName** | `naming` | Média | 6 | Múltiplas classes por arquivo (`Models.kt`, `SupportServices.kt`) — Alvo de **T14** |
| **WildcardImport** | `style` | Baixa | 19 | Imports com `.*` violando as diretrizes de `.ai/standards.md` |
| **MagicNumber** | `style` | Baixa | 14 | Literais de bytes e timeouts dispersos no código |
| **Total de Ocorrências** | | | **44** | |

---

## 2. Detalhamento das Violações Prioritárias

### 2.1. Complexidade e Potenciais Bugs (Severidade Alta)

#### `AnalysisEngineService.kt`
- **Linha 43 · `ComplexMethod`:**
  - Método: `fun executeAnalysis(job: ProcessingJob): ProcessingResult`
  - Complexidade Ciclomática (McCabe): **20** (Limite padrão Detekt: 15 / Alvo de refatoração: ≤ 4).
  - *Diagnóstico:* Método procedural que concentra validação de 3 categorias documentais, tratamento de falhas simuladas, cálculo de veredito, auditoria e notificação.
- **Linha 76 · `UnsafeCallOnNullableType`:**
  - Código: `docIdentificacao.validUntil!!.isBefore(referenceDate)`
  - *Diagnóstico:* Chamada forçada com `!!` introduzindo risco de `NullPointerException` em runtime.
- **Linha 86 · `UnsafeCallOnNullableType`:**
  - Código: `docEndereco.validUntil!!.isBefore(referenceDate)`
  - *Diagnóstico:* Chamada forçada com `!!` sem proteção idiomática de nulos.
- **Linha 94 · `UnsafeCallOnNullableType`:**
  - Código: `docComplementar.validUntil!!.isBefore(referenceDate)`
  - *Diagnóstico:* Chamada forçada com `!!` em documento complementar opcional.

---

### 2.2. Arquitetura e Estrutura de Classes (Severidade Média)

#### `CaseController.kt`
- **Linha 27 · `TooManyFunctions`:**
  - A classe `CaseController` possui 12 funções públicas no mesmo controller (limite padrão: 11).
  - *Causa raiz:* O Controller assume lógica de autenticação (`/auth/login`, `/me`), CSRF (`/csrf`), histórico e CRUD de casos. Será mitigado com a extração de `AuthService` (**T9**).

#### `MatchingDeclarationName` (Múltiplas Declarações por Arquivo)
Detekt sinaliza arquivos que contêm múltiplas classes ou cujo nome não reflete um tipo de nível superior canônico:
1. `backend/src/main/kotlin/com/caseflow/domain/model/Models.kt` (contém `CaseRequest`, `CaseDocument`, `ProcessingJob`, `ProcessingResult`, etc.)
2. `backend/src/main/kotlin/com/caseflow/domain/enums/Enums.kt`
3. `backend/src/main/kotlin/com/caseflow/repository/Repositories.kt`
4. `backend/src/main/kotlin/com/caseflow/service/SupportServices.kt` (`HistoryService` e `NotificationService` no mesmo arquivo)
5. `backend/src/main/kotlin/com/caseflow/domain/exception/Exceptions.kt`
6. `backend/src/main/kotlin/com/caseflow/controller/dto/Dtos.kt`

---

### 2.3. Estilo e Convenções (Severidade Baixa)

#### `WildcardImport` (19 ocorrências)
Imports com `.*` foram detectados nos seguintes arquivos:
- `backend/src/main/kotlin/com/caseflow/domain/model/Models.kt` (`com.caseflow.domain.enums.*`)
- `backend/src/main/kotlin/com/caseflow/controller/CaseController.kt` (`com.caseflow.controller.dto.*`, `com.caseflow.service.*`)
- `backend/src/main/kotlin/com/caseflow/service/CaseService.kt` (`com.caseflow.controller.dto.*`, `com.caseflow.domain.enums.*`, `com.caseflow.domain.model.*`)
- `backend/src/main/kotlin/com/caseflow/service/AnalysisEngineService.kt` (`com.caseflow.domain.enums.*`)
- `backend/src/main/kotlin/com/caseflow/service/DataSeederService.kt` (`com.caseflow.domain.enums.*`, `com.caseflow.domain.model.*`, `com.caseflow.repository.*`)
- `backend/src/test/kotlin/com/caseflow/service/AnalysisEngineServiceTest.kt` (`com.caseflow.domain.enums.*`, `com.caseflow.domain.model.*`)

#### `MagicNumber` (14 ocorrências)
Literais numéricos não declarados como constantes:
- `1200` ms em `AnalysisEngineService.kt:34` (`Thread.sleep(1200)`)
- `1000` e `9999` em `CaseService.kt:195` (faixa randômica de protocolo)
- `1048576`, `2097152`, `850000`, `1200000`, `1500000` em `DataSeederService.kt` (tamanhos de arquivo em bytes)

---

## 3. Próximos Passos no Ciclo de Refatoração

Este baseline serve como registro numérico formal para comparação com a execução final em **T20**:
- Eliminação dos 3 `UnsafeCallOnNullableType` (**T7**).
- Redução da complexidade ciclomática de `executeAnalysis` de 20 para ≤ 4 (**T4 / T7**).
- Desacoplamento e redução de responsabilidades em `CaseController` (**T9**).
- Separação de tipos em arquivos individuais (**T14**).
