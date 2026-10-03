export async function withFallback<T>(
  backendEnabled: boolean,
  backendRequest: () => Promise<T>,
  fallbackRequest: () => T | Promise<T>,
): Promise<T> {
  if (!backendEnabled) return fallbackRequest();

  try {
    return await backendRequest();
  } catch (error) {
    console.warn('Backend indisponível, usando fallback em memória', error);
    return fallbackRequest();
  }
}

export async function requestJson<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, init);
  if (!response.ok) {
    throw new Error(`Requisição falhou com status ${response.status}`);
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}
