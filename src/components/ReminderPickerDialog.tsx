import React, { useState } from 'react';
import { AppStrings } from '../types/note';
import { Bell, X, Calendar } from 'lucide-react';

interface ReminderPickerDialogProps {
  currentReminderAt?: number | null;
  strings: AppStrings;
  isKhmer: boolean;
  onDismiss: () => void;
  onSetReminder: (timestamp: number | null) => void;
}

export const ReminderPickerDialog: React.FC<ReminderPickerDialogProps> = ({
  currentReminderAt,
  strings,
  isKhmer,
  onDismiss,
  onSetReminder,
}) => {
  const [customDateTime, setCustomDateTime] = useState('');

  const now = Date.now();
  const presets = [
    {
      label: isKhmer ? 'ក្នុង 10 វិនាទី (សាកល្បងសំឡេងរំលឹក)' : 'In 10 seconds (Test Alarm)',
      time: now + 10_000,
    },
    {
      label: isKhmer ? 'ក្នុង 15 នាទី' : 'In 15 minutes',
      time: now + 15 * 60_000,
    },
    {
      label: isKhmer ? 'ក្នុង 1 ម៉ោង' : 'In 1 hour',
      time: now + 60 * 60_000,
    },
    {
      label: isKhmer ? 'ស្អែកម៉ោង 9:00 ព្រឹក' : 'Tomorrow 9:00 AM',
      time: (() => {
        const d = new Date();
        d.setDate(d.getDate() + 1);
        d.setHours(9, 0, 0, 0);
        return d.getTime();
      })(),
    },
  ];

  const handleCustomSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!customDateTime) return;
    const ts = new Date(customDateTime).getTime();
    if (!isNaN(ts) && ts > Date.now()) {
      onSetReminder(ts);
      onDismiss();
    }
  };

  const formatTime = (ts: number) => {
    const d = new Date(ts);
    return d.toLocaleString([], {
      month: 'short',
      day: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
    });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm animate-in fade-in">
      <div className="w-full max-w-sm rounded-3xl glass-panel-elevated p-5 flex flex-col gap-3.5 shadow-2xl border border-white/40">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Bell size={18} className="text-blue-500" />
            <h3 className="text-lg font-bold">{strings.reminderTitle}</h3>
          </div>
          <button
            onClick={onDismiss}
            className="w-8 h-8 rounded-full glass-field flex items-center justify-center hover:opacity-80"
          >
            <X size={16} />
          </button>
        </div>

        {/* Current reminder info banner */}
        {currentReminderAt && (
          <div className="flex items-center justify-between p-3 rounded-2xl glass-field border border-blue-500/30 text-xs">
            <span className="font-semibold text-blue-600 dark:text-blue-400">
              {formatTime(currentReminderAt)}
            </span>
            <button
              onClick={() => {
                onSetReminder(null);
                onDismiss();
              }}
              className="text-red-500 font-bold hover:underline"
            >
              {strings.clearReminderBtn}
            </button>
          </div>
        )}

        {/* Quick Presets */}
        <div className="flex flex-col gap-2">
          {presets.map((preset, idx) => (
            <button
              key={idx}
              onClick={() => {
                onSetReminder(preset.time);
                onDismiss();
              }}
              className="flex items-center justify-between px-3.5 py-2.5 rounded-2xl glass-field hover:bg-blue-500/10 hover:border-blue-400 transition-all text-sm font-medium text-left"
            >
              <span>{preset.label}</span>
              <span className="text-xs text-slate-500 dark:text-slate-400">
                {formatTime(preset.time)}
              </span>
            </button>
          ))}
        </div>

        {/* Custom DateTime picker */}
        <form onSubmit={handleCustomSubmit} className="flex flex-col gap-2 pt-1 border-t border-white/20">
          <label className="text-xs font-semibold text-slate-500 dark:text-slate-400 flex items-center gap-1.5">
            <Calendar size={13} />
            <span>{isKhmer ? 'ជ្រើសរើសថ្ងៃ និងម៉ោងផ្ទាល់ខ្លួន' : 'Pick custom date & time'}</span>
          </label>
          <div className="flex gap-2">
            <input
              type="datetime-local"
              value={customDateTime}
              onChange={(e) => setCustomDateTime(e.target.value)}
              className="flex-1 px-3 py-2 rounded-xl glass-field text-xs font-medium focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            <button
              type="submit"
              disabled={!customDateTime}
              className="px-4 py-2 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 text-white text-xs font-bold disabled:opacity-50 hover:brightness-110 active:scale-95 transition-all"
            >
              Set
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
