import React, { useState, useEffect, useMemo, useRef } from 'react';
import {
  Note,
  AppSettings,
  FilterTab,
  SearchFilters,
  NoteColorId,
  ChecklistItem,
  NoteVersion,
  TRANSLATIONS,
} from './types/note';
import {
  loadNotes,
  saveNotes,
  loadSettings,
  saveSettings,
  loadRecentSearches,
  saveRecentSearches,
  calculateStorageBytes,
  exportFullBackupJson,
  importFullBackupJson,
} from './services/storage';
import { playNotificationChime } from './utils/audio';

import { GlassBackground } from './components/GlassBackground';
import { DynamicIslandSavePill } from './components/DynamicIslandSavePill';
import { UndoToastBar } from './components/UndoToastBar';
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

export const App: React.FC = () => {
  const [notes, setNotes] = useState<Note[]>(() => loadNotes());
  const [settings, setSettings] = useState<AppSettings>(() => loadSettings());
  const [recentSearches, setRecentSearches] = useState<string[]>(() => loadRecentSearches());

  const [selectedNoteId, setSelectedNoteId] = useState<string | null>(() => {
    const list = loadNotes();
    const activeNotes = list.filter((n) => !n.isTrashed && !n.isArchived);
    return activeNotes.length > 0 ? activeNotes[0].id : null;
  });

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
  const [isQuickCaptureOpen, setIsQuickCaptureOpen] = useState(false);
  const [isVoiceRecorderOpen, setIsVoiceRecorderOpen] = useState(false);
  const [isDrawingCanvasOpen, setIsDrawingCanvasOpen] = useState(false);
  const [photoCropBase64, setPhotoCropBase64] = useState<string | null>(null);
  const [isReminderPickerOpen, setIsReminderPickerOpen] = useState(false);
  const [isHistoryOpen, setIsHistoryOpen] = useState(false);

  const saveTimerRef = useRef<number | null>(null);
  const snapshotTimerRef = useRef<number | null>(null);

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

  // Auto-snapshot versions every snapshotIntervalSec
  useEffect(() => {
    const intervalMs = Math.max(10, settings.snapshotIntervalSec) * 1000;
    const timer = setInterval(() => {
      setNotes((currentNotes) => {
        let changed = false;
        const now = Date.now();
        const updated = currentNotes.map((note) => {
          if (note.isTrashed || !note.title.trim() && !note.body.trim()) return note;
          // If edited recently and no snapshot within 30s
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
            playNotificationChime();
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

  // Derived filtered notes
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
        if (a.isPinned !== b.isPinned) return a.isPinned ? -1 : 1;
        return b.updatedAt - a.updatedAt;
      });
  }, [notes, searchQuery, activeTab, searchFilters]);

  // All distinct folders
  const allFolders = useMemo(() => {
    const set = new Set<string>();
    notes.forEach((n) => {
      if (n.folder.trim()) set.add(n.folder.trim());
    });
    return Array.from(set).sort();
  }, [notes]);

  // Currently selected note
  const selectedNote = useMemo(() => {
    return notes.find((n) => n.id === selectedNoteId) || null;
  }, [notes, selectedNoteId]);

  // Note CRUD actions
  const handleCreateNote = () => {
    const now = Date.now();
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
      folder: '',
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
      folder: '',
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
        // Undo function
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
    type: 'voice' | 'image' | 'scan' | 'drawing',
    name: string,
    dataBase64: string,
    durationSec?: number
  ) => {
    if (!selectedNoteId) return;
    const now = Date.now();
    const newAtt = {
      id: `att_${now}_${Math.random().toString(36).slice(2, 6)}`,
      type,
      name,
      dataBase64,
      durationSec,
      createdAt: now,
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

  const handleSetReminder = (ts: number | null) => {
    if (!selectedNoteId) return;
    const next = notes.map((n) =>
      n.id === selectedNoteId
        ? {
            ...n,
            reminderAt: ts,
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
          onExportBackupJson={() => exportFullBackupJson(notes, settings, recentSearches)}
          onImportBackupJson={(json) => {
            const res = importFullBackupJson(json);
            if (res) {
              setNotes(res.notes);
              setSettings(res.settings);
              return true;
            }
            return false;
          }}
          onShowToast={showToast}
        />
      ) : (
        /* Main Two-Pane or Mobile Slide Responsive View */
        <div className="relative flex-1 flex gap-3 min-h-0 w-full overflow-hidden">
          {/* Note List Sidebar (Hidden on mobile if editor is active) */}
          <div
            className={`w-full md:w-[380px] md:flex-none h-full transition-all duration-300 ${
              isEditorOpenOnMobile ? 'hidden md:flex' : 'flex'
            }`}
          >
            <NoteListPane
              notes={filteredNotes}
              recentNotes={notes.slice(0, 5)}
              allFolders={allFolders}
              selectedNoteId={selectedNoteId}
              searchQuery={searchQuery}
              recentSearches={recentSearches}
              activeTab={activeTab}
              showFilterRow={showFilterRow}
              searchFilters={searchFilters}
              hidePreviews={settings.hidePreviews}
              isDark={isDark}
              strings={strings}
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
              onSelectNote={(noteId) => {
                setSelectedNoteId(noteId);
                setIsEditorOpenOnMobile(true);
              }}
              onCreateNote={handleCreateNote}
              onQuickCapture={() => setIsQuickCaptureOpen(true)}
              onToggleTheme={toggleTheme}
              onOpenSettings={() => setIsSettingsOpen(true)}
              onOpenContextMenu={(n) => setContextMenuNote(n)}
              onEmptyTrash={handleEmptyTrash}
            />
          </div>

          {/* Note Editor Pane (Hidden on mobile if editor is closed) */}
          <div
            className={`flex-1 h-full min-w-0 transition-all duration-300 ${
              isEditorOpenOnMobile ? 'flex' : 'hidden md:flex'
            }`}
          >
            <NoteEditorPane
              note={selectedNote}
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
                // Handled via file input inside NoteEditorPane
              }}
              onOpenPhotoCrop={(b64) => setPhotoCropBase64(b64)}
              onOpenDrawingCanvas={() => setIsDrawingCanvasOpen(true)}
              onOpenHistory={() => setIsHistoryOpen(true)}
              onRemoveAttachment={handleRemoveAttachment}
              onShowToast={showToast}
            />
          </div>
        </div>
      )}

      {/* Context Bottom Sheet Modal */}
      {contextMenuNote && (
        <NoteContextSheet
          note={contextMenuNote}
          strings={strings}
          onDismiss={() => setContextMenuNote(null)}
          onSelectColor={(c) => handleSelectColor(contextMenuNote.id, c)}
          onTogglePin={() => handleTogglePin(contextMenuNote.id)}
          onDuplicate={() => handleDuplicateNote(contextMenuNote.id)}
          onToggleArchive={() => handleToggleArchive(contextMenuNote.id)}
          onDelete={() => handleDeleteNote(contextMenuNote.id)}
        />
      )}

      {/* Quick Capture Dialog */}
      {isQuickCaptureOpen && (
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
          strings={strings}
          isKhmer={isKhmer}
          onDismiss={() => setIsReminderPickerOpen(false)}
          onSetReminder={handleSetReminder}
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
