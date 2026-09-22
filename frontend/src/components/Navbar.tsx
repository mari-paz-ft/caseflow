import React, { useState, useEffect } from 'react';
import { User } from '../types';
import { USERS, ApiService } from '../services/api';
import { Bell, Shield, User as UserIcon, Server, Database, Sparkles } from 'lucide-react';

interface Props {
  currentUser: User;
  onSwitchUser: (user: User) => void;
  onOpenNotifications: () => void;
  unreadCount: number;
}

export const Navbar: React.FC<Props> = ({
  currentUser,
  onSwitchUser,
  onOpenNotifications,
  unreadCount
}) => {
  const [isLiveBackend, setIsLiveBackend] = useState<boolean>(false);

  useEffect(() => {
    ApiService.checkBackend().then(online => setIsLiveBackend(online));
    const interval = setInterval(() => {
      ApiService.checkBackend().then(online => setIsLiveBackend(online));
    }, 10000);
    return () => clearInterval(interval);
  }, []);

  return (
    <header className="bg-white border-b border-slate-200 sticky top-0 z-30 shadow-sm">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        {/* Brand & Logo */}
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-brand-700 to-blue-500 flex items-center justify-center text-white shadow-md shadow-blue-500/20">
            <Sparkles className="w-5 h-5" />
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <span className="font-bold text-xl tracking-tight text-slate-900">CaseFlow</span>
              <span className="bg-blue-100 text-brand-700 text-xs font-semibold px-2 py-0.5 rounded">
                MVP
              </span>
            </div>
            <p className="text-xs text-slate-500 hidden sm:block">Solicitações e Conferência Documental</p>
          </div>
        </div>

        {/* Backend connectivity indicator & User switcher */}
        <div className="flex items-center space-x-4">
          {/* Status do Backend */}
          <div
            title={isLiveBackend ? 'Conectado ao Backend Spring Boot (Kotlin)' : 'Executando em Modo Mock Local'}
            className={`hidden md:flex items-center space-x-1.5 px-3 py-1 rounded-full text-xs font-medium border ${
              isLiveBackend
                ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                : 'bg-indigo-50 text-indigo-700 border-indigo-200'
            }`}
          >
            {isLiveBackend ? (
              <>
                <Server className="w-3.5 h-3.5 text-emerald-600 animate-pulse" />
                <span>Backend Kotlin Ativo</span>
              </>
            ) : (
              <>
                <Database className="w-3.5 h-3.5 text-indigo-600" />
                <span>Modo Demonstração (Mock)</span>
              </>
            )}
          </div>

          {/* Notificações */}
          <button
            onClick={onOpenNotifications}
            className="relative p-2 rounded-lg text-slate-600 hover:bg-slate-100 transition-colors"
            title="Ver notificações"
          >
            <Bell className="w-5 h-5" />
            {unreadCount > 0 && (
              <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-rose-500 rounded-full ring-2 ring-white animate-pulse" />
            )}
          </button>

          {/* Alternador de Perfil / Usuário */}
          <div className="flex items-center bg-slate-100 p-1 rounded-xl border border-slate-200">
            {USERS.map(user => {
              const isActive = user.id === currentUser.id;
              const isAdmin = user.role === 'ROLE_ADMIN';
              return (
                <button
                  key={user.id}
                  onClick={() => onSwitchUser(user)}
                  className={`flex items-center space-x-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${
                    isActive
                      ? 'bg-white text-slate-900 shadow-sm'
                      : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  {isAdmin ? (
                    <Shield className={`w-3.5 h-3.5 ${isActive ? 'text-purple-600' : 'text-slate-400'}`} />
                  ) : (
                    <UserIcon className={`w-3.5 h-3.5 ${isActive ? 'text-blue-600' : 'text-slate-400'}`} />
                  )}
                  <span className="font-medium">{user.fullName.split(' ')[0]}</span>
                  <span className="text-[10px] uppercase font-bold text-slate-400">
                    {isAdmin ? 'Admin' : 'User'}
                  </span>
                </button>
              );
            })}
          </div>
        </div>
      </div>
    </header>
  );
};
