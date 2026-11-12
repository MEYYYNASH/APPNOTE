import React, { useState, useRef, useEffect } from 'react';
import {
  Note,
  NoteColorId,
  NOTE_COLORS,
  AppStrings,
  ChecklistItem,
  AttachmentType,
  FolderItem,
} from '../types/note';
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
  Copy,
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
  Heading1,
  Heading2,
  Heading3,
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
    isPinned: boolean
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
  onDuplicateNote,
  onToggleMarkdown,
  onOpenReminderDialog,
  onOpenVoiceRecorder,
  onOpenImageUpload,
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

  // History Stack for Undo/Redo
  const [historyStack, setHistoryStack] = useState<string[]>([]);
  const [redoStack, setRedoStack] = useState<string[]>([]);

  // Formatting state
  const [textColor, setTextColor] = useState('#000000');
  const [fontSize, setFontSize] = useState('16px');
  const [textAlign, setTextAlign] = useState<'left' | 'center' | 'right'>('left');

  // Table Modal
  const [showTableModal, setShowTableModal] = useState(false);
  const [tableRows, setTableRows] = useState(3);
  const [tableCols, setTableCols] = useState(3);

  // Hyperlink Modal
  const [showLinkModal, setShowLinkModal] = useState(false);
  const [linkText, setLinkText] = useState('');
  const [linkUrl, setLinkUrl] = useState('');

  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const videoInputRef = useRef<HTMLInputElement | null>(null);
  const pdfInputRef = useRef<HTMLInputElement | null>(null);
  const textareaRef = useRef<HTMLTextAreaElement | null>(null);

  // Stop playback on unmount
  useEffect(() => {
    return () => {
      stopActiveAudio();
    };
  }, []);

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

  // Text formatting helpers for textarea insertion
  const applyFormatting = (before: string, after = before, defaultText = 'text') => {
    const textarea = textareaRef.current;
    if (!textarea) return;

    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const selected = note.body.substring(start, end) || defaultText;
    const replacement = `${before}${selected}${after}`;

    // Push to undo stack
    setHistoryStack((prev) => [...prev, note.body]);
    setRedoStack([]);

    const newBody = note.body.substring(0, start) + replacement + note.body.substring(end);
    onUpdateNote(
      note.title,
      newBody,
      note.isChecklist,
      note.checklist,
      note.tags,
      note.folder,
      note.color,
      note.isPinned
    );

    setTimeout(() => {
      textarea.focus();
      textarea.setSelectionRange(start + before.length, start + before.length + selected.length);
    }, 50);
  };

  const handleUndo = () => {
    if (historyStack.length === 0) return;
    const prev = historyStack[historyStack.length - 1];
    setRedoStack((r) => [...r, note.body]);
    setHistoryStack((h) => h.slice(0, -1));
    onUpdateNote(
      note.title,
      prev,
      note.isChecklist,
      note.checklist,
      note.tags,
      note.folder,
      note.color,
      note.isPinned
    );
  };

  const handleRedo = () => {
    if (redoStack.length === 0) return;
    const next = redoStack[redoStack.length - 1];
    setHistoryStack((h) => [...h, note.body]);
    setRedoStack((r) => r.slice(0, -1));
    onUpdateNote(
      note.title,
      next,
      note.isChecklist,
      note.checklist,
      note.tags,
      note.folder,
      note.color,
      note.isPinned
    );
  };

  // Insert Table Markdown Grid
  const handleInsertTable = () => {
    let header = '|';
    let divider = '|';
    for (let c = 1; c <= tableCols; c++) {
      header += ` Header ${c} |`;
      divider += ' --- |';
    }
    let rowsStr = '';
    for (let r = 1; r <= tableRows; r++) {
      let row = '|';
      for (let c = 1; c <= tableCols; c++) {
        row += ` Item ${r},${c} |`;
      }
      rowsStr += `\n${row}`;
    }
    const tableMarkdown = `\n\n${header}\n${divider}${rowsStr}\n\n`;
    applyFormatting('', '', tableMarkdown);
    setShowTableModal(false);
    onShowToast('Table inserted!');
  };

  // Insert Hyperlink
  const handleInsertLink = () => {
    if (!linkUrl) return;
    const text = linkText || linkUrl;
    applyFormatting('', '', `[${text}](${linkUrl})`);
    setShowLinkModal(false);
    setLinkText('');
    setLinkUrl('');
  };

  // Handlers for updating note fields
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

  const handleBodyChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    onUpdateNote(
      note.title,
      e.target.value,
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
        text: l.replace(/^[*-]\s+/, '').trim(),
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
        note.isPinned
      );
    } else {
      const body = note.checklist.map((i) => (i.isDone ? `[x] ${i.text}` : `- [ ] ${i.text}`)).join('\n');
      onUpdateNote(note.title, body, false, [], note.tags, note.folder, note.color, note.isPinned);
    }
  };

  // Checklist item actions
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

  const handleCopyNote = () => {
    const text =
      (note.title ? `${note.title}\n\n` : '') +
      (note.isChecklist
        ? note.checklist.map((i) => (i.isDone ? '[x] ' : '[ ] ') + i.text).join('\n')
        : note.body);

    navigator.clipboard.writeText(text).then(
      () => onShowToast('Copied to clipboard'),
      () => onShowToast("Couldn't copy text.")
    );
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

  // Video and PDF file upload handlers
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

  // Markdown renderer supporting Tables, Code blocks, Lists, and Links
  const renderMarkdown = (text: string) => {
    const lines = text.split('\n');
    let inTable = false;
    let tableRowsData: string[][] = [];

    return (
      <div className="prose dark:prose-invert max-w-none text-sm space-y-2 p-6 font-normal">
        {lines.map((line, idx) => {
          // Table parsing
          if (line.trim().startsWith('|') && line.trim().endsWith('|')) {
            const cells = line
              .split('|')
              .slice(1, -1)
              .map((c) => c.trim());
            if (cells.some((c) => c.includes('---'))) {
              return null; // separator row
            }
            return (
              <div key={idx} className="overflow-x-auto my-1">
                <table className="w-full border-collapse border border-white/30 text-xs rounded-xl overflow-hidden glass-card">
                  <tbody>
                    <tr className="border-b border-white/20">
                      {cells.map((cell, cIdx) => (
                        <td key={cIdx} className="p-2 border-r border-white/20 font-medium">
                          {cell}
                        </td>
                      ))}
                    </tr>
                  </tbody>
                </table>
              </div>
            );
          }

          if (line.startsWith('# ')) {
            return (
              <h1 key={idx} className="text-2xl font-bold border-b border-white/20 pb-1 mt-4">
                {line.slice(2)}
              </h1>
            );
          }
          if (line.startsWith('## ')) {
            return (
              <h2 key={idx} className="text-xl font-bold mt-3">
                {line.slice(3)}
              </h2>
            );
          }
          if (line.startsWith('### ')) {
            return (
              <h3 key={idx} className="text-lg font-semibold mt-2">
                {line.slice(4)}
              </h3>
            );
          }
          if (line.startsWith('> ')) {
            return (
              <blockquote key={idx} className="pl-3 border-l-4 border-blue-500 italic my-2 text-slate-500">
                {line.slice(2)}
              </blockquote>
            );
          }
          if (line.startsWith('```')) {
            return (
              <pre key={idx} className="p-3 rounded-xl bg-slate-900 text-cyan-400 font-mono text-xs my-2 overflow-x-auto">
                {line.slice(3)}
              </pre>
            );
          }
          if (line.startsWith('- ') || line.startsWith('* ')) {
            return (
              <li key={idx} className="ml-5 list-disc">
                {line.slice(2)}
              </li>
            );
          }
          if (/^\d+\.\s/.test(line)) {
            return (
              <li key={idx} className="ml-5 list-decimal">
                {line.replace(/^\d+\.\s/, '')}
              </li>
            );
          }
          if (line.trim() === '') {
            return <div key={idx} className="h-2" />;
          }

          // Link regex replace
          const linkMatch = line.match(/\[(.*?)\]\((.*?)\)/);
          if (linkMatch) {
            const [full, label, url] = linkMatch;
            const parts = line.split(full);
            return (
              <p key={idx}>
                {parts[0]}
                <a href={url} target="_blank" rel="noopener noreferrer" className="text-blue-500 underline font-semibold">
                  {label}
                </a>
                {parts[1]}
              </p>
            );
          }

          return <p key={idx}>{line}</p>;
        })}
      </div>
    );
  };

  return (
    <main
      className={`flex-1 flex flex-col h-full overflow-hidden glass-panel rounded-3xl z-10 transition-all ${
        isFullScreen ? 'fixed inset-2 z-50 rounded-3xl shadow-2xl' : ''
      }`}
    >
      {/* Hidden file inputs */}
      <input type="file" ref={fileInputRef} accept="image/*" onChange={(e) => {
        const file = e.target.files?.[0];
        if (!file) return;
        const reader = new FileReader();
        reader.onload = () => {
          const b64 = (reader.result as string).replace(/^data:image\/\w+;base64,/, '');
          onOpenPhotoCrop(b64);
        };
        reader.readAsDataURL(file);
        e.target.value = '';
      }} className="hidden" />

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

        {/* Markdown Preview Toggle */}
        {!note.isChecklist && (
          <button
            onClick={onToggleMarkdown}
            title={isMarkdownPreview ? strings.editMode : strings.markdownPreview}
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

      {/* Advanced Rich Text Formatting Toolbar (Visible in edit mode, hidden in reading mode or checklists) */}
      {!isReadingMode && !note.isChecklist && !isMarkdownPreview && (
        <div className="flex items-center gap-1 px-3 py-1.5 border-b border-white/15 bg-white/30 dark:bg-slate-900/30 overflow-x-auto scrollbar-none flex-none text-xs">
          {/* Undo / Redo */}
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

          {/* Bold, Italic, Underline, Strikethrough */}
          <button onClick={() => applyFormatting('**', '**', 'bold text')} title="Bold" className="w-7 h-7 rounded-lg glass-field flex items-center justify-center">
            <Bold size={13} />
          </button>
          <button onClick={() => applyFormatting('*', '*', 'italic text')} title="Italic" className="w-7 h-7 rounded-lg glass-field flex items-center justify-center">
            <Italic size={13} />
          </button>
          <button onClick={() => applyFormatting('<u>', '</u>', 'underlined text')} title="Underline" className="w-7 h-7 rounded-lg glass-field flex items-center justify-center">
            <Underline size={13} />
          </button>
          <button onClick={() => applyFormatting('~~', '~~', 'strikethrough text')} title="Strikethrough" className="w-7 h-7 rounded-lg glass-field flex items-center justify-center">
            <Strikethrough size={13} />
          </button>

          <span className="w-px h-4 bg-white/20 mx-1" />

          {/* Headings */}
          <button onClick={() => applyFormatting('# ', '', 'Heading 1')} title="Heading 1" className="w-7 h-7 rounded-lg glass-field flex items-center justify-center font-bold text-xs">
            H1
          </button>
          <button onClick={() => applyFormatting('## ', '', 'Heading 2')} title="Heading 2" className="w-7 h-7 rounded-lg glass-field flex items-center justify-center font-bold text-xs">
            H2
          </button>
          <button onClick={() => applyFormatting('### ', '', 'Heading 3')} title="Heading 3" className="w-7 h-7 rounded-lg glass-field flex items-center justify-center font-bold text-xs">
            H3
          </button>

          <span className="w-px h-4 bg-white/20 mx-1" />

          {/* Lists & Blockquote & Code */}
          <button onClick={() => applyFormatting('- ', '', 'List item')} title="Bullet List" className="w-7 h-7 rounded-lg glass-field flex items-center justify-center">
            <List size={13} />
          </button>
          <button onClick={() => applyFormatting('1. ', '', 'Numbered item')} title="Numbered List" className="w-7 h-7 rounded-lg glass-field flex items-center justify-center">
            <ListOrdered size={13} />
          </button>
          <button onClick={() => applyFormatting('> ', '', 'Quote text')} title="Blockquote" className="w-7 h-7 rounded-lg glass-field flex items-center justify-center">
            <Quote size={13} />
          </button>
          <button onClick={() => applyFormatting('```\n', '\n```', 'code block')} title="Code Block" className="w-7 h-7 rounded-lg glass-field flex items-center justify-center">
            <Code size={13} />
          </button>

          <span className="w-px h-4 bg-white/20 mx-1" />

          {/* Table & Hyperlink */}
          <button onClick={() => setShowTableModal(true)} title="Insert Table" className="w-7 h-7 rounded-lg glass-field flex items-center justify-center">
            <TableIcon size={13} />
          </button>
          <button onClick={() => setShowLinkModal(true)} title="Insert Link" className="w-7 h-7 rounded-lg glass-field flex items-center justify-center">
            <Link size={13} />
          </button>

          <span className="w-px h-4 bg-white/20 mx-1" />

          {/* Media Attachment shortcuts */}
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
        {/* Title Input */}
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

        {/* Metadata & Word/Char Counter Bar */}
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

          {/* Active Reminder Pill */}
          {note.reminderAt && (
            <div className="flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-amber-500/15 text-amber-600 dark:text-amber-400 font-semibold ml-auto">
              <Clock size={11} />
              <span>{new Date(note.reminderAt).toLocaleString([], { month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' })}</span>
            </div>
          )}
        </div>

        {/* Folder & Tags Sub-row */}
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

        {/* Attachments Section */}
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

              // Image / Scan / Drawing attachment
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

        {/* Main Editor: Checklist, Reading View, Markdown, or Textarea */}
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
            renderMarkdown(note.body)
          ) : (
            <textarea
              ref={textareaRef}
              value={note.body}
              onChange={handleBodyChange}
              placeholder={strings.bodyPlaceholder}
              className="w-full flex-1 bg-transparent resize-none outline-none text-sm sm:text-base leading-relaxed placeholder:text-slate-400 min-h-[400px]"
            />
          )}
        </div>
      </div>

      {/* Insert Table Modal */}
      {showTableModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm animate-in fade-in">
          <div className="w-full max-w-xs rounded-3xl glass-panel-elevated p-5 flex flex-col gap-4 shadow-2xl border border-white/40">
            <h4 className="font-bold text-base">Insert Table</h4>
            <div className="grid grid-cols-2 gap-3 text-xs">
              <label className="flex flex-col gap-1">
                <span className="font-semibold text-slate-500">Rows</span>
                <input
                  type="number"
                  min="1"
                  max="20"
                  value={tableRows}
                  onChange={(e) => setTableRows(parseInt(e.target.value) || 2)}
                  className="p-2 rounded-xl glass-field outline-none font-bold"
                />
              </label>
              <label className="flex flex-col gap-1">
                <span className="font-semibold text-slate-500">Columns</span>
                <input
                  type="number"
                  min="1"
                  max="10"
                  value={tableCols}
                  onChange={(e) => setTableCols(parseInt(e.target.value) || 2)}
                  className="p-2 rounded-xl glass-field outline-none font-bold"
                />
              </label>
            </div>
            <div className="grid grid-cols-2 gap-2 mt-1">
              <button onClick={() => setShowTableModal(false)} className="py-2 rounded-xl glass-field text-xs font-semibold">
                Cancel
              </button>
              <button
                onClick={handleInsertTable}
                className="py-2 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 text-white text-xs font-bold shadow"
              >
                Insert
              </button>
            </div>
          </div>
        </div>
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
