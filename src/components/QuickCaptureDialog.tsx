import React, { useState } from 'react';
import { AppStrings } from '../types/note';
import { Zap, X, Check } from 'lucide-react';

interface QuickCaptureDialogProps {
  strings: AppStrings;
  onDismiss: () => void;
  onSaveCapture: (text: string) => void;
}

export const QuickCaptureDialog: React.FC<QuickCaptureDialogProps> = ({
  strings,
  onDismiss,
  onSaveCapture,
}) => {
  const [text, setText] = useState('');

  const handleSave = () => {
    if (text.trim()) {
      onSaveCapture(text.trim());
      onDismiss();
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm animate-in fade-in">
      <div className="w-full max-w-sm rounded-3xl glass-panel-elevated p-5 flex flex-col gap-3.5 shadow-2xl border border-white/40">
        <div className="flex items-center gap-2">
          <Zap size={20} className="text-amber-500 fill-amber-500" />
          <h3 className="text-lg font-bold">{strings.quickCapture}</h3>
        </div>

        <textarea
          autoFocus
          rows={5}
          value={text}
          onChange={(e) => setText(e.target.value)}
          placeholder={strings.quickCaptureHint}
          className="w-full p-3.5 rounded-2xl glass-field text-sm resize-none focus:outline-none focus:ring-2 focus:ring-blue-500 leading-relaxed"
        />

        <div className="grid grid-cols-2 gap-3">
          <button
            onClick={onDismiss}
            className="flex items-center justify-center gap-1.5 py-2.5 rounded-xl glass-field text-sm font-medium hover:bg-black/5 dark:hover:bg-white/5 transition-colors"
          >
            <X size={16} />
            <span>{strings.cancelBtn}</span>
          </button>
          <button
            onClick={handleSave}
            disabled={!text.trim()}
            className="flex items-center justify-center gap-1.5 py-2.5 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 text-white text-sm font-semibold shadow-md disabled:opacity-50 hover:brightness-110 active:scale-95 transition-all"
          >
            <Check size={16} />
            <span>{strings.attachBtn}</span>
          </button>
        </div>
      </div>
    </div>
  );
};
