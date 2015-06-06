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
        db.execSQL("CREATE TABLE posDB (_id INTEGER PRIMARY KEY,lat TEXT,lot TEXT,posName TEXT);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
