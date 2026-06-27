# UAS-Pemrograman-Mobile-2-Semester-4
## Tugasku - Aplikasi Manajemen Tugas Mahasiswa Terintegrasi AI

**Tugasku** adalah aplikasi *mobile* berbasis Android yang dirancang secara khusus untuk membantu mahasiswa dalam mengelola, mencatat, dan memantau tugas-tugas perkuliahan agar lebih terorganisir dan efisien. Aplikasi ini dikembangkan menggunakan **Java** melalui Android Studio, dan mengusung fitur unggulan yaitu **peringkasan *voice-to-text*** (suara ke teks) yang terintegrasi langsung dengan **Google Gemini AI**. Fitur ini memungkinkan pengguna untuk merekam penjelasan tugas dari dosen atau mencatat ide secara cepat melalui suara, yang kemudian akan diubah menjadi teks dan diringkas secara otomatis oleh AI.

---

### 👨‍💻 Informasi Mahasiswa
| Identitas | Keterangan |
| :--- | :--- |
| **Nama** | Ro'uf Muhammad Fauzan |
| **NIM** | 312410157 |
| **Kelas** | I.24.1A |
| **Universitas** | Universitas Pelita Bangsa |
| **Mata Kuliah** | Pemrograman Mobile 2 |
| **Dosen Pengampu** | Donny Maulana, S.Kom., M.M.S.I. |
| **Demo Aplikasi** | [Tonton Video Demo di YouTube]() |
| **Manajemen Proyek**| [Lihat Project di ClickUp](https://sharing.clickup.com/90181759596/b/h/6-901812361216-2/fa74978ca2faf70) |

---

### ✨ Fitur Utama
1. **Manajemen Tugas Komprehensif**
   Tambah, edit, hapus, dan lihat detail jadwal tugas perkuliahan dengan mudah. Tugas dapat dikategorikan berdasarkan mata kuliah dan tenggat waktu (deadline).
2. **AI Voice-to-Text & Summarization (Google Gemini AI)**
   Rekam suara Anda dan biarkan AI mengubahnya menjadi teks (Transkripsi). Selain itu, fitur peringkasan cerdas dari Gemini AI akan langsung merangkum poin-poin penting dari rekaman Anda menjadi catatan tugas yang terstruktur.
3. **Pengingat (Reminders) & Notifikasi**
   Aplikasi dilengkapi sistem notifikasi berbasis alarm (pengingat lokal) untuk mengingatkan tenggat waktu tugas agar pengguna tidak terlambat mengumpulkan tugas.
4. **Lampiran & Gambar**
   Tambahkan file lampiran atau gambar ke dalam catatan tugas (misalnya foto papan tulis atau dokumen tugas).
5. **Dukungan Tema Mode Gelap & Terang**
   Antarmuka mendukung pergantian tema otomatis maupun manual (Dark Mode & Light Mode) untuk kenyamanan mata pengguna.
6. **Penyimpanan Lokal Mandiri**
   Data tugas dan catatan disimpan secara aman di perangkat lokal menggunakan mekanisme `SharedPreferences` berbasis format JSON, sehingga cepat diakses dan ringan tanpa memerlukan database eksternal.

---

### 📱 User Interface (UI)
Berikut adalah gambaran antarmuka (UI) dari aplikasi **Tugasku** yang dirancang dengan **Figma** dan diimplementasikan secara langsung di Android Studio dengan prinsip Material Design:

*(Catatan: Silakan ganti URL gambar di bawah ini dengan screenshot UI aplikasi yang sudah Anda unggah ke repositori GitHub)*

* **Splash Screen & Login:**
<img width="200" height="400" alt="Screenshot_2026-06-27-20-02-52-596_com example tugasku" src="https://github.com/user-attachments/assets/d4315ce2-a380-418e-95ea-95b391a82bf3" />

* **Dashboard / Halaman Utama Tugas:**
<img width="200" height="400" alt="Screenshot_2026-06-27-20-03-58-937_com example tugasku" src="https://github.com/user-attachments/assets/55216a3f-8df5-4317-b61e-1c8bc3a4e6a2" />

* **Halaman Rekaman Voice-to-Text (AI):**
<img width="200" height="400" alt="Screenshot_2026-06-27-20-04-08-349_com example tugasku" src="https://github.com/user-attachments/assets/f418435f-5947-4e70-86a8-dd132fe26281" />


---

### 💻 Teknologi & Arsitektur
* **Platform:** Android
* **Bahasa Pemrograman:** Java
* **Integrated Development Environment (IDE):** Android Studio
* **Desain UI/UX:** Figma, Material Components XML
* **Integrasi AI:** Google Gemini AI API (`OkHttp3` untuk koneksi jaringan)
* **Penyimpanan Data Lokal:** `SharedPreferences` (JSON Based Data Storage)
* **Sistem Latar Belakang & Notifikasi:** `WorkManager`, `AlarmManager`, dan `BroadcastReceiver`

---

### 🚀 Cara Menjalankan Aplikasi
1. **Clone repositori ini** ke mesin lokal Anda menggunakan terminal atau Git Bash:
   ```bash
   git clone https://github.com/roouf/UAS-Pemrograman-Mobile-2-Semester-4.git
   ```
   *(Silakan sesuaikan URL repo di atas dengan URL GitHub Anda yang sebenarnya).*
2. **Buka Proyek di Android Studio:**
   Buka Android Studio, pilih `File > Open`, dan cari folder repositori yang baru saja di-*clone* (pilih root folder `UAS-Pemrograman-Mobile-2-Semester-4` atau folder app `Tugasku` di dalamnya).
3. **Konfigurasi API Key (Opsional / Jika Diperlukan):**
   Aplikasi ini memerlukan Google Gemini API Key. Key *default* telah diletakkan dalam variabel konfigurasi build proyek. Jika Anda ingin menggunakan key milik sendiri, Anda dapat mengedit variabel `GEMINI_API_KEY` di `Tugasku/app/build.gradle.kts`.
4. **Sinkronisasi Gradle:**
   Tunggu hingga proses sinkronisasi Gradle (Gradle Sync) selesai dan pastikan Anda terhubung ke internet yang stabil agar Android Studio dapat mengunduh seluruh dependensi aplikasi (OkHttp, WorkManager, dll).
5. **Jalankan Aplikasi:**
   Pilih perangkat Android (fisik ataupun emulator) yang ditargetkan dan tekan tombol **Run** (`Shift + F10`). 
   > **Penting:** Pastikan Anda menggunakan perangkat Android asli atau Android Virtual Device (AVD). Aplikasi Android tidak dapat dijalankan langsung di atas emulator/simulator iOS (seperti iPhone).
