package com.example.okano56.test;

/**
 * Created by okano56 on 2015/04/28.
 * タイトルと日付を保存しておくデータベースのヘルパー
 */
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class TitleDBHelper extends SQLiteOpenHelper {

    public TitleDBHelper(Context context) {
        //DBを作成
        super(context, "titleDB", null , 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS titleDB (_id INTEGER PRIMARY KEY AUTOINCREMENT,date TEXT,title TEXT);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
