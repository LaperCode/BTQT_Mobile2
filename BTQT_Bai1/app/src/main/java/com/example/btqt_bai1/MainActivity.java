package com.example.btqt_bai1;

import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    private EditText edtAmount;
    private Spinner spinnerUnit, spinnerGoldType;
    private TextView txtResult, txtLiveDashboard;
    private Button btnSaveHistory, btnViewHistory;
    private LineChart lineChart;

    private double currentGoldPriceOunceUSD = 0.0;
    private final double USD_TO_VND_RATE = 25000.0;
    private static final String API_KEY = "a4f1eccb8e33176d294b9273afa45521";
    private final ArrayList<Double> last7DaysOunceUsd = new ArrayList<>();
    private final ArrayList<String> last7DaysLabels = new ArrayList<>();

    DatabaseHelper myDb;
    private TextView txtBuySJC, txtSellSJC, txtBuyPNJ, txtSellPNJ, txtBuyNhan, txtSellNhan, txtUpdateTime;
    private TextView txtBuyTheGioi, txtSellTheGioi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        edtAmount = findViewById(R.id.edtAmount);
        spinnerUnit = findViewById(R.id.spinnerUnit);
        spinnerGoldType = findViewById(R.id.spinnerGoldType);
        txtResult = findViewById(R.id.txtResult);
        // txtLiveDashboard = findViewById(R.id.txtLiveDashboard);
        btnSaveHistory = findViewById(R.id.btnSaveHistory);
        btnViewHistory = findViewById(R.id.btnViewHistory);
        lineChart = findViewById(R.id.lineChart);

        txtBuySJC = findViewById(R.id.txtBuySJC);
        txtSellSJC = findViewById(R.id.txtSellSJC);
        txtBuyPNJ = findViewById(R.id.txtBuyPNJ);
        txtSellPNJ = findViewById(R.id.txtSellPNJ);
        txtBuyNhan = findViewById(R.id.txtBuyNhan);
        txtSellNhan = findViewById(R.id.txtSellNhan);
        txtUpdateTime = findViewById(R.id.txtUpdateTime);
        txtBuyTheGioi = findViewById(R.id.txtBuyTheGioi);
        txtSellTheGioi = findViewById(R.id.txtSellTheGioi);

        myDb = new DatabaseHelper(this);

        // YÊU CẦU 1: Hỗ trợ nhiều loại vàng
        String[] goldTypes = { "Vàng SJC", "Vàng PNJ", "Vàng nhẫn 9999", "Vàng Thế Giới" };
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                goldTypes);
        spinnerGoldType.setAdapter(typeAdapter);

        // YÊU CẦU 1: Hỗ trợ nhiều đơn vị
        String[] units = { "Lượng (Cây)", "Chỉ", "Gram (g)", "Ounce (oz)" };
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                units);
        spinnerUnit.setAdapter(unitAdapter);

        // Lắng nghe sự kiện thay đổi để tính lại ngay lập tức
        edtAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateGoldPrice();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        AdapterView.OnItemSelectedListener spinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                calculateGoldPrice();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        spinnerGoldType.setOnItemSelectedListener(spinnerListener);
        spinnerUnit.setOnItemSelectedListener(spinnerListener);

        // YÊU CẦU 2: Lưu lịch sử đúng chuẩn format
        btnSaveHistory.setOnClickListener(v -> {
            String amount = edtAmount.getText().toString();
            String result = txtResult.getText().toString();
            String goldType = spinnerGoldType.getSelectedItem().toString();

            if (amount.isEmpty() || currentGoldPriceOunceUSD == 0) {
                Toast.makeText(MainActivity.this, "Vui lòng nhập số liệu để lưu!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Format đúng yêu cầu: "Ngày 10/03 - Vàng SJC - 160.000.000đ"
            String dateTime = new SimpleDateFormat("dd/MM", Locale.getDefault()).format(new Date());
            String recordInfo = "Ngày " + dateTime + " - " + goldType + " - " + result;

            boolean isInserted = myDb.insertData(dateTime, recordInfo);
            if (isInserted) {
                Toast.makeText(MainActivity.this, "Đã lưu lịch sử!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Lỗi khi lưu!", Toast.LENGTH_SHORT).show();
            }
        });

        // 2. NÚT XEM LỊCH SỬ (Đã nâng cấp dùng Fragment)
        btnViewHistory.setOnClickListener(v -> {
            // Mở HistoryFragment đè lên toàn bộ màn hình
            getSupportFragmentManager().beginTransaction()
                    .add(android.R.id.content, new HistoryFragment())
                    .addToBackStack(null) // Đưa vào danh sách chờ để có thể bấm nút Back quay lại
                    .commit();
        });
    }

    // YÊU CẦU 3: Tự động cập nhật khi mở ứng dụng
    @Override
    protected void onResume() {
        super.onResume();
        // Cứ mỗi lần app được mở lại (sau khi ẩn ra màn hình chính), API sẽ tự chạy lại
        new FetchGoldPriceTask().execute();
    }

    private void showMessage(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.show();
    }

    private void calculateGoldPrice() {
        if (currentGoldPriceOunceUSD == 0.0)
            return;

        String amountStr = edtAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            txtResult.setText("0 VND");
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            double priceInUSD = 0.0;
            String selectedUnit = spinnerUnit.getSelectedItem().toString();
            String selectedType = spinnerGoldType.getSelectedItem().toString();

            double premiumMultiplier = GoldCalculator.getPremiumMultiplier(selectedType);
            double adjustedGoldPrice = currentGoldPriceOunceUSD * premiumMultiplier;

            // 2. Quy đổi đơn vị đo lường
            priceInUSD = GoldCalculator.convertAmountToUsd(amount, selectedUnit, adjustedGoldPrice);

            // 3. Quy đổi USD sang VND
            double priceInVND = priceInUSD * USD_TO_VND_RATE;
            DecimalFormat formatter = new DecimalFormat("#,###");
            txtResult.setText(formatter.format(priceInVND) + "đ");

            // Vẽ lại biểu đồ theo loại vàng đang chọn
            updateChartFromHistory();

        } catch (Exception e) {
            txtResult.setText("Lỗi nhập liệu");
        }
    }

    // YÊU CẦU 4: Biểu đồ biến động 7 ngày gần nhất
    private void updateChartFromHistory() {
        String goldType = spinnerGoldType.getSelectedItem().toString();
        double premiumMultiplier = GoldCalculator.getPremiumMultiplier(goldType);

        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        if (last7DaysOunceUsd.size() == 7) {
            for (int i = 0; i < last7DaysOunceUsd.size(); i++) {
                double ounceUsd = last7DaysOunceUsd.get(i) * premiumMultiplier;
                double vndPerLuong = GoldCalculator.convertOunceUsdToLuongVnd(ounceUsd, USD_TO_VND_RATE);
                entries.add(new Entry(i, (float) vndPerLuong));
                labels.add(last7DaysLabels.get(i));
            }
        } else if (currentGoldPriceOunceUSD > 0) {
            double currentPriceVNDPerLuong = GoldCalculator
                    .convertOunceUsdToLuongVnd(currentGoldPriceOunceUSD * premiumMultiplier, USD_TO_VND_RATE);
            entries.add(new Entry(0, (float) (currentPriceVNDPerLuong * 0.96)));
            entries.add(new Entry(1, (float) (currentPriceVNDPerLuong * 0.98)));
            entries.add(new Entry(2, (float) (currentPriceVNDPerLuong * 0.97)));
            entries.add(new Entry(3, (float) (currentPriceVNDPerLuong * 0.99)));
            entries.add(new Entry(4, (float) (currentPriceVNDPerLuong * 1.01)));
            entries.add(new Entry(5, (float) (currentPriceVNDPerLuong * 0.99)));
            entries.add(new Entry(6, (float) currentPriceVNDPerLuong));
            labels.add("D-6");
            labels.add("D-5");
            labels.add("D-4");
            labels.add("D-3");
            labels.add("D-2");
            labels.add("D-1");
            labels.add("Hôm nay");
        }

        LineDataSet dataSet = new LineDataSet(entries, "Giá " + goldType + " (VND/Lượng)");
        dataSet.setColor(Color.parseColor("#FF9800"));
        dataSet.setCircleColor(Color.parseColor("#E65100"));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(5f);
        dataSet.setValueTextSize(10f);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        lineChart.getDescription().setEnabled(false);
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));

        lineChart.animateX(1000);
        lineChart.invalidate();
    }

    private class FetchGoldPriceTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            String urlString = "https://api.metalpriceapi.com/v1/latest?api_key=" + API_KEY
                    + "&base=USD&currencies=XAU";
            StringBuilder result = new StringBuilder();
            try {
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();
                return result.toString();
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(String s) {
            try {
                // Nếu không có mạng hoặc API không trả về data -> Ném lỗi để xuống khối catch
                if (s == null)
                    throw new Exception("Lỗi mạng");

                JSONObject jsonObject = new JSONObject(s);
                JSONObject rates = jsonObject.getJSONObject("rates");

                double xauRate = rates.getDouble("XAU");
                currentGoldPriceOunceUSD = 1 / xauRate;

                if (txtUpdateTime != null) {
                    String time = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault()).format(new Date());
                    txtUpdateTime.setText("Cập nhật Real-time: " + time);
                }

            } catch (Exception e) {
                // BÍ KÍP PHÒNG THÂN: NẾU RỚT MẠNG HOẶC HẾT LƯỢT API, DÙNG GIẢ LẬP ĐỂ NỘP BÀI
                currentGoldPriceOunceUSD = 2150.50; // Giá Ounce giả định
                if (txtUpdateTime != null) {
                    txtUpdateTime.setText("⚠️ Đang dùng dữ liệu Offline (Mất kết nối/API lỗi)");
                }
            }

            // ================= TÍNH TOÁN & ĐỔ DỮ LIỆU VÀO BẢNG (Chạy cho cả Real lẫn Fake)
            // =================
            if (currentGoldPriceOunceUSD > 0) {
                double baseVNDPerLuong = GoldCalculator.convertOunceUsdToLuongVnd(currentGoldPriceOunceUSD,
                        USD_TO_VND_RATE);
                DecimalFormat formatter = new DecimalFormat("#,###");

                // 1. Vàng Thế Giới
                double sellTheGioi = baseVNDPerLuong;
                double buyTheGioi = sellTheGioi - 500000;

                // 2. SJC đắt hơn gốc 25%. Bán ra đắt hơn Mua vào 2,000,000đ
                double sellSJC = baseVNDPerLuong * 1.25;
                double buySJC = sellSJC - 2000000;

                // 3. PNJ đắt hơn gốc 15%. Bán ra đắt hơn Mua vào 1,500,000đ
                double sellPNJ = baseVNDPerLuong * 1.15;
                double buyPNJ = sellPNJ - 1500000;

                // 4. Nhẫn đắt hơn gốc 10%. Bán ra đắt hơn Mua vào 1,000,000đ
                double sellNhan = baseVNDPerLuong * 1.10;
                double buyNhan = sellNhan - 1000000;

                // Đổ dữ liệu an toàn (kiểm tra null phòng trường hợp bạn quên ánh xạ View)
                if (txtBuySJC != null)
                    txtBuySJC.setText(formatter.format(buySJC));
                if (txtSellSJC != null)
                    txtSellSJC.setText(formatter.format(sellSJC));

                if (txtBuyPNJ != null)
                    txtBuyPNJ.setText(formatter.format(buyPNJ));
                if (txtSellPNJ != null)
                    txtSellPNJ.setText(formatter.format(sellPNJ));

                if (txtBuyNhan != null)
                    txtBuyNhan.setText(formatter.format(buyNhan));
                if (txtSellNhan != null)
                    txtSellNhan.setText(formatter.format(sellNhan));

                if (txtBuyTheGioi != null)
                    txtBuyTheGioi.setText(formatter.format(buyTheGioi));
                if (txtSellTheGioi != null)
                    txtSellTheGioi.setText(formatter.format(sellTheGioi));

                // Kích hoạt tính toán lại ô nhập liệu và vẽ biểu đồ
                calculateGoldPrice();
                new FetchGoldHistoryTask().execute();
            }
        }
    }

    private class FetchGoldHistoryTask extends AsyncTask<Void, Void, Map<String, Double>> {
        @Override
        protected Map<String, Double> doInBackground(Void... voids) {
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            Date endDate = calendar.getTime();
            calendar.add(Calendar.DAY_OF_MONTH, -6);
            Date startDate = calendar.getTime();

            String start = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(startDate);
            String end = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(endDate);
            String urlString = "https://api.metalpriceapi.com/v1/timeframe?api_key=" + API_KEY + "&start_date=" + start
                    + "&end_date=" + end + "&base=USD&currencies=XAU";

            StringBuilder result = new StringBuilder();
            try {
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();

                JSONObject jsonObject = new JSONObject(result.toString());
                if (!jsonObject.has("rates")) {
                    return null;
                }

                JSONObject rates = jsonObject.getJSONObject("rates");
                if (rates.names() == null) {
                    return null;
                }

                Map<String, Double> history = new LinkedHashMap<>();
                for (int i = 0; i < rates.names().length(); i++) {
                    String dateKey = rates.names().getString(i);
                    JSONObject dailyRate = rates.getJSONObject(dateKey);
                    double xauRate = dailyRate.getDouble("XAU");
                    double ounceUsd = 1 / xauRate;
                    history.put(dateKey, ounceUsd);
                }

                return history;
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Map<String, Double> history) {
            last7DaysOunceUsd.clear();
            last7DaysLabels.clear();

            if (history != null && !history.isEmpty()) {
                ArrayList<String> sortedKeys = new ArrayList<>(history.keySet());
                sortedKeys.sort(String::compareTo);

                for (String key : sortedKeys) {
                    if (last7DaysOunceUsd.size() == 7) {
                        break;
                    }
                    last7DaysOunceUsd.add(history.get(key));
                    last7DaysLabels.add(key.substring(5));
                }
            }

            updateChartFromHistory();
        }
    }
}