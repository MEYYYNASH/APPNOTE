import React, { useState, useMemo } from 'react';
import { AppStrings } from '../types/note';
import { 
  Table, Plus, Trash2, ArrowUpDown, Search, Download, 
  Star, Calendar, CheckSquare, ChevronDown, Undo2, Redo2, Maximize2, Minimize2, X, PlusCircle,
  Link as LinkIcon, Mail, Phone, DollarSign, Percent, AlertCircle, FileText
} from 'lucide-react';

export interface TableColumn {
  id: string;
  name: string;
  type: 'text' | 'number' | 'date' | 'checkbox' | 'dropdown' | 'status' | 'currency' | 'progress' | 'rating' | 'email' | 'phone' | 'link';
  width: number;
  options?: string[];
}

export interface InteractiveTableData {
  title: string;
  columns: TableColumn[];
  rows: Record<string, any>[];
}

interface InteractiveTableEditorProps {
  initialData?: InteractiveTableData;
  strings: AppStrings;
  onSave: (tableData: InteractiveTableData, markdownOutput: string) => void;
  onClose: () => void;
}

const DEFAULT_TEMPLATES: { name: string; data: InteractiveTableData }[] = [
  {
    name: 'Task Tracker',
    data: {
      title: 'Sprint Task Tracker',
      columns: [
        { id: 'task', name: 'Task Name', type: 'text', width: 220 },
        { id: 'status', name: 'Status', type: 'status', width: 140, options: ['Not Started', 'In Progress', 'Completed', 'Blocked'] },
        { id: 'priority', name: 'Priority', type: 'dropdown', width: 120, options: ['Low', 'Medium', 'High', 'Urgent'] },
        { id: 'due', name: 'Due Date', type: 'date', width: 130 },
        { id: 'progress', name: 'Progress', type: 'progress', width: 120 },
        { id: 'done', name: 'Done', type: 'checkbox', width: 80 }
      ],
      rows: [
        { task: 'Design Glassmorphism UI', status: 'Completed', priority: 'High', due: '2026-11-15', progress: 100, done: true },
        { task: 'Implement Interactive Table', status: 'In Progress', priority: 'Urgent', due: '2026-11-20', progress: 75, done: false },
        { task: 'Cloud Sync & Auth', status: 'Not Started', priority: 'Medium', due: '2026-11-28', progress: 0, done: false }
      ]
    }
  },
  {
    name: 'Project Budget',
    data: {
      title: 'Q4 Project Budget',
      columns: [
        { id: 'item', name: 'Expense Item', type: 'text', width: 200 },
        { id: 'category', name: 'Category', type: 'dropdown', width: 140, options: ['Engineering', 'Design', 'Marketing', 'Infrastructure'] },
        { id: 'cost', name: 'Cost', type: 'currency', width: 120 },
        { id: 'rating', name: 'ROI Rating', type: 'rating', width: 130 },
        { id: 'approved', name: 'Approved', type: 'checkbox', width: 90 }
      ],
      rows: [
        { item: 'Cloud Server Hosting', category: 'Infrastructure', cost: 1250, rating: 5, approved: true },
        { item: 'UI/UX Asset Licensing', category: 'Design', cost: 450, rating: 4, approved: true },
        { item: 'Beta Tester Rewards', category: 'Marketing', cost: 800, rating: 4, approved: false }
      ]
    }
  }
];

export const InteractiveTableEditor: React.FC<InteractiveTableEditorProps> = ({
  initialData,
  strings,
  onSave,
  onClose,
}) => {
  const [tableData, setTableData] = useState<InteractiveTableData>(
    initialData || DEFAULT_TEMPLATES[0].data
  );
  const [history, setHistory] = useState<InteractiveTableData[]>([]);
  const [redoStack, setRedoStack] = useState<InteractiveTableData[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [sortColumn, setSortColumn] = useState<string | null>(null);
  const [sortAsc, setSortAsc] = useState(true);
  const [isMaximized, setIsMaximized] = useState(false);
  const [toastMsg, setToastMsg] = useState<string | null>(null);

  const showToast = (msg: string) => {
    setToastMsg(msg);
    setTimeout(() => setToastMsg(null), 2500);
  };

  const recordState = (newData: InteractiveTableData) => {
    setHistory((prev) => [...prev.slice(-15), tableData]);
    setRedoStack([]);
    setTableData(newData);
  };

  const handleUndo = () => {
    if (history.length === 0) return;
    const prev = history[history.length - 1];
    setRedoStack((r) => [tableData, ...r]);
    setHistory((h) => h.slice(0, -1));
    setTableData(prev);
    showToast('Undo applied');
  };

  const handleRedo = () => {
    if (redoStack.length === 0) return;
    const next = redoStack[0];
    setHistory((h) => [...h, tableData]);
    setRedoStack((r) => r.slice(1));
    setTableData(next);
    showToast('Redo applied');
  };

  const updateCell = (rowIndex: number, colId: string, value: any) => {
    const updatedRows = [...tableData.rows];
    updatedRows[rowIndex] = { ...updatedRows[rowIndex], [colId]: value };
    recordState({ ...tableData, rows: updatedRows });
  };

  const addRow = () => {
    const newRow: Record<string, any> = {};
    tableData.columns.forEach((col) => {
      if (col.type === 'checkbox') newRow[col.id] = false;
      else if (col.type === 'number' || col.type === 'currency' || col.type === 'progress') newRow[col.id] = 0;
      else if (col.type === 'rating') newRow[col.id] = 3;
      else newRow[col.id] = 'New Item';
    });
    recordState({ ...tableData, rows: [...tableData.rows, newRow] });
    showToast('Row added');
  };

  const deleteRow = (index: number) => {
    if (tableData.rows.length <= 1) {
      showToast('Table must have at least one row');
      return;
    }
    const updatedRows = tableData.rows.filter((_, idx) => idx !== index);
    recordState({ ...tableData, rows: updatedRows });
    showToast('Row deleted');
  };

  const addColumn = () => {
    const colNum = tableData.columns.length + 1;
    const newColId = `col_${Date.now()}`;
    const newCol: TableColumn = {
      id: newColId,
      name: `Column ${colNum}`,
      type: 'text',
      width: 150
    };
    const updatedRows = tableData.rows.map((r) => ({ ...r, [newColId]: 'Value' }));
    recordState({
      ...tableData,
      columns: [...tableData.columns, newCol],
      rows: updatedRows
    });
    showToast('Column added');
  };

  const deleteColumn = (colId: string) => {
    if (tableData.columns.length <= 1) {
      showToast('Table must have at least one column');
      return;
    }
    const updatedCols = tableData.columns.filter((c) => c.id !== colId);
    const updatedRows = tableData.rows.map((r) => {
      const copy = { ...r };
      delete copy[colId];
      return copy;
    });
    recordState({ ...tableData, columns: updatedCols, rows: updatedRows });
    showToast('Column deleted');
  };

  const renameColumn = (colId: string, newName: string) => {
    const updatedCols = tableData.columns.map((c) => (c.id === colId ? { ...c, name: newName } : c));
    setTableData({ ...tableData, columns: updatedCols });
  };

  const changeColumnType = (colId: string, type: TableColumn['type']) => {
    const updatedCols = tableData.columns.map((c) => (c.id === colId ? { ...c, type } : c));
    recordState({ ...tableData, columns: updatedCols });
    showToast(`Column type changed to ${type}`);
  };

  const processedRows = useMemo(() => {
    let rows = [...tableData.rows];
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      rows = rows.filter((row) =>
        Object.values(row).some((val) => String(val).toLowerCase().includes(q))
      );
    }
    if (sortColumn) {
      rows.sort((a, b) => {
        const va = a[sortColumn] ?? '';
        const vb = b[sortColumn] ?? '';
        if (va < vb) return sortAsc ? -1 : 1;
        if (va > vb) return sortAsc ? 1 : -1;
        return 0;
      });
    }
    return rows;
  }, [tableData.rows, searchQuery, sortColumn, sortAsc]);

  const handleExportCSV = () => {
    const headers = tableData.columns.map((c) => `"${c.name}"`).join(',');
    const csvRows = tableData.rows.map((r) =>
      tableData.columns.map((c) => `"${String(r[c.id] ?? '')}"`).join(',')
    );
    const csvContent = 'data:text/csv;charset=utf-8,' + [headers, ...csvRows].join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `${tableData.title.toLowerCase().replace(/\s+/g, '_')}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    showToast('Exported as CSV successfully');
  };

  const handleSaveAndInsert = () => {
    let md = `### ${tableData.title}\n\n`;
    md += '| ' + tableData.columns.map((c) => c.name).join(' | ') + ' |\n';
    md += '| ' + tableData.columns.map(() => '---').join(' | ') + ' |\n';
    tableData.rows.forEach((r) => {
      md += '| ' + tableData.columns.map((c) => String(r[c.id] ?? '')).join(' | ') + ' |\n';
    });

    let html = `<div style="overflow-x:auto; margin:14px 0;"><table style="width:100%;border-collapse:collapse;border-radius:18px;overflow:hidden;border:1px solid rgba(255,255,255,0.25);background:rgba(15,23,42,0.85);color:#f8fafc;box-shadow:0 10px 30px rgba(0,0,0,0.3);"><thead><tr style="background:linear-gradient(135deg,rgba(37,99,235,0.4),rgba(79,70,229,0.4));">`;
    tableData.columns.forEach((c) => {
      html += `<th style="padding:12px 16px;text-align:left;border-bottom:1px solid rgba(255,255,255,0.2);color:#93c5fd;font-weight:800;font-size:12px;text-transform:uppercase;letter-spacing:0.05em;">${c.name}</th>`;
    });
    html += '</tr></thead><tbody>';
    tableData.rows.forEach((r, idx) => {
      const bg = idx % 2 === 0 ? 'background:rgba(255,255,255,0.03);' : '';
      html += `<tr style="${bg}">`;
      tableData.columns.forEach((c) => {
        html += `<td style="padding:12px 16px;border-bottom:1px solid rgba(255,255,255,0.1);font-size:13px;color:#e2e8f0;">${String(r[c.id] ?? '')}</td>`;
      });
      html += '</tr>';
    });
    html += '</tbody></table></div><p><br></p>';

    onSave(tableData, html);
    showToast('Interactive Table saved!');
    onClose();
  };

  return (
    <div className={`fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-6 bg-black/70 backdrop-blur-md animate-in fade-in select-none ${isMaximized ? 'p-0' : ''}`}>
      <div className={`w-full ${isMaximized ? 'max-w-full h-full rounded-none' : 'max-w-6xl h-[90vh] rounded-3xl'} glass-panel-elevated flex flex-col shadow-2xl border border-white/40 overflow-hidden bg-slate-900/95 text-white transition-all`}>
        
        {/* Top Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-white/15 bg-slate-900/80">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-2xl bg-gradient-to-tr from-blue-600 to-indigo-600 text-white shadow-md">
              <Table size={20} />
            </div>
            <div>
              <input
                type="text"
                value={tableData.title}
                onChange={(e) => setTableData({ ...tableData, title: e.target.value })}
                className="bg-transparent text-lg font-extrabold text-white focus:outline-none focus:ring-1 focus:ring-blue-500 rounded px-1"
                placeholder="Table Title..."
              />
              <p className="text-xs text-slate-400">Professional Interactive Database & Grid Editor</p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <select
              onChange={(e) => {
                const t = DEFAULT_TEMPLATES.find((x) => x.name === e.target.value);
                if (t) {
                  recordState(JSON.parse(JSON.stringify(t.data)));
                  showToast(`Loaded template: ${t.name}`);
                }
              }}
              className="glass-field px-3 py-1.5 rounded-xl text-xs font-semibold bg-slate-800 text-slate-200 border border-white/20"
              defaultValue=""
            >
              <option value="" disabled>Load Template...</option>
              {DEFAULT_TEMPLATES.map((t) => (
                <option key={t.name} value={t.name}>{t.name}</option>
              ))}
            </select>

            <button
              onClick={handleUndo}
              disabled={history.length === 0}
              className="p-2 rounded-xl glass-field hover:bg-white/10 disabled:opacity-30 transition-colors"
              title="Undo"
            >
              <Undo2 size={16} />
            </button>
            <button
              onClick={handleRedo}
              disabled={redoStack.length === 0}
              className="p-2 rounded-xl glass-field hover:bg-white/10 disabled:opacity-30 transition-colors"
              title="Redo"
            >
              <Redo2 size={16} />
            </button>

            <button
              onClick={handleExportCSV}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl glass-field text-xs font-semibold hover:bg-white/10 transition-colors"
              title="Export CSV"
            >
              <Download size={14} />
              <span className="hidden sm:inline">Export CSV</span>
            </button>

            <button
              onClick={() => setIsMaximized(!isMaximized)}
              className="p-2 rounded-xl glass-field hover:bg-white/10 transition-colors"
              title={isMaximized ? 'Restore' : 'Maximize'}
            >
              {isMaximized ? <Minimize2 size={16} /> : <Maximize2 size={16} />}
            </button>

            <button
              onClick={onClose}
              className="p-2 rounded-xl glass-field hover:bg-red-500/20 hover:text-red-400 transition-colors"
            >
              <X size={18} />
            </button>
          </div>
        </div>

        {/* Toolbar & Search */}
        <div className="flex flex-wrap items-center justify-between gap-3 px-6 py-3 border-b border-white/10 bg-slate-950/40">
          <div className="flex items-center gap-2">
            <button
              onClick={addRow}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-blue-600/30 border border-blue-500/40 text-blue-300 text-xs font-bold hover:bg-blue-600/50 transition-all shadow-sm"
            >
              <Plus size={14} />
              <span>Add Row</span>
            </button>
            <button
              onClick={addColumn}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-indigo-600/30 border border-indigo-500/40 text-indigo-300 text-xs font-bold hover:bg-indigo-600/50 transition-all shadow-sm"
            >
              <PlusCircle size={14} />
              <span>Add Column</span>
            </button>
          </div>

          <div className="flex items-center gap-2">
            <div className="relative">
              <Search size={14} className="absolute left-3 top-2.5 text-slate-400" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search table..."
                className="pl-9 pr-3 py-1.5 rounded-xl glass-field bg-slate-800/80 border border-white/15 text-xs text-white focus:outline-none focus:ring-1 focus:ring-blue-500 w-48 sm:w-64"
              />
            </div>
            {searchQuery && (
              <button
                onClick={() => setSearchQuery('')}
                className="text-xs text-slate-400 hover:text-white underline"
              >
                Clear
              </button>
            )}
          </div>
        </div>

        {/* Table Grid Content */}
        <div className="flex-1 overflow-auto p-4 sm:p-6 bg-slate-950/60">
          <div className="inline-block min-w-full align-middle">
            <div className="rounded-2xl border border-white/20 overflow-hidden shadow-2xl bg-slate-900/90">
              <table className="min-w-full divide-y divide-white/10">
                <thead>
                  <tr className="bg-gradient-to-r from-blue-950/80 via-indigo-950/80 to-slate-900/80">
                    <th scope="col" className="w-12 px-3 py-3.5 text-center text-xs font-bold text-slate-400 uppercase tracking-wider border-r border-white/10">
                      #
                    </th>
                    {tableData.columns.map((col) => (
                      <th
                        key={col.id}
                        scope="col"
                        className="px-4 py-3.5 text-left text-xs font-bold text-slate-200 uppercase tracking-wider border-r border-white/10 group relative"
                        style={{ width: `${col.width}px` }}
                      >
                        <div className="flex items-center justify-between gap-2">
                          <div className="flex items-center gap-1.5 flex-1">
                            {col.type === 'text' && <FileText size={13} className="text-slate-400 shrink-0" />}
                            {col.type === 'number' && <span className="text-[10px] font-mono font-bold text-blue-400">123</span>}
                            {col.type === 'date' && <Calendar size={13} className="text-amber-400 shrink-0" />}
                            {col.type === 'checkbox' && <CheckSquare size={13} className="text-emerald-400 shrink-0" />}
                            {col.type === 'dropdown' && <ChevronDown size={13} className="text-purple-400 shrink-0" />}
                            {col.type === 'status' && <AlertCircle size={13} className="text-cyan-400 shrink-0" />}
                            {col.type === 'currency' && <DollarSign size={13} className="text-emerald-400 shrink-0" />}
                            {col.type === 'progress' && <Percent size={13} className="text-blue-400 shrink-0" />}
                            {col.type === 'rating' && <Star size={13} className="text-amber-400 shrink-0" />}
                            {col.type === 'email' && <Mail size={13} className="text-indigo-400 shrink-0" />}
                            {col.type === 'phone' && <Phone size={13} className="text-teal-400 shrink-0" />}
                            {col.type === 'link' && <LinkIcon size={13} className="text-sky-400 shrink-0" />}

                            <input
                              type="text"
                              value={col.name}
                              onChange={(e) => renameColumn(col.id, e.target.value)}
                              className="bg-transparent font-extrabold text-blue-200 focus:outline-none focus:bg-white/10 rounded px-1 w-full"
                            />
                          </div>

                          <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                            <select
                              value={col.type}
                              onChange={(e) => changeColumnType(col.id, e.target.value as any)}
                              className="bg-slate-800 text-[10px] text-slate-300 rounded border border-white/20 px-1 py-0.5"
                              title="Change Column Type"
                            >
                              <option value="text">Text</option>
                              <option value="number">Number</option>
                              <option value="date">Date</option>
                              <option value="checkbox">Checkbox</option>
                              <option value="dropdown">Dropdown</option>
                              <option value="status">Status</option>
                              <option value="currency">Currency</option>
                              <option value="progress">Progress</option>
                              <option value="rating">Rating</option>
                              <option value="link">Link</option>
                            </select>

                            <button
                              onClick={() => {
                                setSortColumn(col.id);
                                setSortAsc(sortColumn === col.id ? !sortAsc : true);
                              }}
                              className="p-1 rounded hover:bg-white/10 text-slate-300"
                              title="Sort"
                            >
                              <ArrowUpDown size={12} />
                            </button>
                            <button
                              onClick={() => deleteColumn(col.id)}
                              className="p-1 rounded hover:bg-red-500/20 text-red-400"
                              title="Delete Column"
                            >
                              <Trash2 size={12} />
                            </button>
                          </div>
                        </div>
                      </th>
                    ))}
                    <th className="w-12 px-3 py-3.5 text-center text-xs font-bold text-slate-400 uppercase tracking-wider">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/10 bg-slate-900/40">
                  {processedRows.map((row, rIdx) => {
                    const rowBg = rIdx % 2 === 0 ? 'bg-white/[0.02]' : '';
                    return (
                      <tr key={rIdx} className={`hover:bg-blue-600/[0.06] transition-colors ${rowBg}`}>
                        <td className="px-3 py-2.5 whitespace-nowrap text-center text-xs font-mono text-slate-400 border-r border-white/10">
                          {rIdx + 1}
                        </td>
                        {tableData.columns.map((col) => {
                          const val = row[col.id];
                          return (
                            <td key={col.id} className="px-4 py-2.5 whitespace-nowrap text-xs border-r border-white/10">
                              {col.type === 'checkbox' ? (
                                <input
                                  type="checkbox"
                                  checked={!!val}
                                  onChange={(e) => updateCell(rIdx, col.id, e.target.checked)}
                                  className="w-4 h-4 rounded accent-blue-600 cursor-pointer"
                                />
                              ) : col.type === 'status' || col.type === 'dropdown' ? (
                                <select
                                  value={val || ''}
                                  onChange={(e) => updateCell(rIdx, col.id, e.target.value)}
                                  className="bg-slate-800 text-xs text-white rounded-lg px-2 py-1 border border-white/20 focus:outline-none focus:ring-1 focus:ring-blue-500 w-full"
                                >
                                  <option value="">Select...</option>
                                  {(col.options || ['Not Started', 'In Progress', 'Completed']).map((opt) => (
                                    <option key={opt} value={opt}>{opt}</option>
                                  ))}
                                </select>
                              ) : col.type === 'rating' ? (
                                <div className="flex items-center gap-1 cursor-pointer">
                                  {[1, 2, 3, 4, 5].map((star) => (
                                    <Star
                                      key={star}
                                      size={14}
                                      onClick={() => updateCell(rIdx, col.id, star)}
                                      className={star <= (val || 0) ? 'text-amber-400 fill-amber-400' : 'text-slate-600'}
                                    />
                                  ))}
                                </div>
                              ) : col.type === 'progress' ? (
                                <div className="flex items-center gap-2">
                                  <input
                                    type="range"
                                    min="0"
                                    max="100"
                                    value={val || 0}
                                    onChange={(e) => updateCell(rIdx, col.id, parseInt(e.target.value, 10))}
                                    className="w-20 accent-blue-500 h-1 bg-slate-700 rounded-lg"
                                  />
                                  <span className="text-[10px] font-mono text-slate-300">{val || 0}%</span>
                                </div>
                              ) : (
                                <input
                                  type={col.type === 'number' || col.type === 'currency' ? 'number' : col.type === 'date' ? 'date' : 'text'}
                                  value={val ?? ''}
                                  onChange={(e) => updateCell(rIdx, col.id, e.target.value)}
                                  className="bg-transparent text-white focus:outline-none focus:bg-white/10 rounded px-1.5 py-1 w-full"
                                />
                              )}
                            </td>
                          );
                        })}
                        <td className="px-3 py-2.5 whitespace-nowrap text-center">
                          <button
                            onClick={() => deleteRow(rIdx)}
                            className="p-1.5 rounded-lg hover:bg-red-500/20 text-slate-400 hover:text-red-400 transition-colors"
                            title="Delete Row"
                          >
                            <Trash2 size={14} />
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        </div>

        {/* Footer Actions */}
        <div className="flex items-center justify-between px-6 py-4 border-t border-white/15 bg-slate-900/90">
          <div className="text-xs text-slate-400">
            <span>{tableData.rows.length} rows</span> &bull; <span>{tableData.columns.length} columns</span>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={onClose}
              className="px-4 py-2 rounded-xl glass-field text-xs font-semibold hover:bg-white/10 transition-colors"
            >
              {strings.cancelBtn}
            </button>
            <button
              onClick={handleSaveAndInsert}
              className="flex items-center gap-2 px-5 py-2 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 text-white text-xs font-bold shadow-lg hover:brightness-110 active:scale-95 transition-all"
            >
              <Check size={16} />
              <span>Save & Insert into Note</span>
            </button>
          </div>
        </div>

      </div>

      {toastMsg && (
        <div className="fixed bottom-6 right-6 z-50 bg-blue-600 text-white text-xs font-bold px-4 py-2 rounded-xl shadow-2xl border border-blue-400/40 animate-bounce">
          {toastMsg}
        </div>
      )}
    </div>
  );
};
