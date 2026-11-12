import React, { useState } from 'react';
import { FolderItem, Note } from '../types/note';
import {
  Folder,
  FolderPlus,
  Pin,
  Archive,
  Trash2,
  ChevronRight,
  ChevronDown,
  Edit2,
  RotateCcw,
  Plus,
  Search,
  FileText,
  CheckSquare,
  Clock,
  Image,
  Bell,
  Briefcase,
  Book,
  Code,
  Tag,
  Heart,
  Star,
  Layers,
  ArrowUpDown,
  MoreVertical,
} from 'lucide-react';

export const FOLDER_ICONS: Record<string, React.ElementType> = {
  folder: Folder,
  briefcase: Briefcase,
  book: Book,
  code: Code,
  tag: Tag,
  heart: Heart,
  star: Star,
  layers: Layers,
};

export const FOLDER_COLORS = [
  '#3B82F6', // Blue
  '#10B981', // Green
  '#F59E0B', // Amber
  '#EC4899', // Pink
  '#8B5CF6', // Purple
  '#06B6D4', // Cyan
  '#64748B', // Slate
];

interface FolderSidebarProps {
  folders: FolderItem[];
  notes: Note[];
  selectedFolderId: string | null;
  selectedSmartFolder: string | null;
  onSelectFolder: (folderId: string | null) => void;
  onSelectSmartFolder: (smartId: string) => void;
  onCreateFolder: (parentId?: string | null) => void;
  onEditFolder: (folder: FolderItem) => void;
  onDeleteFolder: (folderId: string) => void;
  onTogglePinFolder: (folderId: string) => void;
  onToggleArchiveFolder: (folderId: string) => void;
  onRestoreFolder: (folderId: string) => void;
  onMoveNoteToFolder?: (noteId: string, folderName: string) => void;
}

export const FolderSidebar: React.FC<FolderSidebarProps> = ({
  folders,
  notes,
  selectedFolderId,
  selectedSmartFolder,
  onSelectFolder,
  onSelectSmartFolder,
  onCreateFolder,
  onEditFolder,
  onDeleteFolder,
  onTogglePinFolder,
  onToggleArchiveFolder,
  onRestoreFolder,
  onMoveNoteToFolder,
}) => {
  const [expandedFolders, setExpandedFolders] = useState<Record<string, boolean>>({
    'f-product': true,
  });
  const [folderMenuId, setFolderMenuId] = useState<string | null>(null);
  const [showArchived, setShowArchived] = useState(false);
  const [showTrashed, setShowTrashed] = useState(false);
  const [folderSearch, setFolderSearch] = useState('');
  const [folderSortBy, setFolderSortBy] = useState<'name' | 'notesCount' | 'created'>('name');
  const [dragOverFolderId, setDragOverFolderId] = useState<string | null>(null);

  const toggleExpand = (folderId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    setExpandedFolders((prev) => ({ ...prev, [folderId]: !prev[folderId] }));
  };

  // Helper for folder note count
  const getFolderNoteCount = (folderName: string) => {
    return notes.filter((n) => !n.isTrashed && n.folder.toLowerCase() === folderName.toLowerCase()).length;
  };

  // Smart folder counts
  const smartCounts = {
    all: notes.filter((n) => !n.isTrashed && !n.isArchived).length,
    pinned: notes.filter((n) => !n.isTrashed && n.isPinned).length,
    checklists: notes.filter((n) => !n.isTrashed && n.isChecklist).length,
    recent: notes.filter((n) => !n.isTrashed && Date.now() - n.updatedAt < 7 * 86400000).length,
    scans: notes.filter(
      (n) => !n.isTrashed && n.attachments.some((a) => a.type === 'scan' || a.type === 'pdf')
    ).length,
    reminders: notes.filter((n) => !n.isTrashed && n.reminderAt && !n.reminderFired).length,
    trash: notes.filter((n) => n.isTrashed).length,
  };

  const SMART_FOLDERS = [
    { id: 'all', label: 'All Notes', icon: FileText, count: smartCounts.all, color: '#3B82F6' },
    { id: 'pinned', label: 'Pinned', icon: Pin, count: smartCounts.pinned, color: '#F59E0B' },
    { id: 'checklists', label: 'Checklists', icon: CheckSquare, count: smartCounts.checklists, color: '#10B981' },
    { id: 'recent', label: 'Recent (7 Days)', icon: Clock, count: smartCounts.recent, color: '#8B5CF6' },
    { id: 'scans', label: 'Scans & PDFs', icon: Image, count: smartCounts.scans, color: '#EC4899' },
    { id: 'reminders', label: 'Reminders', icon: Bell, count: smartCounts.reminders, color: '#06B6D4' },
  ];

  // Active user folders
  const activeFolders = folders.filter((f) => !f.isTrashed && !f.isArchived);
  const archivedFolders = folders.filter((f) => !f.isTrashed && f.isArchived);
  const trashedFolders = folders.filter((f) => f.isTrashed);

  // Sorting folders
  const sortedFolders = [...activeFolders].sort((a, b) => {
    // Pinned folders first
    if (a.isPinned !== b.isPinned) return a.isPinned ? -1 : 1;
    if (folderSortBy === 'notesCount') {
      return getFolderNoteCount(b.name) - getFolderNoteCount(a.name);
    }
    if (folderSortBy === 'created') {
      return b.createdAt - a.createdAt;
    }
    return a.name.localeCompare(b.name);
  });

  // Filtered by search if any
  const q = folderSearch.toLowerCase().trim();
  const searchMatched = q
    ? sortedFolders.filter((f) => f.name.toLowerCase().includes(q))
    : null;

  // Drag and drop handler for moving note onto folder
  const handleDragOver = (folderId: string, e: React.DragEvent) => {
    e.preventDefault();
    setDragOverFolderId(folderId);
  };

  const handleDragLeave = (folderId: string) => {
    if (dragOverFolderId === folderId) {
      setDragOverFolderId(null);
    }
  };

  const handleDrop = (folderName: string, e: React.DragEvent) => {
    e.preventDefault();
    setDragOverFolderId(null);
    const noteId = e.dataTransfer.getData('text/plain') || e.dataTransfer.getData('application/x-note-id');
    if (noteId && onMoveNoteToFolder) {
      onMoveNoteToFolder(noteId, folderName);
    }
  };

  // Render a folder node with nested children
  const renderFolderNode = (folder: FolderItem, depth = 0) => {
    const isSelected = selectedFolderId === folder.id && !selectedSmartFolder;
    const subfolders = sortedFolders.filter((f) => f.parentId === folder.id);
    const hasSubfolders = subfolders.length > 0;
    const isExpanded = !!expandedFolders[folder.id];
    const IconComponent = FOLDER_ICONS[folder.icon] || Folder;
    const noteCount = getFolderNoteCount(folder.name);
    const isDragTarget = dragOverFolderId === folder.id;

    return (
      <div key={folder.id} className="flex flex-col">
        <div
          onClick={() => onSelectFolder(folder.id)}
          onDragOver={(e) => handleDragOver(folder.id, e)}
          onDragLeave={() => handleDragLeave(folder.id)}
          onDrop={(e) => handleDrop(folder.name, e)}
          style={{ paddingLeft: `${10 + depth * 14}px` }}
          className={`group relative flex items-center justify-between pr-2 py-1.5 rounded-xl cursor-pointer text-xs font-medium transition-all ${
            isDragTarget
              ? 'ring-2 ring-blue-500 bg-blue-500/20 scale-[1.02]'
              : isSelected
              ? 'bg-blue-500/15 text-blue-600 dark:text-blue-400 font-bold border border-blue-500/30 shadow-sm'
              : 'hover:bg-black/5 dark:hover:bg-white/5 text-slate-700 dark:text-slate-300'
          }`}
        >
          <div className="flex items-center gap-2 min-w-0 flex-1">
            {hasSubfolders ? (
              <button
                onClick={(e) => toggleExpand(folder.id, e)}
                className="w-4 h-4 flex items-center justify-center text-slate-400 hover:text-slate-600"
              >
                {isExpanded ? <ChevronDown size={13} /> : <ChevronRight size={13} />}
              </button>
            ) : (
              <span className="w-4" />
            )}

            <div
              style={{ color: folder.color }}
              className="flex-none flex items-center justify-center"
            >
              <IconComponent size={15} />
            </div>

            <span className="truncate">{folder.name}</span>
          </div>

          <div className="flex items-center gap-1.5 flex-none">
            {folder.isPinned && <Pin size={11} className="text-amber-500 fill-amber-500" />}

            {/* Note Counter */}
            <span
              title={`${noteCount} notes in ${folder.name}`}
              className="px-1.5 py-0.5 rounded-full text-[10px] bg-black/5 dark:bg-white/10 text-slate-400 font-semibold"
            >
              {noteCount}
            </span>

            {/* Quick Context menu trigger */}
            <div className="relative">
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  setFolderMenuId(folderMenuId === folder.id ? null : folder.id);
                }}
                className="w-5 h-5 flex items-center justify-center opacity-0 group-hover:opacity-100 hover:bg-black/10 dark:hover:bg-white/10 rounded-md transition-opacity"
              >
                <MoreVertical size={12} />
              </button>

              {folderMenuId === folder.id && (
                <div
                  onClick={(e) => e.stopPropagation()}
                  className="absolute right-0 top-full mt-1 w-44 rounded-2xl glass-panel-elevated p-1.5 shadow-xl border border-white/30 z-30 flex flex-col gap-0.5 text-xs animate-in fade-in"
                >
                  <button
                    onClick={() => {
                      onCreateFolder(folder.id);
                      setFolderMenuId(null);
                    }}
                    className="flex items-center gap-2 px-2.5 py-1.5 rounded-xl hover:bg-black/5 dark:hover:bg-white/5 text-left font-medium"
                  >
                    <Plus size={13} />
                    <span>Add Subfolder</span>
                  </button>
                  <button
                    onClick={() => {
                      onEditFolder(folder);
                      setFolderMenuId(null);
                    }}
                    className="flex items-center gap-2 px-2.5 py-1.5 rounded-xl hover:bg-black/5 dark:hover:bg-white/5 text-left"
                  >
                    <Edit2 size={13} />
                    <span>Rename / Style</span>
                  </button>
                  <button
                    onClick={() => {
                      onTogglePinFolder(folder.id);
                      setFolderMenuId(null);
                    }}
                    className="flex items-center gap-2 px-2.5 py-1.5 rounded-xl hover:bg-black/5 dark:hover:bg-white/5 text-left"
                  >
                    <Pin size={13} className={folder.isPinned ? 'fill-amber-500 text-amber-500' : ''} />
                    <span>{folder.isPinned ? 'Unpin Folder' : 'Pin Folder'}</span>
                  </button>
                  <button
                    onClick={() => {
                      onToggleArchiveFolder(folder.id);
                      setFolderMenuId(null);
                    }}
                    className="flex items-center gap-2 px-2.5 py-1.5 rounded-xl hover:bg-black/5 dark:hover:bg-white/5 text-left"
                  >
                    <Archive size={13} />
                    <span>Archive Folder</span>
                  </button>
                  <button
                    onClick={() => {
                      onDeleteFolder(folder.id);
                      setFolderMenuId(null);
                    }}
                    className="flex items-center gap-2 px-2.5 py-1.5 rounded-xl hover:bg-red-500/10 text-red-500 text-left font-semibold"
                  >
                    <Trash2 size={13} />
                    <span>Delete Folder</span>
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Subfolders container */}
        {hasSubfolders && isExpanded && (
          <div className="flex flex-col">
            {subfolders.map((child) => renderFolderNode(child, depth + 1))}
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="flex flex-col h-full p-2.5 overflow-hidden">
      {/* Folder Search & Sort Bar */}
      <div className="flex items-center gap-1.5 mb-2">
        <div className="flex-1 flex items-center gap-1.5 px-2.5 py-1.5 rounded-xl glass-field border border-white/20 text-xs">
          <Search size={13} className="text-slate-400 flex-none" />
          <input
            type="text"
            value={folderSearch}
            onChange={(e) => setFolderSearch(e.target.value)}
            placeholder="Search folders..."
            className="bg-transparent outline-none w-full text-xs"
          />
          {folderSearch && (
            <button onClick={() => setFolderSearch('')} className="text-slate-400">
              ×
            </button>
          )}
        </div>

        {/* Folder Sorting selector */}
        <button
          onClick={() => {
            setFolderSortBy((prev) =>
              prev === 'name' ? 'notesCount' : prev === 'notesCount' ? 'created' : 'name'
            );
          }}
          title={`Sort Folders: ${folderSortBy}`}
          className="p-1.5 rounded-xl glass-field hover:bg-black/5 dark:hover:bg-white/5 text-slate-500 flex-none flex items-center justify-center text-[10px] font-bold"
        >
          <ArrowUpDown size={13} />
        </button>
      </div>

      <div className="flex-1 overflow-y-auto space-y-3 pr-1 scrollbar-none text-xs">
        {/* Smart Folders Section */}
        <div>
          <div className="flex items-center justify-between px-2 mb-1 text-[11px] font-bold text-slate-400 uppercase tracking-wider">
            <span>Smart Folders</span>
          </div>
          <div className="space-y-0.5">
            {SMART_FOLDERS.map((sf) => {
              const isSelected = selectedSmartFolder === sf.id;
              const Icon = sf.icon;
              return (
                <button
                  key={sf.id}
                  onClick={() => onSelectSmartFolder(sf.id)}
                  className={`w-full flex items-center justify-between px-2.5 py-1.5 rounded-xl text-left transition-all ${
                    isSelected
                      ? 'bg-blue-500/15 text-blue-600 dark:text-blue-400 font-bold border border-blue-500/30 shadow-sm'
                      : 'hover:bg-black/5 dark:hover:bg-white/5 text-slate-700 dark:text-slate-300'
                  }`}
                >
                  <div className="flex items-center gap-2 truncate">
                    <Icon size={14} style={{ color: sf.color }} />
                    <span className="truncate">{sf.label}</span>
                  </div>
                  <span className="px-1.5 py-0.5 rounded-full text-[10px] bg-black/5 dark:bg-white/10 text-slate-400 font-semibold">
                    {sf.count}
                  </span>
                </button>
              );
            })}
          </div>
        </div>

        {/* User Folders Section */}
        <div>
          <div className="flex items-center justify-between px-2 mb-1 text-[11px] font-bold text-slate-400 uppercase tracking-wider">
            <span>Folders ({activeFolders.length})</span>
            <button
              onClick={() => onCreateFolder(null)}
              title="New Folder"
              className="p-1 rounded-md text-blue-500 hover:bg-blue-500/10"
            >
              <Plus size={13} />
            </button>
          </div>

          {searchMatched ? (
            <div className="space-y-0.5">
              {searchMatched.length === 0 ? (
                <p className="px-2 py-2 text-slate-400 text-[11px]">No matching folders</p>
              ) : (
                searchMatched.map((f) => renderFolderNode(f, 0))
              )}
            </div>
          ) : (
            <div className="space-y-0.5">
              {/* Root-level folders (parentId === null) */}
              {sortedFolders
                .filter((f) => !f.parentId)
                .map((rootFolder) => renderFolderNode(rootFolder, 0))}
            </div>
          )}
        </div>

        {/* Archived Folders Toggle */}
        {archivedFolders.length > 0 && (          <div className="pt-1 border-t border-white/15">
            <button
              onClick={() => setShowArchived((v) => !v)}
              className="w-full flex items-center justify-between px-2 py-1 text-[11px] font-semibold text-slate-400 hover:text-slate-600"
            >
              <div className="flex items-center gap-1.5">
                <Archive size={12} />
                <span>Archived Folders ({archivedFolders.length})</span>
              </div>
              {showArchived ? <ChevronDown size={12} /> : <ChevronRight size={12} />}
            </button>
            {showArchived && (
              <div className="space-y-1 mt-1 pl-2">
                {archivedFolders.map((f) => (
                  <div
                    key={f.id}
                    className="flex items-center justify-between p-1.5 rounded-xl glass-card text-xs"
                  >
                    <span className="truncate">{f.name}</span>
                    <button
                      onClick={() => onToggleArchiveFolder(f.id)}
                      title="Restore folder"
                      className="text-blue-500 hover:underline text-[11px]"
                    >
                      Unarchive
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Recently Deleted Folders Toggle (Restore Deleted Folders) */}
        {trashedFolders.length > 0 && (
          <div className="pt-1 border-t border-white/15">
            <button
              onClick={() => setShowTrashed((v) => !v)}
              className="w-full flex items-center justify-between px-2 py-1 text-[11px] font-semibold text-red-400 hover:text-red-500"
            >
              <div className="flex items-center gap-1.5">
                <Trash2 size={12} />
                <span>Deleted Folders ({trashedFolders.length})</span>
              </div>
              {showTrashed ? <ChevronDown size={12} /> : <ChevronRight size={12} />}
            </button>
            {showTrashed && (
              <div className="space-y-1 mt-1 pl-2">
                {trashedFolders.map((f) => (
                  <div
                    key={f.id}
                    className="flex items-center justify-between p-1.5 rounded-xl bg-red-500/10 text-xs"
                  >
                    <span className="truncate text-red-500">{f.name}</span>
                    <button
                      onClick={() => onRestoreFolder(f.id)}
                      title="Restore deleted folder"
                      className="flex items-center gap-1 text-blue-500 hover:underline text-[11px] font-semibold"
                    >
                      <RotateCcw size={11} />
                      <span>Restore</span>
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>

      {/* New Folder Bottom Button */}
      <button
        onClick={() => onCreateFolder(null)}
        className="mt-2 flex items-center justify-center gap-1.5 py-2 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 text-white text-xs font-semibold shadow hover:brightness-110 active:scale-95 transition-all"
      >
        <FolderPlus size={14} />
        <span>New Folder</span>
      </button>
    </div>
  );
};
