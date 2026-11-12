import React from 'react';
import {
  Note,
  FilterTab,
  SearchFilters,
  NOTE_COLORS,
  AppStrings,
  TypeFilter,
  DateRangeFilter,
  DateModeFilter,
} from '../types/note';
import { AppGlassLogo } from './AppGlassLogo';
import {
  Search,
  X,
  SlidersHorizontal,
  Sun,
  Moon,
  Settings as SettingsIcon,
  Zap,
  Plus,
  Pin,
  CheckSquare,
  Trash2,
  Folder,
  Tag,
  MoreVertical,
  Volume2,
  Image as ImageIcon,
  PenTool,
  Clock,
} from 'lucide-react';

interface NoteListPaneProps {
  notes: Note[];
  recentNotes: Note[];
  allFolders: string[];
  selectedNoteId: string | null;
  searchQuery: string;
  recentSearches: string[];
  activeTab: FilterTab;
  showFilterRow: boolean;
  searchFilters: SearchFilters;
  hidePreviews: boolean;
  isDark: boolean;
  strings: AppStrings;
  onSearchQueryChange: (query: string) => void;
  onCommitSearch: (query: string) => void;
  onSelectTab: (tab: FilterTab) => void;
  onToggleFilterRow: () => void;
  onUpdateFilters: (filters: Partial<SearchFilters>) => void;
  onSelectNote: (noteId: string) => void;
  onCreateNote: () => void;
  onQuickCapture: () => void;
  onToggleTheme: () => void;
  onOpenSettings: () => void;
  onOpenContextMenu: (note: Note) => void;
  onEmptyTrash: () => void;
}

export const NoteListPane: React.FC<NoteListPaneProps> = ({
  notes,
  recentNotes,
  allFolders,
  selectedNoteId,
  searchQuery,
  recentSearches,
  activeTab,
  showFilterRow,
  searchFilters,
  hidePreviews,
  isDark,
  strings,
  onSearchQueryChange,
  onCommitSearch,
  onSelectTab,
  onToggleFilterRow,
  onUpdateFilters,
  onSelectNote,
  onCreateNote,
  onQuickCapture,
  onToggleTheme,
  onOpenSettings,
  onOpenContextMenu,
  onEmptyTrash,
}) => {
  const formatTime = (ts: number) => {
    const d = new Date(ts);
    const today = new Date();
    if (d.toDateString() === today.toDateString()) {
      return d.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' });
    }
    return d.toLocaleDateString([], { month: 'short', day: 'numeric' });
  };

  const getNotePreview = (n: Note) => {
    if (n.isChecklist) {
      if (!n.checklist.length) return 'Empty checklist';
      const done = n.checklist.filter((i) => i.isDone).length;
      return `${done} of ${n.checklist.length} done`;
    }
    const clean = n.body.replace(/[#*`_~-]/g, '').replace(/\s+/g, ' ').trim();
    return clean.slice(0, 85) || 'No text yet';
  };

  return (
    <aside className="relative flex flex-col h-full overflow-hidden glass-panel rounded-3xl z-10">
      {/* Top Bar */}
      <div className="flex items-center justify-between px-4 pt-4 pb-2 gap-2">
        <div className="flex items-center gap-2.5 min-w-0">
          <AppGlassLogo size={32} />
          <h1 className="text-2xl font-bold tracking-tight truncate">{strings.appTitle}</h1>
        </div>

        <div className="flex items-center gap-1.5 flex-none">
          {/* Quick Capture */}
          <button
            onClick={onQuickCapture}
            title={strings.quickCapture}
            className="w-9 h-9 rounded-full glass-field flex items-center justify-center text-amber-500 hover:bg-amber-500/10 active:scale-95 transition-all"
          >
            <Zap size={18} className="fill-amber-500" />
          </button>

          {/* Quick Theme Toggle */}
          <button
            onClick={onToggleTheme}
            title="Switch theme"
            className="w-9 h-9 rounded-full glass-field flex items-center justify-center hover:opacity-85 active:scale-95 transition-all"
          >
            {isDark ? <Sun size={17} className="text-amber-400" /> : <Moon size={17} />}
          </button>

          {/* Settings */}
          <button
            onClick={onOpenSettings}
            title={strings.settingsTitle}
            className="w-9 h-9 rounded-full glass-field flex items-center justify-center hover:opacity-85 active:scale-95 transition-all"
          >
            <SettingsIcon size={17} />
          </button>
        </div>
      </div>

      {/* Search Bar */}
      <div className="px-4 py-1.5">
        <div className="flex items-center gap-2 px-3 h-11 rounded-2xl glass-field border border-white/30 focus-within:ring-2 focus-within:ring-blue-500 transition-all">
          <Search size={17} className="text-slate-400 flex-none" />
          <input
            type="search"
            value={searchQuery}
            onChange={(e) => onSearchQueryChange(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && searchQuery.trim()) {
                onCommitSearch(searchQuery.trim());
              }
            }}
            placeholder={strings.searchPlaceholder}
            className="flex-1 bg-transparent border-0 outline-none text-sm placeholder:text-slate-400 min-w-0"
          />
          {searchQuery && (
            <button
              onClick={() => onSearchQueryChange('')}
              className="text-slate-400 hover:text-slate-600 flex-none"
            >
              <X size={15} />
            </button>
          )}
          <button
            onClick={onToggleFilterRow}
            title={strings.tabFilters}
            className={`w-7 h-7 rounded-lg flex items-center justify-center transition-colors ${
              showFilterRow
                ? 'bg-blue-500 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-600'
            }`}
          >
            <SlidersHorizontal size={14} />
          </button>
        </div>
      </div>

      {/* Recent Searches Chips */}
      {recentSearches.length > 0 && !searchQuery && (
        <div className="flex items-center gap-1.5 px-4 py-1 overflow-x-auto scrollbar-none text-xs">
          <span className="text-slate-400 flex-none mr-0.5">{strings.recentTitle}:</span>
          {recentSearches.map((s, idx) => (
            <button
              key={idx}
              onClick={() => onSearchQueryChange(s)}
              className="flex-none px-2.5 py-1 rounded-full glass-card hover:border-blue-400 text-slate-600 dark:text-slate-300 transition-all text-xs max-w-[120px] truncate"
            >
              {s}
            </button>
          ))}
        </div>
      )}

      {/* Advanced Filters Expandable Bar */}
      {showFilterRow && (
        <div className="mx-4 mb-2 p-2.5 rounded-2xl glass-field flex flex-wrap gap-2 text-xs border border-white/20 animate-in fade-in slide-in-from-top-2">
          {/* Type Filter */}
          <select
            value={searchFilters.type}
            onChange={(e) => onUpdateFilters({ type: e.target.value as TypeFilter })}
            className="px-2.5 py-1.5 rounded-xl glass-card outline-none text-xs"
          >
            <option value="any">Type: any</option>
            <option value="text">Text</option>
            <option value="checklist">Checklist</option>
            <option value="voice">Voice</option>
            <option value="image">Image</option>
            <option value="drawing">Drawing</option>
            <option value="scan">Scan</option>
          </select>

          {/* Folder Filter */}
          <select
            value={searchFilters.folder}
            onChange={(e) => onUpdateFilters({ folder: e.target.value })}
            className="px-2.5 py-1.5 rounded-xl glass-card outline-none text-xs"
          >
            <option value="">Folder: any</option>
            {allFolders.map((f) => (
              <option key={f} value={f}>
                {f}
              </option>
            ))}
          </select>

          {/* Date Range Filter */}
          <select
            value={searchFilters.dateRange}
            onChange={(e) => onUpdateFilters({ dateRange: e.target.value as DateRangeFilter })}
            className="px-2.5 py-1.5 rounded-xl glass-card outline-none text-xs"
          >
            <option value="any">Date: any</option>
            <option value="1d">Last day</option>
            <option value="7d">Last 7 days</option>
            <option value="30d">Last 30 days</option>
          </select>

          {/* Date Mode Filter */}
          <select
            value={searchFilters.dateMode}
            onChange={(e) => onUpdateFilters({ dateMode: e.target.value as DateModeFilter })}
            className="px-2.5 py-1.5 rounded-xl glass-card outline-none text-xs"
          >
            <option value="modified">Modified</option>
            <option value="created">Created</option>
          </select>
        </div>
      )}

      {/* Filter Tabs */}
      <div className="flex gap-2 px-4 py-1.5 overflow-x-auto scrollbar-none flex-none">
        {(
          [
            { id: 'all', label: strings.tabAll },
            { id: 'pinned', label: strings.tabPinned },
            { id: 'checklists', label: strings.tabChecklists },
            { id: 'archive', label: strings.tabArchive },
            { id: 'trash', label: strings.tabTrash },
          ] as { id: FilterTab; label: string }[]
        ).map((tab) => {
          const isActive = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => onSelectTab(tab.id)}
              className={`flex-none px-3.5 py-1.5 rounded-full text-xs font-semibold transition-all ${
                isActive
                  ? 'bg-gradient-to-r from-blue-600 to-indigo-600 text-white shadow-sm scale-105'
                  : 'glass-field hover:opacity-100 opacity-75'
              }`}
            >
              {tab.label}
            </button>
          );
        })}
      </div>

      {/* Trash Banner if Trash Tab is Active */}
      {activeTab === 'trash' && (
        <div className="mx-4 my-2 px-3 py-2 rounded-2xl bg-red-500/10 border border-red-500/20 flex items-center justify-between text-xs text-red-500">
          <span>{strings.trashBanner}</span>
          <button
            onClick={onEmptyTrash}
            className="font-bold underline hover:text-red-600"
          >
            {strings.emptyTrashBtn}
          </button>
        </div>
      )}

      {/* Notes List */}
      <div className="flex-1 overflow-y-auto px-4 py-2 space-y-2 pb-24">
        {notes.length === 0 ? (
          <div className="py-16 text-center text-slate-400 text-sm">
            {searchQuery || activeTab !== 'all' ? 'Nothing matches yet.' : strings.emptyNotesHint}
          </div>
        ) : (
          notes.map((note) => {
            const isSelected = note.id === selectedNoteId;
            const colorConf = NOTE_COLORS[note.color] || NOTE_COLORS.blue;
            const preview = getNotePreview(note);

            return (
              <div
                key={note.id}
                onClick={() => onSelectNote(note.id)}
                className={`group relative w-full text-left p-3.5 pl-5 rounded-2xl glass-card cursor-pointer border transition-all ${
                  isSelected
                    ? 'border-blue-500 ring-1 ring-blue-500 shadow-md'
                    : 'border-white/30 hover:border-blue-400/60 hover:-translate-y-0.5'
                }`}
              >
                {/* Left Colored Accent Stripe */}
                <div
                  style={{
                    background: `linear-gradient(to bottom, ${colorConf.startColor}, ${colorConf.endColor})`,
                  }}
                  className="absolute left-0 top-3.5 bottom-3.5 w-1.5 rounded-r-md"
                />

                {/* Title and Pin */}
                <div className="flex items-center justify-between gap-2 min-w-0">
                  <h4 className="font-bold text-sm truncate flex-1">
                    {note.title || strings.untitledNote}
                  </h4>
                  <div className="flex items-center gap-1.5 flex-none">
                    {note.isPinned && (
                      <Pin size={13} className="text-blue-500 fill-blue-500 flex-none" />
                    )}
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        onOpenContextMenu(note);
                      }}
                      className="w-6 h-6 rounded-full flex items-center justify-center text-slate-400 hover:text-slate-600 hover:bg-black/5 dark:hover:bg-white/5 opacity-0 group-hover:opacity-100 transition-opacity"
                    >
                      <MoreVertical size={14} />
                    </button>
                  </div>
                </div>

                {/* Body Preview */}
                <p
                  className={`text-xs text-slate-500 dark:text-slate-400 line-clamp-2 mt-1 ${
                    hidePreviews ? 'privacy-blur' : ''
                  }`}
                >
                  {preview}
                </p>

                {/* Badges / Meta */}
                <div className="flex items-center gap-2 mt-2 text-[11px] text-slate-400">
                  <span>{formatTime(note.updatedAt)}</span>
                  {note.isChecklist && (
                    <span className="flex items-center gap-0.5 px-1.5 py-0.5 rounded-md glass-field text-[10px] font-semibold text-blue-500">
                      <CheckSquare size={10} />
                      {strings.checklistTag}
                    </span>
                  )}
                  {note.attachments.some((a) => a.type === 'voice') && (
                    <Volume2 size={11} className="text-blue-400" />
                  )}
                  {note.attachments.some((a) => a.type === 'image' || a.type === 'scan') && (
                    <ImageIcon size={11} className="text-indigo-400" />
                  )}
                  {note.attachments.some((a) => a.type === 'drawing') && (
                    <PenTool size={11} className="text-purple-400" />
                  )}
                  {note.reminderAt && (
                    <Clock size={11} className="text-amber-400" />
                  )}
                  {note.folder && (
                    <span className="flex items-center gap-0.5 text-[10px] text-slate-500 dark:text-slate-300 ml-auto truncate max-w-[90px]">
                      <Folder size={10} />
                      {note.folder}
                    </span>
                  )}
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* Floating Action Button (New Note) */}
      <button
        onClick={onCreateNote}
        title={strings.newNote}
        className="absolute right-5 bottom-5 w-14 h-14 rounded-2xl bg-gradient-to-tr from-blue-600 via-indigo-600 to-purple-600 text-white flex items-center justify-center shadow-xl shadow-blue-500/30 hover:scale-105 active:scale-95 transition-all group z-20"
      >
        <Plus size={26} className="group-hover:rotate-90 transition-transform duration-300" />
      </button>
    </aside>
  );
};
