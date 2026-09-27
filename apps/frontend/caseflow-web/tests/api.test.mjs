import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { afterEach, beforeEach, test } from 'node:test';
import { Buffer } from 'node:buffer';
import ts from 'typescript';

const source = await readFile(new URL('../src/services/api.ts', import.meta.url), 'utf8');
const compiled = ts.transpileModule(source, {
  compilerOptions: {
    module: ts.ModuleKind.ESNext,
    target: ts.ScriptTarget.ES2022
  }
}).outputText;
const { ApiService } = await import(`data:text/javascript;base64,${Buffer.from(compiled).toString('base64')}`);
const originalFetch = globalThis.fetch;

function mockFetch(body, status = 200) {
  const calls = [];
  globalThis.fetch = async (url, init) => {
    calls.push({ url: String(url), init });
    return new Response(JSON.stringify(body), {
      status,
      headers: { 'Content-Type': 'application/json' }
    });
  };
  return calls;
}

function requestHeaders(call) {
  return new Headers(call.init.headers);
}

beforeEach(() => ApiService.setAccessToken(null));
afterEach(() => {
  ApiService.setAccessToken(null);
  globalThis.fetch = originalFetch;
});

test('cliente web nao referencia endpoints dos servicos internos', () => {
  assert.doesNotMatch(source, /auth-service|case-service|localhost/);
});

test('login usa o endpoint publico do BFF e envia credenciais', async () => {
  const loginResponse = {
    token: 'jwt-test-token',
    tokenType: 'Bearer',
    expiresAt: '2026-09-26T17:00:00Z',
    user: { id: 'user-1', email: 'user@example.test', fullName: 'Test User', role: 'USER' }
  };
  const calls = mockFetch(loginResponse);

  const result = await ApiService.login('user', 'password');

  assert.equal(calls[0].url, '/api/v1/auth/login');
  assert.deepEqual(JSON.parse(calls[0].init.body), { username: 'user', password: 'password' });
  assert.equal(requestHeaders(calls[0]).get('Authorization'), null);
  assert.equal(result.token, loginResponse.token);
});

test('criacao de caso envia payload autenticado ao BFF', async () => {
  const payload = {
    title: 'Caso documental de teste',
    description: 'Descrição suficiente para validar a criação de caso.',
    type: 'ANALISE_DOCUMENTAL'
  };
  const calls = mockFetch({ id: 'case-1', ...payload });
  ApiService.setAccessToken('jwt-test-token');

  await ApiService.createCase(payload);

  assert.equal(calls[0].url, '/bff/v1/cases');
  assert.equal(calls[0].init.method, 'POST');
  assert.equal(requestHeaders(calls[0]).get('Authorization'), 'Bearer jwt-test-token');
  assert.deepEqual(JSON.parse(calls[0].init.body), payload);
});

test('upload envia arquivo e metadados por multipart autenticado', async () => {
  const calls = mockFetch({ id: 'document-1', contentType: 'application/pdf' });
  ApiService.setAccessToken('jwt-test-token');
  const pdf = new Blob(['%PDF-1.4 test'], { type: 'application/pdf' });

  await ApiService.uploadDocument('case-1', 'IDENTIFICACAO', '2026-12-01', pdf);

  assert.equal(calls[0].url, '/bff/v1/cases/case-1/documents');
  assert.equal(calls[0].init.method, 'POST');
  assert.equal(requestHeaders(calls[0]).get('Authorization'), 'Bearer jwt-test-token');
  assert.equal(calls[0].init.body.get('category'), 'IDENTIFICACAO');
  assert.equal(calls[0].init.body.get('validUntil'), '2026-12-01');
  assert.equal(calls[0].init.body.get('file').type, 'application/pdf');
});

test('submit envia uma chave de idempotencia e a versao do caso', async () => {
  const calls = mockFetch({ id: 'case-1', status: 'ENVIADA' });
  ApiService.setAccessToken('jwt-test-token');

  await ApiService.submitCase('case-1', 4);

  assert.equal(calls[0].url, '/bff/v1/cases/case-1/submit');
  assert.equal(requestHeaders(calls[0]).get('Authorization'), 'Bearer jwt-test-token');
  assert.equal(requestHeaders(calls[0]).get('Idempotency-Key'), 'submit-case-1-4');
  assert.deepEqual(JSON.parse(calls[0].init.body), { version: 4 });
});

test('consulta de caso retorna resultado pelo BFF', async () => {
  const response = {
    id: 'case-1',
    status: 'APROVADA',
    latestResult: { decision: 'APROVADA', reasonCodes: [] }
  };
  const calls = mockFetch(response);
  ApiService.setAccessToken('jwt-test-token');

  const result = await ApiService.getCaseById('case-1');

  assert.equal(calls[0].url, '/bff/v1/cases/case-1');
  assert.equal(requestHeaders(calls[0]).get('Authorization'), 'Bearer jwt-test-token');
  assert.deepEqual(result.latestResult, response.latestResult);
});

test('erro HTTP do BFF e propagado sem sucesso local de fallback', async () => {
  mockFetch({ message: 'Solicitação inválida' }, 422);
  ApiService.setAccessToken('jwt-test-token');

  await assert.rejects(ApiService.getCases(), /Solicitação inválida/);
});
