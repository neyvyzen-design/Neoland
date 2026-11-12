<div align="center">

# 🏝️ Dynamic Island untuk Android

**Pulau dinamis bergaya iPhone untuk ponsel Android.**

Notifikasi · musik · panggilan · timer · navigasi · pengisian daya · peristiwa sistem

[![Versi 1.6.9](https://img.shields.io/badge/versi-1.6.9-8B80F9?style=for-the-badge)](https://github.com/neyvyzen-design/dynamic-island/releases/latest)
[![Build](https://img.shields.io/github/actions/workflow/status/neyvyzen-design/dynamic-island/android.yml?style=for-the-badge&label=build)](https://github.com/neyvyzen-design/dynamic-island/actions)
![Android 8+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat-square&logo=android)

[⬇️ Unduh APK terbaru](https://github.com/neyvyzen-design/dynamic-island/releases/latest) · [Catatan perubahan](CHANGELOG.md)

</div>

## Pembaruan 1.6.9

- **Profil Layar Utama dan Layar Kunci benar-benar terpisah.** Keduanya menyimpan posisi horizontal, jarak dari atas, lebar, tinggi, dan mode poni kamera secara independen.
- Profil berpindah otomatis mengikuti status layar kunci Android.
- Pengaturan lama dari versi 1.6.8 dipakai sebagai nilai awal kedua profil agar posisi island tidak berubah setelah pembaruan.
- Tombol atur ulang hanya mengatur ulang profil yang sedang dipilih.

## Fitur

- Kapsul dinamis untuk musik, panggilan, timer, navigasi, notifikasi, dan aktivitas sistem.
- Tampilan ringkas dan tampilan diperluas.
- Animasi dan efek Liquid Glass.
- Pengaturan ukuran dan posisi.
- Bahasa Indonesia pada antarmuka yang sudah diterjemahkan.

## Cara memasang

1. Unduh `DynamicIsland.apk` dari [halaman Rilis](https://github.com/neyvyzen-design/dynamic-island/releases/latest).
2. Buka APK dan pilih **Perbarui** jika Android menawarkannya.
3. Jangan hapus aplikasi lama jika ingin mempertahankan data. Versi 1.6.9 menggunakan `applicationId` yang sama dan `versionCode` lebih tinggi dari 1.6.8.

## Cara mengatur profil

Buka **Tampilan → Posisi dan Ukuran**. Pilih tab **Layar Utama** untuk mengatur tampilan saat ponsel terbuka, atau **Layar Kunci** untuk mengatur tampilan saat ponsel terkunci. Ubah posisi, jarak atas, lebar, tinggi, atau mode poni kamera; perubahan tersimpan pada profil yang dipilih saja.

## Pengembangan

Proyek menggunakan Kotlin, Jetpack Compose, Gradle, dan Android SDK 35.

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

Setiap push ke cabang utama menjalankan GitHub Actions. Rilis otomatis dibuat berdasarkan `versionName` pada `app/build.gradle.kts`.

Repositori: https://github.com/neyvyzen-design/dynamic-island
