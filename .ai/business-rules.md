# Regras de Negócio e Domínio — CaseFlow

## 1. Visão do Domínio

O CaseFlow gerencia o ciclo de vida de solicitações de conferência documental (`ANALISE_DOCUMENTAL`), garantindo rastreabilidade, imutabilidade após o envio e avaliação automatizada de documentos anexados. A análise é determinística e não comprova autenticidade, identidade ou conteúdo material dos documentos.

---

## 2. Tipos de Usuários e Matriz de Permissões

Existem dois papéis no sistema:

1. **USER (Solicitante):**
   - Cria novos rascunhos de solicitação em nome próprio.
   - Adiciona, substitui e remove documentos enquanto o status for `RASCUNHO`.
   - Envia a solicitação para análise.
   - Consulta apenas suas próprias solicitações, documentos, notificações e histórico.
   - **Proibido:** Consultar ou alterar solicitações de outros usuários; reprocessar solicitações; alterar resultados.
2. **ADMIN (Administrador):**
   - Pode criar solicitações próprias.
   - Consulta todas as solicitações do sistema, seus documentos e histórico (para fins de suporte/auditoria).
   - Pode solicitar reprocessamento de solicitações em estado `FALHA_TECNICA`, obrigatoriamente fornecendo justificativa (10 a 500 caracteres).
   - Consulta e marca como lidas somente suas próprias notificações.
   - **Proibido:** editar rascunhos de terceiros; alterar manualmente o veredito de aprovação/rejeição.

A identidade de autorização é obtida do JWT validado. O `sub` é um UUID determinístico derivado do username. Auth-service aceita username e password não vazios sem persistir contas; username contendo `admin` (case-insensitive) recebe `ADMIN` e qualquer outro recebe `USER`. Apesar das credenciais demonstrativas, o JWT é real. A migração remapeia subjects legados para os UUIDs determinísticos sem remover registros persistidos.

---

## 3. Estados da Solicitação (`CaseStatus`)

O ciclo de vida da solicitação segue a máquina de estados:

1. `RASCUNHO`: Estado inicial. O solicitante pode editar título, descrição e anexar/remover documentos.
2. `ENVIADA`: Submetida pelo solicitante. Dados e anexos tornam-se estritamente imutáveis; um job persistido é criado.
3. `PROCESSANDO`: O scheduler de conferência assumiu o job de execução.
4. `APROVADA`: Estado final positivo. Todos os critérios documentais foram atendidos.
5. `REJEITADA`: Estado final negativo por regras de negócio. Gera motivos específicos (`reasonCodes`) e não permite edição ou reenvio direto.
6. `FALHA_TECNICA`: Estado decorrente de falha técnica após esgotamento das tentativas; permite retry pelo ADMIN.

Fluxo normal: `RASCUNHO` → `ENVIADA` → `PROCESSANDO` → `APROVADA` ou `REJEITADA`. Falhas técnicas transitórias podem repetir o processamento; retry administrativo aceito cria nova execução e retorna o status a `ENVIADA`.

---

## 4. Regras de Validação de Solicitação

- **Tipo inicial:** `ANALISE_DOCUMENTAL`.
- **Título:** Obrigatório, tamanho entre 5 e 120 caracteres.
- **Descrição:** Obrigatória, tamanho entre 20 e 2.000 caracteres.
- **Protocolo:** gerado automaticamente no formato `CF-YYYYMMDD-XXXX` e único no sistema.
- **Controle de Concorrência:** campo `version` numérico incremental. Atualizações e envios exigem a versão esperada; divergência resulta em conflito (`409`), sem sobrescrita silenciosa.
- Após o envio, dados e documentos são imutáveis.

---

## 5. Regras de Documentos Anexos

- **Categorias Permitidas:**
  1. `IDENTIFICACAO` (Obrigatória para aprovação - ex: RG, CNH, Passaporte)
  2. `COMPROVANTE_ENDERECO` (Obrigatória para aprovação - ex: Conta de Luz, Água, Telefone)
  3. `COMPLEMENTAR` (Opcional)
- **Limite:** máximo de 3 documentos ativos por solicitação, no máximo 1 por categoria.
- **Formato:** somente PDF com MIME `application/pdf`, tamanho máximo de 5 MiB e cabeçalho básico `%PDF-`. Extensão ou MIME informado isoladamente não comprovam a validade do arquivo; a verificação não substitui um parser PDF completo.
- **Validade Declarada (`validUntil`):** opcional. Se informada, não pode ser anterior à data UTC de envio para qualquer categoria, inclusive `COMPLEMENTAR`.
- **Condição para Envio:** é obrigatório existir pelo menos 1 documento em estado `READY`. A interface pode alertar sobre categorias obrigatórias ausentes, mas o envio é permitido para registrar formalmente a pendência.
- **Upload inválido:** MIME, tamanho ou assinatura inválidos são recusados no upload; não se cria documento `READY`.
- **Falha de Armazenamento:** arquivo persistido ausente, inacessível, ilegível ou com tamanho/hash divergente durante leitura/análise caracteriza falha técnica, não rejeição por regra de negócio.
- Documentos são privados; toda inclusão, remoção, leitura e download verifica autorização sobre o caso e o documento.

---

## 6. Motor de Regras de Análise Documental (`DOCUMENTAL_V1`)

Ao processar uma solicitação no estado `ENVIADA`, o motor usa a versão de regras registrada no caso e avalia os documentos `READY`:
1. **Critério 1 — Presença de Identificação:**
   - Deve existir documento na categoria `IDENTIFICACAO` com upload finalizado (`READY`).
   - *Se ausente:* código de motivo `FALTA_IDENTIFICACAO`.
2. **Critério 2 — Presença de Comprovante de Endereço:**
   - Deve existir documento na categoria `COMPROVANTE_ENDERECO` com upload finalizado (`READY`).
   - *Se ausente:* código de motivo `FALTA_COMPROVANTE_ENDERECO`.
3. **Critério 3 — Validade dos Documentos:**
   - Se qualquer documento possuir `validUntil` anterior à data UTC de envio, registrar o motivo específico da categoria: `DOCUMENTO_VENCIDO_IDENTIFICACAO`, `DOCUMENTO_VENCIDO_COMPROVANTE_ENDERECO` ou `DOCUMENTO_VENCIDO_COMPLEMENTAR`.
4. **Critério 4 — Integridade Técnica:**
   - Arquivo ausente, ilegível, corrompido ou indisponível durante o processamento não gera motivo de rejeição; caracteriza falha técnica.
5. **Decisão:**
   - Se não houver motivo impeditivo: decisão `APROVADA`.
   - Se houver motivo de negócio: decisão `REJEITADA` com a lista de `reasonCodes`.
   - Se ocorrer falha técnica: aplicar a política de retry e, após esgotamento das tentativas, `FALHA_TECNICA`.
   - Solicitação rejeitada não admite edição ou reenvio direto; o usuário deve criar uma nova solicitação.

---

## 7. Reprocessamento Administrativo

- Aplicável exclusivamente a solicitações com status `FALHA_TECNICA`.
- Exige ator com papel `ADMIN`.
- Exige justificativa com comprimento entre 10 e 500 caracteres.
- Preserva documentos originais, data de referência inicial e versão das regras.
- Incrementa `processingRun` e agenda nova execução.
- A operação também exige `Idempotency-Key`.

---

## 8. Submissão e Idempotência

- Submit e retry exigem `Idempotency-Key`.
- A chave é persistida no case-service com operação, ator/contexto, hash do pedido, recurso e resposta original.
- Mesma chave, operação e contexto: retornar a resposta original sem executar novamente nem criar outro job.
- Mesma chave e operação com contexto diferente: responder `409 Conflict`.
- O aceite do envio é persistido junto ao job antes de iniciar o processamento.
- O BFF propaga a chave recebida sem substituí-la; a UI conserva uma chave por intenção da operação.
- Repetições idempotentes não devem duplicar jobs, transições ou notificações associadas à mesma operação.

---

## 9. Processamento Assíncrono

- Toda solicitação submetida para análise deve possuir um `ProcessingJob` persistido.
- Estados mínimos do job: `SCHEDULED`, `RUNNING`, `COMPLETED` e `FAILED`; persistir tentativas, disponibilidade, lease/token, último erro e timestamps.
- O scheduler interno do case-service reivindica jobs disponíveis com lock e lease. Jobs pendentes ou com lease expirado devem ser recuperados após restart.
- Retry é permitido somente para falhas técnicas. Rejeições por regra de negócio não geram retry automático.
- Política padrão: primeira falha técnica, nova tentativa após 10 segundos; segunda, após 30 segundos; terceira, job `FAILED` e solicitação `FALHA_TECNICA`.

---

## 10. Notificações

- Notificações são internas e persistidas; não há envio de e-mail/SMS ou integração externa no MVP.
- O destinatário do resultado é o proprietário da solicitação. USER e ADMIN consultam/marcam como lidas somente as próprias notificações.
- Cada falha técnica registrada gera notificação ao titular, inclusive falhas que serão repetidas; a decisão final também gera notificação de resultado.
- Repetições idempotentes não duplicam efeitos associados à operação.
