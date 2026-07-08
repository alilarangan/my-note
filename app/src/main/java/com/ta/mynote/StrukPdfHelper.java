package com.ta.mynote;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.text.TextPaint;

import java.io.File;
import java.io.FileOutputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Generate PDF struk/nota format A4 Landscape.
 * Konten nota (lebar 10cm) di sisi KIRI halaman.
 * Sisa ruang kanan dibiarkan kosong.
 */
public class StrukPdfHelper {

    private static final float CM_TO_PT   = 72f / 2.54f;

    private static final float A4_LEBAR   = 29.7f * CM_TO_PT; // ~842 pt
    private static final float A4_TINGGI  = 21.0f * CM_TO_PT; // ~595 pt

    private static final float NOTA_LEBAR = 10.0f * CM_TO_PT;
    private static final float MARGIN      = 0.5f * CM_TO_PT;
    private static final int   MIN_BARIS   = 13;
    private static final float TINGGI_BARIS = 18f;
    private static final float TINGGI_HEADER = 110f;
    private static final float TINGGI_FOOTER = 80f;

    public static File generateStrukPdf(Context context, DepositModel deposit) {
        PdfDocument document = new PdfDocument();

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                (int) A4_LEBAR, (int) A4_TINGGI, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        gambarStruk(canvas, deposit);

        document.finishPage(page);

        File outputDir = new File(context.getCacheDir(), "struk");
        if (!outputDir.exists()) outputDir.mkdirs();

        String namaFile = "Struk_" + deposit.getNama().replaceAll("[^a-zA-Z0-9]", "_") + ".pdf";
        File file = new File(outputDir, namaFile);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            document.writeTo(fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            document.close();
        }

        return file;
    }

    private static void gambarStruk(Canvas canvas, DepositModel deposit) {

        Paint garisTipis = new Paint();
        garisTipis.setColor(Color.BLACK);
        garisTipis.setStrokeWidth(0.5f);
        garisTipis.setStyle(Paint.Style.STROKE);
        garisTipis.setAntiAlias(true);

        TextPaint bold = new TextPaint();
        bold.setColor(Color.BLACK);
        bold.setFakeBoldText(true);
        bold.setAntiAlias(true);

        TextPaint normal = new TextPaint();
        normal.setColor(Color.BLACK);
        normal.setAntiAlias(true);

        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("in", "ID"));

        float x = MARGIN;
        float y = MARGIN;
        float isiLebar = NOTA_LEBAR - (2 * MARGIN);

        String tanggal = new SimpleDateFormat("dd/MM/yyyy", new Locale("in", "ID")).format(new Date());
        normal.setTextSize(8f);
        drawDotLine(canvas, x + 180, y + 10, x + isiLebar, garisTipis);
        drawTextRight(canvas, tanggal, x + isiLebar, y + 9, normal);

        y += 20;
        normal.setTextSize(9f);
        float labelW = normal.measureText("Tuan") + 6;

        canvas.drawText("Tuan", x + 150, y, normal);
        canvas.drawText(deposit.getNama(), x + labelW + 154, y - 1, normal);
        drawDotLine(canvas, x + labelW +153, y + 2, x + isiLebar, garisTipis);

        y += 16;
        canvas.drawText("Toko", x + 150, y, normal);
        drawDotLine(canvas, x + labelW + 153, y + 2, x + isiLebar, garisTipis);

        y += 20;
        bold.setTextSize(11f);
        canvas.drawText("NOTA NO.", x, y, bold);
        normal.setTextSize(9f);
        String noNota = deposit.getId().substring(0, 8).toUpperCase();
        canvas.drawText(noNota, x + bold.measureText("NOTA NO.") + 6, y, normal);
        drawDotLine(canvas, x + labelW + 153, y, x + isiLebar, garisTipis);
        // Garis bawah Nota No
        y += 5;
        canvas.drawLine(x, y, x + isiLebar, y, garisTipis);

        float tableTop = y;

        float cQtyW    = isiLebar * 0.11f;
        float cNamaW   = isiLebar * 0.49f;
        float cHargaW  = isiLebar * 0.18f;
        float cJumlahW = isiLebar * 0.22f;

        float cQty    = x;
        float cNama   = cQty + cQtyW;
        float cHarga  = cNama + cNamaW;
        float cJumlah = cHarga + cHargaW;

        float headerH = 20f;
        canvas.drawRect(x, tableTop, x + isiLebar, tableTop + headerH, garisTipis);
        drawVGaris(canvas, cNama, tableTop, tableTop + headerH, garisTipis);
        drawVGaris(canvas, cHarga, tableTop, tableTop + headerH, garisTipis);
        drawVGaris(canvas, cJumlah, tableTop, tableTop + headerH, garisTipis);

        bold.setTextSize(8f);
        drawTextCenter(canvas, "QTY", cQty, cQty + cQtyW, tableTop + 13, bold);
        drawTextCenter(canvas, "NAMA BARANG", cNama, cNama + cNamaW, tableTop + 13, bold);
        drawTextCenter(canvas, "HARGA", cHarga, cHarga + cHargaW, tableTop + 13, bold);
        drawTextCenter(canvas, "JUMLAH", cJumlah, cJumlah + cJumlahW, tableTop + 13, bold);

        List<DepositModel.RiwayatBelanja> riwayat = deposit.getRiwayat();
        int jumlahBaris = Math.max(riwayat.size(), MIN_BARIS);
        float rowY = tableTop + headerH;
        normal.setTextSize(8.5f);

        for (int i = 0; i < jumlahBaris; i++) {
            float rowBottom = rowY + TINGGI_BARIS;

            canvas.drawLine(x, rowBottom, x + isiLebar, rowBottom, garisTipis);
            drawVGaris(canvas, cNama, rowY, rowBottom, garisTipis);
            drawVGaris(canvas, cHarga, rowY, rowBottom, garisTipis);
            drawVGaris(canvas, cJumlah, rowY, rowBottom, garisTipis);

            if (i < riwayat.size()) {
                DepositModel.RiwayatBelanja item = riwayat.get(i);
                float tY = rowY + (TINGGI_BARIS / 2f) + 3.5f;

                String qtyStr = item.getQty() > 1
                        ? item.getQty() + ""
                        : "1";
                drawTextCenter(canvas, qtyStr, cQty, cQty + cQtyW, tY, normal);

                drawTextEllipsis(canvas, item.getKeterangan(), cNama + 3, cNama + cNamaW - 3, tY, normal);

                drawTextRight(canvas, nf.format(item.getHargaSatuan()), cHarga + cHargaW - 3, tY, normal);

                drawTextRight(canvas, nf.format(item.getTotal()), cJumlah + cJumlahW - 3, tY, normal);
            }

            rowY = rowBottom;
        }

        float tableBottom = rowY;

        canvas.drawLine(x, tableTop, x, tableBottom, garisTipis);
        canvas.drawLine(x + isiLebar, tableTop, x + isiLebar, tableBottom, garisTipis);
        canvas.drawLine(x, tableBottom, x + isiLebar, tableBottom, garisTipis);

        float fY = tableBottom + 18;
        long totalDipakai = 0;
        for (DepositModel.RiwayatBelanja r : riwayat) totalDipakai += r.getTotal();

        bold.setTextSize(9f);
        canvas.drawText("Jumlah Rp.", cHarga - 5, fY, bold);
        String totalStr = nf.format(totalDipakai);
        drawTextRight(canvas, totalStr, x + isiLebar - 3, fY, bold);
        canvas.drawLine(cJumlah, fY + 3, x + isiLebar, fY + 3, garisTipis);
        canvas.drawLine(cJumlah, fY + 5, x + isiLebar, fY + 5, garisTipis);

        fY += 28;
        normal.setTextSize(8.5f);
        canvas.drawText("Tanda Terima", x, fY, normal);
        canvas.drawText("Hormat kami,", cHarga, fY, normal);

        drawVDash(canvas, NOTA_LEBAR, MARGIN, A4_TINGGI - MARGIN, garisTipis);
    }

    private static void drawTextCenter(Canvas canvas, String text, float left, float right, float y, TextPaint p) {
        float tw = p.measureText(text);
        canvas.drawText(text, left + (right - left - tw) / 2f, y, p);
    }

    private static void drawTextRight(Canvas canvas, String text, float rightEdge, float y, TextPaint p) {
        canvas.drawText(text, rightEdge - p.measureText(text), y, p);
    }

    private static void drawTextEllipsis(Canvas canvas, String text, float left, float right, float y, TextPaint p) {
        float maxW = right - left;
        if (p.measureText(text) <= maxW) {
            canvas.drawText(text, left, y, p);
            return;
        }
        String s = text;
        while (s.length() > 1 && p.measureText(s + "...") > maxW) {
            s = s.substring(0, s.length() - 1);
        }
        canvas.drawText(s + "...", left, y, p);
    }

    private static void drawVGaris(Canvas canvas, float x, float top, float bottom, Paint p) {
        canvas.drawLine(x, top, x, bottom, p);
    }

    private static void drawDotLine(Canvas canvas, float startX, float y, float endX, Paint p) {
        float step = 4f;
        for (float cx = startX; cx < endX; cx += step) {
            canvas.drawCircle(cx, y, 0.6f, p);
        }
    }

    private static void drawVDash(Canvas canvas, float x, float startY, float endY, Paint p) {
        float dashLen = 6f, gap = 4f;
        for (float cy = startY; cy < endY; cy += dashLen + gap) {
            canvas.drawLine(x, cy, x, Math.min(cy + dashLen, endY), p);
        }
    }
}