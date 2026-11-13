import React from 'react';
import {
  X,
  Sparkles,
  Link,
  Table,
  Crop,
  FileEdit,
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
  version = 'v1.2.5',
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
                <h2 className="text-xl font-bold tracking-tight">Release v1.2.5</h2>
                <span className="px-2.5 py-0.5 rounded-full text-[10px] font-extrabold uppercase bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                  Latest Release
                </span>
              </div>
              <p className="text-xs text-slate-300 mt-0.5">
                Link Clickability Fix, Professional Interactive Tables & Sleek Flat Crop UI
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
          
          {/* Suite 1: Link Clickability Fix */}
          <div className="p-4 rounded-2xl glass-field border border-white/10 space-y-3">
            <div className="flex items-center gap-2.5 text-sky-400 font-bold text-base">
              <Link size={20} />
              <h3>1. Link Clickability Fix (v1.2.5)</h3>
            </div>
            <p className="text-xs text-slate-300 leading-relaxed">
              Resolved issues where tapping or clicking text links inside notes did not properly navigate or open URLs. Links are now fully clickable and interactive in both Read Mode and Edit Mode.
            </p>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs text-slate-300">
              <div className="flex items-center gap-2">
                <CheckCircle2 size={14} className="text-sky-400 flex-none" />
                <span>Instant link navigation in Reading Mode</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle2 size={14} className="text-sky-400 flex-none" />
                <span>Ctrl+Click / Cmd+Click links in Edit Mode</span>
              </div>
            </div>
          </div>

          {/* Suite 2: Professional Interactive Tables */}
          <div className="p-4 rounded-2xl glass-field border border-white/10 space-y-3">
            <div className="flex items-center gap-2.5 text-blue-400 font-bold text-base">
              <Table size={20} />
              <h3>2. Professional Interactive Table System</h3>
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
            </div>
          </div>

          {/* Suite 3: Sleek Flat Crop & Scan UI */}
          <div className="p-4 rounded-2xl glass-field border border-white/10 space-y-3">
            <div className="flex items-center gap-2.5 text-cyan-400 font-bold text-base">
              <Crop size={20} />
              <h3>3. Sleek Flat Crop & Scan UI (សំប៉ែត)</h3>
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
            </div>
          </div>

          {/* Conflict Resolution Note */}
          <div className="p-4 rounded-2xl bg-amber-500/10 border border-amber-500/30 text-xs text-amber-300 space-y-1.5">
            <div className="flex items-center gap-2 font-bold text-amber-400">
              <ShieldCheck size={16} />
              <span>Installation & Package Conflict Note</span>
            </div>
            <p>
              If your phone displays <em>"App not installed as package conflicts with an existing package"</em>, please uninstall the older v1.0 version first, then install <strong>v1.2.5</strong> for a smooth, conflict-free setup.
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
              href={`https://github.com/${githubOwnerRepo}/releases/download/v1.2.5/GlassNotes.apk`}
              download="GlassNotes-v1.2.5.apk"
              target="_blank"
              rel="noopener noreferrer"
              onClick={() => onDownloadApk?.()}
              className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 text-white text-xs font-bold shadow-lg hover:brightness-110 active:scale-95 transition-all"
            >
              <Download size={14} />
              <span>Download v1.2.5 APK</span>
            </a>
          </div>
        </div>
      </div>
    </div>
  );
};
