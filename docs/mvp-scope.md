# Escopo do MVP CaseFlow

## Propósito e leitura deste documento

Este documento delimita o MVP aprovado e registra separadamente o baseline observado antes das fases de implementação. O estado implementado e as evidências das regras estão em [`README.md`](../README.md) e [`compliance-matrix.md`](compliance-matrix.md). As categorias abaixo distinguem histórico, mocks, exclusões e evoluções possíveis; itens mockados não removem contratos ou limites entre aplicações.

A ordem de implementação é a do plano faseado aprovado. Uma fase só é considerada concluída após seus testes e critérios de aceite; a conclusão de uma fase não implica que os itens das fases seguintes já existam.

## BASELINE HISTÓRICO — Estado observado no início

No baseline observado antes das fases, os itens abaixo existiam em um monólito e foram posteriormente substituídos ou evoluídos:

- A interface React + TypeScript apresentava dashboard de casos, detalhe, upload, submissão, histórico, notificações e resultados.
- O backend inicial era um monólito Kotlin/Spring Boot com endpoints de casos/documentos, modelos de domínio, repositórios JPA e persistência relacional.
- Havia regras determinísticas básicas para presença/validade de documentos e estados de aprovação/rejeição.
- Jobs, histórico e notificações existiam no backend inicial; o processamento usava `@Async`, sem recuperação durável via scheduler.
- O upload usava `./uploads` e metadados de documentos; as validações PDF e a porta de storage aprovadas ainda não existiam nesse baseline.
- Os testes do backend inicial cobriam parte do motor de análise.

**Limite importante:** esta seção descreve apenas o baseline histórico; não representa o estado atual dos quatro limites de aplicação, da segurança ou dos critérios finais de aceite. Consulte o README e a matriz de aderência para evidências atuais.

## MOCKED — Comportamentos mockados

### Mock aprovado para o MVP alvo

- **Credenciais/autenticação de usuário:** auth-service aceita username e password não vazios sem persistir ou verificar contas reais. Username contendo `admin` resulta em `ADMIN`; qualquer outro username resulta em `USER`.
- O mock de credenciais fica dentro do auth-service. O serviço emite JWT real e os consumidores validam assinatura, issuer e expiração. Não se substitui JWT por header de identidade ou token meramente textual.
- Se um mock interno adicional for necessário, ele deverá estar atrás do contrato do serviço apropriado/BFF; o frontend não chama serviço de domínio ou autenticação diretamente nem mascara erro HTTP com fallback local.

### Atalhos mockados do baseline já removidos ou substituídos

- Usuários e casos de demonstração eram semeados pelo backend monolítico.
- A UI alternava usuários localmente e o `ApiService` mantinha um provedor em memória/fallback.
- A segurança do baseline aceitava `X-User-Email`, token textual `mock-token-*` e identidade padrão sem autenticação.
- O backend simulava falha técnica com marcador textual e retornava bytes PDF fabricados quando o arquivo não existia.

Esses atalhos não são requisitos do MVP final e foram removidos ou substituídos nas fases de segurança, frontend, processamento e storage. Consulte `docs/compliance-matrix.md` para o estado e as evidências atuais; não apresente o baseline como arquitetura implementada.

## OUT OF SCOPE — Fora do MVP

- Provedor externo de identidade, OAuth2/OIDC, Keycloak ou autenticação de produção com gestão/verificação persistida de credenciais.
- Cadastro público de usuários, recuperação de senha ou fluxos externos de conta.
- Kafka, RabbitMQ, Redis ou qualquer broker; o scheduler persistido vive no case-service.
- Armazenamento MinIO/S3, Kubernetes, Service Mesh, Vault e OpenTelemetry.
- BFF com banco de dados próprio ou acesso direto ao banco do case-service.
- Acesso direto do frontend a auth-service/case-service.
- Processamento de imagem; anexos aceitos no escopo são PDF.
- OCR, análise por IA, atestação de autenticidade documental ou revisão humana do conteúdo. O resultado automatizado verifica regras documentais declaradas e integridade/disponibilidade conforme o escopo, não comprova autenticidade ou identidade.
- Tornar o sistema production-ready. O MVP não é declaração de prontidão para produção.

## FUTURE — Evolução possível, não comprometida

Qualquer evolução nesta categoria exige decisão e planejamento próprios, sem antecipar implementação nas fases atuais:

- Autenticação real, gestão de usuários/credenciais e provedor de identidade apropriado.
- Evolução da validação documental além da validação básica de MIME, tamanho e assinatura PDF do MVP.
- Armazenamento remoto/gerenciado e serviços externos de notificação.
- Infraestrutura ou observabilidade além do Docker Compose local.

## Critério de arquitetura do MVP

O alvo contém quatro executáveis independentes:

```text
caseflow-web -> caseflow-bff -> auth-service
                            -> case-service
```

A UI consome apenas o BFF; o BFF chama os serviços por contratos REST; auth-service emite JWT real apesar de credenciais mockadas; case-service é dono do domínio, banco, documentos, idempotência e scheduler persistido. O estado do baseline acima não altera esses critérios do alvo.
