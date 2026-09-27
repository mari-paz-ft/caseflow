import { useState, useEffect } from 'react';
import { CaseRequest, User, CreateCasePayload, CaseStatus, NotificationItem } from './types';
import { ApiService } from './services/api';
import { LoginForm } from './components/LoginForm';
import { Navbar } from './components/Navbar';
import { StatusBadge } from './components/StatusBadge';
import { NewCaseModal } from './components/NewCaseModal';
import { NotificationsModal } from './components/NotificationsModal';
import { CaseDetail } from './components/CaseDetail';
import {
  Plus,
  Search,
  FileCheck2,
  FolderOpen,
  CheckCircle,
  XCircle,
  AlertTriangle,
  ArrowUpRight,
  Filter
} from 'lucide-react';

export function App() {
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [cases, setCases] = useState<CaseRequest[]>([]);
  const [selectedCaseId, setSelectedCaseId] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<CaseStatus | 'TODOS'>('TODOS');
  const [searchQuery, setSearchQuery] = useState('');
  const [isNewModalOpen, setIsNewModalOpen] = useState(false);
  const [isNotifsOpen, setIsNotifsOpen] = useState(false);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);

  const loadData = async () => {
    if (!currentUser) return;
    try {
      setLoading(true);
      setLoadError(null);
      const data = await ApiService.getCases();
      setCases(data);
      const notifs = await ApiService.getNotifications();
      setNotifications(notifs);
    } catch (error) {
      setLoadError(error instanceof Error ? error.message : 'Erro ao carregar dados.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    setSelectedCaseId(null);
    if (currentUser) {
      void loadData();
    } else {
      setCases([]);
      setNotifications([]);
      setLoading(false);
    }
  }, [currentUser]);

  const handleLogin = async (username: string, password: string) => {
    const session = await ApiService.login(username, password);
    ApiService.setAccessToken(session.token);
    setCurrentUser(session.user);
  };

  const handleLogout = () => {
    ApiService.setAccessToken(null);
    setCurrentUser(null);
    setCases([]);
    setNotifications([]);
    setSelectedCaseId(null);
  };

  const handleCreateCase = async (payload: CreateCasePayload) => {
    const created = await ApiService.createCase(payload);
    await loadData();
    setSelectedCaseId(created.id);
  };

  const handleMarkNotifRead = async (id: string) => {
    await ApiService.markNotificationRead(id);
    const notifs = await ApiService.getNotifications();
    setNotifications(notifs);
  };

  if (!currentUser) return <LoginForm onLogin={handleLogin} />;

  // Filtragem
  const filteredCases = cases.filter(c => {
    const matchesStatus = statusFilter === 'TODOS' || c.status === statusFilter;
    const matchesSearch =
      c.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      c.protocol.toLowerCase().includes(searchQuery.toLowerCase()) ||
      c.description.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesStatus && matchesSearch;
  });

  // Estatísticas
  const stats = {
    total: cases.length,
    rascunho: cases.filter(c => c.status === 'RASCUNHO').length,
    aprovadas: cases.filter(c => c.status === 'APROVADA').length,
    rejeitadas: cases.filter(c => c.status === 'REJEITADA').length,
    falhas: cases.filter(c => c.status === 'FALHA_TECNICA').length
  };

  const unreadCount = notifications.filter(n => !n.readAt).length;

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      <Navbar
        currentUser={currentUser}
        onLogout={handleLogout}
        onOpenNotifications={() => setIsNotifsOpen(true)}
        unreadCount={unreadCount}
      />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {loadError && <div role="alert" className="mb-4 p-3 bg-rose-50 border border-rose-200 rounded-xl text-rose-700 text-sm">{loadError}</div>}
        {selectedCaseId ? (
          <CaseDetail
            caseId={selectedCaseId}
            currentUser={currentUser}
            onBack={() => setSelectedCaseId(null)}
            onRefreshList={loadData}
          />
        ) : (
          <div className="space-y-6">
            {/* Header com boas-vindas e botão de ação */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div>
                <h1 className="text-2xl font-bold text-slate-900 tracking-tight">
                  Painel de Solicitações
                </h1>
                <p className="text-sm text-slate-500 mt-0.5">
                  {currentUser.role === 'ROLE_ADMIN'
                    ? 'Visão Administrativa: acompanhamento global e auditoria de todos os processos.'
                    : 'Gerencie seus rascunhos, anexe documentos e acompanhe a conferência automática.'}
                </p>
              </div>

              <button
                onClick={() => setIsNewModalOpen(true)}
                className="inline-flex items-center space-x-2 px-4 py-2.5 bg-brand-600 hover:bg-brand-700 text-white text-sm font-semibold rounded-xl shadow-sm shadow-brand-600/20 transition-all hover:shadow-md"
              >
                <Plus className="w-4 h-4" />
                <span>Nova Solicitação</span>
              </button>
            </div>

            {/* Widgets de métricas */}
            <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
              <div
                onClick={() => setStatusFilter('TODOS')}
                className={`p-4 rounded-xl border cursor-pointer transition-all ${
                  statusFilter === 'TODOS'
                    ? 'bg-white border-brand-500 shadow-sm ring-2 ring-brand-500/10'
                    : 'bg-white border-slate-200 hover:border-slate-300'
                }`}
              >
                <div className="flex items-center justify-between text-slate-500 text-xs font-semibold">
                  <span>Todas</span>
                  <FolderOpen className="w-4 h-4 text-slate-400" />
                </div>
                <div className="text-2xl font-bold text-slate-900 mt-2">{stats.total}</div>
              </div>

              <div
                onClick={() => setStatusFilter('RASCUNHO')}
                className={`p-4 rounded-xl border cursor-pointer transition-all ${
                  statusFilter === 'RASCUNHO'
                    ? 'bg-white border-slate-500 shadow-sm ring-2 ring-slate-500/10'
                    : 'bg-white border-slate-200 hover:border-slate-300'
                }`}
              >
                <div className="flex items-center justify-between text-slate-500 text-xs font-semibold">
                  <span>Rascunhos</span>
                  <FileCheck2 className="w-4 h-4 text-slate-400" />
                </div>
                <div className="text-2xl font-bold text-slate-700 mt-2">{stats.rascunho}</div>
              </div>

              <div
                onClick={() => setStatusFilter('APROVADA')}
                className={`p-4 rounded-xl border cursor-pointer transition-all ${
                  statusFilter === 'APROVADA'
                    ? 'bg-white border-emerald-500 shadow-sm ring-2 ring-emerald-500/10'
                    : 'bg-white border-slate-200 hover:border-slate-300'
                }`}
              >
                <div className="flex items-center justify-between text-emerald-700 text-xs font-semibold">
                  <span>Aprovadas</span>
                  <CheckCircle className="w-4 h-4 text-emerald-500" />
                </div>
                <div className="text-2xl font-bold text-emerald-600 mt-2">{stats.aprovadas}</div>
              </div>

              <div
                onClick={() => setStatusFilter('REJEITADA')}
                className={`p-4 rounded-xl border cursor-pointer transition-all ${
                  statusFilter === 'REJEITADA'
                    ? 'bg-white border-rose-500 shadow-sm ring-2 ring-rose-500/10'
                    : 'bg-white border-slate-200 hover:border-slate-300'
                }`}
              >
                <div className="flex items-center justify-between text-rose-700 text-xs font-semibold">
                  <span>Rejeitadas</span>
                  <XCircle className="w-4 h-4 text-rose-500" />
                </div>
                <div className="text-2xl font-bold text-rose-600 mt-2">{stats.rejeitadas}</div>
              </div>

              <div
                onClick={() => setStatusFilter('FALHA_TECNICA')}
                className={`p-4 rounded-xl border cursor-pointer transition-all col-span-2 sm:col-span-1 ${
                  statusFilter === 'FALHA_TECNICA'
                    ? 'bg-white border-amber-500 shadow-sm ring-2 ring-amber-500/10'
                    : 'bg-white border-slate-200 hover:border-slate-300'
                }`}
              >
                <div className="flex items-center justify-between text-amber-800 text-xs font-semibold">
                  <span>Falhas Técnicas</span>
                  <AlertTriangle className="w-4 h-4 text-amber-500" />
                </div>
                <div className="text-2xl font-bold text-amber-700 mt-2">{stats.falhas}</div>
              </div>
            </div>

            {/* Barra de busca e filtros */}
            <div className="flex flex-col sm:flex-row gap-3">
              <div className="relative flex-1">
                <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
                <input
                  type="text"
                  value={searchQuery}
                  onChange={e => setSearchQuery(e.target.value)}
                  placeholder="Pesquisar por título, protocolo ou termo..."
                  className="w-full pl-10 pr-4 py-2.5 bg-white border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 shadow-sm"
                />
              </div>

              <div className="flex items-center space-x-2">
                <Filter className="w-4 h-4 text-slate-400" />
                <select
                  value={statusFilter}
                  onChange={e => setStatusFilter(e.target.value as any)}
                  className="px-3 py-2.5 bg-white border border-slate-200 rounded-xl text-sm text-slate-700 shadow-sm focus:outline-none focus:ring-2 focus:ring-brand-500/20"
                >
                  <option value="TODOS">Todos os Status</option>
                  <option value="RASCUNHO">Rascunhos</option>
                  <option value="ENVIADA">Enviadas</option>
                  <option value="PROCESSANDO">Processando</option>
                  <option value="APROVADA">Aprovadas</option>
                  <option value="REJEITADA">Rejeitadas</option>
                  <option value="FALHA_TECNICA">Falha Técnica</option>
                </select>
              </div>
            </div>

            {/* Listagem */}
            {loading ? (
              <div className="text-center py-16 text-slate-400 text-sm">Carregando solicitações...</div>
            ) : filteredCases.length === 0 ? (
              <div className="bg-white rounded-2xl border border-dashed border-slate-300 p-12 text-center">
                <FolderOpen className="w-8 h-8 text-slate-400 mx-auto mb-2" />
                <h3 className="text-sm font-semibold text-slate-700">Nenhuma solicitação encontrada</h3>
                <p className="text-xs text-slate-500 mt-1">
                  Não encontramos registros correspondentes aos filtros selecionados.
                </p>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {filteredCases.map(caseItem => (
                  <div
                    key={caseItem.id}
                    onClick={() => setSelectedCaseId(caseItem.id)}
                    className="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm hover:shadow-md hover:border-brand-300 transition-all cursor-pointer flex flex-col justify-between group"
                  >
                    <div>
                      <div className="flex items-center justify-between gap-2 mb-3">
                        <span className="font-mono text-xs font-bold text-slate-600 bg-slate-100 px-2 py-0.5 rounded">
                          {caseItem.protocol}
                        </span>
                        <StatusBadge status={caseItem.status} size="sm" />
                      </div>

                      <h3 className="font-bold text-slate-900 text-base group-hover:text-brand-600 transition-colors line-clamp-1">
                        {caseItem.title}
                      </h3>
                      <p className="text-xs text-slate-500 mt-1.5 line-clamp-2">
                        {caseItem.description}
                      </p>
                    </div>

                    <div className="mt-5 pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-slate-400">
                      <div className="flex items-center space-x-2">
                        <span>{caseItem.documents.length} anexo(s)</span>
                        {currentUser.role === 'ROLE_ADMIN' && (
                          <span className="truncate max-w-[110px]" title={caseItem.ownerEmail}>
                            • {caseItem.ownerEmail.split('@')[0]}
                          </span>
                        )}
                      </div>
                      <span className="flex items-center text-brand-600 font-semibold group-hover:translate-x-0.5 transition-transform">
                        Ver detalhes <ArrowUpRight className="w-3.5 h-3.5 ml-0.5" />
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </main>

      {/* Modal de Nova Solicitação */}
      <NewCaseModal
        isOpen={isNewModalOpen}
        onClose={() => setIsNewModalOpen(false)}
        onSubmit={handleCreateCase}
      />

      {/* Modal de Notificações */}
      <NotificationsModal
        isOpen={isNotifsOpen}
        onClose={() => setIsNotifsOpen(false)}
        notifications={notifications}
        onMarkRead={handleMarkNotifRead}
        onSelectCase={id => setSelectedCaseId(id)}
      />
    </div>
  );
}
export default App;
