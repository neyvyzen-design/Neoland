package com.neoland.app.service

import com.neoland.app.core.CrashLog
import android.app.KeyguardManager
import android.app.NotificationManager
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.hardware.camera2.CameraManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import com.neoland.app.core.BatteryInfo
import com.neoland.app.core.Controls
import com.neoland.app.core.IslandBus
import com.neoland.app.core.Notice
import com.neoland.app.core.NoticeStyle
import com.neoland.app.core.Tints
import com.neoland.app.R
import com.neoland.app.core.tr
import com.neoland.app.data.SettingsRepo
import com.neoland.app.island.Ios
import kotlin.math.abs

/** Системные события, которые остров показывает короткими уведомлениями. */
class SystemEvents(private val context: Context) {
    private fun ic(id: Int) = Ios.load(context, id)

    private val main = Handler(Looper.getMainLooper())
    private val startedAt = SystemClock.elapsedRealtime()
    private val audio = context.getSystemService(AudioManager::class.java)
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    private val notifications = context.getSystemService(NotificationManager::class.java)
    private val keyguard = context.getSystemService(KeyguardManager::class.java)
    private val s get() = SettingsRepo.current

    private var lowWarned = false
    private var heatWarned = false
    private var limitReached = false
    private var micActive = false
    private var lastLocation: Boolean? = null
    private var lastPrivacy = 0L
    private var volumeRunnable: Runnable? = null
    private val cameraManager = context.getSystemService(CameraManager::class.java)
    private var usbConnected: Boolean? = null
    private var brightnessMode = -1
    private var dnd: Boolean? = null
    private val headsetBattery = HashMap<String, Int>()
    private val wifiNetworks = HashSet<Network>()
    /** Когда подключили или отключили зарядку — кабель от компьютера не должен давать вторую плашку «USB» */
    private var lastPowerChange = 0L

    /** Первые секунды после запуска система присылает текущее состояние — это не события. */
    private val warmingUp get() = SystemClock.elapsedRealtime() - startedAt < 3000

    private fun notice(enabled: Boolean, n: Notice) {
        if (enabled && !warmingUp) IslandBus.post(n)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            // Ошибка в одном событии не должна ронять весь процесс острова
            runCatching { handle(intent, isInitialStickyBroadcast) }.onFailure { CrashLog.record("SystemEvents", it) }
        }
    }

    private val wifiCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            main.post {
                val wasEmpty = wifiNetworks.isEmpty()
                wifiNetworks += network
                if (wasEmpty) notice(s.evWifiOn, Notice("Wi-Fi", tr("Подключён", "Connected"), ic(R.drawable.ios_wifi), Tints.Blue))
            }
        }

        override fun onLost(network: Network) {
            main.post {
                if (wifiNetworks.remove(network) && wifiNetworks.isEmpty()) {
                    notice(s.evWifiOff, Notice("Wi-Fi", tr("Отключён", "Disconnected"), ic(R.drawable.ios_wifi), Tints.Gray))
                }
            }
        }
    }

    private val brightnessObserver = object : ContentObserver(main) {
        override fun onChange(selfChange: Boolean) {
            val mode = Settings.System.getInt(
                context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, 0,
            )
            if (mode == brightnessMode) return
            brightnessMode = mode
            val auto = mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            notice(
                s.evBrightness,
                Notice(
                    tr("Яркость", "Brightness"),
                    if (auto) tr("Авто", "Auto") else tr("Вручную", "Manual"),
                    if (auto) ic(R.drawable.ios_sunny) else ic(R.drawable.ios_contrast),
                    Tints.Yellow,
                ),
            )
        }
    }

    fun start() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BT_BATTERY_CHANGED)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(USB_STATE)
            addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            addAction(VOLUME_CHANGED)
            addAction(HOTSPOT_CHANGED)
            addAction(LOCATION_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)

        dnd = isDnd()
        val power = context.getSystemService(PowerManager::class.java)
        IslandBus.screenOn.value = power?.isInteractive ?: true
        IslandBus.powerSave.value = power?.isPowerSaveMode == true
        IslandBus.locked.value = isLocked()
        IslandBus.keyguard.value = isKeyguard()
        brightnessMode = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, 0)
        context.contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE), false, brightnessObserver,
        )

        runCatching { audio?.registerAudioRecordingCallback(micCallback, main) }
        runCatching { cameraManager?.registerAvailabilityCallback(cameraCallback, main) }
        runCatching {
            connectivity?.registerNetworkCallback(
                NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build(),
                wifiCallback,
            )
        }
    }

    fun stop() {
        runCatching { context.unregisterReceiver(receiver) }
        runCatching { context.contentResolver.unregisterContentObserver(brightnessObserver) }
        runCatching { connectivity?.unregisterNetworkCallback(wifiCallback) }
        runCatching { audio?.unregisterAudioRecordingCallback(micCallback) }
        runCatching { cameraManager?.unregisterAvailabilityCallback(cameraCallback) }
        volumeRunnable?.let { main.removeCallbacks(it) }
    }

    private fun handle(intent: Intent, initialSticky: Boolean) {
        when (intent.action) {
            Intent.ACTION_BATTERY_CHANGED -> onBattery(intent)

            VOLUME_CHANGED -> onVolume(intent)

            HOTSPOT_CHANGED -> {
                when (intent.getIntExtra("wifi_state", -1)) {
                    13 -> notice(s.evHotspot, Notice(tr("Точка доступа", "Hotspot", "Hotspot"), tr("Вкл", "On", "Aktif"), ic(R.drawable.ic_hotspot_172), Tints.Green))
                    11 -> notice(s.evHotspot, Notice(tr("Точка доступа", "Hotspot", "Hotspot"), tr("Выкл", "Off", "Mati"), ic(R.drawable.ic_hotspot_172), Tints.Gray))
                }
            }

            LOCATION_CHANGED -> onLocation()

            Intent.ACTION_POWER_CONNECTED -> {
                lastPowerChange = SystemClock.elapsedRealtime()
                val level = IslandBus.battery.value.level
                notice(
                    s.evChargeStart,
                    Notice(tr("Зарядка", "Charging"), if (level >= 0) "$level%" else null, ic(R.drawable.ios_flash), Tints.Green,
                        battery = level.takeIf { it >= 0 }),
                )
            }

            Intent.ACTION_POWER_DISCONNECTED -> {
                lastPowerChange = SystemClock.elapsedRealtime()
                val level = IslandBus.battery.value.level
                notice(
                    s.evChargeStop,
                    Notice(tr("Отключено", "Unplugged"), if (level >= 0) "$level%" else null, ic(R.drawable.ios_battery_full), Tints.Orange,
                        battery = level.takeIf { it >= 0 }),
                )
            }

            Intent.ACTION_USER_PRESENT -> onUnlocked()

            Intent.ACTION_SCREEN_OFF -> {
                IslandBus.screenOn.value = false
                // Блокировка может включиться чуть позже экрана — заранее прячем тексты, если она вообще есть
                if (keyguard?.isDeviceSecure == true) IslandBus.locked.value = true
                IslandBus.reply.value = null
                // Как на iPhone: кнопка питания выключает звонящий таймер
                if (IslandBus.timer.value?.ringing == true) Controls.cancelTimer()
                Controls.collapse()
            }

            Intent.ACTION_SCREEN_ON -> {
                IslandBus.screenOn.value = true
                IslandBus.locked.value = isLocked()
                IslandBus.keyguard.value = isKeyguard()
            }

            PowerManager.ACTION_POWER_SAVE_MODE_CHANGED ->
                IslandBus.powerSave.value = context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true

            NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED -> {
                val on = isDnd()
                if (on == dnd) return
                dnd = on
                // Как фокус «Не беспокоить» на iPhone: луна и состояние
                notice(
                    s.evDnd,
                    Notice(tr("Не беспокоить", "Do Not Disturb"), if (on) tr("Вкл", "On") else tr("Выкл", "Off"), ic(R.drawable.ios_moon),
                        if (on) Tints.Purple else Tints.Gray),
                )
            }

            Intent.ACTION_AIRPLANE_MODE_CHANGED -> {
                val on = intent.getBooleanExtra("state", false)
                if (on) notice(s.evAirplaneOn, Notice(tr("Режим полёта", "Airplane Mode"), tr("Вкл", "On"), ic(R.drawable.ios_airplane), Tints.Orange))
                else notice(s.evAirplaneOff, Notice(tr("Режим полёта", "Airplane Mode"), tr("Выкл", "Off"), ic(R.drawable.ios_airplane), Tints.Gray))
            }

            AudioManager.RINGER_MODE_CHANGED_ACTION -> {
                val n = when (intent.getIntExtra(AudioManager.EXTRA_RINGER_MODE, -1)) {
                    // Как переключатель «Звонок/Бесшумно» на iPhone: короткая пилюля, колокольчик качается
                    AudioManager.RINGER_MODE_NORMAL -> Notice("", tr("Звонок", "Ring"), ic(R.drawable.ios_notifications), Color.White, style = NoticeStyle.PILL)
                    AudioManager.RINGER_MODE_VIBRATE -> Notice("", tr("Вибрация", "Vibrate"), ic(R.drawable.ios_phone_portrait),
                        Tints.Purple, style = NoticeStyle.PILL)
                    AudioManager.RINGER_MODE_SILENT -> Notice("", tr("Без звука", "Silent"), ic(R.drawable.ios_notifications_off),
                        Tints.Red, style = NoticeStyle.PILL)
                    else -> null
                }
                if (n != null) notice(s.evRinger, n)
            }


            BluetoothDevice.ACTION_ACL_CONNECTED, BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val device = IntentCompat.getParcelableExtra(intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    ?: return
                // Без разрешения BLUETOOTH_CONNECT имени и класса устройства не узнать
                val name = runCatching { device.name }.getOrNull()
                // Класс не прочитать — не знаем, наушники ли это (могут быть часы или машина), молчим
                val isAudio = runCatching {
                    device.bluetoothClass?.majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO
                }.getOrDefault(false)
                if (!isAudio) {
                    // Не наушники (часы, клавиатура, колонка авто): общая плашка «Bluetooth»
                    val on = intent.action == BluetoothDevice.ACTION_ACL_CONNECTED
                    notice(
                        s.evBluetooth,
                        Notice(name ?: "Bluetooth", null, ic(R.drawable.ic_bluetooth_172), if (on) Tints.Green else Tints.Gray,
                            caption = if (on) tr("Подключено", "Connected", "Terhubung") else tr("Отключено", "Disconnected", "Terputus")),
                    )
                    return
                }
                if (intent.action == BluetoothDevice.ACTION_ACL_CONNECTED) {
                    val battery = headsetBattery[device.address]
                    notice(
                        s.evHeadsetOn,
                        Notice(name ?: tr("Наушники", "Headphones"), null, ic(R.drawable.ios_headset), Tints.Green,
                            ring = battery?.let { it / 100f }, caption = tr("Подключено", "Connected")),
                    )
                } else {
                    headsetBattery.remove(device.address)
                    notice(s.evHeadsetOff, Notice(name ?: tr("Наушники", "Headphones"), null, ic(R.drawable.ios_headset), Tints.Gray,
                        caption = tr("Отключено", "Disconnected")))
                }
            }

            BT_BATTERY_CHANGED -> {
                val device = IntentCompat.getParcelableExtra(intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    ?: return
                val level = intent.getIntExtra(EXTRA_BT_BATTERY, -1)
                if (level >= 0) headsetBattery[device.address] = level
            }

            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                val dev = IntentCompat.getParcelableExtra(intent, UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                notice(s.evUsbOn, Notice(dev?.productName ?: tr("USB-устройство", "USB device"), tr("Подключено", "Connected"),
                    ic(R.drawable.ios_hardware_chip), Tints.Teal))
            }

            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                val dev = IntentCompat.getParcelableExtra(intent, UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                notice(s.evUsbOff, Notice(dev?.productName ?: tr("USB-устройство", "USB device"), tr("Отключено", "Disconnected"),
                    ic(R.drawable.ios_hardware_chip), Tints.Gray))
            }

            USB_STATE -> {
                val connected = intent.getBooleanExtra("connected", false)
                val prev = usbConnected
                usbConnected = connected
                if (initialSticky || prev == null || prev == connected) return
                // Кабель от компьютера ещё и заряжает: плашки «Зарядка» достаточно. Порядок сообщений
                // системы не определён, поэтому смотрим чуть позже, менялась ли зарядка рядом по времени
                main.postDelayed({
                    if (abs(SystemClock.elapsedRealtime() - lastPowerChange) < 3000) return@postDelayed
                    if (connected) notice(s.evUsbOn, Notice("USB", tr("Подключено", "Connected"), ic(R.drawable.ios_hardware_chip), Tints.Teal))
                    else notice(s.evUsbOff, Notice("USB", tr("Отключено", "Disconnected"), ic(R.drawable.ios_hardware_chip), Tints.Gray))
                }, 1500)
            }
        }
    }

    private var lastUnlock = 0L

    /**
     * Разблокировали. Сообщение системы USER_PRESENT доходит с задержкой, поэтому служба острова
     * зовёт это раньше — как только видит, что экран блокировки ушёл. Второй вызов подряд ничего не делает.
     */
    fun onUnlocked() {
        IslandBus.locked.value = false
        IslandBus.keyguard.value = false
        val now = SystemClock.elapsedRealtime()
        if (now - lastUnlock < 3_000) return
        lastUnlock = now
        // 1.7.9: после галочки Face ID значок «замок открыт» не нужен
        if (now - com.neoland.app.island.FaceState.lastOkAt < 3_500) return
        notice(s.evUnlock, Notice("", null, ic(R.drawable.ios_lock_open), Color.White, style = NoticeStyle.PILL))
    }

    /** 1.7.7: пока экран блокировки на экране, телефон мог открыться по лицу или отпечатку — читаем это, не дожидаясь USER_PRESENT */
    fun refreshLock() {
        val now = isLocked()
        if (IslandBus.locked.value == now) return
        IslandBus.locked.value = now
        com.neoland.app.core.CrashLog.record("LockState", IllegalStateException("INFO: deviceLocked=$now keyguard=${isKeyguard()}"))
    }

    /** Заблокирован ли телефон так, что для входа нужен PIN-код, ключ или пароль. */
    private fun isLocked(): Boolean = keyguard?.isDeviceLocked == true

    /** На экране экран блокировки — с PIN-кодом или просто «проведите, чтобы разблокировать». */
    private fun isKeyguard(): Boolean = keyguard?.isKeyguardLocked == true

    private fun isDnd(): Boolean {
        val filter = notifications?.currentInterruptionFilter ?: return false
        return filter != NotificationManager.INTERRUPTION_FILTER_ALL &&
            filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
    }

    private fun onBattery(intent: Intent) {
        val raw = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val level = if (raw >= 0) raw * 100 / scale else -1
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val charging = plugged != 0 &&
            (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL)
        IslandBus.battery.value = BatteryInfo(level, charging)
        com.neoland.app.island.BatteryEstimator.update(level, charging, System.currentTimeMillis())
        checkHeat(intent)
        checkChargeLimit(level, charging)
        checkFull(level, status, plugged)

        if (charging || level > 5) {
            lowWarned = false
        } else if (level >= 0 && !lowWarned) {
            lowWarned = true
            notice(s.evLowBattery, Notice(tr("Низкий заряд", "Low Battery"), "$level%", ic(R.drawable.ios_battery_dead), Tints.Red, battery = level))
        }
    }

    /** 1.7.2: батарея слишком горячая (EXTRA_TEMPERATURE — в десятых долях °C) */
    private fun checkHeat(intent: Intent) {
        val raw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        if (raw < 0) return
        val temp = raw / 10f
        val limit = s.heatLimit
        if (temp < limit - 3f) {
            heatWarned = false
            return
        }
        if (temp >= limit && !heatWarned) {
            heatWarned = true
            notice(
                s.evHeat,
                Notice(tr("Батарея горячая", "Battery hot", "Baterai panas"), "%.1f°C".format(temp), ic(R.drawable.ic_thermo_172), Tints.Orange),
            )
        }
    }

    private var fullShown = false

    /** 1.7.4: батарея полностью заряжена при подключённом питании */
    private fun checkFull(level: Int, status: Int, plugged: Int) {
        val full = plugged != 0 && (status == BatteryManager.BATTERY_STATUS_FULL || level >= 100)
        if (!full) {
            fullShown = false
            return
        }
        if (fullShown) return
        fullShown = true
        if (warmingUp) return
        notice(s.evFull, Notice(tr("Заряд полный", "Battery full", "Baterai penuh"), "100%", ic(R.drawable.ios_battery_full), Tints.Green, battery = 100))
    }

    /** 1.7.2: при зарядке дошли до выбранного уровня (0 — выключено) */
    private fun checkChargeLimit(level: Int, charging: Boolean) {
        val limit = s.chargeLimit
        if (!charging || limit <= 0 || level < 0) {
            limitReached = false
            return
        }
        if (level >= limit && !limitReached) {
            limitReached = true
            notice(true, Notice(tr("Лимит зарядки", "Charge limit", "Batas isi"), "$level%", ic(R.drawable.ios_battery_full), Tints.Green, battery = level))
        } else if (level < limit - 2) {
            limitReached = false
        }
    }

    /** Пилюля громкости: появляется сразу и обновляется на месте при каждом нажатии (без очереди и задержки) */
    private fun onVolume(intent: Intent) {
        if (!s.evVolume || warmingUp) return
        if (intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1) != AudioManager.STREAM_MUSIC) return
        val am = audio ?: return
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val value = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", am.getStreamVolume(AudioManager.STREAM_MUSIC))
        val level = value.coerceIn(0, max) / max.toFloat()
        val icon = if (value <= 0) R.drawable.ic_volume_off_172 else R.drawable.ic_volume_172
        IslandBus.showVolume(
            Notice("", null, ic(icon), Color.White, ring = level, caption = IslandBus.VOLUME_KEY, style = NoticeStyle.PILL),
        )
    }

    /** Геолокация включена или выключена */
    private fun onLocation() {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager ?: return
        val on = androidx.core.location.LocationManagerCompat.isLocationEnabled(lm)
        if (lastLocation == on) return
        lastLocation = on
        notice(
            s.evLocation,
            Notice(tr("Геолокация", "Location", "Lokasi"), if (on) tr("Вкл", "On", "Aktif") else tr("Выкл", "Off", "Mati"),
                ic(R.drawable.ic_location_172), if (on) Tints.Green else Tints.Gray),
        )
    }

    /** 1.7.2: индикатор микрофона (запись звука любым приложением) */
    private val micCallback = object : AudioManager.AudioRecordingCallback() {
        override fun onRecordingConfigChanged(configs: MutableList<AudioRecordingConfiguration>?) {
            val active = !configs.isNullOrEmpty()
            if (active && !micActive) {
                privacy { Notice("", tr("Микрофон", "Microphone", "Mikrofon"), ic(R.drawable.ic_mic_172), Tints.Orange, style = NoticeStyle.PILL) }
            }
            micActive = active
        }
    }

    /** 1.7.2: индикатор камеры (камеру открыло любое приложение, кроме фонарика острова) */
    private val cameraCallback = object : CameraManager.AvailabilityCallback() {
        override fun onCameraUnavailable(cameraId: String) {
            if (IslandBus.torch.value.on) return
            privacy { Notice("", tr("Камера", "Camera", "Kamera"), ic(R.drawable.ic_camera_172), Tints.Green, style = NoticeStyle.PILL) }
        }
    }

    private fun privacy(build: () -> Notice) {
        if (!s.evPrivacy) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastPrivacy < 20_000) return
        lastPrivacy = now
        runCatching { notice(true, build()) }.onFailure { CrashLog.record("privacy", it) }
    }

    private companion object {
        // Скрытые, но давно стабильные широковещательные сообщения системы
        const val BT_BATTERY_CHANGED = "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
        const val EXTRA_BT_BATTERY = "android.bluetooth.device.extra.BATTERY_LEVEL"
        const val USB_STATE = "android.hardware.usb.action.USB_STATE"
        const val VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION"
        const val HOTSPOT_CHANGED = "android.net.wifi.WIFI_AP_STATE_CHANGED"
        const val LOCATION_CHANGED = "android.location.MODE_CHANGED"
    }
}
