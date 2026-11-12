import React from 'react';
import { Check } from 'lucide-react';

interface DynamicIslandSavePillProps {
  visible: boolean;
  label: string;
}

export const DynamicIslandSavePill: React.FC<DynamicIslandSavePillProps> = ({ visible, label }) => {
  return (
    <div
      className={`fixed top-3 left-1/2 -translate-x-1/2 z-50 transition-all duration-300 pointer-events-none ${
        visible ? 'translate-y-0 opacity-100 scale-100' : '-translate-y-12 opacity-0 scale-90'
      }`}
    >
      <div className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-full bg-slate-900/90 dark:bg-white/90 text-white dark:text-slate-900 text-xs font-semibold shadow-lg backdrop-blur-md border border-white/20">
        <Check size={13} className="text-emerald-400 dark:text-emerald-600" />
        <span>{label}</span>
      </div>
    </div>
  );
};
