import React, { useState, useRef, useEffect } from 'react';
import { AppStrings } from '../types/note';
import { Crop, RotateCw, X, Check } from 'lucide-react';

interface PhotoCropDialogProps {
  initialImageBase64: string;
  strings: AppStrings;
  onDismiss: () => void;
  onCropConfirmed: (croppedBase64: string) => void;
}

export const PhotoCropDialog: React.FC<PhotoCropDialogProps> = ({
  initialImageBase64,
  strings,
  onDismiss,
  onCropConfirmed,
}) => {
  const [currentBase64, setCurrentBase64] = useState(initialImageBase64);
  const [activePreset, setActivePreset] = useState('Free');
  const [crop, setCrop] = useState({ left: 0.08, top: 0.08, right: 0.92, bottom: 0.92 });
  const containerRef = useRef<HTMLDivElement | null>(null);
  const isDraggingRef = useRef<number | null>(null); // 0=body, 1=TL, 2=TR, 3=BL, 4=BR

  const applyAspectPreset = (preset: string) => {
    setActivePreset(preset);
    if (preset === 'Free') {
      setCrop({ left: 0.08, top: 0.08, right: 0.92, bottom: 0.92 });
    } else if (preset === '1:1') {
      setCrop({ left: 0.15, top: 0.15, right: 0.85, bottom: 0.85 });
    } else if (preset === '4:3') {
      setCrop({ left: 0.1, top: 0.16, right: 0.9, bottom: 0.84 });
    } else if (preset === '16:9') {
      setCrop({ left: 0.06, top: 0.24, right: 0.94, bottom: 0.76 });
    }
  };

  const handleRotate = () => {
    const img = new Image();
    const prefix = currentBase64.startsWith('data:') ? '' : 'data:image/jpeg;base64,';
    img.src = prefix + currentBase64;
    img.onload = () => {
      const canvas = document.createElement('canvas');
      canvas.width = img.height;
      canvas.height = img.width;
      const ctx = canvas.getContext('2d');
      if (!ctx) return;
      ctx.translate(canvas.width / 2, canvas.height / 2);
      ctx.rotate((90 * Math.PI) / 180);
      ctx.drawImage(img, -img.width / 2, -img.height / 2);
      const rotated = canvas.toDataURL('image/jpeg', 0.9).replace(/^data:image\/jpeg;base64,/, '');
      setCurrentBase64(rotated);
    };
  };

  const handlePointerDown = (target: number, e: React.PointerEvent) => {
    e.preventDefault();
    e.stopPropagation();
    isDraggingRef.current = target;
  };

  const handlePointerMove = (e: React.PointerEvent) => {
    if (isDraggingRef.current === null || !containerRef.current) return;
    const rect = containerRef.current.getBoundingClientRect();
    const nx = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width));
    const ny = Math.max(0, Math.min(1, (e.clientY - rect.top) / rect.height));
    const minSpan = 0.15;

    setCrop((prev) => {
      let { left, top, right, bottom } = prev;
      const mode = isDraggingRef.current;
      if (mode === 1) {
        left = Math.min(nx, right - minSpan);
        top = Math.min(ny, bottom - minSpan);
      } else if (mode === 2) {
        right = Math.max(nx, left + minSpan);
        top = Math.min(ny, bottom - minSpan);
      } else if (mode === 3) {
        left = Math.min(nx, right - minSpan);
        bottom = Math.max(ny, top + minSpan);
      } else if (mode === 4) {
        right = Math.max(nx, left + minSpan);
        bottom = Math.max(ny, top + minSpan);
      }
      return { left, top, right, bottom };
    });
  };

  const handlePointerUp = () => {
    isDraggingRef.current = null;
  };

  const handleCropConfirm = () => {
    const img = new Image();
    const prefix = currentBase64.startsWith('data:') ? '' : 'data:image/jpeg;base64,';
    img.src = prefix + currentBase64;
    img.onload = () => {
      const canvas = document.createElement('canvas');
      const sx = crop.left * img.width;
      const sy = crop.top * img.height;
      const sw = Math.max(10, (crop.right - crop.left) * img.width);
      const sh = Math.max(10, (crop.bottom - crop.top) * img.height);
      canvas.width = sw;
      canvas.height = sh;
      const ctx = canvas.getContext('2d');
      if (!ctx) return;
      ctx.drawImage(img, sx, sy, sw, sh, 0, 0, sw, sh);
      const cropped = canvas.toDataURL('image/jpeg', 0.88).replace(/^data:image\/jpeg;base64,/, '');
      onCropConfirmed(cropped);
    };
  };

  const prefix = currentBase64.startsWith('data:') ? '' : 'data:image/jpeg;base64,';

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-md animate-in fade-in select-none">
      <div className="w-full max-w-md rounded-2xl glass-panel-elevated p-4 flex flex-col gap-2.5 shadow-2xl border border-white/30">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="p-1.5 rounded-xl bg-blue-500/10 text-blue-500">
              <Crop size={16} />
            </div>
            <h3 className="text-base font-bold text-slate-900 dark:text-white">{strings.cropPhotoTitle}</h3>
          </div>
          <button
            onClick={handleRotate}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl glass-field text-xs font-semibold hover:bg-black/5 dark:hover:bg-white/10 transition-colors"
          >
            <RotateCw size={13} />
            <span>{strings.rotateBtn}</span>
          </button>
        </div>

        {/* Aspect presets */}
        <div className="flex items-center gap-1.5 overflow-x-auto pb-0.5">
          {['Free', '1:1', '4:3', '16:9'].map((ratio) => (
            <button
              key={ratio}
              onClick={() => applyAspectPreset(ratio)}
              className={`px-3 py-1 rounded-xl text-xs font-medium transition-all ${
                activePreset === ratio
                  ? 'bg-gradient-to-r from-blue-600 to-indigo-600 text-white font-bold shadow-sm'
                  : 'glass-field hover:opacity-100 opacity-70 text-slate-700 dark:text-slate-300'
              }`}
            >
              {ratio}
            </button>
          ))}
        </div>

        {/* Interactive Crop Container */}
        <div
          ref={containerRef}
          onPointerMove={handlePointerMove}
          onPointerUp={handlePointerUp}
          className="relative w-full h-72 rounded-xl overflow-hidden bg-slate-950 flex items-center justify-center border border-white/20 touch-none cursor-crosshair shadow-inner"
        >
          <img
            src={prefix + currentBase64}
            alt="Crop Preview"
            className="w-full h-full object-contain pointer-events-none"
          />

          {/* Dimmed backdrop masks */}
          <div
            style={{
              top: 0,
              left: 0,
              right: 0,
              height: `${crop.top * 100}%`,
            }}
            className="absolute bg-black/70 pointer-events-none"
          />
          <div
            style={{
              bottom: 0,
              left: 0,
              right: 0,
              height: `${(1 - crop.bottom) * 100}%`,
            }}
            className="absolute bg-black/70 pointer-events-none"
          />
          <div
            style={{
              top: `${crop.top * 100}%`,
              left: 0,
              width: `${crop.left * 100}%`,
              height: `${(crop.bottom - crop.top) * 100}%`,
            }}
            className="absolute bg-black/70 pointer-events-none"
          />
          <div
            style={{
              top: `${crop.top * 100}%`,
              right: 0,
              width: `${(1 - crop.right) * 100}%`,
              height: `${(crop.bottom - crop.top) * 100}%`,
            }}
            className="absolute bg-black/70 pointer-events-none"
          />

          {/* Crop Rectangle & Handles */}
          <div
            style={{
              top: `${crop.top * 100}%`,
              left: `${crop.left * 100}%`,
              width: `${(crop.right - crop.left) * 100}%`,
              height: `${(crop.bottom - crop.top) * 100}%`,
            }}
            className="absolute border border-white/90 pointer-events-none shadow-md"
          >
            {/* Rule of thirds grid lines */}
            <div className="absolute inset-0 grid grid-cols-3 grid-rows-3 pointer-events-none">
              <div className="border-r border-b border-white/25" />
              <div className="border-r border-b border-white/25" />
              <div className="border-b border-white/25" />
              <div className="border-r border-b border-white/25" />
              <div className="border-r border-b border-white/25" />
              <div className="border-b border-white/25" />
              <div className="border-r border-white/25" />
              <div className="border-r border-white/25" />
              <div />
            </div>

            {/* Corner Drag Handles */}
            <div
              onPointerDown={(e) => handlePointerDown(1, e)}
              className="absolute -top-2 -left-2 w-4 h-4 rounded-full bg-white border-2 border-blue-600 pointer-events-auto cursor-nwse-resize shadow-lg flex items-center justify-center"
            />
            <div
              onPointerDown={(e) => handlePointerDown(2, e)}
              className="absolute -top-2 -right-2 w-4 h-4 rounded-full bg-white border-2 border-blue-600 pointer-events-auto cursor-nesw-resize shadow-lg flex items-center justify-center"
            />
            <div
              onPointerDown={(e) => handlePointerDown(3, e)}
              className="absolute -bottom-2 -left-2 w-4 h-4 rounded-full bg-white border-2 border-blue-600 pointer-events-auto cursor-nesw-resize shadow-lg flex items-center justify-center"
            />
            <div
              onPointerDown={(e) => handlePointerDown(4, e)}
              className="absolute -bottom-2 -right-2 w-4 h-4 rounded-full bg-white border-2 border-blue-600 pointer-events-auto cursor-nwse-resize shadow-lg flex items-center justify-center"
            />
          </div>
        </div>

        {/* Buttons */}
        <div className="grid grid-cols-2 gap-2.5 mt-0.5">
          <button
            onClick={onDismiss}
            className="flex items-center justify-center gap-1.5 py-2 rounded-xl glass-field text-xs font-semibold hover:bg-black/5 dark:hover:bg-white/10 transition-colors text-slate-700 dark:text-slate-200"
          >
            <X size={15} />
            <span>{strings.cancelBtn}</span>
          </button>
          <button
            onClick={handleCropConfirm}
            className="flex items-center justify-center gap-1.5 py-2 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 text-white text-xs font-bold shadow-md hover:brightness-110 active:scale-95 transition-all"
          >
            <Check size={15} />
            <span>{strings.cropSaveBtn}</span>
          </button>
        </div>
      </div>
    </div>
  );
};
