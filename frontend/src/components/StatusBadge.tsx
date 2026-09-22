import React from 'react';
import { CaseStatus } from '../types';
import { CheckCircle2, XCircle, AlertTriangle, Clock, FileEdit, RefreshCw } from 'lucide-react';

interface Props {
  status: CaseStatus;
  size?: 'sm' | 'md' | 'lg';
}

export const StatusBadge: React.FC<Props> = ({ status, size = 'md' }) => {
  const config = {
    RASCUNHO: {
      label: 'Rascunho',
      bg: 'bg-slate-100 text-slate-700 border-slate-300',
      icon: FileEdit
    },
    ENVIADA: {
      label: 'Enviada',
      bg: 'bg-blue-50 text-blue-700 border-blue-200',
      icon: Clock
    },
    PROCESSANDO: {
      label: 'Processando',
      bg: 'bg-purple-50 text-purple-700 border-purple-200 animate-pulse',
      icon: RefreshCw
    },
    APROVADA: {
      label: 'Aprovada',
      bg: 'bg-emerald-50 text-emerald-700 border-emerald-300',
      icon: CheckCircle2
    },
    REJEITADA: {
      label: 'Rejeitada',
      bg: 'bg-rose-50 text-rose-700 border-rose-300',
      icon: XCircle
    },
    FALHA_TECNICA: {
      label: 'Falha Técnica',
      bg: 'bg-amber-50 text-amber-800 border-amber-300',
      icon: AlertTriangle
    }
  }[status] || {
    label: status,
    bg: 'bg-gray-100 text-gray-700 border-gray-300',
    icon: Clock
  };

  const Icon = config.icon;
  const sizeClasses = {
    sm: 'text-xs px-2 py-0.5 gap-1',
    md: 'text-xs font-semibold px-2.5 py-1 gap-1.5',
    lg: 'text-sm font-semibold px-3 py-1.5 gap-2'
  }[size];

  return (
    <span className={`inline-flex items-center rounded-full border ${config.bg} ${sizeClasses}`}>
      <Icon className={size === 'sm' ? 'w-3 h-3' : 'w-4 h-4'} />
      <span>{config.label}</span>
    </span>
  );
};
