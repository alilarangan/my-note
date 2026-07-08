package com.ta.mynote;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DetailDepositActivity extends AppCompatActivity {

    DepositModel depositModel;
    List<DepositModel> semuaDeposit;
    SharedPrefHelper prefHelper;

    TextView tvNamaDetail, tvDepositDetail, tvSisaDetail, tvTotalPakai;
    RecyclerView rvBelanja;
    BelanjaAdapter belanjaAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_deposit);

        prefHelper   = new SharedPrefHelper(this);
        semuaDeposit = prefHelper.bacaSemuaDeposit();

        String id = getIntent().getStringExtra(DepositActivity.EXTRA_DEPOSIT_ID);
        for (DepositModel dm : semuaDeposit) {
            if (dm.getId().equals(id)) { depositModel = dm; break; }
        }

        if (depositModel == null) { finish(); return; }

        tvNamaDetail    = findViewById(R.id.tvNamaDetail);
        tvDepositDetail = findViewById(R.id.tvDepositDetail);
        tvSisaDetail    = findViewById(R.id.tvSisaDetail);
        tvTotalPakai    = findViewById(R.id.tvTotalPakai);
        rvBelanja       = findViewById(R.id.rvBelanja);

        ImageButton btnBack       = findViewById(R.id.btnBackDetail);
        Button      btnEditHeader = findViewById(R.id.btnEditHeader);
        Button      btnTambah     = findViewById(R.id.btnTambahBelanja);

        belanjaAdapter = new BelanjaAdapter(depositModel.getRiwayat(),
                new BelanjaAdapter.OnBelanjaListener() {
                    @Override public void onEdit(int position)  { showDialogEditBelanja(position); }
                    @Override public void onHapus(int position) { hapusBelanja(position); }
                });
        rvBelanja.setLayoutManager(new LinearLayoutManager(this));
        rvBelanja.setAdapter(belanjaAdapter);

        btnBack.setOnClickListener(v -> finish());
        btnEditHeader.setOnClickListener(v -> showDialogEditHeader());
        btnTambah.setOnClickListener(v -> showDialogTambahBelanja());

        refreshUI();
    }
    private void refreshUI() {
        tvNamaDetail.setText(depositModel.getNama());
        tvDepositDetail.setText("Deposit awal: " + formatRupiah(depositModel.getDepositAwal()));
        tvSisaDetail.setText("Sisa: " + formatRupiah(depositModel.getSisaSaldo()));

        int colorSisa = depositModel.getSisaSaldo() <= 0
                ? getResources().getColor(R.color.colorError, null)
                : getResources().getColor(R.color.colorTotalValue, null);
        tvSisaDetail.setTextColor(colorSisa);

        long totalPakai = 0;
        for (DepositModel.RiwayatBelanja r : depositModel.getRiwayat()) totalPakai += r.getTotal();
        tvTotalPakai.setText("Total dipakai: " + formatRupiah(totalPakai));

        belanjaAdapter.notifyDataSetChanged();
    }

    private void simpanDanRefresh() {
        for (int i = 0; i < semuaDeposit.size(); i++) {
            if (semuaDeposit.get(i).getId().equals(depositModel.getId())) {
                semuaDeposit.set(i, depositModel);
                break;
            }
        }
        prefHelper.simpanSemuaDeposit(semuaDeposit);
        refreshUI();
    }

    private void showDialogEditHeader() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_edit_header);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        EditText etNama    = dialog.findViewById(R.id.etEditNama);
        EditText etDeposit = dialog.findViewById(R.id.etEditDeposit);
        Button btnSimpan   = dialog.findViewById(R.id.btnSimpanEdit);
        Button btnBatal    = dialog.findViewById(R.id.btnBatalEdit);

        etNama.setText(depositModel.getNama());
        etDeposit.setText(String.valueOf(depositModel.getDepositAwal()));

        btnSimpan.setOnClickListener(v -> {
            String nama    = etNama.getText().toString().trim();
            String deposit = etDeposit.getText().toString().trim().replace(".", "");

            if (TextUtils.isEmpty(nama))    { etNama.setError("Nama tidak boleh kosong"); return; }
            if (TextUtils.isEmpty(deposit)) { etDeposit.setError("Jumlah tidak boleh kosong"); return; }

            long depositBaru = Long.parseLong(deposit);
            long totalPakai  = 0;
            for (DepositModel.RiwayatBelanja r : depositModel.getRiwayat()) totalPakai += r.getTotal();

            DepositModel baru = new DepositModel(depositModel.getId(), nama, depositBaru);
            baru.setSisaSaldo(depositBaru - totalPakai);
            baru.setRiwayat(depositModel.getRiwayat());
            depositModel = baru;

            simpanDanRefresh();
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

    private void showDialogTambahBelanja() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_kurangi_saldo);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextView tvNamaDialog  = dialog.findViewById(R.id.tvNamaDialog);
        TextView tvSaldoInfo   = dialog.findViewById(R.id.tvSaldoInfo);
        TextView tvTotalDialog = dialog.findViewById(R.id.tvTotalDialog);
        EditText etKeterangan  = dialog.findViewById(R.id.etKeterangan);
        EditText etQty         = dialog.findViewById(R.id.etJumlahQty);
        EditText etHarga       = dialog.findViewById(R.id.etJumlahKurang);
        Button   btnSimpan     = dialog.findViewById(R.id.btnSimpanKurang);
        Button   btnBatal      = dialog.findViewById(R.id.btnBatalKurang);

        tvNamaDialog.setText(depositModel.getNama());
        tvSaldoInfo.setText("Sisa saldo: " + formatRupiah(depositModel.getSisaSaldo()));

        TextWatcher hitungTotal = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    long qty   = Long.parseLong(etQty.getText().toString().trim());
                    long harga = Long.parseLong(etHarga.getText().toString().trim().replace(".", ""));
                    tvTotalDialog.setText(formatRupiah(qty * harga));
                } catch (NumberFormatException e) {
                    tvTotalDialog.setText(formatRupiah(0));
                }
            }
        };
        etQty.addTextChangedListener(hitungTotal);
        etHarga.addTextChangedListener(hitungTotal);

        btnSimpan.setOnClickListener(v -> {
            String ket      = etKeterangan.getText().toString().trim();
            String qtyStr   = etQty.getText().toString().trim();
            String hargaStr = etHarga.getText().toString().trim().replace(".", "");

            if (TextUtils.isEmpty(ket))      { etKeterangan.setError("Tidak boleh kosong"); return; }
            if (TextUtils.isEmpty(qtyStr))   { etQty.setError("Tidak boleh kosong"); return; }
            if (TextUtils.isEmpty(hargaStr)) { etHarga.setError("Tidak boleh kosong"); return; }

            long qty   = Long.parseLong(qtyStr);
            long harga = Long.parseLong(hargaStr);

            if (qty <= 0)   { etQty.setError("Harus lebih dari 0"); return; }
            if (harga <= 0) { etHarga.setError("Harus lebih dari 0"); return; }

            long nominal = qty * harga;

            String tanggal = new SimpleDateFormat("dd/MM/yyyy HH:mm",
                    new Locale("in", "ID")).format(new Date());

            depositModel.tambahRiwayat(
                    new DepositModel.RiwayatBelanja(ket, qty, harga, tanggal)
            );
            simpanDanRefresh();
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

    private void showDialogEditBelanja(int position) {
        DepositModel.RiwayatBelanja item = depositModel.getRiwayat().get(position);

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_edit_belanja);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextView tvTotalEditDialog = dialog.findViewById(R.id.tvTotalEditDialog);
        EditText etKeterangan      = dialog.findViewById(R.id.etEditKeterangan);
        EditText etQty             = dialog.findViewById(R.id.etEditQty);
        EditText etHarga           = dialog.findViewById(R.id.etEditJumlah);
        Button   btnSimpan         = dialog.findViewById(R.id.btnSimpanEditBelanja);
        Button   btnBatal          = dialog.findViewById(R.id.btnBatalEditBelanja);

        etKeterangan.setText(item.getKeterangan());
        etQty.setText(String.valueOf(item.getQty()));
        etHarga.setText(String.valueOf(item.getHargaSatuan()));
        tvTotalEditDialog.setText(formatRupiah(item.getTotal()));

        TextWatcher hitungTotal = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    long qty   = Long.parseLong(etQty.getText().toString().trim());
                    long harga = Long.parseLong(etHarga.getText().toString().trim().replace(".", ""));
                    tvTotalEditDialog.setText(formatRupiah(qty * harga));
                } catch (NumberFormatException e) {
                    tvTotalEditDialog.setText(formatRupiah(0));
                }
            }
        };
        etQty.addTextChangedListener(hitungTotal);
        etHarga.addTextChangedListener(hitungTotal);

        btnSimpan.setOnClickListener(v -> {
            String ket      = etKeterangan.getText().toString().trim();
            String qtyStr   = etQty.getText().toString().trim();
            String hargaStr = etHarga.getText().toString().trim().replace(".", "");

            if (TextUtils.isEmpty(ket))      { etKeterangan.setError("Tidak boleh kosong"); return; }
            if (TextUtils.isEmpty(qtyStr))   { etQty.setError("Tidak boleh kosong"); return; }
            if (TextUtils.isEmpty(hargaStr)) { etHarga.setError("Tidak boleh kosong"); return; }

            long qty   = Long.parseLong(qtyStr);
            long harga = Long.parseLong(hargaStr);

            if (qty <= 0)   { etQty.setError("Harus lebih dari 0"); return; }
            if (harga <= 0) { etHarga.setError("Harus lebih dari 0"); return; }

            long totalBaru  = qty * harga;
            long selisih    = totalBaru - item.getTotal(); // selisih dari total lama


            depositModel.getRiwayat().set(position,
                    new DepositModel.RiwayatBelanja(ket, qty, harga, item.getTanggal()));
            depositModel.setSisaSaldo(depositModel.getSisaSaldo() - selisih);

            simpanDanRefresh();
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

    private void hapusBelanja(int position) {
        DepositModel.RiwayatBelanja item = depositModel.getRiwayat().get(position);
        new AlertDialog.Builder(this)
                .setTitle("Hapus Item")
                .setMessage("Hapus \"" + item.getKeterangan() + "\"?")
                .setPositiveButton("Hapus", (d, w) -> {
                    depositModel.setSisaSaldo(depositModel.getSisaSaldo() + item.getTotal());
                    depositModel.getRiwayat().remove(position);
                    simpanDanRefresh();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private String formatRupiah(long angka) {
        return "Rp " + NumberFormat.getNumberInstance(new Locale("in", "ID")).format(angka);
    }
}