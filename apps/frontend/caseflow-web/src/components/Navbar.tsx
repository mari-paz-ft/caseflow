import React from 'react';
import { User } from '../types';
import { Bell, LogOut, Shield, Sparkles, User as UserIcon } from 'lucide-react';

interface Props {
  currentUser: User;
  onLogout: () => void;
  onOpenNotifications: () => void;
  unreadCount: number;
}

export const Navbar: React.FC<Props> = ({ currentUser, onLogout, onOpenNotifications, unreadCount }) => {
  const isAdmin = currentUser.role === 'ROLE_ADMIN';

  return (
    <header className="bg-white border-b border-slate-200 sticky top-0 z-30 shadow-sm">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-brand-700 to-blue-500 flex items-center justify-center text-white shadow-md shadow-blue-500/20">
            <Sparkles className="w-5 h-5" />
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <span className="font-bold text-xl tracking-tight text-slate-900">CaseFlow</span>
              <span className="bg-blue-100 text-brand-700 text-xs font-semibold px-2 py-0.5 rounded">MVP</span>
            </div>
            <p className="text-xs text-slate-500 hidden sm:block">Solicitações e Conferência Documental</p>
          </div>
        </div>

        <div className="flex items-center space-x-3">
          <div className="hidden sm:flex items-center space-x-1.5 px-3 py-1.5 bg-slate-100 rounded-xl text-xs text-slate-700">
            {isAdmin ? <Shield className="w-3.5 h-3.5 text-purple-600" /> : <UserIcon className="w-3.5 h-3.5 text-blue-600" />}
            <span className="font-medium">{currentUser.fullName}</span>
            <span className="uppercase font-bold text-slate-400">{isAdmin ? 'Admin' : 'User'}</span>
          </div>

          <button
            onClick={onOpenNotifications}
            className="relative p-2 rounded-lg text-slate-600 hover:bg-slate-100 transition-colors"
            title="Ver notificações"
          >
            <Bell className="w-5 h-5" />
            {unreadCount > 0 && <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-rose-500 rounded-full ring-2 ring-white" />}
          </button>

          <button
            onClick={onLogout}
            className="inline-flex items-center space-x-1.5 p-2 rounded-lg text-slate-600 hover:bg-slate-100 transition-colors"
            title="Sair"
          >
            <LogOut className="w-4 h-4" />
            <span className="hidden sm:inline text-xs font-medium">Sair</span>
          </button>
        </div>
      </div>
    </header>
  );
};
