import React, { useRef, useState, useEffect } from 'react';
import { AppStrings } from '../types/note';
import { Trash2, X, Check } from 'lucide-react';

interface DrawingCanvasDialogProps {
  strings: AppStrings;
  onDismiss: () => void;
  onAttachDrawing: (dataBase64: string) => void;
}

const PALETTE = ['#2F7CFF', '#A855F7', '#EC4899', '#10B981', '#F59E0B', '#0F172A'];

export const DrawingCanvasDialog: React.FC<DrawingCanvasDialogProps> = ({
  strings,
  onDismiss,
  onAttachDrawing,
}) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const [selectedColor, setSelectedColor] = useState(PALETTE[0]);
  const [isDrawing, setIsDrawing] = useState(false);
  const [hasDrawn, setHasDrawn] = useState(false);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    // Fill with white background
    ctx.fillStyle = '#FFFFFF';
    ctx.fillRect(0, 0, canvas.width, canvas.height);
  }, []);

  const getCanvasPos = (e: React.MouseEvent<HTMLCanvasElement> | React.TouchEvent<HTMLCanvasElement>) => {
    const canvas = canvasRef.current;
    if (!canvas) return { x: 0, y: 0 };
    const rect = canvas.getBoundingClientRect();
    const clientX = 'touches' in e ? e.touches[0].clientX : e.clientX;
    const clientY = 'touches' in e ? e.touches[0].clientY : e.clientY;
    const scaleX = canvas.width / rect.width;
    const scaleY = canvas.height / rect.height;
    return {
      x: (clientX - rect.left) * scaleX,
      y: (clientY - rect.top) * scaleY,
    };
  };

  const startDrawing = (e: React.MouseEvent<HTMLCanvasElement> | React.TouchEvent<HTMLCanvasElement>) => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const { x, y } = getCanvasPos(e);
    ctx.beginPath();
    ctx.moveTo(x, y);
    ctx.strokeStyle = selectedColor;
    ctx.lineWidth = 6;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    setIsDrawing(true);
    setHasDrawn(true);
  };

  const draw = (e: React.MouseEvent<HTMLCanvasElement> | React.TouchEvent<HTMLCanvasElement>) => {
    if (!isDrawing) return;
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const { x, y } = getCanvasPos(e);
    ctx.lineTo(x, y);
    ctx.stroke();
  };

  const stopDrawing = () => {
    setIsDrawing(false);
  };

  const clearCanvas = () => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    ctx.fillStyle = '#FFFFFF';
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    setHasDrawn(false);
  };

  const handleAttach = () => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    if (!hasDrawn) {
      // Draw a neat geometric wave curve if user clicks attach directly
      ctx.strokeStyle = selectedColor;
      ctx.lineWidth = 6;
      ctx.lineCap = 'round';
      ctx.beginPath();
      ctx.moveTo(canvas.width * 0.15, canvas.height * 0.65);
      ctx.bezierCurveTo(
        canvas.width * 0.35, canvas.height * 0.2,
        canvas.width * 0.65, canvas.height * 0.8,
        canvas.width * 0.85, canvas.height * 0.35
      );
      ctx.stroke();
    }

    const dataUrl = canvas.toDataURL('image/png', 0.9);
    const b64 = dataUrl.replace(/^data:image\/png;base64,/, '');
    onAttachDrawing(b64);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm animate-in fade-in">
      <div className="w-full max-w-md rounded-3xl glass-panel-elevated p-5 flex flex-col gap-4 shadow-2xl border border-white/40">
        <div className="flex items-center justify-between">
          <h3 className="text-lg font-bold">{strings.drawBtn}</h3>
          <div className="flex items-center gap-2">
            {PALETTE.map((color) => (
              <button
                key={color}
                onClick={() => setSelectedColor(color)}
                style={{ backgroundColor: color }}
                className={`w-6 h-6 rounded-full transition-transform ${
                  selectedColor === color
                    ? 'ring-2 ring-white scale-110 shadow-md'
                    : 'opacity-80 hover:opacity-100'
                }`}
              />
            ))}
          </div>
        </div>

        <div className="relative w-full h-64 rounded-2xl overflow-hidden border border-white/30 bg-white touch-none">
          <canvas
            ref={canvasRef}
            width={600}
            height={400}
            className="w-full h-full cursor-crosshair"
            onMouseDown={startDrawing}
            onMouseMove={draw}
            onMouseUp={stopDrawing}
            onMouseLeave={stopDrawing}
            onTouchStart={startDrawing}
            onTouchMove={draw}
            onTouchEnd={stopDrawing}
          />
        </div>

        <div className="grid grid-cols-3 gap-2">
          <button
            onClick={clearCanvas}
            className="flex items-center justify-center gap-1.5 py-2.5 rounded-xl glass-field text-sm font-medium hover:bg-red-500/10 hover:text-red-500 transition-colors"
          >
            <Trash2 size={16} />
            <span>{strings.clearBtn}</span>
          </button>
          <button
            onClick={onDismiss}
            className="flex items-center justify-center gap-1.5 py-2.5 rounded-xl glass-field text-sm font-medium hover:bg-black/5 dark:hover:bg-white/5 transition-colors"
          >
            <X size={16} />
            <span>{strings.cancelBtn}</span>
          </button>
          <button
            onClick={handleAttach}
            className="flex items-center justify-center gap-1.5 py-2.5 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 text-white text-sm font-semibold shadow-md hover:brightness-110 active:scale-95 transition-all"
          >
            <Check size={16} />
            <span>{strings.attachBtn}</span>
          </button>
        </div>
      </div>
    </div>
  );
};
