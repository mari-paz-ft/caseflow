import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { dirname, resolve } from 'node:path';
import { setTimeout as sleep } from 'node:timers/promises';
import { fileURLToPath } from 'node:url';

const origin = process.env.CASEFLOW_WEB_URL ?? 'http://127.0.0.1:5173';
const repositoryRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../../../..');
const runId = `${Date.now()}`;
const password = 'local-e2e-nonsecret';
const pdfBytes = '%PDF-1.4\nCaseFlow integration fixture\n%%EOF';

async function request(path, { token, method = 'GET', json, form, headers = {} } = {}) {
  const requestHeaders = new Headers(headers);
  if (token) requestHeaders.set('Authorization', `Bearer ${token}`);
  let body;
  if (json !== undefined) {
    requestHeaders.set('Content-Type', 'application/json');
    body = JSON.stringify(json);
  } else if (form) {
    body = form;
  }
  const response = await fetch(`${origin}${path}`, { method, headers: requestHeaders, body });
  const text = await response.text();
  let responseBody = null;
  if (text) {
    try { responseBody = JSON.parse(text); } catch { responseBody = text; }
  }
  return { status: response.status, body: responseBody };
}

function expectStatus(response, expected, label) {
  assert.equal(
    response.status,
    expected,
    `${label}: esperado HTTP ${expected}, recebido ${response.status} (${response.body?.errorCode ?? 'sem código'})`
  );
  return response.body;
}

async function login(username) {
  const body = expectStatus(await request('/api/v1/auth/login', {
    method: 'POST',
    json: { username, password }
  }), 200, 'login');
  assert.ok(body.token);
  return body.token;
}

async function createCase(token, title, description) {
  return expectStatus(await request('/bff/v1/cases', {
    token,
    method: 'POST',
    json: { title, description, type: 'ANALISE_DOCUMENTAL' }
  }), 201, 'criação de caso');
}

async function uploadPdf(token, caseId, category) {
  const form = new FormData();
  form.append('category', category);
  form.append('validUntil', '2099-12-31');
  form.append('file', new Blob([pdfBytes], { type: 'application/pdf' }), `${category.toLowerCase()}.pdf`);
  expectStatus(await request(`/bff/v1/cases/${caseId}/documents`, {
    token,
    method: 'POST',
    form
  }), 201, `upload ${category}`);
}

async function getCase(token, id) {
  return request(`/bff/v1/cases/${id}`, { token });
}

async function waitFor(label, operation, predicate, timeoutMs = 90000) {
  const deadline = Date.now() + timeoutMs;
  let lastStatus = 'sem resposta';
  while (Date.now() < deadline) {
    try {
      const result = await operation();
      lastStatus = `HTTP ${result.status}`;
      if (predicate(result)) return result;
    } catch {
      lastStatus = 'falha transitória de conexão';
    }
    await sleep(800);
  }
  throw new Error(`Timeout aguardando ${label}; última observação: ${lastStatus}`);
}

const userToken = await login(`phase13-user-${runId}`);
const approvedDraft = await createCase(
  userToken,
  'Fase 13 caso de aprovação',
  'Solicitação usada para validar o percurso integrado de aprovação documental.'
);
await uploadPdf(userToken, approvedDraft.id, 'IDENTIFICACAO');
await uploadPdf(userToken, approvedDraft.id, 'COMPROVANTE_ENDERECO');
const approvedReady = expectStatus(await getCase(userToken, approvedDraft.id), 200, 'consulta do rascunho');
const submitKey = `phase13-submit-${approvedDraft.id}-${runId}`;
const submitPayload = { version: approvedReady.version };
const submitted = expectStatus(await request(`/bff/v1/cases/${approvedDraft.id}/submit`, {
  token: userToken,
  method: 'POST',
  json: submitPayload,
  headers: { 'Idempotency-Key': submitKey }
}), 202, 'submit');
const submitReplay = expectStatus(await request(`/bff/v1/cases/${approvedDraft.id}/submit`, {
  token: userToken,
  method: 'POST',
  json: submitPayload,
  headers: { 'Idempotency-Key': submitKey }
}), 202, 'replay idempotente do submit');
assert.equal(submitted.processingRun, 1);
assert.equal(submitReplay.processingRun, 1);
const approved = await waitFor(
  'resultado APROVADA',
  () => getCase(userToken, approvedDraft.id),
  result => result.status === 200 && result.body?.status === 'APROVADA',
  30000
);
assert.equal(approved.body.latestResult?.decision, 'APROVADA');

const otherUserToken = await login(`phase13-other-${runId}`);
assert.equal((await getCase(otherUserToken, approvedDraft.id)).status, 403);
const adminToken = await login(`phase13-admin-${runId}`);
assert.equal((await getCase(adminToken, approvedDraft.id)).status, 200);

const rejectedDraft = await createCase(
  userToken,
  'Fase 13 caso rejeitado',
  'Solicitação sem comprovante para confirmar rejeição de negócio.'
);
await uploadPdf(userToken, rejectedDraft.id, 'IDENTIFICACAO');
const rejectedReady = expectStatus(await getCase(userToken, rejectedDraft.id), 200, 'consulta do caso de rejeição');
expectStatus(await request(`/bff/v1/cases/${rejectedDraft.id}/submit`, {
  token: userToken,
  method: 'POST',
  json: { version: rejectedReady.version },
  headers: { 'Idempotency-Key': `phase13-reject-${rejectedDraft.id}-${runId}` }
}), 202, 'submit caso de rejeição');
const rejected = await waitFor(
  'resultado REJEITADA',
  () => getCase(userToken, rejectedDraft.id),
  result => result.status === 200 && result.body?.status === 'REJEITADA',
  30000
);
assert.equal(rejected.body.latestResult?.decision, 'REJEITADA');

const technicalDraft = await createCase(
  userToken,
  `[SIMULAR_FALHA] Fase 13 ${runId}`,
  'Solicitação de teste para retry técnico e recuperação após reinício.'
);
await uploadPdf(userToken, technicalDraft.id, 'IDENTIFICACAO');
const technicalReady = expectStatus(await getCase(userToken, technicalDraft.id), 200, 'consulta do caso técnico');
expectStatus(await request(`/bff/v1/cases/${technicalDraft.id}/submit`, {
  token: userToken,
  method: 'POST',
  json: { version: technicalReady.version },
  headers: { 'Idempotency-Key': `phase13-technical-${technicalDraft.id}-${runId}` }
}), 202, 'submit caso técnico');

await waitFor(
  'primeira notificação técnica',
  async () => {
    const response = await request('/bff/v1/notifications', { token: userToken });
    return {
      status: response.status,
      body: Array.isArray(response.body) ? response.body.filter(item => item.caseId === technicalDraft.id) : []
    };
  },
  result => result.status === 200 && result.body.length >= 1,
  20000
);
execFileSync('docker', ['compose', 'restart', 'case-service'], {
  cwd: repositoryRoot,
  stdio: 'ignore'
});

const technicalFailed = await waitFor(
  'esgotamento de retries após restart do case-service',
  async () => {
    const [caseResponse, notificationResponse] = await Promise.all([
      getCase(userToken, technicalDraft.id),
      request('/bff/v1/notifications', { token: userToken })
    ]);
    return {
      status: caseResponse.status,
      body: caseResponse.body,
      notifications: Array.isArray(notificationResponse.body)
        ? notificationResponse.body.filter(item => item.caseId === technicalDraft.id)
        : []
    };
  },
  result => result.status === 200 &&
    result.body?.status === 'FALHA_TECNICA' &&
    result.notifications.length >= 3,
  120000
);
assert.equal(technicalFailed.body.latestResult?.decision, 'FALHA_TECNICA');

const retryKey = `phase13-admin-retry-${technicalDraft.id}-${runId}`;
const retryPayload = { justification: 'Infraestrutura restaurada para validação integrada.' };
const retried = expectStatus(await request(`/bff/v1/cases/${technicalDraft.id}/retry`, {
  token: adminToken,
  method: 'POST',
  json: retryPayload,
  headers: { 'Idempotency-Key': retryKey }
}), 202, 'retry administrativo');
const retryReplay = expectStatus(await request(`/bff/v1/cases/${technicalDraft.id}/retry`, {
  token: adminToken,
  method: 'POST',
  json: retryPayload,
  headers: { 'Idempotency-Key': retryKey }
}), 202, 'replay idempotente do retry');
assert.equal(retried.processingRun, 2);
assert.equal(retryReplay.processingRun, 2);

console.log(JSON.stringify({
  fluxoWebBffServices: 'PASS',
  aprovacao: approved.body.latestResult.decision,
  replaySubmitProcessingRun: submitReplay.processingRun,
  userBCaseA: '403',
  adminConsulta: '200',
  rejeicaoNegocio: rejected.body.latestResult.decision,
  restartRecovery: `${technicalFailed.body.status}; notificacoes=${technicalFailed.notifications.length}`,
  retryAdminReplayProcessingRun: retryReplay.processingRun,
  casosTesteCriados: [approvedDraft.id, rejectedDraft.id, technicalDraft.id]
}, null, 2));
