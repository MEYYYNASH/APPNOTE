import React, { useState, useRef } from 'react';
import {
  SoundSettings,
  PRESET_SOUNDS,
  DEFAULT_SOUND_SETTINGS,
  VIBRATION_PATTERNS,
  VibrationPattern,
  SoundVibrationMode,
  SoundOption,
} from '../types/sound';
import {
  playPresetSound,
  playCustomBase64Audio,
  playReminderAudio,
  stopActiveAudio,
  triggerVibration,
} from '../utils/audio';
import {
  Volume2,
  VolumeX,
  Vibrate,
  Bell,
  Play,
  Square,
  Upload,
  RotateCcw,
  Sliders,
  Moon,
  Clock,
  Music,
  Check,
  X,
  HardDrive,
  FileAudio,
  FolderUp,
} from 'lucide-react';

interface SoundSettingsDialogProps {
  settings: SoundSettings;
  isKhmer: boolean;
  onDismiss: () => void;
  onSaveSettings: (settings: SoundSettings) => void;
  onShowToast: (msg: string) => void;
}

export const SoundSettingsDialog: React.FC<SoundSettingsDialogProps> = ({
  settings,
  isKhmer,
  onDismiss,
  onSaveSettings,
  onShowToast,
}) => {
  const [current, setCurrent] = useState<SoundSettings>({ ...settings });
  const [previewingId, setPreviewingId] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const driveInputRef = useRef<HTMLInputElement | null>(null);

  const stopPreview = () => {
    stopActiveAudio();
    setPreviewingId(null);
  };

  const handlePreviewSound = (sound: SoundOption) => {
    if (previewingId === sound.id) {
      stopPreview();
      return;
    }
    stopPreview();
    setPreviewingId(sound.id);

    const effectiveVol = current.separateAlarmVolume ? current.alarmVolume : current.volume;

    if (sound.dataBase64) {
      playCustomBase64Audio(
        sound.dataBase64,
        effectiveVol,
        current.gradualVolume,
        5,
        false,
        undefined,
        () => setPreviewingId(null)
      );
    } else {
      playPresetSound(
        sound.style as any,
        effectiveVol,
        current.gradualVolume,
        5,
        false,
        undefined,
        () => setPreviewingId(null)
      );
    }
  };

  const handleTestFullReminder = () => {
    if (previewingId === 'full-test') {
      stopPreview();
      return;
    }
    stopPreview();
    setPreviewingId('full-test');
    onShowToast(isKhmer ? 'កំពុងសាកល្បងសំឡេងរំលឹកពេញលេញ...' : 'Testing full reminder sound & vibration...');

    playReminderAudio(
      current.selectedSoundId,
      current,
      undefined,
      () => setPreviewingId(null)
    );
  };

  // Import sound from local device / files
  const handleImportSound = (e: React.ChangeEvent<HTMLInputElement>, source: 'custom-device' | 'custom-drive' | 'custom-files') => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (file.size > 8 * 1024 * 1024) {
      onShowToast('Audio file exceeds 8MB limit.');
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      const b64 = (reader.result as string).replace(/^data:audio\/\w+;base64,/, '');
      const newSound: SoundOption = {
        id: `custom_${Date.now()}`,
        name: file.name.replace(/\.[^/.]+$/, ''),
        style: 'custom',
        source,
        dataBase64: b64,
        fileName: file.name,
      };

      setCurrent((prev) => ({
        ...prev,
        selectedSoundId: newSound.id,
        customSounds: [...prev.customSounds, newSound],
      }));

      onShowToast(`Imported custom sound: ${file.name}`);
    };
    reader.readAsDataURL(file);
    e.target.value = '';
  };

  const handleResetDefaults = () => {
    setCurrent({
      ...DEFAULT_SOUND_SETTINGS,
      customSounds: current.customSounds, // keep imported sounds
    });
    onShowToast('Sound settings reset to defaults.');
  };

  const handleSave = () => {
    stopPreview();
    onSaveSettings(current);
    onShowToast('Reminder & sound settings saved!');
    onDismiss();
  };

  const allSounds = [...PRESET_SOUNDS, ...current.customSounds];

  return (
    <div
      onClick={() => {
        stopPreview();
        onDismiss();
      }}
      className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/50 backdrop-blur-md animate-in fade-in"
    >
      <div
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-xl max-h-[90vh] rounded-3xl glass-panel-elevated p-6 flex flex-col gap-4 shadow-2xl border border-white/40 overflow-hidden"
      >
        {/* Hidden File Inputs */}
        <input
          type="file"
          ref={fileInputRef}
          accept="audio/*,.mp3,.wav,.ogg,.m4a"
          onChange={(e) => handleImportSound(e, 'custom-device')}
          className="hidden"
        />
        <input
          type="file"
          ref={driveInputRef}
          accept="audio/*,.mp3,.wav,.ogg"
          onChange={(e) => handleImportSound(e, 'custom-drive')}
          className="hidden"
        />

        {/* Header */}
        <div className="flex items-center justify-between pb-1 border-b border-white/10">
          <div className="flex items-center gap-2.5">
            <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-amber-500 to-rose-500 text-white flex items-center justify-center shadow-md">
              <Bell size={20} />
            </div>
            <div>
              <h3 className="text-lg font-bold">
                {isKhmer ? 'ការកំណត់សំឡេងរំលឹក និងរោទ៍' : 'Reminder & Sound Settings'}
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                {isKhmer ? 'កែសម្រួលសំឡេងរោទ៍ រំញ័រ កម្រិតសំឡេង និងរបៀបកុំរំខាន' : 'Customize ringtone styles, volume, vibration & DND'}
              </p>
            </div>
          </div>
          <button
            onClick={() => {
              stopPreview();
              onDismiss();
            }}
            className="w-8 h-8 rounded-full glass-field flex items-center justify-center hover:opacity-80"
          >
            <X size={16} />
          </button>
        </div>

        {/* Scrollable Content Body */}
        <div className="flex-1 overflow-y-auto space-y-4 pr-1 scrollbar-thin text-xs">
          {/* Section 1: Main Sound & Vibration Mode */}
          <div className="p-3.5 rounded-2xl glass-card space-y-3">
            <div className="flex items-center justify-between">
              <span className="font-bold text-sm">
                {isKhmer ? 'សំឡេង និងរំញ័រ' : 'Alert Delivery Mode'}
              </span>
              <button
                onClick={() => setCurrent((p) => ({ ...p, enabled: !p.enabled }))}
                className={`px-3 py-1 rounded-full text-[11px] font-bold transition-all ${
                  current.enabled
                    ? 'bg-blue-600 text-white shadow'
                    : 'bg-black/10 dark:bg-white/10 text-slate-400'
                }`}
              >
                {current.enabled ? 'Enabled' : 'Disabled'}
              </button>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
              {[
                { id: 'sound-vibrate', label: 'Sound + Vibration', icon: Volume2 },
                { id: 'sound-only', label: 'Sound Only', icon: Music },
                { id: 'vibrate-only', label: 'Vibration Only', icon: Vibrate },
                { id: 'silent', label: 'Silent Reminder', icon: VolumeX },
              ].map((m) => {
                const Icon = m.icon;
                const isSel = current.mode === m.id;
                return (
                  <button
                    key={m.id}
                    onClick={() => {
                      setCurrent((p) => ({ ...p, mode: m.id as SoundVibrationMode }));
                      if (m.id.includes('vibrate')) triggerVibration(current.vibrationPattern);
                    }}
                    className={`p-2.5 rounded-xl flex flex-col items-center gap-1.5 text-center transition-all ${
                      isSel
                        ? 'bg-blue-600 text-white font-bold shadow'
                        : 'glass-field hover:bg-black/5 dark:hover:bg-white/5 opacity-80'
                    }`}
                  >
                    <Icon size={16} />
                    <span className="text-[11px] leading-tight">{m.label}</span>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Section 2: Ringtone Selection & Preview */}
          <div className="p-3.5 rounded-2xl glass-card space-y-3">
            <div className="flex items-center justify-between">
              <span className="font-bold text-sm">
                {isKhmer ? 'សំឡេងរោទ៍រំលឹក' : 'Ringtone & Melody'}
              </span>
              <div className="flex items-center gap-1.5">
                <button
                  onClick={() => fileInputRef.current?.click()}
                  title="Import from Device"
                  className="px-2.5 py-1 rounded-xl glass-field flex items-center gap-1 hover:text-blue-500 font-semibold"
                >
                  <Upload size={12} />
                  <span>Device</span>
                </button>
                <button
                  onClick={() => driveInputRef.current?.click()}
                  title="Import from Google Drive / Files"
                  className="px-2.5 py-1 rounded-xl glass-field flex items-center gap-1 hover:text-blue-500 font-semibold"
                >
                  <FolderUp size={12} />
                  <span>Drive</span>
                </button>
              </div>
            </div>

            <div className="space-y-1.5 max-h-48 overflow-y-auto pr-1 scrollbar-thin">
              {allSounds.map((snd) => {
                const isSelected = current.selectedSoundId === snd.id;
                const isPlaying = previewingId === snd.id;
                return (
                  <div
                    key={snd.id}
                    className={`flex items-center justify-between p-2.5 rounded-xl transition-all ${
                      isSelected
                        ? 'bg-blue-500/15 border border-blue-500/30 font-semibold text-blue-600 dark:text-blue-400'
                        : 'glass-field hover:bg-black/5 dark:hover:bg-white/5'
                    }`}
                  >
                    <button
                      onClick={() => setCurrent((p) => ({ ...p, selectedSoundId: snd.id }))}
                      className="flex items-center gap-2.5 flex-1 text-left min-w-0"
                    >
                      <div className="w-5 h-5 rounded-full border border-blue-500 flex items-center justify-center flex-none">
                        {isSelected && <div className="w-2.5 h-2.5 rounded-full bg-blue-500" />}
                      </div>
                      <span className="truncate">{snd.name}</span>
                      {snd.source !== 'preset' && (
                        <span className="px-1.5 py-0.5 rounded text-[9px] bg-purple-500/10 text-purple-500 font-bold">
                          Custom
                        </span>
                      )}
                    </button>

                    <button
                      onClick={() => handlePreviewSound(snd)}
                      title={isPlaying ? 'Stop Preview' : 'Preview Sound'}
                      className={`w-7 h-7 rounded-lg flex items-center justify-center transition-all ${
                        isPlaying
                          ? 'bg-rose-500 text-white shadow'
                          : 'glass-field hover:text-blue-500'
                      }`}
                    >
                      {isPlaying ? <Square size={12} /> : <Play size={12} />}
                    </button>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Section 3: Volume & Playback Controls */}
          <div className="p-3.5 rounded-2xl glass-card space-y-3">
            <span className="font-bold text-sm">
              {isKhmer ? 'កម្រិតសំឡេង និងលក្ខណៈរោទ៍' : 'Volume & Playback Dynamics'}
            </span>

            {/* Main Volume Slider */}
            <div className="space-y-1">
              <div className="flex items-center justify-between text-[11px]">
                <span className="text-slate-500">General Reminder Volume</span>
                <span className="font-bold">{current.volume}%</span>
              </div>
              <input
                type="range"
                min={0}
                max={100}
                value={current.volume}
                onChange={(e) => setCurrent((p) => ({ ...p, volume: Number(e.target.value) }))}
                className="w-full accent-blue-600 cursor-pointer"
              />
            </div>

            {/* Separate Alarm Volume Toggle & Slider */}
            <div className="pt-2 border-t border-white/10 space-y-2">
              <div className="flex items-center justify-between">
                <div>
                  <div className="font-semibold text-[11px]">Separate Alarm Volume</div>
                  <div className="text-[10px] text-slate-500">Louder dedicated volume for urgent alarms</div>
                </div>
                <input
                  type="checkbox"
                  checked={current.separateAlarmVolume}
                  onChange={(e) => setCurrent((p) => ({ ...p, separateAlarmVolume: e.target.checked }))}
                  className="w-4 h-4 accent-blue-600 rounded"
                />
              </div>

              {current.separateAlarmVolume && (
                <div className="space-y-1 pl-1">
                  <div className="flex items-center justify-between text-[11px]">
                    <span className="text-slate-500">Alarm Volume Level</span>
                    <span className="font-bold text-amber-500">{current.alarmVolume}%</span>
                  </div>
                  <input
                    type="range"
                    min={0}
                    max={100}
                    value={current.alarmVolume}
                    onChange={(e) => setCurrent((p) => ({ ...p, alarmVolume: Number(e.target.value) }))}
                    className="w-full accent-amber-500 cursor-pointer"
                  />
                </div>
              )}
            </div>

            {/* Gradual Volume & Repeat */}
            <div className="grid grid-cols-2 gap-3 pt-2 border-t border-white/10">
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={current.gradualVolume}
                  onChange={(e) => setCurrent((p) => ({ ...p, gradualVolume: e.target.checked }))}
                  className="w-4 h-4 accent-blue-600 rounded"
                />
                <div>
                  <div className="font-semibold text-[11px]">Gradually Increase</div>
                  <div className="text-[10px] text-slate-500">Gentle fade-in volume</div>
                </div>
              </label>

              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={current.repeatRingtone}
                  onChange={(e) => setCurrent((p) => ({ ...p, repeatRingtone: e.target.checked }))}
                  className="w-4 h-4 accent-blue-600 rounded"
                />
                <div>
                  <div className="font-semibold text-[11px]">Repeat Ringtone</div>
                  <div className="text-[10px] text-slate-500">Loop until dismissed</div>
                </div>
              </label>
            </div>

            {/* Duration Selector */}
            <div className="pt-2 border-t border-white/10 flex items-center justify-between">
              <span className="text-[11px] font-semibold text-slate-500">Sound Duration</span>
              <div className="flex items-center gap-1">
                {[5, 10, 15, 30].map((sec) => (
                  <button
                    key={sec}
                    onClick={() => setCurrent((p) => ({ ...p, durationSeconds: sec }))}
                    className={`px-2.5 py-1 rounded-lg text-[10px] font-bold ${
                      current.durationSeconds === sec
                        ? 'bg-blue-600 text-white shadow'
                        : 'glass-field hover:bg-black/5'
                    }`}
                  >
                    {sec}s
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* Section 4: Vibration Pattern */}
          <div className="p-3.5 rounded-2xl glass-card space-y-3">
            <span className="font-bold text-sm">
              {isKhmer ? 'លំនាំរំញ័រ' : 'Custom Vibration Pattern'}
            </span>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              {(Object.keys(VIBRATION_PATTERNS) as VibrationPattern[]).map((patKey) => {
                const conf = VIBRATION_PATTERNS[patKey];
                const isSel = current.vibrationPattern === patKey;
                return (
                  <button
                    key={patKey}
                    onClick={() => {
                      setCurrent((p) => ({ ...p, vibrationPattern: patKey }));
                      triggerVibration(patKey);
                    }}
                    className={`flex items-center justify-between p-2.5 rounded-xl text-left transition-all ${
                      isSel
                        ? 'bg-blue-500/15 border border-blue-500/30 text-blue-600 font-bold'
                        : 'glass-field hover:bg-black/5'
                    }`}
                  >
                    <div className="flex items-center gap-2">
                      <Vibrate size={15} />
                      <span>{conf.name}</span>
                    </div>
                    {isSel && <Check size={14} />}
                  </button>
                );
              })}
            </div>
          </div>

          {/* Section 5: Do Not Disturb (DND) */}
          <div className="p-3.5 rounded-2xl glass-card space-y-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Moon size={16} className="text-purple-500" />
                <div>
                  <span className="font-bold text-sm">Do Not Disturb (DND)</span>
                  <div className="text-[10px] text-slate-500">Silence sound reminders during quiet hours</div>
                </div>
              </div>
              <input
                type="checkbox"
                checked={current.dndEnabled}
                onChange={(e) => setCurrent((p) => ({ ...p, dndEnabled: e.target.checked }))}
                className="w-4 h-4 accent-purple-600 rounded"
              />
            </div>

            {current.dndEnabled && (
              <div className="grid grid-cols-2 gap-3 pt-2 border-t border-white/10">
                <div className="space-y-1">
                  <span className="text-[10px] text-slate-500">Quiet Time Start</span>
                  <input
                    type="time"
                    value={current.dndStartTime}
                    onChange={(e) => setCurrent((p) => ({ ...p, dndStartTime: e.target.value }))}
                    className="w-full px-3 py-1.5 rounded-xl glass-field outline-none font-semibold"
                  />
                </div>
                <div className="space-y-1">
                  <span className="text-[10px] text-slate-500">Quiet Time End</span>
                  <input
                    type="time"
                    value={current.dndEndTime}
                    onChange={(e) => setCurrent((p) => ({ ...p, dndEndTime: e.target.value }))}
                    className="w-full px-3 py-1.5 rounded-xl glass-field outline-none font-semibold"
                  />
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Footer Actions */}
        <div className="flex items-center justify-between pt-2 border-t border-white/15 gap-2 flex-wrap">
          <div className="flex items-center gap-2">
            <button
              onClick={handleTestFullReminder}
              className={`flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-bold transition-all ${
                previewingId === 'full-test'
                  ? 'bg-rose-500 text-white shadow-lg'
                  : 'bg-amber-500/15 text-amber-600 hover:bg-amber-500/25'
              }`}
            >
              {previewingId === 'full-test' ? <Square size={13} /> : <Play size={13} />}
              <span>{previewingId === 'full-test' ? 'Stop Test' : 'Test Reminder Sound'}</span>
            </button>

            <button
              onClick={handleResetDefaults}
              title="Reset to default sound settings"
              className="p-2 rounded-xl glass-field hover:bg-black/5 text-slate-500"
            >
              <RotateCcw size={14} />
            </button>
          </div>

          <div className="flex items-center gap-2 ml-auto">
            <button
              onClick={() => {
                stopPreview();
                onDismiss();
              }}
              className="px-4 py-2 rounded-xl glass-field text-xs font-semibold hover:bg-black/5"
            >
              Cancel
            </button>
            <button
              onClick={handleSave}
              className="flex items-center gap-1 px-5 py-2 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 text-white text-xs font-bold shadow-md hover:brightness-110 active:scale-95"
            >
              <Check size={14} />
              <span>Save Sound Settings</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
