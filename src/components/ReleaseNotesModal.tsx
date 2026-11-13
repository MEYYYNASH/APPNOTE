import React from 'react';
import {
  X,
  Sparkles,
  Table,
  Crop,
  FileEdit,
  BellRing,
  Download,
  ExternalLink,
  CheckCircle2,
  ShieldCheck,
} from 'lucide-react';

interface ReleaseNotesModalProps {
  version?: string;
  isOpen: boolean;
  onClose: () => void;
  onDownloadApk?: () => void;
  githubOwnerRepo: string;
}

export const ReleaseNotesModal: React.FC<ReleaseNotesModalProps> = ({
  version = 'v1.2.02',
  isOpen,
  onClose,
  onDownloadApk,
  githubOwnerRepo,
}) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-md animate-in fade-in duration-200 select-none">
      <div className="relative w-full max-w-2xl max-h-[90vh] flex flex-col rounded-3xl glass-card border border-white/30 shadow-2xl overflow-hidden bg-slate-900/90 text-white">
        
        {/* Header Banner */}
        <div className="p-6 border-b border-white/10 flex items-start justify-between bg-gradient-to-r from-blue-600/30 via-indigo-600/30 to-purple-600/30">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-blue-500 to-indigo-500 flex items-center justify-center shadow-lg text-white">
              <Sparkles size={24} />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-xl font-bold tracking-tight">Features Preview v1.2.02</h2>
                <span className="px-2.5 py-0.5 rounded-full text-[10px] font-extrabold uppercase bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                  Latest v1.2.2 Release
                </span>
              </div>
              <p className="text-xs text-slate-300 mt-0.5">
                Professional Interactive Tables, Sleek Flat Crop UI, Immersive Read Mode & Advanced Grid System
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full glass-field flex items-center justify-center text-slate-400 hover:text-white hover:bg-white/10 transition-colors"
          >
            <X size={18} />
          </button>
        </div>

        {/* Scrollable Feature Sections */}
        <div className="flex-1 overflow-y-auto p-6 space-y-6 text-sm">
          
          {/* Suite 1: Professional Interactive Tables */}
          <div className="p-4 rounded-2xl glass-field border border-white/10 space-y-3">
            <div className="flex items-center gap-2.5 text-blue-400 font-bold text-base">
              <Table size={20} />
              <h3>1. Professional Interactive Table System (v1.2.02)</h3>
            </div>
            <p className="text-xs text-slate-300 leading-relaxed">
              Notion Database + Excel + Numbers inspired fully interactive visual grid system built directly into APPNOTE.
            </p>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs text-slate-300">
              <div className="flex items-center gap-2">
                <CheckCircle2 size={14} className="text-emerald-400 flex-none" />
                <span>Rich Cell Types: Text, Numbers, Dates, Checkboxes</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle2 size={14} className="text-emerald-400 flex-none" />
                <span>Status Badges & Dropdown Selections</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle2 size={14} className="text-emerald-400 flex-none" />
                <span>Currency, Progress Bars & Star Ratings</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle2 size={14} className="text-emerald-400 flex-none" />
                <span>Add, Delete & Rename Rows and Columns</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle2 size={14} className="text-emerald-400 flex-none" />
                <span>Sorting, Filtering & Instant Search</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle2 size={14} className="text-emerald-400 flex-none" />
                <span>Pre-built Templates & CSV Export</span>
              </div>
            </div>
          </div>

          {/* Suite 2: Sleek Flat Crop & Scan UI */}
          <div className="p-4 rounded-2xl glass-field border border-white/10 space-y-3">
            <div className="flex items-center gap-2.5 text-cyan-400 font-bold text-base">
              <Crop size={20} />
              <h3>2. Sleek Flat Crop & Scan UI (សំប៉ែត)</h3>
            </div>
            <p className="text-xs text-slate-300 leading-relaxed">
              Redesigned high-end minimalist crop editor with flat proportions, flush aspect ratio chips, and glowing corner handles.
            </p>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs text-slate-300">
              <div className="flex items-center gap-2">
                <CheckCircle2 size={14} className="text-cyan-400 flex-none" />
                <span>Ultra-flat modern minimalist aesthetic</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle2 size={14} className="text-cyan-400 flex-none" />
                <span>Aspect ratio presets (Free, 1:1, 4:3, 16:9)</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle2 size={14} className="text-cyan-400 flex-none" />
                <span>Precise rule-of-thirds grid preview</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle2 size={14} className="text-cyan-400 flex-none" />
                <span>Instant 90° rotation & high-contrast scan filter</span>
              </div>
            </div>
          </div>

          {/* Suite 3: Immersive Reading Mode */}
          <div className="p-4 rounded-2xl glass-field border border-white/10 space-y-3">
            <div className="flex items-center gap-2.5 text-purple-400 font-bold text-base">
              <FileEdit size={20} />
              <h3>3. Immersive Reading Mode & Typography</h3>
            </div>
            <p className="text-xs text-slate-300 leading-relaxed">
              Distraction-free reading environment with curated typography, generous padding, and polished glassmorphic styling.
            </p>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs text-slate-300">
              <div className="flex items-center gap-2">
                <CheckCircle2 size={14} className="text-purple-400 flex-none" />
                <span>Immersive reading container with slate gradient</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle2 size={14} className="text-purple-400 flex-none" />
                <span>Custom font styling & typography scaling</span>
              </div>
            </div>
          </div>

          {/* Conflict Resolution Note */}
          <div className="p-4 rounded-2xl bg-amber-500/10 border border-amber-500/30 text-xs text-amber-300 space-y-1.5">
            <div className="flex items-center gap-2 font-bold text-amber-400">
              <ShieldCheck size={16} />
              <span>Installation & Package Conflict Note</span>
            </div>
            <p>
              If your phone displays <em>"App not installed as package conflicts with an existing package"</em>, please uninstall the older v1.0 version first, then install <strong>v1.2.02</strong> for a smooth, conflict-free setup.
            </p>
          </div>
        </div>

        {/* Footer Actions */}
        <div className="p-5 border-t border-white/10 flex flex-wrap items-center justify-between gap-3 bg-slate-900/90">
          <a
            href={`https://github.com/${githubOwnerRepo}/releases`}
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center gap-1.5 text-xs text-slate-400 hover:text-white transition-colors"
          >
            <ExternalLink size={14} />
            <span>GitHub Releases Repository</span>
          </a>

          <div className="flex items-center gap-2">
            <button
              onClick={onClose}
              className="px-4 py-2 rounded-xl glass-field text-xs font-semibold hover:bg-white/10 transition-colors"
            >
              Close
            </button>
            <a
              href={`https://github.com/${githubOwnerRepo}/releases/download/v1.2.02/GlassNotes.apk`}
              download="GlassNotes-v1.2.02.apk"
              target="_blank"
              rel="noopener noreferrer"
              onClick={() => onDownloadApk?.()}
              className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 text-white text-xs font-bold shadow-lg hover:brightness-110 active:scale-95 transition-all"
            >
              <Download size={14} />
              <span>Download v1.2.02 APK</span>
            </a>
          </div>
        </div>
      </div>
    </div>
  );
};
