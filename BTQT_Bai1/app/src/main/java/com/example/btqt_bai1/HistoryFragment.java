package com.example.btqt_bai1;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class HistoryFragment extends Fragment {

    private ListView listViewHistory;
    private Button btnBack, btnExportPdf;
    private DatabaseHelper myDb;
    private ArrayList<HistoryRecord> historyList;

    private static class HistoryRecord { // Lớp lưu trữ bản ghi lịch sử
        String id; // ID của bản ghi trong DB
        String text; // Nội dung hiển thị

        // Constructor
        HistoryRecord(String id, String text) {
            this.id = id;
            this.text = text;
        }
    }

    // Adapter cho ListView
    private class HistoryAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return historyList.size();
        }

        @Override
        public Object getItem(int position) {
            return historyList.get(position);
        }

        @Override
        public long getItemId(int position)
        {
            return position;
        }

        // Hiển thị mỗi bản ghi trong ListView
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(requireContext()).inflate(R.layout.item_history, parent, false);
            }

            TextView txtHistoryRecord = convertView.findViewById(R.id.txtHistoryRecord);
            ImageButton btnDelete = convertView.findViewById(R.id.btnDelete);

            HistoryRecord record = historyList.get(position);
            txtHistoryRecord.setText(record.text);

            btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Xóa lịch sử")
                        .setMessage("Bạn có chắc chắn muốn xóa bản ghi này không?")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            myDb.deleteData(record.id);
                            loadHistoryData(); // Tải lại danh sách sau khi xóa
                            Toast.makeText(requireContext(), "Đã xóa lịch sử", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            });
            return convertView;
        }
    }

    // Tạo giao diện fragment_history
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        // Nạp giao diện fragment_history.xml
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        listViewHistory = view.findViewById(R.id.listViewHistory);
        btnBack = view.findViewById(R.id.btnBack);
        btnExportPdf = view.findViewById(R.id.btnExportPdf);
        myDb = new DatabaseHelper(requireContext()); // Gọi Database

        loadHistoryData();

        // Xử lý sự kiện bấm nút QUAY LẠI
        btnBack.setOnClickListener(v -> {
            // Hủy Fragment này để lùi về màn hình MainActivity
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        // Xử lý sự kiện bấm nút XUẤT BÁO CÁO PDF
        btnExportPdf.setOnClickListener(v -> {
            if (historyList == null || historyList.isEmpty()
                    || (historyList.size() == 1 && historyList.get(0).id.equals("-1"))) {
                Toast.makeText(requireContext(), "Không có dữ liệu để xuất!", Toast.LENGTH_SHORT).show();
                return;
            }
            generateAndSharePdf();
        });

        return view;
    }

    // Hàm lấy dữ liệu từ DB và đẩy lên ListView
    private void loadHistoryData() {
        Cursor res = myDb.getAllData();
        historyList = new ArrayList<>();

        if (res.getCount() == 0) {
            historyList.add(new HistoryRecord("-1", "Chưa có dữ liệu lịch sử nào."));
        } else {
            // Đọc từng dòng dữ liệu trong DB
            while (res.moveToNext()) {
                String id = res.getString(0);
                String text = "Lúc: " + res.getString(1) + "\n" + res.getString(2);
                historyList.add(new HistoryRecord(id, text));
            }
        }

        HistoryAdapter adapter = new HistoryAdapter();
        listViewHistory.setAdapter(adapter);
    }

    // =====================================================
    // PHẦN XUẤT PDF VÀ CHIA SẺ
    // =====================================================

    private void generateAndSharePdf() {
        // Kích thước trang A4 (đơn vị PostScript point: 1 inch = 72 point)
        int pageWidth = 595;  // A4 width
        int pageHeight = 842; // A4 height
        int margin = 40;
        int contentWidth = pageWidth - 2 * margin;

        PdfDocument document = new PdfDocument();

        // Chuẩn bị Paint cho các loại text
        Paint titlePaint = new Paint();
        titlePaint.setColor(Color.parseColor("#E65100"));
        titlePaint.setTextSize(22);
        titlePaint.setFakeBoldText(true);
        titlePaint.setAntiAlias(true);

        Paint subtitlePaint = new Paint();
        subtitlePaint.setColor(Color.parseColor("#757575"));
        subtitlePaint.setTextSize(12);
        subtitlePaint.setAntiAlias(true);

        Paint headerPaint = new Paint();
        headerPaint.setColor(Color.parseColor("#2196F3"));
        headerPaint.setTextSize(14);
        headerPaint.setFakeBoldText(true);
        headerPaint.setAntiAlias(true);

        Paint bodyPaint = new Paint();
        bodyPaint.setColor(Color.BLACK);
        bodyPaint.setTextSize(11);
        bodyPaint.setAntiAlias(true);

        Paint sttPaint = new Paint();
        sttPaint.setColor(Color.parseColor("#FF9800"));
        sttPaint.setTextSize(11);
        sttPaint.setFakeBoldText(true);
        sttPaint.setAntiAlias(true);

        Paint linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#E0E0E0"));
        linePaint.setStrokeWidth(1);

        Paint accentLinePaint = new Paint();
        accentLinePaint.setColor(Color.parseColor("#FF9800"));
        accentLinePaint.setStrokeWidth(3);

        Paint footerPaint = new Paint();
        footerPaint.setColor(Color.parseColor("#9E9E9E"));
        footerPaint.setTextSize(9);
        footerPaint.setAntiAlias(true);

        int pageNumber = 1;
        int yPosition = margin;
        int maxY = pageHeight - margin - 30; // Dành chỗ cho footer

        // --- Bắt đầu trang đầu tiên ---
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // Viền trang decorative
        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.parseColor("#2196F3"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2);
        canvas.drawRect(20, 20, pageWidth - 20, pageHeight - 20, borderPaint);

        // === HEADER ===
        // Tiêu đề chính
        yPosition = margin + 30;
        String title = "BÁO CÁO LỊCH SỬ GIÁ VÀNG";
        float titleWidth = titlePaint.measureText(title);
        canvas.drawText(title, (pageWidth - titleWidth) / 2f, yPosition, titlePaint);

        // Đường accent dưới tiêu đề
        yPosition += 10;
        float lineStart = (pageWidth - titleWidth) / 2f;
        canvas.drawLine(lineStart, yPosition, lineStart + titleWidth, yPosition, accentLinePaint);

        // Subtitle: App name
        yPosition += 20;
        String appName = "Gold Tracker App";
        float appNameWidth = subtitlePaint.measureText(appName);
        canvas.drawText(appName, (pageWidth - appNameWidth) / 2f, yPosition, subtitlePaint);

        // Ngày xuất báo cáo
        yPosition += 18;
        String exportDate = "Ngày xuất: " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
        float dateWidth = subtitlePaint.measureText(exportDate);
        canvas.drawText(exportDate, (pageWidth - dateWidth) / 2f, yPosition, subtitlePaint);

        // Tổng số bản ghi
        yPosition += 18;
        String totalRecords = "Tổng số bản ghi: " + historyList.size();
        float totalWidth = subtitlePaint.measureText(totalRecords);
        canvas.drawText(totalRecords, (pageWidth - totalWidth) / 2f, yPosition, subtitlePaint);

        // Đường kẻ phân cách header
        yPosition += 15;
        canvas.drawLine(margin, yPosition, pageWidth - margin, yPosition, linePaint);
        yPosition += 20;

        // === NỘI DUNG: Danh sách lịch sử ===
        canvas.drawText("DANH SÁCH LỊCH SỬ TRA CỨU", margin, yPosition, headerPaint);
        yPosition += 20;

        for (int i = 0; i < historyList.size(); i++) {
            HistoryRecord record = historyList.get(i);

            // Tách text thành các dòng (mỗi record có \n)
            String[] lines = record.text.split("\n");
            int recordHeight = lines.length * 16 + 15; // Ước lượng chiều cao bản ghi

            // Kiểm tra xem có cần sang trang mới không
            if (yPosition + recordHeight > maxY) {
                // Vẽ footer trang hiện tại
                drawFooter(canvas, pageWidth, pageHeight, footerPaint, pageNumber);
                document.finishPage(page);

                // Tạo trang mới
                pageNumber++;
                pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();

                // Viền trang
                canvas.drawRect(20, 20, pageWidth - 20, pageHeight - 20, borderPaint);

                yPosition = margin + 20;
            }

            // Vẽ STT
            String stt = "#" + (i + 1);
            canvas.drawText(stt, margin, yPosition, sttPaint);

            // Vẽ từng dòng của bản ghi
            for (String line : lines) {
                // Tự động cắt dòng nếu quá dài
                String trimmedLine = line.trim();
                if (bodyPaint.measureText(trimmedLine) > contentWidth - 30) {
                    // Cắt dòng thủ công
                    int charsFit = bodyPaint.breakText(trimmedLine, true, contentWidth - 30, null);
                    canvas.drawText(trimmedLine.substring(0, charsFit), margin + 30, yPosition, bodyPaint);
                    yPosition += 16;
                    if (charsFit < trimmedLine.length()) {
                        canvas.drawText(trimmedLine.substring(charsFit), margin + 30, yPosition, bodyPaint);
                    }
                } else {
                    canvas.drawText(trimmedLine, margin + 30, yPosition, bodyPaint);
                }
                yPosition += 16;
            }

            // Đường kẻ phân cách giữa các bản ghi
            yPosition += 5;
            canvas.drawLine(margin + 20, yPosition, pageWidth - margin - 20, yPosition, linePaint);
            yPosition += 12;
        }

        // Vẽ footer trang cuối
        drawFooter(canvas, pageWidth, pageHeight, footerPaint, pageNumber);
        document.finishPage(page);

        // === LƯU FILE PDF ===
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "BaoCao_GiaVang_" + timestamp + ".pdf";
            File pdfFile = new File(requireContext().getCacheDir(), fileName);

            FileOutputStream fos = new FileOutputStream(pdfFile);
            document.writeTo(fos);
            fos.close();
            document.close();

            Toast.makeText(requireContext(), "Đã tạo PDF thành công!", Toast.LENGTH_SHORT).show();

            // Chia sẻ file PDF
            sharePdf(pdfFile);

        } catch (Exception e) {
            document.close();
            Toast.makeText(requireContext(), "Lỗi khi tạo PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Vẽ footer cho mỗi trang PDF
    private void drawFooter(Canvas canvas, int pageWidth, int pageHeight, Paint footerPaint, int pageNumber) {
        int footerY = pageHeight - 35;
        canvas.drawText("Được tạo bởi Gold Tracker App", 40, footerY, footerPaint);
        String pageText = "Trang " + pageNumber;
        float pageTextWidth = footerPaint.measureText(pageText);
        canvas.drawText(pageText, pageWidth - 40 - pageTextWidth, footerY, footerPaint);
    }

    // Chia sẻ file PDF qua các ứng dụng khác
    private void sharePdf(File pdfFile) {
        Uri pdfUri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".fileprovider",
                pdfFile);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Báo cáo Giá Vàng - Gold Tracker");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Đây là báo cáo lịch sử giá vàng từ ứng dụng Gold Tracker.");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, "Chia sẻ báo cáo qua..."));
    }
}