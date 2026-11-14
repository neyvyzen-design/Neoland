package com.neoland.app.core

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.telecom.TelecomManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Perintah sistem dari kapsul Nayland: pemutar media, timer, senter, panggilan, clipboard, dan layar. */
object Controls {
    @Volatile
    var mediaController: MediaController? = null

    var torchOn: () -> Unit = {}
    var torchOff: () -> Unit = {}
    var torchLevel: (Int) -> Unit = {}

    fun expand(id: String) {
        IslandBus.focus.value = id
        IslandBus.expanded.value = true
    }

    fun collapse() {
        IslandBus.expanded.value = false
        IslandBus.focus.value = null
    }

    // Pemutar media

    fun playerAction(action: String) {
        runCatching { mediaController?.transportControls?.sendCustomAction(action, null) }
    }

    fun playPause() {
        val c = mediaController ?: return
        runCatching {
            if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause()
            else c.transportControls.play()
        }
    }

    fun next() {
        runCatching { mediaController?.transportControls?.skipToNext() }
    }

    fun prev() {
        runCatching { mediaController?.transportControls?.skipToPrevious() }
    }

    fun seekTo(ms: Long) {
        runCatching { mediaController?.transportControls?.seekTo(ms.coerceAtLeast(0L)) }
    }

    // Timer

    fun startTimer(minutes: Int) {
        val total = minutes.coerceIn(1, 99) * 60_000L
        IslandBus.timer.value = TimerState(total, SystemClock.elapsedRealtime() + total)
    }

    fun startTimerSeconds(seconds: Int) {
        val total = seconds.coerceAtLeast(1) * 1000L
        IslandBus.timer.value = TimerState(total, SystemClock.elapsedRealtime() + total)
    }

    fun pauseTimer() {
        val t = IslandBus.timer.value ?: return
        if (t.pausedLeft == null && !t.ringing) {
            IslandBus.timer.value = t.copy(pausedLeft = t.left(SystemClock.elapsedRealtime()))
        }
    }

    fun resumeTimer() {
        val t = IslandBus.timer.value ?: return
        val left = t.pausedLeft ?: return
        if (!t.ringing) {
            IslandBus.timer.value = t.copy(endAt = SystemClock.elapsedRealtime() + left.coerceAtLeast(1000L), pausedLeft = null)
        }
    }

    fun cancelTimer() {
        IslandBus.timer.value = null
        if (IslandBus.focus.value == "timer") collapse()
    }

    // Stopwatch

    fun startStopwatch() {
        IslandBus.stopwatch.value = StopwatchState(SystemClock.elapsedRealtime(), 0)
    }

    fun toggleStopwatch() {
        val w = IslandBus.stopwatch.value ?: return
        val now = SystemClock.elapsedRealtime()
        IslandBus.stopwatch.value =
            if (w.running) StopwatchState(null, w.elapsed(now)) else StopwatchState(now, w.accumulated)
    }

    fun stopStopwatch() {
        IslandBus.stopwatch.value = null
        if (IslandBus.focus.value == "stopwatch") collapse()
    }

    // Utilitas & Aplikasi

    fun copyText(context: Context, label: String, text: String): Boolean {
        if (text.isEmpty()) return false
        return runCatching {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return false
            cm.setPrimaryClip(ClipData.newPlainText(label, text))
            true
        }.getOrDefault(false)
    }

    fun openApp(context: Context, pkg: String) {
        if (pkg.isBlank()) return
        val intent = context.packageManager.getLaunchIntentForPackage(pkg) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        runCatching { context.startActivity(intent) }
        collapse()
    }

    /** Kirim PendingIntent dari notifikasi dengan izin background activity start untuk Android 14+. */
    fun send(context: Context, intent: PendingIntent?): Boolean {
        intent ?: return false
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val options = ActivityOptions.makeBasic()
                    .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                intent.send(context, 0, null, null, null, null, options.toBundle())
            } else {
                intent.send()
            }
        }.isSuccess
    }

    /** Balas pesan langsung dari kartu notifikasi (mendukung Android 14+ background start). */
    fun sendReply(context: Context, action: MsgAction, text: String): Boolean {
        val clean = text.trim()
        if (clean.isEmpty() || action.inputs.isEmpty()) return false
        return runCatching {
            val inputs = action.inputs.toTypedArray()
            val results = android.os.Bundle()
            inputs.forEach { results.putCharSequence(it.resultKey, clean) }
            val fill = Intent()
            android.app.RemoteInput.addResultsToIntent(inputs, fill, results)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val options = ActivityOptions.makeBasic()
                    .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                action.intent.send(context, 0, fill, null, null, null, options.toBundle())
            } else {
                action.intent.send(context, 0, fill)
            }
        }.isSuccess
    }

    // Panggilan telepon

    @Suppress("DEPRECATION", "MissingPermission")
    fun answerCall(context: Context, call: CallInfo) {
        if (call.key == TEST_CALL) {
            IslandBus.calls.value = IslandBus.calls.value + (TEST_CALL to call.copy(incoming = false, base = System.currentTimeMillis()))
            return
        }
        if (send(context, call.answer)) return
        val telecom = context.getSystemService(TelecomManager::class.java)
        val ok = telecom != null && runCatching { telecom.acceptRingingCall() }.isSuccess
        if (!ok) showCallScreen(context, call)
    }

    @Suppress("DEPRECATION", "MissingPermission")
    fun endCall(context: Context, call: CallInfo) {
        if (call.key == TEST_CALL) {
            IslandBus.calls.value = IslandBus.calls.value - TEST_CALL
            collapse()
            return
        }
        if (send(context, if (call.incoming) call.decline else call.hangUp)) return
        val telecom = context.getSystemService(TelecomManager::class.java)
        val ok = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            telecom != null &&
            runCatching { telecom.endCall() }.getOrDefault(false)
        if (!ok) showCallScreen(context, call)
    }

    @Suppress("MissingPermission")
    private fun showCallScreen(context: Context, call: CallInfo) {
        val telecom = context.getSystemService(TelecomManager::class.java)
        if (telecom != null && runCatching { telecom.showInCallScreen(false) }.isSuccess) return
        if (send(context, call.open)) return
        val dialer = runCatching { telecom?.defaultDialerPackage }.getOrNull()?.takeIf { it.isNotEmpty() } ?: call.pkg
        if (dialer.isNotEmpty()) openApp(context, dialer)
    }

    fun hideIncomingCall() {
        IslandBus.callHidden.value = true
    }

    fun screenshot() {
        IslandBus.commands.tryEmit(ServiceCommand.SCREENSHOT)
    }

    fun lockScreen() {
        IslandBus.commands.tryEmit(ServiceCommand.LOCK)
    }

    fun turnOffIsland() {
        IslandBus.commands.tryEmit(ServiceCommand.TURN_OFF)
    }

    fun openCamera(context: Context) {
        val primary = Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { context.startActivity(primary) }.isSuccess) return
        val secure = Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(secure) }
    }

    @Suppress("MissingPermission")
    fun openCall(context: Context, call: CallInfo) {
        if (call.key == TEST_CALL) return
        if (send(context, call.open)) return
        val telecom = context.getSystemService(TelecomManager::class.java)
        if (telecom != null && runCatching { telecom.showInCallScreen(false) }.isSuccess) return
        if (call.pkg.isNotEmpty()) openApp(context, call.pkg)
    }

    fun showTestCall() {
        val now = System.currentTimeMillis()
        IslandBus.calls.value = IslandBus.calls.value +
            (TEST_CALL to CallInfo(TEST_CALL, "", tr("Анна", "Anna", "Anna"), null, true, now, now, null, null, null, null))
        IslandBus.scope.launch {
            delay(20_000)
            IslandBus.calls.value = IslandBus.calls.value - TEST_CALL
        }
    }

    const val TEST_CALL = "test-call"

    fun showTestLive(context: Context) {
        val live = LiveInfo(
            key = TEST_LIVE, pkg = context.packageName, kind = LiveKind.NAV,
            title = tr("300 м", "300 m", "300 m"),
            text = tr("Поверните направо на Тверскую улицу", "Turn right onto Main Street", "Belok kanan ke Jalan Utama"),
            sub = "",
            icon = com.neoland.app.service.NotificationParser.icon(context, context.packageName), art = null,
            postTime = System.currentTimeMillis(), open = null,
            distance = tr("300 м", "300 m", "300 m"),
            eta = tr("Прибытие в 18:42 · 12 мин · 4,2 км", "Arrive 18:42 · 12 min · 4.2 km", "Tiba 18.42 · 12 mnt · 4,2 km"),
            turn = Turn.RIGHT,
            expiresAt = System.currentTimeMillis() + TEST_LIVE_MS,
        )
        IslandBus.live.value = IslandBus.live.value + (TEST_LIVE to live)
        IslandBus.post(com.neoland.app.service.LiveParser.banner(context, live))
        IslandBus.scope.launch {
            delay(TEST_LIVE_MS)
            IslandBus.live.value = IslandBus.live.value - TEST_LIVE
        }
    }

    private const val TEST_LIVE_MS = 10_000L
    const val TEST_LIVE = "test-live"

    fun openOutput(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_VOLUME)
        } else {
            Intent(Settings.ACTION_SOUND_SETTINGS)
        }
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        collapse()
    }
}
