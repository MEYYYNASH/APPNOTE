import React from 'react';

interface AppGlassLogoProps {
  size?: number;
  className?: string;
}

export const AppGlassLogo: React.FC<AppGlassLogoProps> = ({ size = 36, className = '' }) => {
  return (
    <div
      style={{ width: size, height: size }}
      className={`relative inline-flex items-center justify-center rounded-2xl bg-gradient-to-br from-blue-500 via-indigo-500 to-purple-600 shadow-md p-1.5 overflow-hidden border border-white/40 ${className}`}
    >
      {/* Specular glass reflection shine */}
      <div className="absolute inset-0 bg-gradient-to-b from-white/30 to-transparent pointer-events-none rounded-2xl" />
      <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2.2"
        strokeLinecap="round"
        strokeLinejoin="round"
        className="w-full h-full text-white drop-shadow"
      >
        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
        <polyline points="14 2 14 8 20 8" />
        <line x1="16" y1="13" x2="8" y2="13" />
        <line x1="16" y1="17" x2="8" y2="17" />
        <polyline points="10 9 9 9 8 9" />
      </svg>
    </div>
  );
};
