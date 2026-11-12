// Audio synthesis, playback, vibration and custom sound engine
import {
  SoundOption,
  SoundSettings,
  RingtoneStyle,
  VIBRATION_PATTERNS,
  DEFAULT_SOUND_SETTINGS,
} from '../types/sound';

let audioCtx: AudioContext | null = null;
function getAudioContext(): AudioContext {
  if (!audioCtx) {
    const AudioContextClass = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
    audioCtx = new AudioContextClass();
  }
  if (audioCtx.state === 'suspended') {
    audioCtx.resume();
  }
  return audioCtx;
}

let activePlaybackStop: (() => void) | null = null;

export function stopActiveAudio() {
  if (activePlaybackStop) {
    activePlaybackStop();
    activePlaybackStop = null;
  }
  if ('vibrate' in navigator) {
    navigator.vibrate(0);
  }
}

/**
 * Triggers device vibration following selected pattern
 */
export function triggerVibration(patternKey: keyof typeof VIBRATION_PATTERNS = 'default') {
  if ('vibrate' in navigator) {
    const conf = VIBRATION_PATTERNS[patternKey] || VIBRATION_PATTERNS.default;
    try {
      navigator.vibrate(conf.pattern);
    } catch {
      // ignore
    }
  }
}

/**
 * Synthesizes high-fidelity ringtones across multiple acoustic styles
 */
export function playPresetSound(
  style: RingtoneStyle,
  volumePercent = 80,
  gradual = false,
  durationSec = 5,
  loop = false,
  onProgress?: (progress: number) => void,
  onEnded?: () => void
): () => void {
  stopActiveAudio();
  const ctx = getAudioContext();
  const startTime = ctx.currentTime;
  const masterGain = ctx.createGain();
  masterGain.connect(ctx.destination);

  const targetVol = Math.max(0.01, Math.min(1.0, volumePercent / 100));

  if (gradual) {
    // Gradually ramp up volume from 10% to targetVol over 3.5s
    masterGain.gain.setValueAtTime(0.05, startTime);
    masterGain.gain.exponentialRampToValueAtTime(targetVol, startTime + Math.min(3.5, durationSec * 0.8));
  } else {
    masterGain.gain.setValueAtTime(targetVol, startTime);
  }

  const nodesToStop: { stop: (t: number) => void }[] = [];

  const scheduleNotes = (baseTime: number) => {
    switch (style) {
      case 'digital-pulse': {
        // Classic rhythmic 880Hz / 1760Hz double beeps
        const beeps = [880, 880, 1760, 880];
        beeps.forEach((freq, idx) => {
          const osc = ctx.createOscillator();
          const g = ctx.createGain();
          osc.type = 'square';
          osc.frequency.setValueAtTime(freq, baseTime + idx * 0.22);
          g.gain.setValueAtTime(0.18, baseTime + idx * 0.22);
          g.gain.exponentialRampToValueAtTime(0.001, baseTime + idx * 0.22 + 0.16);
          osc.connect(g);
          g.connect(masterGain);
          osc.start(baseTime + idx * 0.22);
          osc.stop(baseTime + idx * 0.22 + 0.18);
          nodesToStop.push(osc);
        });
        break;
      }
      case 'marimba': {
        // Warm wooden acoustic marimba triad (F4, A4, C5, F5)
        const notes = [349.23, 440.0, 523.25, 698.46, 880.0];
        notes.forEach((freq, idx) => {
          const osc = ctx.createOscillator();
          const g = ctx.createGain();
          osc.type = 'sine';
          osc.frequency.setValueAtTime(freq, baseTime + idx * 0.28);
          g.gain.setValueAtTime(0.35, baseTime + idx * 0.28);
          g.gain.exponentialRampToValueAtTime(0.001, baseTime + idx * 0.28 + 0.8);
          osc.connect(g);
          g.connect(masterGain);
          osc.start(baseTime + idx * 0.28);
          osc.stop(baseTime + idx * 0.28 + 0.85);
          nodesToStop.push(osc);
        });
        break;
      }
      case 'zen-bell': {
        // Low warm harmonic Tibetan meditation bell
        const freqs = [432, 864, 1296];
        freqs.forEach((freq, i) => {
          const osc = ctx.createOscillator();
          const g = ctx.createGain();
          osc.type = 'triangle';
          osc.frequency.setValueAtTime(freq, baseTime);
          const initial = i === 0 ? 0.4 : 0.15;
          g.gain.setValueAtTime(initial, baseTime);
          g.gain.exponentialRampToValueAtTime(0.0001, baseTime + 4.2);
          osc.connect(g);
          g.connect(masterGain);
          osc.start(baseTime);
          osc.stop(baseTime + 4.5);
          nodesToStop.push(osc);
        });
        break;
      }
      case 'classic-phone': {
        // Traditional dual-tone multi-frequency phone ring (440Hz + 480Hz)
        [440, 480].forEach((freq) => {
          const osc = ctx.createOscillator();
          const g = ctx.createGain();
          osc.type = 'sine';
          osc.frequency.setValueAtTime(freq, baseTime);
          // Ring for 1.8s, pause
          g.gain.setValueAtTime(0.22, baseTime);
          g.gain.setValueAtTime(0.22, baseTime + 1.8);
          g.gain.exponentialRampToValueAtTime(0.001, baseTime + 1.85);
          osc.connect(g);
          g.connect(masterGain);
          osc.start(baseTime);
          osc.stop(baseTime + 1.9);
          nodesToStop.push(osc);
        });
        break;
      }
      case 'harp-wave': {
        // Pentatonic upward harp glissando
        const harp = [261.63, 329.63, 392.0, 523.25, 659.25, 783.99, 1046.5];
        harp.forEach((freq, idx) => {
          const osc = ctx.createOscillator();
          const g = ctx.createGain();
          osc.type = 'sine';
          osc.frequency.setValueAtTime(freq, baseTime + idx * 0.16);
          g.gain.setValueAtTime(0.3, baseTime + idx * 0.16);
          g.gain.exponentialRampToValueAtTime(0.001, baseTime + idx * 0.16 + 1.2);
          osc.connect(g);
          g.connect(masterGain);
          osc.start(baseTime + idx * 0.16);
          osc.stop(baseTime + idx * 0.16 + 1.3);
          nodesToStop.push(osc);
        });
        break;
      }
      case 'gentle-glass':
      default: {
        // Frosted crystal chime chord
        const chimes = [523.25, 659.25, 783.99, 1046.5, 1318.5];
        chimes.forEach((freq, idx) => {
          const osc = ctx.createOscillator();
          const g = ctx.createGain();
          osc.type = 'sine';
          osc.frequency.setValueAtTime(freq, baseTime + idx * 0.22);
          g.gain.setValueAtTime(0.28, baseTime + idx * 0.22);
          g.gain.exponentialRampToValueAtTime(0.001, baseTime + idx * 0.22 + 1.4);
          osc.connect(g);
          g.connect(masterGain);
          osc.start(baseTime + idx * 0.22);
          osc.stop(baseTime + idx * 0.22 + 1.5);
          nodesToStop.push(osc);
        });
        break;
      }
    }
  };

  // Schedule repetitions if duration exceeds single loop or loop is requested
  const patternPeriod = style === 'zen-bell' ? 4.5 : 2.5;
  const repeats = loop ? Math.ceil(durationSec / patternPeriod) : Math.min(Math.ceil(durationSec / patternPeriod), 3);
  for (let r = 0; r < repeats; r++) {
    scheduleNotes(startTime + r * patternPeriod);
  }

  const effectiveDuration = Math.min(durationSec, repeats * patternPeriod);

  let interval: number | null = window.setInterval(() => {
    const elapsed = ctx.currentTime - startTime;
    const pct = Math.min(1.0, elapsed / effectiveDuration);
    onProgress?.(pct);
  }, 80);

  const timeout = window.setTimeout(() => {
    if (interval) clearInterval(interval);
    interval = null;
    onProgress?.(0);
    onEnded?.();
    if (activePlaybackStop === stopFn) activePlaybackStop = null;
  }, effectiveDuration * 1000);

  const stopFn = () => {
    if (interval) clearInterval(interval);
    clearTimeout(timeout);
    try {
      masterGain.gain.setValueAtTime(0, ctx.currentTime);
      nodesToStop.forEach((n) => {
        try { n.stop(ctx.currentTime); } catch { /* ignore */ }
      });
    } catch {
      // ignore
    }
    onProgress?.(0);
    onEnded?.();
  };

  activePlaybackStop = stopFn;
  return stopFn;
}

/**
 * Play a custom imported sound or preset sound with complete volume, gradual increase, and vibration
 */
export function playReminderAudio(
  soundId: string,
  soundSettings: SoundSettings,
  onProgress?: (progress: number) => void,
  onEnded?: () => void
): () => void {
  stopActiveAudio();

  // Check Do Not Disturb (DND)
  if (soundSettings.dndEnabled) {
    const now = new Date();
    const curMinutes = now.getHours() * 60 + now.getMinutes();
    const [startH, startM] = soundSettings.dndStartTime.split(':').map(Number);
    const [endH, endM] = soundSettings.dndEndTime.split(':').map(Number);
    const startMinutes = startH * 60 + startM;
    const endMinutes = endH * 60 + endM;

    const inDnd = startMinutes <= endMinutes
      ? curMinutes >= startMinutes && curMinutes <= endMinutes
      : curMinutes >= startMinutes || curMinutes <= endMinutes;

    if (inDnd) {
      onEnded?.();
      return () => {};
    }
  }

  // Handle Vibration
  if (soundSettings.mode === 'sound-vibrate' || soundSettings.mode === 'vibrate-only') {
    triggerVibration(soundSettings.vibrationPattern);
  }

  // Handle Silent
  if (!soundSettings.enabled || soundSettings.mode === 'silent' || soundSettings.mode === 'vibrate-only') {
    onEnded?.();
    return () => {};
  }

  const effectiveVol = soundSettings.separateAlarmVolume
    ? soundSettings.alarmVolume
    : soundSettings.volume;

  // Check if soundId is a custom sound
  const customSound = soundSettings.customSounds.find((s) => s.id === soundId);
  if (customSound && customSound.dataBase64) {
    return playCustomBase64Audio(
      customSound.dataBase64,
      effectiveVol,
      soundSettings.gradualVolume,
      soundSettings.durationSeconds,
      soundSettings.repeatRingtone,
      onProgress,
      onEnded
    );
  }

  // Otherwise, play corresponding preset style
  const style = (soundId as RingtoneStyle) || 'gentle-glass';
  return playPresetSound(
    style,
    effectiveVol,
    soundSettings.gradualVolume,
    soundSettings.durationSeconds,
    soundSettings.repeatRingtone,
    onProgress,
    onEnded
  );
}

/**
 * Play custom imported audio from device / Google Drive / files
 */
export function playCustomBase64Audio(
  base64Data: string,
  volumePercent = 80,
  gradual = false,
  durationSec = 10,
  repeat = false,
  onProgress?: (progress: number) => void,
  onEnded?: () => void
): () => void {
  stopActiveAudio();
  const prefix = base64Data.startsWith('data:') ? '' : 'data:audio/mp3;base64,';
  const audio = new Audio(prefix + base64Data);
  const targetVol = Math.max(0.01, Math.min(1.0, volumePercent / 100));

  audio.volume = gradual ? targetVol * 0.15 : targetVol;
  audio.loop = repeat;

  let progressInterval: number | null = null;
  let gradualInterval: number | null = null;

  if (gradual) {
    let curVol = targetVol * 0.15;
    gradualInterval = window.setInterval(() => {
      curVol = Math.min(targetVol, curVol + 0.08);
      audio.volume = curVol;
      if (curVol >= targetVol && gradualInterval) {
        clearInterval(gradualInterval);
        gradualInterval = null;
      }
    }, 400);
  }

  const maxTimer = window.setTimeout(() => {
    stopFn();
  }, durationSec * 1000);

  audio.onplay = () => {
    progressInterval = window.setInterval(() => {
      if (audio.duration && audio.duration > 0) {
        onProgress?.(audio.currentTime / audio.duration);
      }
    }, 80);
  };

  const stopFn = () => {
    if (progressInterval) clearInterval(progressInterval);
    if (gradualInterval) clearInterval(gradualInterval);
    clearTimeout(maxTimer);
    try {
      audio.pause();
      audio.currentTime = 0;
    } catch {
      // ignore
    }
    onProgress?.(0);
    onEnded?.();
    if (activePlaybackStop === stopFn) activePlaybackStop = null;
  };

  audio.onended = stopFn;
  audio.onerror = stopFn;

  audio.play().catch(() => stopFn());
  activePlaybackStop = stopFn;
  return stopFn;
}

/**
 * Helper to play base64 voice notes
 */
export function playBase64Audio(
  base64Data: string,
  onProgress?: (progress: number) => void,
  onEnded?: () => void
): () => void {
  return playCustomBase64Audio(base64Data, 100, false, 120, false, onProgress, onEnded);
}

export function playNotificationChime() {
  playPresetSound('gentle-glass', 85, false, 2, false);
}

export function generateAudibleWavBase64(durationSeconds = 1.5): string {
  const sampleRate = 22050;
  const numSamples = Math.floor(sampleRate * durationSeconds);
  const buffer = new ArrayBuffer(44 + numSamples * 2);
  const view = new DataView(buffer);
  
  const writeString = (offset: number, str: string) => {
    for (let i = 0; i < str.length; i++) view.setUint8(offset + i, str.charCodeAt(i));
  };
  
  writeString(0, 'RIFF');
  view.setUint32(4, 36 + numSamples * 2, true);
  writeString(8, 'WAVE');
  writeString(12, 'fmt ');
  view.setUint32(16, 16, true);
  view.setUint16(20, 1, true);
  view.setUint16(22, 1, true);
  view.setUint32(24, sampleRate, true);
  view.setUint32(28, sampleRate * 2, true);
  view.setUint16(32, 2, true);
  view.setUint16(34, 16, true);
  writeString(36, 'data');
  view.setUint32(40, numSamples * 2, true);
  
  let offset = 44;
  for (let i = 0; i < numSamples; i++) {
    const t = i / sampleRate;
    const env = Math.exp(-t * 2.5);
    const sample = Math.sin(2 * Math.PI * 440 * t) * 0.4 * env;
    const clamped = Math.max(-1, Math.min(1, sample));
    view.setInt16(offset, clamped < 0 ? clamped * 0x8000 : clamped * 0x7fff, true);
    offset += 2;
  }
  
  const bytes = new Uint8Array(buffer);
  let binary = '';
  for (let i = 0; i < bytes.byteLength; i++) binary += String.fromCharCode(bytes[i]);
  return btoa(binary);
}
