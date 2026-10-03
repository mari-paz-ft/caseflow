import { FormEvent } from 'react'
import { RotateCcw } from 'lucide-react'

interface Props {
  isOpen: boolean
  isRetrying: boolean
  justification: string
  onJustificationChange: (value: string) => void
  onClose: () => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
}

export function RetryModal({
  isOpen,
  isRetrying,
  justification,
  onJustificationChange,
  onClose,
  onSubmit,
}: Props) {
  if (!isOpen) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm p-4">
      <div className="bg-white rounded-2xl shadow-xl border border-slate-200 w-full max-w-lg p-6 space-y-4">
        <h3 className="text-base font-bold text-slate-900 flex items-center space-x-2">
          <RotateCcw className="w-5 h-5 text-amber-600" />
          <span>Solicitar Reprocessamento Administrativo</span>
        </h3>
        <p className="text-xs text-slate-600">
          Conforme as regras de governança, o administrador pode solicitar nova conferência para casos em FALHA TÉCNICA mediante justificativa formal auditável (10 a 500 caracteres).
        </p>

        <form onSubmit={onSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Justificativa Operacional *</label>
            <textarea
              required
              rows={3}
              value={justification}
              onChange={event => onJustificationChange(event.target.value)}
              placeholder="Ex: Armazenamento em disco restaurado e verificado com sucesso pelo time de infraestrutura."
              className="w-full px-3.5 py-2 bg-slate-50 border border-slate-300 rounded-xl text-xs focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500"
            />
            <span className="text-[10px] text-slate-400">{justification.length}/500 caracteres</span>
          </div>

          <div className="flex justify-end space-x-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="px-3 py-1.5 text-xs text-slate-600 bg-slate-100 hover:bg-slate-200 rounded-lg"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={isRetrying || justification.trim().length < 10}
              className="px-4 py-1.5 text-xs font-semibold text-white bg-amber-600 hover:bg-amber-700 disabled:opacity-50 rounded-lg shadow-sm"
            >
              {isRetrying ? 'Reprocessando...' : 'Confirmar Reprocessamento'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
