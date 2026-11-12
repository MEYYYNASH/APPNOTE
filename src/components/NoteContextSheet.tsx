import React from 'react';
import { Note, NoteColorId, NOTE_COLORS, AppStrings } from '../types/note';
import { Pin, Copy, Archive, RotateCcw, Trash2, Check } from 'lucide-react';

interface NoteContextSheetProps {
  note: Note;
  strings: AppStrings;
  onDismiss: () => void;
  onSelectColor: (color: NoteColorId) => void;
  onTogglePin: () => void;
  onDuplicate: () => void;
  onToggleArchive: () => void;
  onDelete: () => void;
}

export const NoteContextSheet: React.FC<NoteContextSheetProps> = ({
  note,
  strings,
  onDismiss,
  onSelectColor,
  onTogglePin,
  onDuplicate,
  onToggleArchive,
  onDelete,
}) => {
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

        <div className="px-1">
          <h3 className="font-bold text-base truncate">
            {note.title || strings.untitledNote}
          </h3>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            {note.folder ? `${note.folder} · ` : ''}
            {new Date(note.updatedAt).toLocaleDateString()}
          </p>
        </div>

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
          <button
            onClick={() => {
              onTogglePin();
              onDismiss();
            }}
            className="flex items-center gap-3 px-3.5 py-2.5 rounded-2xl glass-field hover:bg-black/5 dark:hover:bg-white/5 transition-all text-sm font-medium text-left"
          >
            <Pin size={17} className={note.isPinned ? 'text-blue-500 fill-blue-500' : ''} />
            <span>{note.isPinned ? strings.unpinBtn : strings.pinBtn}</span>
          </button>

          <button
            onClick={() => {
              onDuplicate();
              onDismiss();
            }}
            className="flex items-center gap-3 px-3.5 py-2.5 rounded-2xl glass-field hover:bg-black/5 dark:hover:bg-white/5 transition-all text-sm font-medium text-left"
          >
            <Copy size={17} />
            <span>{strings.duplicateBtn}</span>
          </button>

          <button
            onClick={() => {
              onToggleArchive();
              onDismiss();
            }}
            className="flex items-center gap-3 px-3.5 py-2.5 rounded-2xl glass-field hover:bg-black/5 dark:hover:bg-white/5 transition-all text-sm font-medium text-left"
          >
            {note.isArchived ? <RotateCcw size={17} /> : <Archive size={17} />}
            <span>{note.isArchived ? strings.unarchiveBtn : strings.archiveBtn}</span>
          </button>

          <button
            onClick={() => {
              onDelete();
              onDismiss();
            }}
            className="flex items-center gap-3 px-3.5 py-2.5 rounded-2xl glass-field text-red-500 hover:bg-red-500/10 transition-all text-sm font-semibold text-left"
          >
            <Trash2 size={17} />
            <span>{strings.deleteBtn}</span>
          </button>
        </div>
      </div>
    </div>
  );
};
