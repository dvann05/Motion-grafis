package com.example.model

enum class WorkspaceTab(val title: String) {
    STUDIO("Studio"),
    STORY_AI("Story AI"),
    SCENE_EDITOR("Scene Editor"),
    AUDIO("Audio & Sub"),
    EXPORT("Export MP4"),
    API_SETTINGS("Settings & API")
}

enum class AnimationStyle(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val badgeTag: String
) {
    CARTOON_2D(
        id = "cartoon_2d",
        displayName = "2D Cartoon",
        subtitle = "Expressive rigged characters, bold outlines & cel-shaded colors",
        badgeTag = "2D RIG"
    ),
    PIXEL_ART(
        id = "pixel_art",
        displayName = "Pixel Art",
        subtitle = "Retro 16-bit grid sprites, arcade palette & stepped motion",
        badgeTag = "16-BIT"
    ),
    FLAT_VECTOR(
        id = "flat_vector",
        displayName = "Flat Vector",
        subtitle = "Modern geometric shapes, smooth bezier waves & crisp gradients",
        badgeTag = "VECTOR"
    ),
    MOTION_INFOGRAPHIC(
        id = "motion_infographic",
        displayName = "Motion Infographic",
        subtitle = "Animated charts, live KPI counters, nodes & HUD telemetry",
        badgeTag = "DATA HUD"
    ),
    KINETIC_TYPOGRAPHY(
        id = "kinetic_typography",
        displayName = "Kinetic Typography",
        subtitle = "High-energy rhythmic text choreography, bold kinetic blocks",
        badgeTag = "KINETIC"
    ),
    MINIMALIST(
        id = "minimalist",
        displayName = "Minimalist Animation",
        subtitle = "Clean editorial balance, fine geometric orbits & subtle motion",
        badgeTag = "MINIMAL"
    );

    companion object {
        fun fromId(id: String?): AnimationStyle =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: FLAT_VECTOR
    }
}

enum class StoryTheme(val id: String, val labelId: String, val labelEn: String) {
    TECH_AI("tech_ai", "Sains & Teknologi AI", "AI & Future Tech"),
    EDUCATION("education", "Edukasi & Pengetahuan", "Education & Explainer"),
    BUSINESS("business", "Bisnis & Startup", "Business & Startup"),
    STORYTELLING("storytelling", "Cerita Fiksi & Petualangan", "Fiction & Adventure"),
    MOTIVATION("motivation", "Motivasi & Kreator", "Motivation & Mindset"),
    PRODUCT_PROMO("product_promo", "Promosi Produk Digital", "Product Launch Promo");

    companion object {
        fun fromId(id: String?): StoryTheme =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: TECH_AI
    }
}

enum class StoryLanguage(val code: String, val displayName: String, val localeTag: String) {
    INDONESIAN("id", "Bahasa Indonesia", "id-ID"),
    ENGLISH("en", "English", "en-US");

    companion object {
        fun fromCode(code: String?): StoryLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: INDONESIAN
    }
}

enum class BackgroundPreset(
    val id: String,
    val label: String,
    val primaryHex: String,
    val secondaryHex: String
) {
    CYBER_CITY("cyber_city", "Cyber Neon Skyline", "#0B1026", "#00E5FF"),
    NEON_GRID("neon_grid", "Synth Horizon Grid", "#140A2E", "#EC4899"),
    SUNSET_HORIZON("sunset_horizon", "Warm Sunset Studio", "#2B102F", "#F59E0B"),
    DEEP_SPACE("deep_space", "Deep Space Nebula", "#060B19", "#6366F1"),
    STUDIO_MINIMAL("studio_minimal", "Dark Editorial Slate", "#0F172A", "#38BDF8"),
    DATA_MATRIX("data_matrix", "Quantum Data Matrix", "#061E1E", "#10B981");

    companion object {
        fun fromId(id: String?): BackgroundPreset =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: CYBER_CITY
    }
}

enum class CharacterType(val id: String, val label: String) {
    ROBOT_AI("robot_ai", "Nova AI Bot"),
    ASTRONAUT("astronaut", "Astro Explorer"),
    CREATOR_HERO("creator_hero", "Motion Creator"),
    BUSINESS_PRO("business_pro", "Startup Founder"),
    PIXEL_CAT("pixel_cat", "Cyber Neko"),
    NONE("none", "Tanpa Karakter");

    companion object {
        fun fromId(id: String?): CharacterType =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: ROBOT_AI
    }
}

enum class CharacterMotion(val id: String, val label: String) {
    WALK_IN("walk_in", "Walk & Wave"),
    BOUNCE("bounce", "Dynamic Bounce"),
    FLOAT_WAVE("float_wave", "Zero-G Float"),
    DANCE_PULSE("dance_pulse", "Rhythm Groove"),
    ZOOM_HERO("zoom_hero", "Hero Zoom In"),
    STATIC_IDLE("static_idle", "Subtle Breathing");

    companion object {
        fun fromId(id: String?): CharacterMotion =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: FLOAT_WAVE
    }
}

enum class MovingObjectType(val id: String, val label: String) {
    ROCKET_SHIP("rocket_ship", "Boost Rocket"),
    GROWTH_CHART("growth_chart", "Live Hologram Chart"),
    SPARK_STARS("spark_stars", "Energy Particles"),
    IDEA_BULBS("idea_bulbs", "Glowing Idea Nodes"),
    DATA_ORBS("data_orbs", "Quantum Orbs"),
    FLOATING_COINS("floating_coins", "Crypto / Gold Badges"),
    NONE("none", "Tanpa Objek");

    companion object {
        fun fromId(id: String?): MovingObjectType =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: SPARK_STARS
    }
}

enum class ObjectMotion(val id: String, val label: String) {
    ORBIT("orbit", "360° Orbit"),
    SLIDE_ACROSS("slide_across", "Fly Across"),
    FLOAT_UP("float_up", "Rising Stream"),
    SPIN_PULSE("spin_pulse", "Spin & Pulse"),
    PARTICLE_RAIN("particle_rain", "Matrix Cascade");

    companion object {
        fun fromId(id: String?): ObjectMotion =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: ORBIT
    }
}

enum class TextAnimIn(val id: String, val label: String) {
    TYPEWRITER("typewriter", "Typewriter Reveal"),
    SLIDE_UP("slide_up", "Slide Up Smooth"),
    POP_ELASTIC("pop_elastic", "Elastic Pop"),
    GLITCH_IN("glitch_in", "Cyber Glitch In"),
    FADE_SCALE("fade_scale", "Cinematic Scale");

    companion object {
        fun fromId(id: String?): TextAnimIn =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: POP_ELASTIC
    }
}

enum class TextAnimOut(val id: String, val label: String) {
    FADE_OUT("fade_out", "Smooth Fade Out"),
    SLIDE_DOWN("slide_down", "Drop Down"),
    ZOOM_OUT("zoom_out", "Hyper Zoom Out");

    companion object {
        fun fromId(id: String?): TextAnimOut =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: FADE_OUT
    }
}

enum class SceneTransition(val id: String, val label: String) {
    CROSSFADE("crossfade", "Crossfade Dissolve"),
    WIPE_LEFT("wipe_left", "Motion Wipe"),
    CIRCLE_IRIS("circle_iris", "Circle Iris Reveal"),
    GLITCH_FLASH("glitch_flash", "Neon Glitch Cut"),
    ZOOM_THROUGH("zoom_through", "Camera Zoom Through"),
    CUT("cut", "Direct Cut");

    companion object {
        fun fromId(id: String?): SceneTransition =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: CROSSFADE
    }
}

enum class BgmTrack(val id: String, val label: String, val bpm: Int) {
    CYBER_SYNTHWAVE("cyber_synthwave", "Cyber Synthwave Pulse", 118),
    UPBEAT_MOTION("upbeat_motion", "Upbeat Studio Pop", 125),
    LOFI_CHILL("lofi_chill", "Lo-Fi Story Groove", 88),
    CINEMATIC_AMBIENT("cinematic_ambient", "Cinematic Horizon Pad", 72),
    RETRO_8BIT("retro_8bit", "8-Bit Chiptune Arcade", 136),
    NONE("none", "Mute Background Music", 0);

    companion object {
        fun fromId(id: String?): BgmTrack =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: CYBER_SYNTHWAVE
    }
}

enum class SfxType(val id: String, val label: String) {
    WHOOSH("whoosh", "Motion Whoosh"),
    POP_BUBBLE("pop_bubble", "Kinetic Pop"),
    DIGITAL_CHIME("digital_chime", "Future Chime"),
    GLITCH_ZAP("glitch_zap", "Cyber Zap"),
    LEVEL_UP("level_up", "Success Arpeggio"),
    NONE("none", "Tanpa SFX");

    companion object {
        fun fromId(id: String?): SfxType =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: WHOOSH
    }
}

data class MotionScene(
    val id: String,
    val title: String,
    val headlineText: String,
    val narrationText: String,
    val visualDescription: String,
    val durationSeconds: Int = 6,
    val backgroundPreset: BackgroundPreset = BackgroundPreset.CYBER_CITY,
    val primaryColorHex: String = "#00E5FF",
    val secondaryColorHex: String = "#8B5CF6",
    val characterType: CharacterType = CharacterType.ROBOT_AI,
    val characterMotion: CharacterMotion = CharacterMotion.FLOAT_WAVE,
    val characterSpeed: Float = 1.0f,
    val objectType: MovingObjectType = MovingObjectType.SPARK_STARS,
    val objectMotion: ObjectMotion = ObjectMotion.ORBIT,
    val textAnimIn: TextAnimIn = TextAnimIn.POP_ELASTIC,
    val textAnimOut: TextAnimOut = TextAnimOut.FADE_OUT,
    val transitionToNext: SceneTransition = SceneTransition.CROSSFADE,
    val sfxOnEnter: SfxType = SfxType.WHOOSH
)

data class MotionProject(
    val id: String = "proj_default",
    val title: String = "Masa Depan Kreator AI",
    val storyPrompt: String = "Bagaimana teknologi AI membantu kreator membuat animasi 2D dan motion graphics dalam hitungan detik.",
    val theme: StoryTheme = StoryTheme.TECH_AI,
    val language: StoryLanguage = StoryLanguage.INDONESIAN,
    val targetDurationSeconds: Int = 30,
    val animationStyle: AnimationStyle = AnimationStyle.FLAT_VECTOR,
    val bgmTrack: BgmTrack = BgmTrack.CYBER_SYNTHWAVE,
    val bgmVolume: Float = 0.45f,
    val voiceEnabled: Boolean = true,
    val voicePitch: Float = 1.0f,
    val voiceSpeed: Float = 1.05f,
    val autoSubtitles: Boolean = true,
    val scenes: List<MotionScene> = defaultSampleScenes()
) {
    val totalDurationSeconds: Int
        get() = scenes.sumOf { it.durationSeconds.coerceAtLeast(2) }.coerceAtLeast(1)
}

enum class ApiConnectionState {
    DISCONNECTED,
    TESTING,
    CONNECTED,
    INVALID_KEY,
    QUOTA_EXCEEDED,
    NETWORK_ERROR
}

data class ExportProgressState(
    val isExporting: Boolean = false,
    val currentFrame: Int = 0,
    val totalFrames: Int = 0,
    val statusText: String = "",
    val exportedFilePath: String? = null,
    val exportedMediaStoreUri: String? = null,
    val fileSizeKb: Long = 0L,
    val errorMessage: String? = null
) {
    val progressFraction: Float
        get() = if (totalFrames > 0) (currentFrame.toFloat() / totalFrames.toFloat()).coerceIn(0f, 1f) else 0f
}

fun defaultSampleScenes(): List<MotionScene> = listOf(
    MotionScene(
        id = "scene_1",
        title = "Scene 1 • Hook & Intro",
        headlineText = "IDE JADI ANIMASI DALAM DETIK",
        narrationText = "Pernah punya ide cerita brilian tapi butuh waktu berhari-hari membuat video animasi?",
        visualDescription = "Robot AI futuristik muncul di tengah kota siber dengan partikel cahaya berputar.",
        durationSeconds = 7,
        backgroundPreset = BackgroundPreset.CYBER_CITY,
        primaryColorHex = "#00E5FF",
        secondaryColorHex = "#8B5CF6",
        characterType = CharacterType.ROBOT_AI,
        characterMotion = CharacterMotion.ZOOM_HERO,
        characterSpeed = 1.1f,
        objectType = MovingObjectType.IDEA_BULBS,
        objectMotion = ObjectMotion.ORBIT,
        textAnimIn = TextAnimIn.POP_ELASTIC,
        textAnimOut = TextAnimOut.FADE_OUT,
        transitionToNext = SceneTransition.CIRCLE_IRIS,
        sfxOnEnter = SfxType.WHOOSH
    ),
    MotionScene(
        id = "scene_2",
        title = "Scene 2 • AI Story Engine",
        headlineText = "OTOMATISASI NASKAH & SCENE",
        narrationText = "Motion Lab AI mengubah satu kalimat ide menjadi naskah, karakter bergerak, dan grafik visual.",
        visualDescription = "Kreator digital mengarahkan hologram grafik dan partikel data yang meluncur ke atas.",
        durationSeconds = 8,
        backgroundPreset = BackgroundPreset.NEON_GRID,
        primaryColorHex = "#EC4899",
        secondaryColorHex = "#00E5FF",
        characterType = CharacterType.CREATOR_HERO,
        characterMotion = CharacterMotion.WALK_IN,
        characterSpeed = 1.0f,
        objectType = MovingObjectType.GROWTH_CHART,
        objectMotion = ObjectMotion.FLOAT_UP,
        textAnimIn = TextAnimIn.SLIDE_UP,
        textAnimOut = TextAnimOut.SLIDE_DOWN,
        transitionToNext = SceneTransition.WIPE_LEFT,
        sfxOnEnter = SfxType.POP_BUBBLE
    ),
    MotionScene(
        id = "scene_3",
        title = "Scene 3 • Multi-Style Motion",
        headlineText = "6 GAYA VISUAL PROFESIONAL",
        narrationText = "Pilih gaya 2D Cartoon, Pixel Art, Flat Vector, hingga Kinetic Typography sesuai identitasmu.",
        visualDescription = "Astronot melayang menembus galaksi sambil dikelilingi orb energi berputar cepat.",
        durationSeconds = 8,
        backgroundPreset = BackgroundPreset.DEEP_SPACE,
        primaryColorHex = "#38BDF8",
        secondaryColorHex = "#A855F7",
        characterType = CharacterType.ASTRONAUT,
        characterMotion = CharacterMotion.FLOAT_WAVE,
        characterSpeed = 1.2f,
        objectType = MovingObjectType.DATA_ORBS,
        objectMotion = ObjectMotion.SPIN_PULSE,
        textAnimIn = TextAnimIn.GLITCH_IN,
        textAnimOut = TextAnimOut.FADE_OUT,
        transitionToNext = SceneTransition.ZOOM_THROUGH,
        sfxOnEnter = SfxType.DIGITAL_CHIME
    ),
    MotionScene(
        id = "scene_4",
        title = "Scene 4 • Export 1080p MP4",
        headlineText = "SIAP UNDUH FULL HD 9:16",
        narrationText = "Render video vertikal Full HD seribu delapan puluh piksel tiga puluh FPS dan bagikan ke dunia!",
        visualDescription = "Roket meluncur kencang diiringi bintang keemasan dan indikator pencapaian puncak.",
        durationSeconds = 7,
        backgroundPreset = BackgroundPreset.SUNSET_HORIZON,
        primaryColorHex = "#F59E0B",
        secondaryColorHex = "#F43F5E",
        characterType = CharacterType.ROBOT_AI,
        characterMotion = CharacterMotion.DANCE_PULSE,
        characterSpeed = 1.3f,
        objectType = MovingObjectType.ROCKET_SHIP,
        objectMotion = ObjectMotion.SLIDE_ACROSS,
        textAnimIn = TextAnimIn.FADE_SCALE,
        textAnimOut = TextAnimOut.ZOOM_OUT,
        transitionToNext = SceneTransition.CROSSFADE,
        sfxOnEnter = SfxType.LEVEL_UP
    )
)
