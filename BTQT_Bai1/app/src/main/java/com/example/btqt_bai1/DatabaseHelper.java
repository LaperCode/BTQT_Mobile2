package com.example.btqt_bai1;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "GoldTracker.db";
    private static final String TABLE_NAME = "HistoryLog";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tạo bảng lưu Lịch sử gồm: ID, Ngày giờ, và Nội dung
        String createTable = "CREATE TABLE " + TABLE_NAME + " (ID INTEGER PRIMARY KEY AUTOINCREMENT, DATETIME TEXT, RECORD_INFO TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // Hàm thêm lịch sử mới
    public boolean insertData(String dateTime, String recordInfo) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("DATETIME", dateTime);
        contentValues.put("RECORD_INFO", recordInfo);
        long result = db.insert(TABLE_NAME, null, contentValues);
        return result != -1;
    }

    // Hàm lấy toàn bộ lịch sử
    public Cursor getAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_NAME + " ORDER BY ID DESC", null);
    }
}