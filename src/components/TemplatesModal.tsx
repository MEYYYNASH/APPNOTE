import React, { useState } from 'react';
import { NOTE_TEMPLATES, NoteTemplate } from '../types/templates';
import { X, Check, Users, Briefcase, CheckSquare, GraduationCap, Sparkles, LayoutTemplate } from 'lucide-react';

interface TemplatesModalProps {
  onDismiss: () => void;
  onSelectTemplate: (template: NoteTemplate) => void;
}

export const TemplatesModal: React.FC<TemplatesModalProps> = ({
  onDismiss,
  onSelectTemplate,
}) => {
  const [selectedCategory, setSelectedCategory] = useState<string>('All');
  const categories = ['All', 'Work', 'Meeting', 'Personal', 'Study', 'Creative'];

  const filtered = selectedCategory === 'All'
    ? NOTE_TEMPLATES
    : NOTE_TEMPLATES.filter((t) => t.category === selectedCategory);

  const getIcon = (iconName: string) => {
    switch (iconName) {
      case 'Users': return Users;
      case 'Briefcase': return Briefcase;
      case 'CheckSquare': return CheckSquare;
      case 'GraduationCap': return GraduationCap;
      case 'Sparkles': return Sparkles;
      default: return LayoutTemplate;
    }
  };

  return (
    <div
      onClick={onDismiss}
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-md animate-in fade-in"
    >
      <div
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-xl max-h-[85vh] rounded-3xl glass-panel-elevated p-6 flex flex-col gap-4 shadow-2xl border border-white/40 overflow-hidden"
      >
        {/* Header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-2xl bg-gradient-to-tr from-blue-600 to-indigo-600 text-white flex items-center justify-center shadow-md">
              <LayoutTemplate size={18} />
            </div>
            <div>
              <h3 className="text-lg font-bold">Note Templates</h3>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Choose a pre-structured blueprint to jumpstart your note
              </p>
            </div>
          </div>
          <button
            onClick={onDismiss}
            className="w-8 h-8 rounded-full glass-field flex items-center justify-center hover:opacity-80"
          >
            <X size={16} />
          </button>
        </div>

        {/* Category Pill Filters */}
        <div className="flex items-center gap-1.5 overflow-x-auto pb-1 scrollbar-none">
          {categories.map((cat) => (
            <button
              key={cat}
              onClick={() => setSelectedCategory(cat)}
              className={`px-3 py-1.5 rounded-full text-xs font-semibold transition-all ${
                selectedCategory === cat
                  ? 'bg-blue-600 text-white shadow'
                  : 'glass-field hover:bg-black/5 dark:hover:bg-white/5 opacity-75'
              }`}
            >
              {cat}
            </button>
          ))}
        </div>

        {/* Templates Grid */}
        <div className="flex-1 overflow-y-auto space-y-2.5 pr-1 scrollbar-thin">
          {filtered.map((tmpl) => {
            const Icon = getIcon(tmpl.icon);
            return (
              <div
                key={tmpl.id}
                onClick={() => {
                  onSelectTemplate(tmpl);
                  onDismiss();
                }}
                className="group p-4 rounded-2xl glass-card border border-white/20 hover:border-blue-500/40 hover:bg-blue-500/5 transition-all cursor-pointer flex items-start justify-between gap-3"
              >
                <div className="flex items-start gap-3 min-w-0">
                  <div className="w-10 h-10 rounded-xl glass-field flex-none flex items-center justify-center text-blue-500 group-hover:scale-110 transition-transform">
                    <Icon size={20} />
                  </div>
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <h4 className="text-sm font-bold truncate group-hover:text-blue-500 transition-colors">
                        {tmpl.title}
                      </h4>
                      <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-blue-500/10 text-blue-500 border border-blue-500/20">
                        {tmpl.category}
                      </span>
                    </div>
                    <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 line-clamp-2">
                      {tmpl.description}
                    </p>
                  </div>
                </div>

                <button
                  type="button"
                  className="flex-none px-3 py-1.5 rounded-xl bg-blue-600/10 hover:bg-blue-600 text-blue-600 hover:text-white text-xs font-bold transition-all shadow-sm flex items-center gap-1"
                >
                  <Check size={13} />
                  <span>Use</span>
                </button>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};
