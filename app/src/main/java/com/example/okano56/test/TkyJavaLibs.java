package com.example.okano56.test;

import android.content.Context;
import android.graphics.Paint;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
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
    /**
     * 文字列をandroidの端末毎に調整する
     *
     * @return
     */
    public float resizeFontInButton(TextView btn){

        float viewWidth = btn.getWidth() ;
        float viewHeight = btn.getHeight() ;

        float textSize =btn.getTextSize() ;

        log(to_s(textSize));
        Paint paint = new Paint();
        paint.setTextSize(textSize) ;

        Paint.FontMetrics fm = paint.getFontMetrics() ;
        float textHeight = (float)(Math.abs(fm.top))+(Math.abs(fm.descent)) ;

        float textWidth = paint.measureText(btn.getText().toString()) ;

            while (viewWidth< textWidth || viewHeight < textHeight) {
                // 横幅に収まるまでループ
                if (5.0f >= textSize) {
                    // 最小サイズ以下になる場合は最小サイズ
                    textSize = 5.0f;
                    log("test") ;
                    break;
                }
                // テキストサイズをデクリメント
                textSize--;
                // Paintにテキストサイズ設定
                paint.setTextSize(textSize);

                fm = paint.getFontMetrics() ;
                textHeight = (float)(Math.abs(fm.top))+(Math.abs(fm.descent)) ;
                textWidth = paint.measureText(btn.getText().toString()) ;
            }
        log(to_s(textSize));
        log("textH"+to_s(textHeight)+":textWI"+to_s(textWidth));
        log("maxH"+to_s(viewHeight)+":maxWI"+to_s(viewWidth));
        return textSize ;
    }
}
