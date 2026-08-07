package com.ta.mynote;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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

    EditText    inputData;
    TextView    hasil;
    ImageButton btnClear, btnShare, btnDeposit, btnCetakStruk;
    Button      btnSimpanDeposit, btnTambahNol, btnTambahDash, btnTambahChecklis;

    private boolean isFormatting  = false;
    private String  totalTerakhir = "Rp 0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputData        = findViewById(R.id.input);
        hasil            = findViewById(R.id.txtTotal);
        btnClear         = findViewById(R.id.btnClear);
        btnShare         = findViewById(R.id.btnShare);
        btnDeposit       = findViewById(R.id.btnDeposit);
        btnCetakStruk    = findViewById(R.id.btnCetakStruk);
        btnSimpanDeposit = findViewById(R.id.btnSimpanDeposit);
        btnTambahNol     = findViewById(R.id.btnTambahNol);
        btnTambahDash    = findViewById(R.id.btnTambahDash);
        btnTambahChecklis = findViewById(R.id.btnTambahChecklist);

        // ── Auto lanjut checklist saat Enter di baris [ ] atau [x] ──
        inputData.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_ENTER &&
                    event.getAction() == android.view.KeyEvent.ACTION_DOWN) {
                int cursor = inputData.getSelectionStart();
                String text = inputData.getText().toString();
                int lineStart = text.lastIndexOf('\n', Math.max(0, cursor - 2));
                lineStart = (lineStart == -1) ? 0 : lineStart + 1;
                int lineEnd = cursor - 1;
                if (lineEnd < lineStart) lineEnd = lineStart;
                String currentLine = text.substring(lineStart, lineEnd);
                if (currentLine.startsWith("[ ]") || currentLine.startsWith("[x]") || currentLine.startsWith("[X]")) {
                    inputData.post(() -> {
                        int pos = inputData.getSelectionStart();
                        inputData.getText().insert(pos, "[ ] ");
                    });
                }
            }
            return false;
        });

        inputData.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;
                isFormatting = true;

                String text      = s.toString();
                String formatted = formatSemuaBaris(text);

                if (!formatted.equals(text)) {
                    int cursor  = inputData.getSelectionEnd();
                    int selisih = formatted.length() - text.length();
                    s.replace(0, s.length(), formatted);
                    int newCursor = Math.max(0, Math.min(cursor + selisih, formatted.length()));
                    inputData.setSelection(newCursor);
                }

                applyChecklistSpans(inputData.getText());
                hitungTotal(inputData.getText().toString());
                isFormatting = false;
            }
        });

        // Tap pada teks untuk toggle checkbox
        inputData.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                toggleCheckboxPadaTap(event.getX(), event.getY());
            }
            return false;
        });

        btnClear.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Bersihkan Data")
                        .setMessage("Yakin mau hapus semua?")
                        .setPositiveButton("Ya", (d, w) -> bersihkanData())
                        .setNegativeButton("Batal", null)
                        .show()
        );

        btnShare.setOnClickListener(v -> bagikanCatatan());
        btnDeposit.setOnClickListener(v -> startActivity(new Intent(this, DepositActivity.class)));
        btnCetakStruk.setOnClickListener(v -> cetakStruk());
        btnSimpanDeposit.setOnClickListener(v -> simpanKeDeposit());
        btnTambahNol.setOnClickListener(v -> tambahTigaNol());
        btnTambahDash.setOnClickListener(v -> sisipkanTeks(" - "));
        btnTambahChecklis.setOnClickListener(v -> tambahChecklist());
    }

    // ─────────────────────────────────────────────────────
    // CHECKLIST — SPANNABLE
    // ─────────────────────────────────────────────────────

    private void applyChecklistSpans(Spannable span) {
        if (span == null) return;
        String teks = span.toString();

        // Hapus span lama
        for (StrikethroughSpan s : span.getSpans(0, span.length(), StrikethroughSpan.class)) span.removeSpan(s);
        for (ForegroundColorSpan s : span.getSpans(0, span.length(), ForegroundColorSpan.class)) span.removeSpan(s);

        String[] baris = teks.split("\n", -1);
        int pos = 0;
        for (String bari : baris) {
            int end = pos + bari.length();
            if (bari.startsWith("[x]") || bari.startsWith("[X]")) {
                span.setSpan(new StrikethroughSpan(), pos, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                span.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.colorInputHint)),
                        pos, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            pos = end + 1;
        }
    }

    private void toggleCheckboxPadaTap(float tapX, float tapY) {
        if (inputData.getLayout() == null) return;
        int line = inputData.getLayout().getLineForVertical((int) (tapY - inputData.getTotalPaddingTop()));
        if (line < 0) return;

        int lineStart = inputData.getLayout().getLineStart(line);
        int lineEnd   = inputData.getLayout().getLineEnd(line);
        String teks   = inputData.getText().toString();
        if (lineStart >= teks.length()) return;

        String bari = teks.substring(lineStart, Math.min(lineEnd, teks.length()));

        float charWidth = inputData.getPaint().measureText("W");
        boolean tapDiAreaCheckbox = tapX < (inputData.getPaddingLeft() + charWidth * 2);
        if (!tapDiAreaCheckbox) return;

        String gantiDari, gantiKe;
        if (bari.startsWith("[ ]"))      { gantiDari = "[ ]"; gantiKe = "[x]"; }
        else if (bari.startsWith("[x]")) { gantiDari = "[x]"; gantiKe = "[ ]"; }
        else if (bari.startsWith("[X]")) { gantiDari = "[X]"; gantiKe = "[ ]"; }
        else return;

        isFormatting = true;
        String teksBaru = teks.substring(0, lineStart)
                + bari.replaceFirst(Pattern.quote(gantiDari), gantiKe)
                + teks.substring(Math.min(lineEnd, teks.length()));

        inputData.setText(teksBaru);
        inputData.setSelection(Math.min(lineStart + bari.length(), teksBaru.length()));
        applyChecklistSpans(inputData.getText());
        hitungTotal(teksBaru);
        isFormatting = false;
    }

    // ─────────────────────────────────────────────────────
    // FORMAT & HITUNG
    // ─────────────────────────────────────────────────────

    private String formatSemuaBaris(String text) {
        String[] lines = text.split("\n", -1);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            result.append(formatSatuBaris(lines[i]));
            if (i < lines.length - 1) result.append("\n");
        }
        return result.toString();
    }

    private String formatSatuBaris(String line) {
        String prefix = "", isi = line;
        if (line.startsWith("[ ] ") || line.startsWith("[x] ") || line.startsWith("[X] ")) {
            prefix = line.substring(0, 4); isi = line.substring(4);
        } else if (line.startsWith("[ ]") || line.startsWith("[x]") || line.startsWith("[X]")) {
            prefix = line.substring(0, 3); isi = line.substring(3);
        }
        return prefix + formatHargaDalamBaris(isi);
    }

    private String formatHargaDalamBaris(String line) {
        Matcher m1 = Pattern.compile("(-\\s*)([0-9][0-9.]*)(\\s*x\\s*)([0-9]{1,3})\\s*$").matcher(line);
        if (m1.find()) return line.substring(0, m1.start()) + m1.group(1)
                + formatAngka(m1.group(2).replace(".", "")) + " x " + m1.group(4);

        Matcher m2 = Pattern.compile("(-\\s*)([0-9][0-9.]*)\\s*$").matcher(line);
        if (m2.find()) return line.substring(0, m2.start()) + m2.group(1)
                + formatAngka(m2.group(2).replace(".", ""));

        Matcher m3 = Pattern.compile("^([^-]*)([0-9]{1,3})(\\s*x\\s*)([0-9][0-9.]*)\\s*$").matcher(line);
        if (m3.find()) return m3.group(1) + m3.group(2) + " x " + formatAngka(m3.group(4).replace(".", ""));

        return line;
    }

    private void hitungTotal(String text) {
        long total = 0;
        for (String line : text.split("\n")) {
            total += parseHargaDariBaris(stripChecklistPrefix(line.trim()));
        }
        totalTerakhir = formatRupiah(total);
        hasil.setText(totalTerakhir);
    }
    private void tambahTigaNol() {
        int cursor = inputData.getSelectionEnd();
        if (cursor < 0) cursor = inputData.getText().length();
        String teks = inputData.getText().toString();

        if (cursor > 0 && Character.isDigit(teks.charAt(cursor - 1))) {
            String baru = teks.substring(0, cursor) + "000" + teks.substring(cursor);
            isFormatting = true;
            String formatted = formatSemuaBaris(baru);
            inputData.setText(formatted);
            int selisih = formatted.length() - baru.length();
            inputData.setSelection(Math.min(cursor + 3 + selisih, formatted.length()));
            applyChecklistSpans(inputData.getText());
            hitungTotal(formatted);
            isFormatting = false;
        } else {
            Toast.makeText(this, "Posisikan kursor setelah angka", Toast.LENGTH_SHORT).show();
        }
    }

    private void sisipkanTeks(String tambahan) {
        String teks = inputData.getText().toString();
        int cursor = inputData.getSelectionStart();

        int akhirBaris = teks.indexOf('\n', cursor);

        if (akhirBaris == -1){
            akhirBaris = teks.length();
        }
        String baru = teks.substring(0, akhirBaris)+tambahan+teks.substring(akhirBaris);
        isFormatting = true;
        inputData.setText(baru);
        inputData.setSelection(akhirBaris + tambahan.length());
        applyChecklistSpans(inputData.getText());
        hitungTotal(baru);
        isFormatting = false;
    }

    private void tambahChecklist() {
        int cursor = inputData.getSelectionEnd();
        if (cursor < 0) cursor = inputData.getText().length();
        String teks = inputData.getText().toString();
        int awalBaris = teks.lastIndexOf('\n', cursor - 1) + 1;
        int akhirBaris = teks.indexOf('\n', cursor);
        if (akhirBaris < 0) akhirBaris = teks.length();
        String bari = teks.substring(awalBaris, akhirBaris);

        int cursorBaru = cursor;

        if (bari.startsWith("[ ] ")) {
            bari = bari.substring(4);
            cursorBaru = Math.max(awalBaris, cursor - 4);
        } else if (bari.startsWith("[x] ")) {
            bari = bari.substring(4);
            cursorBaru = Math.max(awalBaris, cursor - 4);
        } else if (bari.startsWith("[X] ")) {
            bari = bari.substring(4);
            cursorBaru = Math.max(awalBaris, cursor - 4);
        } else {
            bari = "[ ] " + bari;
            cursorBaru = Math.max(awalBaris, cursor + 4);
        }

        String baru = teks.substring(0, awalBaris) + bari + teks.substring(akhirBaris);
        isFormatting = true;
        inputData.setText(baru);
        inputData.setSelection(Math.min(cursorBaru, baru.length()));
        applyChecklistSpans(inputData.getText());
        hitungTotal(baru);
        isFormatting = false;
    }

    private void bersihkanData() {
        inputData.setText("");
        totalTerakhir = "Rp 0";
        hasil.setText(totalTerakhir);
    }

    private String getTeksIsi() {
        return inputData.getText().toString().trim();
    }

    private void bagikanCatatan() {
        String gabungan = getTeksIsi();
        if (gabungan.isEmpty()) {
            Toast.makeText(this, "Catatan masih kosong!", Toast.LENGTH_SHORT).show(); return;
        }

        String waktu = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("in", "ID")).format(new Date());
        StringBuilder sb = new StringBuilder();
        sb.append("*List Belanja & Harga*\n").append(waktu).append("\n─────────────────\n");

        for (String line : gabungan.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;

            String isi = line;
            if (line.startsWith("[x] ") || line.startsWith("[X] ")) {
                isi = line.substring(4);
            } else if (line.startsWith("[ ] ")) {
                isi = line.substring(4);
            } else if (line.startsWith("[x]") || line.startsWith("[X]")) {
                isi = line.substring(3);
            } else if (line.startsWith("[ ]")) {
                isi = line.substring(3);
            }

            long[] qh = parseQtyHargaDariBaris(isi);
            if (qh != null) {
                if (qh[0] == 1) {
                    sb.append("- ").append(extractNamaItem(isi))
                            .append(" → ").append(formatRupiah(qh[1])).append("\n");
                } else {
                    sb.append("- ").append(extractNamaItem(isi))
                            .append(" (").append(qh[0]).append(" x ").append(formatRupiah(qh[1])).append(")")
                            .append(" → ").append(formatRupiah(qh[0] * qh[1])).append("\n");
                }
            } else {
                sb.append(isi).append("\n");
            }
        }
        sb.append("─────────────────\n*Total : ").append(totalTerakhir).append("*");

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(intent, "Bagikan via..."));
    }

    private void simpanKeDeposit() {
        String gabungan = getTeksIsi();
        if (gabungan.isEmpty()) { Toast.makeText(this, "Catatan masih kosong!", Toast.LENGTH_SHORT).show(); return; }

        String[] semuaBaris = gabungan.split("\n", -1);
        int jumlahItem = 0;
        for (String b : semuaBaris) {
            if (parseHargaDariBaris(stripChecklistPrefix(b.trim())) > 0) jumlahItem++;
        }
        if (jumlahItem == 0) { Toast.makeText(this, "Tidak ada item dengan harga!", Toast.LENGTH_SHORT).show(); return; }

        SharedPrefHelper prefHelper     = new SharedPrefHelper(this);
        List<DepositModel> semuaDeposit = prefHelper.bacaSemuaDeposit();

        String[] pilihan = new String[semuaDeposit.size() + 1];
        for (int i = 0; i < semuaDeposit.size(); i++)
            pilihan[i] = semuaDeposit.get(i).getNama() + "  (sisa: " + formatRupiah(semuaDeposit.get(i).getSisaSaldo()) + ")";
        pilihan[semuaDeposit.size()] = "+ Buat Deposit Baru";

        new AlertDialog.Builder(this)
                .setTitle("Simpan ke deposit mana?")
                .setItems(pilihan, (dialog, which) -> {
                    if (which == semuaDeposit.size()) showDialogBuatDepositBaru(semuaBaris, prefHelper, semuaDeposit);
                    else konfirmasiDanSimpan(semuaDeposit.get(which), semuaBaris, prefHelper, semuaDeposit);
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showDialogBuatDepositBaru(String[] semuaBaris, SharedPrefHelper prefHelper, List<DepositModel> semuaDeposit) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_tambah_deposit);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        EditText etNama   = dialog.findViewById(R.id.etNama);
        EditText etJumlah = dialog.findViewById(R.id.etJumlah);
        Button btnS       = dialog.findViewById(R.id.btnSimpan);
        Button btnB       = dialog.findViewById(R.id.btnBatal);

        btnS.setOnClickListener(v -> {
            String nama   = etNama.getText().toString().trim();
            String jumlah = etJumlah.getText().toString().trim().replace(".", "");
            if (nama.isEmpty())   { etNama.setError("Nama tidak boleh kosong"); return; }
            if (jumlah.isEmpty()) { etJumlah.setError("Jumlah tidak boleh kosong"); return; }
            DepositModel baru = new DepositModel(UUID.randomUUID().toString(), nama, Long.parseLong(jumlah));
            semuaDeposit.add(baru);
            masukkanItemKe(baru, semuaBaris, prefHelper, semuaDeposit);
            dialog.dismiss();
        });
        btnB.setOnClickListener(v -> dialog.dismiss());
        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void konfirmasiDanSimpan(DepositModel dipilih, String[] semuaBaris, SharedPrefHelper prefHelper, List<DepositModel> semuaDeposit) {
        long total = 0;
        for (String b : semuaBaris) total += parseHargaDariBaris(stripChecklistPrefix(b.trim()));
        new AlertDialog.Builder(this)
                .setTitle("Konfirmasi")
                .setMessage("Tambahkan ke *" + dipilih.getNama() + "*?\n\nTotal: " + formatRupiah(total) + "\nSisa: " + formatRupiah(dipilih.getSisaSaldo()))
                .setPositiveButton("Simpan", (d, w) -> masukkanItemKe(dipilih, semuaBaris, prefHelper, semuaDeposit))
                .setNegativeButton("Batal", null).show();
    }

    private void masukkanItemKe(DepositModel target, String[] semuaBaris, SharedPrefHelper prefHelper, List<DepositModel> semuaDeposit) {
        String tanggal = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("in", "ID")).format(new Date());
        int itemMasuk = 0;
        long totalBelanja = 0;

        for (String line : semuaBaris) {
            String isi = stripChecklistPrefix(line.trim());
            if (isi.isEmpty()) continue;
            long[] qh = parseQtyHargaDariBaris(isi);
            if (qh != null) {
                target.tambahRiwayat(new DepositModel.RiwayatBelanja(extractNamaItem(isi), qh[0], qh[1], tanggal));
                totalBelanja += qh[0] * qh[1];
                itemMasuk++;
            }
        }

        if (itemMasuk == 0) { Toast.makeText(this, "Tidak ada item dengan harga!", Toast.LENGTH_SHORT).show(); return; }
        if (totalBelanja > target.getSisaSaldo()) Toast.makeText(this, "⚠ Total melebihi sisa saldo!", Toast.LENGTH_LONG).show();

        prefHelper.simpanSemuaDeposit(semuaDeposit);
        Toast.makeText(this, itemMasuk + " item tersimpan ke " + target.getNama(), Toast.LENGTH_SHORT).show();

        new AlertDialog.Builder(this)
                .setTitle("Tersimpan!")
                .setMessage("Mau buka halaman deposit?")
                .setPositiveButton("Buka", (d, w) -> startActivity(new Intent(this, DepositActivity.class)))
                .setNegativeButton("Nanti", null).show();
    }

    private void cetakStruk() {
        SharedPrefHelper prefHelper     = new SharedPrefHelper(this);
        List<DepositModel> semuaDeposit = prefHelper.bacaSemuaDeposit();
        if (semuaDeposit.isEmpty()) { Toast.makeText(this, "Belum ada data deposit!", Toast.LENGTH_SHORT).show(); return; }

        String[] pilihan = new String[semuaDeposit.size()];
        for (int i = 0; i < semuaDeposit.size(); i++)
            pilihan[i] = semuaDeposit.get(i).getNama() + "  (" + semuaDeposit.get(i).getRiwayat().size() + " item)";

        new AlertDialog.Builder(this).setTitle("Cetak struk untuk siapa?")
                .setItems(pilihan, (dialog, which) -> {
                    DepositModel d = semuaDeposit.get(which);
                    if (d.getRiwayat().isEmpty()) { Toast.makeText(this, "Belum ada item!", Toast.LENGTH_SHORT).show(); return; }
                    pilihTanggalCetak(d);
                }).setNegativeButton("Batal", null).show();
    }

    /** Dialog DatePicker untuk memilih tanggal cetak struk */
    private void pilihTanggalCetak(DepositModel deposit) {
        java.util.Calendar cal = java.util.Calendar.getInstance();

        android.app.DatePickerDialog datePicker = new android.app.DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    // Format tanggal yang dipilih
                    String tanggalDipilih = String.format(new Locale("in", "ID"),
                            "%02d/%02d/%04d", dayOfMonth, month + 1, year);
                    generateDanBukaStruk(deposit, tanggalDipilih);
                },
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH),
                cal.get(java.util.Calendar.DAY_OF_MONTH)
        );

        datePicker.setTitle("Pilih tanggal cetak");
        datePicker.show();
    }

    private void generateDanBukaStruk(DepositModel deposit, String tanggal) {
        try {
            File pdfFile = StrukPdfHelper.generateStrukPdf(this, deposit, tanggal);
            android.net.Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", pdfFile);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Cetak / Bagikan Struk"));
        } catch (Exception e) {
            Toast.makeText(this, "Gagal membuat struk: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ─────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────

    private String stripChecklistPrefix(String line) {
        if (line.startsWith("[ ] ") || line.startsWith("[x] ") || line.startsWith("[X] ")) return line.substring(4);
        if (line.startsWith("[ ]") || line.startsWith("[x]") || line.startsWith("[X]")) return line.substring(3);
        return line;
    }

    private long[] parseQtyHargaDariBaris(String line) {
        String b = line.toLowerCase().replace(".", "");
        try {
            Matcher m1 = Pattern.compile("-(\\s*[0-9]+)\\s*x\\s*([0-9]{1,3})\\s*$").matcher(b);
            if (m1.find()) return new long[]{ Long.parseLong(m1.group(2).trim()), Long.parseLong(m1.group(1).trim()) };

            if (b.matches("^[^a-z\\-]*[0-9]+\\s*x\\s*[0-9]+.*$")) {
                Matcher m = Pattern.compile("([0-9]+)\\s*x\\s*([0-9]+)").matcher(b);
                long qty = 0, harga = 0;
                while (m.find()) { qty = Long.parseLong(m.group(1)); harga = Long.parseLong(m.group(2)); }
                if (qty > 0 && harga > 0) return new long[]{qty, harga};
            }

            if (b.contains("-")) {
                int d = b.lastIndexOf("-");
                String angka = b.substring(d + 1).trim().replaceAll("[^0-9]", "");
                if (!angka.isEmpty()) return new long[]{1, Long.parseLong(angka)};
            }
        } catch (Exception ignored) {}
        return null;
    }

    private long parseHargaDariBaris(String line) {
        long[] r = parseQtyHargaDariBaris(line);
        return r == null ? 0 : r[0] * r[1];
    }

    private String extractNamaItem(String line) {
        int d = line.lastIndexOf("-");
        if (d > 0) { String n = line.substring(0, d).trim(); if (!n.isEmpty()) return n; }
        return line.trim();
    }

    private String formatAngka(String angka) {
        if (angka.isEmpty()) return angka;
        try {
            long n = Long.parseLong(angka);
            if (n < 1000) return angka;
            return NumberFormat.getNumberInstance(new Locale("in", "ID")).format(n);
        } catch (NumberFormatException e) { return angka; }
    }

    private String formatRupiah(long angka) {
        return "Rp " + NumberFormat.getNumberInstance(new Locale("in", "ID")).format(angka);
    }
}