package com.neoland.app.ui.screens

import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material.icons.rounded.Bluetooth
import android.os.SystemClock
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.BlurCircular
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.BubbleChart
import androidx.compose.material.icons.rounded.Contactless
import androidx.compose.material.icons.rounded.Flare
import androidx.compose.material.icons.rounded.FormatColorFill
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LineWeight
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neoland.app.core.IslandBus
import com.neoland.app.core.Tints
import com.neoland.app.core.tr
import com.neoland.app.data.Elasticity
import com.neoland.app.data.ExpandStyle
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.ui.platform.LocalContext
import com.neoland.app.data.UiTheme
import com.neoland.app.data.withTheme
import com.neoland.app.data.FxRate
import com.neoland.app.data.IslandSettings
import com.neoland.app.data.SettingsRepo
import com.neoland.app.ui.ChoiceDialog
import com.neoland.app.ui.Group
import com.neoland.app.ui.GroupItem
import com.neoland.app.ui.GroupSlider
import com.neoland.app.ui.GroupToggle
import com.neoland.app.ui.Page
import com.neoland.app.ui.Pos
import com.neoland.app.ui.Screen
import com.neoland.app.ui.SectionHeader
import com.neoland.app.ui.Tile
import com.neoland.app.ui.Ui

/**
 * «Оформление»: где стоит остров и как он выглядит — положение и размер, цвет и стекло, анимации и эффекты.
 * Что остров показывает и как себя ведёт — во вкладке «Функции».
 */
@Composable
fun CapsuleScreen(open: (Screen) -> Unit) {
    val s by SettingsRepo.state.collectAsState()
    Page(tr("Оформление", "Appearance")) {
        IslandPreview()

        SectionHeader(tr("Стиль интерфейса", "Interface style", "Gaya tampilan"))
        Group {
            ThemeItem(s)
        }

        SectionHeader(tr("Положение и размер", "Position and size"))
        Group {
            GroupItem(Pos.First, Icons.Rounded.OpenWith, tr("Положение и размер", "Position and size"),
                tr("Капсула ${s.width}×${s.height} dp — подогнать под камеру", "Capsule ${s.width}×${s.height} dp — fit it to the camera"),
                color = Tile.Indigo, onClick = { open(Screen.SIZE) }) { Arrow() }
            GroupItem(Pos.Last, Icons.Rounded.SwapHoriz, tr("Размер с активностью", "Size with activity"),
                if (s.activeWidth == 0) tr("Толщина +${s.activeExtra} dp, ширина как без неё", "Thickness +${s.activeExtra} dp, width as without it")
                else tr("Толщина +${s.activeExtra}, ширина +${s.activeWidth} dp", "Thickness +${s.activeExtra}, width +${s.activeWidth} dp"),
                color = Tile.Purple, onClick = { open(Screen.LOOK) }) { Arrow() }
        }

        SectionHeader(tr("Цвет и стекло", "Color and glass"))
        Group {
            GroupItem(Pos.First, Icons.Rounded.FormatColorFill, tr("Цвет капсулы", "Capsule color"), tr("Цвет, контур и тень", "Color, outline and shadow"),
                color = Tile.Orange, onClick = { open(Screen.COLORS) }) { Arrow() }
            GroupToggle(
                if (s.liquidGlass) Pos.Middle else Pos.Last, Icons.Rounded.WaterDrop, "Liquid Glass",
                tr("Стекло с бликами и сияющей кромкой, как в iOS. Блики двигаются при наклоне",
                    "Glass with highlights and a glowing edge, like in iOS. Highlights move when you tilt the phone"),
                s.liquidGlass, color = Tile.Teal, badge = tr("Экспериментальный", "Experimental"),
            ) { v -> SettingsRepo.update { copy(liquidGlass = v) } }
            if (s.liquidGlass) {
                GroupSlider(
                    Pos.Last, Icons.Rounded.Opacity, tr("Плотность стекла", "Glass density"),
                    value = s.glassOpacity.toFloat(), range = 40f..95f, label = "${s.glassOpacity}%", color = Tile.Teal,
                ) { v -> SettingsRepo.update { copy(glassOpacity = v.toInt()) } }
            }
        }

        SectionHeader(tr("Анимации", "Animations"))
        Group {
            GroupItem(Pos.First, Icons.Rounded.Animation, tr("Раскрытие острова", "Island expansion"),
                "${s.expandStyle.title} · ${s.durationPercent}%",
                color = Tile.Blue, onClick = { open(Screen.ANIM) }) { Arrow() }
            // Новое раскрытие — отдельным переключателем: выключено — прежнее перетекание
            GroupToggle(Pos.Middle, Icons.Rounded.BubbleChart, tr("Жидкое раскрытие", "Liquid expansion"),
                tr("Содержимое растёт вместе с островом и проявляется из размытия",
                    "The content grows together with the island and emerges from a blur"),
                s.liquidExpand, color = Tile.Indigo, badge = tr("Новое", "New"),
            ) { v -> SettingsRepo.update { copy(liquidExpand = v) } }
            // Упругость — на виду: «слишком прыгает» правится одним нажатием
            ElasticityItem(Pos.Middle, s, Tile.Teal)
            GroupItem(Pos.Last, Icons.Rounded.AutoAwesome, tr("Эффекты", "Effects"), effectsSummary(s),
                color = Tile.Purple, onClick = { open(Screen.EFFECTS) }) { Arrow() }
        }

        SectionHeader(tr("Новое в 1.7.2", "New in 1.7.2", "Baru di 1.7.2"))
        NewFeatures(s)
    }
}

/** Готовые стили интерфейса (iOS, HyperOS и другие) — один выбор меняет цвет, стекло и анимацию. */
@Composable
private fun ThemeItem(s: IslandSettings) {
    var dialog by remember { mutableStateOf(false) }
    GroupItem(
        Pos.Single, Icons.Rounded.Palette, tr("Стиль интерфейса", "Interface style", "Gaya tampilan"),
        s.uiTheme.title + " · " + s.uiTheme.hint, color = Tile.Indigo, onClick = { dialog = true },
    )
    if (dialog) {
        ChoiceDialog(
            tr("Стиль интерфейса", "Interface style", "Gaya tampilan"), UiTheme.entries, s.uiTheme, { it.title },
            { v -> SettingsRepo.update { withTheme(v) } },
        ) { dialog = false }
    }
}

/** Новое в 1.7.2: предупреждения, пилюля громкости, индикаторы камеры/микрофона и резервная копия. */
@Composable
private fun NewFeatures(s: IslandSettings) {
    val ctx = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val raw = runCatching { ctx.contentResolver.openInputStream(uri)?.use { String(it.readBytes()) } }.getOrNull()
            val ok = raw != null && SettingsRepo.importJson(raw)
            Toast.makeText(
                ctx,
                if (ok) tr("Настройки восстановлены", "Settings restored", "Pengaturan dipulihkan")
                else tr("Не удалось прочитать файл", "Could not read the file", "File tidak bisa dibaca"),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }
    Group {
        GroupToggle(
            Pos.First, Icons.Rounded.Thermostat, tr("Горячая батарея", "Battery heat alert", "Peringatan baterai panas"),
            tr("Плашка, когда батарея перегрелась", "A pill when the battery gets too hot", "Pil saat baterai terlalu panas"),
            s.evHeat, color = Tile.Orange, badge = tr("Новое", "New", "Baru"),
        ) { v -> SettingsRepo.update { copy(evHeat = v) } }
        if (s.evHeat) {
            GroupSlider(
                Pos.Middle, Icons.Rounded.Thermostat, tr("Порог температуры", "Temperature limit", "Batas suhu"),
                value = s.heatLimit.toFloat(), range = 38f..50f, label = "${s.heatLimit}°C", color = Tile.Orange,
            ) { v -> SettingsRepo.update { copy(heatLimit = v.toInt()) } }
        }
        GroupSlider(
            Pos.Middle, Icons.Rounded.BatteryChargingFull, tr("Лимит зарядки", "Charge limit", "Batas isi baterai"),
            value = if (s.chargeLimit == 0) 40f else s.chargeLimit.toFloat(), range = 40f..100f,
            label = if (s.chargeLimit == 0) tr("Выкл", "Off", "Mati") else "${s.chargeLimit}%", color = Tile.Teal,
        ) { v -> SettingsRepo.update { copy(chargeLimit = if (v.toInt() <= 40) 0 else v.toInt()) } }
        GroupToggle(
            Pos.Middle, Icons.Rounded.VolumeUp, tr("Пилюля громкости", "Volume pill", "Pil volume"),
            tr("Показывает громкость после нажатия кнопок", "Shows the volume after pressing the keys", "Menampilkan volume setelah tombol ditekan"),
            s.evVolume, color = Tile.Blue, badge = tr("Новое", "New", "Baru"),
        ) { v -> SettingsRepo.update { copy(evVolume = v) } }
        GroupToggle(
            Pos.Middle, Icons.Rounded.PrivacyTip, tr("Камера и микрофон", "Camera and microphone", "Kamera dan mikrofon"),
            tr("Плашка, когда приложение включает камеру или запись звука", "A pill when an app turns on the camera or records audio", "Pil saat aplikasi memakai kamera atau merekam suara"),
            s.evPrivacy, color = Tile.Purple, badge = tr("Новое", "New", "Baru"),
        ) { v -> SettingsRepo.update { copy(evPrivacy = v) } }
        GroupToggle(
            Pos.Middle, Icons.Rounded.Bluetooth, tr("Bluetooth-устройства", "Bluetooth devices", "Perangkat Bluetooth"),
            tr("Часы, клавиатура и другое, кроме наушников", "Watches, keyboards and more, except headphones", "Jam, keyboard, dan lainnya selain headset"),
            s.evBluetooth, color = Tile.Blue, badge = tr("Новое", "New", "Baru"),
        ) { v -> SettingsRepo.update { copy(evBluetooth = v) } }
        GroupToggle(
            Pos.Middle, Icons.Rounded.WifiTethering, tr("Точка доступа", "Hotspot", "Hotspot"),
            tr("Плашка при включении и выключении", "A pill when it turns on or off", "Pil saat hotspot aktif atau mati"),
            s.evHotspot, color = Tile.Teal, badge = tr("Новое", "New", "Baru"),
        ) { v -> SettingsRepo.update { copy(evHotspot = v) } }
        GroupToggle(
            Pos.Middle, Icons.Rounded.LocationOn, tr("Геолокация", "Location", "Lokasi"),
            tr("Плашка при включении и выключении GPS", "A pill when GPS turns on or off", "Pil saat GPS aktif atau mati"),
            s.evLocation, color = Tile.Orange, badge = tr("Новое", "New", "Baru"),
        ) { v -> SettingsRepo.update { copy(evLocation = v) } }
        GroupSlider(
            Pos.Middle, Icons.Rounded.Speed, tr("Скорость появления", "Entry speed", "Kecepatan masuk"),
            value = s.animInPercent.toFloat(), range = 50f..250f, label = "${s.animInPercent}%", color = Tile.Indigo,
        ) { v -> SettingsRepo.update { copy(animInPercent = v.toInt()) } }
        GroupSlider(
            Pos.Middle, Icons.Rounded.Speed, tr("Скорость ухода", "Exit speed", "Kecepatan keluar"),
            value = s.animOutPercent.toFloat(), range = 50f..250f, label = "${s.animOutPercent}%", color = Tile.Indigo,
        ) { v -> SettingsRepo.update { copy(animOutPercent = v.toInt()) } }
        GroupSlider(
            Pos.Middle, Icons.Rounded.Timer, tr("Время пилюли громкости", "Volume pill time", "Lama pil volume"),
            value = s.volumeHoldMs.toFloat(), range = 600f..4000f, label = "${s.volumeHoldMs / 100 / 10.0} s", color = Tile.Blue,
        ) { v -> SettingsRepo.update { copy(volumeHoldMs = (v / 100f).toInt() * 100) } }
        GroupToggle(
            Pos.Middle, Icons.Rounded.ScreenRotation, tr("Горизонтально: у камеры", "Landscape: follow the camera", "Landscape: ikut kamera"),
            tr("В повороте остров встаёт у выреза камеры", "In landscape the island sits next to the camera", "Saat landscape island menempel di dekat kamera"),
            s.landscapeFollow, color = Tile.Teal, badge = tr("Новое", "New", "Baru"),
        ) { v -> SettingsRepo.update { copy(landscapeFollow = v) } }
        GroupSlider(
            Pos.Middle, Icons.Rounded.ScreenRotation, tr("Сдвиг по высоте (горизонтально)", "Landscape vertical shift", "Geser vertikal (landscape)"),
            value = s.landscapeShift.toFloat(), range = -150f..150f, label = "${s.landscapeShift} dp", color = Tile.Teal,
        ) { v -> SettingsRepo.update { copy(landscapeShift = v.toInt()) } }
        GroupItem(
            Pos.Middle, Icons.Rounded.FileUpload, tr("Сохранить настройки", "Back up settings", "Cadangkan pengaturan"),
            tr("Файл в папке «Загрузки»", "A file in the Downloads folder", "File di folder Download"), color = Tile.Gray,
            onClick = {
                val ok = runCatching { backupSettings(ctx) }.getOrDefault(false)
                Toast.makeText(
                    ctx,
                    if (ok) tr("Сохранено в Загрузки", "Saved to Downloads", "Tersimpan di Download")
                    else tr("Не удалось сохранить", "Could not save", "Gagal menyimpan"),
                    Toast.LENGTH_SHORT,
                ).show()
            },
        )
        GroupItem(
            Pos.Last, Icons.Rounded.FileDownload, tr("Восстановить настройки", "Restore settings", "Pulihkan pengaturan"),
            tr("Выберите файл копии", "Pick a backup file", "Pilih file cadangan"), color = Tile.Gray,
            onClick = { picker.launch(arrayOf("application/json", "text/plain", "*/*")) },
        )
    }
}

/** Копия настроек в «Загрузки» (Android 10+ без разрешений через MediaStore). */
private fun backupSettings(ctx: android.content.Context): Boolean {
    if (Build.VERSION.SDK_INT < 29) return false
    val values = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, "LiquidIsland-settings.json")
        put(MediaStore.Downloads.MIME_TYPE, "application/json")
    }
    val uri = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
    ctx.contentResolver.openOutputStream(uri)?.use { it.write(SettingsRepo.exportJson().toByteArray()) } ?: return false
    return true
}

/** Пункт «Упругость» с выбором — прямо в «Оформлении», рядом с «Раскрытием острова». */
@Composable
private fun ElasticityItem(pos: Pos, s: IslandSettings, color: Color) {
    var dialog by remember { mutableStateOf(false) }
    GroupItem(pos, Icons.Rounded.Waves, tr("Упругость", "Elasticity"), s.elasticity.title, color = color, onClick = { dialog = true })
    if (dialog) {
        ChoiceDialog(tr("Упругость анимации", "Animation elasticity"), Elasticity.entries, s.elasticity, { it.title },
            { v -> SettingsRepo.update { copy(elasticity = v) } }) { dialog = false }
    }
}

/** Пока двигают ползунок размера, на острове пример активности — сразу видно, какой она будет. */
private fun previewActivity() {
    IslandBus.widthPreviewUntil.value = SystemClock.elapsedRealtime() + 2_500
}

/** «Размер с активностью»: насколько капсула с навигатором, фонариком или музыкой толще и шире спокойной. */
@Composable
fun LookScreen(onBack: () -> Unit) {
    val s by SettingsRepo.state.collectAsState()
    Page(tr("Размер с активностью", "Size with activity"), onBack) {
        IslandPreview()
        Group {
            GroupSlider(
                Pos.First, Icons.Rounded.LineWeight, tr("Толщина", "Thickness"),
                value = s.activeExtra.toFloat(), range = 0f..14f, label = "+${s.activeExtra} dp", steps = 13, color = Tile.Purple,
            ) { v ->
                previewActivity()
                SettingsRepo.update { copy(activeExtra = v.toInt()) }
            }
            GroupSlider(
                Pos.Last, Icons.Rounded.SwapHoriz, tr("Ширина", "Width"),
                value = s.activeWidth.toFloat(), range = 0f..120f, steps = 23,
                label = if (s.activeWidth == 0) tr("Как без неё", "As without it") else "+${s.activeWidth} dp", color = Tile.Blue,
            ) { v ->
                // Сразу видно, не налезает ли капсула на значки строки состояния
                previewActivity()
                SettingsRepo.update { copy(activeWidth = (v / 5).toInt() * 5) }
            }
        }
    }
}

/** «Раскрытие острова»: как остров раскрывается, насколько пружинит и сколько видно события. */
@Composable
fun AnimScreen(onBack: () -> Unit) {
    val s by SettingsRepo.state.collectAsState()
    var styleDialog by remember { mutableStateOf(false) }

    Page(tr("Раскрытие острова", "Island expansion"), onBack, action = {
        Text(tr("Сброс", "Reset"), color = Ui.Sub, fontSize = 16.sp, modifier = Modifier.clickable { SettingsRepo.resetAnimations() })
    }) {
        Group {
            GroupItem(Pos.First, Icons.Rounded.Animation, tr("Стиль раскрытия", "Expansion style"), s.expandStyle.title, color = Tile.Purple,
                onClick = { styleDialog = true })
            GroupSlider(
                Pos.Middle, Icons.Rounded.Speed, tr("Длительность", "Duration"),
                value = s.durationPercent.toFloat(), range = 0f..100f, label = "${s.durationPercent}%", color = Tile.Indigo,
            ) { v -> SettingsRepo.update { copy(durationPercent = v.toInt()) } }
            // Сколько видно событие; плашки, которым нужно больше времени (сообщение с кнопками, оплата), — от него же
            GroupSlider(
                Pos.Middle, Icons.Rounded.Schedule, tr("Показ событий", "Event display"),
                value = s.noticeDurationMs / 1000f, range = 1.5f..6f, label = tr("%.1f с", "%.1f s").format(s.noticeDurationMs / 1000f), color = Tile.Orange,
            ) { v -> SettingsRepo.update { copy(noticeDurationMs = (v * 1000).toInt()) } }
            GroupToggle(Pos.Last, Icons.Rounded.BlurOn, tr("Уменьшить эффекты", "Reduce effects"),
                tr("Может быть полезно для не самых производительных устройств", "Can help on less powerful devices"),
                s.reduceEffects, color = Tile.Gray) { v -> SettingsRepo.update { copy(reduceEffects = v) } }
        }
    }

    if (styleDialog) {
        ChoiceDialog(tr("Стиль раскрытия", "Expansion style"), ExpandStyle.entries, s.expandStyle, { it.title },
            { v -> SettingsRepo.update { copy(expandStyle = v) } }) { styleDialog = false }
    }
}

/** Что включено в «Эффектах» — подпись пункта. */
private fun effectsSummary(s: IslandSettings): String {
    if (s.reduceEffects) return tr("Выключены: включено «Уменьшить эффекты»", "Off: Reduce effects is on")
    val on = listOfNotNull(
        tr("размытие", "blur").takeIf { s.fxBlur },
        tr("эквалайзер", "equalizer").takeIf { s.fxEqualizer },
        tr("свечение", "glow").takeIf { s.fxGlow },
        tr("волны оплаты", "payment waves").takeIf { s.fxPay },
        tr("вздрагивание", "shake").takeIf { s.fxShake },
    )
    return if (on.isEmpty()) tr("Все выключены", "All off") else on.joinToString(", ").replaceFirstChar { it.uppercase() }
}

/** «Эффекты»: каждый живой эффект острова включается и выключается отдельно. */
@Composable
fun EffectsScreen(onBack: () -> Unit) {
    val s by SettingsRepo.state.collectAsState()
    var rateDialog by remember { mutableStateOf(false) }
    Page(tr("Эффекты", "Effects"), onBack, action = {
        Text(tr("Сброс", "Reset"), color = Ui.Sub, fontSize = 16.sp, modifier = Modifier.clickable { SettingsRepo.resetEffects() })
    }) {
        IslandPreview()
        if (s.reduceEffects) {
            Text(
                tr("Сейчас включено «Уменьшить эффекты» в «Раскрытии острова» — пока оно включено, эффектов нет",
                    "Reduce effects is on in Island expansion — while it's on, there are no effects"),
                color = Tints.Orange, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 8.dp),
            )
        }

        SectionHeader(tr("Движение", "Motion"))
        Group {
            GroupToggle(Pos.First, Icons.Rounded.BlurCircular, tr("Размытие при смене", "Blur on change"),
                tr("Содержимое острова перетекает с размытием, как на iPhone (Android 12+)",
                    "The island's content flows with a blur, like on iPhone (Android 12+)"),
                s.fxBlur, color = Tile.Blue) { v -> SettingsRepo.update { copy(fxBlur = v) } }
            GroupToggle(Pos.Middle, Icons.Rounded.GraphicEq, tr("Живой эквалайзер", "Live equalizer"),
                tr("Полоски у музыки и звонка пляшут. Выключено — стоят ровно, так экономнее",
                    "The music and call bars dance. Off — they stand still, which saves power"),
                s.fxEqualizer, color = Tile.Pink) { v -> SettingsRepo.update { copy(fxEqualizer = v) } }
            // Раскрытие и смена содержимого всегда идут с частотой экрана — это только для того, что движется постоянно
            GroupItem(Pos.Middle, Icons.Rounded.Speed, tr("Частота эквалайзера", "Equalizer frame rate"),
                tr("${s.fxRate.title}. Ещё светящаяся точка фонарика и навигатора",
                    "${s.fxRate.title}. Also the glowing dot of the flashlight and navigation"),
                color = Tile.Indigo, onClick = { rateDialog = true })
            GroupToggle(Pos.Last, Icons.Rounded.NotificationsActive, tr("Вздрагивание", "Shake"),
                tr("Капсула вздрагивает при смене режима звука, колокольчик звонящего таймера качается",
                    "The capsule shakes when the ring mode changes, and a ringing timer's bell swings"),
                s.fxShake, color = Tile.Orange) { v -> SettingsRepo.update { copy(fxShake = v) } }
        }

        SectionHeader(tr("Свечение", "Glow"))
        Group {
            GroupToggle(Pos.First, Icons.Rounded.Flare, tr("Свечение обложки", "Artwork glow"),
                tr("Мягкий ореол цвета обложки вокруг острова, пока играет музыка", "A soft halo in the artwork's color around the island while music plays"),
                s.fxGlow, color = Tile.Purple) { v -> SettingsRepo.update { copy(fxGlow = v) } }
            GroupToggle(Pos.Last, Icons.Rounded.Contactless, tr("Волны оплаты", "Payment waves"),
                tr("Волны NFC вокруг острова, пока ждём терминал, и вспышка с искрами на «Оплачено»",
                    "NFC waves around the island while waiting for the reader, and a burst of sparks on Paid"),
                s.fxPay, color = Tile.Teal) { v -> SettingsRepo.update { copy(fxPay = v) } }
        }

        SectionHeader(tr("Батарея", "Battery"))
        GroupToggle(Pos.Single, Icons.Rounded.BatterySaver, tr("Экономия заряда", "Battery saving"),
            tr("В режиме энергосбережения Android остров сам обходится без живых эффектов",
                "In Android's battery saver mode the island skips live effects by itself"),
            s.batterySaver, color = Tile.Green) { v -> SettingsRepo.update { copy(batterySaver = v) } }
    }

    if (rateDialog) {
        ChoiceDialog(tr("Частота эквалайзера", "Equalizer frame rate"), FxRate.entries, s.fxRate, { it.title },
            { v -> SettingsRepo.update { copy(fxRate = v) } }) { rateDialog = false }
    }
}
