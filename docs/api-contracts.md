# Contratos HTTP do CaseFlow MVP

## Fronteiras

O navegador chama exclusivamente o `caseflow-bff`. O BFF valida o JWT e integra com `auth-service` e `case-service` por REST síncrono; os DTOs públicos do BFF são próprios e mapeados para os contratos internos. O BFF não tem banco de domínio.

| Processo | Porta local | Acesso no Compose | Responsabilidade |
| --- | ---: | --- | --- |
| `caseflow-web` | 5173 | Pública | Interface; encaminha `/api/v1` e `/bff/v1` ao BFF |
| `caseflow-bff` | 8081 | Pública e rede interna | API de entrada, autenticação JWT e proxy de contratos |
| `auth-service` | 8082 local; 8080 no Compose | Rede interna | Login demonstrativo e emissão/validação JWT |
| `case-service` | 8080 | Rede interna | Casos, documentos, autorização, análise e persistência |
| `auth-db` | 5432 | Rede interna | PostgreSQL preparado para auth-service |
| `case-db` | 5432 | Rede interna | PostgreSQL de domínio do case-service |

As portas de auth-service e case-service não são publicadas no host pelo Compose. `/health` existe para healthchecks; os endpoints de negócio continuam sujeitos às regras de segurança.

## Autenticação e headers

### Login público via BFF

`POST /api/v1/auth/login`

```json
{"username":"usuario","password":"senha"}
```

Username e password não vazios são aceitos para demonstração. Username contendo `admin`, sem distinção entre maiúsculas/minúsculas, recebe `ADMIN`; os demais recebem `USER`. A resposta inclui `token`, `tokenType`, `expiresAt` e `user`. O JWT é real, assinado com HMAC-SHA256, issuer `caseflow-auth-service` e validade padrão de 3.600 segundos.

O token deve ser enviado nas operações protegidas:

```http
Authorization: Bearer <jwt>
```

O frontend mantém o token em memória. Não há sessão/cookie OIDC, identidade por `X-User-Email` ou token textual. O `case-service` valida o JWT independentemente do BFF.

### Headers comuns

- `Authorization`: obrigatório nas rotas protegidas.
- `Idempotency-Key`: obrigatória em submit e retry; o BFF encaminha o valor sem substituí-lo.
- `Correlation-Id`: opcional; encaminhado ao serviço downstream quando presente.

## API pública do BFF

Prefixo de negócio: `/bff/v1`.

| Método | Rota | Acesso | Sucesso |
| --- | --- | --- | --- |
| POST | `/bff/v1/cases` | USER/ADMIN autenticado | 201 e solicitação criada |
| GET | `/bff/v1/cases` | USER/ADMIN autenticado | 200 e lista conforme autorização |
| GET | `/bff/v1/cases/{id}` | Proprietário ou ADMIN | 200 e detalhe, documentos e resultado mais recente |
| PUT | `/bff/v1/cases/{id}` | Proprietário, somente RASCUNHO | 200 e rascunho atualizado |
| POST | `/bff/v1/cases/{id}/documents` | Proprietário, somente RASCUNHO | 201; multipart com `category`, `file` e `validUntil` opcional |
| DELETE | `/bff/v1/cases/{id}/documents/{documentId}` | Proprietário, somente RASCUNHO | 204 |
| GET | `/bff/v1/cases/{id}/documents/{documentId}/content` | Proprietário ou ADMIN conforme policy | 200 e bytes do PDF |
| POST | `/bff/v1/cases/{id}/submit` | Proprietário, somente RASCUNHO | 202 e caso enviado |
| GET | `/bff/v1/cases/{id}/history` | Proprietário ou ADMIN | 200 e histórico |
| POST | `/bff/v1/cases/{id}/retry` | ADMIN, somente FALHA_TECNICA | 202 e nova execução agendada |
| GET | `/bff/v1/notifications` | Usuário autenticado | 200 e notificações próprias |
| PATCH | `/bff/v1/notifications/{id}` | Destinatário | 200 e notificação atualizada |

### Criação e atualização

`POST /bff/v1/cases` aceita:

```json
{
  "title": "Solicitação documental",
  "description": "Descrição com pelo menos vinte caracteres.",
  "type": "ANALISE_DOCUMENTAL"
}
```

O título deve ter 5–120 caracteres; a descrição, 20–2.000. O tipo aceito é `ANALISE_DOCUMENTAL`. Atualização usa `PUT /bff/v1/cases/{id}` com `title`, `description` e `version`.

### Upload e download

Upload usa `multipart/form-data`:

- `category`: `IDENTIFICACAO`, `COMPROVANTE_ENDERECO` ou `COMPLEMENTAR`;
- `file`: PDF com MIME `application/pdf`, no máximo 5 MiB e cabeçalho `%PDF-`;
- `validUntil`: data ISO `YYYY-MM-DD`, opcional.

O serviço confere os bytes reais, persiste o arquivo sob `./data/documents` e guarda tamanho, tipo e SHA-256. A leitura valida integridade. Arquivo ausente, ilegível ou divergente é falha técnica; não há PDF substituto/fabricado.

### Submit, retry e resultado

Submit recebe `POST /bff/v1/cases/{id}/submit`, `Idempotency-Key` e corpo:

```json
{"version":1}
```

Retry recebe `POST /bff/v1/cases/{id}/retry`, `Idempotency-Key` e corpo:

```json
{"justification":"Justificativa entre 10 e 500 caracteres."}
```

Submit exige ao menos um documento em `READY`; a falta de outra categoria obrigatória pode ser enviada e resulta em `REJEITADA`. A mesma chave e contexto reproduzem a resposta anterior; a mesma chave com contexto diferente retorna conflito. O resultado é consultado em `GET /bff/v1/cases/{id}` no campo `latestResult`. Ausência de documento obrigatório/validade expirada resulta em `REJEITADA`, sem retry. Falhas técnicas repetem após 10 e 30 segundos; na terceira falha o caso termina em `FALHA_TECNICA`. Cada falha técnica notifica o titular.

## Contratos internos

O BFF chama `POST /api/v1/auth/login` em auth-service. Auth-service também expõe `GET /api/v1/auth/me` para contexto autenticado interno. O BFF chama as rotas equivalentes do case-service sob `/api/v1`; o navegador nunca usa esses endpoints internos diretamente.

O BFF normaliza erros em `{status, errorCode, message, traceId}`, preservando status downstream quando há resposta e usando `503` quando um serviço não pode ser alcançado. O case-service também inclui `timestamp`, `error` e, em validação, `validationErrors`. Respostas de erro não incluem stack trace, segredo, JWT ou conteúdo privado. Status comuns: 400 para entrada inválida/header obrigatório ausente; 401 para JWT ausente/inválido; 403 para falta de permissão; 404 para recurso inexistente; 409 para conflito de estado/idempotência/persistência; 500 para falha técnica; 503 para serviço downstream indisponível.
