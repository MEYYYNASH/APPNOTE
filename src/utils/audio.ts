// Synthetic RIFF WAV generation and Web Audio playback helper

export function generateAudibleWavBase64(durationSeconds = 3): string {
  const sampleRate = 22050;
  const numSamples = Math.floor(sampleRate * durationSeconds);
  const numChannels = 1;
  const bytesPerSample = 2; // 16-bit PCM
  const blockAlign = numChannels * bytesPerSample;
  const byteRate = sampleRate * blockAlign;
  const dataSize = numSamples * blockAlign;
  const buffer = new ArrayBuffer(44 + dataSize);
  const view = new DataView(buffer);

  // RIFF identifier
  writeString(view, 0, 'RIFF');
  view.setUint32(4, 36 + dataSize, true);
  writeString(view, 8, 'WAVE');
  // format chunk identifier
  writeString(view, 12, 'fmt ');
  view.setUint32(16, 16, true); // format chunk size
  view.setUint16(20, 1, true); // sample format: PCM
  view.setUint16(22, numChannels, true);
  view.setUint32(24, sampleRate, true);
  view.setUint32(28, byteRate, true);
  view.setUint16(32, blockAlign, true);
  view.setUint16(34, bytesPerSample * 8, true); // bits per sample
  // data chunk identifier
  writeString(view, 36, 'data');
  view.setUint32(40, dataSize, true);

  // Synthesize a pleasant gentle glass chime chord sequence (C5 - E5 - G5 - C6)
  const notes = [523.25, 659.25, 783.99, 1046.5];
  let offset = 44;
  for (let i = 0; i < numSamples; i++) {
    const t = i / sampleRate;
    const noteIdx = Math.min(Math.floor((t / durationSeconds) * notes.length), notes.length - 1);
    const freq = notes[noteIdx];
    // Envelope: quick attack and exponential decay for each chime
    const segmentDuration = durationSeconds / notes.length;
    const segmentTime = t % segmentDuration;
    const env = Math.exp(-segmentTime * 3.5);
    const sample = Math.sin(2 * Math.PI * freq * t) * 0.45 * env;
    const clamped = Math.max(-1, Math.min(1, sample));
    view.setInt16(offset, clamped < 0 ? clamped * 0x8000 : clamped * 0x7fff, true);
    offset += 2;
  }

  const bytes = new Uint8Array(buffer);
  let binary = '';
  const len = bytes.byteLength;
  for (let i = 0; i < len; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary);
}

function writeString(view: DataView, offset: number, string: string) {
  for (let i = 0; i < string.length; i++) {
    view.setUint8(offset + i, string.charCodeAt(i));
  }
}

let activeAudio: HTMLAudioElement | null = null;
let activeProgressInterval: number | null = null;

export function stopActiveAudio() {
  if (activeProgressInterval) {
    clearInterval(activeProgressInterval);
    activeProgressInterval = null;
  }
  if (activeAudio) {
    try {
      activeAudio.pause();
      activeAudio.currentTime = 0;
    } catch {
      // ignore
    }
    activeAudio = null;
  }
}

export function playBase64Audio(
  base64Data: string,
  onProgress?: (progress: number) => void,
  onEnded?: () => void
): () => void {
  stopActiveAudio();

  const mime = base64Data.startsWith('data:') ? '' : 'data:audio/wav;base64,';
  const audio = new Audio(mime + base64Data);
  activeAudio = audio;

  audio.onplay = () => {
    if (activeProgressInterval) clearInterval(activeProgressInterval);
    activeProgressInterval = window.setInterval(() => {
      if (audio.duration && audio.duration > 0) {
        onProgress?.(audio.currentTime / audio.duration);
      }
    }, 80);
  };

  const handleEnd = () => {
    if (activeProgressInterval) {
      clearInterval(activeProgressInterval);
      activeProgressInterval = null;
    }
    onProgress?.(0);
    onEnded?.();
    if (activeAudio === audio) activeAudio = null;
  };

  audio.onended = handleEnd;
  audio.onerror = handleEnd;

  audio.play().catch(() => {
    // Autoplay or decode fallback
    handleEnd();
  });

  return () => {
    if (activeAudio === audio) {
      stopActiveAudio();
      handleEnd();
    }
  };
}

export function playNotificationChime() {
  const b64 = generateAudibleWavBase64(1.2);
  playBase64Audio(b64);
}
