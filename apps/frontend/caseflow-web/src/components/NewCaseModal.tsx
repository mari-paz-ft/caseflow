import React, { useState } from 'react';
import { CreateCasePayload } from '../types';
import { X, FilePlus2, AlertCircle } from 'lucide-react';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (payload: CreateCasePayload) => Promise<void>;
}

export const NewCaseModal: React.FC<Props> = ({ isOpen, onClose, onSubmit }) => {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (title.trim().length < 5 || title.trim().length > 120) {
      setError('O título deve conter entre 5 e 120 caracteres.');
      return;
    }
    if (description.trim().length < 20 || description.trim().length > 2000) {
      setError('A descrição deve conter entre 20 e 2.000 caracteres.');
      return;
    }

    try {
      setIsSubmitting(true);
      setError(null);
      await onSubmit({
        title: title.trim(),
        description: description.trim(),
        type: 'ANALISE_DOCUMENTAL'
      });
      setTitle('');
      setDescription('');
      onClose();
    } catch (err: any) {
      setError(err.message || 'Erro ao criar a solicitação.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm p-4">
      <div className="bg-white rounded-2xl shadow-xl border border-slate-200 w-full max-w-lg overflow-hidden animate-in fade-in zoom-in-95 duration-200">
        <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <FilePlus2 className="w-5 h-5 text-brand-600" />
            <h2 className="text-lg font-bold text-slate-900">Novo Rascunho de Solicitação</h2>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {error && (
            <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-rose-700 text-xs flex items-center space-x-2">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          <div>
            <div className="flex justify-between items-center mb-1">
              <label className="block text-xs font-semibold text-slate-700">
                Título da Solicitação *
              </label>
              <span className={`text-[10px] ${title.length < 5 || title.length > 120 ? 'text-slate-400' : 'text-emerald-600'}`}>
                {title.length}/120
              </span>
            </div>
            <input
              type="text"
              required
              value={title}
              onChange={e => setTitle(e.target.value)}
              placeholder="Ex: Credenciamento Documental - Alpha Tech"
              className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500"
            />
          </div>

          <div>
            <div className="flex justify-between items-center mb-1">
              <label className="block text-xs font-semibold text-slate-700">
                Descrição Detalhada *
              </label>
              <span className={`text-[10px] ${description.length < 20 || description.length > 2000 ? 'text-slate-400' : 'text-emerald-600'}`}>
                {description.length}/2000
              </span>
            </div>
            <textarea
              required
              rows={4}
              value={description}
              onChange={e => setDescription(e.target.value)}
              placeholder="Descreva o propósito da solicitação e os documentos que serão conferidos (mínimo 20 caracteres)..."
              className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500"
            />
          </div>

          <div className="pt-2 flex justify-end space-x-3">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm font-medium text-slate-700 bg-slate-100 hover:bg-slate-200 rounded-xl transition-colors"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-4 py-2 text-sm font-medium text-white bg-brand-600 hover:bg-brand-700 disabled:opacity-50 rounded-xl shadow-sm transition-colors"
            >
              {isSubmitting ? 'Criando...' : 'Salvar Rascunho'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
