import React from 'react';

interface UndoToastBarProps {
  message: string | null;
  undoLabel?: string;
  onUndo?: () => void;
  onDismiss?: () => void;
}

export const UndoToastBar: React.FC<UndoToastBarProps> = ({
  message,
  undoLabel = 'Undo',
  onUndo,
  onDismiss,
}) => {
  if (!message) return null;

  return (
    <div className="fixed bottom-6 left-1/2 -translate-x-1/2 z-50 transition-all duration-300 animate-in fade-in slide-in-from-bottom-4">
      <div className="flex items-center gap-4 px-4 py-2.5 rounded-2xl glass-panel-elevated shadow-xl border border-white/30 text-sm max-w-sm">
        <span className="truncate">{message}</span>
        {onUndo && (
          <button
            onClick={() => {
              onUndo();
              onDismiss?.();
            }}
            className="font-bold text-blue-500 hover:text-blue-600 dark:text-blue-400 transition-colors"
          >
            {undoLabel}
          </button>
        )}
      </div>
    </div>
  );
};
