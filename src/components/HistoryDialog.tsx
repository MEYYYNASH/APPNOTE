import React from 'react';
import { Note, NoteVersion, AppStrings } from '../types/note';
import { History, X, RotateCcw } from 'lucide-react';

interface HistoryDialogProps {
  note: Note;
  strings: AppStrings;
  onDismiss: () => void;
  onRestoreVersion: (version: NoteVersion) => void;
}

export const HistoryDialog: React.FC<HistoryDialogProps> = ({
  note,
  strings,
  onDismiss,
  onRestoreVersion,
}) => {
  const formatTime = (ts: number) => {
    return new Date(ts).toLocaleString([], {
      month: 'short',
      day: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
    });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm animate-in fade-in">
      <div className="w-full max-w-md rounded-3xl glass-panel-elevated p-5 flex flex-col gap-4 shadow-2xl border border-white/40 max-h-[85vh] overflow-hidden">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <History size={18} className="text-blue-500" />
            <h3 className="text-lg font-bold">{strings.historyTitle}</h3>
          </div>
          <button
            onClick={onDismiss}
            className="w-8 h-8 rounded-full glass-field flex items-center justify-center hover:opacity-80"
          >
            <X size={16} />
          </button>
        </div>

        <div className="overflow-y-auto pr-1 flex flex-col gap-4 flex-1">
          {/* Version history */}
          <div>
            <h4 className="text-xs font-bold uppercase tracking-wider text-blue-500 mb-2">
              {strings.versionsLabel} ({note.versions.length})
            </h4>
            {note.versions.length === 0 ? (
              <p className="text-xs text-slate-500 dark:text-slate-400 py-3 text-center">
                No past versions saved yet.
              </p>
            ) : (
              <div className="flex flex-col gap-2">
                {note.versions.slice(0, 10).map((ver) => (
                  <div
                    key={ver.id}
                    className="flex items-center justify-between p-3 rounded-2xl glass-card gap-3"
                  >
                    <div className="min-w-0 flex-1">
                      <p className="text-sm font-semibold truncate">
                        {ver.title || strings.untitledNote}
                      </p>
                      <p className="text-xs text-slate-500 dark:text-slate-400">
                        {formatTime(ver.timestamp)} · {ver.isChecklist ? 'Checklist' : 'Text'}
                      </p>
                    </div>
                    <button
                      onClick={() => {
                        onRestoreVersion(ver);
                        onDismiss();
                      }}
                      className="flex items-center gap-1 px-3 py-1.5 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 text-white text-xs font-bold shadow hover:brightness-110 active:scale-95 transition-all"
                    >
                      <RotateCcw size={13} />
                      <span>{strings.restoreBtn}</span>
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Activity log */}
          <div>
            <h4 className="text-xs font-bold uppercase tracking-wider text-blue-500 mb-2">
              {strings.activityLogLabel}
            </h4>
            <div className="flex flex-col gap-1.5">
              {note.activityLog.slice(0, 20).map((log) => (
                <div key={log.id} className="flex items-center justify-between text-xs py-1 px-2">
                  <span className="capitalize font-medium text-slate-700 dark:text-slate-300">
                    {log.action}
                  </span>
                  <span className="text-slate-400">{formatTime(log.timestamp)}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
