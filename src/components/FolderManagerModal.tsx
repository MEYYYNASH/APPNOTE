import React, { useState } from 'react';
import { FolderItem } from '../types/note';
import { FOLDER_ICONS, FOLDER_COLORS } from './FolderSidebar';
import { X, Check } from 'lucide-react';

interface FolderManagerModalProps {
  existingFolder?: FolderItem | null;
  parentFolderId?: string | null;
  allFolders: FolderItem[];
  onDismiss: () => void;
  onSave: (data: {
    id?: string;
    name: string;
    parentId: string | null;
    color: string;
    icon: string;
  }) => void;
}

export const FolderManagerModal: React.FC<FolderManagerModalProps> = ({
  existingFolder,
  parentFolderId = null,
  allFolders,
  onDismiss,
  onSave,
}) => {
  const [name, setName] = useState(existingFolder?.name || '');
  const [parentId, setParentId] = useState<string | null>(
    existingFolder ? existingFolder.parentId : parentFolderId
  );
  const [color, setColor] = useState(existingFolder?.color || FOLDER_COLORS[0]);
  const [icon, setIcon] = useState(existingFolder?.icon || 'folder');

  const availableParents = allFolders.filter(
    (f) => !f.isTrashed && (!existingFolder || f.id !== existingFolder.id)
  );

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    onSave({
      id: existingFolder?.id,
      name: name.trim(),
      parentId,
      color,
      icon,
    });
    onDismiss();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm animate-in fade-in">
      <div className="w-full max-w-sm rounded-3xl glass-panel-elevated p-5 flex flex-col gap-4 shadow-2xl border border-white/40">
        <div className="flex items-center justify-between">
          <h3 className="text-lg font-bold">
            {existingFolder ? 'Edit Folder' : parentId ? 'New Subfolder' : 'New Folder'}
          </h3>
          <button
            onClick={onDismiss}
            className="w-8 h-8 rounded-full glass-field flex items-center justify-center hover:opacity-80"
          >
            <X size={16} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          {/* Name */}
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-semibold text-slate-500">Folder Name</label>
            <input
              type="text"
              required
              autoFocus
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. Work, Project Alpha"
              className="px-3.5 py-2.5 rounded-2xl glass-field text-sm outline-none focus:ring-2 focus:ring-blue-500 font-medium"
            />
          </div>

          {/* Parent Folder selection */}
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-semibold text-slate-500">Parent Folder</label>
            <select
              value={parentId || ''}
              onChange={(e) => setParentId(e.target.value || null)}
              className="px-3.5 py-2.5 rounded-2xl glass-field text-sm outline-none font-medium"
            >
              <option value="">None (Top Level Root)</option>
              {availableParents.map((f) => (
                <option key={f.id} value={f.id}>
                  {f.name}
                </option>
              ))}
            </select>
          </div>

          {/* Icon Selection */}
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-semibold text-slate-500">Folder Icon</label>
            <div className="grid grid-cols-5 gap-2 p-2 rounded-2xl glass-field">
              {Object.keys(FOLDER_ICONS).map((iconKey) => {
                const IconComp = FOLDER_ICONS[iconKey];
                const isCur = icon === iconKey;
                return (
                  <button
                    type="button"
                    key={iconKey}
                    onClick={() => setIcon(iconKey)}
                    className={`h-9 rounded-xl flex items-center justify-center transition-all ${
                      isCur
                        ? 'bg-blue-600 text-white shadow'
                        : 'hover:bg-black/5 dark:hover:bg-white/5 opacity-70'
                    }`}
                  >
                    <IconComp size={16} />
                  </button>
                );
              })}
            </div>
          </div>

          {/* Color Selection */}
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-semibold text-slate-500">Accent Color</label>
            <div className="flex items-center justify-between p-2 rounded-2xl glass-field">
              {FOLDER_COLORS.map((c) => (
                <button
                  type="button"
                  key={c}
                  onClick={() => setColor(c)}
                  style={{ backgroundColor: c }}
                  className={`w-6 h-6 rounded-full transition-transform ${
                    color === c ? 'ring-2 ring-white scale-125 shadow-md' : 'opacity-80 hover:opacity-100'
                  }`}
                />
              ))}
            </div>
          </div>

          {/* Actions */}
          <div className="grid grid-cols-2 gap-3 mt-1">
            <button
              type="button"
              onClick={onDismiss}
              className="py-2.5 rounded-xl glass-field text-xs font-semibold hover:bg-black/5 transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={!name.trim()}
              className="flex items-center justify-center gap-1.5 py-2.5 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 text-white text-xs font-bold shadow disabled:opacity-50 hover:brightness-110 active:scale-95 transition-all"
            >
              <Check size={14} />
              <span>{existingFolder ? 'Save Changes' : 'Create Folder'}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
