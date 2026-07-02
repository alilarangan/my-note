package com.ta.mynote;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class BelanjaAdapter extends RecyclerView.Adapter<BelanjaAdapter.ViewHolder> {

    public interface OnBelanjaListener {
        void onEdit(int position);
        void onHapus(int position);
    }

    private final List<DepositModel.RiwayatBelanja> list;
    private final OnBelanjaListener listener;

    public BelanjaAdapter(List<DepositModel.RiwayatBelanja> list, OnBelanjaListener listener) {
        this.list     = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_belanja, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DepositModel.RiwayatBelanja item = list.get(position);

        holder.tvKeterangan.setText(item.getKeterangan());
        holder.tvTanggal.setText(item.getTanggal());

        // Tampilkan qty x harga satuan = total
        if (item.getQty() > 1) {
            holder.tvJumlah.setText(
                    item.getQty() + " x " + formatRupiah(item.getHargaSatuan())
                            + " = " + formatRupiah(item.getTotal())
            );
        } else {
            holder.tvJumlah.setText(formatRupiah(item.getHargaSatuan()));
        }

        holder.btnEdit.setOnClickListener(v -> listener.onEdit(holder.getAdapterPosition()));
        holder.btnHapus.setOnClickListener(v -> listener.onHapus(holder.getAdapterPosition()));
    }

    @Override
    public int getItemCount() { return list.size(); }

    private String formatRupiah(long angka) {
        return "Rp " + NumberFormat.getNumberInstance(new Locale("in", "ID")).format(angka);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvKeterangan, tvJumlah, tvTanggal, btnEdit, btnHapus;

        ViewHolder(View itemView) {
            super(itemView);
            tvKeterangan = itemView.findViewById(R.id.tvKeteranganBelanja);
            tvJumlah     = itemView.findViewById(R.id.tvJumlahBelanja);
            tvTanggal    = itemView.findViewById(R.id.tvTanggalBelanja);
            btnEdit      = itemView.findViewById(R.id.btnEditBelanja);
            btnHapus     = itemView.findViewById(R.id.btnHapusBelanja);
        }
    }
}