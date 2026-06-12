# HomeCraft - Furniture E-Commerce

HomeCraft adalah aplikasi e-commerce Android untuk perabotan rumah tangga. Aplikasi ini dirancang untuk memenuhi spesifikasi teknis pengembangan aplikasi mobile tingkat lanjut, mencakup integrasi API, basis data lokal, navigasi modern, dan antarmuka yang responsif.

---

## 📱 Deskripsi Aplikasi

Aplikasi ini mengambil data produk dari **Kohl's API** (RapidAPI) dan menyimpannya ke dalam basis data lokal untuk mendukung fungsionalitas secara offline. Pengguna dapat mendaftar akun, melihat daftar produk, menambahkan item ke keranjang belanja, melakukan proses *checkout* dengan penentuan alamat via GPS (OpenStreetMap), serta melacak status pengiriman melalui peta.

### Fitur Utama
- **Katalog Produk:** Menampilkan daftar produk yang ditarik langsung dari API.
- **Keranjang & Checkout:** Manajemen pesanan dengan simulasi pengiriman dan metode pembayaran.
- **Dukungan Multi-Bahasa:** Dapat diubah antara bahasa Indonesia dan Inggris.
- **Tema Dinamis:** Mendukung *Light Mode* (Terang) dan *Dark Mode* (Gelap).
- **Keamanan:** Penyimpanan *password* lokal menggunakan sistem enkripsi *hash* SHA-256.

---

## 🚀 Cara Penggunaan

1. **Instalasi & Menjalankan Aplikasi:** 
   Buka proyek ini di Android Studio, pastikan sinkronisasi Gradle berhasil, lalu tekan tombol **Run (▶)** untuk menjalankan aplikasi pada Emulator atau perangkat Android fisik.
2. **Registrasi & Login:** 
   Pada halaman awal, buat akun baru melalui menu **Register**. Setelah berhasil, lakukan **Login** menggunakan kredensial yang dibuat.
3. **Melihat Produk:**
   Di halaman utama (Home), telusuri daftar produk yang ada. Tarik layar ke bawah (*Swipe-to-Refresh*) untuk memaksa pembaruan data dari server API.
4. **Melakukan Pesanan:**
   Pilih produk yang disukai, tambahkan ke keranjang, dan buka halaman keranjang (Cart). Lanjutkan ke halaman **Checkout** untuk mengatur alamat pengiriman (bisa menggunakan GPS) dan melakukan konfirmasi.
5. **Melacak Pengiriman:**
   Setelah pembayaran terkonfirmasi, status dan rute pengiriman paket dapat dipantau langsung dari menu pelacakan pesanan.
6. **Pengaturan Profil:**
   Buka menu **Settings** untuk mengganti tema atau merubah bahasa antarmuka aplikasi.

---

## 🛠️ Implementasi Teknis

Aplikasi ini dibangun untuk memenuhi ketujuh kriteria spesifikasi teknis berikut:

### 1. Activity & Intent
- **Activity:** Proyek ini terdiri dari beberapa Activity terpisah (`SplashActivity`, `MainActivity`, `DetailActivity`, `CheckoutActivity`, dll). `SplashActivity` dikonfigurasi sebagai *Launcher* utama aplikasi.
- **Intent:** Penggunaan *Explicit Intent* diterapkan untuk navigasi antar Activity dan perpindahan parameter data (seperti ID Produk atau lokasi geografis).

### 2. RecyclerView
- Komponen `RecyclerView` digunakan sebagai fondasi utama untuk merender daftar item secara efisien, seperti pada katalog produk (`HomeFragment`), daftar keranjang (`CartFragment`), dan halaman kategori.

### 3. Fragment & Navigation Component
- Aplikasi memanfaatkan **Jetpack Navigation Component** (`NavHostFragment` dan `nav_graph.xml`) yang terhubung dengan `BottomNavigationView` di `MainActivity` untuk mengontrol perpindahan antar 5 Fragment secara terstruktur.

### 4. Background Thread
- **ExecutorService:** Digunakan untuk menjalankan seluruh kueri basis data SQLite (membaca/menyimpan data) di luar *UI Thread* untuk menjaga kelancaran aplikasi.
- **Handler:** Digunakan untuk mengontrol penjadwalan UI dan animasi.

### 5. Networking
- **Retrofit2:** Diimplementasikan untuk melakukan panggilan HTTP asinkron ke server Kohl's API.
- **Penanganan Kesalahan Jaringan:** Disediakan modul `SwipeRefreshLayout` dan tombol muat ulang ("Coba Lagi") sebagai tindak lanjut bila perangkat tidak memiliki akses internet.

### 6. Local Data Persistent
- **SQLite Database:** Dikelola via `SQLiteOpenHelper` untuk menyimpan data akun pengguna, riwayat keranjang belanja, serta *caching* respons API sehingga katalog tetap bisa diakses saat perangkat *offline*.
- **SharedPreferences:** Diterapkan untuk menyimpan data berukuran kecil namun persisten, seperti status sesi login pengguna dan preferensi pengaturan (Bahasa dan Tema).

### 7. Tema Aplikasi
- Menggunakan pendekatan standar sistem (melalui `AppCompatDelegate`) agar pengguna dapat berganti antara mode Terang dan Gelap (*Light/Dark Theme*) secara langsung dari dalam aplikasi.
