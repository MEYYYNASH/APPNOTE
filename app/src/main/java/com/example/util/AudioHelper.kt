package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Base64
import com.example.model.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

data class RingtonePreset(
    val id: String,
    val labelEn: String,
    val labelKm: String,
    val description: String,
    val frequencies: DoubleArray,
    val tempoHz: Double
)

data class VibrationPreset(
    val id: String,
    val labelEn: String,
    val labelKm: String,
    val timings: LongArray
)

object AudioHelper {

    private var activeMediaPlayer: MediaPlayer? = null
    private var activeAudioTrack: AudioTrack? = null

    val RINGTONE_PRESETS: List<RingtonePreset> = listOf(
        RingtonePreset(
            id = "crystal_chime",
            labelEn = "Crystal Chime",
            labelKm = "សំឡេងគ្រីស្តាល់",
            description = "Bright harmonic glass arpeggio",
            frequencies = doubleArrayOf(523.25, 659.25, 783.99, 1046.50, 1318.51, 1046.50),
            tempoHz = 3.2
        ),
        RingtonePreset(
            id = "glass_bell",
            labelEn = "Glass Bell",
            labelKm = "ជួងកញ្ចក់",
            description = "Warm resonant bell chime",
            frequencies = doubleArrayOf(587.33, 880.00, 1174.66, 880.00),
            tempoHz = 2.2
        ),
        RingtonePreset(
            id = "harmonic_pulse",
            labelEn = "Harmonic Pulse",
            labelKm = "ចង្វាក់អាម៉ូនី",
            description = "Modern synth pulse chord",
            frequencies = doubleArrayOf(440.00, 554.37, 659.25, 880.00, 659.25, 554.37),
            tempoHz = 4.0
        ),
        RingtonePreset(
            id = "soft_marimba",
            labelEn = "Soft Marimba",
            labelKm = "ម៉ារីមបាទន់ភ្លន់",
            description = "Wooden mallet melodic cadence",
            frequencies = doubleArrayOf(392.00, 493.88, 587.33, 783.99, 587.33, 493.88),
            tempoHz = 3.6
        ),
        RingtonePreset(
            id = "morning_harp",
            labelEn = "Morning Harp",
            labelKm = "ពិណពេលព្រឹក",
            description = "Ascending gentle harp glissando",
            frequencies = doubleArrayOf(329.63, 392.00, 493.88, 587.33, 659.25, 783.99, 987.77),
            tempoHz = 4.5
        ),
        RingtonePreset(
            id = "digital_beep",
            labelEn = "Digital Alert",
            labelKm = "សំឡេងឌីជីថល",
            description = "Crisp classic digital double-beep",
            frequencies = doubleArrayOf(987.77, 0.0, 987.77, 0.0, 1318.51, 0.0),
            tempoHz = 5.5
        ),
        RingtonePreset(
            id = "zen_bowl",
            labelEn = "Zen Singing Bowl",
            labelKm = "ចានសមាធិ",
            description = "Deep meditative singing bowl sustain",
            frequencies = doubleArrayOf(256.00, 384.00, 512.00),
            tempoHz = 1.2
        ),
        RingtonePreset(
            id = "urgent_double",
            labelEn = "Urgent Chime",
            labelKm = "សំឡេងបន្ទាន់",
            description = "High-clarity dual octave alert",
            frequencies = doubleArrayOf(880.00, 1760.00, 880.00, 1760.00, 1318.51, 1760.00),
            tempoHz = 6.0
        )
    )

    val VIBRATION_PRESETS: List<VibrationPreset> = listOf(
        VibrationPreset(
            id = "default",
            labelEn = "Standard Pulse",
            labelKm = "ញ័រធម្មតា",
            timings = longArrayOf(0L, 350L)
        ),
        VibrationPreset(
            id = "short_pulse",
            labelEn = "Short Tap",
            labelKm = "ញ័រខ្លី",
            timings = longArrayOf(0L, 140L)
        ),
        VibrationPreset(
            id = "double_beat",
            labelEn = "Double Beat",
            labelKm = "ញ័រពីរដង",
            timings = longArrayOf(0L, 180L, 110L, 220L)
        ),
        VibrationPreset(
            id = "heartbeat",
            labelEn = "Heartbeat",
            labelKm = "ចង្វាក់បេះដូង",
            timings = longArrayOf(0L, 120L, 90L, 180L, 350L, 120L, 90L, 180L)
        ),
        VibrationPreset(
            id = "rapid_alert",
            labelEn = "Rapid Alert",
            labelKm = "ញ័រញាប់",
            timings = longArrayOf(0L, 100L, 70L, 100L, 70L, 100L, 70L, 180L)
        ),
        VibrationPreset(
            id = "sos",
            labelEn = "SOS Pattern",
            labelKm = "ចង្វាក់ SOS",
            timings = longArrayOf(0L, 100L, 80L, 100L, 80L, 100L, 180L, 280L, 120L, 280L)
        ),
        VibrationPreset(
            id = "long_buzz",
            labelEn = "Long Buzz",
            labelKm = "ញ័រវែង",
            timings = longArrayOf(0L, 650L)
        )
    )

    fun ensureMediaVolumeAudible(context: Context, targetPercent: Int = 80, separateControl: Boolean = true) {
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val stream = if (separateControl) AudioManager.STREAM_MUSIC else AudioManager.STREAM_NOTIFICATION
            val maxVol = am.getStreamMaxVolume(stream).coerceAtLeast(1)
            val desiredVol = ((maxVol * (targetPercent.coerceIn(10, 100) / 100f)).toInt()).coerceIn(1, maxVol)
            val curVol = am.getStreamVolume(stream)
            if (separateControl || curVol < (maxVol * 0.35f).toInt()) {
                am.setStreamVolume(stream, desiredVol, 0)
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

    fun triggerVibrationPattern(context: Context, patternId: String) {
        try {
            val preset = VIBRATION_PRESETS.find { it.id == patternId } ?: VIBRATION_PRESETS.first()
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(preset.timings, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(preset.timings, -1)
                }
            }
        } catch (_: Exception) {
        }
    }

    fun generateRingtoneWavBase64(
        styleId: String = "crystal_chime",
        durationSec: Int = 4,
        gradualFadeIn: Boolean = false
    ): String {
        val preset = RINGTONE_PRESETS.find { it.id == styleId } ?: RINGTONE_PRESETS.first()
        val sampleRate = 22050
        val seconds = durationSec.coerceIn(2, 30)
        val totalSamples = sampleRate * seconds
        val pcmBytes = ByteArray(totalSamples * 2)

        val freqs = preset.frequencies
        val tempo = preset.tempoHz
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val stepPos = t * tempo
            val stepIndex = (stepPos.toInt()) % freqs.size
            val localT = stepPos - stepPos.toInt()
            val f0 = freqs[stepIndex]

            val fadeFactor = if (gradualFadeIn) {
                (t / (seconds.toDouble() * 0.65)).coerceIn(0.12, 1.0)
            } else {
                1.0
            }

            val sampleVal = if (f0 <= 1.0) {
                0.0
            } else {
                val envelope = exp(-localT * 1.9) * (1.0 - exp(-localT * 50.0))
                (
                    0.65 * sin(2.0 * PI * f0 * t) +
                        0.23 * sin(2.0 * PI * (f0 * 1.5) * t) +
                        0.12 * sin(2.0 * PI * (f0 * 2.0) * t)
                    ) * envelope * fadeFactor
            }

            val sampleInt = (sampleVal * 26500.0).toInt().coerceIn(-32760, 32760).toShort()
            pcmBytes[i * 2] = (sampleInt.toInt() and 0xFF).toByte()
            pcmBytes[i * 2 + 1] = ((sampleInt.toInt() shr 8) and 0xFF).toByte()
        }

        val wavBytes = wrapPcmInRiffWav(pcmBytes, sampleRate, 1, 16)
        return Base64.encodeToString(wavBytes, Base64.NO_WRAP)
    }

    fun previewRingtoneStyle(
        context: Context,
        styleId: String,
        customSoundBase64: String = "",
        volumePercent: Int = 85
    ) {
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            playReminderPreviewOrAlert(
                context = context,
                settings = AppSettings(
                    reminderSoundEnabled = true,
                    reminderRingtoneStyle = styleId,
                    customSoundBase64 = customSoundBase64,
                    reminderVolumePercent = volumePercent
                ),
                overrideStyleId = styleId,
                overrideVolumePercent = volumePercent
            )
        }
    }

    suspend fun playReminderPreviewOrAlert(
        context: Context,
        settings: AppSettings,
        overrideStyleId: String? = null,
        overrideAlertMode: String? = null,
        overrideVibrationPattern: String? = null,
        overrideVolumePercent: Int? = null
    ) = withContext(Dispatchers.IO) {
        stopActivePlayback()
        if (!settings.reminderSoundEnabled && overrideAlertMode == null) return@withContext

        val alertMode = (overrideAlertMode?.takeIf { it.isNotBlank() } ?: settings.reminderAlertMode)
        val vibPattern = (overrideVibrationPattern?.takeIf { it.isNotBlank() } ?: settings.customVibrationPattern)
        val volPercent = (overrideVolumePercent?.takeIf { it >= 0 } ?: settings.reminderVolumePercent).coerceIn(0, 100)
        val styleId = (overrideStyleId?.takeIf { it.isNotBlank() } ?: settings.reminderRingtoneStyle)

        if (alertMode == "silent") {
            return@withContext
        }

        if (alertMode == "vibration_only" || alertMode == "sound_vibration") {
            triggerVibrationPattern(context, vibPattern)
            if (alertMode == "vibration_only") return@withContext
        }

        if (volPercent == 0) return@withContext
        ensureMediaVolumeAudible(context, volPercent, settings.separateSoundVolumeControl)

        val repeatCount = settings.repeatRingtoneCount.coerceIn(1, 5)
        val durationSec = settings.reminderSoundDurationSec.coerceIn(2, 15)
        val volumeScalar = (volPercent / 100f).coerceIn(0.05f, 1.0f)

        // 1. If Default Phone Ringtone is enabled and no custom override style was requested
        if (settings.useDefaultPhoneRingtone && overrideStyleId.isNullOrBlank()) {
            try {
                val defaultUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                if (defaultUri != null) {
                    val ringtone = RingtoneManager.getRingtone(context, defaultUri)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ringtone?.volume = volumeScalar
                    }
                    ringtone?.play()
                    delay((durationSec * 1000L).coerceAtMost(5000L))
                    ringtone?.stop()
                    return@withContext
                }
            } catch (_: Exception) {
            }
        }

        // 2. If Custom Imported Sound Base64 is selected
        if (styleId == "custom_imported" && settings.customSoundBase64.isNotBlank()) {
            for (r in 0 until repeatCount) {
                playVoiceBase64(
                    context = context,
                    dataBase64 = settings.customSoundBase64,
                    durationSec = durationSec,
                    volumeScalar = volumeScalar,
                    gradualFadeIn = settings.graduallyIncreaseVolume,
                    onProgress = {}
                )
            }
            return@withContext
        }

        // 3. Synthesized preset ringtone style
        val wavB64 = generateRingtoneWavBase64(
            styleId = styleId,
            durationSec = durationSec,
            gradualFadeIn = settings.graduallyIncreaseVolume
        )
        for (r in 0 until repeatCount) {
            playVoiceBase64(
                context = context,
                dataBase64 = wavB64,
                durationSec = durationSec,
                volumeScalar = volumeScalar,
                gradualFadeIn = settings.graduallyIncreaseVolume,
                onProgress = {}
            )
        }
    }

    /**
     * Generates a standard 44-byte RIFF WAVE file (16-bit PCM mono at 22050 Hz)
     * with a rich, loud, clear acoustic voice/chime progression so playback is always
     * clearly audible on both physical devices and cloud streaming emulators.
     */
    fun generateAudibleWavBase64(durationSec: Int = 3): String {
        return generateRingtoneWavBase64("crystal_chime", durationSec.coerceIn(2, 10), false)
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
        header.putInt(16)
        header.putShort(1)
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
        volumeScalar: Float = 1.0f,
        gradualFadeIn: Boolean = false,
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
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            mp.setDataSource(tempFile.absolutePath)
            mp.prepare()
            val initialVol = if (gradualFadeIn) (volumeScalar * 0.15f) else volumeScalar
            mp.setVolume(initialVol, initialVol)
            mp.start()
            playedWithMediaPlayer = true

            val totalMs = mp.duration.takeIf { it > 300 } ?: (durationSec.coerceAtLeast(2) * 1000)
            val startMs = System.currentTimeMillis()
            while (mp.isPlaying) {
                val elapsed = System.currentTimeMillis() - startMs
                val progress = (elapsed.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
                if (gradualFadeIn) {
                    val curVol = (volumeScalar * (0.15f + 0.85f * progress)).coerceIn(0.05f, 1.0f)
                    mp.setVolume(curVol, curVol)
                }
                onProgress(progress)
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
                    track.setVolume(volumeScalar)
                    track.play()

                    val totalMs = ((pcmLength / 2).toLong() * 1000L / sampleRate).coerceAtLeast(1500L)
                    val steps = 25
                    for (s in 1..steps) {
                        onProgress(s.toFloat() / steps)
                        delay(totalMs / steps)
                    }
                }
            } catch (_: Exception) {
                try {
                    val tone = ToneGenerator(AudioManager.STREAM_MUSIC, (volumeScalar * 100).toInt().coerceIn(10, 100))
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
