package com.example.btqt_bai1;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;

public class HistoryFragment extends Fragment {

    private ListView listViewHistory;
    private Button btnBack;
    private DatabaseHelper myDb;
    private ArrayList<HistoryRecord> historyList;

    private static class HistoryRecord {
        String id;
        String text;

        HistoryRecord(String id, String text) {
            this.id = id;
            this.text = text;
        }
    }

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
        public long getItemId(int position) {
            return position;
        }

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
                            android.widget.Toast
                                    .makeText(requireContext(), "Đã xóa lịch sử", android.widget.Toast.LENGTH_SHORT)
                                    .show();
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            });

            return convertView;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        // Nạp giao diện fragment_history.xml
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        listViewHistory = view.findViewById(R.id.listViewHistory);
        btnBack = view.findViewById(R.id.btnBack);
        myDb = new DatabaseHelper(requireContext()); // Gọi Database

        loadHistoryData();

        // Xử lý sự kiện bấm nút QUAY LẠI
        btnBack.setOnClickListener(v -> {
            // Hủy Fragment này để lùi về màn hình MainActivity
            requireActivity().getSupportFragmentManager().popBackStack();
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
}