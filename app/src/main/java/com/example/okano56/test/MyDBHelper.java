package com.example.okano56.test;

/**
 * Created by okano56 on 2015/04/28.
 */
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MyDBHelper extends SQLiteOpenHelper {

    public MyDBHelper(Context context) {
        //DBを作成
        super(context, "posDB", null , 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
//        db.execSQL("CREATE TABLE posDB (_id INTEGER PRIMARY KEY,lat TEXT,lot TEXT,posName TEXT,posMemo TEXT);");
//        db.execSQL("CREATE TABLE IF NOT EXISTS posDB (_id TEXT PRIMARY KEY,lat TEXT,lot TEXT,posName TEXT,posMemo TEXT, date TEXT);");
        db.execSQL("CREATE TABLE IF NOT EXISTS posDB (_id INTEGER PRIMARY KEY AUTOINCREMENT,lat TEXT,lot TEXT,posName TEXT,posMemo TEXT, date TEXT, title TEXT);");
        db.execSQL("CREATE TABLE IF NOT EXISTS titleDB (_id INTEGER PRIMARY KEY AUTOINCREMENT,date TEXT,title TEXT);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
