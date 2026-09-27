import React, { useState } from 'react';
import { AlertCircle, LogIn, Shield, Sparkles, User as UserIcon } from 'lucide-react';

interface Props {
  onLogin: (username: string, password: string) => Promise<void>;
}

export const LoginForm: React.FC<Props> = ({ onLogin }) => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);
    setIsSubmitting(true);
    try {
      await onLogin(username.trim(), password);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Não foi possível entrar.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <main className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
      <form onSubmit={handleSubmit} className="w-full max-w-md bg-white border border-slate-200 rounded-2xl shadow-sm p-7 space-y-5">
        <div className="flex items-center space-x-3">
          <div className="w-11 h-11 rounded-xl bg-gradient-to-tr from-brand-700 to-blue-500 flex items-center justify-center text-white">
            <Sparkles className="w-5 h-5" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-slate-900">Entrar no CaseFlow</h1>
            <p className="text-xs text-slate-500">Acesso de demonstração via autenticação mockada</p>
          </div>
        </div>

        {error && (
          <div role="alert" className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-rose-700 text-xs flex items-center space-x-2">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <label className="block space-y-1.5">
          <span className="text-xs font-semibold text-slate-700">Usuário</span>
          <span className="relative block">
            <UserIcon className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              autoComplete="username"
              required
              value={username}
              onChange={event => setUsername(event.target.value)}
              className="w-full pl-9 pr-3 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500"
            />
          </span>
        </label>

        <label className="block space-y-1.5">
          <span className="text-xs font-semibold text-slate-700">Senha</span>
          <span className="relative block">
            <Shield className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              type="password"
              autoComplete="current-password"
              required
              value={password}
              onChange={event => setPassword(event.target.value)}
              className="w-full pl-9 pr-3 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500"
            />
          </span>
        </label>

        <p className="text-[11px] text-slate-500">Qualquer username e senha não vazios funcionam. Username contendo “admin” recebe perfil ADMIN.</p>
        <button
          type="submit"
          disabled={isSubmitting || !username.trim() || !password}
          className="w-full inline-flex items-center justify-center space-x-2 px-4 py-2.5 bg-brand-600 hover:bg-brand-700 disabled:opacity-50 text-white text-sm font-semibold rounded-xl"
        >
          <LogIn className="w-4 h-4" />
          <span>{isSubmitting ? 'Entrando...' : 'Entrar'}</span>
        </button>
      </form>
    </main>
  );
};
