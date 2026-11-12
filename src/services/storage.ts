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
  appVersion: '1.1.17',
  latestAvailableVersion: 'v1.1.17',
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
  const n1Created = now - 3600_000 * 5;
  const n2Created = now - 3600_000 * 18;
  const n3Created = now - 3600_000 * 42;

  return [
    {
      id: 'seed-1',
      title: 'Glassmorphism Design System',
      body: '# Frosted Glass Principles\n\nOur interface uses **translucent panels** with `26px backdrop blur`, saturation boost, and a crisp `1px` specular edge.\n\n- Soft drifting color blobs in blue, violet, and cyan\n- Two-color gradient accents across interactive pills\n- Clean 2px rounded line icons only',
      isChecklist: false,
      checklist: [],
      color: 'blue',
      isPinned: true,
      isArchived: false,
      isTrashed: false,
      tags: ['design', 'architecture', 'ui'],
      folder: 'Product',
      attachments: [],
      versions: [
        {
          id: 'v-seed-1',
          timestamp: n1Created,
          title: 'Glassmorphism Design System',
          body: '# Frosted Glass Principles\nInitial specification draft for translucent cards.',
          checklist: [],
          isChecklist: false,
        },
      ],
      activityLog: [
        { id: 'l-1', timestamp: now - 1800_000, action: 'edited' },
        { id: 'l-0', timestamp: n1Created, action: 'created' },
      ],
      createdAt: n1Created,
      updatedAt: now - 1800_000,
    },
    {
      id: 'seed-2',
      title: 'Release Sprint Checklist',
      body: '',
      isChecklist: true,
      checklist: [
        { id: 'c-1', text: 'Configure Figtree, Serif, and Mono typography', isDone: true },
        { id: 'c-2', text: 'Verify two-pane layout on wide screens and mobile slide', isDone: true },
        { id: 'c-3', text: 'Test Voice, Image, Scan, and Drawing canvas attachments', isDone: true },
        { id: 'c-4', text: 'Add English and Khmer language translations', isDone: false },
        { id: 'c-5', text: 'Prepare JSON backup and restore verification', isDone: false },
      ],
      color: 'violet',
      isPinned: true,
      isArchived: false,
      isTrashed: false,
      tags: ['roadmap', 'sprint'],
      folder: 'Engineering',
      attachments: [],
      versions: [
        {
          id: 'v-seed-2',
          timestamp: n2Created,
          title: 'Release Sprint Checklist',
          body: '',
          checklist: [
            { id: 'c-1', text: 'Configure Figtree, Serif, and Mono typography', isDone: true },
            { id: 'c-2', text: 'Verify two-pane layout on wide screens and mobile slide', isDone: false },
          ],
          isChecklist: true,
        },
      ],
      activityLog: [
        { id: 'l-3', timestamp: now - 3600_000 * 2, action: 'edited' },
        { id: 'l-2', timestamp: n2Created, action: 'created' },
      ],
      createdAt: n2Created,
      updatedAt: now - 3600_000 * 2,
    },
    {
      id: 'seed-3',
      title: 'Offline & LocalStorage Architecture',
      body: '## Privacy First Storage\n\nAll notes, attachments, and version snapshots are saved in **local storage** wrapped in `try/catch` with an automatic in-memory fallback.\n\n- Data is preserved securely right on this device\n- Export full JSON backups anytime from Settings\n\n### What Was Added & Updated\n\n- **Real-Time Download Speed (MB/s):** Low-pass filtered speed tracking displaying smooth instantaneous transfer rates without erratic jumping.\n- **Speed Time to End (ETA):** Automatically calculates remaining completion time (e.g. `12s left`, `1m 15s left`, and Khmer `12 វិ`, `1 ន 15 វិ`).\n- **Live MB Progress Counter:** Real-time bytes transferred vs total file size (`18.4 MB / 45.2 MB`) alongside percentage.\n- **Live Download HUD in Settings:** Animated glass gradient progress bar, Speedometer metric card, Time to End metric card, and one-tap cancel.\n- **"Test Download Speed":** Benchmark latency (ping in ms) and CDN throughput (MB/s) with quality ratings (Ultra Fast, Fast, Good).',
      isChecklist: false,
      checklist: [],
      color: 'green',
      isPinned: false,
      isArchived: false,
      isTrashed: false,
      tags: ['privacy', 'storage', 'update'],
      folder: 'Architecture',
      attachments: [],
      versions: [
        {
          id: 'v-seed-3',
          timestamp: n3Created,
          title: 'Offline & LocalStorage Architecture',
          body: 'All notes are stored locally on device.',
          checklist: [],
          isChecklist: false,
        },
      ],
      activityLog: [{ id: 'l-4', timestamp: n3Created, action: 'created' }],
      createdAt: n3Created,
      updatedAt: n3Created,
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
    // Purge trashed notes older than 30 days
    return list.filter((n) => {
      if (n.isTrashed && n.trashedAt && now - n.trashedAt > thirtyDaysMs) {
        return false;
      }
      return true;
    });
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
    return { ...DEFAULT_SETTINGS, ...parsed };
  } catch {
    return DEFAULT_SETTINGS;
  }
}

export function saveSettings(settings: AppSettings): boolean {
  return safeSet(KEY_SETTINGS, JSON.stringify(settings));
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
