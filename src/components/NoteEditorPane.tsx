import React, { useState, useRef, useEffect } from 'react';
import {
  Note,
  NoteColorId,
  NOTE_COLORS,
  AppStrings,
  ChecklistItem,
  AttachmentType,
  NoteVersion,
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
} from 'lucide-react';
import { playBase64Audio, stopActiveAudio } from '../utils/audio';

interface NoteEditorPaneProps {
  note: Note | null;
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
  onOpenHistory: () => void;
  onRemoveAttachment: (attachmentId: string) => void;
  onShowToast: (msg: string) => void;
}

export const NoteEditorPane: React.FC<NoteEditorPaneProps> = ({
  note,
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
  onOpenHistory,
  onRemoveAttachment,
  onShowToast,
}) => {
  const [playingAudioId, setPlayingAudioId] = useState<string | null>(null);
  const [audioProgress, setAudioProgress] = useState(0);
  const fileInputRef = useRef<HTMLInputElement | null>(null);

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
  const doneItems = note.checklist.filter((i) => i.isDone).length;
  const checklistPercent = note.checklist.length
    ? Math.round((doneItems / note.checklist.length) * 100)
    : 0;

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
      // Convert text lines to checklist
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
      // Convert checklist back to text
      const body = note.checklist.map((i) => (i.isDone ? `✓ ${i.text}` : i.text)).join('\n');
      onUpdateNote(
        note.title,
        body,
        false,
        [],
        note.tags,
        note.folder,
        note.color,
        note.isPinned
      );
    }
  };

  // Checklist item modifications
  const handleCheckItemToggle = (itemId: string) => {
    const next = note.checklist.map((item) =>
      item.id === itemId ? { ...item, isDone: !item.isDone } : item
    );
    onUpdateNote(
      note.title,
      note.body,
      true,
      next,
      note.tags,
      note.folder,
      note.color,
      note.isPinned
    );
  };

  const handleCheckItemText = (itemId: string, text: string) => {
    const next = note.checklist.map((item) =>
      item.id === itemId ? { ...item, text } : item
    );
    onUpdateNote(
      note.title,
      note.body,
      true,
      next,
      note.tags,
      note.folder,
      note.color,
      note.isPinned
    );
  };

  const handleCheckItemAdd = (afterIdx?: number) => {
    const newItem: ChecklistItem = { id: `c_${Date.now()}_${Math.random()}`, text: '', isDone: false };
    const next = [...note.checklist];
    if (afterIdx !== undefined && afterIdx >= 0) {
      next.splice(afterIdx + 1, 0, newItem);
    } else {
      next.push(newItem);
    }
    onUpdateNote(
      note.title,
      note.body,
      true,
      next,
      note.tags,
      note.folder,
      note.color,
      note.isPinned
    );
  };

  const handleCheckItemRemove = (itemId: string) => {
    const next = note.checklist.filter((item) => item.id !== itemId);
    onUpdateNote(
      note.title,
      note.body,
      true,
      next,
      note.tags,
      note.folder,
      note.color,
      note.isPinned
    );
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
      () => onShowToast("Couldn't copy. Please select and copy text manually.")
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

  // Image file input handler
  const handleImageFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      const b64 = (reader.result as string).replace(/^data:image\/\w+;base64,/, '');
      onOpenPhotoCrop(b64);
    };
    reader.readAsDataURL(file);
    e.target.value = '';
  };

  // Simple Markdown renderer
  const renderMarkdown = (text: string) => {
    const lines = text.split('\n');
    return (
      <div className="prose dark:prose-invert max-w-none text-sm space-y-2 p-4">
        {lines.map((line, idx) => {
          if (line.startsWith('# ')) {
            return (
              <h1 key={idx} className="text-xl font-bold border-b pb-1">
                {line.slice(2)}
              </h1>
            );
          }
          if (line.startsWith('## ')) {
            return (
              <h2 key={idx} className="text-lg font-bold">
                {line.slice(3)}
              </h2>
            );
          }
          if (line.startsWith('### ')) {
            return (
              <h3 key={idx} className="text-base font-semibold">
                {line.slice(4)}
              </h3>
            );
          }
          if (line.startsWith('- ') || line.startsWith('* ')) {
            return (
              <li key={idx} className="ml-4 list-disc">
                {line.slice(2)}
              </li>
            );
          }
          if (line.trim() === '') {
            return <div key={idx} className="h-2" />;
          }
          return <p key={idx}>{line}</p>;
        })}
      </div>
    );
  };

  return (
    <main className="flex-1 flex flex-col h-full overflow-hidden glass-panel rounded-3xl z-10">
      {/* Hidden file input for uploading images */}
      <input
        type="file"
        ref={fileInputRef}
        accept="image/*"
        onChange={handleImageFileChange}
        className="hidden"
      />

      {/* Top Toolbar */}
      <div className="flex items-center gap-1 sm:gap-2 px-3 sm:px-4 py-3 border-b border-white/20 overflow-x-auto scrollbar-none flex-none">
        {showBackButton && (
          <button
            onClick={onBack}
            className="w-9 h-9 rounded-full glass-field flex items-center justify-center hover:opacity-85 active:scale-95 transition-all flex-none"
          >
            <ArrowLeft size={18} />
          </button>
        )}

        {/* 6 Color Swatches */}
        <div className="flex items-center gap-1.5 px-1 flex-none">
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

        {/* Pin toggle */}
        <button
          onClick={handleTogglePin}
          title={note.isPinned ? strings.unpinBtn : strings.pinBtn}
          className={`w-8 h-8 rounded-full glass-field flex items-center justify-center transition-all flex-none ${
            note.isPinned ? 'bg-blue-500 text-white shadow-sm' : 'hover:opacity-80'
          }`}
        >
          <Pin size={15} className={note.isPinned ? 'fill-white' : ''} />
        </button>

        {/* Note Type Toggle (Checklist <-> Text) */}
        <button
          onClick={handleToggleChecklist}
          title={strings.checklistTag}
          className={`w-8 h-8 rounded-full glass-field flex items-center justify-center transition-all flex-none ${
            note.isChecklist ? 'bg-indigo-500 text-white shadow-sm' : 'hover:opacity-80'
          }`}
        >
          <CheckSquare size={15} />
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
            {isMarkdownPreview ? <Edit3 size={15} /> : <Eye size={15} />}
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
          <Bell size={15} />
          {note.reminderAt && (
            <span className="absolute -top-1 -right-1 w-2.5 h-2.5 bg-red-500 rounded-full border border-white" />
          )}
        </button>

        {/* Voice Attachment */}
        <button
          onClick={onOpenVoiceRecorder}
          title={strings.voiceBtn}
          className="w-8 h-8 rounded-full glass-field flex items-center justify-center hover:opacity-80 active:scale-95 transition-all flex-none"
        >
          <Mic size={15} />
        </button>

        {/* Image Attachment (opens file picker) */}
        <button
          onClick={() => fileInputRef.current?.click()}
          title={strings.imageBtn}
          className="w-8 h-8 rounded-full glass-field flex items-center justify-center hover:opacity-80 active:scale-95 transition-all flex-none"
        >
          <ImageIcon size={15} />
        </button>

        {/* Drawing Canvas */}
        <button
          onClick={onOpenDrawingCanvas}
          title={strings.drawBtn}
          className="w-8 h-8 rounded-full glass-field flex items-center justify-center hover:opacity-80 active:scale-95 transition-all flex-none"
        >
          <PenTool size={15} />
        </button>

        {/* History / Snapshots */}
        <button
          onClick={onOpenHistory}
          title={strings.historyTitle}
          className="w-8 h-8 rounded-full glass-field flex items-center justify-center hover:opacity-80 active:scale-95 transition-all flex-none"
        >
          <History size={15} />
        </button>

        {/* Duplicate */}
        <button
          onClick={onDuplicateNote}
          title={strings.duplicateBtn}
          className="w-8 h-8 rounded-full glass-field flex items-center justify-center hover:opacity-80 active:scale-95 transition-all flex-none"
        >
          <Copy size={15} />
        </button>

        {/* Copy text */}
        <button
          onClick={handleCopyNote}
          title={strings.copyBtn}
          className="w-8 h-8 rounded-full glass-field flex items-center justify-center hover:opacity-80 active:scale-95 transition-all flex-none"
        >
          <Copy size={15} />
        </button>

        {/* Delete */}
        <button
          onClick={onDeleteNote}
          title={strings.deleteBtn}
          className="w-8 h-8 rounded-full glass-field text-red-500 hover:bg-red-500/10 active:scale-95 transition-all flex-none"
        >
          <Trash2 size={15} />
        </button>
      </div>

      {/* Editor Content Area */}
      <div className="flex-1 flex flex-col min-h-0 overflow-y-auto">
        {/* Title Input */}
        <div className="px-5 pt-4 pb-1">
          <input
            type="text"
            value={note.title}
            onChange={handleTitleChange}
            placeholder={strings.untitledNote}
            className="w-full text-2xl font-bold bg-transparent outline-none tracking-tight placeholder:text-slate-400"
          />
        </div>

        {/* Metadata Bar */}
        <div className="flex items-center gap-3 px-5 py-1 text-xs text-slate-500 dark:text-slate-400">
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
              {wordCount} {strings.wordsLabel} · {strings.editedLabel} {formatTime(note.updatedAt)}
            </span>
          )}

          {/* Active Reminder Pill */}
          {note.reminderAt && (
            <div className="flex items-center gap-1 px-2 py-0.5 rounded-full bg-amber-500/15 text-amber-600 dark:text-amber-400 font-semibold ml-auto">
              <Clock size={11} />
              <span>{new Date(note.reminderAt).toLocaleString([], { month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' })}</span>
            </div>
          )}
        </div>

        {/* Folder & Tags Sub-row */}
        <div className="flex items-center gap-2 px-5 py-1.5 text-xs">
          <div className="flex items-center gap-1 px-2 py-1 rounded-xl glass-field flex-1 max-w-[160px]">
            <Folder size={12} className="text-slate-400 flex-none" />
            <input
              type="text"
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
              placeholder={strings.folderPlaceholder}
              className="bg-transparent outline-none w-full text-xs"
            />
          </div>

          <div className="flex items-center gap-1 px-2 py-1 rounded-xl glass-field flex-1 max-w-[240px]">
            <Tag size={12} className="text-slate-400 flex-none" />
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
          <div className="px-5 py-2 flex flex-col gap-2">
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

        {/* Main Editor: Checklist or Textarea */}
        <div className="flex-1 px-5 py-2 flex flex-col min-h-[300px]">
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
                    onChange={(e) => handleCheckItemText(item.id, e.target.value)}
                    onKeyDown={(e) => handleCheckItemKeyDown(e, idx, item)}
                    placeholder="List item..."
                    className={`flex-1 bg-transparent outline-none text-sm leading-relaxed ${
                      item.isDone ? 'line-through text-slate-400' : ''
                    }`}
                  />

                  <button
                    onClick={() => handleCheckItemRemove(item.id)}
                    className="w-6 h-6 rounded-full flex items-center justify-center text-slate-400 hover:text-red-500 opacity-0 group-hover:opacity-100 transition-opacity flex-none"
                  >
                    <X size={14} />
                  </button>
                </div>
              ))}

              <button
                onClick={() => handleCheckItemAdd()}
                className="flex items-center gap-2 px-3 py-2 rounded-xl text-blue-500 hover:bg-blue-500/10 text-sm font-semibold transition-colors mt-2"
              >
                <Plus size={16} />
                <span>{strings.addChecklistItem}</span>
              </button>
            </div>
          ) : isMarkdownPreview ? (
            renderMarkdown(note.body)
          ) : (
            <textarea
              value={note.body}
              onChange={handleBodyChange}
              placeholder={strings.bodyPlaceholder}
              className="w-full flex-1 bg-transparent resize-none outline-none text-sm leading-relaxed placeholder:text-slate-400 min-h-[350px]"
            />
          )}
        </div>
      </div>
    </main>
  );
};
