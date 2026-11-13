export type NoteColorId = 'blue' | 'green' | 'amber' | 'violet' | 'pink' | 'slate';

export interface NoteColorConfig {
  id: NoteColorId;
  labelEn: string;
  labelKm: string;
  startColor: string;
  endColor: string;
  dotColor: string;
}

export const NOTE_COLORS: Record<NoteColorId, NoteColorConfig> = {
  blue: {
    id: 'blue',
    labelEn: 'Blue',
    labelKm: 'ខៀវ',
    startColor: '#2F7CFF',
    endColor: '#7A5CFF',
    dotColor: '#3B82F6',
  },
  green: {
    id: 'green',
    labelEn: 'Green',
    labelKm: 'បៃតង',
    startColor: '#10B981',
    endColor: '#06B6D4',
    dotColor: '#10B981',
  },
  amber: {
    id: 'amber',
    labelEn: 'Amber',
    labelKm: 'លឿងទុំ',
    startColor: '#F59E0B',
    endColor: '#EF4444',
    dotColor: '#F59E0B',
  },
  violet: {
    id: 'violet',
    labelEn: 'Violet',
    labelKm: 'ស្វាយ',
    startColor: '#A855F7',
    endColor: '#6366F1',
    dotColor: '#A855F7',
  },
  pink: {
    id: 'pink',
    labelEn: 'Pink',
    labelKm: 'ផ្កាឈូក',
    startColor: '#EC4899',
    endColor: '#F43F5E',
    dotColor: '#EC4899',
  },
  slate: {
    id: 'slate',
    labelEn: 'Slate',
    labelKm: 'ប្រផេះ',
    startColor: '#64748B',
    endColor: '#0F172A',
    dotColor: '#64748B',
  },
};

export type ThemeOption = 'system' | 'light' | 'dark';
export type FontOption = 'figtree' | 'serif' | 'mono' | 'custom';
export type LanguageOption = 'en' | 'km';
export type NoteType = 'text' | 'checklist';
export type AttachmentType = 'voice' | 'image' | 'scan' | 'drawing' | 'video' | 'pdf' | 'doc';

export interface NoteAttachment {
  id: string;
  type: AttachmentType;
  name: string;
  dataBase64: string;
  durationSec?: number;
  createdAt: number;
  pages?: string[]; // for multi-page scans
  pdfDataUri?: string; // for compiled PDF export
  ocrText?: string; // extracted text from document scanner
  fileSize?: number;
}

export interface FolderItem {
  id: string;
  name: string;
  parentId: string | null;
  color: string;
  icon: string;
  isPinned: boolean;
  isArchived: boolean;
  isTrashed: boolean;
  trashedAt?: number | null;
  createdAt: number;
  updatedAt: number;
  order: number;
}

export type NoteSortOption = 'updated' | 'created' | 'nameAsc' | 'nameDesc';

export interface ChecklistItem {
  id: string;
  text: string;
  isDone: boolean;
}

export interface NoteVersion {
  id: string;
  timestamp: number;
  title: string;
  body: string;
  checklist: ChecklistItem[];
  isChecklist: boolean;
}

export interface ActivityLogEntry {
  id: string;
  timestamp: number;
  action: string;
}

export type EditorFontStyleId = 'sans' | 'serif' | 'mono' | 'handwritten' | 'geometric';

export interface Note {
  id: string;
  title: string;
  body: string;
  richHtml?: string;
  bodyFontStyle?: EditorFontStyleId;
  bodyTextColor?: string;
  bodyTextSize?: string;
  bodyTextAlign?: 'left' | 'center' | 'right';
  isChecklist: boolean;
  checklist: ChecklistItem[];
  color: NoteColorId;
  isPinned: boolean;
  isArchived: boolean;
  isTrashed: boolean;
  trashedAt?: number | null;
  reminderAt?: number | null;
  reminderFired?: boolean;
  reminderSoundId?: string;
  tags: string[];
  folder: string;
  attachments: NoteAttachment[];
  versions: NoteVersion[];
  activityLog: ActivityLogEntry[];
  createdAt: number;
  updatedAt: number;
}

export interface AppSettings {
  theme: ThemeOption;
  accent: NoteColorId;
  font: FontOption;
  customFontPath: string;
  customFontName: string;
  fontScale: number;
  language: LanguageOption;
  compactMode: boolean;
  animationsEnabled: boolean;
  defaultNoteType: NoteType;
  snapshotIntervalSec: number;
  notificationsEnabled: boolean;
  hidePreviews: boolean;
  
  githubOwnerRepo: string;
  githubBranch: string;
  githubToken: string;
  appVersion: string;
  latestAvailableVersion: string;
  includePrereleases: boolean;
}

export type FilterTab = 'all' | 'pinned' | 'checklists' | 'archive' | 'trash';

export type TypeFilter = 'any' | 'text' | 'checklist' | 'voice' | 'image' | 'drawing' | 'scan';
export type DateRangeFilter = 'any' | '1d' | '7d' | '30d';
export type DateModeFilter = 'modified' | 'created';

export interface SearchFilters {
  type: TypeFilter;
  folder: string;
  dateRange: DateRangeFilter;
  dateMode: DateModeFilter;
}

export interface AppStrings {
  appTitle: string;
  searchPlaceholder: string;
  recentTitle: string;
  tabAll: string;
  tabPinned: string;
  tabChecklists: string;
  tabArchive: string;
  tabTrash: string;
  tabFilters: string;
  quickCapture: string;
  quickCaptureHint: string;
  newNote: string;
  untitledNote: string;
  checklistTag: string;
  wordsLabel: string;
  editedLabel: string;
  doneOfLabel: (done: number, total: number) => string;
  folderPlaceholder: string;
  tagsPlaceholder: string;
  bodyPlaceholder: string;
  addChecklistItem: string;
  voiceBtn: string;
  imageBtn: string;
  scanBtn: string;
  drawBtn: string;
  markdownPreview: string;
  editMode: string;
  historyTitle: string;
  versionsLabel: string;
  activityLogLabel: string;
  restoreBtn: string;
  shareBtn: string;
  copyBtn: string;
  deleteBtn: string;
  pinBtn: string;
  unpinBtn: string;
  duplicateBtn: string;
  archiveBtn: string;
  unarchiveBtn: string;
  savedPill: string;
  settingsTitle: string;
  themeLabel: string;
  accentLabel: string;
  fontLabel: string;
  importFontBtn: string;
  fontSizeLabel: string;
  languageLabel: string;
  compactModeLabel: string;
  animationsLabel: string;
  defaultTypeLabel: string;
  snapshotIntervalLabel: string;
  notificationsLabel: string;
  hidePreviewsLabel: string;
  storageUsageLabel: string;
  emptyTrashBtn: string;
  copyBackupBtn: string;
  restoreBackupBtn: string;
  
  aboutTitle: string;
  backendNotice: string;
  trashBanner: string;
  emptyNotesHint: string;
  undoLabel: string;
  clearBtn: string;
  attachBtn: string;
  cancelBtn: string;
  reminderTitle: string;
  clearReminderBtn: string;
  cropPhotoTitle: string;
  cropSaveBtn: string;
  rotateBtn: string;
  githubSyncTitle: string;
  githubPushApkBtn: string;
  testVoiceBtn: string;
}

export const TRANSLATIONS: Record<LanguageOption, AppStrings> = {
  en: {
    appTitle: "Notes",
    searchPlaceholder: "Search notes",
    recentTitle: "Recent",
    tabAll: "All",
    tabPinned: "Pinned",
    tabChecklists: "Checklists",
    tabArchive: "Archive",
    tabTrash: "Trash",
    tabFilters: "Filters",
    quickCapture: "Quick capture",
    quickCaptureHint: "Jot it down",
    newNote: "New note",
    untitledNote: "Untitled",
    checklistTag: "Checklist",
    wordsLabel: "words",
    editedLabel: "Edited",
    doneOfLabel: (done, total) => `${done} of ${total} done`,
    folderPlaceholder: "Folder",
    tagsPlaceholder: "Tags (comma separated)",
    bodyPlaceholder: "Start writing",
    addChecklistItem: "Add item",
    voiceBtn: "Voice",
    imageBtn: "Image",
    scanBtn: "Scan",
    drawBtn: "Draw",
    markdownPreview: "Markdown",
    editMode: "Edit",
    historyTitle: "History",
    versionsLabel: "Version history",
    activityLogLabel: "Activity",
    restoreBtn: "Restore",
    shareBtn: "Share",
    copyBtn: "Copy",
    deleteBtn: "Delete",
    pinBtn: "Pin",
    unpinBtn: "Unpin",
    duplicateBtn: "Duplicate",
    archiveBtn: "Archive",
    unarchiveBtn: "Restore from archive",
    savedPill: "Saved",
    settingsTitle: "Settings",
    themeLabel: "Theme",
    accentLabel: "Accent",
    fontLabel: "Font",
    importFontBtn: "Import font from Drive (.ttf/.otf)",
    fontSizeLabel: "Font size",
    languageLabel: "Language",
    compactModeLabel: "Compact mode",
    animationsLabel: "Animations",
    defaultTypeLabel: "Default note type",
    snapshotIntervalLabel: "Auto-save snapshot (sec)",
    notificationsLabel: "Notifications",
    hidePreviewsLabel: "Hide previews (privacy)",
    storageUsageLabel: "Storage",
    emptyTrashBtn: "Empty trash",
    copyBackupBtn: "Copy backup",
    restoreBackupBtn: "Restore backup",
    
    aboutTitle: "Notes, version 1.2.0",
    backendNotice: "Notes, version 1.2.0 (com.aistudio.glassnotes.vqpzxm). Data stays on this device in localStorage (with in-memory fallback). Use GitHub Sync above to push releases and JSON backups directly to your repository.",
    trashBanner: "Deleted · tap any note to restore",
    emptyNotesHint: "No notes yet. Tap + to write your first one.",
    undoLabel: "Undo",
    clearBtn: "Clear",
    attachBtn: "Attach",
    cancelBtn: "Cancel",
    reminderTitle: "Reminder",
    clearReminderBtn: "Clear reminder",
    cropPhotoTitle: "Crop Photo",
    cropSaveBtn: "Crop & Save",
    rotateBtn: "Rotate 90°",
    githubSyncTitle: "App Update & Version Notification",
    githubPushApkBtn: "Notify & Publish New Release",
    testVoiceBtn: "Test sound",
  },
  km: {
    appTitle: "កំណត់ត្រា",
    searchPlaceholder: "ស្វែងរកកំណត់ត្រា",
    recentTitle: "ថ្មីៗ",
    tabAll: "ទាំងអស់",
    tabPinned: "បានខ្ទាស់",
    tabChecklists: "បញ្ជី",
    tabArchive: "ប័ណ្ណសារ",
    tabTrash: "ធុងសំរាម",
    tabFilters: "តម្រង",
    quickCapture: "កត់ត្រារហ័ស",
    quickCaptureHint: "សរសេរគំនិតរបស់អ្នក...",
    newNote: "កំណត់ត្រាថ្មី",
    untitledNote: "គ្មានចំណងជើង",
    checklistTag: "បញ្ជី",
    wordsLabel: "ពាក្យ",
    editedLabel: "បានកែប្រែ",
    doneOfLabel: (done, total) => `${done} នៃ ${total} រួចរាល់`,
    folderPlaceholder: "ថតឯកសារ",
    tagsPlaceholder: "ស្លាក (ខណ្ឌដោយក្បៀស)",
    bodyPlaceholder: "ចាប់ផ្តើមសរសេរ",
    addChecklistItem: "បន្ថែមធាតុ",
    voiceBtn: "សំឡេង",
    imageBtn: "រូបភាព",
    scanBtn: "ស្កេន",
    drawBtn: "គំនូរ",
    markdownPreview: "Markdown",
    editMode: "កែសម្រួល",
    historyTitle: "ប្រវត្តិ",
    versionsLabel: "ប្រវត្តិកំណែ",
    activityLogLabel: "សកម្មភាព",
    restoreBtn: "ស្តារ",
    shareBtn: "ចែករំលែក",
    copyBtn: "ចម្លង",
    deleteBtn: "លុប",
    pinBtn: "ខ្ទាស់",
    unpinBtn: "ដោះខ្ទាស់",
    duplicateBtn: "ចម្លងទ្វេ",
    archiveBtn: "ទុកក្នុងប័ណ្ណសារ",
    unarchiveBtn: "ស្តារពីប័ណ្ណសារ",
    savedPill: "បានរក្សាទុក",
    settingsTitle: "ការកំណត់",
    themeLabel: "រូបរាង",
    accentLabel: "ពណ៌លម្អ",
    fontLabel: "ពុម្ពអក្សរ",
    importFontBtn: "នាំចូលពុម្ពអក្សរពី Drive (.ttf/.otf)",
    fontSizeLabel: "ទំហំអក្សរ",
    languageLabel: "ភាសា",
    compactModeLabel: "ទម្រង់បង្រួម",
    animationsLabel: "ចលនា",
    defaultTypeLabel: "ប្រភេទកំណត់ត្រាដើម",
    snapshotIntervalLabel: "រក្សាទុកកំណែស្វ័យប្រវត្តិ (វិនាទី)",
    notificationsLabel: "ការជូនដំណឹង",
    hidePreviewsLabel: "លាក់ខ្លឹមសារមើលជាមុន",
    storageUsageLabel: "ទំហំផ្ទុក",
    emptyTrashBtn: "សម្អាតធុងសំរាម",
    copyBackupBtn: "ចម្លងទិន្នន័យបម្រុង",
    restoreBackupBtn: "ស្តារទិន្នន័យបម្រុង",
    
    aboutTitle: "Notes, កំណែ 1.2.0",
    backendNotice: "ទិន្នន័យទាំងអស់រក្សាទុកក្នុងឧបករណ៍នេះ។ អ្នកអាចរុញឯកសារ APK និងទិន្នន័យបម្រុងទៅកាន់ GitHub ផ្ទាល់ពីផ្នែកខាងលើ។",
    trashBanner: "បានលុប · ចុចដើម្បីស្តារឡើងវិញ",
    emptyNotesHint: "មិនទាន់មានកំណត់ត្រាទេ។ ចុច + ដើម្បីសរសេរ។",
    undoLabel: "មិនធ្វើវិញ",
    clearBtn: "សម្អាត",
    attachBtn: "ភ្ជាប់",
    cancelBtn: "បោះបង់",
    reminderTitle: "ការរំលឹក",
    clearReminderBtn: "លុបការរំលឹក",
    cropPhotoTitle: "កាត់រូបភាព",
    cropSaveBtn: "កាត់ និងរក្សាទុក",
    rotateBtn: "បង្វិល 90°",
    githubSyncTitle: "អាប់ដេតកម្មវិធី និងការជូនដំណឹងកំណែថ្មី",
    githubPushApkBtn: "បញ្ចេញកំណែថ្មី និងជូនដំណឹង",
    testVoiceBtn: "សាកល្បងសំឡេង",
  },
};
