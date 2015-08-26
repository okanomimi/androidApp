package com.example.okano56.test;

import android.content.Context;
import android.graphics.Paint;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import java.math.BigDecimal;

/**
 * Created by okano56 on 2015/06/07.
 * this class is my tool of google map API
 */
public class TkyJavaLibs {

    Context context ;

    //コンストラクタ
    public TkyJavaLibs(Context context){
        this.context = context ;
    }

    //コンストラクタ
    public TkyJavaLibs(){
        context = null ;
    }

    //log表示用
    public void log(String logdata){Log.e("debug", logdata)  ;
    }

    //文字列に変換用
    public String to_s(int data){
       return String.valueOf(data)  ;
    }

    //文字列に変換用
    public String to_s(float data){
       return String.valueOf(data)  ;
    }

    //文字列に変換用
    public String to_s(double data){
       return String.valueOf(data)  ;
    }

    //文字列に変換用
    public String to_s(BigDecimal data){
       return String.valueOf(data)  ;
    }

    //
    public void toast(String text) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }

    /**
     * 文字列をandroidの端末毎に調整する
     *
     * @return
     */
    public float resizeFontInButton(Button btn,float maxSize ){
        Paint paint = new Paint();

        float textSize =btn.getTextSize() ;
        paint.setTextSize(textSize) ;
        float textWidth = paint.measureText(btn.getText().toString()) ;

            while (maxSize < textWidth) {
                // 横幅に収まるまでループ
                if (10.0f >= textSize) {
                    // 最小サイズ以下になる場合は最小サイズ
                    textSize = 10.0f;
                    break;
                }
                // テキストサイズをデクリメント
                textSize--;
                // Paintにテキストサイズ設定
                paint.setTextSize(textSize);
                // テキストの横幅を再取得
                textWidth = paint.measureText(btn.getText().toString());

            }
        return textSize ;
    }
}
