package com.example.data

import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class ApiTestResult {
    data class Connected(val latencyMs: Long, val message: String) : ApiTestResult()
    data class InvalidKey(val message: String) : ApiTestResult()
    data class QuotaExceeded(val message: String) : ApiTestResult()
    data class NetworkError(val message: String) : ApiTestResult()
}

data class StoryGenerationResult(
    val projectTitle: String,
    val scenes: List<MotionScene>,
    val usedAi: Boolean,
    val statusNotice: String
)

class GeminiStoryService {

    // IMPORTANT: No HttpLoggingInterceptor is attached so API keys are NEVER logged to Logcat.
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun testConnection(apiKey: String): ApiTestResult = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isEmpty()) {
            return@withContext ApiTestResult.InvalidKey(
                "API Key belum dimasukkan. Silakan masukkan Google Gemini API Key terlebih dahulu."
            )
        }
        if (cleanKey.length < 20 || cleanKey == "MY_GEMINI_API_KEY") {
            return@withContext ApiTestResult.InvalidKey(
                "Format API Key tidak valid. Pastikan API Key dimulai dengan 'AIza...' dari Google AI Studio."
            )
        }

        val startTime = System.currentTimeMillis()
        val payload = JSONObject().apply {
            put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", "Reply with OK"))
                    )
                )
            )
            put(
                "generationConfig",
                JSONObject().put("maxOutputTokens", 8)
            )
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$GEMINI_MODEL:generateContent")
            .addHeader("x-goog-api-key", cleanKey)
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val elapsed = (System.currentTimeMillis() - startTime).coerceAtLeast(1L)
                val bodyStr = response.body?.string().orEmpty()

                when {
                    response.isSuccessful -> {
                        ApiTestResult.Connected(
                            latencyMs = elapsed,
                            message = "Terhubung ke Gemini API ($GEMINI_MODEL • ${elapsed}ms)"
                        )
                    }
                    response.code == 429 || bodyStr.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
                        bodyStr.contains("quota", ignoreCase = true) -> {
                        ApiTestResult.QuotaExceeded(
                            "Kuota API habis (HTTP 429: Resource Exhausted). Periksa batas kuota atau billing di Google AI Studio."
                        )
                    }
                    response.code in listOf(400, 401, 403) ||
                        bodyStr.contains("API_KEY_INVALID", ignoreCase = true) ||
                        bodyStr.contains("API key not valid", ignoreCase = true) -> {
                        ApiTestResult.InvalidKey(
                            "API Key salah atau tidak valid (HTTP ${response.code}). Periksa kembali Google Gemini API Key Anda."
                        )
                    }
                    else -> {
                        ApiTestResult.NetworkError(
                            "Koneksi gagal dengan status HTTP ${response.code}. Coba lagi beberapa saat."
                        )
                    }
                }
            }
        } catch (_: IOException) {
            ApiTestResult.NetworkError(
                "Gagal terhubung ke server Gemini API. Periksa koneksi internet perangkat Anda."
            )
        } catch (_: Exception) {
            ApiTestResult.NetworkError(
                "Terjadi kesalahan saat menguji koneksi Gemini API."
            )
        }
    }

    suspend fun generateStoryIdea(
        apiKey: String,
        theme: StoryTheme,
        language: StoryLanguage
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isNotEmpty() && cleanKey != "MY_GEMINI_API_KEY" && cleanKey.length >= 20) {
            try {
                val langInstruction = if (language == StoryLanguage.INDONESIAN) {
                    "Tulis dalam Bahasa Indonesia yang menarik, sinematik, dan ringkas (2-3 kalimat)."
                } else {
                    "Write in catchy, cinematic English (2-3 sentences)."
                }
                val promptText = """
                    Buat 1 ide cerita video animasi pendek 9:16 bertema "${if (language == StoryLanguage.INDONESIAN) theme.labelId else theme.labelEn}".
                    $langInstruction
                    Langsung tulis isi idenya tanpa tanda kutip atau pembuka.
                """.trimIndent()

                val payload = JSONObject().apply {
                    put(
                        "contents",
                        JSONArray().put(
                            JSONObject().put(
                                "parts",
                                JSONArray().put(JSONObject().put("text", promptText))
                            )
                        )
                    )
                }

                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/$GEMINI_MODEL:generateContent")
                    .addHeader("x-goog-api-key", cleanKey)
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string().orEmpty()
                        val text = extractCandidateText(bodyStr).trim()
                        if (text.isNotEmpty()) {
                            return@withContext text to "Ide cerita berhasil dibuat dengan Gemini AI."
                        }
                    }
                }
            } catch (_: Exception) {
                // Fall through to smart preset idea
            }
        }

        val fallbackIdea = getFallbackIdea(theme, language)
        val notice = if (cleanKey.isEmpty()) {
            "Menggunakan ide cerita pintar studio (Masukkan Gemini API Key di menu Settings untuk kustomisasi AI penuh)."
        } else {
            "Ide cerita siap digunakan."
        }
        fallbackIdea to notice
    }

    suspend fun generateFullScriptAndScenes(
        apiKey: String,
        storyPrompt: String,
        theme: StoryTheme,
        language: StoryLanguage,
        targetDurationSeconds: Int,
        animationStyle: AnimationStyle
    ): StoryGenerationResult = withContext(Dispatchers.IO) {
        val cleanPrompt = storyPrompt.trim().ifEmpty {
            getFallbackIdea(theme, language)
        }
        val sceneCount = when (targetDurationSeconds) {
            45 -> 5
            60 -> 6
            else -> 4
        }
        val baseDuration = targetDurationSeconds / sceneCount
        val remainder = targetDurationSeconds % sceneCount

        val cleanKey = apiKey.trim()
        if (cleanKey.isNotEmpty() && cleanKey != "MY_GEMINI_API_KEY" && cleanKey.length >= 20) {
            try {
                val langName = if (language == StoryLanguage.INDONESIAN) "Bahasa Indonesia" else "English"
                val systemPrompt = """
                    You are MOTION LAB AI, a professional 2D animation & motion graphics director.
                    Create a complete animated video script divided into exactly $sceneCount scenes for a $targetDurationSeconds-second vertical (9:16) video.
                    Story Idea: "$cleanPrompt"
                    Theme: "${theme.labelEn}"
                    Visual Style: "${animationStyle.displayName}"
                    Language for all titles, headlines, narrations, and descriptions: $langName.

                    Respond ONLY with valid JSON matching this exact structure (no markdown fences):
                    {
                      "projectTitle": "Short catchy title",
                      "scenes": [
                        {
                          "title": "Scene 1 • Hook",
                          "headlineText": "UPPERCASE SHORT KINETIC HEADLINE (max 5 words)",
                          "narrationText": "Spoken voiceover narration for subtitles and TTS (1-2 engaging sentences)",
                          "visualDescription": "Visual description of character and motion graphics in this scene",
                          "backgroundPreset": "cyber_city | neon_grid | sunset_horizon | deep_space | studio_minimal | data_matrix",
                          "primaryColorHex": "#00E5FF",
                          "secondaryColorHex": "#8B5CF6",
                          "characterType": "robot_ai | astronaut | creator_hero | business_pro | pixel_cat",
                          "characterMotion": "walk_in | bounce | float_wave | dance_pulse | zoom_hero",
                          "objectType": "rocket_ship | growth_chart | spark_stars | idea_bulbs | data_orbs | floating_coins",
                          "objectMotion": "orbit | slide_across | float_up | spin_pulse | particle_rain",
                          "textAnimIn": "typewriter | slide_up | pop_elastic | glitch_in | fade_scale",
                          "textAnimOut": "fade_out | slide_down | zoom_out",
                          "transitionToNext": "crossfade | wipe_left | circle_iris | glitch_flash | zoom_through",
                          "sfxOnEnter": "whoosh | pop_bubble | digital_chime | glitch_zap | level_up"
                        }
                      ]
                    }
                """.trimIndent()

                val payload = JSONObject().apply {
                    put(
                        "contents",
                        JSONArray().put(
                            JSONObject().put(
                                "parts",
                                JSONArray().put(JSONObject().put("text", systemPrompt))
                            )
                        )
                    )
                    put(
                        "generationConfig",
                        JSONObject().apply {
                            put("temperature", 0.7)
                            put("responseMimeType", "application/json")
                        }
                    )
                }

                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/$GEMINI_MODEL:generateContent")
                    .addHeader("x-goog-api-key", cleanKey)
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string().orEmpty()
                    if (response.isSuccessful) {
                        val candidateJson = extractCandidateText(bodyStr)
                            .removePrefix("```json")
                            .removePrefix("```")
                            .removeSuffix("```")
                            .trim()
                        val parsed = parseAiSceneResponse(
                            jsonStr = candidateJson,
                            sceneCount = sceneCount,
                            baseDuration = baseDuration,
                            remainder = remainder
                        )
                        if (parsed != null && parsed.second.isNotEmpty()) {
                            return@withContext StoryGenerationResult(
                                projectTitle = parsed.first,
                                scenes = parsed.second,
                                usedAi = true,
                                statusNotice = "Naskah & ${parsed.second.size} scene berhasil dibuat oleh Gemini AI ($targetDurationSeconds detik)."
                            )
                        }
                    } else if (response.code == 429) {
                        val fallback = buildSmartLocalScenes(
                            prompt = cleanPrompt,
                            theme = theme,
                            language = language,
                            sceneCount = sceneCount,
                            baseDuration = baseDuration,
                            remainder = remainder
                        )
                        return@withContext StoryGenerationResult(
                            projectTitle = fallback.first,
                            scenes = fallback.second,
                            usedAi = false,
                            statusNotice = "Kuota Gemini API habis (HTTP 429). Menggunakan Smart Scene Engine lokal agar proyek tetap berjalan."
                        )
                    } else if (response.code in listOf(400, 401, 403)) {
                        val fallback = buildSmartLocalScenes(
                            prompt = cleanPrompt,
                            theme = theme,
                            language = language,
                            sceneCount = sceneCount,
                            baseDuration = baseDuration,
                            remainder = remainder
                        )
                        return@withContext StoryGenerationResult(
                            projectTitle = fallback.first,
                            scenes = fallback.second,
                            usedAi = false,
                            statusNotice = "API Key tidak valid (HTTP ${response.code}). Menggunakan Smart Scene Engine lokal — periksa Settings API."
                        )
                    }
                }
            } catch (_: Exception) {
                // Fall back gracefully without crashing
            }
        }

        val fallback = buildSmartLocalScenes(
            prompt = cleanPrompt,
            theme = theme,
            language = language,
            sceneCount = sceneCount,
            baseDuration = baseDuration,
            remainder = remainder
        )
        val msg = if (cleanKey.isEmpty()) {
            "Naskah & ${fallback.second.size} scene dibuat dengan Smart Studio Engine (Tambahkan Gemini API Key di Settings untuk generasi AI cloud)."
        } else {
            "Naskah & ${fallback.second.size} scene siap diedit ($targetDurationSeconds detik)."
        }
        StoryGenerationResult(
            projectTitle = fallback.first,
            scenes = fallback.second,
            usedAi = false,
            statusNotice = msg
        )
    }

    private fun extractCandidateText(rawResponse: String): String {
        val root = JSONObject(rawResponse)
        val candidates = root.optJSONArray("candidates") ?: return ""
        if (candidates.length() == 0) return ""
        val content = candidates.getJSONObject(0).optJSONObject("content") ?: return ""
        val parts = content.optJSONArray("parts") ?: return ""
        if (parts.length() == 0) return ""
        return parts.getJSONObject(0).optString("text", "")
    }

    private fun parseAiSceneResponse(
        jsonStr: String,
        sceneCount: Int,
        baseDuration: Int,
        remainder: Int
    ): Pair<String, List<MotionScene>>? {
        return try {
            val obj = JSONObject(jsonStr)
            val title = obj.optString("projectTitle", "Motion AI Story").ifBlank { "Motion AI Story" }
            val arr = obj.optJSONArray("scenes") ?: return null
            val list = mutableListOf<MotionScene>()
            val count = arr.length().coerceAtMost(8)
            for (i in 0 until count) {
                val s = arr.getJSONObject(i)
                val dur = baseDuration + if (i < remainder) 1 else 0
                list.add(
                    MotionScene(
                        id = "scene_ai_${System.currentTimeMillis()}_$i",
                        title = s.optString("title", "Scene ${i + 1}").ifBlank { "Scene ${i + 1}" },
                        headlineText = s.optString("headlineText", "SCENE ${i + 1}").uppercase(),
                        narrationText = s.optString("narrationText", ""),
                        visualDescription = s.optString("visualDescription", ""),
                        durationSeconds = dur.coerceIn(4, 15),
                        backgroundPreset = BackgroundPreset.fromId(s.optString("backgroundPreset")),
                        primaryColorHex = s.optString("primaryColorHex", "#00E5FF"),
                        secondaryColorHex = s.optString("secondaryColorHex", "#8B5CF6"),
                        characterType = CharacterType.fromId(s.optString("characterType")),
                        characterMotion = CharacterMotion.fromId(s.optString("characterMotion")),
                        characterSpeed = 1.05f,
                        objectType = MovingObjectType.fromId(s.optString("objectType")),
                        objectMotion = ObjectMotion.fromId(s.optString("objectMotion")),
                        textAnimIn = TextAnimIn.fromId(s.optString("textAnimIn")),
                        textAnimOut = TextAnimOut.fromId(s.optString("textAnimOut")),
                        transitionToNext = SceneTransition.fromId(s.optString("transitionToNext")),
                        sfxOnEnter = SfxType.fromId(s.optString("sfxOnEnter"))
                    )
                )
            }
            if (list.isEmpty()) null else (title to list)
        } catch (_: Exception) {
            null
        }
    }

    private fun getFallbackIdea(theme: StoryTheme, language: StoryLanguage): String {
        val isId = language == StoryLanguage.INDONESIAN
        return when (theme) {
            StoryTheme.TECH_AI -> if (isId) {
                "Perjalanan asisten robot pintar yang membantu kreator muda mengubah sketsa sederhana menjadi video animasi futuristik yang memukau dunia."
            } else {
                "A smart AI companion robot helps a young creator transform simple sketches into futuristic motion graphics that captivate the world."
            }
            StoryTheme.EDUCATION -> if (isId) {
                "Bagaimana cara kerja lubang hitam dan gravitasi di luar angkasa dijelaskan secara visual dalam 45 detik yang seru dan mudah dipahami."
            } else {
                "How black holes bend time and light in deep space, explained visually in an exciting step-by-step animated journey."
            }
            StoryTheme.BUSINESS -> if (isId) {
                "Strategi startup lokal menembus pasar global dengan inovasi produk digital dan otomatisasi cerdas yang meningkatkan pertumbuhan 300 persen."
            } else {
                "How a lean digital startup scales globally using smart automation and bold motion storytelling to triple growth."
            }
            StoryTheme.STORYTELLING -> if (isId) {
                "Petualangan kucing siber dan astronot penjelajah yang menemukan planet kristal bercahaya di ujung galaksi Andromeda."
            } else {
                "A cyber cat and a lone astronaut discover a glowing crystalline planet pulsing with musical energy at the edge of the galaxy."
            }
            StoryTheme.MOTIVATION -> if (isId) {
                "Kekuatan konsistensi 1 persen setiap hari: bagaimana langkah kecil hari ini membangun lompatan besar di masa depan."
            } else {
                "The power of improving 1 percent every single day: how small daily habits compound into extraordinary breakthroughs."
            }
            StoryTheme.PRODUCT_PROMO -> if (isId) {
                "Peluncuran aplikasi generasi baru yang menggabungkan desain kilat, analitik real-time, dan kolaborasi tanpa batas dalam satu sentuhan."
            } else {
                "Launching the next-generation creative platform that blends instant design, live analytics, and seamless collaboration in one tap."
            }
        }
    }

    private fun buildSmartLocalScenes(
        prompt: String,
        theme: StoryTheme,
        language: StoryLanguage,
        sceneCount: Int,
        baseDuration: Int,
        remainder: Int
    ): Pair<String, List<MotionScene>> {
        val isId = language == StoryLanguage.INDONESIAN
        val words = prompt.split(Regex("\\s+")).filter { it.length > 2 }
        val topicKeyword = words.take(3).joinToString(" ").uppercase().ifBlank {
            if (isId) "INOVASI MASA DEPAN" else "FUTURE INNOVATION"
        }
        val projectTitle = words.take(4).joinToString(" ").replaceFirstChar { it.uppercase() }
            .ifBlank { if (isId) "Proyek Animasi Motion Lab" else "Motion Lab AI Story" }

        val presets = BackgroundPreset.entries
        val characters = listOf(
            CharacterType.ROBOT_AI,
            CharacterType.CREATOR_HERO,
            CharacterType.ASTRONAUT,
            CharacterType.BUSINESS_PRO,
            CharacterType.PIXEL_CAT
        )
        val charMotions = listOf(
            CharacterMotion.ZOOM_HERO,
            CharacterMotion.WALK_IN,
            CharacterMotion.FLOAT_WAVE,
            CharacterMotion.BOUNCE,
            CharacterMotion.DANCE_PULSE
        )
        val objects = listOf(
            MovingObjectType.IDEA_BULBS,
            MovingObjectType.GROWTH_CHART,
            MovingObjectType.ROCKET_SHIP,
            MovingObjectType.DATA_ORBS,
            MovingObjectType.SPARK_STARS,
            MovingObjectType.FLOATING_COINS
        )
        val objMotions = ObjectMotion.entries
        val textIns = TextAnimIn.entries
        val transitions = listOf(
            SceneTransition.CIRCLE_IRIS,
            SceneTransition.WIPE_LEFT,
            SceneTransition.GLITCH_FLASH,
            SceneTransition.ZOOM_THROUGH,
            SceneTransition.CROSSFADE
        )
        val sfxList = listOf(
            SfxType.WHOOSH,
            SfxType.POP_BUBBLE,
            SfxType.DIGITAL_CHIME,
            SfxType.GLITCH_ZAP,
            SfxType.LEVEL_UP
        )

        val scenes = (0 until sceneCount).map { idx ->
            val dur = baseDuration + if (idx < remainder) 1 else 0
            val bg = presets[idx % presets.size]
            val stageHeadline = when (idx) {
                0 -> topicKeyword
                1 -> if (isId) "TANTANGAN & SOLUSI CERDAS" else "THE SMART BREAKTHROUGH"
                2 -> if (isId) "TRANSFORMASI VISUAL NYATA" else "VISUAL TRANSFORMATION"
                sceneCount - 1 -> if (isId) "SAATNYA MULAI SEKARANG!" else "START CREATING TODAY!"
                else -> if (isId) "AKSELERASI TANPA BATAS" else "LIMITLESS ACCELERATION"
            }
            val narration = when (idx) {
                0 -> prompt
                1 -> if (isId) {
                    "Dengan pendekatan baru di bidang ${theme.labelId}, setiap proses kini bergerak jauh lebih cepat dan terarah."
                } else {
                    "With a bold new approach in ${theme.labelEn}, every step moves faster and with crystal clarity."
                }
                2 -> if (isId) {
                    "Lihat bagaimana elemen visual, data interaktif, dan karakter bergerak berpadu secara harmonis dalam satu alur cerita."
                } else {
                    "Watch how dynamic visuals, interactive data, and expressive motion blend seamlessly into one story."
                }
                sceneCount - 1 -> if (isId) {
                    "Inilah waktunya mewujudkan ide terbaikmu dan membagikannya ke seluruh dunia dalam resolusi tinggi!"
                } else {
                    "Now is the moment to bring your biggest ideas to life and share them with the world in full HD!"
                }
                else -> if (isId) {
                    "Momentum terus meningkat saat teknologi otomatisasi bekerja mendukung kreativitas tanpa batas."
                } else {
                    "Momentum keeps building as smart automation amplifies human creativity without limits."
                }
            }
            val visualDesc = if (isId) {
                "Adegan ${idx + 1}: ${characters[idx % characters.size].label} bergerak dengan gaya ${charMotions[idx % charMotions.size].label} bersama ${objects[idx % objects.size].label}."
            } else {
                "Scene ${idx + 1}: ${characters[idx % characters.size].label} animates with ${charMotions[idx % charMotions.size].label} alongside ${objects[idx % objects.size].label}."
            }

            MotionScene(
                id = "scene_gen_${System.currentTimeMillis()}_$idx",
                title = "Scene ${idx + 1} • ${
                    when (idx) {
                        0 -> "Opening Hook"
                        sceneCount - 1 -> "Climax & Finale"
                        else -> "Story Beat ${idx + 1}"
                    }
                }",
                headlineText = stageHeadline,
                narrationText = narration,
                visualDescription = visualDesc,
                durationSeconds = dur.coerceIn(4, 15),
                backgroundPreset = bg,
                primaryColorHex = bg.secondaryHex,
                secondaryColorHex = if (idx % 2 == 0) "#8B5CF6" else "#00E5FF",
                characterType = characters[idx % characters.size],
                characterMotion = charMotions[idx % charMotions.size],
                characterSpeed = 1.0f + (idx % 3) * 0.1f,
                objectType = objects[idx % objects.size],
                objectMotion = objMotions[idx % objMotions.size],
                textAnimIn = textIns[idx % textIns.size],
                textAnimOut = TextAnimOut.entries[idx % TextAnimOut.entries.size],
                transitionToNext = transitions[idx % transitions.size],
                sfxOnEnter = sfxList[idx % sfxList.size]
            )
        }
        return projectTitle to scenes
    }

    companion object {
        private const val GEMINI_MODEL = "gemini-2.5-flash"
    }
}
