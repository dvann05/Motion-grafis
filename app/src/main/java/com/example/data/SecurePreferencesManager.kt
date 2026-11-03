package com.example.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.BuildConfig
import com.example.model.*
import com.example.ui.theme.AccentThemeOption
import org.json.JSONArray
import org.json.JSONObject

class SecurePreferencesManager(context: Context) {

    private val appContext = context.applicationContext

    private val sharedPrefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                appContext,
                PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            // Fallback for JVM/Robolectric test environments where AndroidKeyStore is unavailable
            appContext.getSharedPreferences(FALLBACK_PREFS_FILE_NAME, Context.MODE_PRIVATE)
        }
    }

    fun getSavedApiKey(): String {
        val stored = sharedPrefs.getString(KEY_GEMINI_API_KEY, null)?.trim().orEmpty()
        if (stored.isNotEmpty()) {
            return stored
        }
        val envKey = try {
            BuildConfig.GEMINI_API_KEY.trim()
        } catch (_: Exception) {
            ""
        }
        return if (envKey.isNotEmpty() && envKey != "MY_GEMINI_API_KEY" && envKey != "null") {
            envKey
        } else {
            ""
        }
    }

    fun isUsingBuildConfigKey(): Boolean {
        val stored = sharedPrefs.getString(KEY_GEMINI_API_KEY, null)?.trim().orEmpty()
        if (stored.isNotEmpty()) return false
        val envKey = try {
            BuildConfig.GEMINI_API_KEY.trim()
        } catch (_: Exception) {
            ""
        }
        return envKey.isNotEmpty() && envKey != "MY_GEMINI_API_KEY" && envKey != "null"
    }

    fun saveApiKey(apiKey: String) {
        sharedPrefs.edit()
            .putString(KEY_GEMINI_API_KEY, apiKey.trim())
            .apply()
    }

    fun deleteApiKey() {
        sharedPrefs.edit()
            .remove(KEY_GEMINI_API_KEY)
            .putBoolean(KEY_LAST_CONNECTED, false)
            .apply()
    }

    fun saveLastConnectionStatus(connected: Boolean, message: String) {
        sharedPrefs.edit()
            .putBoolean(KEY_LAST_CONNECTED, connected)
            .putString(KEY_LAST_STATUS_MSG, message)
            .apply()
    }

    fun getLastConnected(): Boolean = sharedPrefs.getBoolean(KEY_LAST_CONNECTED, false)

    fun getLastStatusMessage(): String =
        sharedPrefs.getString(KEY_LAST_STATUS_MSG, null).orEmpty()

    fun getAccentTheme(): AccentThemeOption {
        val id = sharedPrefs.getString(KEY_ACCENT_THEME, AccentThemeOption.CYBER_CYAN.id)
        return AccentThemeOption.fromId(id)
    }

    fun saveAccentTheme(option: AccentThemeOption) {
        sharedPrefs.edit()
            .putString(KEY_ACCENT_THEME, option.id)
            .apply()
    }

    fun saveProject(project: MotionProject) {
        try {
            val json = serializeProject(project)
            sharedPrefs.edit().putString(KEY_SAVED_PROJECT_JSON, json).apply()
        } catch (_: Exception) {
            // Ignore serialization issue
        }
    }

    fun loadProject(): MotionProject? {
        val raw = sharedPrefs.getString(KEY_SAVED_PROJECT_JSON, null) ?: return null
        return try {
            deserializeProject(raw)
        } catch (_: Exception) {
            null
        }
    }

    private fun serializeProject(project: MotionProject): String {
        val obj = JSONObject()
        obj.put("id", project.id)
        obj.put("title", project.title)
        obj.put("storyPrompt", project.storyPrompt)
        obj.put("theme", project.theme.id)
        obj.put("language", project.language.code)
        obj.put("targetDurationSeconds", project.targetDurationSeconds)
        obj.put("animationStyle", project.animationStyle.id)
        obj.put("bgmTrack", project.bgmTrack.id)
        obj.put("bgmVolume", project.bgmVolume.toDouble())
        obj.put("voiceEnabled", project.voiceEnabled)
        obj.put("voicePitch", project.voicePitch.toDouble())
        obj.put("voiceSpeed", project.voiceSpeed.toDouble())
        obj.put("autoSubtitles", project.autoSubtitles)

        val scenesArr = JSONArray()
        for (scene in project.scenes) {
            val sObj = JSONObject()
            sObj.put("id", scene.id)
            sObj.put("title", scene.title)
            sObj.put("headlineText", scene.headlineText)
            sObj.put("narrationText", scene.narrationText)
            sObj.put("visualDescription", scene.visualDescription)
            sObj.put("durationSeconds", scene.durationSeconds)
            sObj.put("backgroundPreset", scene.backgroundPreset.id)
            sObj.put("primaryColorHex", scene.primaryColorHex)
            sObj.put("secondaryColorHex", scene.secondaryColorHex)
            sObj.put("characterType", scene.characterType.id)
            sObj.put("characterMotion", scene.characterMotion.id)
            sObj.put("characterSpeed", scene.characterSpeed.toDouble())
            sObj.put("objectType", scene.objectType.id)
            sObj.put("objectMotion", scene.objectMotion.id)
            sObj.put("textAnimIn", scene.textAnimIn.id)
            sObj.put("textAnimOut", scene.textAnimOut.id)
            sObj.put("transitionToNext", scene.transitionToNext.id)
            sObj.put("sfxOnEnter", scene.sfxOnEnter.id)
            scenesArr.put(sObj)
        }
        obj.put("scenes", scenesArr)
        return obj.toString()
    }

    private fun deserializeProject(raw: String): MotionProject {
        val obj = JSONObject(raw)
        val scenesArr = obj.optJSONArray("scenes")
        val scenes = mutableListOf<MotionScene>()
        if (scenesArr != null) {
            for (i in 0 until scenesArr.length()) {
                val sObj = scenesArr.getJSONObject(i)
                scenes.add(
                    MotionScene(
                        id = sObj.optString("id", "scene_${i + 1}"),
                        title = sObj.optString("title", "Scene ${i + 1}"),
                        headlineText = sObj.optString("headlineText", "SCENE ${i + 1}"),
                        narrationText = sObj.optString("narrationText", ""),
                        visualDescription = sObj.optString("visualDescription", ""),
                        durationSeconds = sObj.optInt("durationSeconds", 6).coerceIn(3, 20),
                        backgroundPreset = BackgroundPreset.fromId(sObj.optString("backgroundPreset")),
                        primaryColorHex = sObj.optString("primaryColorHex", "#00E5FF"),
                        secondaryColorHex = sObj.optString("secondaryColorHex", "#8B5CF6"),
                        characterType = CharacterType.fromId(sObj.optString("characterType")),
                        characterMotion = CharacterMotion.fromId(sObj.optString("characterMotion")),
                        characterSpeed = sObj.optDouble("characterSpeed", 1.0).toFloat(),
                        objectType = MovingObjectType.fromId(sObj.optString("objectType")),
                        objectMotion = ObjectMotion.fromId(sObj.optString("objectMotion")),
                        textAnimIn = TextAnimIn.fromId(sObj.optString("textAnimIn")),
                        textAnimOut = TextAnimOut.fromId(sObj.optString("textAnimOut")),
                        transitionToNext = SceneTransition.fromId(sObj.optString("transitionToNext")),
                        sfxOnEnter = SfxType.fromId(sObj.optString("sfxOnEnter"))
                    )
                )
            }
        }
        return MotionProject(
            id = obj.optString("id", "proj_saved"),
            title = obj.optString("title", "Motion Lab Project"),
            storyPrompt = obj.optString("storyPrompt", ""),
            theme = StoryTheme.fromId(obj.optString("theme")),
            language = StoryLanguage.fromCode(obj.optString("language")),
            targetDurationSeconds = obj.optInt("targetDurationSeconds", 30),
            animationStyle = AnimationStyle.fromId(obj.optString("animationStyle")),
            bgmTrack = BgmTrack.fromId(obj.optString("bgmTrack")),
            bgmVolume = obj.optDouble("bgmVolume", 0.45).toFloat(),
            voiceEnabled = obj.optBoolean("voiceEnabled", true),
            voicePitch = obj.optDouble("voicePitch", 1.0).toFloat(),
            voiceSpeed = obj.optDouble("voiceSpeed", 1.05).toFloat(),
            autoSubtitles = obj.optBoolean("autoSubtitles", true),
            scenes = if (scenes.isNotEmpty()) scenes else defaultSampleScenes()
        )
    }

    companion object {
        private const val PREFS_FILE_NAME = "motion_lab_encrypted_prefs"
        private const val FALLBACK_PREFS_FILE_NAME = "motion_lab_fallback_prefs"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key_secret"
        private const val KEY_LAST_CONNECTED = "gemini_last_connected"
        private const val KEY_LAST_STATUS_MSG = "gemini_last_status_msg"
        private const val KEY_ACCENT_THEME = "studio_accent_theme"
        private const val KEY_SAVED_PROJECT_JSON = "studio_saved_project_json"
    }
}
