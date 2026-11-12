import React, { useState, useEffect, useMemo, useRef } from 'react';
import {
  Note,
  AppSettings,
  FilterTab,
  SearchFilters,
  NoteColorId,
  ChecklistItem,
  NoteVersion,
  FolderItem,
  NoteSortOption,
  AttachmentType,
  NoteAttachment,
  TRANSLATIONS,
} from './types/note';
import {
  loadNotes,
  saveNotes,
  loadSettings,
  saveSettings,
  loadRecentSearches,
  saveRecentSearches,
  loadFolders,
  saveFolders,
  calculateStorageBytes,
  exportFullBackupJson,
  importFullBackupJson,
  loadSoundSettings,
  saveSoundSettings,
} from './services/storage';
import { SoundSettings, DEFAULT_SOUND_SETTINGS } from './types/sound';
import { SoundSettingsDialog } from './components/SoundSettingsDialog';
import { playReminderAudio } from './utils/audio';
import { playNotificationChime } from './utils/audio';

import { GlassBackground } from './components/GlassBackground';
import { DynamicIslandSavePill } from './components/DynamicIslandSavePill';
import { UndoToastBar } from './components/UndoToastBar';
import { FolderSidebar } from './components/FolderSidebar';
import { FolderManagerModal } from './components/FolderManagerModal';
import { DocumentScannerModal } from './components/DocumentScannerModal';
import { NoteListPane } from './components/NoteListPane';
import { NoteEditorPane } from './components/NoteEditorPane';
import { SettingsScreen } from './components/SettingsScreen';
import { DrawingCanvasDialog } from './components/DrawingCanvasDialog';
import { VoiceRecorderDialog } from './components/VoiceRecorderDialog';
import { PhotoCropDialog } from './components/PhotoCropDialog';
import { ReminderPickerDialog } from './components/ReminderPickerDialog';
import { HistoryDialog } from './components/HistoryDialog';
import { QuickCaptureDialog } from './components/QuickCaptureDialog';
import { NoteContextSheet } from './components/NoteContextSheet';
import { TemplatesModal } from './components/TemplatesModal';
import { NoteTemplate } from './types/templates';

export const App: React.FC = () => {
  const [notes, setNotes] = useState<Note[]>(() => loadNotes());
  const [folders, setFolders] = useState<FolderItem[]>(() => loadFolders());
  const [settings, setSettings] = useState<AppSettings>(() => loadSettings());
  const [recentSearches, setRecentSearches] = useState<string[]>(() => loadRecentSearches());

  const [selectedNoteId, setSelectedNoteId] = useState<string | null>(() => {
    const list = loadNotes();
    const activeNotes = list.filter((n) => !n.isTrashed && !n.isArchived);
    return activeNotes.length > 0 ? activeNotes[0].id : null;
  });

  // Folder & Organization state
  const [selectedFolderId, setSelectedFolderId] = useState<string | null>(null);
  const [selectedSmartFolder, setSelectedSmartFolder] = useState<string | null>(null);
  const [sortBy, setSortBy] = useState<NoteSortOption>('updated');
  const [isFolderSidebarOpen, setIsFolderSidebarOpen] = useState(true);

  // Folder Manager Modal state
  const [folderModalData, setFolderModalData] = useState<{
    isOpen: boolean;
    existingFolder?: FolderItem | null;
    parentId?: string | null;
  }>({
    isOpen: false,
    existingFolder: null,
    parentId: null,
  });

  // Document Scanner Modal state
  const [isScannerOpen, setIsScannerOpen] = useState(false);

  const [searchQuery, setSearchQuery] = useState('');
  const [activeTab, setActiveTab] = useState<FilterTab>('all');
  const [showFilterRow, setShowFilterRow] = useState(false);
  const [searchFilters, setSearchFilters] = useState<SearchFilters>({
    type: 'any',
    folder: '',
    dateRange: 'any',
    dateMode: 'modified',
  });

  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const [isEditorOpenOnMobile, setIsEditorOpenOnMobile] = useState(false);
  const [isMarkdownPreview, setIsMarkdownPreview] = useState(false);

  // Indicators & Toasts
  const [showSavedPill, setShowSavedPill] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [undoCallback, setUndoCallback] = useState<(() => void) | null>(null);

  // Modal Dialogs
  const [contextMenuNote, setContextMenuNote] = useState<Note | null>(null);
  const [quickCaptureOpen, setIsQuickCaptureOpen] = useState(false);
  const [isVoiceRecorderOpen, setIsVoiceRecorderOpen] = useState(false);
  const [isDrawingCanvasOpen, setIsDrawingCanvasOpen] = useState(false);
  const [photoCropBase64, setPhotoCropBase64] = useState<string | null>(null);
  const [isReminderPickerOpen, setIsReminderPickerOpen] = useState(false);
  const [isHistoryOpen, setIsHistoryOpen] = useState(false);
  const [isTemplatesModalOpen, setIsTemplatesModalOpen] = useState(false);
  const [soundSettings, setSoundSettings] = useState<SoundSettings>(() => loadSoundSettings());
  const [isSoundSettingsOpen, setIsSoundSettingsOpen] = useState(false);

  const saveTimerRef = useRef<number | null>(null);

  const strings = TRANSLATIONS[settings.language] || TRANSLATIONS.en;
  const isKhmer = settings.language === 'km';

  // Apply Theme & Font to DOM root
  useEffect(() => {
    const root = document.documentElement;

    // Theme: system | light | dark
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    const isDark =
      settings.theme === 'dark' || (settings.theme === 'system' && prefersDark);

    if (isDark) {
      root.setAttribute('data-theme', 'dark');
    } else {
      root.setAttribute('data-theme', 'light');
    }

    // Font family
    if (settings.font === 'serif') {
      root.style.setProperty('--font-family', "'Newsreader', Georgia, serif");
    } else if (settings.font === 'mono') {
      root.style.setProperty('--font-family', "'JetBrains Mono', monospace");
    } else {
      root.style.setProperty('--font-family', "'Figtree', system-ui, sans-serif");
    }

    // Font scaling
    root.style.setProperty('--font-scale', String(settings.fontScale));
  }, [settings.theme, settings.font, settings.fontScale]);

  // Persist Settings
  const updateSettings = (updater: (prev: AppSettings) => AppSettings) => {
    setSettings((prev) => {
      const next = updater(prev);
      saveSettings(next);
      return next;
    });
  };

  // Toast Helper
  const showToast = (msg: string, onUndo?: () => void) => {
    setToastMessage(msg);
    setUndoCallback(() => onUndo || null);
    setTimeout(() => {
      setToastMessage((current) => (current === msg ? null : current));
    }, 4500);
  };

  // Save notes helper with debounced trigger for "Saved" pill
  const persistNotes = (nextNotes: Note[]) => {
    setNotes(nextNotes);
    saveNotes(nextNotes);

    if (saveTimerRef.current) clearTimeout(saveTimerRef.current);
    saveTimerRef.current = window.setTimeout(() => {
      setShowSavedPill(true);
      setTimeout(() => setShowSavedPill(false), 1600);
    }, 350);
  };

  // Persist Folders helper
  const persistFolders = (nextFolders: FolderItem[]) => {
    setFolders(nextFolders);
    saveFolders(nextFolders);
  };

  // Auto-snapshot versions every snapshotIntervalSec
  useEffect(() => {
    const intervalMs = Math.max(10, settings.snapshotIntervalSec) * 1000;
    const timer = setInterval(() => {
      setNotes((currentNotes) => {
        let changed = false;
        const now = Date.now();
        const updated = currentNotes.map((note) => {
          if (note.isTrashed || (!note.title.trim() && !note.body.trim())) return note;
          const lastSnapshotTime = note.versions[0]?.timestamp || 0;
          if (now - note.updatedAt < intervalMs && now - lastSnapshotTime > intervalMs) {
            changed = true;
            const newVer: NoteVersion = {
              id: `v_${now}_${Math.random().toString(36).slice(2, 6)}`,
              timestamp: now,
              title: note.title,
              body: note.body,
              checklist: [...note.checklist],
              isChecklist: note.isChecklist,
            };
            return {
              ...note,
              versions: [newVer, ...note.versions.slice(0, 9)],
            };
          }
          return note;
        });

        if (changed) {
          saveNotes(updated);
          return updated;
        }
        return currentNotes;
      });
    }, intervalMs);

    return () => clearInterval(timer);
  }, [settings.snapshotIntervalSec]);

  // Reminder Checker (runs every 2 seconds)
  useEffect(() => {
    const interval = setInterval(() => {
      const now = Date.now();
      setNotes((currentNotes) => {
        let hasFired = false;
        const updated = currentNotes.map((n) => {
          if (n.reminderAt && n.reminderAt <= now && !n.reminderFired && !n.isTrashed) {
            hasFired = true;
            playReminderAudio(n.reminderSoundId || soundSettings.selectedSoundId, soundSettings);
            showToast(`Reminder: ${n.title || strings.untitledNote}`);
            return { ...n, reminderFired: true };
          }
          return n;
        });

        if (hasFired) {
          saveNotes(updated);
          return updated;
        }
        return currentNotes;
      });
    }, 2000);

    return () => clearInterval(interval);
  }, [strings]);

  // Active folder object lookup
  const activeFolderObj = useMemo(() => {
    return folders.find((f) => f.id === selectedFolderId) || null;
  }, [folders, selectedFolderId]);

  // Derived filtered & sorted notes
  const filteredNotes = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    const now = Date.now();

    return notes
      .filter((n) => {
        // Tab filtering
        if (activeTab === 'trash') {
          if (!n.isTrashed) return false;
        } else {
          if (n.isTrashed) return false;
          if (activeTab === 'archive') {
            if (!n.isArchived) return false;
          } else {
            if (n.isArchived) return false;
            if (activeTab === 'pinned' && !n.isPinned) return false;
            if (activeTab === 'checklists' && !n.isChecklist) return false;
          }
        }

        // Selected Folder filtering
        if (activeFolderObj) {
          // Match note.folder with folder name or any of its subfolders
          const matchSubfolders = folders
            .filter((f) => f.parentId === activeFolderObj.id)
            .map((f) => f.name.toLowerCase());
          const allowedNames = [activeFolderObj.name.toLowerCase(), ...matchSubfolders];
          if (!allowedNames.includes(n.folder.toLowerCase())) {
            return false;
          }
        }

        // Smart Folder filtering
        if (selectedSmartFolder) {
          if (selectedSmartFolder === 'pinned' && !n.isPinned) return false;
          if (selectedSmartFolder === 'checklists' && !n.isChecklist) return false;
          if (selectedSmartFolder === 'recent' && now - n.updatedAt > 7 * 86400000) return false;
          if (
            selectedSmartFolder === 'scans' &&
            !n.attachments.some((a) => a.type === 'scan' || a.type === 'pdf')
          )
            return false;
          if (selectedSmartFolder === 'reminders' && (!n.reminderAt || n.reminderFired)) return false;
        }

        // Advanced filter - type
        if (searchFilters.type !== 'any') {
          if (searchFilters.type === 'text' && n.isChecklist) return false;
          if (searchFilters.type === 'checklist' && !n.isChecklist) return false;
          if (
            ['voice', 'image', 'drawing', 'scan'].includes(searchFilters.type) &&
            !n.attachments.some((a) => a.type === searchFilters.type)
          ) {
            return false;
          }
        }

        // Advanced filter - folder
        if (searchFilters.folder && n.folder !== searchFilters.folder) {
          return false;
        }

        // Advanced filter - date range
        if (searchFilters.dateRange !== 'any') {
          const targetDate =
            searchFilters.dateMode === 'created' ? n.createdAt : n.updatedAt;
          const diffMs = now - targetDate;
          if (searchFilters.dateRange === '1d' && diffMs > 24 * 3600_000) return false;
          if (searchFilters.dateRange === '7d' && diffMs > 7 * 24 * 3600_000) return false;
          if (searchFilters.dateRange === '30d' && diffMs > 30 * 24 * 3600_000) return false;
        }

        // Search text matching
        if (q) {
          const matchTitle = n.title.toLowerCase().includes(q);
          const matchBody = n.body.toLowerCase().includes(q);
          const matchChecklist = n.checklist.some((c) => c.text.toLowerCase().includes(q));
          const matchFolder = n.folder.toLowerCase().includes(q);
          const matchTags = n.tags.some((t) => t.toLowerCase().includes(q));
          if (!matchTitle && !matchBody && !matchChecklist && !matchFolder && !matchTags) {
            return false;
          }
        }

        return true;
      })
      .sort((a, b) => {
        // Keep pinned notes prioritized
        if (a.isPinned !== b.isPinned) return a.isPinned ? -1 : 1;

        if (sortBy === 'created') {
          return b.createdAt - a.createdAt;
        }
        if (sortBy === 'nameAsc') {
          return (a.title || 'Untitled').localeCompare(b.title || 'Untitled');
        }
        if (sortBy === 'nameDesc') {
          return (b.title || 'Untitled').localeCompare(a.title || 'Untitled');
        }
        // Default: recently modified
        return b.updatedAt - a.updatedAt;
      });
  }, [notes, searchQuery, activeTab, selectedFolderId, selectedSmartFolder, activeFolderObj, folders, searchFilters, sortBy]);

  // All distinct folder names across notes
  const allFolderNames = useMemo(() => {
    const set = new Set<string>();
    folders.forEach((f) => {
      if (!f.isTrashed) set.add(f.name);
    });
    notes.forEach((n) => {
      if (n.folder.trim()) set.add(n.folder.trim());
    });
    return Array.from(set).sort();
  }, [folders, notes]);

  // Currently selected note
  const selectedNote = useMemo(() => {
    return notes.find((n) => n.id === selectedNoteId) || null;
  }, [notes, selectedNoteId]);

  // Note CRUD actions
  const handleCreateNote = (targetFolderName?: string) => {
    const now = Date.now();
    const folderToUse = targetFolderName || activeFolderObj?.name || '';

    const newNote: Note = {
      id: `n_${now}_${Math.random().toString(36).slice(2, 6)}`,
      title: '',
      body: '',
      isChecklist: settings.defaultNoteType === 'checklist',
      checklist: settings.defaultNoteType === 'checklist' ? [{ id: `c_${now}`, text: '', isDone: false }] : [],
      color: settings.accent,
      isPinned: false,
      isArchived: false,
      isTrashed: false,
      tags: [],
      folder: folderToUse,
      attachments: [],
      versions: [],
      activityLog: [{ id: `l_${now}`, timestamp: now, action: 'created' }],
      createdAt: now,
      updatedAt: now,
    };

    persistNotes([newNote, ...notes]);
    setSelectedNoteId(newNote.id);
    setIsEditorOpenOnMobile(true);
  };

  const handleQuickCapture = (text: string) => {
    const now = Date.now();
    const lines = text.split('\n');
    const title = lines[0].slice(0, 50);
    const body = lines.slice(1).join('\n').trim();

    const newNote: Note = {
      id: `n_${now}_${Math.random().toString(36).slice(2, 6)}`,
      title,
      body: body || text,
      isChecklist: false,
      checklist: [],
      color: settings.accent,
      isPinned: false,
      isArchived: false,
      isTrashed: false,
      tags: ['quick-capture'],
      folder: activeFolderObj?.name || '',
      attachments: [],
      versions: [],
      activityLog: [{ id: `l_${now}`, timestamp: now, action: 'created' }],
      createdAt: now,
      updatedAt: now,
    };

    persistNotes([newNote, ...notes]);
    setSelectedNoteId(newNote.id);
    showToast('Quick note saved');
  };

  const handleUpdateNoteContent = (
    title: string,
    body: string,
    isChecklist: boolean,
    checklist: ChecklistItem[],
    tags: string[],
    folder: string,
    color: NoteColorId,
    isPinned: boolean
  ) => {
    if (!selectedNoteId) return;
    const now = Date.now();

    const updated = notes.map((n) => {
      if (n.id !== selectedNoteId) return n;
      return {
        ...n,
        title,
        body,
        isChecklist,
        checklist,
        tags,
        folder,
        color,
        isPinned,
        updatedAt: now,
      };
    });

    persistNotes(updated);
  };

  const handleDeleteNote = (noteId: string) => {
    const target = notes.find((n) => n.id === noteId);
    if (!target) return;

    if (target.isTrashed) {
      // Permanent delete
      const next = notes.filter((n) => n.id !== noteId);
      persistNotes(next);
      if (selectedNoteId === noteId) {
        setSelectedNoteId(next[0]?.id || null);
        setIsEditorOpenOnMobile(false);
      }
      showToast('Permanently deleted');
    } else {
      // Move to Trash with Undo
      const now = Date.now();
      const next = notes.map((n) =>
        n.id === noteId
          ? {
              ...n,
              isTrashed: true,
              trashedAt: now,
              activityLog: [{ id: `l_${now}`, timestamp: now, action: 'moved to trash' }, ...n.activityLog],
            }
          : n
      );
      persistNotes(next);
      if (selectedNoteId === noteId) {
        setIsEditorOpenOnMobile(false);
      }
      showToast('Note moved to Trash', () => {
        const restored = notes.map((n) => (n.id === noteId ? { ...n, isTrashed: false, trashedAt: null } : n));
        persistNotes(restored);
        setSelectedNoteId(noteId);
      });
    }
  };

  const handleDuplicateNote = (noteId: string) => {
    const orig = notes.find((n) => n.id === noteId);
    if (!orig) return;
    const now = Date.now();

    const dup: Note = {
      ...orig,
      id: `n_${now}_${Math.random().toString(36).slice(2, 6)}`,
      title: orig.title ? `${orig.title} (Copy)` : 'Copy',
      isPinned: false,
      createdAt: now,
      updatedAt: now,
      activityLog: [{ id: `l_${now}`, timestamp: now, action: 'duplicated' }],
    };

    persistNotes([dup, ...notes]);
    setSelectedNoteId(dup.id);
    showToast('Note duplicated');
  };

  const handleToggleArchive = (noteId: string) => {
    const target = notes.find((n) => n.id === noteId);
    if (!target) return;
    const nextArch = !target.isArchived;

    const next = notes.map((n) =>
      n.id === noteId ? { ...n, isArchived: nextArch, updatedAt: Date.now() } : n
    );
    persistNotes(next);
    showToast(nextArch ? 'Archived' : 'Restored from archive');
  };

  const handleMoveNoteToFolder = (noteId: string, targetFolder: string) => {
    const updated = notes.map((n) =>
      n.id === noteId ? { ...n, folder: targetFolder, updatedAt: Date.now() } : n
    );
    persistNotes(updated);
    showToast(targetFolder ? `Moved to "${targetFolder}"` : "Removed from folder");
  };

  const handleCopyNoteToFolder = (noteId: string, targetFolder?: string) => {
    const orig = notes.find((n) => n.id === noteId);
    if (!orig) return;
    const now = Date.now();
    const destFolder = targetFolder !== undefined ? targetFolder : orig.folder;
    const dup: Note = {
      ...orig,
      id: `n_${now}_${Math.random().toString(36).slice(2, 6)}`,
      title: orig.title ? `${orig.title} (Copy)` : "Copy",
      folder: destFolder,
      isPinned: false,
      createdAt: now,
      updatedAt: now,
      activityLog: [{ id: `l_${now}`, timestamp: now, action: `copied to ${destFolder || "root"}` }],
    };
    persistNotes([dup, ...notes]);
    setSelectedNoteId(dup.id);
    showToast(destFolder ? `Copied to "${destFolder}"` : "Note copied");
  };

  const handleSelectTemplate = (tmpl: NoteTemplate) => {
    const now = Date.now();
    const folderToUse = activeFolderObj?.name || "";
    const newNote: Note = {
      id: `n_${now}_${Math.random().toString(36).slice(2, 6)}`,
      title: tmpl.defaultTitle,
      body: tmpl.defaultBody,
      isChecklist: tmpl.isChecklist,
      checklist: tmpl.checklist ? [...tmpl.checklist] : [],
      color: settings.accent,
      isPinned: false,
      isArchived: false,
      isTrashed: false,
      tags: [...tmpl.tags],
      folder: folderToUse,
      attachments: [],
      versions: [],
      activityLog: [{ id: `l_${now}`, timestamp: now, action: `created from template: ${tmpl.title}` }],
      createdAt: now,
      updatedAt: now,
    };
    persistNotes([newNote, ...notes]);
    setSelectedNoteId(newNote.id);
    setIsEditorOpenOnMobile(true);
    showToast(`Created from template: ${tmpl.title}`);
  };

  const handleTogglePin = (noteId: string) => {
    const next = notes.map((n) =>
      n.id === noteId ? { ...n, isPinned: !n.isPinned, updatedAt: Date.now() } : n
    );
    persistNotes(next);
  };

  const handleSelectColor = (noteId: string, color: NoteColorId) => {
    const next = notes.map((n) =>
      n.id === noteId ? { ...n, color, updatedAt: Date.now() } : n
    );
    persistNotes(next);
  };

  const handleEmptyTrash = () => {
    const count = notes.filter((n) => n.isTrashed).length;
    if (count === 0) return;
    const next = notes.filter((n) => !n.isTrashed);
    persistNotes(next);
    showToast(`Emptied ${count} notes from Trash`);
  };

  const handleAddAttachment = (
    type: AttachmentType,
    name: string,
    dataBase64: string,
    durationSec?: number,
    extra?: any
  ) => {
    if (!selectedNoteId) return;
    const now = Date.now();
    const newAtt: NoteAttachment = {
      id: `att_${now}_${Math.random().toString(36).slice(2, 6)}`,
      type,
      name,
      dataBase64,
      durationSec,
      createdAt: now,
      pages: extra?.pages,
      pdfDataUri: extra?.pdfDataUri,
      ocrText: extra?.ocrText,
      fileSize: extra?.fileSize,
    };

    const next = notes.map((n) =>
      n.id === selectedNoteId
        ? {
            ...n,
            attachments: [...n.attachments, newAtt],
            updatedAt: now,
            activityLog: [{ id: `l_${now}`, timestamp: now, action: `attached ${type}` }, ...n.activityLog],
          }
        : n
    );
    persistNotes(next);
    showToast(`Attached ${type}`);
  };

  const handleRemoveAttachment = (attachmentId: string) => {
    if (!selectedNoteId) return;
    const next = notes.map((n) =>
      n.id === selectedNoteId
        ? {
            ...n,
            attachments: n.attachments.filter((a) => a.id !== attachmentId),
            updatedAt: Date.now(),
          }
        : n
    );
    persistNotes(next);
  };

  const handleRestoreVersion = (ver: NoteVersion) => {
    if (!selectedNoteId) return;
    const now = Date.now();
    const next = notes.map((n) => {
      if (n.id !== selectedNoteId) return n;
      return {
        ...n,
        title: ver.title,
        body: ver.body,
        isChecklist: ver.isChecklist,
        checklist: [...ver.checklist],
        updatedAt: now,
        activityLog: [{ id: `l_${now}`, timestamp: now, action: 'restored version' }, ...n.activityLog],
      };
    });
    persistNotes(next);
    showToast('Version restored');
  };

  const handleSetReminder = (ts: number | null, soundId?: string) => {
    if (!selectedNoteId) return;
    const next = notes.map((n) =>
      n.id === selectedNoteId
        ? {
            ...n,
            reminderAt: ts,
            reminderSoundId: soundId || n.reminderSoundId,
            reminderFired: false,
            updatedAt: Date.now(),
          }
        : n
    );
    persistNotes(next);
    showToast(ts ? 'Reminder scheduled' : 'Reminder cleared');
  };

  const handleCommitSearch = (term: string) => {
    if (!recentSearches.includes(term)) {
      const updated = [term, ...recentSearches.filter((s) => s !== term)].slice(0, 6);
      setRecentSearches(updated);
      saveRecentSearches(updated);
    }
  };

  // Folder Management CRUD handlers
  const handleOpenCreateFolderModal = (parentId?: string | null) => {
    setFolderModalData({
      isOpen: true,
      existingFolder: null,
      parentId: parentId || null,
    });
  };

  const handleOpenEditFolderModal = (folder: FolderItem) => {
    setFolderModalData({
      isOpen: true,
      existingFolder: folder,
      parentId: folder.parentId,
    });
  };

  const handleSaveFolder = (data: {
    id?: string;
    name: string;
    parentId: string | null;
    color: string;
    icon: string;
  }) => {
    const now = Date.now();
    if (data.id) {
      // Update folder
      const updated = folders.map((f) =>
        f.id === data.id
          ? {
              ...f,
              name: data.name,
              parentId: data.parentId,
              color: data.color,
              icon: data.icon,
              updatedAt: now,
            }
          : f
      );
      persistFolders(updated);
      showToast('Folder updated');
    } else {
      // Create new folder
      const newFolder: FolderItem = {
        id: `f_${now}_${Math.random().toString(36).slice(2, 6)}`,
        name: data.name,
        parentId: data.parentId,
        color: data.color,
        icon: data.icon,
        isPinned: false,
        isArchived: false,
        isTrashed: false,
        createdAt: now,
        updatedAt: now,
        order: folders.length,
      };
      persistFolders([...folders, newFolder]);
      showToast('Folder created');
    }
    setFolderModalData({ isOpen: false, existingFolder: null, parentId: null });
  };

  const handleDeleteFolder = (folderId: string) => {
    const target = folders.find((f) => f.id === folderId);
    if (!target) return;

    if (target.isTrashed) {
      // Permanent delete
      persistFolders(folders.filter((f) => f.id !== folderId));
      if (selectedFolderId === folderId) setSelectedFolderId(null);
      showToast('Folder permanently deleted');
    } else {
      // Soft trash
      const updated = folders.map((f) => (f.id === folderId ? { ...f, isTrashed: true, trashedAt: Date.now() } : f));
      persistFolders(updated);
      if (selectedFolderId === folderId) setSelectedFolderId(null);
      showToast('Folder moved to Trash', () => {
        const restored = folders.map((f) => (f.id === folderId ? { ...f, isTrashed: false, trashedAt: null } : f));
        persistFolders(restored);
      });
    }
  };

  const handleTogglePinFolder = (folderId: string) => {
    const updated = folders.map((f) => (f.id === folderId ? { ...f, isPinned: !f.isPinned, updatedAt: Date.now() } : f));
    persistFolders(updated);
  };

  const handleToggleArchiveFolder = (folderId: string) => {
    const updated = folders.map((f) =>
      f.id === folderId ? { ...f, isArchived: !f.isArchived, updatedAt: Date.now() } : f
    );
    persistFolders(updated);
    showToast('Folder archive status updated');
  };

  const handleRestoreFolder = (folderId: string) => {
    const updated = folders.map((f) =>
      f.id === folderId ? { ...f, isTrashed: false, isArchived: false, trashedAt: null, updatedAt: Date.now() } : f
    );
    persistFolders(updated);
    showToast('Folder restored');
  };

  // Document Scanner Save Handler
  const handleSaveScanToNote = (scanData: {
    title: string;
    folder: string;
    pages: string[];
    pdfDataUri: string;
    extractedText: string;
  }) => {
    const now = Date.now();
    const scanAttachment: NoteAttachment = {
      id: `att_scan_${now}`,
      type: 'scan',
      name: `${scanData.title || 'Scanned Document'}.pdf`,
      dataBase64: scanData.pages[0] || '',
      pages: scanData.pages,
      pdfDataUri: scanData.pdfDataUri,
      ocrText: scanData.extractedText,
      createdAt: now,
    };

    const newNote: Note = {
      id: `n_${now}_${Math.random().toString(36).slice(2, 6)}`,
      title: scanData.title || 'Scanned Document',
      body: scanData.extractedText
        ? scanData.extractedText
        : `Scanned document with ${scanData.pages.length} page(s). PDF export generated.`,
      isChecklist: false,
      checklist: [],
      color: settings.accent,
      isPinned: false,
      isArchived: false,
      isTrashed: false,
      tags: ['scan', 'document'],
      folder: scanData.folder || activeFolderObj?.name || '',
      attachments: [scanAttachment],
      versions: [],
      activityLog: [{ id: `l_${now}`, timestamp: now, action: 'created from document scanner' }],
      createdAt: now,
      updatedAt: now,
    };

    persistNotes([newNote, ...notes]);
    setSelectedNoteId(newNote.id);
    setIsEditorOpenOnMobile(true);
    showToast(`Saved scan with ${scanData.pages.length} page(s)`);
  };

  const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
  const isDark = settings.theme === 'dark' || (settings.theme === 'system' && prefersDark);

  const toggleTheme = () => {
    const next = isDark ? 'light' : 'dark';
    updateSettings((s) => ({ ...s, theme: next }));
  };

  const trashedCount = notes.filter((n) => n.isTrashed).length;

  return (
    <div className="relative w-screen h-screen overflow-hidden flex flex-col p-2 sm:p-3">
      {/* Dynamic Animated Floating Color Blobs */}
      <GlassBackground />

      {/* Top Dynamic Island Save Pill */}
      <DynamicIslandSavePill visible={showSavedPill} label={strings.savedPill} />

      {/* Undo Toast Bar */}
      <UndoToastBar
        message={toastMessage}
        undoLabel={strings.undoLabel}
        onUndo={undoCallback || undefined}
        onDismiss={() => setToastMessage(null)}
      />

      {/* Settings Screen Overlay / View */}
      {isSettingsOpen ? (
        <SettingsScreen
          settings={settings}
          strings={strings}
          storageBytes={calculateStorageBytes()}
          trashedCount={trashedCount}
          onBack={() => setIsSettingsOpen(false)}
          onUpdateSettings={updateSettings}
          onEmptyTrash={handleEmptyTrash}
          onExportBackupJson={() => exportFullBackupJson(notes, settings, recentSearches, folders)}
          onImportBackupJson={(json) => {
            const res = importFullBackupJson(json);
            if (res) {
              setNotes(res.notes);
              setSettings(res.settings);
              if (res.folders) setFolders(res.folders);
              return true;
            }
            return false;
          }}
          onShowToast={showToast}
          onOpenSoundSettings={() => setIsSoundSettingsOpen(true)}
        />
      ) : (
        /* Main 3-Pane or 2-Pane Responsive View */
        <div className="relative flex-1 flex gap-3 min-h-0 w-full overflow-hidden">
          {/* Column 1: Collapsible Folder & Smart Views Sidebar */}
          {isFolderSidebarOpen && (
            <div
              className={`w-64 flex-none h-full transition-all duration-300 glass-panel rounded-3xl z-20 ${
                isEditorOpenOnMobile ? 'hidden' : 'hidden lg:flex'
              }`}
            >
              <FolderSidebar
                folders={folders}
                notes={notes}
                selectedFolderId={selectedFolderId}
                selectedSmartFolder={selectedSmartFolder}
                onSelectFolder={(fId) => {
                  setSelectedFolderId(fId);
                  setSelectedSmartFolder(null);
                }}
                onSelectSmartFolder={(smartId) => {
                  setSelectedSmartFolder(smartId);
                  setSelectedFolderId(null);
                }}
                onCreateFolder={handleOpenCreateFolderModal}
                onEditFolder={handleOpenEditFolderModal}
                onDeleteFolder={handleDeleteFolder}
                onTogglePinFolder={handleTogglePinFolder}
                onToggleArchiveFolder={handleToggleArchiveFolder}
                onRestoreFolder={handleRestoreFolder}
          onMoveNoteToFolder={handleMoveNoteToFolder}
              />
            </div>
          )}

          {/* Column 2: Note List Pane */}
          <div
            className={`w-full md:w-[380px] md:flex-none h-full transition-all duration-300 ${
              isEditorOpenOnMobile ? 'hidden md:flex' : 'flex'
            }`}
          >
            <NoteListPane
              notes={filteredNotes}
              recentNotes={notes.slice(0, 5)}
              allFolders={allFolderNames}
              folders={folders}
              selectedFolderId={selectedFolderId}
              selectedSmartFolder={selectedSmartFolder}
              selectedNoteId={selectedNoteId}
              searchQuery={searchQuery}
              recentSearches={recentSearches}
              activeTab={activeTab}
              showFilterRow={showFilterRow}
              searchFilters={searchFilters}
              sortBy={sortBy}
              hidePreviews={settings.hidePreviews}
              isDark={isDark}
              strings={strings}
              isFolderSidebarOpen={isFolderSidebarOpen}
              onSearchQueryChange={setSearchQuery}
              onCommitSearch={handleCommitSearch}
              onSelectTab={(tab) => {
                setActiveTab(tab);
                const first = notes.find((n) => {
                  if (tab === 'trash') return n.isTrashed;
                  if (n.isTrashed) return false;
                  if (tab === 'archive') return n.isArchived;
                  if (n.isArchived) return false;
                  if (tab === 'pinned') return n.isPinned;
                  if (tab === 'checklists') return n.isChecklist;
                  return true;
                });
                if (first) setSelectedNoteId(first.id);
              }}
              onToggleFilterRow={() => setShowFilterRow((v) => !v)}
              onUpdateFilters={(partial) => setSearchFilters((prev) => ({ ...prev, ...partial }))}
              onSortByChange={setSortBy}
              onSelectFolder={(fId) => {
                setSelectedFolderId(fId);
                setSelectedSmartFolder(null);
              }}
              onSelectSmartFolder={(sId) => {
                setSelectedSmartFolder(sId);
                setSelectedFolderId(null);
              }}
              onClearFolderFilter={() => {
                setSelectedFolderId(null);
                setSelectedSmartFolder(null);
              }}
              onToggleFolderSidebar={() => setIsFolderSidebarOpen((v) => !v)}
              onSelectNote={(noteId) => {
                setSelectedNoteId(noteId);
                setIsEditorOpenOnMobile(true);
              }}
              onCreateNote={() => handleCreateNote()}
              onOpenTemplates={() => setIsTemplatesModalOpen(true)}
              onOpenScanner={() => setIsScannerOpen(true)}
              onQuickCapture={() => setIsQuickCaptureOpen(true)}
              onToggleTheme={toggleTheme}
              onOpenSettings={() => setIsSettingsOpen(true)}
              onOpenContextMenu={(n) => setContextMenuNote(n)}
              onEmptyTrash={handleEmptyTrash}
            />
          </div>

          {/* Column 3: Advanced Note Editor Pane */}
          <div
            className={`flex-1 h-full min-w-0 transition-all duration-300 ${
              isEditorOpenOnMobile ? 'flex' : 'hidden md:flex'
            }`}
          >
            <NoteEditorPane
              note={selectedNote}
              folders={folders}
              showBackButton={isEditorOpenOnMobile}
              isMarkdownPreview={isMarkdownPreview}
              strings={strings}
              isKhmer={isKhmer}
              onBack={() => setIsEditorOpenOnMobile(false)}
              onUpdateNote={handleUpdateNoteContent}
              onDeleteNote={() => {
                if (selectedNoteId) handleDeleteNote(selectedNoteId);
              }}
              onDuplicateNote={() => {
                if (selectedNoteId) handleDuplicateNote(selectedNoteId);
              }}
              onToggleMarkdown={() => setIsMarkdownPreview((v) => !v)}
              onOpenReminderDialog={() => setIsReminderPickerOpen(true)}
              onOpenVoiceRecorder={() => setIsVoiceRecorderOpen(true)}
              onOpenImageUpload={() => {
                // Handled in NoteEditorPane file trigger
              }}
              onOpenPhotoCrop={(b64) => setPhotoCropBase64(b64)}
              onOpenDrawingCanvas={() => setIsDrawingCanvasOpen(true)}
              onOpenScanner={() => setIsScannerOpen(true)}
              onOpenHistory={() => setIsHistoryOpen(true)}
              onRemoveAttachment={handleRemoveAttachment}
              onAddAttachment={handleAddAttachment}
              onShowToast={showToast}
            />
          </div>
        </div>
      )}

      {/* Document Scanner Modal */}
      {isScannerOpen && (
        <DocumentScannerModal
          folders={folders}
          defaultFolder={activeFolderObj?.name || ''}
          onDismiss={() => setIsScannerOpen(false)}
          onSaveScanToNote={handleSaveScanToNote}
          onShowToast={showToast}
        />
      )}

      {/* Folder Manager Modal (Create/Edit Folder) */}
      {folderModalData.isOpen && (
        <FolderManagerModal
          existingFolder={folderModalData.existingFolder}
          parentFolderId={folderModalData.parentId}
          allFolders={folders}
          onDismiss={() => setFolderModalData({ isOpen: false })}
          onSave={handleSaveFolder}
        />
      )}

      {/* Context Bottom Sheet Modal */}
      {contextMenuNote && (
        <NoteContextSheet
          note={contextMenuNote}
          folders={folders}
          strings={strings}
          onDismiss={() => setContextMenuNote(null)}
          onSelectColor={(c) => handleSelectColor(contextMenuNote.id, c)}
          onTogglePin={() => handleTogglePin(contextMenuNote.id)}
          onDuplicate={() => handleDuplicateNote(contextMenuNote.id)}
          onCopyNote={(targetFolder) => handleCopyNoteToFolder(contextMenuNote.id, targetFolder)}
          onMoveNote={(targetFolder) => handleMoveNoteToFolder(contextMenuNote.id, targetFolder)}
          onToggleArchive={() => handleToggleArchive(contextMenuNote.id)}
          onDelete={() => handleDeleteNote(contextMenuNote.id)}
        />
      )}

      {/* Quick Capture Dialog */}
      {quickCaptureOpen && (
        <QuickCaptureDialog
          strings={strings}
          onDismiss={() => setIsQuickCaptureOpen(false)}
          onSaveCapture={handleQuickCapture}
        />
      )}

      {/* Drawing Canvas Modal */}
      {isDrawingCanvasOpen && (
        <DrawingCanvasDialog
          strings={strings}
          onDismiss={() => setIsDrawingCanvasOpen(false)}
          onAttachDrawing={(b64) => {
            handleAddAttachment('drawing', 'Drawing Note', b64);
            setIsDrawingCanvasOpen(false);
          }}
        />
      )}

      {/* Voice Recorder Modal */}
      {isVoiceRecorderOpen && (
        <VoiceRecorderDialog
          strings={strings}
          onDismiss={() => setIsVoiceRecorderOpen(false)}
          onAttachVoice={(b64, durationSec) => {
            handleAddAttachment('voice', 'Voice Memo', b64, durationSec);
            setIsVoiceRecorderOpen(false);
          }}
        />
      )}

      {/* Photo Crop Modal */}
      {photoCropBase64 && (
        <PhotoCropDialog
          initialImageBase64={photoCropBase64}
          strings={strings}
          onDismiss={() => setPhotoCropBase64(null)}
          onCropConfirmed={(croppedB64) => {
            handleAddAttachment('image', 'Photo Attachment', croppedB64);
            setPhotoCropBase64(null);
          }}
        />
      )}

      {/* Reminder Picker Modal */}
      {isReminderPickerOpen && selectedNote && (
        <ReminderPickerDialog
          currentReminderAt={selectedNote.reminderAt}
          currentSoundId={selectedNote.reminderSoundId}
          soundSettings={soundSettings}
          strings={strings}
          isKhmer={isKhmer}
          onDismiss={() => setIsReminderPickerOpen(false)}
          onSetReminder={handleSetReminder}
          onOpenSoundSettings={() => setIsSoundSettingsOpen(true)}
        />
      )}

      {/* Custom Reminder & Sound Settings Modal */}
      {isSoundSettingsOpen && (
        <SoundSettingsDialog
          settings={soundSettings}
          isKhmer={isKhmer}
          onDismiss={() => setIsSoundSettingsOpen(false)}
          onSaveSettings={(newSettings) => {
            setSoundSettings(newSettings);
            saveSoundSettings(newSettings);
          }}
          onShowToast={showToast}
        />
      )}

      {/* History Snapshots Modal */}
      {isHistoryOpen && selectedNote && (
        <HistoryDialog
          note={selectedNote}
          strings={strings}
          onDismiss={() => setIsHistoryOpen(false)}
          onRestoreVersion={handleRestoreVersion}
        />
      )}
    </div>
  );
};
