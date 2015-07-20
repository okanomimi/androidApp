package com.example.okano56.test;

/**
 * Created by okano56 on 2015/04/28.
 */
import java.io.File;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class DBSampleA extends Activity implements View.OnClickListener{

    private Button btn;
    private EditText edittext;
    private TextView textview;
    private MyDBHelper myhelper;
    private String str;
    private static SQLiteDatabase db;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_db);

        //ボタンの生成
        btn = (Button)findViewById(R.id.AddButton);
        btn.setOnClickListener(this);

        //エディットテキストの生成
        edittext = (EditText)findViewById(R.id.AddEditText);

        //テキストビューの生成
        textview = (TextView)findViewById(R.id.MyTextView);

        //データベースヘルパーの生成
        myhelper = new MyDBHelper(this);
        db = myhelper.getWritableDatabase();

        //データベーステーブルクリア
        db.delete("testtb", null, null);

        //データベースのデータを読み取って表示する。
        Cursor c = db.query("testtb", new String[] {"_id", "comment"}, null, null, null, null, null);
        startManagingCursor(c);
        str = "データベース一覧\n";
        while(c.moveToNext()) {
            str += c.getString(c.getColumnIndex("_id")) + ":" +
                    c.getString(c.getColumnIndex("comment")) + "\n";
        }

        textview.setText(str);
    }

    //ボタンクリックイベントの処理
    public void onClick(View v) {

        //データベースに、データを登録。
        ContentValues values = new ContentValues();
        values.put("comment", edittext.getText().toString());
        db.insert("testtb", null, values);

        //データベースのデータを読み取って表示する。
        Cursor c = db.query("testtb", new String[] {"_id", "comment"}, null, null, null, null, null);
        startManagingCursor(c);
        str = "データベース一覧\n";
        while(c.moveToNext()) {
            str += c.getString(c.getColumnIndex("_id")) + ":" +
                    c.getString(c.getColumnIndex("comment")) + "\n";

        }

        textview.setText(str);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        myhelper.close();

        //データベース削除
        File file = new File("/data/data/com.sample.android.dbsamplea/databases/testdb");
        file.delete();
    }
}
