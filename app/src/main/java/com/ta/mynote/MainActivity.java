package com.ta.mynote;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    EditText inputData;
    TextView hasil;
    ImageButton btnClear, btnShare, btnDeposit;
    Button btnSimpanDeposit;
    ImageButton btnCetakStruk;

    private boolean isFormatting = false;
    private String totalTerakhir = "Rp 0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputData        = findViewById(R.id.input);
        hasil            = findViewById(R.id.txtTotal);
        btnClear         = findViewById(R.id.btnClear);
        btnShare         = findViewById(R.id.btnShare);
        btnDeposit       = findViewById(R.id.btnDeposit);
        btnSimpanDeposit = findViewById(R.id.btnSimpanDeposit);

        inputData.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;
                isFormatting = true;

                String text      = s.toString();
                String formatted = formatSemuaHarga(text);

                if (!formatted.equals(text)) {
                    int cursorPos = inputData.getSelectionEnd();
                    int selisih   = formatted.length() - text.length();
                    s.replace(0, s.length(), formatted);
                    int newCursor = Math.max(0, Math.min(cursorPos + selisih, formatted.length()));
                    inputData.setSelection(newCursor);
                }

                hitungTotal(formatted);
                isFormatting = false;
            }
        });

        btnClear.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Bersihkan Data")
                        .setMessage("Yakin mau hapus semua?")
                        .setPositiveButton("Ya", (dialog, which) -> bersihkanData())
                        .setNegativeButton("Batal", null)
                        .show()
        );

        btnShare.setOnClickListener(v -> bagikanCatatan());
        btnDeposit.setOnClickListener(v -> startActivity(new Intent(this, DepositActivity.class)));
        btnSimpanDeposit.setOnClickListener(v -> simpanKeDeposit());

        // Tombol +000 langsung di layout
        Button btnTambahNol = findViewById(R.id.btnTambahNol);
        btnTambahNol.setOnClickListener(v -> tambahTigaNol());

        // Tombol cetak struk
        btnCetakStruk = findViewById(R.id.btnCetakStruk);
        btnCetakStruk.setOnClickListener(v -> cetakStruk());
    }

    // ─────────────────────────────────────────────────────
    // CETAK STRUK
    // Pilih deposit → generate PDF → buka share/print sheet
    // ─────────────────────────────────────────────────────
    private void cetakStruk() {
        SharedPrefHelper prefHelper     = new SharedPrefHelper(this);
        List<DepositModel> semuaDeposit = prefHelper.bacaSemuaDeposit();

        if (semuaDeposit.isEmpty()) {
            Toast.makeText(this, "Belum ada data deposit untuk dicetak!", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] pilihanNama = new String[semuaDeposit.size()];
        for (int i = 0; i < semuaDeposit.size(); i++) {
            DepositModel dm = semuaDeposit.get(i);
            int jumlahItem = dm.getRiwayat().size();
            pilihanNama[i] = dm.getNama() + "  (" + jumlahItem + " item)";
        }

        new AlertDialog.Builder(this)
                .setTitle("Cetak struk untuk siapa?")
                .setItems(pilihanNama, (dialog, which) -> {
                    DepositModel dipilih = semuaDeposit.get(which);
                    if (dipilih.getRiwayat().isEmpty()) {
                        Toast.makeText(this, "Belum ada item belanja untuk " + dipilih.getNama(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    generateDanBukaStruk(dipilih);
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void generateDanBukaStruk(DepositModel deposit) {
        try {
            File pdfFile = StrukPdfHelper.generateStrukPdf(this, deposit);

            // Dapatkan URI lewat FileProvider agar bisa dibuka app lain
            android.net.Uri uri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    pdfFile
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Cetak / Bagikan Struk"));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Gagal membuat struk: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ── Tambah 3 nol di posisi kursor ──
    private void tambahTigaNol() {
        int cursorPos = inputData.getSelectionEnd();
        if (cursorPos < 0) cursorPos = inputData.getText().length();

        String teks = inputData.getText().toString();

        // Cek apakah karakter sebelum kursor adalah angka
        if (cursorPos > 0 && Character.isDigit(teks.charAt(cursorPos - 1))) {
            // Sisipkan "000" di posisi kursor
            String baru = teks.substring(0, cursorPos) + "000" + teks.substring(cursorPos);
            isFormatting = true;
            inputData.setText(baru);
            inputData.setSelection(Math.min(cursorPos + 3, baru.length()));
            isFormatting = false;

            // Trigger format dan hitung ulang
            String formatted = formatSemuaHarga(baru);
            if (!formatted.equals(baru)) {
                isFormatting = true;
                inputData.setText(formatted);
                // Kursor ikut selisih penambahan titik
                int selisih = formatted.length() - baru.length();
                int newCursor = Math.min(cursorPos + 3 + selisih, formatted.length());
                inputData.setSelection(newCursor);
                isFormatting = false;
            }
            hitungTotal(inputData.getText().toString());
        } else {
            Toast.makeText(this, "Posisikan kursor setelah angka", Toast.LENGTH_SHORT).show();
        }
    }

    // ─────────────────────────────────────────────────────
    // SIMPAN KE DEPOSIT
    // ─────────────────────────────────────────────────────
    private void simpanKeDeposit() {
        String teks = inputData.getText().toString().trim();
        if (teks.isEmpty()) {
            Toast.makeText(this, "Catatan masih kosong!", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] semuaBaris = teks.split("\n", -1);
        int jumlahItem = 0;
        for (String b : semuaBaris) {
            if (parseHargaDariBaris(b.trim()) > 0) jumlahItem++;
        }

        if (jumlahItem == 0) {
            Toast.makeText(this, "Tidak ada item dengan harga yang bisa disimpan!", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPrefHelper prefHelper     = new SharedPrefHelper(this);
        List<DepositModel> semuaDeposit = prefHelper.bacaSemuaDeposit();

        String[] pilihanNama = new String[semuaDeposit.size() + 1];
        for (int i = 0; i < semuaDeposit.size(); i++) {
            DepositModel dm = semuaDeposit.get(i);
            pilihanNama[i]  = dm.getNama() + "  (sisa: " + formatRupiah(dm.getSisaSaldo()) + ")";
        }
        pilihanNama[semuaDeposit.size()] = "+ Buat Deposit Baru";

        new AlertDialog.Builder(this)
                .setTitle("Simpan ke deposit mana?")
                .setItems(pilihanNama, (dialog, which) -> {
                    if (which == semuaDeposit.size()) {
                        showDialogBuatDepositBaru(semuaBaris, prefHelper, semuaDeposit);
                    } else {
                        konfirmasiDanSimpan(semuaDeposit.get(which), semuaBaris, prefHelper, semuaDeposit);
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showDialogBuatDepositBaru(
            String[] semuaBaris,
            SharedPrefHelper prefHelper,
            List<DepositModel> semuaDeposit) {

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_tambah_deposit);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        EditText etNama   = dialog.findViewById(R.id.etNama);
        EditText etJumlah = dialog.findViewById(R.id.etJumlah);
        Button btnSimpan  = dialog.findViewById(R.id.btnSimpan);
        Button btnBatal   = dialog.findViewById(R.id.btnBatal);

        btnSimpan.setOnClickListener(v -> {
            String nama   = etNama.getText().toString().trim();
            String jumlah = etJumlah.getText().toString().trim().replace(".", "");

            if (TextUtils.isEmpty(nama))   { etNama.setError("Nama tidak boleh kosong"); return; }
            if (TextUtils.isEmpty(jumlah)) { etJumlah.setError("Jumlah tidak boleh kosong"); return; }

            long depositAwal = Long.parseLong(jumlah);
            DepositModel depositBaru = new DepositModel(UUID.randomUUID().toString(), nama, depositAwal);
            semuaDeposit.add(depositBaru);
            masukkanItemKe(depositBaru, semuaBaris, prefHelper, semuaDeposit);
            dialog.dismiss();
        });

        btnBatal.setOnClickListener(v -> dialog.dismiss());
        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private void konfirmasiDanSimpan(
            DepositModel dipilih,
            String[] semuaBaris,
            SharedPrefHelper prefHelper,
            List<DepositModel> semuaDeposit) {

        long totalBaru = 0;
        for (String b : semuaBaris) totalBaru += parseHargaDariBaris(b.trim());

        String pesan = "Tambahkan catatan ke deposit milik\n*" + dipilih.getNama() + "*?\n\n"
                + "Total belanja: " + formatRupiah(totalBaru) + "\n"
                + "Sisa saldo saat ini: " + formatRupiah(dipilih.getSisaSaldo());

        new AlertDialog.Builder(this)
                .setTitle("Konfirmasi")
                .setMessage(pesan)
                .setPositiveButton("Simpan", (d, w) ->
                        masukkanItemKe(dipilih, semuaBaris, prefHelper, semuaDeposit))
                .setNegativeButton("Batal", null)
                .show();
    }

    private void masukkanItemKe(
            DepositModel target,
            String[] semuaBaris,
            SharedPrefHelper prefHelper,
            List<DepositModel> semuaDeposit) {

        String tanggal = new SimpleDateFormat("dd/MM/yyyy HH:mm",
                new Locale("in", "ID")).format(new Date());

        long totalBelanja = 0;
        int  itemMasuk    = 0;

        for (String line : semuaBaris) {
            line = line.trim();
            if (line.isEmpty()) continue;

            long[] qtyHarga = parseQtyHargaDariBaris(line);
            if (qtyHarga != null) {
                long qty   = qtyHarga[0];
                long harga = qtyHarga[1];
                String namaItem = extractNamaItem(line);
                target.tambahRiwayat(new DepositModel.RiwayatBelanja(namaItem, qty, harga, tanggal));
                totalBelanja += qty * harga;
                itemMasuk++;
            }
        }

        if (itemMasuk == 0) {
            Toast.makeText(this, "Tidak ada item dengan harga yang bisa disimpan!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (totalBelanja > target.getSisaSaldo()) {
            Toast.makeText(this,
                    "⚠ Total belanja melebihi sisa saldo " + target.getNama() + "!",
                    Toast.LENGTH_LONG).show();
        }

        prefHelper.simpanSemuaDeposit(semuaDeposit);
        Toast.makeText(this,
                itemMasuk + " item berhasil disimpan ke deposit " + target.getNama() + "!",
                Toast.LENGTH_SHORT).show();

        new AlertDialog.Builder(this)
                .setTitle("Tersimpan!")
                .setMessage("Mau buka halaman deposit sekarang?")
                .setPositiveButton("Buka", (d, w) ->
                        startActivity(new Intent(this, DepositActivity.class)))
                .setNegativeButton("Nanti", null)
                .show();
    }

    private long[] parseQtyHargaDariBaris(String line) {
        String lineBersih = line.toLowerCase().replace(".", "");
        try {
            Pattern pHargaXQty = Pattern.compile("-(\\s*[0-9]+)\\s*x\\s*([0-9]{1,3})\\s*$");
            Matcher m1 = pHargaXQty.matcher(lineBersih);
            if (m1.find()) {
                long harga = Long.parseLong(m1.group(1).trim());
                long qty   = Long.parseLong(m1.group(2).trim());
                return new long[]{qty, harga};
            }

            if (lineBersih.matches("^[^a-z\\-]*[0-9]+\\s*x\\s*[0-9]+.*$")) {
                Pattern pQtyXHarga = Pattern.compile("([0-9]+)\\s*x\\s*([0-9]+)");
                Matcher m = pQtyXHarga.matcher(lineBersih);
                long qty = 0, harga = 0;
                while (m.find()) {
                    qty   = Long.parseLong(m.group(1));
                    harga = Long.parseLong(m.group(2));
                }
                if (qty > 0 && harga > 0) return new long[]{qty, harga};
            }

            if (lineBersih.contains("-")) {
                int lastDash = lineBersih.lastIndexOf("-");
                String angka = lineBersih.substring(lastDash + 1).trim().replaceAll("[^0-9]", "");
                if (!angka.isEmpty()) return new long[]{1, Long.parseLong(angka)};
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private long parseHargaDariBaris(String line) {
        long[] result = parseQtyHargaDariBaris(line);
        if (result == null) return 0;
        return result[0] * result[1];
    }

    private String extractNamaItem(String line) {
        int dashIdx = line.lastIndexOf("-");
        if (dashIdx > 0) {
            String nama = line.substring(0, dashIdx).trim();
            if (!nama.isEmpty()) return nama;
        }
        return line.trim();
    }

    private void bagikanCatatan() {
        String isiCatatan = inputData.getText().toString().trim();
        if (isiCatatan.isEmpty()) {
            Toast.makeText(this, "Catatan masih kosong!", Toast.LENGTH_SHORT).show();
            return;
        }

        String waktu = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("in", "ID")).format(new Date());
        StringBuilder sb = new StringBuilder();
        sb.append("*List Belanja & Harga*\n");
        sb.append(waktu).append("\n");
        sb.append("─────────────────\n");

        String[] baris = isiCatatan.split("\n", -1);
        for (String line : baris) {
            line = line.trim();
            if (line.isEmpty()) continue;

            long[] qtyHarga = parseQtyHargaDariBaris(line);
            if (qtyHarga != null) {
                long qty   = qtyHarga[0];
                long harga = qtyHarga[1];
                long total = qty * harga;
                String nama = extractNamaItem(line);
                sb.append("- ").append(nama)
                        .append(" (").append(qty).append(" x ").append(formatRupiah(harga)).append(")")
                        .append(" → ").append(formatRupiah(total)).append("\n");
            } else {
                sb.append(line).append("\n");
            }
        }

        sb.append("─────────────────\n");
        sb.append("*Total : ").append(totalTerakhir).append("*");

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(shareIntent, "Bagikan via..."));
    }

    private String formatSemuaHarga(String text) {
        String[] lines = text.split("\n", -1);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            result.append(formatHargaDalamBaris(lines[i]));
            if (i < lines.length - 1) result.append("\n");
        }
        return result.toString();
    }

    private String formatHargaDalamBaris(String line) {
        Pattern p1 = Pattern.compile("(-\\s*)([0-9][0-9.]*)(\\s*x\\s*)([0-9]{1,3})\\s*$");
        Matcher m1 = p1.matcher(line);
        if (m1.find()) {
            return line.substring(0, m1.start()) + m1.group(1)
                    + formatAngka(m1.group(2).replace(".", ""))
                    + " x " + m1.group(4).replace(".", "");
        }

        Pattern p2 = Pattern.compile("(-\\s*)([0-9][0-9.]*)\\s*$");
        Matcher m2 = p2.matcher(line);
        if (m2.find()) {
            return line.substring(0, m2.start()) + m2.group(1)
                    + formatAngka(m2.group(2).replace(".", ""));
        }

        Pattern p3 = Pattern.compile("^([^-]*)([0-9]{1,3})(\\s*x\\s*)([0-9][0-9.]*)\\s*$");
        Matcher m3 = p3.matcher(line);
        if (m3.find()) {
            return m3.group(1) + m3.group(2) + " x "
                    + formatAngka(m3.group(4).replace(".", ""));
        }

        return line;
    }

    private String formatAngka(String angka) {
        if (angka.isEmpty()) return angka;
        try {
            long nilai = Long.parseLong(angka);
            if (nilai < 1000) return angka;
            return NumberFormat.getNumberInstance(new Locale("in", "ID")).format(nilai);
        } catch (NumberFormatException e) {
            return angka;
        }
    }

    private void hitungTotal(String text) {
        String[] lines = text.split("\n");
        long total = 0;

        for (String line : lines) {
            line = line.toLowerCase().trim();
            if (line.isEmpty()) continue;

            long subtotal = 0;
            try {
                String lineBersih = line.replace(".", "");

                Pattern pXAkhir = Pattern.compile("-(\\s*[0-9]+)\\s*x\\s*([0-9]{1,3})\\s*$");
                Matcher mX = pXAkhir.matcher(lineBersih);

                if (mX.find()) {
                    subtotal = Long.parseLong(mX.group(1).trim()) * Long.parseLong(mX.group(2).trim());
                } else if (lineBersih.matches("^[^a-z]*[0-9]+\\s*x\\s*[0-9]+.*$")) {
                    Pattern pX = Pattern.compile("([0-9]+)\\s*x\\s*([0-9]+)");
                    Matcher m  = pX.matcher(lineBersih);
                    long qty = 0, harga = 0;
                    while (m.find()) {
                        qty   = Long.parseLong(m.group(1));
                        harga = Long.parseLong(m.group(2));
                    }
                    subtotal = qty * harga;
                } else if (lineBersih.contains("-")) {
                    int lastDash = lineBersih.lastIndexOf("-");
                    String angka = lineBersih.substring(lastDash + 1).trim().replaceAll("[^0-9]", "");
                    if (!angka.isEmpty()) subtotal = Long.parseLong(angka);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            total += subtotal;
        }

        totalTerakhir = formatRupiah(total);
        hasil.setText(totalTerakhir);
    }

    private String formatRupiah(long angka) {
        return "Rp " + NumberFormat.getNumberInstance(new Locale("in", "ID")).format(angka);
    }

    private void bersihkanData() {
        inputData.setText("");
        totalTerakhir = "Rp 0";
        hasil.setText(totalTerakhir);
    }
}