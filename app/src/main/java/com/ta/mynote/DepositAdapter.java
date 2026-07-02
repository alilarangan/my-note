package com.ta.mynote;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class DepositAdapter extends RecyclerView.Adapter<DepositAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onCardClick(int position);
        void onHapusClick(int position);
        void onKirimWaClick(int position);
    }

    private final List<DepositModel> list;
    private final OnItemClickListener listener;

    public DepositAdapter(List<DepositModel> list, OnItemClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_deposit, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DepositModel dm = list.get(position);
        Context ctx     = holder.itemView.getContext();

        holder.tvNama.setText(dm.getNama());
        holder.tvDeposit.setText("Deposit awal: " + formatRupiah(dm.getDepositAwal()));
        holder.tvSisa.setText("Sisa: " + formatRupiah(dm.getSisaSaldo()));

        // Warna sisa saldo
        int colorSisa = dm.getSisaSaldo() <= 0
                ? ctx.getResources().getColor(R.color.colorError, null)
                : ctx.getResources().getColor(R.color.colorHeaderTitle, null);
        holder.tvSisa.setTextColor(colorSisa);

        // Jumlah item belanja
        int jumlahItem = dm.getRiwayat().size();
        holder.tvJumlahItem.setText(jumlahItem + " item belanja");

        // Tap kartu → detail
        holder.itemView.setOnClickListener(v -> listener.onCardClick(position));

        // Tombol WA & Hapus — cegah tap kartu ikut terpicu
        holder.btnKirimWa.setOnClickListener(v -> {
            v.setOnClickListener(null); // debounce
            listener.onKirimWaClick(position);
            v.post(() -> v.setOnClickListener(vv -> listener.onKirimWaClick(holder.getAdapterPosition())));
        });
        holder.btnHapus.setOnClickListener(v -> listener.onHapusClick(position));
    }

    @Override
    public int getItemCount() { return list.size(); }

    private String formatRupiah(long angka) {
        return "Rp " + NumberFormat.getNumberInstance(new Locale("in", "ID")).format(angka);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNama, tvDeposit, tvSisa, tvJumlahItem, btnKirimWa, btnHapus;

        ViewHolder(View itemView) {
            super(itemView);
            tvNama       = itemView.findViewById(R.id.tvNama);
            tvDeposit    = itemView.findViewById(R.id.tvDeposit);
            tvSisa       = itemView.findViewById(R.id.tvSisa);
            tvJumlahItem = itemView.findViewById(R.id.tvJumlahItem);
            btnKirimWa   = itemView.findViewById(R.id.btnKirimWa);
            btnHapus     = itemView.findViewById(R.id.btnHapus);
        }
    }
}