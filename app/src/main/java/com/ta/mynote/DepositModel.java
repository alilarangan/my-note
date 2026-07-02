package com.ta.mynote;

import java.util.ArrayList;
import java.util.List;

public class DepositModel {
    private String id;
    private String nama;
    private long depositAwal;
    private long sisaSaldo;
    private List<RiwayatBelanja> riwayat;

    public DepositModel(String id, String nama, long depositAwal) {
        this.id = id;
        this.nama = nama;
        this.depositAwal = depositAwal;
        this.sisaSaldo = depositAwal;
        this.riwayat = new ArrayList<>();
    }

    public String getId() { return id; }
    public String getNama() { return nama; }
    public long getDepositAwal() { return depositAwal; }
    public long getSisaSaldo() { return sisaSaldo; }
    public void setSisaSaldo(long sisaSaldo) { this.sisaSaldo = sisaSaldo; }
    public List<RiwayatBelanja> getRiwayat() { return riwayat; }
    public void setRiwayat(List<RiwayatBelanja> riwayat) { this.riwayat = riwayat; }

    public void tambahRiwayat(RiwayatBelanja r) {
        riwayat.add(r);
        sisaSaldo -= r.getTotal(); // kurangi pakai total (qty x harga)
    }

    // ── Inner class riwayat belanja ──
    public static class RiwayatBelanja {
        private String keterangan;
        private long   qty;          // jumlah item
        private long   hargaSatuan;  // harga per item
        private String tanggal;

        // Constructor baru dengan qty & hargaSatuan
        public RiwayatBelanja(String keterangan, long qty, long hargaSatuan, String tanggal) {
            this.keterangan  = keterangan;
            this.qty         = qty;
            this.hargaSatuan = hargaSatuan;
            this.tanggal     = tanggal;
        }

        // Constructor lama (qty=1, jumlah=hargaSatuan) — untuk kompatibilitas data lama
        public RiwayatBelanja(String keterangan, long jumlah, String tanggal) {
            this.keterangan  = keterangan;
            this.qty         = 1;
            this.hargaSatuan = jumlah;
            this.tanggal     = tanggal;
        }

        public String getKeterangan()  { return keterangan; }
        public long   getQty()         { return qty; }
        public long   getHargaSatuan() { return hargaSatuan; }
        public long   getTotal()       { return qty * hargaSatuan; }
        public String getTanggal()     { return tanggal; }

        // Alias untuk kompatibilitas kode lama yang masih pakai getJumlah()
        public long getJumlah() { return getTotal(); }
    }
}