# Checklist manual de aceitação visual — CaseFlow

Use este roteiro no navegador local para verificar os estados de carregamento, erro, vazio e sucesso sem instalar dependências de teste. O roteiro usa somente login stateless e consultas; não crie, edite, envie ou remova casos/documentos, nem marque notificações como lidas.

## Preparação

1. Inicie a stack local aprovada e abra `http://localhost:5173`.
2. Abra as ferramentas do navegador na aba **Network**. Para os estados de carregamento, use *Slow 3G* e recarregue antes da ação. Para estados de erro, bloqueie temporariamente a requisição indicada; remova o bloqueio ao terminar cada verificação.
3. Para verificar lista/detalhe com dados, use a conta demonstrativa `solicitante@caseflow.local` com qualquer senha não vazia. Para a lista vazia, use um username único que nunca tenha sido usado; a autenticação demonstrativa não persiste usuários.

## Verificações

- [ ] **Login — carregamento:** com rede lenta, enviar o formulário; o botão fica desabilitado e indica `Entrando...` enquanto o login aguarda resposta.
- [ ] **Login — erro:** bloquear `/api/v1/auth/login`, enviar o formulário e confirmar que uma mensagem com `role="alert"` é exibida; remover o bloqueio.
- [ ] **Lista — carregamento:** autenticar com a conta demonstrativa em rede lenta; confirmar `Carregando solicitações...` antes de a resposta chegar.
- [ ] **Lista — erro:** autenticado, bloquear `/bff/v1/cases`, recarregar a lista e confirmar que o erro fica visível em `role="alert"`; remover o bloqueio.
- [ ] **Lista — vazia:** sair e autenticar com username único; confirmar `Nenhuma solicitação encontrada` e a descrição de estado vazio, sem criar registros.
- [ ] **Lista — sucesso:** autenticar com `solicitante@caseflow.local`; confirmar cards dos casos existentes e seus estados.
- [ ] **Detalhe — carregamento:** abrir um caso existente em rede lenta; confirmar `Carregando solicitação...`.
- [ ] **Detalhe — sucesso:** abrir um caso existente sem bloqueios e confirmar título, status, documentos/histórico disponíveis.
- [ ] **Detalhe — erro:** voltar à lista, bloquear a consulta de detalhe `/bff/v1/cases/{id}`, abrir um caso e confirmar mensagem de erro e ação para voltar; remover o bloqueio.

## Evidência

Registre data, navegador/versão, ambiente/URL e o resultado de cada caixa. Marque `PASS` somente após observar o estado no navegador; em caso de falha, registre o passo e corrija antes de atualizar a matriz de aderência. Ao final, desative throttling e todos os bloqueios de rede e encerre a sessão.
