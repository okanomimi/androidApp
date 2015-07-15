package com.example.okano56.test;

import android.graphics.Paint;
import android.util.Log;
import android.widget.Button;

/**
 * Created by okano56 on 2015/06/07.
 * this class is my tool of google map API
 */
public class TkyJavaLibs {

    //log表示用
    public void log(String logdata){
        Log.e("debug", logdata)  ;
    }

    //文字列に変換用
    public String to_s(int data){
       return String.valueOf(data)  ;
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
