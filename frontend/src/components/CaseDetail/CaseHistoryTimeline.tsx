import { CaseHistory } from '../../types'
import { History } from 'lucide-react'

interface Props {
  histories: CaseHistory[]
}

export function CaseHistoryTimeline({ histories }: Props) {
  return (
    <div className="space-y-4 pt-6 border-t border-slate-100">
      <h2 className="text-base font-bold text-slate-900 flex items-center space-x-2">
        <History className="w-5 h-5 text-brand-600" />
        <span>Histórico de Transições e Rastreabilidade</span>
      </h2>

      <div className="relative pl-6 border-l-2 border-slate-200 space-y-4 my-4">
        {histories.map(history => (
          <div key={history.id} className="relative group">
            <div className="absolute -left-[31px] top-1.5 w-3 h-3 rounded-full bg-brand-600 ring-4 ring-white" />
            <div className="flex flex-col sm:flex-row sm:items-center justify-between text-xs">
              <div className="flex items-center space-x-2">
                <span className="font-bold text-slate-800">{history.eventType}</span>
                <span className="px-1.5 py-0.5 bg-slate-100 text-slate-600 rounded text-[10px] font-mono">
                  {history.actorSubject}
                </span>
              </div>
              <span className="text-slate-400 text-[11px] mt-0.5 sm:mt-0">
                {new Date(history.occurredAt).toLocaleString()}
              </span>
            </div>
            {history.details && <p className="text-xs text-slate-600 mt-1">{history.details}</p>}
          </div>
        ))}
      </div>
    </div>
  )
}
