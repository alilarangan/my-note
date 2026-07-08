MyNote Aplikasi Pencatat Belanja & Deposit

MyNote merupakan aplikasi Android untuk mencatat daftar belanjaan beserta harga secara cepat, menghitung total otomatis, dan mengelola deposit uang per orang.

Fitur Utama
1. Catatan Belanja (Halaman Utama)
   - Input teks bebas dengan format sederhana - tulis nama item dan harga, total langsung dihitung otomatis
   - Mendukung format penulisan harga:
     - Nasi goreng - 15000 → harga satuan
     - Nasi goreng - 15000 x 3 → harga × jumlah
     - 3 x 15000 → jumlah × harga
   - Format angka otomatis - ketik 15000 langsung menjadi 15.000
   - Tombol +000 untuk menambah tiga nol di posisi kursor secara cepat
   - Total belanja ditampilkan real-time di bagian bawah
2. Share / Kirim WA
   - Bagikan catatan belanja via share sheet Android (WhatsApp, Telegram, dll)
   - Format pesan rapi dengan daftar item, qty × harga, dan total
3. Simpan ke Deposit
   - Simpan catatan belanja langsung ke deposit orang tertentu
   - Bisa pilih deposit yang sudah ada atau buat deposit baru
   - Item belanja otomatis ter-parse dengan qty dan harga satuan terpisah
4. Cetak Struk
   - Generate PDF struk/nota format toko klasik
   - Ukuran A4 landscape, konten nota di sisi kiri (lebar 10cm)
   - Tabel berisi kolom: Banyaknya, Nama Barang, Harga, Jumlah
   - Minimal 13 baris tabel, tinggi dinamis sesuai jumlah item
   - PDF bisa langsung di-print atau dibagikan via share sheet
5. Manajemen Deposit
   - Simpan data deposit per orang dengan nama dan jumlah deposit awal
   - Setiap orang punya saldo yang berkurang otomatis saat belanja dicatat
   - Data tersimpan permanen di penyimpanan lokal HP (SharedPreferences)

Halaman Daftar Deposit:
- Lihat semua orang beserta sisa saldo masing-masing
- Ringkasan total deposit dan total sisa di bagian atas
- Kirim laporan belanja per orang langsung ke WA
  
Halaman Detail Deposit:
- Lihat daftar belanja lengkap per orang
- Setiap item tampil dengan format: 3 x Rp 15.000 = Rp 45.000
- Edit nama, jumlah deposit, qty, harga satuan per item
- Hapus item belanja — saldo otomatis dikembalikan
- Tambah item belanja baru dengan form qty × harga, total tampil real-time
  
Teknologi
- Bahasa: Java
- Platform: Android (min SDK 30 / Android 11, target SDK 35)
- Penyimpanan: SharedPreferences (JSON)
- PDF: Android native PdfDocument + Canvas drawing
- UI: Material Components, ConstraintLayout, CardView, RecyclerView
- Dark mode: Otomatis mengikuti mode sistem HP
  
Cocok Digunakan Untuk
- Pedagang warung/kantin yang mencatat pesanan harian
- Koordinator arisan atau patungan makanan
- Siapa saja yang perlu mencatat belanja dan mengelola titipan uang

<img width="720" height="1600" alt="Screenshot_20260708_122253_MyNote" src="https://github.com/user-attachments/assets/d06b8fd4-3440-4f7a-bffe-363d365fcb47" />

     
