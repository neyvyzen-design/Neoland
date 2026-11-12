package com.neoland.app.core

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalConfiguration
import com.neoland.app.data.AppLanguage
import com.neoland.app.data.SettingsRepo
import java.util.Locale

object Lang {
    private val selected get() = SettingsRepo.current.language
    private fun systemLanguage(): String = runCatching {
        Resources.getSystem().configuration.locales[0].language
    }.getOrNull() ?: Locale.getDefault().language
    val id: Boolean get() = selected == AppLanguage.ID ||
        (selected == AppLanguage.SYSTEM && systemLanguage() in listOf("id", "in"))
    val en: Boolean get() = selected == AppLanguage.EN ||
        (selected == AppLanguage.SYSTEM && systemLanguage() !in listOf("ru", "id", "in"))
    val locale: Locale get() = when {
        id -> Locale.forLanguageTag("id-ID")
        en -> Locale.ENGLISH
        else -> Locale.forLanguageTag("ru")
    }
}

/* Terjemahan untuk teks UI yang paling sering terlihat. Teks yang belum ada tetap memakai
   bahasa Inggris agar tidak merusak placeholder, nama aplikasi, atau format dinamis. */
private val idText = mapOf(
    "Home" to "Beranda", "Features" to "Fitur", "Appearance" to "Tampilan",
    "Home screen" to "Layar utama", "Lock screen" to "Layar kunci",
    "Position and size" to "Posisi dan ukuran", "Horizontal" to "Posisi horizontal",
    "Top offset" to "Jarak dari atas",
    "Width" to "Lebar", "Height" to "Tinggi", "Center" to "Tengah",
    "About" to "Tentang", "About the island" to "Tentang Dynamic Island",
    "What the island shows" to "Yang ditampilkan island", "Dynamic cards" to "Kartu dinamis",
    "Music, calls, timers, flashlight, navigation" to "Musik, panggilan, timer, senter, navigasi",
    "Notifications" to "Notifikasi", "App messages with replies right in the island" to "Pesan aplikasi dengan balasan langsung di island",
    "Events" to "Peristiwa", "Charging, Wi-Fi, headphones, payments, alarm" to "Pengisian daya, Wi-Fi, earphone, pembayaran, alarm",
    "Lyrics" to "Lirik", "Synced lyrics in the expanded player" to "Lirik tersinkronisasi di pemutar yang diperluas",
    "Controls" to "Kontrol", "Gestures" to "Gestur", "Taps and swipes on the capsule" to "Ketukan dan usapan pada kapsul",
    "Behavior" to "Perilaku", "When to show and collapse the island, haptics" to "Waktu tampil dan menutup island, serta getaran",
    "System banners" to "Notifikasi sistem", "General" to "Umum", "Language" to "Bahasa",
    "Bahasa Indonesia" to "Bahasa Indonesia", "System default" to "Ikuti sistem",
    "Reset all settings" to "Atur ulang semua pengaturan", "Back to how it was after installing" to "Kembalikan ke pengaturan awal",
    "Reset settings?" to "Atur ulang pengaturan?", "Reset" to "Atur ulang", "Cancel" to "Batal",
    "When to show" to "Kapan ditampilkan", "Hide the empty capsule" to "Sembunyikan kapsul kosong",
    "Show the capsule only when it has something in it" to "Tampilkan kapsul hanya saat ada aktivitas",
    "Hide in landscape" to "Sembunyikan dalam mode lanskap", "Stays out of games and videos" to "Tidak mengganggu game dan video",
    "Hide the player in its own app" to "Sembunyikan pemutar di aplikasinya", "Schedule" to "Jadwal",
    "On a schedule" to "Sesuai jadwal", "Expanded island" to "Island yang diperluas",
    "Collapse the expanded island" to "Tutup island yang diperluas", "Expand" to "Perluas",
    "Feedback" to "Respons", "Haptics" to "Getaran", "Turn on the screen" to "Nyalakan layar",
    "Color" to "Warna", "Capsule color" to "Warna kapsul", "Outline" to "Garis tepi",
    "Thickness" to "Ketebalan", "Shadow" to "Bayangan", "Effects" to "Efek",
    "Animations" to "Animasi", "Duration" to "Durasi", "Elasticity" to "Elastisitas",
    "Brightness" to "Kecerahan", "Auto" to "Otomatis", "Manual" to "Manual",
    "Back" to "Kembali", "Close" to "Tutup", "Open" to "Buka", "Save" to "Simpan",
    "Done" to "Selesai", "Later" to "Nanti", "Install" to "Instal", "Update" to "Perbarui",
    "Checking for updates…" to "Memeriksa pembaruan…", "Check for updates" to "Periksa pembaruan",
    "Check now" to "Periksa sekarang", "Up to date" to "Sudah versi terbaru",
    "Permissions" to "Izin", "All granted" to "Semua izin diberikan", "Needed: " to "Diperlukan: ",
    "Accessibility" to "Aksesibilitas", "Notification access" to "Akses notifikasi",
    "Calls" to "Panggilan", "Incoming call" to "Panggilan masuk", "Message" to "Pesan",
    "Reply" to "Balas", "Mark as read" to "Tandai sudah dibaca", "Sent" to "Terkirim",
    "Timer" to "Timer", "Stopwatch" to "Stopwatch", "Flashlight" to "Senter",
    "Charging" to "Mengisi daya", "Connected" to "Terhubung", "Disconnected" to "Terputus",
    "Airplane Mode" to "Mode pesawat", "Do Not Disturb" to "Jangan ganggu",
    "Headphones" to "Earphone", "Wi-Fi connected" to "Wi-Fi terhubung", "Wi-Fi disconnected" to "Wi-Fi terputus",
    "Payment" to "Pembayaran", "Paid" to "Berhasil dibayar", "Hold near reader" to "Dekatkan ke pembaca",
    "Navigation" to "Navigasi", "Turn left" to "Belok kiri", "Turn right" to "Belok kanan",
    "Go straight" to "Lurus", "Make a U-turn" to "Putar balik", "now" to "sekarang",
    "today" to "hari ini", "tomorrow" to "besok", "On" to "Aktif", "Off" to "Nonaktif",
    "Yes" to "Ya", "No" to "Tidak", "Hide" to "Sembunyikan", "Stop" to "Berhenti",
    "Pause" to "Jeda", "Paused" to "Dijeda", "Open Accessibility" to "Buka Aksesibilitas",
    "Try it" to "Coba", "Set up" to "Atur", "Instructions" to "Petunjuk",
    "What's new" to "Yang baru", "Project" to "Proyek", "GitHub page" to "Halaman GitHub"

    ,"App info in Android" to "Info aplikasi di Android"
    ,"App notifications" to "Notifikasi aplikasi"
    ,"Appearance" to "Tampilan"
    ,"Animation elasticity" to "Elastisitas animasi"
    ,"Artwork glow" to "Cahaya sampul musik"
    ,"Arrive \$it" to "Tiba \$it"
    ,"Arrive 18:42 · 12 min · 4.2 km" to "Tiba 18.42 · 12 menit · 4,2 km"
    ,"Battery saver or “Activity control” → “No restrictions”" to "Penghemat baterai atau “Kontrol aktivitas” → “Tanpa batasan”"
    ,"Battery saving" to "Penghemat baterai"
    ,"Battery usage → allow background activity" to "Penggunaan baterai → izinkan aktivitas latar belakang"
    ,"Battery → Unrestricted, and remove the island from Sleeping apps" to "Baterai → Tanpa batasan, lalu keluarkan Island dari aplikasi tidur"
    ,"Blur on change" to "Buram saat berubah"
    ,"Brightness mode" to "Mode kecerahan"
    ,"Button color" to "Warna tombol"
    ,"Call" to "Panggilan"
    ,"Can help on less powerful devices" to "Dapat membantu perangkat dengan performa rendah"
    ,"Can't reach GitHub. Check your internet connection and try again." to "Tidak dapat terhubung ke GitHub. Periksa koneksi internet lalu coba lagi."
    ,"Center" to "Tengah"
    ,"Changes in version \$version" to "Perubahan pada versi \$version"
    ,"Charging indicator" to "Indikator pengisian daya"
    ,"Charging started" to "Pengisian daya dimulai"
    ,"Charging stopped" to "Pengisian daya dihentikan"
    ,"Check now" to "Periksa sekarang"
    ,"Checking…" to "Memeriksa…"
    ,"Clock buttons" to "Tombol jam"
    ,"Close" to "Tutup"
    ,"Color and glass" to "Warna dan kaca"
    ,"Color from artwork" to "Warna dari sampul musik"
    ,"Color, outline and shadow" to "Warna, garis tepi, dan bayangan"
    ,"Connections" to "Koneksi"
    ,"Content hidden" to "Konten disembunyikan"
    ,"Copy all" to "Salin semua"
    ,"Couldn't check for updates" to "Tidak dapat memeriksa pembaruan"
    ,"Couldn't download. Check your internet and try again." to "Tidak dapat mengunduh. Periksa internet lalu coba lagi."
    ,"Couldn't send the reply" to "Tidak dapat mengirim balasan"
    ,"Description, new versions and source code" to "Deskripsi, versi terbaru, dan kode sumber"
    ,"Device unlock" to "Buka kunci perangkat"
    ,"Downloading…" to "Mengunduh…"
    ,"Downloads and installs in one tap" to "Unduh dan instal dengan satu ketukan"
    ,"Downloads and uploads in Google Play, Telegram and other apps" to "Unduhan dan unggahan di Google Play, Telegram, dan aplikasi lain"
    ,"Event display" to "Tampilan peristiwa"
    ,"Example: a new version arrives" to "Contoh: versi baru tersedia"
    ,"Expand the capsule, up — collapse" to "Perluas kapsul, geser ke atas untuk menutup"
    ,"Expansion style" to "Gaya perluasan"
    ,"Experimental" to "Eksperimental"
    ,"Find Dynamic Island and choose “Don't optimize” or “Unrestricted”" to "Cari Dynamic Island lalu pilih “Jangan optimalkan” atau “Tanpa batasan”"
    ,"First turn on the service in Accessibility" to "Aktifkan layanan terlebih dahulu di Aksesibilitas"
    ,"Flashlight controls" to "Kontrol senter"
    ,"From the open app" to "Dari aplikasi yang sedang dibuka"
    ,"Glass density" to "Kerapatan efek kaca"
    ,"Glow" to "Cahaya"
    ,"Headphone names when they connect" to "Nama headphone saat tersambung"
    ,"Headphones connected" to "Headphone tersambung"
    ,"Headphones disconnected" to "Headphone terputus"
    ,"Help" to "Bantuan"
    ,"Hi! This is how a notification looks in the island" to "Hai! Seperti inilah tampilan notifikasi di Island"
    ,"How it works" to "Cara kerja"
    ,"How to turn the island on with Android 13+" to "Cara mengaktifkan Island di Android 13 ke atas"
    ,"If it doesn't turn on" to "Jika tidak dapat diaktifkan"
    ,"If the island still disappears" to "Jika Island masih menghilang"
    ,"If the switch is grayed out (Android 13+)" to "Jika tombol tidak dapat ditekan (Android 13 ke atas)"
    ,"Installed version: \$current" to "Versi terpasang: \$current"
    ,"Instructions" to "Petunjuk"
    ,"Island expansion" to "Perluasan Island"
    ,"Island on" to "Island aktif"
    ,"Island running" to "Island sedang berjalan"
    ,"Keep the island loaded" to "Jaga Island tetap aktif"
    ,"Keep the island running" to "Biarkan Island tetap berjalan"
    ,"Live equalizer" to "Equalizer langsung"
    ,"Lock it in recent apps" to "Kunci di aplikasi terbaru"
    ,"Lock on the lock screen" to "Kunci di layar kunci"
    ,"Looking for lyrics…" to "Mencari lirik…"
    ,"Low Battery" to "Baterai lemah"
    ,"Low battery" to "Baterai lemah"
    ,"Messages show up twice?" to "Pesan muncul dua kali?"
    ,"Messengers" to "Aplikasi pesan"
    ,"Modes" to "Mode"
    ,"Music and calls" to "Musik dan panggilan"
    ,"New" to "Baru"
    ,"No Clock app found" to "Aplikasi Jam tidak ditemukan"
    ,"No dialer found — call yourself and it will show up here" to "Aplikasi telepon tidak ditemukan — lakukan panggilan agar muncul di sini"
    ,"No list of changes found" to "Daftar perubahan tidak ditemukan"
    ,"No lyrics found" to "Lirik tidak ditemukan"
    ,"No message has reached the island yet" to "Belum ada pesan yang masuk ke Island"
    ,"Nothing yet: no navigation, taxi or delivery notification has arrived." to "Belum ada: notifikasi navigasi, taksi, atau pengantaran belum diterima."
    ,"Nothing yet: the island hasn't seen a payment screen or a bank notification." to "Belum ada: Island belum mendeteksi layar pembayaran atau notifikasi bank."
    ,"Notification buttons" to "Tombol notifikasi"
    ,"Open quick settings fully, tap the pencil and drag the Island tile to the others." to "Buka panel pengaturan cepat, ketuk ikon pensil, lalu seret ubin Island ke daftar ubin."
    ,"Other" to "Lainnya"
    ,"Payment screen" to "Layar pembayaran"
    ,"Player controls" to "Kontrol pemutar"
    ,"Position and size" to "Posisi dan ukuran"
    ,"Progress indicator" to "Indikator progres"
    ,"Pull down" to "Usap ke bawah"
    ,"Push sideways" to "Geser ke samping"
    ,"Reduce effects" to "Kurangi efek"
    ,"Ring mode" to "Mode dering"
    ,"Ring, vibrate or silent" to "Dering, getar, atau senyap"
    ,"Restricted settings, permissions, battery" to "Pengaturan terbatas, izin, dan baterai"
    ,"Screen: " to "Layar: "
    ,"Second activity alongside" to "Aktivitas kedua di samping"
    ,"Set up" to "Siapkan"
    ,"Show the capsule only when it has something in it" to "Tampilkan kapsul hanya saat ada aktivitas"
    ,"Single tap" to "Ketuk sekali"
    ,"Size with activity" to "Ukuran saat ada aktivitas"
    ,"So only the island shows calls, timers and battery" to "Agar hanya Island yang menampilkan panggilan, timer, dan baterai"
    ,"So the island shows incoming calls" to "Agar Island menampilkan panggilan masuk"
    ,"Surprises" to "Kejutan"
    ,"Swipes" to "Usapan"
    ,"Switch grayed out? Instructions" to "Tombol tidak aktif? Lihat petunjuk"
    ,"System UI" to "Antarmuka sistem"
    ,"Tap and turn off “Pop on screen”" to "Ketuk lalu nonaktifkan “Munculkan di layar”"
    ,"Tap — the example shows up in the island at the top of the screen" to "Ketuk — contoh akan muncul di Island di bagian atas layar"
    ,"Tapping the empty island" to "Mengetuk Island yang kosong"
    ,"Text lines" to "Baris teks"
    ,"The capsule shows over the camera" to "Kapsul tampil di atas kamera"
    ,"The quick settings tile" to "Ubin pengaturan cepat"
    ,"The service is starting…" to "Layanan sedang dimulai…"
    ,"The tile is already in quick settings" to "Ubin sudah ada di pengaturan cepat"
    ,"The update file is damaged" to "File pembaruan rusak"
    ,"Thickness" to "Ketebalan"
    ,"Timer and alarm alerts" to "Peringatan timer dan alarm"
    ,"Timer and stopwatch" to "Timer dan stopwatch"
    ,"Timer from assistant" to "Timer dari asisten"
    ,"Timers and alarm" to "Timer dan alarm"
    ,"Timers and alarms" to "Timer dan alarm"
    ,"Top offset" to "Jarak dari atas"
    ,"Unplugged" to "Tidak tersambung ke daya"
    ,"Unrestricted battery" to "Baterai tanpa batasan"
    ,"Updates" to "Pembaruan"
    ,"Vibrate" to "Getar"
    ,"Wake up" to "Bangun"
    ,"What it does" to "Fungsinya"
    ,"What it does and where the idea comes from" to "Fungsi dan asal idenya"
    ,"What the island saw when you paid — if “Paid” didn't show" to "Data yang dideteksi Island saat membayar — jika tulisan “Paid” tidak muncul"
    ,"What's new" to "Yang baru"
    ,"What's new in \$current" to "Yang baru di \$current"
    ,"When to show" to "Kapan ditampilkan"
    ,"Width" to "Lebar"
    ,"With Reply and Mark as read buttons" to "Dengan tombol Balas dan Tandai sudah dibaca"
    ,"Yes" to "Ya"
    ,"You can answer or decline" to "Kamu dapat menjawab atau menolak panggilan"
    ,"You have the latest version \$current" to "Kamu sudah menggunakan versi terbaru \$current"

)

fun tr(ru: String, en: String, id: String? = null): String = when {
    Lang.id -> id ?: idText[en] ?: en
    Lang.en -> en
    else -> ru
}

@Composable
fun LangScope(content: @Composable () -> Unit) {
    val s by SettingsRepo.state.collectAsState()
    LocalConfiguration.current
    key(s.language, Lang.en, Lang.id) { content() }
}
