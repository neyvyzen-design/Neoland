package com.neoland.app.data

import android.content.Context
import android.content.SharedPreferences
import com.neoland.app.core.tr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
enum class FxRate(val fps: Int, private val ru: String, private val en: String, private val id: String) {
    ECO(30, "30 кадров — экономнее", "30 fps — saves power", "30 fps — hemat daya"),
    SMOOTH(60, "60 кадров — плавно", "60 fps — smooth", "60 fps — mulus"),
    MAX(0, "Как у экрана — до 120", "Screen rate — up to 120", "Ikuti layar — hingga 120 fps"),
    ;

    val title: String get() = tr(ru, en, id)
}

@Serializable
enum class ExpandStyle(private val ru: String, private val en: String, private val id: String) {
    FRUIT("Как Фрукт", "Like a Fruit", "Dinamis"),
    SPRING("Пружина", "Spring", "Pegas"),
    SMOOTH("Плавно", "Smooth", "Halus"),
    IOS_SMOOTH("iOS · Плавно", "iOS · Smooth", "iOS · Halus"),
    IOS_SNAPPY("iOS · Быстро", "iOS · Snappy", "iOS · Cepat"),
    IOS_BOUNCY("iOS · Упруго", "iOS · Bouncy", "iOS · Membal"),
    IOS_LIQUID("iOS 26 · Liquid", "iOS 26 · Liquid", "iOS 26 · Liquid"),
    INSTANT("Без анимации", "No animation", "Tanpa animasi"),
    ;

    val title: String get() = tr(ru, en, id)
}

@Serializable
enum class UiTheme(
    private val ru: String, private val en: String, private val id: String,
    private val hintRu: String, private val hintEn: String, private val hintId: String,
) {
    CUSTOM("Свой", "Custom", "Kustom", "Ваши настройки без пресета", "Your own settings, no preset", "Pengaturan sendiri tanpa preset"),
    NAYLAND("Nayland · Signature", "Nayland · Signature", "Nayland · Signature",
        "Глубокое обсидиановое стекло с мягким бликом и плавной пружиной",
        "Deep obsidian glass with subtle highlights and liquid motion",
        "Kaca obsidian pekat dengan kilau halus dan animasi liquid"),
    IOS_GLASS("iOS · Liquid Glass", "iOS · Liquid Glass", "iOS · Liquid Glass",
        "Чёрная капсула со стеклом и бликами", "Black capsule with glass and highlights", "Kapsul hitam dengan kaca dan kilau"),
    IOS_CLASSIC("iOS · Классика", "iOS · Classic", "iOS · Klasik",
        "Чистый чёрный, без эффектов", "Pure black, no effects", "Hitam polos tanpa efek"),
    HYPEROS("HyperOS", "HyperOS", "HyperOS",
        "Мягкий тёмно-серый с тонким контуром", "Soft dark gray with a thin outline", "Abu gelap lembut dengan garis tipis"),
    MATERIAL_YOU("Material You", "Material You", "Material You",
        "Акцент из обоев Android", "Accent from the Android wallpaper", "Aksen dari wallpaper Android"),
    AMOLED("AMOLED · Экономия", "AMOLED · Saver", "AMOLED · Hemat",
        "Чёрный, минимум эффектов, 30 кадров", "Black, minimal effects, 30 fps", "Hitam, efek minimal, 30 fps"),
    ;

    val title: String get() = tr(ru, en, id)
    val hint: String get() = tr(hintRu, hintEn, hintId)
}

fun IslandSettings.withTheme(t: UiTheme): IslandSettings = when (t) {
    UiTheme.CUSTOM -> copy(uiTheme = t)
    UiTheme.NAYLAND -> copy(
        uiTheme = t, color = 0xFF050508, outline = true, outlineColor = 0x26FFFFFF, outlineWidth = 0.75f,
        outlineFromArt = true, shadow = false, liquidGlass = true, glassOpacity = 74, liquidExpand = true,
        expandStyle = ExpandStyle.IOS_LIQUID, elasticity = Elasticity.LOW,
        reduceEffects = false, fxBlur = true, fxGlow = true, fxRate = FxRate.SMOOTH,
        accentMode = AccentMode.ADAPTIVE,
    )
    UiTheme.IOS_GLASS -> copy(
        uiTheme = t, color = 0xFF000000, outline = false, shadow = false,
        liquidGlass = true, glassOpacity = 68, liquidExpand = true,
        expandStyle = ExpandStyle.IOS_LIQUID, elasticity = Elasticity.LOW,
        reduceEffects = false, fxBlur = true, fxGlow = true, accentMode = AccentMode.ADAPTIVE,
    )
    UiTheme.IOS_CLASSIC -> copy(
        uiTheme = t, color = 0xFF000000, outline = false, shadow = false,
        liquidGlass = false, liquidExpand = false,
        expandStyle = ExpandStyle.IOS_SMOOTH, elasticity = Elasticity.NONE,
        reduceEffects = false, fxGlow = false, accentMode = AccentMode.DEFAULT,
    )
    UiTheme.HYPEROS -> copy(
        uiTheme = t, color = 0xFF17171B, outline = true, outlineColor = 0x33FFFFFF, outlineWidth = 0.8f,
        outlineFromArt = false, shadow = true, liquidGlass = false, liquidExpand = false,
        expandStyle = ExpandStyle.IOS_SNAPPY, elasticity = Elasticity.LOW,
        reduceEffects = false, accentMode = AccentMode.CUSTOM, accentColor = 0xFF3482FF,
    )
    UiTheme.MATERIAL_YOU -> copy(
        uiTheme = t, color = 0xFF000000, outline = false, shadow = false,
        liquidGlass = false, liquidExpand = false,
        expandStyle = ExpandStyle.IOS_SMOOTH, elasticity = Elasticity.LOW,
        reduceEffects = false, accentMode = AccentMode.SYSTEM,
    )
    UiTheme.AMOLED -> copy(
        uiTheme = t, color = 0xFF000000, outline = false, shadow = false,
        liquidGlass = false, liquidExpand = false, expandStyle = ExpandStyle.SMOOTH,
        elasticity = Elasticity.NONE, reduceEffects = true, fxBlur = false, fxGlow = false,
        fxRate = FxRate.ECO, accentMode = AccentMode.DEFAULT,
    )
}

@Serializable
enum class Elasticity(private val ru: String, private val en: String, private val id: String, val damping: Float) {
    NONE("Без упругости", "No bounce", "Tanpa pantulan", 1f),
    LOW("Слабая упругость", "Light bounce", "Pantulan ringan", 0.8f),
    MEDIUM("Средняя упругость", "Medium bounce", "Pantulan sedang", 0.62f),
    HIGH("Сильная упругость", "Strong bounce", "Pantulan kuat", 0.48f),
    ;

    val title: String get() = tr(ru, en, id)
}

@Serializable
enum class TapAction(private val ru: String, private val en: String, private val id: String) {
    EXPAND("Раскрыть капсулу", "Expand the capsule", "Perluas kapsul"),
    OPEN_APP("Открыть приложение", "Open the app", "Buka aplikasi"),
    PLAY_PAUSE("Пауза / воспроизведение", "Play / pause", "Putar / jeda"),
    NEXT_TRACK("Следующий трек", "Next track", "Lagu berikutnya"),
    SCREENSHOT("Скриншот без острова", "Screenshot without the island", "Tangkapan layar tanpa kapsul"),
    FLASHLIGHT("Фонарик", "Flashlight", "Senter"),
    CAMERA("Камера", "Camera", "Kamera"),
    LOCK_SCREEN("Заблокировать экран", "Lock the screen", "Kunci layar"),
    NOTHING("Ничего", "Nothing", "Tidak ada"),
    ;

    val title: String get() = tr(ru, en, id)
}

@Serializable
enum class QuickAction(
    private val ru: String, private val en: String, private val id: String,
    private val shortRu: String, private val shortEn: String, private val shortId: String,
) {
    TIMER("Таймер", "Timer", "Timer", "Таймер", "Timer", "Timer"),
    STOPWATCH("Секундомер", "Stopwatch", "Stopwatch", "Секундомер", "Stopwatch", "Stopwatch"),
    FLASHLIGHT("Фонарик", "Flashlight", "Senter", "Фонарик", "Flashlight", "Senter"),
    SCREENSHOT("Скриншот без острова", "Screenshot without the island", "Tangkapan layar", "Скриншот", "Screenshot", "Screenshot"),
    CAMERA("Камера", "Camera", "Kamera", "Камера", "Camera", "Kamera"),
    LOCK("Заблокировать экран", "Lock the screen", "Kunci layar", "Блокировка", "Lock", "Kunci"),
    CALCULATOR("Калькулятор", "Calculator", "Kalkulator", "Калькулятор", "Calculator", "Kalkulator"),
    NETWORK("Скорость сети", "Network speed", "Kecepatan jaringan", "Скорость", "Speed", "Kecepatan"),
    DEVICE("Устройство", "Device", "Info perangkat", "Устройство", "Device", "Perangkat"),
    FOCUS("Фокус 25 минут", "Focus 25 min", "Fokus 25 menit", "Фокус 25", "Focus 25", "Fokus 25"),
    BREAK("Перерыв 5 минут", "Break 5 min", "Istirahat 5 menit", "Перерыв 5", "Break 5", "Istirahat 5"),
    ;

    val title: String get() = tr(ru, en, id)
    val short: String get() = tr(shortRu, shortEn, shortId)
}

@Serializable
enum class AccentMode(
    private val ru: String, private val en: String, private val id: String,
    private val hintRu: String, private val hintEn: String, private val hintId: String,
) {
    DEFAULT("Сиреневый", "Lavender", "Lavender", "Цвет приложения", "The app's color", "Warna bawaan Nayland"),
    SYSTEM("Системный", "System", "Sistem", "Цвет из обоев Android 12+ (Material You)", "From your Android 12+ wallpaper (Material You)", "Mengikuti warna wallpaper Android 12+ (Material You)"),
    ADAPTIVE("Из аватара", "From the avatar", "Dari ikon/avatar", "Подбирается по аватару или значку приложения", "Matched to the avatar or app icon", "Menyesuaikan warna avatar atau ikon aplikasi"),
    CUSTOM("Свой", "Custom", "Kustom", "Любой цвет из набора", "Any color from the palette", "Pilih warna bebas dari palet"),
    ;

    val title: String get() = tr(ru, en, id)
    val hint: String get() = tr(hintRu, hintEn, hintId)
}

@Serializable
enum class AppLanguage {
    SYSTEM, RU, EN, ID;

    val title: String
        get() = when (this) {
            SYSTEM -> tr("Как в системе", "System default", "Ikuti sistem")
            RU -> "Русский"
            EN -> "English"
            ID -> "Bahasa Indonesia"
        }
}

@Serializable
data class IslandPlacement(
    val offsetX: Int = 0,
    val offsetY: Int = 11,
    val width: Int = 104,
    val height: Int = 31,
)

@Serializable
data class IslandSettings(
    val enabled: Boolean = true,
    val language: AppLanguage = AppLanguage.SYSTEM,

    // Kartu dinamis
    val cardMedia: Boolean = true,
    val cardTorch: Boolean = true,
    val cardProgress: Boolean = true,
    val cardTimer: Boolean = true,
    val cardCharging: Boolean = true,
    val cardCall: Boolean = true,
    val cardMessages: Boolean = true,
    val msgActions: Boolean = true,
    val cardLive: Boolean = true,
    val cardAlarm: Boolean = true,
    val clockAutoTap: Boolean = true,
    val timerFromAssistant: Boolean = true,
    val wakeForAlerts: Boolean = true,
    val easterEggs: Boolean = true,

    // Event & indikator
    val evChargeStart: Boolean = true,
    val evChargeStop: Boolean = true,
    val evLowBattery: Boolean = true,
    val evWifiOn: Boolean = true,
    val evWifiOff: Boolean = true,
    val evHeadsetOn: Boolean = true,
    val evHeadsetOff: Boolean = true,
    val evAirplaneOn: Boolean = true,
    val evAirplaneOff: Boolean = true,
    val evRinger: Boolean = true,
    val evBrightness: Boolean = true,
    val evUsbOn: Boolean = true,
    val evUsbOff: Boolean = true,
    val evUnlock: Boolean = false,
    val evLock: Boolean = true,
    val evDnd: Boolean = true,
    val evPayReady: Boolean = true,
    val payHintOnOpen: Boolean = false,
    val evPaySuccess: Boolean = true,
    val evAlarmSet: Boolean = true,

    // Ukuran & posisi
    val width: Int = 104,
    val height: Int = 31,
    val offsetX: Int = 0,
    val offsetY: Int = 11,
    val lockPlacement: IslandPlacement? = null,
    val cameraGap: Int = 34,
    val activeExtra: Int = 6,
    val activeWidth: Int = 40,
    val layoutVersion: Int = 1,

    // Warna & tampilan
    val color: Long = 0xFF000000,
    val outline: Boolean = false,
    val outlineColor: Long = 0xFF3A3A40,
    val outlineWidth: Float = 1f,
    val outlineFromArt: Boolean = true,
    val shadow: Boolean = false,

    val accentMode: AccentMode = AccentMode.DEFAULT,
    val accentColor: Long = 0xFF0A84FF,

    // Pembaruan
    val autoUpdateCheck: Boolean = true,
    val skippedVersion: String = "",
    val lastSeenVersion: String = "",
    val lastUpdateCheck: Long = 0,
    val islandVersion: String = "",

    // Liquid Glass
    val liquidGlass: Boolean = false,
    val glassOpacity: Int = 68,

    // Animasi & tema
    val expandStyle: ExpandStyle = ExpandStyle.FRUIT,
    val uiTheme: UiTheme = UiTheme.CUSTOM,
    val evHeat: Boolean = true,
    val heatLimit: Int = 42,
    val chargeLimit: Int = 0,
    val evVolume: Boolean = false,
    val evPrivacy: Boolean = false,
    val evBluetooth: Boolean = true,
    val evHotspot: Boolean = true,
    val evLocation: Boolean = true,
    val animInPercent: Int = 100,
    val animOutPercent: Int = 100,
    val volumeHoldMs: Int = 1400,
    val durationPercent: Int = 35,
    val liquidExpand: Boolean = false,
    val elasticity: Elasticity = Elasticity.LOW,
    val reduceEffects: Boolean = false,
    val haptics: Boolean = true,

    // Efek
    val fxBlur: Boolean = true,
    val fxEqualizer: Boolean = true,
    val fxRate: FxRate = FxRate.SMOOTH,
    val fxGlow: Boolean = false,
    val fxPay: Boolean = true,
    val fxShake: Boolean = true,
    val batterySaver: Boolean = false,

    // Gestur
    val tapAction: TapAction = TapAction.EXPAND,
    val doubleTapAction: TapAction = TapAction.NOTHING,
    val longPressAction: TapAction = TapAction.OPEN_APP,
    val swipeTracks: Boolean = true,
    val swipeExpand: Boolean = true,

    // Lirik
    val lyricsEnabled: Boolean = true,

    // Perilaku
    val hideIdle: Boolean = false,
    val hideInLandscape: Boolean = true,
    val landscapeFollow: Boolean = true,
    val landscapeShift: Int = 0,
    val hideWhenPlayerOpen: Boolean = true,
    val scheduleOn: Boolean = false,
    val scheduleFrom: Int = 10,
    val scheduleTo: Int = 23,
    val noticeDurationMs: Int = 2500,
    val timerMinutes: Int = 5,
    val autoCollapseSec: Int = 0,
    val bubble: Boolean = true,
    val idleExpand: Boolean = true,
    val quickActions: List<QuickAction> = listOf(
        QuickAction.TIMER,
        QuickAction.STOPWATCH,
        QuickAction.FLASHLIGHT,
        QuickAction.CALCULATOR,
        QuickAction.NETWORK,
        QuickAction.DEVICE,
    ),
    val labCalc: Boolean = false,
    val labBrandColors: Boolean = false,
    val evFull: Boolean = true,
    val pagerNextOnLeft: Boolean = false,
    val lockMinimal: Boolean = true,
    val faceIdAnim: Boolean = true,
    val keepAlive: Boolean = false,
    val msgLines: Int = 3,
    val msgInApp: Boolean = false,
    val msgChannels: Map<String, String> = emptyMap(),
    val bannerHintHidden: Boolean = false,
) {
    fun forPowerSave(on: Boolean): IslandSettings = if (on && batterySaver && !reduceEffects) copy(reduceEffects = true) else this

    fun inSchedule(hour: Int): Boolean = when {
        !scheduleOn || scheduleFrom == scheduleTo -> true
        scheduleFrom < scheduleTo -> hour in scheduleFrom until scheduleTo
        else -> hour >= scheduleFrom || hour < scheduleTo
    }

    fun lockScreenSettings(): IslandSettings {
        val p = lockPlacement ?: IslandPlacement(offsetX, offsetY, width, height)
        return copy(offsetX = p.offsetX, offsetY = p.offsetY, width = p.width, height = p.height)
    }

    val liveEqualizer: Boolean get() = fxEqualizer && !reduceEffects
}

object SettingsRepo {
    private const val KEY = "settings_v1"
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    private var prefs: SharedPreferences? = null

    var hadSavedSettings = false
        private set
    private val _state = MutableStateFlow(IslandSettings())
    val state: StateFlow<IslandSettings> = _state.asStateFlow()
    val current: IslandSettings get() = _state.value

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences("island", Context.MODE_PRIVATE)
        prefs = p
        hadSavedSettings = p.contains(KEY)
        var hadNotch = false
        p.getString(KEY, null)?.let { raw ->
            hadNotch = Regex("\"notch\"\\s*:\\s*true").containsMatchIn(raw)
            runCatching { json.decodeFromString<IslandSettings>(raw) }.onSuccess { _state.value = it }
        }
        if (hadNotch) {
            val d = IslandSettings()
            update { copy(offsetY = d.offsetY, lockPlacement = lockPlacement?.copy(offsetY = d.offsetY)) }
        }
        val cur = _state.value
        if (cur.layoutVersion in 2..2) update { copy(cameraGap = IslandSettings().cameraGap, layoutVersion = 3) }
        if (cur.layoutVersion < 2) {
            update {
                copy(
                    width = if (width == 96) 126 else width,
                    height = if (height == 34) 37 else height,
                    offsetY = if (offsetY == 8) 11 else offsetY,
                    layoutVersion = 3,
                )
            }
        }
        if (current.layoutVersion < 4) {
            val d = IslandSettings()
            update {
                val old = width == 126 && height == 37
                copy(
                    width = if (old) d.width else width,
                    height = if (old) d.height else height,
                    activeWidth = if (old && activeWidth == 48) d.activeWidth else activeWidth,
                    elasticity = if (elasticity == Elasticity.HIGH) d.elasticity else elasticity,
                    layoutVersion = 4,
                )
            }
        }
        if (current.lockPlacement == null) {
            update {
                copy(lockPlacement = IslandPlacement(offsetX, offsetY, width, height))
            }
        }
    }

    fun exportJson(): String = runCatching { json.encodeToString(_state.value) }.getOrDefault("{}")

    fun importJson(raw: String): Boolean = runCatching {
        val v = json.decodeFromString<IslandSettings>(raw)
        update {
            v.copy(
                islandVersion = islandVersion, lastSeenVersion = lastSeenVersion,
                lastUpdateCheck = lastUpdateCheck, skippedVersion = skippedVersion,
            )
        }
        true
    }.getOrDefault(false)

    fun update(block: IslandSettings.() -> IslandSettings) {
        val next = _state.value.block()
        if (next == _state.value) return
        _state.value = next
        runCatching {
            prefs?.edit()?.putString(KEY, json.encodeToString(next))?.apply()
        }
    }

    fun reset() = update { IslandSettings() }

    fun resetAnimations() = update {
        val d = IslandSettings()
        copy(
            expandStyle = d.expandStyle,
            durationPercent = d.durationPercent,
            liquidExpand = d.liquidExpand,
            elasticity = d.elasticity,
            reduceEffects = d.reduceEffects,
            noticeDurationMs = d.noticeDurationMs,
        )
    }

    fun resetEffects() = update {
        val d = IslandSettings()
        copy(
            fxBlur = d.fxBlur, fxEqualizer = d.fxEqualizer, fxRate = d.fxRate, fxGlow = d.fxGlow, fxPay = d.fxPay,
            fxShake = d.fxShake, batterySaver = d.batterySaver,
        )
    }
}
