package com.example.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.speech.tts.TextToSpeech
import com.example.model.BgmTrack
import com.example.model.SfxType
import com.example.model.StoryLanguage
import kotlinx.coroutines.*
import java.util.Locale
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class StudioAudioEngine(context: Context) {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var lastSpokenSceneId: String? = null

    private val audioScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var bgmJob: Job? = null

    init {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isTtsReady = true
                }
            }
        } catch (_: Exception) {
            isTtsReady = false
        }
    }

    fun speakSceneNarrationIfChanged(
        sceneId: String,
        narrationText: String,
        language: StoryLanguage,
        enabled: Boolean,
        pitch: Float,
        speed: Float
    ) {
        if (!enabled || !isTtsReady || narrationText.isBlank()) return
        if (lastSpokenSceneId == sceneId) return
        lastSpokenSceneId = sceneId

        try {
            val engine = tts ?: return
            val locale = if (language == StoryLanguage.INDONESIAN) {
                Locale.forLanguageTag("id-ID")
            } else {
                Locale.US
            }
            engine.language = locale
            engine.setPitch(pitch.coerceIn(0.6f, 1.6f))
            engine.setSpeechRate(speed.coerceIn(0.6f, 1.8f))
            engine.speak(narrationText, TextToSpeech.QUEUE_FLUSH, null, "utt_$sceneId")
        } catch (_: Exception) {
            // Ignore TTS hardware unavailability
        }
    }

    fun previewVoiceSample(
        sampleText: String,
        language: StoryLanguage,
        pitch: Float,
        speed: Float
    ) {
        if (!isTtsReady || sampleText.isBlank()) return
        try {
            val engine = tts ?: return
            engine.language = if (language == StoryLanguage.INDONESIAN) {
                Locale.forLanguageTag("id-ID")
            } else {
                Locale.US
            }
            engine.setPitch(pitch.coerceIn(0.6f, 1.6f))
            engine.setSpeechRate(speed.coerceIn(0.6f, 1.8f))
            engine.speak(sampleText, TextToSpeech.QUEUE_FLUSH, null, "utt_preview")
        } catch (_: Exception) {
        }
    }

    fun resetSceneVoiceTracker() {
        lastSpokenSceneId = null
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
    }

    fun startBgmLoop(track: BgmTrack, volume: Float) {
        stopBgmLoop()
        if (track == BgmTrack.NONE || volume <= 0.02f) return

        bgmJob = audioScope.launch {
            val sampleRate = 22050
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(2048)

            var audioTrack: AudioTrack? = null
            try {
                audioTrack = AudioTrack.Builder()
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
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack.play()

                val notesHz = when (track) {
                    BgmTrack.CYBER_SYNTHWAVE -> floatArrayOf(110f, 130.81f, 164.81f, 220f, 196f, 164.81f, 146.83f, 130.81f)
                    BgmTrack.UPBEAT_MOTION -> floatArrayOf(261.63f, 329.63f, 392.0f, 523.25f, 392.0f, 329.63f, 293.66f, 392.0f)
                    BgmTrack.LOFI_CHILL -> floatArrayOf(174.61f, 220f, 261.63f, 329.63f, 196f, 246.94f, 293.66f, 349.23f)
                    BgmTrack.CINEMATIC_AMBIENT -> floatArrayOf(98f, 146.83f, 196f, 246.94f, 110f, 164.81f, 220f, 261.63f)
                    BgmTrack.RETRO_8BIT -> floatArrayOf(330f, 392f, 659.25f, 523.25f, 587.33f, 783.99f, 659.25f, 523.25f)
                    BgmTrack.NONE -> floatArrayOf(220f)
                }

                val stepSamples = (sampleRate * 60 / track.bpm.coerceAtLeast(70) / 2).coerceAtLeast(1024)
                val chunk = ShortArray(stepSamples)
                var stepIdx = 0
                var phase = 0.0

                while (isActive) {
                    val freq = notesHz[stepIdx % notesHz.size]
                    val amp = (volume.coerceIn(0f, 1f) * 3200f)
                    for (i in 0 until stepSamples) {
                        val env = exp(-2.4 * i.toDouble() / stepSamples.toDouble())
                        val raw = sin(phase)
                        val wave = if (track == BgmTrack.RETRO_8BIT) {
                            if (raw >= 0.0) 0.8 else -0.8
                        } else {
                            raw
                        }
                        chunk[i] = (wave * env * amp).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                        phase += 2.0 * PI * freq / sampleRate
                    }
                    audioTrack.write(chunk, 0, chunk.size)
                    stepIdx++
                }
            } catch (_: Exception) {
                // Ignore audio hardware issues on headless environments
            } finally {
                try {
                    audioTrack?.stop()
                    audioTrack?.release()
                } catch (_: Exception) {
                }
            }
        }
    }

    fun stopBgmLoop() {
        bgmJob?.cancel()
        bgmJob = null
    }

    fun playSfx(sfx: SfxType) {
        if (sfx == SfxType.NONE) return
        audioScope.launch {
            val sampleRate = 22050
            val durationSamples = (sampleRate * 0.26f).toInt()
            val pcm = ShortArray(durationSamples)
            var phase = 0.0

            for (i in 0 until durationSamples) {
                val p = i.toFloat() / durationSamples.toFloat()
                val freq = when (sfx) {
                    SfxType.WHOOSH -> 180f + 520f * sin(p * PI.toFloat())
                    SfxType.POP_BUBBLE -> 320f + 680f * p
                    SfxType.DIGITAL_CHIME -> if (p < 0.5f) 587.33f else 880f
                    SfxType.GLITCH_ZAP -> if ((i / 180) % 2 == 0) 740f else 290f
                    SfxType.LEVEL_UP -> when {
                        p < 0.33f -> 440f
                        p < 0.66f -> 554.37f
                        else -> 659.25f
                    }
                    SfxType.NONE -> 0f
                }
                val env = (1f - p * 0.85f)
                pcm[i] = (sin(phase) * env * 6500f).toInt().toShort()
                phase += 2.0 * PI * freq / sampleRate
            }

            var track: AudioTrack? = null
            try {
                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(pcm, 0, pcm.size)
                track.play()
                delay(300)
            } catch (_: Exception) {
            } finally {
                try {
                    track?.stop()
                    track?.release()
                } catch (_: Exception) {
                }
            }
        }
    }

    fun release() {
        stopBgmLoop()
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        }
        audioScope.cancel()
    }
}
