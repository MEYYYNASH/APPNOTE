export type RingtoneStyle = 'gentle-glass' | 'digital-pulse' | 'marimba' | 'zen-bell' | 'classic-phone' | 'harp-wave';
export type SoundVibrationMode = 'sound-vibrate' | 'sound-only' | 'vibrate-only' | 'silent';
export type VibrationPattern = 'default' | 'gentle' | 'heartbeat' | 'urgent' | 'double-buzz';

export interface SoundOption {
  id: string;
  name: string;
  style: RingtoneStyle | 'custom';
  source: 'preset' | 'custom-device' | 'custom-drive' | 'custom-files';
  dataBase64?: string; // For imported custom sounds
  fileName?: string;
}

export interface SoundSettings {
  enabled: boolean;
  mode: SoundVibrationMode;
  selectedSoundId: string;
  volume: number; // 0 to 100
  separateAlarmVolume: boolean;
  alarmVolume: number; // 0 to 100
  gradualVolume: boolean;
  repeatRingtone: boolean;
  durationSeconds: number; // e.g. 5, 10, 15, 30, 60
  vibrationPattern: VibrationPattern;
  dndEnabled: boolean;
  dndStartTime: string; // e.g. "22:00"
  dndEndTime: string; // e.g. "07:00"
  customSounds: SoundOption[];
}

export interface NoteReminderConfig {
  timestamp: number | null;
  soundId?: string; // Per-reminder custom sound
  vibrationMode?: SoundVibrationMode;
}

export const PRESET_SOUNDS: SoundOption[] = [
  { id: 'gentle-glass', name: 'Gentle Glass Chimes', style: 'gentle-glass', source: 'preset' },
  { id: 'digital-pulse', name: 'Digital Pulse Alarm', style: 'digital-pulse', source: 'preset' },
  { id: 'marimba', name: 'Warm Marimba', style: 'marimba', source: 'preset' },
  { id: 'zen-bell', name: 'Zen Temple Bell', style: 'zen-bell', source: 'preset' },
  { id: 'classic-phone', name: 'Classic Phone Ringtone', style: 'classic-phone', source: 'preset' },
  { id: 'harp-wave', name: 'Ethereal Harp Wave', style: 'harp-wave', source: 'preset' },
];

export const DEFAULT_SOUND_SETTINGS: SoundSettings = {
  enabled: true,
  mode: 'sound-vibrate',
  selectedSoundId: 'gentle-glass',
  volume: 85,
  separateAlarmVolume: true,
  alarmVolume: 90,
  gradualVolume: true,
  repeatRingtone: false,
  durationSeconds: 10,
  vibrationPattern: 'default',
  dndEnabled: false,
  dndStartTime: '22:00',
  dndEndTime: '07:00',
  customSounds: [],
};

export const VIBRATION_PATTERNS: Record<VibrationPattern, { name: string; pattern: number[] }> = {
  default: { name: 'Standard (500ms)', pattern: [500, 250, 500] },
  gentle: { name: 'Gentle Whisper', pattern: [150, 150, 150] },
  heartbeat: { name: 'Heartbeat Pulse', pattern: [120, 150, 120, 500] },
  urgent: { name: 'Urgent Staccato', pattern: [300, 100, 300, 100, 300] },
  'double-buzz': { name: 'Double Buzz', pattern: [250, 100, 250] },
};
