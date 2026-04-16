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
    private TextView txtAdvice;
    private TextView txtCurrentPrice;

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
        txtAdvice = findViewById(R.id.txtAdvice);

        myDb = new DatabaseHelper(this);

        // Hỗ trợ nhiều loại vàng
        String[] goldTypes = { "Vàng SJC", "Vàng PNJ", "Vàng nhẫn 9999", "Vàng 24k", "Vàng 18k", "Vàng Thế Giới" };
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                goldTypes);
        spinnerGoldType.setAdapter(typeAdapter);

        // Hỗ trợ nhiều đơn vị
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

        // Lưu lịch sử đúng chuẩn format
        btnSaveHistory.setOnClickListener(v -> {
            String amount = edtAmount.getText().toString();
            String result = txtResult.getText().toString();
            String goldType = spinnerGoldType.getSelectedItem().toString();
            String unit = spinnerUnit.getSelectedItem().toString();

            if (amount.isEmpty() || currentGoldPriceOunceUSD == 0) {
                Toast.makeText(MainActivity.this, "Vui lòng nhập số liệu để lưu!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Format đúng yêu cầu hiển thị 2 dòng
            String dateTime = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
            String recordInfo = amount + " " + unit + " " + goldType + " = " + result.replace("đ", " VND");

            boolean isInserted = myDb.insertData(dateTime, recordInfo);
            if (isInserted) {
                Toast.makeText(MainActivity.this, "Đã lưu lịch sử!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Lỗi khi lưu!", Toast.LENGTH_SHORT).show();
            }
        });

        btnViewHistory.setOnClickListener(v -> {
            // Mở HistoryFragment đè lên toàn bộ màn hình
            getSupportFragmentManager().beginTransaction()
                    .add(android.R.id.content, new HistoryFragment())
                    .addToBackStack(null) // Đưa vào danh sách chờ để có thể bấm nút Back quay lại
                    .commit();
        });
    }

    // Tự động cập nhật khi mở ứng dụng
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

    // Biểu đồ biến động 7 ngày gần nhất
    private void updateChartFromHistory() {
        String goldType = spinnerGoldType.getSelectedItem().toString();
        double premiumMultiplier = GoldCalculator.getPremiumMultiplier(goldType);

        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        if (last7DaysOunceUsd.size() == 7) { // Nếu có dữ liệu lịch sử
            for (int i = 0; i < last7DaysOunceUsd.size(); i++) {
                double ounceUsd = last7DaysOunceUsd.get(i) * premiumMultiplier;
                double vndPerLuong = GoldCalculator.convertOunceUsdToLuongVnd(ounceUsd, USD_TO_VND_RATE);
                entries.add(new Entry(i, (float) vndPerLuong));
                labels.add(last7DaysLabels.get(i));
            }
        } else if (currentGoldPriceOunceUSD > 0) { // Nếu chưa có dữ liệu lịch sử
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

        // Vẽ biểu đồ
        LineDataSet dataSet = new LineDataSet(entries, "Giá " + goldType + " (VND/Lượng)");
        dataSet.setColor(Color.parseColor("#FF9800"));
        dataSet.setCircleColor(Color.parseColor("#E65100"));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(5f);
        dataSet.setValueTextSize(10f);

        // Set up biểu đồ
        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        lineChart.getDescription().setEnabled(false);
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));

        lineChart.animateX(1000);
        lineChart.invalidate();

        if (txtAdvice != null && entries.size() >= 2) {
            float firstVal = entries.get(0).getY();
            float lastVal = entries.get(entries.size() - 1).getY();
            if (lastVal > firstVal) {
                txtAdvice.setText("Giá đang tăng trong 7 ngày qua.");
                txtAdvice.setTextColor(Color.parseColor("#4CAF50")); // xanh la
            } else if (lastVal < firstVal) {
                txtAdvice.setText("Giá đang giảm trong 7 ngày qua.");
                txtAdvice.setTextColor(Color.parseColor("#F44336")); // Do
            } else {
                txtAdvice.setText("Giá đang ổn định.");
                txtAdvice.setTextColor(Color.parseColor("#2196F3")); // xanh duong
            }
        }
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

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    return "ERROR:" + responseCode;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();
                return result.toString();
            } catch (Exception e) {
                return "ERROR:" + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String s) {
            try {
                if (s == null)
                    throw new Exception("Lỗi mạng không xác định");
                if (s.startsWith("ERROR:")) {
                    throw new Exception(s.replace("ERROR:", ""));
                }

                JSONObject jsonObject = new JSONObject(s);
                if (!jsonObject.has("success") || !jsonObject.getBoolean("success")) {
                    throw new Exception("Lỗi từ API Server");
                }

                JSONObject rates = jsonObject.getJSONObject("rates");
                double xauRate = rates.getDouble("XAU");

                // xauRate là tỉ lệ vàng 1 USD mua được, nên cần nghịch đảo để lấy giá 1 Ounce =USD
                currentGoldPriceOunceUSD = 1.0 / xauRate;

                if (txtUpdateTime != null) {
                    String time = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault()).format(new Date());
                    txtUpdateTime.setText("Cập nhật Real-time: " + time);
                }

            } catch (Exception e) {
                currentGoldPriceOunceUSD = 0.0;
                if (txtUpdateTime != null) {
                    txtUpdateTime.setText("Lỗi API: " + e.getMessage());
                }
                Toast.makeText(MainActivity.this, "Lỗi API: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return; // Ngừng tiếp tục khi có lỗi, bỏ qua code đổ dữ liệu
            }
            if (currentGoldPriceOunceUSD > 0) {
                double baseVNDPerLuong = GoldCalculator.convertOunceUsdToLuongVnd(currentGoldPriceOunceUSD,
                        USD_TO_VND_RATE);
                DecimalFormat formatter = new DecimalFormat("#,###");

                // Vàng Thế Giới
                double sellTheGioi = baseVNDPerLuong;
                double buyTheGioi = sellTheGioi - 500000;

                // SJC đắt hơn gốc 25%. Bán ra đắt hơn Mua vào 2,000,000đ
                double sellSJC = baseVNDPerLuong * 1.25;
                double buySJC = sellSJC - 2000000;

                // PNJ đắt hơn gốc 15%. Bán ra đắt hơn Mua vào 1,500,000đ
                double sellPNJ = baseVNDPerLuong * 1.15;
                double buyPNJ = sellPNJ - 1500000;

                // Nhẫn đắt hơn gốc 10%. Bán ra đắt hơn Mua vào 1,000,000đ
                double sellNhan = baseVNDPerLuong * 1.10;
                double buyNhan = sellNhan - 1000000;

                // 24K and 18K logic
                double sell24k = baseVNDPerLuong * 1.05; // 24k Premium from GoldCalculator
                double buy24k = sell24k - 800000;
                double sell18k = baseVNDPerLuong * 0.75; // 18k Premium from GoldCalculator
                double buy18k = sell18k - 600000;

                // Đổ dữ liệu an toàn và kiểm tra null phòng trường hợp quên ánh xạ View
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
            Map<String, Double> history = new LinkedHashMap<>();
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.add(Calendar.DAY_OF_MONTH, -6);

            for (int i = 0; i < 6; i++) {
                Date date = calendar.getTime();
                String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date);
                String displayDate = dateStr;

                String urlString = "https://api.metalpriceapi.com/v1/" + dateStr + "?api_key=" + API_KEY
                        + "&base=USD&currencies=XAU";
                try {
                    URL url = new URL(urlString);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");

                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    reader.close();

                    JSONObject jsonObject = new JSONObject(result.toString());
                    if (jsonObject.has("success") && jsonObject.getBoolean("success")) {
                        double rate = jsonObject.getJSONObject("rates").getDouble("XAU");
                        history.put(displayDate, 1.0 / rate);
                    }
                } catch (Exception e) {
                }
                calendar.add(Calendar.DAY_OF_MONTH, 1);
            }

            // Include current rate for the 7th point
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            history.put(today, currentGoldPriceOunceUSD);

            return history.isEmpty() ? null : history;
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