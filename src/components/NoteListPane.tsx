import React, { useState } from "react";
import {
  Note,
  FilterTab,
  SearchFilters,
  NOTE_COLORS,
  AppStrings,
  TypeFilter,
  FolderItem,
  NoteSortOption,
} from "../types/note";
import { AppGlassLogo } from "./AppGlassLogo";
import {
  Search,
  X,
  SlidersHorizontal,
  Sun,
  Moon,
  Zap,
  Plus,
  Pin,
  Home,
  CheckSquare,
  Layers,
  Hexagon,
  Bell,
} from "lucide-react";

interface NoteListPaneProps {
  notes: Note[];
  recentNotes: Note[];
  allFolders: string[];
  folders: FolderItem[];
  selectedFolderId: string | null;
  selectedSmartFolder: string | null;
  selectedNoteId: string | null;
  searchQuery: string;
  recentSearches: string[];
  activeTab: FilterTab;
  showFilterRow: boolean;
  searchFilters: SearchFilters;
  sortBy: NoteSortOption;
  hidePreviews: boolean;
  isDark: boolean;
  strings: AppStrings;
  isFolderSidebarOpen: boolean;
  onSearchQueryChange: (query: string) => void;
  onCommitSearch: (query: string) => void;
  onSelectTab: (tab: FilterTab) => void;
  onToggleFilterRow: () => void;
  onUpdateFilters: (filters: Partial<SearchFilters>) => void;
  onSortByChange: (sort: NoteSortOption) => void;
  onSelectFolder: (folderId: string | null) => void;
  onSelectSmartFolder: (smartId: string) => void;
  onClearFolderFilter: () => void;
  onToggleFolderSidebar: () => void;
  onSelectNote: (noteId: string) => void;
  onCreateNote: () => void;
  onOpenTemplates?: () => void;
  onOpenScanner: () => void;
  onQuickCapture: () => void;
  onToggleTheme: () => void;
  onOpenSettings: () => void;
  onOpenContextMenu: (note: Note) => void;
  onEmptyTrash: () => void;
  onToggleChecklistItem?: (noteId: string, itemId: string) => void;
}

export const NoteListPane: React.FC<NoteListPaneProps> = ({
  notes,
  allFolders,
  folders,
  selectedFolderId,
  selectedSmartFolder,
  selectedNoteId,
  searchQuery,
  activeTab,
  showFilterRow,
  searchFilters,
  sortBy,
  hidePreviews,
  isDark,
  strings,
  isFolderSidebarOpen,
  onSearchQueryChange,
  onCommitSearch,
  onSelectTab,
  onToggleFilterRow,
  onUpdateFilters,
  onSortByChange,
  onClearFolderFilter,
  onToggleFolderSidebar,
  onSelectNote,
  onCreateNote,
  onOpenTemplates,
  onOpenScanner,
  onQuickCapture,
  onToggleTheme,
  onOpenSettings,
  onOpenContextMenu,
  onEmptyTrash,
}) => {
  const [recentSearchList] = useState<string[]>([]);
  void recentSearchList;

  const formatCleanDate = (ts: number) => {
    const d = new Date(ts);
    const now = new Date();
    if (d.toDateString() === now.toDateString()) {
      return d.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
    }
    return d.toLocaleDateString([], { month: "short", day: "numeric" });
  };

  const getNotePreview = (n: Note) => {
    if (n.isChecklist && n.checklist.length > 0) {
      const done = n.checklist.filter((c) => c.isDone).length;
      return `${done} of ${n.checklist.length} done`;
    }
    const clean = n.body.replace(/\n+/g, " ").replace(/\s+/g, " ").trim();
    return clean || "No additional text";
  };

  const getBarColor = (colorId: Note["color"]) => {
    const found = NOTE_COLORS.find((c) => c.id === colorId);
    return found ? found.dot : "#3B82F6";
  };

  const activeFolder = selectedFolderId
    ? folders.find((f) => f.id === selectedFolderId)
    : null;

  const activeDockSection =
    activeTab === "checklists"
      ? "checklists"
      : activeTab === "archive" || activeTab === "trash"
      ? "vault"
      : "home";

  return (
    <aside className="relative flex flex-col h-full overflow-hidden glass-panel rounded-3xl z-10 w-full select-none">
      {/* Clean Minimal Header: Logo + "Notes" + Daily Quote on left; Quick Capture & Theme on right */}
      <div className="flex items-center justify-between px-4 pt-3.5 pb-2.5 gap-3 flex-none">
        <div className="flex items-center gap-3 min-w-0">
          <AppGlassLogo size={38} />
          <div className="min-w-0">
            <h1 className="text-xl font-bold tracking-tight truncate leading-tight">
              {strings.appTitle}
            </h1>
            <p className="text-[11px] opacity-65 truncate">“Start where you are.”</p>
          </div>
        </div>

        <div className="flex items-center gap-2 flex-none">
          <button
            onClick={onQuickCapture}
            title={strings.quickCapture}
            className="w-9 h-9 rounded-full glass-field flex items-center justify-center hover:opacity-90 active:scale-95 transition-all"
          >
            <Zap size={16} />
          </button>

          <button
            onClick={onToggleTheme}
            title="Switch theme"
            className="w-9 h-9 rounded-full glass-field flex items-center justify-center hover:opacity-90 active:scale-95 transition-all"
          >
            {isDark ? <Sun size={16} /> : <Moon size={16} />}
          </button>
        </div>
      </div>

      {/* Rounded Search Pill */}
      <div className="px-4 py-1 flex-none">
        <div className="flex items-center gap-2.5 px-3.5 h-10 rounded-2xl glass-field border border-white/20 focus-within:ring-2 focus-within:ring-indigo-400 transition-all">
          <Search size={16} className="opacity-60 flex-none" />
          <input
            type="search"
            value={searchQuery}
            onChange={(e) => onSearchQueryChange(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter" && searchQuery.trim()) {
                onCommitSearch(searchQuery.trim());
              }
            }}
            placeholder={strings.searchPlaceholder}
            className="flex-1 bg-transparent border-0 outline-none text-sm placeholder:opacity-60 min-w-0"
          />
          {searchQuery && (
            <button
              onClick={() => onSearchQueryChange("")}
              className="opacity-60 hover:opacity-100 flex-none"
            >
              <X size={14} />
            </button>
          )}
          <button
            onClick={onToggleFilterRow}
            title={strings.tabFilters}
            className={`w-7 h-7 rounded-full flex items-center justify-center transition-colors ${
              showFilterRow
                ? "bg-indigo-500 text-white"
                : "opacity-70 hover:opacity-100"
            }`}
          >
            <SlidersHorizontal size={14} />
          </button>
        </div>
      </div>

      {/* Expandable Search & Sort Filters */}
      {showFilterRow && (
        <div className="mx-4 my-1 p-2 rounded-2xl glass-field flex flex-wrap gap-1.5 text-xs border border-white/20 flex-none">
          <select
            value={searchFilters.type}
            onChange={(e) => onUpdateFilters({ type: e.target.value as TypeFilter })}
            className="px-2 py-1 rounded-xl glass-card outline-none text-[11px]"
          >
            <option value="any">Type: any</option>
            <option value="text">Text</option>
            <option value="checklist">Checklist</option>
            <option value="voice">Voice</option>
            <option value="image">Image</option>
            <option value="drawing">Drawing</option>
            <option value="scan">Scan</option>
          </select>

          <select
            value={searchFilters.folder}
            onChange={(e) => onUpdateFilters({ folder: e.target.value })}
            className="px-2 py-1 rounded-xl glass-card outline-none text-[11px]"
          >
            <option value="">Folder: any</option>
            {allFolders.map((f) => (
              <option key={f} value={f}>
                {f}
              </option>
            ))}
          </select>

          <select
            value={sortBy}
            onChange={(e) => onSortByChange(e.target.value as NoteSortOption)}
            className="px-2 py-1 rounded-xl glass-card outline-none text-[11px]"
          >
            <option value="updated">Sort: Modified</option>
            <option value="created">Sort: Created</option>
            <option value="nameAsc">Sort: A-Z</option>
            <option value="nameDesc">Sort: Z-A</option>
          </select>

          {onOpenTemplates && (
            <button
              onClick={onOpenTemplates}
              className="px-2.5 py-1 rounded-xl bg-indigo-500/20 text-indigo-300 font-semibold text-[11px]"
            >
              + Templates
            </button>
          )}
        </div>
      )}

      {/* Clean Horizontal Pill Bar */}
      <div className="flex items-center gap-2 px-4 py-2 overflow-x-auto scrollbar-none flex-none">
        <button
          onClick={() => onSelectTab("all")}
          className={`flex-none px-3.5 py-1.5 rounded-full text-xs font-semibold transition-all border ${
            activeTab === "all" && !selectedFolderId
              ? "bg-indigo-400/90 text-white border-indigo-300 shadow-sm"
              : "glass-field border-white/20 opacity-85 hover:opacity-100"
          }`}
        >
          {strings.tabAll}
        </button>

        <button
          onClick={() => onSelectTab("pinned")}
          className={`flex-none px-3.5 py-1.5 rounded-full text-xs font-semibold transition-all border ${
            activeTab === "pinned"
              ? "bg-indigo-400/90 text-white border-indigo-300 shadow-sm"
              : "glass-field border-white/20 opacity-85 hover:opacity-100"
          }`}
        >
          {strings.tabPinned}
        </button>

        <button
          onClick={onToggleFolderSidebar}
          className={`flex-none px-3.5 py-1.5 rounded-full text-xs font-semibold transition-all border ${
            isFolderSidebarOpen || selectedFolderId
              ? "bg-indigo-400/90 text-white border-indigo-300 shadow-sm"
              : "glass-field border-white/20 opacity-85 hover:opacity-100"
          }`}
        >
          Folders
        </button>

        <button
          onClick={onOpenScanner}
          className="flex-none px-3.5 py-1.5 rounded-full text-xs font-semibold glass-field border border-white/20 opacity-85 hover:opacity-100"
        >
          Scanner
        </button>

        <button
          onClick={() => onSelectTab("trash")}
          className={`flex-none px-3.5 py-1.5 rounded-full text-xs font-semibold transition-all border ${
            activeTab === "trash"
              ? "bg-indigo-400/90 text-white border-indigo-300 shadow-sm"
              : "glass-field border-white/20 opacity-85 hover:opacity-100"
          }`}
        >
          {strings.tabTrash}
        </button>
      </div>

      {/* Active Folder Pill */}
      {(activeFolder || selectedSmartFolder) && (
        <div className="mx-4 mb-1.5 px-3 py-1 rounded-xl glass-field border border-white/20 flex items-center justify-between text-xs flex-none">
          <span className="font-semibold truncate">
            {activeFolder ? activeFolder.name : selectedSmartFolder} ({notes.length})
          </span>
          <button onClick={onClearFolderFilter} className="opacity-70 hover:opacity-100">
            <X size={13} />
          </button>
        </div>
      )}

      {/* Trash Banner */}
      {activeTab === "trash" && (
        <div className="mx-4 my-1.5 px-3 py-1.5 rounded-2xl bg-red-500/15 border border-red-500/30 flex items-center justify-between text-xs text-red-400 flex-none">
          <span>{strings.trashBanner}</span>
          <button onClick={onEmptyTrash} className="font-bold underline hover:text-red-300">
            {strings.emptyTrashBtn}
          </button>
        </div>
      )}

      {/* Single-Column Clean Frosted Glass Note Rows */}
      <div className="flex-1 overflow-y-auto px-4 py-1.5 pb-24 scrollbar-none space-y-2.5">
        {notes.length === 0 ? (
          <div className="py-16 text-center opacity-60 text-sm">
            {searchQuery || activeTab !== "all" || selectedFolderId || selectedSmartFolder
              ? "Nothing matches yet."
              : strings.emptyNotesHint}
          </div>
        ) : (
          notes.map((note) => {
            const isSelected = note.id === selectedNoteId;
            const metaParts = [formatCleanDate(note.updatedAt)];
            if (note.isChecklist) metaParts.push("Checklist");
            if (note.folder) metaParts.push(note.folder);

            return (
              <div
                key={note.id}
                onClick={() => onSelectNote(note.id)}
                onContextMenu={(e) => {
                  e.preventDefault();
                  onOpenContextMenu(note);
                }}
                className={`rounded-2xl p-3 glass-card border transition-all cursor-pointer flex items-center gap-3 ${
                  isSelected
                    ? "border-sky-400 ring-1 ring-sky-400/50"
                    : "border-white/20 hover:border-white/35"
                }`}
              >
                <div
                  className="w-1 h-12 rounded-full flex-none"
                  style={{ backgroundColor: getBarColor(note.color) }}
                />
                <div className="min-w-0 flex-1">
                  <div className="flex items-center justify-between gap-2">
                    <h4 className="font-semibold text-sm truncate">
                      {note.title || strings.untitledNote}
                    </h4>
                    <div className="flex items-center gap-1.5 flex-none">
                      {note.reminderAt && (
                        <Bell size={12} className="text-amber-400" />
                      )}
                      {note.isPinned && (
                        <Pin size={13} className="text-sky-400 fill-sky-400" />
                      )}
                    </div>
                  </div>
                  <p
                    className={`text-xs opacity-70 truncate mt-0.5 ${
                      hidePreviews ? "privacy-blur" : ""
                    }`}
                  >
                    {getNotePreview(note)}
                  </p>
                  <p className="text-[11px] opacity-55 truncate mt-1">
                    {metaParts.join(" · ")}
                  </p>
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* Single Clean Floating Capsule Bottom Dock */}
      <div className="absolute inset-x-4 bottom-3 z-20 flex items-center justify-between gap-2.5 p-2 rounded-[32px] glass-field border border-white/20 backdrop-blur-xl">
        <div className="flex items-center gap-1 p-1 rounded-full bg-slate-900/90 border border-white/10">
          <button
            onClick={() => onSelectTab("all")}
            className={`h-10 rounded-full flex items-center justify-center gap-1.5 px-3.5 text-xs font-bold transition-all ${
              activeDockSection === "home"
                ? "bg-white text-slate-900 shadow"
                : "text-white/75 hover:text-white"
            }`}
          >
            <Home size={16} />
            {activeDockSection === "home" && <span>Home</span>}
          </button>
          <button
            onClick={() => onSelectTab("checklists")}
            className={`h-10 rounded-full flex items-center justify-center gap-1.5 px-3 text-xs font-bold transition-all ${
              activeDockSection === "checklists"
                ? "bg-white text-slate-900 shadow"
                : "text-white/75 hover:text-white"
            }`}
          >
            <CheckSquare size={16} />
            {activeDockSection === "checklists" && <span>{strings.tabChecklists}</span>}
          </button>
          <button
            onClick={() => onSelectTab("archive")}
            className={`h-10 rounded-full flex items-center justify-center gap-1.5 px-3 text-xs font-bold transition-all ${
              activeDockSection === "vault"
                ? "bg-white text-slate-900 shadow"
                : "text-white/75 hover:text-white"
            }`}
          >
            <Layers size={16} />
            {activeDockSection === "vault" && <span>{strings.tabArchive}</span>}
          </button>
          <button
            onClick={onOpenSettings}
            title={strings.settingsTitle}
            className="h-10 px-3 rounded-full flex items-center justify-center text-white/75 hover:text-white transition-all"
          >
            <Hexagon size={16} />
          </button>
        </div>

        <button
          onClick={onCreateNote}
          title={strings.newNote}
          className="w-12 h-12 rounded-full flex items-center justify-center text-white shadow-lg flex-none hover:scale-105 active:scale-95 transition-all"
          style={{
            background: "linear-gradient(135deg, #8B8DF8 0%, #6366F1 100%)",
          }}
        >
          <Plus size={22} strokeWidth={2.4} />
        </button>
      </div>
    </aside>
  );
};
