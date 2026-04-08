package com.example.btqt_bai1;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;

public class HistoryFragment extends Fragment {

    private ListView listViewHistory;
    private Button btnBack;
    private DatabaseHelper myDb;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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
        ArrayList<String> historyList = new ArrayList<>();

        if (res.getCount() == 0) {
            historyList.add("Chưa có dữ liệu lịch sử nào.");
        } else {
            // Đọc từng dòng dữ liệu trong DB
            while (res.moveToNext()) {
                String record = "Lúc: " + res.getString(1) + "\n" + res.getString(2);
                historyList.add(record);
            }
        }

        // Dùng ArrayAdapter mặc định của Android để vẽ danh sách
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, historyList);
        listViewHistory.setAdapter(adapter);
    }
}