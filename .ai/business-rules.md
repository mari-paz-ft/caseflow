# Regras de Negócio e Domínio — CaseFlow

## 1. Visão do Domínio
O CaseFlow gerencia o ciclo de vida de solicitações de conferência documental (`ANALISE_DOCUMENTAL`), garantindo rastreabilidade, imutabilidade após o envio e avaliação automatizada de documentos anexados.

---

## 2. Tipos de Usuários e Matriz de Permissões
Existem dois papéis no sistema:
1. **USER (Solicitante):**
   - Cria novos rascunhos de solicitação em nome próprio.
   - Adiciona, substitui e remove documentos enquanto o status for `RASCUNHO`.
   - Envia a solicitação para análise.
   - Consulta apenas suas próprias solicitações, notificações e histórico.
   - **Proibido:** Consultar ou alterar solicitações de outros usuários; reprocessar solicitações; alterar resultados.
2. **ADMIN (Administrador):**
   - Pode criar solicitações próprias.
   - Consulta todas as solicitações do sistema, seus documentos e histórico (para fins de suporte/auditoria).
   - Pode solicitar reprocessamento de solicitações em estado `FALHA_TECNICA`, obrigatoriamente fornecendo justificativa (10 a 500 caracteres).
   - **Proibido:** Editar rascunhos de terceiros; alterar manualmente o veredito de aprovação/rejeição.

---

## 3. Estados da Solicitação (`CaseStatus`)
O ciclo de vida da solicitação segue a máquina de estados:

1. `RASCUNHO`: Estado inicial. O solicitante pode editar título, descrição e anexar/remover documentos.
2. `ENVIADA`: Submetida pelo solicitante. Dados e anexos tornam-se estritamente imutáveis. Job de análise criado.
3. `PROCESSANDO`: O motor de conferência assumiu o job de execução.
4. `APROVADA`: Estado final positivo. Todos os critérios documentais foram atendidos.
5. `REJEITADA`: Estado final negativo por regras de negócio. Gera motivos específicos (`reasonCodes`).
6. `FALHA_TECNICA`: Estado decorrente de erro inesperado de infraestrutura ou armazenamento. Permite retry pelo ADMIN.

---

## 4. Regras de Validação de Solicitação
- **Tipo inicial:** `ANALISE_DOCUMENTAL`.
- **Título:** Obrigatório, tamanho entre 5 e 120 caracteres.
- **Descrição:** Obrigatória, tamanho entre 20 e 2.000 caracteres.
- **Protocolo:** Gerado automaticamente no formato `CF-YYYYMMDD-XXXX` (ex: `CF-20260922-0042`), único no sistema.
- **Controle de Concorrência:** Campo `version` numérico incremental. Ações de envio exigem envio da versão esperada (`optimistic locking`).

---

## 5. Regras de Documentos Anexos
- **Categorias Permitidas:**
  1. `IDENTIFICACAO` (Obrigatória para aprovação - ex: RG, CNH, Passaporte)
  2. `COMPROVANTE_ENDERECO` (Obrigatória para aprovação - ex: Conta de Luz, Água, Telefone)
  3. `COMPLEMENTAR` (Opcional)
- **Limite:** Máximo de 3 documentos por solicitação (no máximo 1 por categoria ativa).
- **Formato e Tamanho:** Formato PDF (ou imagem no MVP); tamanho máximo de 5 MiB por arquivo.
- **Validade Declarada (`validUntil`):** Data informada no momento do anexo.
- **Condição para Envio:** Pelo menos 1 documento anexado. A interface alerta caso falte categoria obrigatória, mas permite enviar para registro formal da pendência.

---

## 6. Motor de Regras de Análise Documental (`DOCUMENTAL_V1`)
Ao processar uma solicitação no estado `ENVIADA`:
1. **Critério 1 — Presença de Identificação:**
   - Deve existir um documento na categoria `IDENTIFICACAO` com upload finalizado.
   - *Se ausente:* Código de motivo `FALTA_IDENTIFICACAO`.
2. **Critério 2 — Presença de Comprovante de Endereço:**
   - Deve existir um documento na categoria `COMPROVANTE_ENDERECO` com upload finalizado.
   - *Se ausente:* Código de motivo `FALTA_COMPROVANTE_ENDERECO`.
3. **Critério 3 — Validade do Documento:**
   - Se um documento possuir data de validade declarada (`validUntil`), esta data não pode ser anterior à data/hora UTC do envio da solicitação.
   - *Se expirado:* Código de motivo `DOCUMENTO_VENCIDO` (especificando a categoria).
4. **Decisão:**
   - Se não houver nenhum motivo impeditivo: Decisão `APROVADA`.
   - Se houver qualquer motivo impeditivo: Decisão `REJEITADA` com a lista de códigos gerados.
   - Solicitação rejeitada não admite edição ou reenvio direto; o usuário deve criar uma nova solicitação.

---

## 7. Reprocessamento Administrativo
- Aplicável **exclusivamente** para solicitações com status `FALHA_TECNICA`.
- Exige ator com papel `ADMIN`.
- Exige justificativa com comprimento entre 10 e 500 caracteres.
- Preserva documentos originais, data de referência inicial e regras aplicadas.
- Incrementa o `processing_run` (contador de execuções) e move o status de volta para `ENVIADA`.
