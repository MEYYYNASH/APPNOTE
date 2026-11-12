import React from 'react';
import {
  X,
  Sparkles,
  FolderTree,
  ScanLine,
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
  version = 'v1.1.18',
  isOpen,
  onClose,
  onDownloadApk,
  githubOwnerRepo,
}) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-md animate-in fade-in duration-200">
      <div className="relative w-full max-w-2xl max-h-[90vh] flex flex-col rounded-3xl glass-card border border-white/30 shadow-2xl overflow-hidden bg-slate-900/80 text-white">
        
        {/* Header Banner */}
        <div className="p-6 border-b border-white/10 flex items-start justify-between bg-gradient-to-r from-blue-600/30 via-indigo-600/30 to-purple-600/30">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-blue-500 to-indigo-500 flex items-center justify-center shadow-lg text-white">
              <Sparkles size={24} />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-xl font-bold tracking-tight">What's New in {version}</h2>
                <span className="px-2 py-0.5 rounded-full text-[10px] font-extrabold uppercase bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                  Latest Release
                </span>
              </div>
              <p className="text-xs text-slate-300 mt-0.5">
                Massive update bringing 4 powerhouse feature suites to GlassNotes
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
          
          {/* Suite 1: Folders */}
          <div className="p-4 rounded-2xl glass-field border border-white/10 space-y-3">
            <div className="flex items-center gap-2.5 text-blue-400 font-bold text-base">
              <FolderTree size={20} />
              <h3>1. Complete Folders System</h3>
            </div>
            <p className="text-xs text-slate-300 leading-relaxed">
              Organize your thoughts with unlimited hierarchy, visual identifiers, and seamless dragging.
            </p>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs text-slate-300">
              <div className="flex items-center gap-2">
                <CheckCircle2 size={14} className="text-emerald-400 flex-none" />
                <span>Create, Rename & Delete Folders</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle2 size={14} className="text-emerald-400 flex-none" />
                <span>Subfolders tree hierarchy</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle2 size={14} className="text-emerald-400 flex-none" />
                <span>Custom folder icons & vibrant colors</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle2 size={14} className="text-emerald-400 flex-none" />
                <span>Drag & Drop notes into folders</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle2 size={14} className="text-emerald-400 flex-none" />
                <span>Move & Copy notes via context sheet</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle2 size={14} className="text-emerald-400 flex-none" />
                <span>Pin, Archive & Restore Deleted Folders</span>
              </div>
            </div>
          </div>

          {/* Suite 2: Document Scanner & OCR */}
          <div className="p-4 rounded-2xl glass-field border border-white/10 space-y-3">
            <div className="flex items-center gap-2.5 text-cyan-400 font-bold text-base">
              <ScanLine size={20} />
              <h3>2. Document Scanner & Optical OCR</h3>
            </div>
            <p className="text-xs text-slate-300 leading-relaxed">
              Digitize physical papers, receipts, and book pages with scanner-grade optical filters and direct PDF generation.
            </p>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs text-slate-300">
              <div className="flex items-center gap-2">
                <CheckCircle2 size={14} className="text-cyan-400 flex-none" />
                <span>Camera scanner & photo gallery import</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle2 size={14} className="text-cyan-400 flex-none" />
                <span>Auto edge detection & manual crop perspective</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle2 size={14} className="text-cyan-400 flex-none" />
                <span>Multi-page scanning, reorder & rotation</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle2 size={14} className="text-cyan-400 flex-none" />
                <span>Black & White and Grayscale enhancement</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle2 size={14} className="text-cyan-400 flex-none" />
                <span>OCR text recognition into note body</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle2 size={14} className="text-cyan-400 flex-none" />
                <span>Direct Image-to-PDF export & sharing</span>
              </div>
            </div>
          </div>

          {/* Suite 3: Advanced Rich Note Editor */}
          <div className="p-4 rounded-2xl glass-field border border-white/10 space-y-3">
            <div className="flex items-center gap-2.5 text-purple-400 font-bold text-base">
              <FileEdit size={20} />
              <h3>3. Advanced Rich-Text Note Editor</h3>
            </div>
            <p className="text-xs text-slate-300 leading-relaxed">
              Express your creativity with extensive formatting, multi-media attachments, drawing, and pre-built templates.
            </p>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs text-slate-300">
              <div className="flex items-center gap-2">
                <CheckCircle2 size={14} className="text-purple-400 flex-none" />
                <span>Headings H1 / H2 / H3, Bold, Italic, Strikethrough</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle2 size={14} className="text-purple-400 flex-none" />
                <span>Interactive checklists, numbered & bullet lists</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle2 size={14} className="text-purple-400 flex-none" />
                <span>Markdown tables, blockquotes, code blocks</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle2 size={14} className="text-purple-400 flex-none" />
                <span>Voice recorder with waveform playback</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle2 size={14} className="text-purple-400 flex-none" />
                <span>Handwriting & Vector Drawing Canvas</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle2 size={14} className="text-purple-400 flex-none" />
                <span>5 Templates: Meeting, Project, Planner, Study, Brainstorm</span>
              </div>
            </div>
          </div>

          {/* Suite 4: Custom Reminder & Audio */}
          <div className="p-4 rounded-2xl glass-field border border-white/10 space-y-3">
            <div className="flex items-center gap-2.5 text-amber-400 font-bold text-base">
              <BellRing size={20} />
              <h3>4. Custom Reminder & Sound Settings</h3>
            </div>
            <p className="text-xs text-slate-300 leading-relaxed">
              Tailor every reminder with synthetic crystal chimes, file audio imports, custom vibration patterns, and quiet hours.
            </p>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs text-slate-300">
              <div className="flex items-center gap-2">
                <CheckCircle2 size={14} className="text-amber-400 flex-none" />
                <span>6 synthesized sound styles & custom audio upload</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle2 size={14} className="text-amber-400 flex-none" />
                <span>Sound + Vibration, Vibration Only, or Silent</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle2 size={14} className="text-amber-400 flex-none" />
                <span>Volume slider (0–100%) & gradual volume ramp</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle2 size={14} className="text-amber-400 flex-none" />
                <span>Custom vibration patterns (Heartbeat, Pulse, SOS)</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle2 size={14} className="text-amber-400 flex-none" />
                <span>Do Not Disturb (DND) scheduled quiet hours</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle2 size={14} className="text-amber-400 flex-none" />
                <span>Per-reminder individual sound selection</span>
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
              If your phone displays <em>"App not installed as package conflicts with an existing package"</em>, please uninstall the older v1.0 version first, then install <strong>v1.1.18</strong> for a smooth, conflict-free setup.
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
              href={`https://github.com/${githubOwnerRepo}/releases/download/v1.1.18/GlassNotes.apk`}
              download="GlassNotes-v1.1.18.apk"
              target="_blank"
              rel="noopener noreferrer"
              onClick={() => onDownloadApk?.()}
              className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 text-white text-xs font-bold shadow-lg hover:brightness-110 active:scale-95 transition-all"
            >
              <Download size={14} />
              <span>Download v1.1.18 APK</span>
            </a>
          </div>
        </div>
      </div>
    </div>
  );
};
