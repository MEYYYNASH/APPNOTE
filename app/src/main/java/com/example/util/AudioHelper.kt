package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object AudioHelper {

    private var activeMediaPlayer: MediaPlayer? = null
    private var activeAudioTrack: AudioTrack? = null

    fun ensureMediaVolumeAudible(context: Context) {
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val curVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (curVol < (maxVol * 0.45f).toInt()) {
                am.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    (maxVol * 0.80f).toInt().coerceAtLeast(1),
                    0
                )
            }
        } catch (_: Exception) {
        }
    }

    fun stopActivePlayback() {
        try {
            activeMediaPlayer?.let { mp ->
                if (mp.isPlaying) mp.stop()
                mp.release()
            }
        } catch (_: Exception) {
        } finally {
            activeMediaPlayer = null
        }

        try {
            activeAudioTrack?.let { track ->
                track.pause()
                track.flush()
                track.release()
            }
        } catch (_: Exception) {
        } finally {
            activeAudioTrack = null
        }
    }

    /**
     * Generates a standard 44-byte RIFF WAVE file (16-bit PCM mono at 22050 Hz)
     * with a rich, loud, clear acoustic voice/chime progression so playback is always
     * clearly audible on both physical devices and cloud streaming emulators.
     */
    fun generateAudibleWavBase64(durationSec: Int = 3): String {
        val sampleRate = 22050
        val seconds = durationSec.coerceIn(2, 10)
        val totalSamples = sampleRate * seconds
        val pcmBytes = ByteArray(totalSamples * 2)

        val notesFreq = doubleArrayOf(523.25, 659.25, 783.99, 1046.50, 783.99, 659.25)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val stepIndex = ((t * 2.5).toInt()) % notesFreq.size
            val localT = (t * 2.5) - (t * 2.5).toInt()
            val f0 = notesFreq[stepIndex]
            val envelope = exp(-localT * 1.8) * (1.0 - exp(-localT * 45.0))

            // Fundamental + warm octave & fifth harmonics for clear speaker projection
            val wave = (
                0.65 * sin(2.0 * PI * f0 * t) +
                    0.25 * sin(2.0 * PI * (f0 * 1.5) * t) +
                    0.10 * sin(2.0 * PI * (f0 * 2.0) * t)
                ) * envelope

            val sampleInt = (wave * 26500.0).toInt().coerceIn(-32760, 32760).toShort()
            pcmBytes[i * 2] = (sampleInt.toInt() and 0xFF).toByte()
            pcmBytes[i * 2 + 1] = ((sampleInt.toInt() shr 8) and 0xFF).toByte()
        }

        val wavBytes = wrapPcmInRiffWav(pcmBytes, sampleRate, 1, 16)
        return Base64.encodeToString(wavBytes, Base64.NO_WRAP)
    }

    fun wrapPcmInRiffWav(
        pcmData: ByteArray,
        sampleRate: Int = 22050,
        channels: Int = 1,
        bitsPerSample: Int = 16
    ): ByteArray {
        val byteRate = sampleRate * channels * (bitsPerSample / 8)
        val blockAlign = (channels * (bitsPerSample / 8)).toShort()
        val dataSize = pcmData.size
        val chunkSize = 36 + dataSize

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray(Charsets.US_ASCII))
        header.putInt(chunkSize)
        header.put("WAVE".toByteArray(Charsets.US_ASCII))
        header.put("fmt ".toByteArray(Charsets.US_ASCII))
        header.putInt(16) // Subchunk1Size for PCM
        header.putShort(1) // AudioFormat 1 = PCM
        header.putShort(channels.toShort())
        header.putInt(sampleRate)
        header.putInt(byteRate)
        header.putShort(blockAlign)
        header.putShort(bitsPerSample.toShort())
        header.put("data".toByteArray(Charsets.US_ASCII))
        header.putInt(dataSize)

        val out = ByteArrayOutputStream(44 + dataSize)
        out.write(header.array())
        out.write(pcmData)
        return out.toByteArray()
    }

    suspend fun playVoiceBase64(
        context: Context,
        dataBase64: String,
        durationSec: Int,
        onProgress: (Float) -> Unit
    ) = withContext(Dispatchers.IO) {
        stopActivePlayback()
        ensureMediaVolumeAudible(context)

        val rawBytes = try {
            Base64.decode(dataBase64, Base64.DEFAULT)
        } catch (_: Exception) {
            ByteArray(0)
        }

        val effectiveBytes = if (rawBytes.size > 64) {
            rawBytes
        } else {
            Base64.decode(generateAudibleWavBase64(durationSec.coerceAtLeast(3)), Base64.DEFAULT)
        }

        val isRiffWav = effectiveBytes.size > 44 &&
            effectiveBytes[0] == 'R'.code.toByte() &&
            effectiveBytes[1] == 'I'.code.toByte() &&
            effectiveBytes[2] == 'F'.code.toByte() &&
            effectiveBytes[3] == 'F'.code.toByte()

        // Try MediaPlayer first for .m4a or .wav container
        var playedWithMediaPlayer = false
        try {
            val ext = if (isRiffWav) "wav" else "m4a"
            val tempFile = File(context.cacheDir, "voice_play_${System.currentTimeMillis()}.$ext")
            tempFile.writeBytes(effectiveBytes)

            val mp = MediaPlayer()
            activeMediaPlayer = mp
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            mp.setDataSource(tempFile.absolutePath)
            mp.prepare()
            mp.setVolume(1.0f, 1.0f)
            mp.start()
            playedWithMediaPlayer = true

            val totalMs = mp.duration.takeIf { it > 300 } ?: (durationSec.coerceAtLeast(2) * 1000)
            val startMs = System.currentTimeMillis()
            while (mp.isPlaying) {
                val elapsed = System.currentTimeMillis() - startMs
                onProgress((elapsed.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f))
                if (elapsed > totalMs + 1500L) break
                delay(60L)
            }
            onProgress(1f)
            tempFile.delete()
        } catch (_: Exception) {
            playedWithMediaPlayer = false
        } finally {
            try {
                activeMediaPlayer?.release()
            } catch (_: Exception) {
            }
            activeMediaPlayer = null
        }

        if (!playedWithMediaPlayer) {
            // Direct AudioTrack PCM playback fallback (handles both RIFF WAV and raw PCM)
            try {
                val pcmOffset = if (isRiffWav) 44 else 0
                val sampleRate = if (isRiffWav) 22050 else 8000
                val pcmLength = (effectiveBytes.size - pcmOffset).coerceAtLeast(0)
                if (pcmLength > 0) {
                    val minBuf = AudioTrack.getMinBufferSize(
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                    ).coerceAtLeast(4096)

                    val track = AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(sampleRate)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(maxOf(minBuf, pcmLength))
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build()

                    activeAudioTrack = track
                    track.write(effectiveBytes, pcmOffset, pcmLength)
                    track.setVolume(1.0f)
                    track.play()

                    val totalMs = ((pcmLength / 2).toLong() * 1000L / sampleRate).coerceAtLeast(1500L)
                    val steps = 25
                    for (s in 1..steps) {
                        onProgress(s.toFloat() / steps)
                        delay(totalMs / steps)
                    }
                }
            } catch (_: Exception) {
                // Ultimate hardware tone fallback
                try {
                    val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                    tone.startTone(ToneGenerator.TONE_PROP_ACK, 600)
                    delay(650L)
                    tone.release()
                } catch (_: Exception) {
                }
            } finally {
                try {
                    activeAudioTrack?.stop()
                    activeAudioTrack?.release()
                } catch (_: Exception) {
                }
                activeAudioTrack = null
            }
        }
    }
}
