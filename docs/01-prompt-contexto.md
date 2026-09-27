# Prompt 1: Geração de Contexto (.ai/)

Este prompt é destinado a um assistente de IA para gerar ou revisar a estrutura canônica de Context Engineering do CaseFlow com base nas decisões aprovadas e no estado observado do repositório.

---

```text
Atue como Engenheiro de Software Sênior e Especialista em Context Engineering.

Leia `docs/adr/001-mvp-architecture.md`, `docs/mvp-scope.md` e
`docs/CaseFlow-Arquitetura-Pratica.md`. Verifique também o código/configuração antes de
escrever afirmações sobre funcionalidades já implementadas. Gere ou atualize somente os
quatro arquivos canônicos em `.ai/`:

.ai/
├── architecture.md
├── business-rules.md
├── tech-stack.md
└── standards.md

Use estes requisitos aprovados como baseline:
- Quatro aplicações independentes: caseflow-web, caseflow-bff, auth-service e case-service.
- O navegador chama somente o BFF; BFF comunica-se com auth-service e case-service por REST.
- auth-service aceita credenciais não vazias como mock e emite JWT HMAC real. Username contendo
  `admin` recebe ADMIN; os demais recebem USER. Não presuma persistência de usuários.
- BFF e case-service validam JWT; case-service aplica autorização por sujeito e recurso.
- PostgreSQL em runtime para case-service, conexão preparada no auth-service e H2 nos testes.
- Gradle Kotlin DSL é o único build backend aprovado. Stack: Kotlin, Java 21, Spring Boot,
  React e TypeScript; fixe as versões observadas nos manifests/wrappers.
- Case-service possui storage PDF local por porta, jobs duráveis e scheduler com leases,
  idempotência, retries técnicos e notificações.

Em `standards.md`, descreva organização, dependências entre camadas, DTOs, segurança, erros e
verificações sem permitir que o frontend contorne o BFF.
Em `architecture.md`, inclua topologia física, responsabilidades, comunicação, persistência e
limites das aplicações.
Em `tech-stack.md`, liste somente tecnologias aprovadas e observadas; Maven e tecnologias fora
do MVP não são alternativas implícitas.
Em `business-rules.md`, descreva USER/ADMIN, estados, documentos, análise, idempotência,
recuperação, retries e notificações com critérios verificáveis.

Mantenha requisitos, decisões aprovadas, mocks, itens fora de escopo e comportamento observado
claramente separados. Não reintroduza OAuth2/OIDC, sessão no BFF, BFF DB, usuário padrão,
X-User-Email, fallback mock no frontend, PDF fabricado, broker ou worker externo. Não altere
código, não invente funcionalidades e preserve as decisões já aceitas; sinalize conflitos novos
em vez de resolvê-los por suposição.
```
