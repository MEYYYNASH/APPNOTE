import React, { useState } from 'react';
import { Note, NoteColorId, NOTE_COLORS, AppStrings, FolderItem } from '../types/note';
import { Pin, Copy, Archive, RotateCcw, Trash2, Check, FolderInput, Folder, ChevronRight, X } from 'lucide-react';

interface NoteContextSheetProps {
  note: Note;
  folders: FolderItem[];
  strings: AppStrings;
  onDismiss: () => void;
  onSelectColor: (color: NoteColorId) => void;
  onTogglePin: () => void;
  onDuplicate: () => void;
  onCopyNote: (targetFolder?: string) => void;
  onMoveNote: (targetFolder: string) => void;
  onToggleArchive: () => void;
  onDelete: () => void;
}

export const NoteContextSheet: React.FC<NoteContextSheetProps> = ({
  note,
  folders,
  strings,
  onDismiss,
  onSelectColor,
  onTogglePin,
  onDuplicate,
  onCopyNote,
  onMoveNote,
  onToggleArchive,
  onDelete,
}) => {
  const [showMovePicker, setShowMovePicker] = useState(false);
  const [showCopyPicker, setShowCopyPicker] = useState(false);

  const activeFolders = folders.filter((f) => !f.isTrashed);

  return (
    <div
      onClick={onDismiss}
      className="fixed inset-0 z-50 flex items-end sm:items-center justify-center p-3 sm:p-4 bg-black/40 backdrop-blur-sm animate-in fade-in"
    >
      <div
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-sm rounded-3xl glass-panel-elevated p-5 flex flex-col gap-4 shadow-2xl border border-white/40 animate-in slide-in-from-bottom-6"
      >
        {/* Handle */}
        <div className="w-10 h-1 rounded-full bg-slate-400/40 mx-auto" />

        <div className="px-1 flex items-start justify-between">
          <div className="min-w-0 flex-1">
            <h3 className="font-bold text-base truncate">
              {note.title || strings.untitledNote}
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              {note.folder ? `${note.folder} · ` : 'No folder · '}
              {new Date(note.updatedAt).toLocaleDateString()}
            </p>
          </div>
          <button
            onClick={onDismiss}
            className="w-7 h-7 rounded-full glass-field flex items-center justify-center hover:opacity-75 ml-2"
          >
            <X size={14} />
          </button>
        </div>

        {/* Folder Move/Copy Picker Overlay */}
        {showMovePicker ? (
          <div className="flex flex-col gap-2 animate-in fade-in">
            <div className="flex items-center justify-between pb-1 border-b border-white/10 text-xs font-bold text-blue-500">
              <span>Move Note to Folder:</span>
              <button
                onClick={() => setShowMovePicker(false)}
                className="text-slate-400 hover:text-slate-600"
              >
                Back
              </button>
            </div>
            <div className="max-h-48 overflow-y-auto space-y-1 pr-1 scrollbar-thin text-xs">
              <button
                onClick={() => {
                  onMoveNote('');
                  onDismiss();
                }}
                className={`w-full flex items-center justify-between p-2 rounded-xl text-left ${
                  !note.folder ? 'bg-blue-500/20 text-blue-500 font-bold' : 'glass-field hover:bg-black/5'
                }`}
              >
                <span>No Folder (Unorganized)</span>
                {!note.folder && <Check size={14} />}
              </button>
              {activeFolders.map((f) => (
                <button
                  key={f.id}
                  onClick={() => {
                    onMoveNote(f.name);
                    onDismiss();
                  }}
                  className={`w-full flex items-center justify-between p-2 rounded-xl text-left ${
                    note.folder === f.name ? 'bg-blue-500/20 text-blue-500 font-bold' : 'glass-field hover:bg-black/5'
                  }`}
                >
                  <div className="flex items-center gap-2 truncate">
                    <Folder size={14} style={{ color: f.color }} />
                    <span className="truncate">{f.name}</span>
                  </div>
                  {note.folder === f.name && <Check size={14} />}
                </button>
              ))}
            </div>
          </div>
        ) : showCopyPicker ? (
          <div className="flex flex-col gap-2 animate-in fade-in">
            <div className="flex items-center justify-between pb-1 border-b border-white/10 text-xs font-bold text-indigo-500">
              <span>Copy Note to Folder:</span>
              <button
                onClick={() => setShowCopyPicker(false)}
                className="text-slate-400 hover:text-slate-600"
              >
                Back
              </button>
            </div>
            <div className="max-h-48 overflow-y-auto space-y-1 pr-1 scrollbar-thin text-xs">
              <button
                onClick={() => {
                  onCopyNote('');
                  onDismiss();
                }}
                className="w-full flex items-center justify-between p-2 rounded-xl text-left glass-field hover:bg-black/5"
              >
                <span>Same Folder / Root</span>
              </button>
              {activeFolders.map((f) => (
                <button
                  key={f.id}
                  onClick={() => {
                    onCopyNote(f.name);
                    onDismiss();
                  }}
                  className="w-full flex items-center justify-between p-2 rounded-xl text-left glass-field hover:bg-black/5"
                >
                  <div className="flex items-center gap-2 truncate">
                    <Folder size={14} style={{ color: f.color }} />
                    <span className="truncate">{f.name}</span>
                  </div>
                </button>
              ))}
            </div>
          </div>
        ) : (
          <>
            {/* 6 Color Swatches */}
            <div className="flex items-center justify-between px-1 py-1">
              {(Object.keys(NOTE_COLORS) as NoteColorId[]).map((cid) => {
                const conf = NOTE_COLORS[cid];
                const isSelected = note.color === cid;
                return (
                  <button
                    key={cid}
                    onClick={() => {
                      onSelectColor(cid);
                      onDismiss();
                    }}
                    style={{
                      background: `linear-gradient(135deg, ${conf.startColor}, ${conf.endColor})`,
                    }}
                    className={`w-9 h-9 rounded-full flex items-center justify-center text-white transition-transform ${
                      isSelected ? 'ring-2 ring-white scale-110 shadow-md' : 'opacity-85 hover:opacity-100'
                    }`}
                  >
                    {isSelected && <Check size={16} />}
                  </button>
                );
              })}
            </div>

            {/* Action items */}
            <div className="flex flex-col gap-1.5 pt-1 border-t border-white/20">
              {/* Move Note to Folder */}
              <button
                onClick={() => setShowMovePicker(true)}
                className="flex items-center justify-between px-3.5 py-2 rounded-2xl glass-field hover:bg-black/5 dark:hover:bg-white/5 transition-all text-sm font-medium text-left"
              >
                <div className="flex items-center gap-3">
                  <FolderInput size={17} className="text-blue-500" />
                  <span>Move to Folder...</span>
                </div>
                <ChevronRight size={15} className="text-slate-400" />
              </button>

              {/* Copy Note to Folder */}
              <button
                onClick={() => setShowCopyPicker(true)}
                className="flex items-center justify-between px-3.5 py-2 rounded-2xl glass-field hover:bg-black/5 dark:hover:bg-white/5 transition-all text-sm font-medium text-left"
              >
                <div className="flex items-center gap-3">
                  <Copy size={17} className="text-indigo-500" />
                  <span>Copy to Folder...</span>
                </div>
                <ChevronRight size={15} className="text-slate-400" />
              </button>

              <button
                onClick={() => {
                  onTogglePin();
                  onDismiss();
                }}
                className="flex items-center gap-3 px-3.5 py-2 rounded-2xl glass-field hover:bg-black/5 dark:hover:bg-white/5 transition-all text-sm font-medium text-left"
              >
                <Pin size={17} className={note.isPinned ? 'text-amber-500 fill-amber-500' : ''} />
                <span>{note.isPinned ? strings.unpinBtn : strings.pinBtn}</span>
              </button>

              <button
                onClick={() => {
                  onDuplicate();
                  onDismiss();
                }}
                className="flex items-center gap-3 px-3.5 py-2 rounded-2xl glass-field hover:bg-black/5 dark:hover:bg-white/5 transition-all text-sm font-medium text-left"
              >
                <Copy size={17} />
                <span>{strings.duplicateBtn}</span>
              </button>

              <button
                onClick={() => {
                  onToggleArchive();
                  onDismiss();
                }}
                className="flex items-center gap-3 px-3.5 py-2 rounded-2xl glass-field hover:bg-black/5 dark:hover:bg-white/5 transition-all text-sm font-medium text-left"
              >
                {note.isArchived ? <RotateCcw size={17} /> : <Archive size={17} />}
                <span>{note.isArchived ? strings.unarchiveBtn : strings.archiveBtn}</span>
              </button>

              <button
                onClick={() => {
                  onDelete();
                  onDismiss();
                }}
                className="flex items-center gap-3 px-3.5 py-2 rounded-2xl glass-field text-red-500 hover:bg-red-500/10 transition-all text-sm font-semibold text-left"
              >
                <Trash2 size={17} />
                <span>{strings.deleteBtn}</span>
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
};
