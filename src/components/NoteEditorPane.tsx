import React, { useState, useRef, useEffect, useCallback } from 'react';
import {
  Note,
  NoteColorId,
  NOTE_COLORS,
  AppStrings,
  ChecklistItem,
  AttachmentType,
  FolderItem,
  EditorFontStyleId,
} from '../types/note';
import { InteractiveTableEditor } from './InteractiveTableEditor';
import {
  ArrowLeft,
  Pin,
  CheckSquare,
  FileText,
  Eye,
  Edit3,
  Bell,
  Clock,
  Mic,
  Image as ImageIcon,
  Crop,
  PenTool,
  History,
  Trash2,
  Plus,
  X,
  Play,
  Pause,
  Folder,
  Tag,
  Check,
  Bold,
  Italic,
  Underline,
  Strikethrough,
  List,
  ListOrdered,
  Quote,
  Code,
  Link,
  Table as TableIcon,
  AlignLeft,
  AlignCenter,
  AlignRight,
  Maximize2,
  Minimize2,
  BookOpen,
  Camera,
  Video,
  FileCode,
  Download,
  RotateCcw,
  RotateCw,
  Highlighter,
  Palette,
  Type as TypeIcon,
  RemoveFormatting,
  ALargeSmall,
} from 'lucide-react';
import { playBase64Audio, stopActiveAudio } from '../utils/audio';

interface NoteEditorPaneProps {
  note: Note | null;
  folders: FolderItem[];
  showBackButton: boolean;
  isMarkdownPreview: boolean;
  strings: AppStrings;
  isKhmer: boolean;
  onBack: () => void;
  onUpdateNote: (
    title: string,
    body: string,
    isChecklist: boolean,
    checklist: ChecklistItem[],
    tags: string[],
    folder: string,
    color: NoteColorId,
    isPinned: boolean,
    extraRich?: Partial<Pick<Note, 'richHtml' | 'bodyFontStyle' | 'bodyTextColor' | 'bodyTextSize' | 'bodyTextAlign'>>
  ) => void;
  onDeleteNote: () => void;
  onDuplicateNote: () => void;
  onToggleMarkdown: () => void;
  onOpenReminderDialog: () => void;
  onOpenVoiceRecorder: () => void;
  onOpenImageUpload: () => void;
  onOpenPhotoCrop: (b64: string) => void;
  onOpenDrawingCanvas: () => void;
  onOpenScanner: () => void;
  onOpenHistory: () => void;
  onRemoveAttachment: (attachmentId: string) => void;
  onAddAttachment: (type: AttachmentType, name: string, dataBase64: string, durationSec?: number, extra?: any) => void;
  onShowToast: (msg: string) => void;
}

const FONT_STYLE_OPTIONS: {
  id: EditorFontStyleId;
  labelEn: string;
  labelKm: string;
  cssFamily: string;
}[] = [
  { id: 'sans', labelEn: 'Modern Sans', labelKm: 'អក្សរទំនើប', cssFamily: "'Figtree', system-ui, sans-serif" },
  { id: 'serif', labelEn: 'Editorial Serif', labelKm: 'អក្សរកាសែត', cssFamily: "'Playfair Display', Georgia, serif" },
  { id: 'mono', labelEn: 'Code Mono', labelKm: 'អក្សរកូដ', cssFamily: "'Roboto Mono', 'JetBrains Mono', monospace" },
  { id: 'handwritten', labelEn: 'Handwritten', labelKm: 'អក្សរសរសេរដៃ', cssFamily: "'Caveat', cursive" },
  { id: 'geometric', labelEn: 'Geometric', labelKm: 'អក្សរធរណីមាត្រ', cssFamily: "'Space Grotesk', sans-serif" },
];

const PRESET_COLORS = [
  '#FFFFFF',
  '#E2E8F0',
  '#0F172A',
  '#38BDF8',
  '#60A5FA',
  '#A855F7',
  '#EC4899',
  '#F43F5E',
  '#FB923C',
  '#FACC15',
  '#4ADE80',
  '#2DD4BF',
];

function resolveFontCss(styleId?: EditorFontStyleId): string {
  return FONT_STYLE_OPTIONS.find((f) => f.id === styleId)?.cssFamily || FONT_STYLE_OPTIONS[0].cssFamily;
}

function escapeHtml(raw: string): string {
  return raw
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function convertInlineMarkdownToHtml(text: string): string {
  let out = escapeHtml(text);
  out = out.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
  out = out.replace(/==(.*?)==/g, '<mark style="background-color:#facc1555;padding:0 2px;border-radius:3px;">$1</mark>');
  out = out.replace(/~~(.*?)~~/g, '<s>$1</s>');
  out = out.replace(/&lt;u&gt;(.*?)&lt;\/u&gt;/g, '<u>$1</u>');
  out = out.replace(/\*(.*?)\*/g, '<em>$1</em>');
  out = out.replace(/\[([^\]]+)\]\((https?:\/\/[^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer" style="color:#38bdf8;text-decoration:underline;">$1</a>');
  return out;
}

function convertLegacyBodyToRichHtml(rawBody: string): { cleanBody: string; html: string } {
  const cleanedRaw = rawBody.replace(/#st(ly|yl)e#/gi, '');
  const lines = cleanedRaw.split('\n');
  const htmlParts: string[] = [];
  const cleanLines: string[] = [];
  let inList: 'ul' | 'ol' | null = null;
  let inCode = false;

  const closeList = () => {
    if (inList) {
      htmlParts.push(`</${inList}>`);
      inList = null;
    }
  };

  for (const rawLine of lines) {
    const trimmed = rawLine.trim();
    if (trimmed.startsWith('```')) {
      closeList();
      if (!inCode) {
        inCode = true;
        htmlParts.push('<pre style="background:rgba(15,23,42,0.75);color:#22d3ee;padding:10px;border-radius:10px;font-family:monospace;">');
      } else {
        inCode = false;
        htmlParts.push('</pre>');
      }
      continue;
    }
    if (inCode) {
      cleanLines.push(rawLine);
      htmlParts.push(`${escapeHtml(rawLine)}\n`);
      continue;
    }

    if (trimmed.startsWith('|') && trimmed.endsWith('|')) {
      const cells = trimmed.slice(1, -1).split('|').map((c) => c.trim());
      if (cells.length > 0 && cells.every((c) => /^[:\-\s]+$/.test(c))) {
        continue;
      }
      closeList();
      cleanLines.push(cells.join(' • '));
      htmlParts.push(`<p>${cells.map((c) => convertInlineMarkdownToHtml(c)).join(' &nbsp;•&nbsp; ')}</p>`);
      continue;
    }

    if (trimmed.startsWith('### ')) {
      closeList();
      const text = trimmed.slice(4);
      cleanLines.push(text);
      htmlParts.push(`<h3>${convertInlineMarkdownToHtml(text)}</h3>`);
    } else if (trimmed.startsWith('## ')) {
      closeList();
      const text = trimmed.slice(3);
      cleanLines.push(text);
      htmlParts.push(`<h2>${convertInlineMarkdownToHtml(text)}</h2>`);
    } else if (trimmed.startsWith('# ')) {
      closeList();
      const text = trimmed.slice(2);
      cleanLines.push(text);
      htmlParts.push(`<h1>${convertInlineMarkdownToHtml(text)}</h1>`);
    } else if (trimmed.startsWith('> ')) {
      closeList();
      const text = trimmed.slice(2);
      cleanLines.push(text);
      htmlParts.push(`<blockquote>${convertInlineMarkdownToHtml(text)}</blockquote>`);
    } else if (trimmed.startsWith('- ') || trimmed.startsWith('* ') || trimmed.startsWith('• ')) {
      const text = trimmed.slice(2);
      cleanLines.push(`• ${text}`);
      if (inList !== 'ul') {
        closeList();
        inList = 'ul';
        htmlParts.push('<ul>');
      }
      htmlParts.push(`<li>${convertInlineMarkdownToHtml(text)}</li>`);
    } else if (/^\d+\.\s/.test(trimmed)) {
      const text = trimmed.replace(/^\d+\.\s/, '');
      cleanLines.push(trimmed);
      if (inList !== 'ol') {
        closeList();
        inList = 'ol';
        htmlParts.push('<ol>');
      }
      htmlParts.push(`<li>${convertInlineMarkdownToHtml(text)}</li>`);
    } else if (trimmed === '') {
      closeList();
      cleanLines.push('');
      htmlParts.push('<p><br></p>');
    } else {
      closeList();
      const strippedInline = rawLine
        .replace(/\*\*(.*?)\*\*/g, '$1')
        .replace(/==(.*?)==/g, '$1')
        .replace(/~~(.*?)~~/g, '$1')
        .replace(/<\/?u>/g, '')
        .replace(/\[([^\]]+)\]\([^)]+\)/g, '$1');
      cleanLines.push(strippedInline);
      htmlParts.push(`<p>${convertInlineMarkdownToHtml(rawLine)}</p>`);
    }
  }
  closeList();
  if (inCode) htmlParts.push('</pre>');

  return {
    cleanBody: cleanLines.join('\n'),
    html: htmlParts.join(''),
  };
}

export const NoteEditorPane: React.FC<NoteEditorPaneProps> = ({
  note,
  folders,
  showBackButton,
  isMarkdownPreview,
  strings,
  isKhmer,
  onBack,
  onUpdateNote,
  onDeleteNote,
  onToggleMarkdown,
  onOpenReminderDialog,
  onOpenVoiceRecorder,
  onOpenPhotoCrop,
  onOpenDrawingCanvas,
  onOpenScanner,
  onOpenHistory,
  onRemoveAttachment,
  onAddAttachment,
  onShowToast,
}) => {
  const [playingAudioId, setPlayingAudioId] = useState<string | null>(null);
  const [audioProgress, setAudioProgress] = useState(0);

  // Editor View Modes
  const [isReadingMode, setIsReadingMode] = useState(false);
  const [isFullScreen, setIsFullScreen] = useState(false);

  // Undo/Redo snapshots of { body, html }
  const [historyStack, setHistoryStack] = useState<{ body: string; html: string }[]>([]);
  const [redoStack, setRedoStack] = useState<{ body: string; html: string }[]>([]);

  // Modals: Custom Color Picker, Font Style Picker, Table, Link
  const [showColorModal, setShowColorModal] = useState(false);
  const [colorModalTab, setColorModalTab] = useState<'text' | 'highlight'>('text');
  const [pickedColor, setPickedColor] = useState('#38BDF8');

  const [showFontModal, setShowFontModal] = useState(false);

  const [showTableModal, setShowTableModal] = useState(false);
  const [tableRows, setTableRows] = useState(3);
  const [tableCols, setTableCols] = useState(3);

  const [showLinkModal, setShowLinkModal] = useState(false);
  const [linkText, setLinkText] = useState('');
  const [linkUrl, setLinkUrl] = useState('');

  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const videoInputRef = useRef<HTMLInputElement | null>(null);
  const pdfInputRef = useRef<HTMLInputElement | null>(null);
  const editorRef = useRef<HTMLDivElement | null>(null);
  const savedRangeRef = useRef<Range | null>(null);

  useEffect(() => {
    return () => {
      stopActiveAudio();
    };
  }, []);

  // Load initial HTML & auto-migrate any legacy markdown when switching notes
  useEffect(() => {
    if (!note || note.isChecklist || isReadingMode || isMarkdownPreview) return;
    const el = editorRef.current;
    if (!el) return;

    if (note.richHtml !== undefined) {
      if (el.innerHTML !== note.richHtml) {
        el.innerHTML = note.richHtml || '';
      }
      return;
    }

    const { cleanBody, html } = convertLegacyBodyToRichHtml(note.body || '');
    el.innerHTML = html;
    if (cleanBody !== note.body || html !== (note.richHtml || '')) {
      onUpdateNote(
        note.title,
        cleanBody,
        note.isChecklist,
        note.checklist,
        note.tags,
        note.folder,
        note.color,
        note.isPinned,
        { richHtml: html }
      );
    }
    // Only run on note switch or mode switch back into edit
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [note?.id, note?.isChecklist, isReadingMode, isMarkdownPreview]);

  const saveCurrentSelection = useCallback(() => {
    const sel = window.getSelection();
    if (sel && sel.rangeCount > 0 && editorRef.current?.contains(sel.anchorNode)) {
      savedRangeRef.current = sel.getRangeAt(0).cloneRange();
    }
  }, []);

  const restoreSavedSelection = useCallback(() => {
    const el = editorRef.current;
    if (!el) return;
    el.focus();
    const sel = window.getSelection();
    if (savedRangeRef.current && sel) {
      sel.removeAllRanges();
      sel.addRange(savedRangeRef.current);
    }
  }, []);

  const pushSnapshot = useCallback(() => {
    if (!note) return;
    const currentHtml = editorRef.current?.innerHTML ?? note.richHtml ?? '';
    setHistoryStack((prev) => [...prev.slice(-39), { body: note.body, html: currentHtml }]);
    setRedoStack([]);
  }, [note]);

  const syncFromEditorDom = useCallback(() => {
    if (!note || !editorRef.current) return;
    const html = editorRef.current.innerHTML;
    const plain = (editorRef.current.innerText || '').replace(/\u00A0/g, ' ');
    onUpdateNote(
      note.title,
      plain,
      note.isChecklist,
      note.checklist,
      note.tags,
      note.folder,
      note.color,
      note.isPinned,
      { richHtml: html }
    );
  }, [note, onUpdateNote]);

  const execWysiwygCommand = (command: string, value?: string) => {
    if (!note) return;
    pushSnapshot();
    restoreSavedSelection();
    document.execCommand(command, false, value);
    syncFromEditorDom();
    saveCurrentSelection();
  };

  const toggleBlockFormat = (tag: 'H1' | 'H2' | 'H3' | 'BLOCKQUOTE') => {
    if (!note) return;
    pushSnapshot();
    restoreSavedSelection();
    const current = (document.queryCommandValue('formatBlock') || '').toUpperCase();
    document.execCommand('formatBlock', false, current === tag ? 'P' : tag);
    syncFromEditorDom();
    saveCurrentSelection();
  };

  if (!note) {
    return (
      <main className="flex-1 flex flex-col items-center justify-center p-8 glass-panel rounded-3xl text-center text-slate-400">
        <FileText size={48} className="text-slate-300 dark:text-slate-600 mb-3" />
        <p className="text-sm font-medium">Select a note or tap + to create one.</p>
      </main>
    );
  }

  const formatTime = (ts: number) => {
    const d = new Date(ts);
    const today = new Date();
    if (d.toDateString() === today.toDateString()) {
      return d.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' });
    }
    return d.toLocaleDateString([], { month: 'short', day: 'numeric' });
  };

  const wordCount = note.body.trim() ? note.body.trim().split(/\s+/).length : 0;
  const charCount = note.body.length;
  const readTimeMin = Math.max(1, Math.ceil(wordCount / 200));

  const doneItems = note.checklist.filter((i) => i.isDone).length;
  const checklistPercent = note.checklist.length
    ? Math.round((doneItems / note.checklist.length) * 100)
    : 0;

  const activeFontId: EditorFontStyleId = note.bodyFontStyle || 'sans';
  const activeFontLabel = FONT_STYLE_OPTIONS.find((f) => f.id === activeFontId)?.labelEn || 'Sans';
  const activeFontCss = resolveFontCss(activeFontId);
  const activeTextSize = note.bodyTextSize || '16px';
  const activeTextAlign = note.bodyTextAlign || 'left';

  const handleUndo = () => {
    if (historyStack.length === 0 || !note) return;
    const prev = historyStack[historyStack.length - 1];
    const currentHtml = editorRef.current?.innerHTML ?? note.richHtml ?? '';
    setRedoStack((r) => [...r, { body: note.body, html: currentHtml }]);
    setHistoryStack((h) => h.slice(0, -1));
    if (editorRef.current) editorRef.current.innerHTML = prev.html;
    onUpdateNote(
      note.title,
      prev.body,
      note.isChecklist,
      note.checklist,
      note.tags,
      note.folder,
      note.color,
      note.isPinned,
      { richHtml: prev.html }
    );
  };

  const handleRedo = () => {
    if (redoStack.length === 0 || !note) return;
    const next = redoStack[redoStack.length - 1];
    const currentHtml = editorRef.current?.innerHTML ?? note.richHtml ?? '';
    setHistoryStack((h) => [...h, { body: note.body, html: currentHtml }]);
    setRedoStack((r) => r.slice(0, -1));
    if (editorRef.current) editorRef.current.innerHTML = next.html;
    onUpdateNote(
      note.title,
      next.body,
      note.isChecklist,
      note.checklist,
      note.tags,
      note.folder,
      note.color,
      note.isPinned,
      { richHtml: next.html }
    );
  };

  const handleApplyCustomColor = (hexColor: string | null, wholeNote = false) => {
    if (colorModalTab === 'text') {
      if (wholeNote || !savedRangeRef.current || savedRangeRef.current.collapsed) {
        onUpdateNote(
          note.title,
          note.body,
          note.isChecklist,
          note.checklist,
          note.tags,
          note.folder,
          note.color,
          note.isPinned,
          { bodyTextColor: hexColor || undefined }
        );
      } else {
        pushSnapshot();
        restoreSavedSelection();
        if (hexColor) {
          document.execCommand('foreColor', false, hexColor);
        } else {
          document.execCommand('removeFormat', false);
        }
        syncFromEditorDom();
      }
    } else {
      pushSnapshot();
      restoreSavedSelection();
      document.execCommand('hiliteColor', false, hexColor ? `${hexColor}55` : 'transparent');
      syncFromEditorDom();
    }
    setShowColorModal(false);
  };

  const handleSelectFontStyle = (styleId: EditorFontStyleId, applyScope: 'selection' | 'note') => {
    if (applyScope === 'note' || !savedRangeRef.current || savedRangeRef.current.collapsed) {
      onUpdateNote(
        note.title,
        note.body,
        note.isChecklist,
        note.checklist,
        note.tags,
        note.folder,
        note.color,
        note.isPinned,
        { bodyFontStyle: styleId }
      );
    } else {
      const css = resolveFontCss(styleId);
      pushSnapshot();
      restoreSavedSelection();
      document.execCommand('fontName', false, css);
      syncFromEditorDom();
    }
    setShowFontModal(false);
  };

  const handleInsertInlineCode = () => {
    pushSnapshot();
    restoreSavedSelection();
    const sel = window.getSelection();
    const selectedText = sel && !sel.isCollapsed ? sel.toString() : 'code snippet';
    const html = `<code style="background:rgba(56,189,248,0.16);color:#38bdf8;padding:2px 6px;border-radius:6px;font-family:'Roboto Mono',monospace;">${escapeHtml(selectedText)}</code>&nbsp;`;
    document.execCommand('insertHTML', false, html);
    syncFromEditorDom();
  };

  const handleInsertTable = () => {
    let html = '<div style="overflow-x:auto; margin:12px 0;"><table style="width:100%;border-collapse:collapse;border-radius:16px;overflow:hidden;border:1px solid rgba(255,255,255,0.2);background:rgba(0,0,0,0.2);"><thead><tr style="background:linear-gradient(135deg,rgba(37,99,235,0.3),rgba(79,70,229,0.3));">';
    for (let c = 1; c <= tableCols; c++) {
      html += `<th style="padding:10px 14px;text-align:left;border-bottom:1px solid rgba(255,255,255,0.2);color:#93c5fd;font-weight:800;font-size:11px;text-transform:uppercase;letter-spacing:0.05em;">Column ${c}</th>`;
    }
    html += '</tr></thead><tbody>';
    for (let r = 1; r <= tableRows; r++) {
      const bg = r % 2 === 0 ? 'background:rgba(255,255,255,0.04);' : '';
      html += `<tr style="${bg}">`;
      for (let c = 1; c <= tableCols; c++) {
        html += `<td style="padding:10px 14px;border-bottom:1px solid rgba(255,255,255,0.1);font-size:12px;color:#e2e8f0;">Item ${r}.${c}</td>`;
      }
      html += '</tr>';
    }
    html += '</tbody></table></div><p><br></p>';
    pushSnapshot();
    restoreSavedSelection();
    document.execCommand('insertHTML', false, html);
    syncFromEditorDom();
    setShowTableModal(false);
    onShowToast('Table inserted!');
  };

  const handleInsertLink = () => {
    if (!linkUrl) return;
    const cleanUrl = linkUrl.trim().startsWith('http') ? linkUrl.trim() : `https://${linkUrl.trim()}`;
    const label = linkText.trim() || cleanUrl.replace(/^https?:\/\//, '');
    const html = `<a href="${escapeHtml(cleanUrl)}" target="_blank" rel="noopener noreferrer" style="color:#38bdf8;text-decoration:underline;font-weight:600;">${escapeHtml(label)}</a>&nbsp;`;
    pushSnapshot();
    restoreSavedSelection();
    document.execCommand('insertHTML', false, html);
    syncFromEditorDom();
    setShowLinkModal(false);
    setLinkText('');
    setLinkUrl('');
  };

  const handleTitleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    onUpdateNote(
      e.target.value,
      note.body,
      note.isChecklist,
      note.checklist,
      note.tags,
      note.folder,
      note.color,
      note.isPinned
    );
  };

  const handleColorChange = (color: NoteColorId) => {
    onUpdateNote(
      note.title,
      note.body,
      note.isChecklist,
      note.checklist,
      note.tags,
      note.folder,
      color,
      note.isPinned
    );
  };

  const handleTogglePin = () => {
    onUpdateNote(
      note.title,
      note.body,
      note.isChecklist,
      note.checklist,
      note.tags,
      note.folder,
      note.color,
      !note.isPinned
    );
  };

  const handleToggleChecklist = () => {
    if (!note.isChecklist) {
      const lines = note.body.split('\n').filter((l) => l.trim().length > 0);
      const items: ChecklistItem[] = lines.map((l, idx) => ({
        id: `c_${Date.now()}_${idx}`,
        text: l.replace(/^[*•-]\s+/, '').trim(),
        isDone: false,
      }));
      onUpdateNote(
        note.title,
        '',
        true,
        items.length ? items : [{ id: `c_${Date.now()}`, text: '', isDone: false }],
        note.tags,
        note.folder,
        note.color,
        note.isPinned,
        { richHtml: '' }
      );
    } else {
      const body = note.checklist.map((i) => (i.isDone ? `[x] ${i.text}` : `• ${i.text}`)).join('\n');
      const html = `<ul>${note.checklist.map((i) => `<li>${i.isDone ? '<s>' : ''}${escapeHtml(i.text)}${i.isDone ? '</s>' : ''}</li>`).join('')}</ul>`;
      onUpdateNote(note.title, body, false, [], note.tags, note.folder, note.color, note.isPinned, { richHtml: html });
    }
  };

  const handleCheckItemToggle = (itemId: string) => {
    const next = note.checklist.map((item) =>
      item.id === itemId ? { ...item, isDone: !item.isDone } : item
    );
    onUpdateNote(note.title, note.body, true, next, note.tags, note.folder, note.color, note.isPinned);
  };

  const handleCheckItemText = (itemId: string, text: string) => {
    const next = note.checklist.map((item) =>
      item.id === itemId ? { ...item, text } : item
    );
    onUpdateNote(note.title, note.body, true, next, note.tags, note.folder, note.color, note.isPinned);
  };

  const handleCheckItemAdd = (afterIdx?: number) => {
    const newItem: ChecklistItem = { id: `c_${Date.now()}_${Math.random()}`, text: '', isDone: false };
    const next = [...note.checklist];
    if (afterIdx !== undefined && afterIdx >= 0) {
      next.splice(afterIdx + 1, 0, newItem);
    } else {
      next.push(newItem);
    }
    onUpdateNote(note.title, note.body, true, next, note.tags, note.folder, note.color, note.isPinned);
  };

  const handleCheckItemRemove = (itemId: string) => {
    const next = note.checklist.filter((item) => item.id !== itemId);
    onUpdateNote(note.title, note.body, true, next, note.tags, note.folder, note.color, note.isPinned);
  };

  const handleCheckItemKeyDown = (
    e: React.KeyboardEvent<HTMLInputElement>,
    idx: number,
    item: ChecklistItem
  ) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      handleCheckItemAdd(idx);
    } else if (e.key === 'Backspace' && item.text === '' && note.checklist.length > 1) {
      e.preventDefault();
      handleCheckItemRemove(item.id);
    }
  };

  const handleAudioPlayToggle = (attachmentId: string, b64: string) => {
    if (playingAudioId === attachmentId) {
      stopActiveAudio();
      setPlayingAudioId(null);
      setAudioProgress(0);
    } else {
      setPlayingAudioId(attachmentId);
      setAudioProgress(0.05);
      playBase64Audio(
        b64,
        (p) => setAudioProgress(p),
        () => {
          setPlayingAudioId(null);
          setAudioProgress(0);
        }
      );
    }
  };

  const handleVideoUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      const b64 = (reader.result as string).replace(/^data:video\/\w+;base64,/, '');
      onAddAttachment('video', file.name, b64, 0, { fileSize: file.size });
    };
    reader.readAsDataURL(file);
    e.target.value = '';
  };

  const handlePdfUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      const b64 = (reader.result as string).replace(/^data:application\/pdf;base64,/, '');
      onAddAttachment('pdf', file.name, b64, 0, { fileSize: file.size });
    };
    reader.readAsDataURL(file);
    e.target.value = '';
  };

  const previewHtml = note.richHtml ?? convertLegacyBodyToRichHtml(note.body || '').html;

  return (
    <main
      className={`flex-1 flex flex-col h-full overflow-hidden glass-panel rounded-3xl z-10 transition-all ${
        isFullScreen ? 'fixed inset-2 z-50 rounded-3xl shadow-2xl' : ''
      }`}
    >
      {/* Hidden file inputs */}
      <input
        type="file"
        ref={fileInputRef}
        accept="image/*"
        onChange={(e) => {
          const file = e.target.files?.[0];
          if (!file) return;
          const reader = new FileReader();
          reader.onload = () => {
            const b64 = (reader.result as string).replace(/^data:image\/\w+;base64,/, '');
            onOpenPhotoCrop(b64);
          };
          reader.readAsDataURL(file);
          e.target.value = '';
        }}
        className="hidden"
      />
      <input type="file" ref={videoInputRef} accept="video/*" onChange={handleVideoUpload} className="hidden" />
      <input type="file" ref={pdfInputRef} accept=".pdf,application/pdf" onChange={handlePdfUpload} className="hidden" />

      {/* Top Action Bar */}
      <div className="flex items-center gap-1.5 sm:gap-2 px-3 sm:px-4 py-2.5 border-b border-white/20 overflow-x-auto scrollbar-none flex-none">
        {showBackButton && (
          <button
            onClick={onBack}
            className="w-8 h-8 rounded-full glass-field flex items-center justify-center hover:opacity-85 active:scale-95 transition-all flex-none"
          >
            <ArrowLeft size={16} />
          </button>
        )}

        {/* 6 Color Swatches */}
        <div className="flex items-center gap-1 px-1 flex-none">
          {(Object.keys(NOTE_COLORS) as NoteColorId[]).map((cid) => {
            const conf = NOTE_COLORS[cid];
            const isSelected = note.color === cid;
            return (
              <button
                key={cid}
                onClick={() => handleColorChange(cid)}
                style={{
                  background: `linear-gradient(135deg, ${conf.startColor}, ${conf.endColor})`,
                }}
                className={`w-5 h-5 rounded-full transition-transform ${
                  isSelected ? 'ring-2 ring-white scale-125 shadow-sm' : 'opacity-80 hover:opacity-100'
                }`}
              />
            );
          })}
        </div>

        <div className="flex-1 min-w-2" />

        {/* Document Scanner Button */}
        <button
          onClick={onOpenScanner}
          title="Document Scanner"
          className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-gradient-to-r from-pink-600 to-rose-600 text-white text-xs font-bold shadow hover:brightness-110 active:scale-95 transition-all flex-none"
        >
          <Camera size={14} />
          <span className="hidden sm:inline">Scan</span>
        </button>

        {/* Reading Mode Toggle */}
        <button
          onClick={() => setIsReadingMode((v) => !v)}
          title="Reading Mode"
          className={`w-8 h-8 rounded-full glass-field flex items-center justify-center transition-all flex-none ${
            isReadingMode ? 'bg-amber-500 text-white shadow-sm' : 'hover:opacity-80'
          }`}
        >
          <BookOpen size={14} />
        </button>

        {/* Pin toggle */}
        <button
          onClick={handleTogglePin}
          title={note.isPinned ? strings.unpinBtn : strings.pinBtn}
          className={`w-8 h-8 rounded-full glass-field flex items-center justify-center transition-all flex-none ${
            note.isPinned ? 'bg-blue-500 text-white shadow-sm' : 'hover:opacity-80'
          }`}
        >
          <Pin size={14} className={note.isPinned ? 'fill-white' : ''} />
        </button>

        {/* Note Type Toggle (Checklist <-> Text) */}
        <button
          onClick={handleToggleChecklist}
          title={strings.checklistTag}
          className={`w-8 h-8 rounded-full glass-field flex items-center justify-center transition-all flex-none ${
            note.isChecklist ? 'bg-indigo-500 text-white shadow-sm' : 'hover:opacity-80'
          }`}
        >
          <CheckSquare size={14} />
        </button>

        {/* Rich View Toggle */}
        {!note.isChecklist && (
          <button
            onClick={onToggleMarkdown}
            title={isMarkdownPreview ? strings.editMode : 'Preview'}
            className={`w-8 h-8 rounded-full glass-field flex items-center justify-center transition-all flex-none ${
              isMarkdownPreview ? 'bg-purple-500 text-white shadow-sm' : 'hover:opacity-80'
            }`}
          >
            {isMarkdownPreview ? <Edit3 size={14} /> : <Eye size={14} />}
          </button>
        )}

        {/* Reminder Picker */}
        <button
          onClick={onOpenReminderDialog}
          title={strings.reminderTitle}
          className={`relative w-8 h-8 rounded-full glass-field flex items-center justify-center transition-all flex-none ${
            note.reminderAt ? 'bg-amber-500 text-white shadow-sm' : 'hover:opacity-80'
          }`}
        >
          <Bell size={14} />
          {note.reminderAt && (
            <span className="absolute -top-1 -right-1 w-2.5 h-2.5 bg-red-500 rounded-full border border-white" />
          )}
        </button>

        {/* History / Snapshots */}
        <button
          onClick={onOpenHistory}
          title={strings.historyTitle}
          className="w-8 h-8 rounded-full glass-field flex items-center justify-center hover:opacity-80 active:scale-95 transition-all flex-none"
        >
          <History size={14} />
        </button>

        {/* Full-Screen Toggle */}
        <button
          onClick={() => setIsFullScreen((v) => !v)}
          title={isFullScreen ? 'Exit Full Screen' : 'Full Screen'}
          className="w-8 h-8 rounded-full glass-field flex items-center justify-center hover:opacity-80 active:scale-95 transition-all flex-none"
        >
          {isFullScreen ? <Minimize2 size={14} /> : <Maximize2 size={14} />}
        </button>

        {/* Delete */}
        <button
          onClick={onDeleteNote}
          title={strings.deleteBtn}
          className="w-8 h-8 rounded-full glass-field text-red-500 hover:bg-red-500/10 active:scale-95 transition-all flex-none"
        >
          <Trash2 size={14} />
        </button>
      </div>

      {/* Live WYSIWYG Rich Formatting Bar (No literal markdown syntax inserted) */}
      {!isReadingMode && !note.isChecklist && !isMarkdownPreview && (
        <div
          onMouseDown={(e) => e.preventDefault()}
          className="flex items-center gap-1 px-3 py-1.5 border-b border-white/15 bg-white/30 dark:bg-slate-900/30 overflow-x-auto scrollbar-none flex-none text-xs"
        >
          <button
            onClick={handleUndo}
            disabled={historyStack.length === 0}
            title="Undo"
            className="w-7 h-7 rounded-lg glass-field flex items-center justify-center disabled:opacity-40"
          >
            <RotateCcw size={13} />
          </button>
          <button
            onClick={handleRedo}
            disabled={redoStack.length === 0}
            title="Redo"
            className="w-7 h-7 rounded-lg glass-field flex items-center justify-center disabled:opacity-40"
          >
            <RotateCw size={13} />
          </button>

          <span className="w-px h-4 bg-white/20 mx-1" />

          <button onClick={() => execWysiwygCommand('bold')} title="Bold" className="w-7 h-7 rounded-lg glass-field flex items-center justify-center">
            <Bold size={13} />
          </button>
          <button onClick={() => execWysiwygCommand('italic')} title="Italic" className="w-7 h-7 rounded-lg glass-field flex items-center justify-center">
            <Italic size={13} />
          </button>
          <button onClick={() => execWysiwygCommand('underline')} title="Underline" className="w-7 h-7 rounded-lg glass-field flex items-center justify-center">
            <Underline size={13} />
          </button>
          <button onClick={() => execWysiwygCommand('strikeThrough')} title="Strikethrough" className="w-7 h-7 rounded-lg glass-field flex items-center justify-center">
            <Strikethrough size={13} />
          </button>
          <button
            onClick={() => execWysiwygCommand('hiliteColor', '#FACC1555')}
            title="Highlight"
            className="px-2 h-7 rounded-lg glass-field flex items-center gap-1 font-semibold"
          >
            <Highlighter size={12} />
            <span>Highlight</span>
          </button>

          <span className="w-px h-4 bg-white/20 mx-1" />

          <button onClick={() => toggleBlockFormat('H1')} title="Heading 1" className="w-7 h-7 rounded-lg glass-field flex items-center justify-center font-bold">
            H1
          </button>
          <button onClick={() => toggleBlockFormat('H2')} title="Heading 2" className="w-7 h-7 rounded-lg glass-field flex items-center justify-center font-bold">
            H2
          </button>
          <button onClick={() => toggleBlockFormat('H3')} title="Heading 3" className="w-7 h-7 rounded-lg glass-field flex items-center justify-center font-bold">
            H3
          </button>

          <span className="w-px h-4 bg-white/20 mx-1" />

          {/* Font Style Picker Button (Real Typography, Never #style#) */}
          <button
            onClick={() => {
              saveCurrentSelection();
              setShowFontModal(true);
            }}
            title="Font Style"
            className="px-2.5 h-7 rounded-lg glass-field flex items-center gap-1.5 font-semibold"
          >
            <TypeIcon size={12} />
            <span>{activeFontLabel.split(' ').pop()}</span>
          </button>

          {/* Custom Color Picker Button (No Random Colors) */}
          <button
            onClick={() => {
              saveCurrentSelection();
              setPickedColor(note.bodyTextColor || '#38BDF8');
              setShowColorModal(true);
            }}
            title="Custom Text & Highlight Color"
            className="px-2.5 h-7 rounded-lg glass-field flex items-center gap-1.5 font-semibold"
          >
            <span
              className="w-3 h-3 rounded-full border border-white/70"
              style={{ backgroundColor: note.bodyTextColor || '#38BDF8' }}
            />
            <Palette size={12} />
            <span>Color</span>
          </button>

          <span className="w-px h-4 bg-white/20 mx-1" />

          <button onClick={() => execWysiwygCommand('insertUnorderedList')} title="Bullet List" className="w-7 h-7 rounded-lg glass-field flex items-center justify-center">
            <List size={13} />
          </button>
          <button onClick={() => execWysiwygCommand('insertOrderedList')} title="Numbered List" className="w-7 h-7 rounded-lg glass-field flex items-center justify-center">
            <ListOrdered size={13} />
          </button>
          <button onClick={() => toggleBlockFormat('BLOCKQUOTE')} title="Quote" className="w-7 h-7 rounded-lg glass-field flex items-center justify-center">
            <Quote size={13} />
          </button>
          <button onClick={handleInsertInlineCode} title="Code Snippet" className="w-7 h-7 rounded-lg glass-field flex items-center justify-center">
            <Code size={13} />
          </button>

          <button
            onClick={() => {
              saveCurrentSelection();
              setShowTableModal(true);
            }}
            title="Insert Table"
            className="w-7 h-7 rounded-lg glass-field flex items-center justify-center"
          >
            <TableIcon size={13} />
          </button>
          <button
            onClick={() => {
              saveCurrentSelection();
              const selText = window.getSelection()?.toString() || '';
              if (selText) setLinkText(selText);
              setShowLinkModal(true);
            }}
            title="Insert Link"
            className="w-7 h-7 rounded-lg glass-field flex items-center justify-center"
          >
            <Link size={13} />
          </button>

          <span className="w-px h-4 bg-white/20 mx-1" />

          {/* Text Size */}
          <button
            onClick={() => {
              const order = ['14px', '16px', '19px', '22px'];
              const nextSize = order[(order.indexOf(activeTextSize) + 1) % order.length];
              onUpdateNote(note.title, note.body, note.isChecklist, note.checklist, note.tags, note.folder, note.color, note.isPinned, {
                bodyTextSize: nextSize,
              });
            }}
            title="Text Size"
            className="px-2 h-7 rounded-lg glass-field flex items-center gap-1 font-semibold"
          >
            <ALargeSmall size={13} />
            <span>{activeTextSize}</span>
          </button>

          {/* Alignment */}
          <button
            onClick={() => {
              const order: ('left' | 'center' | 'right')[] = ['left', 'center', 'right'];
              const nextAlign = order[(order.indexOf(activeTextAlign) + 1) % order.length];
              onUpdateNote(note.title, note.body, note.isChecklist, note.checklist, note.tags, note.folder, note.color, note.isPinned, {
                bodyTextAlign: nextAlign,
              });
            }}
            title="Alignment"
            className="w-7 h-7 rounded-lg glass-field flex items-center justify-center"
          >
            {activeTextAlign === 'center' ? <AlignCenter size={13} /> : activeTextAlign === 'right' ? <AlignRight size={13} /> : <AlignLeft size={13} />}
          </button>

          {/* Clear Formatting */}
          <button
            onClick={() => {
              execWysiwygCommand('removeFormat');
              onUpdateNote(note.title, note.body, note.isChecklist, note.checklist, note.tags, note.folder, note.color, note.isPinned, {
                bodyFontStyle: 'sans',
                bodyTextColor: undefined,
                bodyTextSize: '16px',
                bodyTextAlign: 'left',
              });
            }}
            title="Clear Formatting"
            className="w-7 h-7 rounded-lg glass-field flex items-center justify-center"
          >
            <RemoveFormatting size={13} />
          </button>

          <span className="w-px h-4 bg-white/20 mx-1" />

          <button onClick={() => fileInputRef.current?.click()} title="Insert Image" className="w-7 h-7 rounded-lg glass-field flex items-center justify-center">
            <ImageIcon size={13} />
          </button>
          <button onClick={() => videoInputRef.current?.click()} title="Attach Video" className="w-7 h-7 rounded-lg glass-field flex items-center justify-center">
            <Video size={13} />
          </button>
          <button onClick={() => pdfInputRef.current?.click()} title="Attach PDF" className="w-7 h-7 rounded-lg glass-field flex items-center justify-center">
            <FileCode size={13} />
          </button>
          <button onClick={onOpenVoiceRecorder} title="Record Voice" className="w-7 h-7 rounded-lg glass-field flex items-center justify-center">
            <Mic size={13} />
          </button>
          <button onClick={onOpenDrawingCanvas} title="Drawing Canvas" className="w-7 h-7 rounded-lg glass-field flex items-center justify-center">
            <PenTool size={13} />
          </button>
        </div>
      )}

      {/* Editor Content Area */}
      <div className="flex-1 flex flex-col min-h-0 overflow-y-auto">
        <div className="px-6 pt-5 pb-1">
          <input
            type="text"
            value={note.title}
            onChange={handleTitleChange}
            readOnly={isReadingMode}
            placeholder={strings.untitledNote}
            className={`w-full text-2xl sm:text-3xl font-extrabold bg-transparent outline-none tracking-tight placeholder:text-slate-400 ${
              isReadingMode ? 'cursor-default' : ''
            }`}
          />
        </div>

        <div className="flex items-center gap-3 px-6 py-1 text-xs text-slate-500 dark:text-slate-400 flex-wrap">
          {note.isChecklist ? (
            <div className="flex items-center gap-2 flex-1 max-w-xs">
              <span className="font-semibold text-blue-500">
                {strings.doneOfLabel(doneItems, note.checklist.length)}
              </span>
              <div className="flex-1 h-1.5 rounded-full bg-slate-200 dark:bg-slate-700 overflow-hidden">
                <div
                  style={{ width: `${checklistPercent}%` }}
                  className="h-full bg-gradient-to-r from-blue-500 to-indigo-500 transition-all duration-300"
                />
              </div>
            </div>
          ) : (
            <span>
              {wordCount} words · {charCount} chars · {readTimeMin} min read · Edited {formatTime(note.updatedAt)}
            </span>
          )}

          {note.reminderAt && (
            <div className="flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-amber-500/15 text-amber-600 dark:text-amber-400 font-semibold ml-auto">
              <Clock size={11} />
              <span>{new Date(note.reminderAt).toLocaleString([], { month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' })}</span>
            </div>
          )}
        </div>

        <div className="flex items-center gap-2 px-6 py-2 text-xs flex-wrap">
          <div className="flex items-center gap-1.5 px-3 py-1 rounded-xl glass-field flex-1 max-w-[200px]">
            <Folder size={13} className="text-blue-500 flex-none" />
            <select
              value={note.folder}
              onChange={(e) =>
                onUpdateNote(
                  note.title,
                  note.body,
                  note.isChecklist,
                  note.checklist,
                  note.tags,
                  e.target.value,
                  note.color,
                  note.isPinned
                )
              }
              className="bg-transparent outline-none w-full text-xs font-semibold cursor-pointer"
            >
              <option value="">No folder</option>
              {folders.map((f) => (
                <option key={f.id} value={f.name}>
                  {f.name}
                </option>
              ))}
            </select>
          </div>

          <div className="flex items-center gap-1.5 px-3 py-1 rounded-xl glass-field flex-1 max-w-[260px]">
            <Tag size={13} className="text-slate-400 flex-none" />
            <input
              type="text"
              value={note.tags.join(', ')}
              onChange={(e) =>
                onUpdateNote(
                  note.title,
                  note.body,
                  note.isChecklist,
                  note.checklist,
                  e.target.value
                    .split(',')
                    .map((s) => s.trim())
                    .filter(Boolean),
                  note.folder,
                  note.color,
                  note.isPinned
                )
              }
              placeholder={strings.tagsPlaceholder}
              className="bg-transparent outline-none w-full text-xs"
            />
          </div>
        </div>

        {note.attachments.length > 0 && (
          <div className="px-6 py-2 flex flex-col gap-2">
            {note.attachments.map((att) => {
              if (att.type === 'voice') {
                const isPlaying = playingAudioId === att.id;
                return (
                  <div
                    key={att.id}
                    className="flex items-center gap-3 p-3 rounded-2xl glass-card border border-white/30"
                  >
                    <button
                      onClick={() => handleAudioPlayToggle(att.id, att.dataBase64)}
                      className="w-10 h-10 rounded-full bg-gradient-to-tr from-blue-500 to-indigo-600 text-white flex items-center justify-center shadow-md active:scale-95 transition-all"
                    >
                      {isPlaying ? <Pause size={17} /> : <Play size={17} className="ml-0.5" />}
                    </button>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between text-xs mb-1">
                        <span className="font-semibold truncate">{att.name}</span>
                        <span className="text-slate-400">{att.durationSec || 3}s</span>
                      </div>
                      <div className="h-1.5 rounded-full bg-slate-200 dark:bg-slate-700 overflow-hidden">
                        <div
                          style={{ width: `${(isPlaying ? audioProgress : 0) * 100}%` }}
                          className="h-full bg-blue-500 transition-all duration-100"
                        />
                      </div>
                    </div>
                    <button
                      onClick={() => onRemoveAttachment(att.id)}
                      className="w-7 h-7 rounded-full flex items-center justify-center text-slate-400 hover:text-red-500"
                    >
                      <X size={15} />
                    </button>
                  </div>
                );
              }

              if (att.type === 'video') {
                const pfx = att.dataBase64.startsWith('data:') ? '' : 'data:video/mp4;base64,';
                return (
                  <div key={att.id} className="relative rounded-2xl overflow-hidden glass-card max-w-md border border-white/30">
                    <video src={pfx + att.dataBase64} controls className="w-full max-h-60 rounded-t-2xl bg-black" />
                    <div className="flex items-center justify-between px-3 py-1.5 text-xs text-slate-400">
                      <span className="truncate">{att.name}</span>
                      <button onClick={() => onRemoveAttachment(att.id)} className="text-red-500 hover:underline">
                        Remove
                      </button>
                    </div>
                  </div>
                );
              }

              if (att.type === 'pdf') {
                const pfx = att.dataBase64.startsWith('data:') ? '' : 'data:application/pdf;base64,';
                return (
                  <div key={att.id} className="flex items-center justify-between p-3 rounded-2xl glass-card border border-white/30 text-xs">
                    <div className="flex items-center gap-2.5 truncate">
                      <div className="w-8 h-8 rounded-xl bg-red-500/15 text-red-500 flex items-center justify-center">
                        <FileCode size={16} />
                      </div>
                      <span className="font-semibold truncate">{att.name}</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <a
                        href={pfx + att.dataBase64}
                        download={att.name.endsWith('.pdf') ? att.name : `${att.name}.pdf`}
                        className="flex items-center gap-1 text-blue-500 hover:underline font-semibold"
                      >
                        <Download size={13} />
                        <span>Download</span>
                      </a>
                      <button onClick={() => onRemoveAttachment(att.id)} className="text-slate-400 hover:text-red-500 ml-2">
                        <X size={14} />
                      </button>
                    </div>
                  </div>
                );
              }

              const prefix = att.dataBase64.startsWith('data:') ? '' : 'data:image/png;base64,';
              return (
                <div
                  key={att.id}
                  className="relative group rounded-2xl overflow-hidden glass-card border border-white/30 max-w-sm"
                >
                  <img
                    src={prefix + att.dataBase64}
                    alt={att.name}
                    className="w-full max-h-56 object-cover"
                  />
                  <div className="absolute top-2 right-2 flex items-center gap-1.5">
                    {(att.type === 'image' || att.type === 'scan') && (
                      <button
                        onClick={() => onOpenPhotoCrop(att.dataBase64)}
                        title="Crop / Rotate"
                        className="w-7 h-7 rounded-full bg-black/60 text-white flex items-center justify-center backdrop-blur hover:bg-black/80"
                      >
                        <Crop size={13} />
                      </button>
                    )}
                    <button
                      onClick={() => onRemoveAttachment(att.id)}
                      title="Remove"
                      className="w-7 h-7 rounded-full bg-black/60 text-white flex items-center justify-center backdrop-blur hover:bg-red-600"
                    >
                      <X size={13} />
                    </button>
                  </div>
                  <div className="px-3 py-1.5 text-xs text-slate-500 dark:text-slate-400 bg-white/40 dark:bg-slate-900/40 backdrop-blur truncate">
                    {att.name} ({att.type})
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {/* Main Editor: Checklist, Rich Reading Preview, or Live WYSIWYG Rich Editor */}
        <div className="flex-1 px-6 py-2 flex flex-col min-h-[350px]">
          {note.isChecklist ? (
            <div className="space-y-1.5 pb-12">
              {note.checklist.map((item, idx) => (
                <div
                  key={item.id}
                  className="group flex items-center gap-3 p-2 rounded-xl hover:bg-white/30 dark:hover:bg-slate-800/30 transition-colors"
                >
                  <button
                    onClick={() => handleCheckItemToggle(item.id)}
                    className={`w-6 h-6 rounded-lg flex items-center justify-center transition-all flex-none ${
                      item.isDone
                        ? 'bg-gradient-to-tr from-blue-600 to-indigo-600 text-white shadow-sm'
                        : 'border-2 border-slate-400 hover:border-blue-500'
                    }`}
                  >
                    {item.isDone && <Check size={14} />}
                  </button>

                  <input
                    type="text"
                    value={item.text}
                    readOnly={isReadingMode}
                    onChange={(e) => handleCheckItemText(item.id, e.target.value)}
                    onKeyDown={(e) => handleCheckItemKeyDown(e, idx, item)}
                    placeholder="List item..."
                    className={`flex-1 bg-transparent outline-none text-sm leading-relaxed ${
                      item.isDone ? 'line-through text-slate-400' : ''
                    }`}
                  />

                  {!isReadingMode && (
                    <button
                      onClick={() => handleCheckItemRemove(item.id)}
                      className="w-6 h-6 rounded-full flex items-center justify-center text-slate-400 hover:text-red-500 opacity-0 group-hover:opacity-100 transition-opacity flex-none"
                    >
                      <X size={14} />
                    </button>
                  )}
                </div>
              ))}

              {!isReadingMode && (
                <button
                  onClick={() => handleCheckItemAdd()}
                  className="flex items-center gap-2 px-3 py-2 rounded-xl text-blue-500 hover:bg-blue-500/10 text-sm font-semibold transition-colors mt-2"
                >
                  <Plus size={16} />
                  <span>{strings.addChecklistItem}</span>
                </button>
              )}
            </div>
          ) : isReadingMode || isMarkdownPreview ? (
            <div className="max-w-4xl mx-auto p-6 sm:p-10 rounded-3xl glass-card border border-white/30 shadow-2xl bg-gradient-to-br from-slate-900/95 via-slate-900/80 to-indigo-950/90 text-white my-4">
              <div className="flex items-center gap-2 pb-4 mb-6 border-b border-white/10 text-xs font-bold text-amber-400 uppercase tracking-widest">
                <span className="w-2.5 h-2.5 rounded-full bg-amber-400 animate-pulse" />
                <span>Immersive Reading Mode</span>
              </div>
              <div
                style={{
                  fontFamily: activeFontCss,
                  fontSize: activeTextSize,
                  textAlign: activeTextAlign,
                  color: note.bodyTextColor || undefined,
                }}
                className="prose prose-invert max-w-none leading-relaxed [&_h1]:text-3xl [&_h1]:font-extrabold [&_h1]:text-transparent [&_h1]:bg-clip-text [&_h1]:bg-gradient-to-r [&_h1]:from-blue-400 [&_h1]:to-indigo-400 [&_h1]:my-4 [&_h2]:text-2xl [&_h2]:font-bold [&_h2]:text-indigo-300 [&_h2]:my-3 [&_h3]:text-xl [&_h3]:font-semibold [&_h3]:my-2 [&_p]:text-slate-200 [&_p]:leading-relaxed [&_p]:my-2.5 [&_ul]:list-disc [&_ul]:pl-6 [&_ol]:list-decimal [&_ol]:pl-6 [&_blockquote]:border-l-4 [&_blockquote]:border-blue-500 [&_blockquote]:pl-4 [&_blockquote]:italic [&_blockquote]:text-slate-300 [&_blockquote]:bg-blue-500/10 [&_blockquote]:py-2 [&_blockquote]:rounded-r-xl [&_blockquote]:my-3 [&_code]:px-2 [&_code]:py-1 [&_code]:rounded-lg [&_code]:bg-blue-500/20 [&_code]:text-cyan-300 [&_code]:font-mono [&_code]:text-xs [&_table]:w-full [&_table]:border-collapse [&_table]:my-4 [&_table]:overflow-hidden [&_table]:rounded-2xl [&_table]:border [&_table]:border-white/20 [&_table]:bg-black/20 [&_th]:bg-gradient-to-r [&_th]:from-blue-600/30 [&_th]:to-indigo-600/30 [&_th]:text-blue-200 [&_th]:font-extrabold [&_th]:px-4 [&_th]:py-3 [&_th]:border-b [&_th]:border-white/20 [&_th]:text-left [&_th]:text-xs [&_th]:uppercase [&_th]:tracking-wider [&_td]:px-4 [&_td]:py-3 [&_td]:border-b [&_td]:border-white/10 [&_td]:text-xs [&_tr:nth-child(even)]:[&_td]:bg-white/5 [&_tr:hover]:[&_td]:bg-white/10"
                dangerouslySetInnerHTML={{ __html: previewHtml }}
              />
            </div>
          ) : (
            <div
              ref={editorRef}
              contentEditable
              suppressContentEditableWarning
              onInput={syncFromEditorDom}
              onKeyUp={saveCurrentSelection}
              onMouseUp={saveCurrentSelection}
              onBlur={saveCurrentSelection}
              data-placeholder={strings.bodyPlaceholder}
              style={{
                fontFamily: activeFontCss,
                fontSize: activeTextSize,
                textAlign: activeTextAlign,
                color: note.bodyTextColor || undefined,
              }}
              className="w-full flex-1 outline-none leading-relaxed min-h-[400px] select-text empty:before:content-[attr(data-placeholder)] empty:before:text-slate-400 [&_h1]:text-2xl [&_h1]:font-extrabold [&_h1]:my-2 [&_h2]:text-xl [&_h2]:font-bold [&_h2]:my-1.5 [&_h3]:text-lg [&_h3]:font-semibold [&_h3]:my-1 [&_ul]:list-disc [&_ul]:pl-5 [&_ol]:list-decimal [&_ol]:pl-5 [&_blockquote]:border-l-4 [&_blockquote]:border-blue-500 [&_blockquote]:pl-3 [&_blockquote]:italic [&_blockquote]:my-1.5"
            />
          )}
        </div>
      </div>

      {/* Custom Color Picker Modal (Text Color & Highlight Color) */}
      {showColorModal && (
        <div
          onClick={() => setShowColorModal(false)}
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm animate-in fade-in"
        >
          <div
            onClick={(e) => e.stopPropagation()}
            className="w-full max-w-sm rounded-3xl glass-panel-elevated p-5 flex flex-col gap-3.5 shadow-2xl border border-white/40"
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2 font-bold text-base">
                <Palette size={17} className="text-blue-500" />
                <span>{isKhmer ? 'ជ្រើសរើសពណ៌ផ្ទាល់ខ្លួន' : 'Custom Text & Highlight Color'}</span>
              </div>
              <button onClick={() => setShowColorModal(false)} className="w-7 h-7 rounded-full glass-field flex items-center justify-center">
                <X size={14} />
              </button>
            </div>

            <div className="grid grid-cols-2 gap-2 text-xs">
              <button
                onClick={() => setColorModalTab('text')}
                className={`py-2 rounded-xl font-bold transition-all ${
                  colorModalTab === 'text' ? 'bg-blue-600 text-white shadow' : 'glass-field'
                }`}
              >
                Text Color
              </button>
              <button
                onClick={() => setColorModalTab('highlight')}
                className={`py-2 rounded-xl font-bold transition-all ${
                  colorModalTab === 'highlight' ? 'bg-blue-600 text-white shadow' : 'glass-field'
                }`}
              >
                Highlight Color
              </button>
            </div>

            <div className="flex flex-wrap gap-2">
              <button
                onClick={() => handleApplyCustomColor(null, true)}
                className="w-8 h-8 rounded-full glass-field border border-white/30 flex items-center justify-center text-xs font-bold"
                title="Reset to default"
              >
                <RemoveFormatting size={13} />
              </button>
              {PRESET_COLORS.map((hex) => (
                <button
                  key={hex}
                  onClick={() => setPickedColor(hex)}
                  style={{ backgroundColor: hex }}
                  className={`w-8 h-8 rounded-full transition-transform ${
                    pickedColor.toUpperCase() === hex.toUpperCase()
                      ? 'ring-2 ring-white scale-110 shadow'
                      : 'border border-white/30'
                  }`}
                />
              ))}
            </div>

            <div className="flex items-center gap-2.5 text-xs">
              <input
                type="color"
                value={pickedColor}
                onChange={(e) => setPickedColor(e.target.value.toUpperCase())}
                className="w-11 h-9 rounded-xl cursor-pointer bg-transparent border-0"
              />
              <input
                type="text"
                value={pickedColor}
                onChange={(e) => setPickedColor(e.target.value)}
                placeholder="#38BDF8"
                className="flex-1 p-2 rounded-xl glass-field outline-none font-mono font-bold"
              />
              <span
                className="px-3 py-1.5 rounded-xl font-semibold text-xs"
                style={{
                  color: colorModalTab === 'text' ? pickedColor : undefined,
                  backgroundColor: colorModalTab === 'highlight' ? `${pickedColor}55` : 'rgba(255,255,255,0.08)',
                }}
              >
                Sample
              </span>
            </div>

            <div className="grid grid-cols-2 gap-2 mt-1">
              {colorModalTab === 'text' ? (
                <>
                  <button
                    onClick={() => handleApplyCustomColor(pickedColor, false)}
                    className="py-2 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 text-white text-xs font-bold shadow"
                  >
                    Apply to Selection
                  </button>
                  <button
                    onClick={() => handleApplyCustomColor(pickedColor, true)}
                    className="py-2 rounded-xl glass-field text-xs font-bold"
                  >
                    Apply to Note
                  </button>
                </>
              ) : (
                <>
                  <button
                    onClick={() => handleApplyCustomColor(null)}
                    className="py-2 rounded-xl glass-field text-xs font-semibold"
                  >
                    Clear Highlight
                  </button>
                  <button
                    onClick={() => handleApplyCustomColor(pickedColor)}
                    className="py-2 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 text-white text-xs font-bold shadow"
                  >
                    Apply Highlight
                  </button>
                </>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Font Style Picker Modal */}
      {showFontModal && (
        <div
          onClick={() => setShowFontModal(false)}
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm animate-in fade-in"
        >
          <div
            onClick={(e) => e.stopPropagation()}
            className="w-full max-w-sm rounded-3xl glass-panel-elevated p-5 flex flex-col gap-3 shadow-2xl border border-white/40"
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2 font-bold text-base">
                <TypeIcon size={17} className="text-blue-500" />
                <span>{isKhmer ? 'រចនាប័ទ្មពុម្ពអក្សរ' : 'Font Style'}</span>
              </div>
              <button onClick={() => setShowFontModal(false)} className="w-7 h-7 rounded-full glass-field flex items-center justify-center">
                <X size={14} />
              </button>
            </div>

            <div className="space-y-2 max-h-[60vh] overflow-y-auto">
              {FONT_STYLE_OPTIONS.map((font) => {
                const isSelected = activeFontId === font.id;
                const hasSelection = Boolean(savedRangeRef.current && !savedRangeRef.current.collapsed);
                return (
                  <button
                    key={font.id}
                    onClick={() => handleSelectFontStyle(font.id, hasSelection ? 'selection' : 'note')}
                    className={`w-full text-left p-3 rounded-2xl border transition-all flex items-center justify-between ${
                      isSelected
                        ? 'bg-blue-500/15 border-blue-500/50'
                        : 'glass-field border-white/20 hover:bg-white/10'
                    }`}
                  >
                    <div>
                      <div style={{ fontFamily: font.cssFamily }} className="font-bold text-base">
                        {isKhmer ? font.labelKm : font.labelEn}
                      </div>
                      <div style={{ fontFamily: font.cssFamily }} className="text-xs opacity-70">
                        The quick glass note captures every idea.
                      </div>
                    </div>
                    {isSelected && <Check size={16} className="text-blue-500 flex-none" />}
                  </button>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {showTableModal && (
        <InteractiveTableEditor
          strings={strings}
          onClose={() => setShowTableModal(false)}
          onSave={(_data, html) => {
            pushSnapshot();
            restoreSavedSelection();
            document.execCommand('insertHTML', false, html);
            syncFromEditorDom();
            setShowTableModal(false);
            onShowToast('Interactive Table inserted!');
          }}
        />
      )}

      {/* Insert Hyperlink Modal */}
      {showLinkModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm animate-in fade-in">
          <div className="w-full max-w-sm rounded-3xl glass-panel-elevated p-5 flex flex-col gap-3.5 shadow-2xl border border-white/40">
            <h4 className="font-bold text-base">Insert Hyperlink</h4>
            <input
              type="text"
              placeholder="Display text (e.g. Website)"
              value={linkText}
              onChange={(e) => setLinkText(e.target.value)}
              className="p-2.5 rounded-xl glass-field text-xs outline-none"
            />
            <input
              type="url"
              placeholder="URL (https://...)"
              value={linkUrl}
              onChange={(e) => setLinkUrl(e.target.value)}
              className="p-2.5 rounded-xl glass-field text-xs outline-none"
            />
            <div className="grid grid-cols-2 gap-2 mt-1">
              <button onClick={() => setShowLinkModal(false)} className="py-2 rounded-xl glass-field text-xs font-semibold">
                Cancel
              </button>
              <button
                onClick={handleInsertLink}
                disabled={!linkUrl}
                className="py-2 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 text-white text-xs font-bold shadow disabled:opacity-50"
              >
                Add Link
              </button>
            </div>
          </div>
        </div>
      )}
    </main>
  );
};
