import { FormEvent } from 'react'
import { DocumentCategory } from '../../types'
import { UploadCloud } from 'lucide-react'

interface Props {
  category: DocumentCategory
  onCategoryChange: (category: DocumentCategory) => void
  validUntil: string
  onValidUntilChange: (date: string) => void
  onFileSelected: (file: File | null) => void
  hasSelectedFile: boolean
  isUploading: boolean
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
}

export function DocumentUploadForm({
  category,
  onCategoryChange,
  validUntil,
  onValidUntilChange,
  onFileSelected,
  hasSelectedFile,
  isUploading,
  onSubmit,
}: Props) {
  return (
    <form onSubmit={onSubmit} className="p-4 bg-slate-50 border border-slate-200 rounded-xl space-y-3 mt-4">
      <h3 className="text-xs font-bold text-slate-700 uppercase tracking-wider flex items-center space-x-1.5">
        <UploadCloud className="w-4 h-4 text-brand-600" />
        <span>Anexar Novo Arquivo</span>
      </h3>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <div>
          <label className="block text-[11px] font-semibold text-slate-600 mb-1">Categoria *</label>
          <select
            value={category}
            onChange={event => onCategoryChange(event.target.value as DocumentCategory)}
            className="w-full px-3 py-1.5 bg-white border border-slate-300 rounded-lg text-xs"
          >
            <option value="IDENTIFICACAO">IDENTIFICACAO (RG/CNH)</option>
            <option value="COMPROVANTE_ENDERECO">COMPROVANTE_ENDERECO</option>
            <option value="COMPLEMENTAR">COMPLEMENTAR</option>
          </select>
        </div>

        <div>
          <label className="block text-[11px] font-semibold text-slate-600 mb-1">Data de Validade (opcional)</label>
          <input
            type="date"
            value={validUntil}
            onChange={event => onValidUntilChange(event.target.value)}
            className="w-full px-3 py-1.5 bg-white border border-slate-300 rounded-lg text-xs"
          />
        </div>

        <div>
          <label className="block text-[11px] font-semibold text-slate-600 mb-1">Arquivo (PDF até 5MB) *</label>
          <input
            type="file"
            required
            accept=".pdf,application/pdf"
            onChange={event => onFileSelected(event.target.files?.[0] || null)}
            className="w-full text-xs text-slate-500 file:mr-2 file:py-1 file:px-2.5 file:rounded-lg file:border-0 file:text-xs file:font-semibold file:bg-brand-50 file:text-brand-700 hover:file:bg-brand-100"
          />
        </div>
      </div>

      <div className="flex justify-end pt-1">
        <button
          type="submit"
          disabled={isUploading || !hasSelectedFile}
          className="px-3 py-1.5 bg-brand-600 hover:bg-brand-700 disabled:opacity-50 text-white rounded-lg text-xs font-semibold transition-colors"
        >
          {isUploading ? 'Anexando...' : 'Confirmar Anexo'}
        </button>
      </div>
    </form>
  )
}
