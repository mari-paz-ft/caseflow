import React from 'react';
import { NotificationItem } from '../types';
import { X, Bell, Check, Clock } from 'lucide-react';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  notifications: NotificationItem[];
  onMarkRead: (id: string) => void;
  onSelectCase: (caseId: string) => void;
}

export const NotificationsModal: React.FC<Props> = ({
  isOpen,
  onClose,
  notifications,
  onMarkRead,
  onSelectCase
}) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm p-4">
      <div className="bg-white rounded-2xl shadow-xl border border-slate-200 w-full max-w-lg overflow-hidden flex flex-col max-h-[85vh]">
        <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <Bell className="w-5 h-5 text-brand-600" />
            <h2 className="text-lg font-bold text-slate-900">Central de Notificações</h2>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6 overflow-y-auto space-y-3 flex-1">
          {notifications.length === 0 ? (
            <div className="text-center py-12 text-slate-400 text-sm">
              Nenhuma notificação registrada no momento.
            </div>
          ) : (
            notifications.map(notif => {
              const isUnread = !notif.readAt;
              return (
                <div
                  key={notif.id}
                  className={`p-4 rounded-xl border transition-all ${
                    isUnread
                      ? 'bg-blue-50/50 border-blue-200 shadow-sm'
                      : 'bg-slate-50 border-slate-200 opacity-80'
                  }`}
                >
                  <div className="flex items-start justify-between">
                    <h3 className="text-sm font-semibold text-slate-900">{notif.title}</h3>
                    {isUnread && (
                      <button
                        onClick={() => onMarkRead(notif.id)}
                        className="text-xs text-brand-600 hover:text-brand-800 flex items-center space-x-1 font-medium ml-2 shrink-0"
                        title="Marcar como lida"
                      >
                        <Check className="w-3.5 h-3.5" />
                        <span>Marcar lida</span>
                      </button>
                    )}
                  </div>
                  <p className="text-xs text-slate-600 mt-1">{notif.message}</p>
                  <div className="mt-3 flex items-center justify-between">
                    <span className="text-[11px] text-slate-400 flex items-center space-x-1">
                      <Clock className="w-3 h-3" />
                      <span>{new Date(notif.createdAt).toLocaleTimeString()}</span>
                    </span>
                    <button
                      onClick={() => {
                        onSelectCase(notif.caseId);
                        onClose();
                      }}
                      className="text-xs font-medium text-brand-600 hover:underline"
                    >
                      Ver solicitação &rarr;
                    </button>
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>
    </div>
  );
};
