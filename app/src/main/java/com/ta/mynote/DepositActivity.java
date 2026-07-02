package com.ta.mynote;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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
import java.util.UUID;

public class DepositActivity extends AppCompatActivity {

    public static final String EXTRA_DEPOSIT_ID = "deposit_id";

    RecyclerView rvDeposit;
    DepositAdapter adapter;
    List<DepositModel> listDeposit;
    SharedPrefHelper prefHelper;
    TextView tvTotalDeposit, tvTotalSisa;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deposit);

        prefHelper  = new SharedPrefHelper(this);
        listDeposit = prefHelper.bacaSemuaDeposit();

        rvDeposit      = findViewById(R.id.rvDeposit);
        tvTotalDeposit = findViewById(R.id.tvTotalDeposit);
        tvTotalSisa    = findViewById(R.id.tvTotalSisa);

        ImageButton btnBack   = findViewById(R.id.btnBack);
        Button      btnTambah = findViewById(R.id.btnTambahDeposit);

        adapter = new DepositAdapter(listDeposit, new DepositAdapter.OnItemClickListener() {
            @Override
            public void onCardClick(int position) {
                // Buka halaman detail dengan ID deposit
                Intent intent = new Intent(DepositActivity.this, DetailDepositActivity.class);
                intent.putExtra(EXTRA_DEPOSIT_ID, listDeposit.get(position).getId());
                startActivity(intent);
            }

            @Override
            public void onHapusClick(int position) {
                showDialogHapus(position);
            }

            @Override
            public void onKirimWaClick(int position) {
                kirimKeWa(position);
            }
        });

        rvDeposit.setLayoutManager(new LinearLayoutManager(this));
        rvDeposit.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
        btnTambah.setOnClickListener(v -> showDialogTambah());

        updateRingkasan();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data setelah kembali dari DetailDepositActivity
        listDeposit.clear();
        listDeposit.addAll(prefHelper.bacaSemuaDeposit());
        adapter.notifyDataSetChanged();
        updateRingkasan();
    }

    private void kirimKeWa(int position) {
        DepositModel dm = listDeposit.get(position);
        String waktu = new SimpleDateFormat("dd/MM/yyyy  HH:mm", new Locale("in", "ID"))
                .format(new Date());
        List<DepositModel.RiwayatBelanja> riwayat = dm.getRiwayat();

        if (riwayat.isEmpty()) {
            Toast.makeText(this, "Belum ada catatan belanja untuk " + dm.getNama(),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("*List Belanja & Harga (").append(dm.getNama()).append(")*\n");
        sb.append(waktu).append("\n");
        sb.append("─────────────────\n");

        long totalPakai = 0;
        for (DepositModel.RiwayatBelanja r : riwayat) {
            sb.append("- ").append(r.getKeterangan())
                    .append(" (").append(r.getQty()).append("x").append(formatRupiah(r.getHargaSatuan())).append(")")
                    .append("  →  ").append(formatRupiah(r.getJumlah())).append("\n");
            totalPakai += r.getJumlah();
        }

        sb.append("─────────────────\n");
        sb.append("Saldo Awal : ").append(formatRupiah(dm.getDepositAwal())).append("\n");
        sb.append("Total Pakai : ").append(formatRupiah(totalPakai)).append("\n");
        sb.append("*─────────────────*\n");
        sb.append("*Sisa Saldo: ").append(formatRupiah(dm.getSisaSaldo())).append("*");

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(shareIntent, "Kirim via..."));
    }

    private void showDialogTambah() {
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

            DepositModel dm = new DepositModel(UUID.randomUUID().toString(), nama, Long.parseLong(jumlah));
            listDeposit.add(dm);
            adapter.notifyItemInserted(listDeposit.size() - 1);
            prefHelper.simpanSemuaDeposit(listDeposit);
            updateRingkasan();
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

    private void showDialogHapus(int position) {
        new AlertDialog.Builder(this)
                .setTitle("Hapus Deposit")
                .setMessage("Hapus data " + listDeposit.get(position).getNama() + "?")
                .setPositiveButton("Hapus", (d, w) -> {
                    listDeposit.remove(position);
                    adapter.notifyItemRemoved(position);
                    adapter.notifyItemRangeChanged(position, listDeposit.size());
                    prefHelper.simpanSemuaDeposit(listDeposit);
                    updateRingkasan();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void updateRingkasan() {
        long totalDeposit = 0, totalSisa = 0;
        for (DepositModel dm : listDeposit) {
            totalDeposit += dm.getDepositAwal();
            totalSisa    += dm.getSisaSaldo();
        }
        tvTotalDeposit.setText("Total Deposit: " + formatRupiah(totalDeposit));
        tvTotalSisa.setText("Total Sisa: " + formatRupiah(totalSisa));
    }

    private String formatRupiah(long angka) {
        return "Rp " + NumberFormat.getNumberInstance(new Locale("in", "ID")).format(angka);
    }
}