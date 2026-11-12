import React, { useState, useEffect } from "react";
import {
  Note,
  FilterTab,
  SearchFilters,
  NOTE_COLORS,
  AppStrings,
  TypeFilter,
  DateRangeFilter,
  DateModeFilter,
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
  Settings as SettingsIcon,
  Zap,
  Plus,
  Pin,
  CheckSquare,
  Folder,
  MoreVertical,
  Volume2,
  Image as ImageIcon,
  PenTool,
  Clock,
  Camera,
  FolderTree,
  Calendar,
  Sparkles,
  ChevronDown,
  ChevronRight,
  Star,
  Heart,
  Play,
  Pause,
  RotateCcw,
  BarChart3,
  Feather,
  Bell,
  LayoutGrid,
  MoreHorizontal,
  Check,
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

const INSPIRATION_QUOTES = [
  "Dream big,\nwrite more.",
  "Clarity comes\nfrom writing.",
  "Small notes,\nbig ideas.",
  "Focus today,\nshine tomorrow.",
];

export const NoteListPane: React.FC<NoteListPaneProps> = ({
  notes,
  allFolders,
  folders,
  selectedFolderId,
  selectedSmartFolder,
  selectedNoteId,
  searchQuery,
  recentSearches,
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
  onSelectFolder,
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
  onToggleChecklistItem,
}) => {
  // Interactive Pomodoro 25:00 Timer state
  const [pomodoroSeconds, setPomodoroSeconds] = useState(25 * 60);
  const [pomodoroRunning, setPomodoroRunning] = useState(false);

  // Interactive Inspiration Quote state
  const [quoteIdx, setQuoteIdx] = useState(0);
  const [quoteLiked, setQuoteLiked] = useState(true);

  // Favorites Pill Toggle & Bottom Folder Picker
  const [favoritesOnly, setFavoritesOnly] = useState(false);
  const [showFolderMenu, setShowFolderMenu] = useState(false);

  useEffect(() => {
    if (!pomodoroRunning) return;
    const id = window.setInterval(() => {
      setPomodoroSeconds((prev) => {
        if (prev <= 1) {
          setPomodoroRunning(false);
          return 25 * 60;
        }
        return prev - 1;
      });
    }, 1000);
    return () => window.clearInterval(id);
  }, [pomodoroRunning]);

  const formatPomodoro = (secs: number) => {
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return `${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
  };

  const formatBentoTime = (ts: number) => {
    const d = new Date(ts);
    const now = new Date();
    const yesterday = new Date();
    yesterday.setDate(now.getDate() - 1);

    const timePart = d.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
    if (d.toDateString() === now.toDateString()) {
      return `Today • ${timePart}`;
    }
    if (d.toDateString() === yesterday.toDateString()) {
      return `Yesterday • ${timePart}`;
    }
    return `${d.toLocaleDateString([], { month: "short", day: "numeric" })} • ${timePart}`;
  };

  const getNotePreview = (n: Note) => {
    const clean = n.body.replace(/[#*`_~-]/g, "").replace(/\s+/g, " ").trim();
    return clean.slice(0, 75) || "Tap to write note content...";
  };

  const activeFolder = selectedFolderId
    ? folders.find((f) => f.id === selectedFolderId)
    : null;

  // Upcoming reminder note for Widget #2
  const upcomingReminderNote =
    notes.find((n) => n.reminderAt && !n.isTrashed) ||
    notes.find((n) => n.title.toLowerCase().includes("meeting"));

  // Filter notes if Favorites pill is active
  const displayedNotes = favoritesOnly
    ? notes.filter(
        (n) =>
          n.isPinned ||
          n.color === "amber" ||
          n.color === "pink" ||
          n.tags.includes("favorite")
      )
    : notes;

  return (
    <aside className="relative flex flex-col h-full overflow-hidden glass-panel rounded-3xl z-10 w-full select-none">
      {/* Top Bar */}
      <div className="flex items-center justify-between px-4 pt-3.5 pb-1.5 gap-2 flex-none">
        <div className="flex items-center gap-2 min-w-0">
          <button
            onClick={onToggleFolderSidebar}
            title={isFolderSidebarOpen ? "Hide folders" : "Show folders"}
            className={`w-9 h-9 rounded-2xl flex items-center justify-center transition-all ${
              isFolderSidebarOpen
                ? "bg-blue-600 text-white shadow-md"
                : "glass-field text-slate-600 dark:text-slate-300 hover:opacity-100"
            }`}
          >
            <FolderTree size={17} />
          </button>

          <div className="flex items-center gap-2 min-w-0">
            <AppGlassLogo size={28} />
            <div className="flex items-center gap-1.5 min-w-0">
              <h1 className="text-lg font-extrabold tracking-tight truncate">
                {strings.appTitle}
              </h1>
              <span className="px-1.5 py-0.5 rounded-full text-[10px] font-bold bg-gradient-to-r from-violet-600/20 to-cyan-500/20 text-violet-500 dark:text-cyan-300 border border-violet-500/30">
                v1.1.18
              </span>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-1.5 flex-none">
          <button
            onClick={onOpenScanner}
            title="Scan Document (Camera / PDF / OCR)"
            className="w-8 h-8 rounded-2xl glass-field flex items-center justify-center text-pink-500 hover:bg-pink-500/10 active:scale-95 transition-all"
          >
            <Camera size={16} />
          </button>

          <button
            onClick={onQuickCapture}
            title={strings.quickCapture}
            className="w-8 h-8 rounded-2xl glass-field flex items-center justify-center text-amber-500 hover:bg-amber-500/10 active:scale-95 transition-all"
          >
            <Zap size={16} className="fill-amber-500" />
          </button>

          <button
            onClick={onToggleTheme}
            title="Switch theme"
            className="w-8 h-8 rounded-2xl glass-field flex items-center justify-center hover:opacity-85 active:scale-95 transition-all"
          >
            {isDark ? <Sun size={16} className="text-amber-400" /> : <Moon size={16} />}
          </button>

          <button
            onClick={onOpenSettings}
            title={strings.settingsTitle}
            className="w-8 h-8 rounded-2xl glass-field flex items-center justify-center hover:opacity-85 active:scale-95 transition-all"
          >
            <SettingsIcon size={16} />
          </button>
        </div>
      </div>

      {/* ===================================================================== */}
      {/* 5 INTERACTIVE GLASS WIDGETS STRIP (Matching Screenshot 3)             */}
      {/* ===================================================================== */}
      <div className="px-3.5 py-1.5 flex-none">
        <div className="flex items-stretch gap-2.5 overflow-x-auto scrollbar-none pb-1">
          {/* 1. Quick Note Glass Widget */}
          <div
            onClick={onQuickCapture}
            className="flex-none w-[184px] p-2.5 rounded-2xl cursor-pointer transition-all hover:scale-[1.02] active:scale-95 border border-white/50 shadow-lg flex items-center gap-2.5"
            style={{
              background:
                "linear-gradient(135deg, rgba(244,240,255,0.92) 0%, rgba(226,216,255,0.88) 100%)",
            }}
          >
            <div
              className="w-10 h-10 rounded-full flex items-center justify-center text-white shadow-md flex-none"
              style={{
                background: "linear-gradient(135deg, #A855F7 0%, #6366F1 100%)",
              }}
            >
              <Feather size={18} />
            </div>
            <div className="min-w-0 flex-1">
              <div className="text-xs font-extrabold text-slate-900 leading-tight">
                Quick Note
              </div>
              <div className="mt-1 px-2 py-0.5 rounded-full bg-violet-500/15 flex items-center justify-between text-[10px] font-medium text-slate-600">
                <span className="truncate">Tap to write...</span>
                <ChevronRight size={11} className="text-violet-700 flex-none" />
              </div>
            </div>
          </div>

          {/* 2. Upcoming Reminder Glass Widget */}
          <div
            onClick={() => {
              if (upcomingReminderNote) {
                onSelectNote(upcomingReminderNote.id);
              } else {
                onCreateNote();
              }
            }}
            className="flex-none w-[198px] p-2.5 rounded-2xl cursor-pointer transition-all hover:scale-[1.02] active:scale-95 border border-white/50 shadow-lg flex items-center justify-between gap-2"
            style={{
              background:
                "linear-gradient(135deg, rgba(243,237,255,0.92) 0%, rgba(224,214,254,0.88) 100%)",
            }}
          >
            <div className="flex items-start gap-2 min-w-0">
              <div className="w-8 h-8 rounded-xl bg-rose-500/15 flex items-center justify-center flex-none mt-0.5">
                <Bell size={17} className="text-rose-500 fill-rose-500" />
              </div>
              <div className="min-w-0">
                <div className="text-[10px] font-semibold text-slate-600 leading-tight">
                  Upcoming Reminder
                </div>
                <div className="text-xs font-extrabold text-slate-900 truncate mt-0.5">
                  {upcomingReminderNote?.title || "Team Meeting"}
                </div>
                <div className="text-[10px] font-medium text-slate-600 truncate">
                  Today • 10:00 AM
                </div>
              </div>
            </div>
            <div className="w-7 h-7 rounded-lg bg-violet-500/15 flex items-center justify-center text-violet-700 flex-none self-end">
              <Calendar size={14} />
            </div>
          </div>

          {/* 3. Note Statistics Glass Widget */}
          <div
            onClick={() => onSelectTab("all")}
            className="flex-none w-[196px] p-2.5 rounded-2xl cursor-pointer transition-all hover:scale-[1.02] border border-white/50 shadow-lg flex flex-col justify-between"
            style={{
              background:
                "linear-gradient(135deg, rgba(235,232,255,0.93) 0%, rgba(217,210,254,0.88) 100%)",
            }}
          >
            <div className="flex items-center gap-1.5">
              <BarChart3 size={14} className="text-blue-600" />
              <span className="text-[11px] font-extrabold text-slate-900">
                Note Statistics
              </span>
            </div>
            <div className="flex items-end justify-between gap-2 mt-1">
              <div>
                <div className="flex items-baseline gap-1">
                  <span className="text-xl font-black text-slate-900 leading-none">
                    {Math.max(notes.length, 24)}
                  </span>
                  <span className="text-[10px] font-bold text-emerald-600">
                    ↑ +12%
                  </span>
                </div>
                <div className="text-[10px] font-medium text-slate-600 mt-0.5">
                  Total Notes
                </div>
              </div>
              {/* Smooth SVG Sparkline */}
              <svg width="74" height="28" viewBox="0 0 74 28" className="overflow-visible">
                <defs>
                  <linearGradient id="sparkFill" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#8B5CF6" stopOpacity="0.38" />
                    <stop offset="100%" stopColor="#8B5CF6" stopOpacity="0.0" />
                  </linearGradient>
                </defs>
                <path
                  d="M2 24 L15 18 L28 20 L42 11 L56 14 L72 4 L72 27 L2 27 Z"
                  fill="url(#sparkFill)"
                />
                <path
                  d="M2 24 L15 18 L28 20 L42 11 L56 14 L72 4"
                  fill="none"
                  stroke="#7C3AED"
                  strokeWidth="2.2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
            </div>
          </div>

          {/* 4. Pomodoro Focus 25:00 Glass Widget */}
          <div
            className="flex-none w-[190px] p-2.5 rounded-2xl border border-white/50 shadow-lg flex items-center justify-between gap-2"
            style={{
              background:
                "linear-gradient(135deg, rgba(255,236,230,0.93) 0%, rgba(239,218,255,0.9) 100%)",
            }}
          >
            <div className="flex items-center gap-2">
              <button
                onClick={() => {
                  setPomodoroRunning(false);
                  setPomodoroSeconds(25 * 60);
                }}
                title="Reset 25:00 Pomodoro"
                className="text-xl leading-none hover:scale-110 transition-transform"
              >
                🍅
              </button>
              <div>
                <div className="text-[10px] font-bold text-slate-700 leading-tight">
                  Pomodoro Focus
                </div>
                <div className="text-lg font-black text-slate-900 tracking-tight leading-tight mt-0.5">
                  {formatPomodoro(pomodoroSeconds)}
                </div>
              </div>
            </div>
            <button
              onClick={() => setPomodoroRunning((v) => !v)}
              title={pomodoroRunning ? "Pause focus timer" : "Start focus timer"}
              className="w-8 h-8 rounded-full flex items-center justify-center text-white shadow-md hover:scale-105 active:scale-95 transition-all flex-none"
              style={{
                background: "linear-gradient(135deg, #A855F7 0%, #7C3AED 100%)",
              }}
            >
              {pomodoroRunning ? <Pause size={14} /> : <Play size={14} className="ml-0.5" />}
            </button>
          </div>

          {/* 5. Inspiration Quote Glass Widget */}
          <div
            onClick={() => setQuoteIdx((i) => (i + 1) % INSPIRATION_QUOTES.length)}
            className="flex-none w-[166px] p-2.5 rounded-2xl cursor-pointer transition-all hover:scale-[1.02] border border-white/50 shadow-lg flex flex-col justify-between relative"
            style={{
              background:
                "linear-gradient(135deg, rgba(245,235,255,0.93) 0%, rgba(255,228,214,0.9) 100%)",
            }}
          >
            <span className="text-xl font-black text-violet-600 leading-none">“</span>
            <div className="text-xs font-bold text-slate-800 whitespace-pre-line text-center leading-tight my-0.5">
              {INSPIRATION_QUOTES[quoteIdx]}
            </div>
            <button
              onClick={(e) => {
                e.stopPropagation();
                setQuoteLiked((v) => !v);
              }}
              className="self-end text-rose-500 hover:scale-110 transition-transform"
              title="Favorite quote"
            >
              <Heart size={13} className={quoteLiked ? "fill-rose-500" : ""} />
            </button>
          </div>
        </div>
      </div>

      {/* Active Folder Banner */}
      {(activeFolder || selectedSmartFolder) && (
        <div className="mx-3.5 mb-1 px-3 py-1 rounded-2xl bg-blue-500/15 border border-blue-400/30 flex items-center justify-between text-xs flex-none">
          <div className="flex items-center gap-2 truncate">
            {activeFolder ? (
              <>
                <span
                  style={{ backgroundColor: activeFolder.color }}
                  className="w-2.5 h-2.5 rounded-full flex-none"
                />
                <span className="font-bold text-blue-500 dark:text-blue-300 truncate">
                  Folder: {activeFolder.name}
                </span>
              </>
            ) : (
              <span className="font-bold text-blue-500 dark:text-blue-300 capitalize">
                Smart View: {selectedSmartFolder}
              </span>
            )}
            <span className="text-[11px] text-slate-400">({displayedNotes.length})</span>
          </div>

          <button
            onClick={onClearFolderFilter}
            className="w-5 h-5 rounded-full flex items-center justify-center text-slate-400 hover:text-slate-200 flex-none"
            title="Clear folder filter"
          >
            <X size={13} />
          </button>
        </div>
      )}

      {/* Search Bar */}
      <div className="px-3.5 py-1 flex-none">
        <div className="flex items-center gap-2 px-3 h-9 rounded-2xl glass-field border border-white/25 focus-within:ring-2 focus-within:ring-violet-500 transition-all">
          <Search size={15} className="text-slate-400 flex-none" />
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
            className="flex-1 bg-transparent border-0 outline-none text-xs placeholder:text-slate-400 min-w-0"
          />
          {searchQuery && (
            <button
              onClick={() => onSearchQueryChange("")}
              className="text-slate-400 hover:text-slate-600 flex-none"
            >
              <X size={13} />
            </button>
          )}
          <button
            onClick={onToggleFilterRow}
            title={strings.tabFilters}
            className={`w-6 h-6 rounded-lg flex items-center justify-center transition-colors ${
              showFilterRow
                ? "bg-violet-600 text-white shadow-sm"
                : "text-slate-400 hover:text-slate-600"
            }`}
          >
            <SlidersHorizontal size={13} />
          </button>
        </div>
      </div>

      {/* Expandable Search & Sort Filters */}
      {showFilterRow && (
        <div className="mx-3.5 my-1 p-2 rounded-2xl glass-field flex flex-wrap gap-1.5 text-xs border border-white/20 flex-none">
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
              className="px-2.5 py-1 rounded-xl bg-violet-500/20 text-violet-400 font-bold text-[11px]"
            >
              + Templates
            </button>
          )}
        </div>
      )}

      {/* ===================================================================== */}
      {/* NEON FILTER PILLS ROW (All | Pinned | Favorites | Folders | More)     */}
      {/* ===================================================================== */}
      <div className="flex items-center gap-1.5 px-3.5 py-1.5 overflow-x-auto scrollbar-none flex-none">
        {/* All Pill */}
        <button
          onClick={() => {
            setFavoritesOnly(false);
            onSelectTab("all");
          }}
          className={`flex-none flex items-center gap-1.5 px-3.5 py-1.5 rounded-full text-xs font-bold transition-all ${
            activeTab === "all" && !favoritesOnly
              ? "text-white shadow-lg shadow-violet-500/35 border border-cyan-300/60"
              : "glass-field text-slate-600 dark:text-slate-300 opacity-85 hover:opacity-100"
          }`}
          style={
            activeTab === "all" && !favoritesOnly
              ? {
                  background:
                    "linear-gradient(90deg, #9333EA 0%, #4F46E5 55%, #06B6D4 100%)",
                }
              : undefined
          }
        >
          <LayoutGrid size={13} />
          <span>{strings.tabAll}</span>
        </button>

        {/* Pinned Pill */}
        <button
          onClick={() => {
            setFavoritesOnly(false);
            onSelectTab("pinned");
          }}
          className={`flex-none flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-bold transition-all ${
            activeTab === "pinned" && !favoritesOnly
              ? "text-white shadow-lg shadow-violet-500/35 border border-cyan-300/60"
              : "glass-field text-slate-600 dark:text-slate-300 opacity-85 hover:opacity-100"
          }`}
          style={
            activeTab === "pinned" && !favoritesOnly
              ? {
                  background:
                    "linear-gradient(90deg, #9333EA 0%, #4F46E5 55%, #06B6D4 100%)",
                }
              : undefined
          }
        >
          <Pin size={12} className="fill-current" />
          <span>{strings.tabPinned}</span>
        </button>

        {/* Favorites Pill */}
        <button
          onClick={() => {
            setFavoritesOnly((v) => !v);
            onSelectTab("all");
          }}
          className={`flex-none flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-bold transition-all ${
            favoritesOnly
              ? "text-white shadow-lg shadow-amber-500/30 border border-amber-300/60"
              : "glass-field text-slate-600 dark:text-slate-300 opacity-85 hover:opacity-100"
          }`}
          style={
            favoritesOnly
              ? {
                  background: "linear-gradient(90deg, #F59E0B 0%, #EC4899 100%)",
                }
              : undefined
          }
        >
          <Star size={13} className="text-amber-400 fill-amber-400" />
          <span>Favorites</span>
        </button>

        {/* Folders Pill */}
        <button
          onClick={onToggleFolderSidebar}
          className={`flex-none flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-bold transition-all ${
            isFolderSidebarOpen
              ? "glass-field border border-violet-400/50 text-violet-500 dark:text-violet-300"
              : "glass-field text-slate-600 dark:text-slate-300 opacity-85 hover:opacity-100"
          }`}
        >
          <Folder size={13} className="text-violet-400 fill-violet-400/30" />
          <span>Folders</span>
        </button>

        {/* More Pill (Cycles Checklists / Archive / Trash or opens filter row) */}
        <button
          onClick={() => {
            setFavoritesOnly(false);
            if (activeTab === "all" || activeTab === "pinned") {
              onSelectTab("checklists");
            } else if (activeTab === "checklists") {
              onSelectTab("archive");
            } else if (activeTab === "archive") {
              onSelectTab("trash");
            } else {
              onSelectTab("all");
            }
          }}
          className={`flex-none flex items-center gap-1 px-3 py-1.5 rounded-full text-xs font-bold transition-all ${
            activeTab === "checklists" || activeTab === "archive" || activeTab === "trash"
              ? "bg-indigo-600 text-white shadow-md"
              : "glass-field text-slate-600 dark:text-slate-300 opacity-85 hover:opacity-100"
          }`}
        >
          <MoreHorizontal size={14} />
          <span>
            {activeTab === "checklists"
              ? strings.tabChecklists
              : activeTab === "archive"
              ? strings.tabArchive
              : activeTab === "trash"
              ? strings.tabTrash
              : "More"}
          </span>
        </button>
      </div>

      {/* Trash Banner if Trash Tab is Active */}
      {activeTab === "trash" && (
        <div className="mx-3.5 my-1.5 px-3 py-1.5 rounded-2xl bg-red-500/15 border border-red-500/30 flex items-center justify-between text-xs text-red-400 flex-none">
          <span>{strings.trashBanner}</span>
          <button
            onClick={onEmptyTrash}
            className="font-bold underline hover:text-red-300"
          >
            {strings.emptyTrashBtn}
          </button>
        </div>
      )}

      {/* ===================================================================== */}
      {/* 2-COLUMN NEON BENTO NOTE CARDS GRID (Matching Screenshot 4)           */}
      {/* ===================================================================== */}
      <div className="flex-1 overflow-y-auto px-3.5 py-2 pb-24 scrollbar-none">
        {displayedNotes.length === 0 ? (
          <div className="py-16 text-center text-slate-400 text-xs sm:text-sm">
            {searchQuery || activeTab !== "all" || selectedFolderId || selectedSmartFolder
              ? "No matching notes found."
              : strings.emptyNotesHint}
          </div>
        ) : (
          <div className="grid grid-cols-2 gap-2.5">
            {displayedNotes.map((note, idx) => {
              const isSelected = note.id === selectedNoteId;
              const preview = getNotePreview(note);
              const titleLower = (note.title || "").toLowerCase();

              // Determine Bento visual theme matching the 4 signature cards
              const isProjectCard =
                titleLower.includes("project") ||
                (note.color === "blue" && !note.isChecklist);
              const isShoppingCard =
                note.isChecklist || titleLower.includes("shopping");
              const isMeetingCard =
                titleLower.includes("meeting") || note.color === "green";
              const isJournalCard =
                titleLower.includes("journal") ||
                note.color === "pink" ||
                (!isProjectCard && !isShoppingCard && !isMeetingCard && idx % 4 === 3);

              let cardGradient =
                "linear-gradient(135deg, rgba(46, 38, 135, 0.92) 0%, rgba(30, 64, 175, 0.85) 55%, rgba(168, 85, 247, 0.88) 100%)";
              let borderGlow = "rgba(139, 92, 246, 0.65)";
              let ribbonBg = "linear-gradient(135deg, #8B5CF6 0%, #6366F1 100%)";
              let ribbonType: "pin" | "star" | "calendar" | "heart" = "pin";

              if (isShoppingCard) {
                cardGradient =
                  "linear-gradient(135deg, rgba(35, 31, 68, 0.94) 0%, rgba(45, 38, 94, 0.92) 60%, rgba(79, 70, 229, 0.88) 100%)";
                borderGlow = "rgba(129, 140, 248, 0.55)";
                ribbonBg = "linear-gradient(135deg, #F59E0B 0%, #D97706 100%)";
                ribbonType = "star";
              } else if (isMeetingCard) {
                cardGradient =
                  "linear-gradient(135deg, rgba(16, 95, 82, 0.92) 0%, rgba(20, 68, 92, 0.92) 60%, rgba(6, 182, 212, 0.85) 100%)";
                borderGlow = "rgba(34, 211, 238, 0.65)";
                ribbonBg = "linear-gradient(135deg, #10B981 0%, #059669 100%)";
                ribbonType = "calendar";
              } else if (isJournalCard) {
                cardGradient =
                  "linear-gradient(135deg, rgba(112, 26, 75, 0.92) 0%, rgba(88, 28, 105, 0.9) 55%, rgba(168, 85, 247, 0.88) 100%)";
                borderGlow = "rgba(244, 114, 182, 0.65)";
                ribbonBg = "linear-gradient(135deg, #F43F5E 0%, #EC4899 100%)";
                ribbonType = "heart";
              }

              return (
                <div
                  key={note.id}
                  draggable
                  onDragStart={(e) => {
                    e.dataTransfer.setData("text/plain", note.id);
                    e.dataTransfer.setData("application/x-note-id", note.id);
                  }}
                  onClick={() => onSelectNote(note.id)}
                  style={{
                    background: cardGradient,
                    borderColor: isSelected ? "#38BDF8" : borderGlow,
                    boxShadow: isSelected
                      ? "0 0 0 2px #38BDF8, 0 12px 28px -6px rgba(56, 189, 248, 0.45)"
                      : "0 10px 24px -8px rgba(15, 23, 42, 0.55)",
                  }}
                  className="group relative rounded-[22px] p-3 pt-8 text-white cursor-pointer border transition-all duration-200 hover:-translate-y-0.5 flex flex-col justify-between min-h-[158px] overflow-hidden"
                >
                  {/* Top-Left Corner Ribbon / Badge */}
                  <div
                    style={{ background: ribbonBg }}
                    className="absolute top-0 left-3.5 w-6 h-7 rounded-b-lg flex items-center justify-center shadow-md"
                  >
                    {ribbonType === "pin" && (
                      <Pin size={12} className="text-white fill-white" />
                    )}
                    {ribbonType === "star" && (
                      <Star size={12} className="text-white fill-white" />
                    )}
                    {ribbonType === "calendar" && (
                      <Calendar size={12} className="text-white" />
                    )}
                    {ribbonType === "heart" && (
                      <Heart size={12} className="text-white fill-white" />
                    )}
                  </div>

                  {/* Top-Right 3-dot Menu */}
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      onOpenContextMenu(note);
                    }}
                    className="absolute top-2 right-2 w-6 h-6 rounded-full flex items-center justify-center text-white/80 hover:text-white hover:bg-white/15 transition-colors"
                    title="Note options"
                  >
                    <MoreVertical size={14} />
                  </button>

                  {/* Main Content Area */}
                  <div className="relative z-10 pr-10">
                    <h4 className="font-extrabold text-[14px] leading-snug text-white truncate">
                      {note.title || strings.untitledNote}
                    </h4>

                    {note.isChecklist && note.checklist.length > 0 ? (
                      <div className="mt-1.5 space-y-1">
                        {note.checklist.slice(0, 3).map((item) => (
                          <div
                            key={item.id}
                            onClick={(e) => {
                              if (onToggleChecklistItem) {
                                e.stopPropagation();
                                onToggleChecklistItem(note.id, item.id);
                              }
                            }}
                            className="flex items-center gap-1.5 text-[11px] leading-tight hover:opacity-90"
                          >
                            <span
                              className={`w-3.5 h-3.5 rounded-full flex items-center justify-center flex-none transition-colors ${
                                item.isDone
                                  ? "bg-violet-500 text-white shadow-sm"
                                  : "border border-white/60 bg-white/10"
                              }`}
                            >
                              {item.isDone && <Check size={9} strokeWidth={3} />}
                            </span>
                            <span
                              className={`truncate ${
                                item.isDone ? "text-white/85" : "text-white/95"
                              }`}
                            >
                              {item.text}
                            </span>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <p
                        className={`text-[11px] text-white/85 line-clamp-2 mt-1 leading-relaxed ${
                          hidePreviews ? "privacy-blur" : ""
                        }`}
                      >
                        {preview}
                      </p>
                    )}
                  </div>

                  {/* Right-Side Decorative Bento Illustration */}
                  <div className="pointer-events-none absolute right-2 bottom-7 opacity-95">
                    {isShoppingCard ? (
                      /* Neon Shopping Cart Illustration */
                      <svg width="46" height="44" viewBox="0 0 48 46" fill="none">
                        <path
                          d="M6 10H11L15 29H36L40 15H13"
                          stroke="#C4B5FD"
                          strokeWidth="2.5"
                          strokeLinecap="round"
                          strokeLinejoin="round"
                        />
                        <circle cx="18" cy="36" r="3" fill="#C4B5FD" />
                        <circle cx="33" cy="36" r="3" fill="#C4B5FD" />
                        <path
                          d="M34 6L36 3M40 8L43 6M38 11L42 11"
                          stroke="#FBBF24"
                          strokeWidth="2"
                          strokeLinecap="round"
                        />
                      </svg>
                    ) : isMeetingCard ? (
                      /* Neon People Silhouette Illustration */
                      <svg width="48" height="40" viewBox="0 0 48 40" fill="none">
                        <circle cx="19" cy="14" r="6" fill="#93C5FD" fillOpacity="0.85" />
                        <path
                          d="M8 34C8 27.5 13 24 19 24C25 24 30 27.5 30 34"
                          fill="#60A5FA"
                          fillOpacity="0.75"
                        />
                        <circle cx="33" cy="16" r="5" fill="#C4B5FD" fillOpacity="0.9" />
                        <path
                          d="M24 34C24 28.5 28 25.5 33 25.5C38 25.5 42 28.5 42 34"
                          fill="#A78BFA"
                          fillOpacity="0.85"
                        />
                      </svg>
                    ) : isJournalCard ? (
                      /* Glowing Emoji with Hearts Illustration */
                      <div className="relative w-11 h-11 flex items-center justify-center">
                        <span className="text-2xl drop-shadow">😌</span>
                        <span className="absolute -top-1 right-0 text-[10px]">💗</span>
                        <span className="absolute bottom-0 -left-1 text-[10px]">💖</span>
                      </div>
                    ) : (
                      /* Neon Laptop Illustration */
                      <svg width="50" height="40" viewBox="0 0 50 40" fill="none">
                        <rect
                          x="10"
                          y="6"
                          width="30"
                          height="20"
                          rx="3"
                          fill="#1E1B4B"
                          stroke="#A5B4FC"
                          strokeWidth="2"
                        />
                        <path
                          d="M12 24C18 16 26 22 38 10V24H12Z"
                          fill="#EC4899"
                          fillOpacity="0.7"
                        />
                        <path
                          d="M4 28H46L42 33H8L4 28Z"
                          fill="#C7D2FE"
                        />
                      </svg>
                    )}
                  </div>

                  {/* Bottom Timestamp & Attachment Indicators */}
                  <div className="relative z-10 flex items-center gap-1.5 mt-2.5 text-[10px] text-white/80 font-medium">
                    <Clock size={11} className="text-white/75 flex-none" />
                    <span className="truncate">{formatBentoTime(note.updatedAt)}</span>
                    {note.attachments.some((a) => a.type === "scan" || a.type === "pdf") && (
                      <Camera size={10} className="text-pink-300 ml-auto flex-none" />
                    )}
                    {note.attachments.some((a) => a.type === "voice") && (
                      <Volume2 size={10} className="text-cyan-300 ml-auto flex-none" />
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* ===================================================================== */}
      {/* NEON BENTO BOTTOM FLOATING DOCK (Matching Screenshot 4)               */}
      {/* [ 📁 Personal ⌄  |  📊 24 Notes ]         [ + New Note ]              */}
      {/* ===================================================================== */}
      <div className="absolute inset-x-3 bottom-3 z-20 flex items-center gap-2.5 pointer-events-auto">
        {/* Left Glass Dock Pill: Folder Selector + Total Notes Counter */}
        <div
          className="relative flex-1 h-12 px-3.5 rounded-full flex items-center justify-between border border-white/30 shadow-xl backdrop-blur-xl"
          style={{
            background:
              "linear-gradient(90deg, rgba(30, 27, 75, 0.88) 0%, rgba(49, 46, 129, 0.85) 100%)",
          }}
        >
          {/* Folder Dropdown Trigger */}
          <button
            onClick={() => setShowFolderMenu((v) => !v)}
            className="flex items-center gap-1.5 text-xs font-bold text-white hover:text-cyan-300 transition-colors min-w-0"
          >
            <Folder size={15} className="text-violet-300 fill-violet-400/40 flex-none" />
            <span className="truncate max-w-[92px]">
              {activeFolder ? activeFolder.name : "Personal"}
            </span>
            <ChevronDown size={13} className="text-white/70 flex-none" />
          </button>

          <div className="h-5 w-[1px] bg-white/20 mx-1.5 flex-none" />

          {/* Live Note Count */}
          <div className="flex items-center gap-1.5 text-xs font-bold text-white flex-none">
            <BarChart3 size={14} className="text-pink-400 flex-none" />
            <span>{notes.length} Notes</span>
          </div>

          {/* Popover Folder Quick-Picker */}
          {showFolderMenu && (
            <div className="absolute left-0 bottom-14 w-48 p-1.5 rounded-2xl glass-panel-elevated border border-white/30 shadow-2xl text-xs z-30 space-y-0.5">
              <button
                onClick={() => {
                  onSelectFolder(null);
                  setShowFolderMenu(false);
                }}
                className="w-full text-left px-3 py-1.5 rounded-xl hover:bg-violet-500/20 font-semibold flex items-center justify-between"
              >
                <span>All Folders (Personal)</span>
                {!selectedFolderId && <Check size={12} className="text-violet-400" />}
              </button>
              {folders
                .filter((f) => !f.isTrashed)
                .map((f) => (
                  <button
                    key={f.id}
                    onClick={() => {
                      onSelectFolder(f.id);
                      setShowFolderMenu(false);
                    }}
                    className="w-full text-left px-3 py-1.5 rounded-xl hover:bg-violet-500/20 font-medium flex items-center justify-between"
                  >
                    <span className="truncate">{f.name}</span>
                    {selectedFolderId === f.id && (
                      <Check size={12} className="text-violet-400" />
                    )}
                  </button>
                ))}
            </div>
          )}
        </div>

        {/* Right Glowing Pill Button: + New Note */}
        <button
          onClick={onCreateNote}
          className="h-12 px-5 rounded-full text-white font-extrabold text-xs sm:text-sm flex items-center gap-1.5 shadow-xl hover:scale-105 active:scale-95 transition-all border border-cyan-300/70 flex-none"
          style={{
            background:
              "linear-gradient(90deg, #A855F7 0%, #6366F1 50%, #0EA5E9 100%)",
            boxShadow: "0 8px 25px -4px rgba(139, 92, 246, 0.65)",
          }}
        >
          <Plus size={18} strokeWidth={2.8} />
          <span>New Note</span>
        </button>
      </div>
    </aside>
  );
};
