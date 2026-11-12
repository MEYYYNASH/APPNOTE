import { SoundSettings, DEFAULT_SOUND_SETTINGS } from '../types/sound';
import { Note, AppSettings, ChecklistItem, NoteVersion, ActivityLogEntry, FolderItem } from '../types/note';

const PREFS_NAME = 'glass_notes_local_storage';
const KEY_NOTES = 'glass_notes_data_v1';
const KEY_SETTINGS = 'glass_notes_settings_v1';
const KEY_RECENT_SEARCHES = 'glass_notes_recent_searches_v1';
const KEY_FOLDERS = 'glass_notes_folders_v1';

export const DEFAULT_SETTINGS: AppSettings = {
  theme: 'system',
  accent: 'blue',
  font: 'figtree',
  customFontPath: '',
  customFontName: '',
  fontScale: 1.0,
  language: 'en',
  compactMode: false,
  animationsEnabled: true,
  defaultNoteType: 'text',
  snapshotIntervalSec: 30,
  notificationsEnabled: false,
  hidePreviews: false,
  
  githubOwnerRepo: 'MEYYYNASH/APPNOTE',
  githubBranch: 'main',
  githubToken: '',
  appVersion: '1.1.19',
  latestAvailableVersion: 'v1.1.19',
  includePrereleases: false,
};

let inMemoryFallbackNotes: string | null = null;
let inMemoryFallbackSettings: string | null = null;
let inMemoryFallbackSearches: string | null = null;
let inMemoryFallbackFolders: string | null = null;

function safeGet(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    if (key === KEY_NOTES) return inMemoryFallbackNotes;
    if (key === KEY_SETTINGS) return inMemoryFallbackSettings;
    if (key === KEY_RECENT_SEARCHES) return inMemoryFallbackSearches;
    if (key === KEY_FOLDERS) return inMemoryFallbackFolders;
    return null;
  }
}

function safeSet(key: string, val: string): boolean {
  try {
    localStorage.setItem(key, val);
    return true;
  } catch {
    if (key === KEY_NOTES) inMemoryFallbackNotes = val;
    if (key === KEY_SETTINGS) inMemoryFallbackSettings = val;
    if (key === KEY_RECENT_SEARCHES) inMemoryFallbackSearches = val;
    if (key === KEY_FOLDERS) inMemoryFallbackFolders = val;
    return false;
  }
}

export function createSeedFolders(): FolderItem[] {
  const now = Date.now();
  return [
    {
      id: 'f-product',
      name: 'Product',
      parentId: null,
      color: '#3B82F6',
      icon: 'briefcase',
      isPinned: true,
      isArchived: false,
      isTrashed: false,
      createdAt: now - 3600_000 * 48,
      updatedAt: now - 3600_000 * 2,
      order: 0,
    },
    {
      id: 'f-design',
      name: 'Design Specs',
      parentId: 'f-product',
      color: '#EC4899',
      icon: 'sparkles',
      isPinned: false,
      isArchived: false,
      isTrashed: false,
      createdAt: now - 3600_000 * 24,
      updatedAt: now - 3600_000 * 1,
      order: 1,
    },
    {
      id: 'f-engineering',
      name: 'Engineering',
      parentId: null,
      color: '#8B5CF6',
      icon: 'code',
      isPinned: true,
      isArchived: false,
      isTrashed: false,
      createdAt: now - 3600_000 * 36,
      updatedAt: now - 3600_000 * 3,
      order: 2,
    },
    {
      id: 'f-architecture',
      name: 'Architecture',
      parentId: null,
      color: '#10B981',
      icon: 'book',
      isPinned: false,
      isArchived: false,
      isTrashed: false,
      createdAt: now - 3600_000 * 50,
      updatedAt: now - 3600_000 * 10,
      order: 3,
    },
    {
      id: 'f-personal',
      name: 'Personal',
      parentId: null,
      color: '#F59E0B',
      icon: 'heart',
      isPinned: false,
      isArchived: false,
      isTrashed: false,
      createdAt: now - 3600_000 * 60,
      updatedAt: now - 3600_000 * 12,
      order: 4,
    },
  ];
}

export function loadFolders(): FolderItem[] {
  const raw = safeGet(KEY_FOLDERS);
  if (!raw) {
    const seeded = createSeedFolders();
    saveFolders(seeded);
    return seeded;
  }
  try {
    const list: FolderItem[] = JSON.parse(raw);
    return Array.isArray(list) ? list : createSeedFolders();
  } catch {
    const seeded = createSeedFolders();
    saveFolders(seeded);
    return seeded;
  }
}

export function saveFolders(folders: FolderItem[]): boolean {
  return safeSet(KEY_FOLDERS, JSON.stringify(folders));
}

export function createSeedNotes(): Note[] {
  const now = Date.now();
  const n1Created = now - 3600_000 * 2;
  const n2Created = now - 3600_000 * 3;
  const n3Created = now - 3600_000 * 18;
  const n4Created = now - 3600_000 * 22;

  return [
    {
      id: 'bento-project-ideas',
      title: 'Project Ideas',
      body: 'Build a modern note app with AI features, multi-page document scanner, rich markdown editor, and custom reminder sounds.',
      isChecklist: false,
      checklist: [],
      color: 'blue',
      isPinned: true,
      isArchived: false,
      isTrashed: false,
      tags: ['project', 'ideas', 'ui'],
      folder: 'Product',
      attachments: [],
      versions: [
        {
          id: 'v-bento-1',
          timestamp: n1Created,
          title: 'Project Ideas',
          body: 'Build a modern note app with AI features...',
          checklist: [],
          isChecklist: false,
        },
      ],
      activityLog: [{ id: 'l-1', timestamp: n1Created, action: 'created' }],
      createdAt: n1Created,
      updatedAt: now - 600_000,
    },
    {
      id: 'bento-shopping-list',
      title: 'Shopping List',
      body: '',
      isChecklist: true,
      checklist: [
        { id: 'shop-1', text: 'Milk', isDone: true },
        { id: 'shop-2', text: 'Bread', isDone: true },
        { id: 'shop-3', text: 'Coffee', isDone: false },
      ],
      color: 'violet',
      isPinned: true,
      isArchived: false,
      isTrashed: false,
      tags: ['shopping', 'personal'],
      folder: 'Personal',
      attachments: [],
      versions: [],
      activityLog: [{ id: 'l-2', timestamp: n2Created, action: 'created' }],
      createdAt: n2Created,
      updatedAt: now - 1200_000,
    },
    {
      id: 'bento-meeting-notes',
      title: 'Meeting Notes',
      body: 'Discuss the new project and timeline with engineering and design teams.',
      isChecklist: false,
      checklist: [],
      color: 'green',
      isPinned: false,
      isArchived: false,
      isTrashed: false,
      tags: ['meeting', 'timeline'],
      folder: 'Engineering',
      reminderAt: now + 3600_000 * 2,
      attachments: [],
      versions: [],
      activityLog: [{ id: 'l-3', timestamp: n3Created, action: 'created' }],
      createdAt: n3Created,
      updatedAt: n3Created,
    },
    {
      id: 'bento-daily-journal',
      title: 'Daily Journal',
      body: 'Today is a great day! ✨ Focused on shipping v1.1.19 with real Jetpack Glance Home Screen widgets and a clean single-column UI.',
      isChecklist: false,
      checklist: [],
      color: 'pink',
      isPinned: false,
      isArchived: false,
      isTrashed: false,
      tags: ['journal', 'daily'],
      folder: 'Personal',
      attachments: [],
      versions: [],
      activityLog: [{ id: 'l-4', timestamp: n4Created, action: 'created' }],
      createdAt: n4Created,
      updatedAt: n4Created,
    },
  ];
}

export function loadNotes(): Note[] {
  const raw = safeGet(KEY_NOTES);
  if (!raw) {
    const seeded = createSeedNotes();
    saveNotes(seeded);
    return seeded;
  }
  try {
    const list: Note[] = JSON.parse(raw);
    const now = Date.now();
    const thirtyDaysMs = 30 * 24 * 60 * 60 * 1000;
    const active = list.filter((n) => {
      if (n.isTrashed && n.trashedAt && now - n.trashedAt > thirtyDaysMs) {
        return false;
      }
      return true;
    });
    // Ensure the 4 Bento showcase notes are present if user only had old seed notes
    const hasBento = active.some((n) => n.id.startsWith('bento-'));
    if (!hasBento) {
      const merged = [...createSeedNotes(), ...active];
      saveNotes(merged);
      return merged;
    }
    return active;
  } catch {
    const seeded = createSeedNotes();
    saveNotes(seeded);
    return seeded;
  }
}

export function saveNotes(notes: Note[]): boolean {
  return safeSet(KEY_NOTES, JSON.stringify(notes));
}

export function loadSettings(): AppSettings {
  const raw = safeGet(KEY_SETTINGS);
  if (!raw) return DEFAULT_SETTINGS;
  try {
    const parsed = JSON.parse(raw);
    const latestVer =
      !parsed.latestAvailableVersion ||
      parsed.latestAvailableVersion === 'v1.1.17' ||
      parsed.latestAvailableVersion === '1.1.17' ||
      parsed.latestAvailableVersion === 'v1.1.18' ||
      parsed.latestAvailableVersion === '1.1.18'
        ? 'v1.1.19'
        : parsed.latestAvailableVersion;
    const upgraded: AppSettings = {
      ...DEFAULT_SETTINGS,
      ...parsed,
      githubOwnerRepo: 'MEYYYNASH/APPNOTE',
      appVersion: '1.1.19',
      latestAvailableVersion: latestVer,
    };
    saveSettings(upgraded);
    return upgraded;
  } catch {
    return DEFAULT_SETTINGS;
  }
}

export function saveSettings(settings: AppSettings): boolean {
  return safeSet(KEY_SETTINGS, JSON.stringify({ ...settings, appVersion: '1.1.19' }));
}

export function loadRecentSearches(): string[] {
  const raw = safeGet(KEY_RECENT_SEARCHES);
  if (!raw) return ['#roadmap', 'Product', 'Architecture'];
  try {
    const arr = JSON.parse(raw);
    return Array.isArray(arr) ? arr.slice(0, 6) : [];
  } catch {
    return ['#roadmap', 'Product', 'Architecture'];
  }
}

export function saveRecentSearches(searches: string[]) {
  safeSet(KEY_RECENT_SEARCHES, JSON.stringify(searches.slice(0, 6)));
}

export function calculateStorageBytes(): number {
  const n = safeGet(KEY_NOTES)?.length || 0;
  const s = safeGet(KEY_SETTINGS)?.length || 0;
  const r = safeGet(KEY_RECENT_SEARCHES)?.length || 0;
  return n + s + r;
}

export function exportFullBackupJson(
  notes: Note[],
  settings: AppSettings,
  recentSearches: string[],
  folders: FolderItem[] = loadFolders()
): string {
  const backup = {
    version: settings.appVersion,
    exportedAt: Date.now(),
    notes,
    folders,
    settings,
    recentSearches,
  };
  return JSON.stringify(backup, null, 2);
}

export function importFullBackupJson(
  jsonString: string
): { notes: Note[]; settings: AppSettings; folders: FolderItem[] } | null {
  try {
    const data = JSON.parse(jsonString);
    if (!data || !Array.isArray(data.notes)) return null;
    const notes: Note[] = data.notes;
    const folders: FolderItem[] = Array.isArray(data.folders) ? data.folders : loadFolders();
    const settings: AppSettings = { ...DEFAULT_SETTINGS, ...(data.settings || {}) };
    saveNotes(notes);
    saveFolders(folders);
    saveSettings(settings);
    if (Array.isArray(data.recentSearches)) {
      saveRecentSearches(data.recentSearches);
    }
    return { notes, settings, folders };
  } catch {
    return null;
  }
}

const KEY_SOUND_SETTINGS = "glassnotes_sound_settings";

export function loadSoundSettings(): SoundSettings {
  try {
    const raw = localStorage.getItem(KEY_SOUND_SETTINGS);
    if (!raw) return DEFAULT_SOUND_SETTINGS;
    return { ...DEFAULT_SOUND_SETTINGS, ...JSON.parse(raw) };
  } catch {
    return DEFAULT_SOUND_SETTINGS;
  }
}

export function saveSoundSettings(settings: SoundSettings): void {
  try {
    localStorage.setItem(KEY_SOUND_SETTINGS, JSON.stringify(settings));
  } catch {
    // ignore
  }
}
