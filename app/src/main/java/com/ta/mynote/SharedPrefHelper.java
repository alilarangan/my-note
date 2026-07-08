package com.ta.mynote;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SharedPrefHelper {
    private static final String PREF_NAME = "mynote_deposit";
    private static final String KEY_DATA  = "data_deposit";

    private final SharedPreferences prefs;

    public SharedPrefHelper(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void simpanSemuaDeposit(List<DepositModel> list) {
        try {
            JSONArray jsonArray = new JSONArray();
            for (DepositModel dm : list) {
                JSONObject obj = new JSONObject();
                obj.put("id",         dm.getId());
                obj.put("nama",       dm.getNama());
                obj.put("depositAwal", dm.getDepositAwal());
                obj.put("sisaSaldo",  dm.getSisaSaldo());

                JSONArray riwayatArr = new JSONArray();
                for (DepositModel.RiwayatBelanja r : dm.getRiwayat()) {
                    JSONObject rObj = new JSONObject();
                    rObj.put("keterangan",  r.getKeterangan());
                    rObj.put("qty",         r.getQty());
                    rObj.put("hargaSatuan", r.getHargaSatuan());
                    rObj.put("tanggal",     r.getTanggal());
                    riwayatArr.put(rObj);
                }
                obj.put("riwayat", riwayatArr);
                jsonArray.put(obj);
            }
            prefs.edit().putString(KEY_DATA, jsonArray.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<DepositModel> bacaSemuaDeposit() {
        List<DepositModel> list = new ArrayList<>();
        try {
            String json = prefs.getString(KEY_DATA, "[]");
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                DepositModel dm = new DepositModel(
                        obj.getString("id"),
                        obj.getString("nama"),
                        obj.getLong("depositAwal")
                );
                dm.setSisaSaldo(obj.getLong("sisaSaldo"));

                JSONArray riwayatArr = obj.getJSONArray("riwayat");
                List<DepositModel.RiwayatBelanja> riwayat = new ArrayList<>();
                for (int j = 0; j < riwayatArr.length(); j++) {
                    JSONObject rObj = riwayatArr.getJSONObject(j);

                    String keterangan = rObj.getString("keterangan");
                    long qty, hargaSatuan;

                    if (rObj.has("qty") && rObj.has("hargaSatuan")) {
                        qty         = rObj.getLong("qty");
                        hargaSatuan = rObj.getLong("hargaSatuan");
                    } else {
                        qty         = 1;
                        hargaSatuan = rObj.getLong("jumlah");
                    }

                    if (keterangan.contains(" (") && keterangan.contains("x") && keterangan.endsWith(")")) {
                        try {
                            int idxBuka  = keterangan.lastIndexOf(" (");
                            int idxTutup = keterangan.lastIndexOf(")");
                            String dalam = keterangan.substring(idxBuka + 2, idxTutup);
                            String[] parts = dalam.split("x");
                            long parsedQty   = Long.parseLong(parts[0].trim());
                            long parsedHarga = Long.parseLong(parts[1].replaceAll("[^0-9]", ""));
                            if (parsedQty > 0 && parsedHarga > 0) {
                                qty         = parsedQty;
                                hargaSatuan = parsedHarga;
                                keterangan  = keterangan.substring(0, idxBuka).trim();
                            }
                        } catch (Exception ignored) {}
                    }

                    riwayat.add(new DepositModel.RiwayatBelanja(
                            keterangan,
                            qty,
                            hargaSatuan,
                            rObj.getString("tanggal")
                    ));
                }
                dm.setRiwayat(riwayat);
                list.add(dm);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}