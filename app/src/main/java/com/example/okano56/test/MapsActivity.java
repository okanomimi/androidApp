package com.example.okano56.test;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.valueOf;


public class MapsActivity extends FragmentActivity implements LocationListener{
    private static TkyJavaLibs t  ;        //自分用ライブラリ
    private static GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private static final String TAG = MapsActivity.class.getSimpleName();
    // 更新時間(目安)
    private static final int LOCATION_UPDATE_MIN_TIME = 0;
    // 更新距離(目安)
    private static final int LOCATION_UPDATE_MIN_DISTANCE = 0;

    private static final double LOCATION_ACCURACY = 30.0;  //取得するgpsの精度

    private static final double SAVING_DISTANCE = 0.002;  //この距離以上離れたら保存するようにしている
    private static final String INFO_STRING = "<<Info>>\n";  //この距離以上離れたら保存するようにしている
    private static String providerName = "" ;
    private static final int ButtonNum = 3 ;        //全ボタンの数
    private LocationManager mLocationManager;

    private MyDBHelper myDBHelper;   //to create database
    private static SQLiteDatabase db;    //database
    private String message;
    private static Marker mMarker;
    public static ArrayList markerList;
    public static ArrayList<Polyline> lineList;       //地点と地点を結ぶ
    private Location lastLocation;      //直近のポジションデータ保存用
    private boolean isSaving;        //現在セーブ中かどうかの判定用
    private static HashMap<Marker,Integer> markerHash ;   //markerにデータベースと同じidを設定できないので、これで代用
    private int viewWidth ;     //端末の画面の横サイズ保存用

    private static Integer dataId = -1  ;    //現在保存中のID
    private ArrayList<Long> oneTimeSaivingIdList  ;      //一度saveボタンを押して保存するモードに入ってる時
    private ArrayList<Circle> saivingCircleList  ;      //現在保存しているやつのLatLong
    private ArrayList<Circle> circleList  ;      //一度saveボタンを押して保存するモードに入ってる時
    private static ArrayList<Circle> tempCircleList  ;      //リストをクリックした時のサークルリスト
    private static TextView debugText ;
    private static TextView providerText ;
    private BootstrapButton saveButton ;
    private BootstrapButton outputButton ;
    private Button deleteButton ;

    private static String finalDate = "" ;      //データ保存時に必要
    static Notification n ;   //ステータスバーに通知用
    static NotificationManager nm ;
    private static ScrollView sc ;
    private static Boolean isSaveOneTime = false;
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        t = new TkyJavaLibs(getApplicationContext()) ;     //自分用のライブラリ

        t.log("---------------onCreate") ;
        isSaving = false ;
        Intent intent = getIntent() ;
        viewWidth = intent.getIntExtra("viewWidth", 0)-100 ;        //@note 修正必要　ここでは、画面のサイズを取得
        markerHash  = new HashMap<Marker,Integer>();    //データベースのマーカーIDとマーカーリストのIDとを一致させるためのハッシュ
//        deleteDatabase("posDB") ;
        markerList = new ArrayList<Marker>();
        lineList = new ArrayList<Polyline>();
        setContentView(R.layout.activity_maps);
        myDBHelper = new MyDBHelper(this);
        db = myDBHelper.getWritableDatabase();

        oneTimeSaivingIdList = new ArrayList<Long>() ;
        saivingCircleList = new ArrayList<Circle>() ;

        circleList = new ArrayList<Circle>() ;
        mLocationManager = (LocationManager)this.getSystemService(Service.LOCATION_SERVICE);  //位置データを取る用
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

        setUpMapIfNeeded();     //google mapの設定

//        printCircles() ;    //保存されているデータを点表示する

        //直近に取得したGPSデータを取得する。
        Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        lastLocation = location ;

        //デバック用のテキスト
        debugText = (TextView) findViewById(R.id.debugText) ;
        debugText.setWidth(debugText.getWidth());
        debugText.setHeight(debugText.getHeight());
        debugText.setText(INFO_STRING);

        //現在のprovider名を通知するようのテキスト
        providerText = (TextView) findViewById(R.id.providerText) ;
        providerText.setWidth(providerText.getWidth());
        providerText.setHeight(providerText.getHeight());

        //ステータスバー関係
        PendingIntent pending = PendingIntent.getActivity(this, 0, intent, 0) ;
        n = new Notification.Builder(this)
                .setContentTitle("記録中")
                .setContentText("旅記録により記録中")
                .setSmallIcon(R.drawable.pin66)
                .setAutoCancel(true)
                .setOngoing(true)
                .setContentIntent(pending)
                .build() ;
//        n = new Notification();   //ステータスバーに通知用
//        n.icon = R.drawable.pin66  ;
//        n.tickerText = "記録中" ;
//        n.flags = Notification.FLAG_AUTO_CANCEL ;
        nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE) ;

        //「データを保存する」ボタン
        saveButton = (BootstrapButton) findViewById(R.id.saveMapData);  //
//        saveButton.setTextSize(t.resizeFontInButton(saveButton, viewWidth / ButtonNum)) ;


        //一時停止した時に保存したデータを読み取って呼びだす
        try {
            oneTimeSaivingIdList = new ArrayList<>() ;
            long[] savedOneTime = savedInstanceState.getLongArray("test");
            for (long d : savedOneTime) {
                oneTimeSaivingIdList.add(Long.valueOf(d));
                t.log(t.to_s(Long.valueOf(d)));
            }
            isSaving = savedInstanceState.getBoolean("isSaving");
            if (isSaving)
                t.log("保存開始してる") ;
            else
                t.log("no") ;


        }catch (NullPointerException e){
            t.log("Null") ;
        }

        saveButton.setOnClickListener(new OnClickListener() {
                                          @Override
                                          public void onClick(View v) {
                                              if (isSaving) {  //保存終了ボタンが押されたら
                                                  saveOneTimeSave();
                                              } else {   //保存ボタンが押されたら
                                                  isSaving = true;
//                                                  t.log("START:::"+providerName);
                                                  oneTimeSaivingIdList = new ArrayList<Long>();
                                                  t.toast("保存開始");
                                                  saveButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.saving_botton));
                                                  saveButton.setText("保存終了");
                                                  Cursor id = db.rawQuery("select max(dataId) as dataId from posDB ;", null) ;
//                                                  t.log(t.to_s(id.getPosition())) ;
//                                                  t.log(t.to_s(id.getColumnCount())) ;
                                                  if(id.getColumnCount() <= 0){
                                                      dataId = 0 ;
                                                  }else {
                                                      id.moveToFirst() ;
//                                                      String d = id.getInt(id.getColumnIndex("dataId"));
                                                      dataId = id.getInt(id.getColumnIndex("dataId"));
//                                                      dataId = Integer.parseInt(d) ;
//                                                      t.log(t.to_s(dataId)) ;
                                                      dataId += 1 ;
                                                  }
                                                  nm.notify(1, n);
                                                  gpsTextCheck(providerName)     ;

                                              }
                                          }
                                      }
        );


        //マップデータを表示するボタンの実装
        outputButton = (BootstrapButton) findViewById(R.id.openMapData);
//        outputButton.setTextSize(t.resizeFontInButton(outputButton, viewWidth / ButtonNum)) ;
        outputButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment alertDialog = new ListEachDateDialog() ;
                alertDialog.show(getFragmentManager(), "") ;
            }
        });

        //データベースのすべてのデータを削除する
//        deleteButton = (Button) findViewById(R.id.deleteButton);
//        deleteButton.setTextSize(t.resizeFontInButton(deleteButton, viewWidth / ButtonNum)) ;
//        deleteButton.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                deleteAllPosDatabase();   //delete posDB's data
//                deleteMarkerList();   //delete markers
//                deleteLineList(lineList);
//                message = "データベースを削除しました";
//                t.toast(message);
//            }
//        });

        //GPSが有効かどうかの判定
        gpsStartUp() ;
        //以下、広告
        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        try{
            SharedPreferences pref = getPreferences(MODE_PRIVATE);
            dataId = pref.getInt("dataId", 0);
        }catch(Exception e){


        }
    }

    /**
     * gpsが起動していなければ、起動するようにする
     */
    private void gpsStartUp(){
        String providers =
                android.provider.Settings.Secure.getString(
                        getContentResolver(),
                        Settings.Secure.
                                LOCATION_PROVIDERS_ALLOWED) ;
        if (providers.indexOf("gps", 0) == -1){
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS) ;
            startActivity(intent);
        }

    }

    /**
     *  データベースに保存してある地点をサークル表示する
     */
    private void printCircles() {
        Cursor c = db.query("posDB",null, null, null, null, null, null);
        circleList = new ArrayList<Circle>();   //マーカーの初期化
        //items[i]の日付を持つデータをデータベースから取り出す
        c.moveToFirst() ;
        while(c.moveToNext()) {
            String lat = c.getString(c.getColumnIndex("lat"));
            String lot = c.getString(c.getColumnIndex("lot"));
            LatLng location = new LatLng(Float.valueOf(lat).floatValue(), Float.valueOf(lot).floatValue());
            Circle circle = mMap.addCircle(new CircleOptions()
                            .center(location)
                            .radius(1.0)
                            .fillColor(Color.BLACK)
                            .strokeColor(Color.BLACK)
            ) ;
            circleList.add(circle) ;
        }
    }

    /**
     *  データベースに保存してある地点をサークル表示する
     */
    private void printCircles(ArrayList<LatLng> circleList) {
        //items[i]の日付を持つデータをデータベースから取り出す
        for (LatLng l : circleList) {
            Circle circle = mMap.addCircle(new CircleOptions()
                            .center(l)
                            .radius(4.0)
                            .fillColor(Color.RED)
                            .strokeColor(Color.RED)
            );
        }
    }

    /**
     * 表示してあるサークルを削除するメソッド
     * @param circleList 消したいサークルリスト
     */
    private static void deleteCircles(ArrayList<Circle> circleList){
        if (circleList == null)
            return;

        for(Circle circle: circleList){
            circle.remove();
        }
    }

    /**
     *  各マーカーに表示するかしないかを設定
     * @param isVisible マーカーを表示するかしないか
     */
    private static void setMarkerListVisible(Boolean isVisible){
        for(int i = markerList.size() -1 ; i >= 0;i--){
            Marker marker = (Marker)markerList.get(i);
            marker.setVisible(isVisible);    //各マーカーの表示
        }
    }

    /**
     *  指定したマーカーIDをもつマーカーを表示するかしなかの設定
     * @param isVisible 表示するかしないか
     * @param markerId マーカーのID
     */
    private static void setMarkerVisible(Boolean isVisible, String markerId){
        for(int i = markerList.size() -1 ; i >= 0;i--){
            Marker marker = (Marker)markerList.get(i);
            if (marker.getId().equals(markerId)) {
                marker.setVisible(isVisible);    //各マーカーの表示
            }
        }
    }

    /**
     *  マーカーのinfoWindowを非表示にする
     */
    private  static void hideInfoWindows(){
        for(int i = markerList.size() -1 ; i >= 0;i--){
            Marker marker = (Marker)markerList.get(i);
            marker.hideInfoWindow();
        }
    }

    /**
     * ２つの地点のユークリッド距離を返す
     * @param location
     * @param location2
     * @return
     */
    protected static double getDistance(Location location,Location location2) {
        double x = location.getLatitude() ;
        double y = location.getLongitude() ;
        double x2 = location2.getLatitude() ;
        double y2 = location2.getLongitude() ;
        double distance =  Math.sqrt((x2 - x) * (x2 - x) + (y2 - y) * (y2 - y));
        return distance;
    }

    /**
     * ２つの地点のユークリッド距離を返す
     * @param location
     * @param location2
     * @return
     */
    protected static double getDistance(LatLng location,LatLng location2) {
        double x = location.latitude ;
        double y = location.longitude ;
        double x2 = location2.latitude ;
        double y2 = location2.longitude ;
        double distance =  Math.sqrt((x2 - x) * (x2 - x) + (y2 - y) * (y2 - y));
//        double distance = SphericalUtil.co

        return distance;
    }

    /**
     * ２つの地点のユークリッド距離を返す
     * 公式APIを使用したバージョン
     * @param location
     * @param location2
     * @return
     */
    protected static void getDistance(LatLng location,LatLng location2, float[] results) {
        Location.distanceBetween(location.latitude,
                location.longitude, location2.latitude, location2.longitude, results);

    }

    /**
     * ２つの地点のユークリッド距離を返す
     * 公式APIを使用したバージョン
     * @param location
     * @param location2
     * @return
     */
    protected static void getDistance(Location location,Location location2, float[] results) {
        Location.distanceBetween(location.getLatitude(),
                location.getLongitude(), location2.getLatitude(), location2.getLongitude(), results);

    }
    /**
     * delete marker list content
     */
    private void deleteMarkerList(){
        for(int i = markerList.size() -1 ; i >= 0;i--){
            Marker marker = (Marker)markerList.get(i);
            marker.remove();
        }
    }

    /**
     * delete posDB's datas
     */
    private void deleteAllPosDatabase(){
        Cursor c = db.query("posDB",null, null, null, null, null, null);
        String id;
        while(c.moveToNext()) {
            id= c.getString(c.getColumnIndex("_id"));
            db.delete("posDB", "_id=\""+id+"\"", null);
        }
    }

    /**
     * 点と点を線でつなぐ
     * @param markerList つなげたいマーカーリスト
     */
    private static void drawLines(ArrayList<Marker> markerList){
        if (markerList == null)
            return ;

        int markerNum = 0 ;
        deleteCircles(tempCircleList) ;
        tempCircleList = new ArrayList<Circle>() ;
        lineList = new ArrayList<Polyline>() ;

        LatLng from = null ;        LatLng to = null ;
        Circle circle = null ;
        for(Marker marker: markerList){
            if (markerNum == 0){    //スタート地点のマーカーなら
                from = marker.getPosition()  ;
                circle = createCircle(marker.getPosition(), 4.0, Color.YELLOW) ;
            }else{
                to = marker.getPosition() ;
                Polyline line = mMap.addPolyline(new PolylineOptions()
                                .add(from, to)
                                .width(4)
                                .color(Color.GREEN).
                                        geodesic(true)
                ) ;
//                circle = createCircle(marker.getPosition(), 4.0, Color.BLUE) ;
                lineList.add(line) ;
                from = to ;
            }
            markerNum++ ;
            if (markerNum == markerList.size()){       //最後マーカなら
                circle = createCircle(marker.getPosition(), 4.0, Color.GREEN) ;
            }
            tempCircleList.add(circle) ;    //一時的にマーカーを保存しておく
        }
    }

    /**
     * サークルオブジェクトを作成して返す
     * @param latlong
     * @param radius
     * @param color
     * @return
     */
    private static Circle createCircle(LatLng latlong, double radius, int color){
        Circle circle = mMap.addCircle(new CircleOptions()
                        .center(latlong)
                        .radius(radius)
                        .fillColor(color)
                        .strokeColor(color)
        ) ;
        return circle ;
    }

    /**
     * 表示してあるラインを消す
     * @param lineList 消す対象のラインリスト
     */
    private static void deleteLineList(ArrayList<Polyline> lineList){
        for(Polyline line: lineList){
            line.setVisible(false);
            line.remove();
        }
        lineList.clear() ;
    }

    /**
     * 表示してあるラインを消す
     */
    private static void deleteLineList(){
        for(Polyline line: lineList){
//            line.setVisible(false);
            line.remove();
        }
        lineList.clear() ;
    }

    /**
     * マーカーの削除用のメソッド
     * @param markerList 削除対象のマーカーが入っているリスト
     * @param dataId    削除対象マーカーID
     */
    private static void deleteMarker(ArrayList<Marker> markerList, int dataId){
        Marker marker = getMarkerById(dataId);
        markerList.remove((markerList.indexOf(marker))) ;       //リストから削除
        marker.remove();
        db.delete("posDB", "_id=\"" + String.valueOf(dataId) + "\"", null);
    }

    /**
     * windowのタイトルに何か記入されていれば、マーカー表示
     * @param markerList
     */
    private static void displayMarkerWindow(ArrayList<Marker> markerList){
        for(Marker marker: markerList){
            if (!marker.getTitle().equals("empty"))
                marker.setVisible(true);
        }
    }

    /**
     * create marker options
     * @param location
     * @param name
     * @param memo
     * @param icon
     * @return markerOptions
     */
    private static MarkerOptions createMarkerOptions(LatLng location, String name, String memo, BitmapDescriptor icon){
        MarkerOptions options = new MarkerOptions();
        options.position(location);
        options.title(name);
        if (icon != null) {
            options.icon(icon);
        }
        options.snippet(memo);
        options.visible(false) ;    //この段階では非表示

        return options ;
    }

    /**
     * saveボタンからfinishまでのデータにタイトルをつける
     */
    private void saveOneTimeSave(){
        LayoutInflater inflater = this.getLayoutInflater() ;
        final View layout = inflater.inflate(  R.layout.save_pos_data,null);
        AlertDialog.Builder inputTitle = new AlertDialog.Builder(this)  ;
        inputTitle.setTitle("タイトル入力") ;
        inputTitle.setView(layout) ;
        inputTitle.setPositiveButton("OK", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                EditText id
                        = (EditText) layout.findViewById(R.id.edit_text);
//                EditText pass
//                        = (EditText) layout.findViewById(R.id.edit_text2);
                String title = id.getText().toString();

                Cursor c = db.rawQuery("select * from posDB where dataId in(\"" + t.to_s(dataId) + "\") ;", null);
                //c.moveToFirst() ;
                //以下変えたほうがいいかも
                while(c.moveToNext()) {
                    ContentValues values = new ContentValues();
                    String id_1 = c.getString(c.getColumnIndex("_id"));
                    String lat = c.getString(c.getColumnIndex("lat"));
                    String lot = c.getString(c.getColumnIndex("lot"));
                    String name = c.getString(c.getColumnIndex("posName"));
                    String memo = c.getString(c.getColumnIndex("posMemo"));
                    String date = c.getString(c.getColumnIndex("date"));
                    String time = c.getString(c.getColumnIndex("time"));
                    String _dataId = c.getString(c.getColumnIndex("dataId"));

                    values.put("lat", valueOf(lat));
                    values.put("lot", valueOf(valueOf(lot)));
                    values.put("posName", valueOf(name));
                    values.put("posMemo", valueOf(memo));
                    values.put("date", valueOf(date));
                    values.put("dataId", valueOf(_dataId));
                    values.put("time", valueOf(time));
                    values.put("title", valueOf(_dataId) + ":" + valueOf(title) + ":" + finalDate);

                    db.update("posDB", values, "_id=\"" + String.valueOf(id_1) + "\"", null);
                }

                //保存してあるデータにタイトルをつける
//                for (Long oneTimeId : oneTimeSaivingIdList) {
////                    Cursor c = db.rawQuery("select * from posDB where _id in(\"" + t.to_s(oneTimeId) + "\") ;", null);
//                    Cursor c = db.rawQuery("select * from posDB where _id in(\"" + t.to_s(oneTimeId) + "\") ;", null);
//                    c.moveToFirst();
//                    ContentValues values = new ContentValues();
////                    String id_1 = c.getString(c.getColumnIndex("_id"));
//                    String lat = c.getString(c.getColumnIndex("lat"));
//                    String lot = c.getString(c.getColumnIndex("lot"));
//                    String name = c.getString(c.getColumnIndex("posName"));
//                    String memo = c.getString(c.getColumnIndex("posMemo"));
//                    String date = c.getString(c.getColumnIndex("date"));
//                    String time = c.getString(c.getColumnIndex("time"));
//                    String _dataId = c.getString(c.getColumnIndex("dataId"));
//
//                    values.put("lat", valueOf(lat));
//                    values.put("lot", valueOf(valueOf(lot)));
//                    values.put("posName", valueOf(name));
//                    values.put("posMemo", valueOf(memo));
//                    values.put("date", valueOf(date));
//                    values.put("dataId", valueOf(_dataId));
//                    values.put("time", valueOf(time));
//                    values.put("title", valueOf(_dataId) + ":" + valueOf(title) + ":" + finalDate);
//
//                    db.update("posDB", values, "_id=\"" + String.valueOf(oneTimeId) + "\"", null);
//                }

                isSaveOneTime = true;
                isSaving = false;
                t.toast("保存終了");
                saveButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.normal_button));
                lastLocation = null;
                saveButton.setText("保存開始");
                nm.cancel(1);
                gpsTextCheck(providerName);

//                t.log("SS"+valueOf(isSaveOneTime)) ;
            }
        });
        inputTitle.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Cancel ボタンクリック処理
//                db.delete("posDB", "dataId=\"" + String.valueOf(dataId) + "\"", null);

                isSaveOneTime = false;
            }
        });
        // 表示
        inputTitle.create().show();
    }

    /**
     * posデータをDBに格納する
     * @param location
     * @param name
     * @param memo
     */
    private void insertPosDataToDB(Location location, String name, String memo){
        Date date = new Date() ;
        DateFormat df = new SimpleDateFormat("yyyy/MM/dd/HH:mm") ;
        DateFormat df2 = new SimpleDateFormat("yyyy/MM/dd/") ;
        ContentValues values = new ContentValues();
        values.put("lat", valueOf(location.getLatitude()));
        values.put("lot", valueOf(location.getLongitude()));
        values.put("posName", valueOf(name));
        values.put("posMemo", valueOf(t.to_s(location.getAccuracy())));
        values.put("date", valueOf(df2.format(date)));
        values.put("time", valueOf(df.format(date)));
        values.put("title", valueOf(df2.format(date)));
        values.put("dataId", valueOf(dataId));
        finalDate = valueOf(df2.format(date)) ;
        oneTimeSaivingIdList.add(db.insert("posDB", null, values));     //idを保存していく

//        debugText.append(t.to_s(location.getAccuracy())+"\n");
        //日付とタイトルデータを入れる。ここではタイトルは日付と同じにする
        //その日付データが存在していなければ、以下を実行する.もしかしたらいらないかも
        if (!isContationInDB(db, "titleDB", "date", valueOf(df.format(date)))) {
            ContentValues valuesOfTitle = new ContentValues() ;
            valuesOfTitle.put("date", valueOf(df.format(date)));
            valuesOfTitle.put("title", valueOf(df.format(date)));
            db.insert("titleDB", null, valuesOfTitle);
        }
    }

    /**
     * データベースに引数データ存在しているかを確認するメソッド
     * @param  database
     * @param  table
     * @param  column
     * @param  recordName
     */
    public boolean isContationInDB(SQLiteDatabase database,  String table, String column, String recordName){
        Cursor c = database.rawQuery("select * from "+table+" where "+column+" in(\"" + recordName+ "\") ;", null) ;
        if (c.getCount() <= 0)      //データがデータベースに存在しなければ
            return false ;

        return true ;
    }

    @Override
    protected void onStop(){
        gpsCheck();
        t.log("-----------------------onStop");
        super.onStop();
    }



    /**
     * 「戻る」ボタンを押した時の処理
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if (keyCode == KeyEvent.KEYCODE_BACK){
            new AlertDialog.Builder(this)
                    .setTitle("アプリケーションの終了")
                    .setMessage("アプリケーションを終了してよろしいですか？")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // TODO 自動生成されたメソッド・スタブ
                            MapsActivity.this.finish() ;
//                            finish() ;
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // TODO 自動生成されたメソッド・スタブ

                        }
                    })
                    .show();
            return true;
        }
        return false;
    }

    @Override
    protected void onRestart() {
        t.log("----------OnRestart");
        gpsCheck();
        super.onRestart();
    }

    @Override
    protected void onResume() {
        t.log("----------OnResume");
        gpsCheck();
        super.onResume();


    }
    @Override
    protected void onPause() {
        t.log("----------OnPause");

        gpsCheck();
        super.onPause();
        SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();

        editor
                .clear()
                .putInt("dataId", dataId)
                .commit();


    }

    /**
     * メモリ不足によりアプリが消去がされた時に呼び出される処理
     * @param outState
     */
    @Override
    protected void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);

        t.log("----------------onSaveInstanceState") ;

        gpsCheck();
        outState.putBoolean("isSaving", isSaving);
        long[] test = new long[oneTimeSaivingIdList.size()+1];
        //int i = 0 ;
        //for(Long t : oneTimeSaivingIdList) {
        for(int t = 0 ;t < oneTimeSaivingIdList.size() ;t++) {
            //test[i] = Long.valueOf(t) ;
            test[t] = oneTimeSaivingIdList.get(t) ;
         //   i++ ;
        }

        outState.putLongArray("test", test);
        outState.putInt("dataId",dataId);
        outState.putString("finalDate",finalDate);
    }


    /**
     復帰後の処理
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);
        oneTimeSaivingIdList = new ArrayList<>() ;

        t.log("------------onResoterInstanceState") ;
        gpsCheck();

        long[] savedOneTime = savedInstanceState.getLongArray("test") ;
        //for(long d:savedOneTime){
        for(int d=0 ;d < savedOneTime.length ; d++){
            //oneTimeSaivingIdList.add(Long.valueOf(d)) ;
            oneTimeSaivingIdList.add(savedOneTime[d]) ;
            t.log(t.to_s(Long.valueOf(d))) ;
        }
        finalDate= savedInstanceState.getString("finalDate") ;
        isSaving = savedInstanceState.getBoolean("isSaving") ;
        dataId= savedInstanceState.getInt("dataId");
        if (isSaving)
            saveButton.setText("保存終了") ;
        else
            saveButton.setText("保存開始") ;


    }

    @Override
    protected void onStart() {
        gpsCheck();

        t.log("-----------------------onStart");
        super.onStart();
    }


    /**
     * 完全にプログラムが終了するときに呼び出される
     */
    @Override
    protected void onDestroy(){

        t.log("-----------------------onDestroy");
        //ここは必要か微妙
        if (isSaving){
            isSaving = false ;
            lastLocation = null ;
        }

        nm.cancel(1);
        mMap = null ;
        super.onDestroy();
    }


    // Called when the location has changed.
    @Override
    public void onLocationChanged(Location location) {
        gpsCheck();
        //@note あとで住所を入力できるように
        if (isSaving) {         //保存モードであれば
            if (location.getAccuracy() < LOCATION_ACCURACY) {   //ある程度gps精度が高ければ
                if (lastLocation != null) {
//                debugText.setText("test");
                    if (getDistance(lastLocation, location) > SAVING_DISTANCE) {  //座標のズレが誤差以上であれば保存
                        //
                        insertPosDataToDB(location, "empty", "");
                        lastLocation = location;       //直近のロケーションデータを更新
                        Circle circle = createCircle(new LatLng(location.getLatitude(), location.getLongitude()), 4.0, Color.BLUE);
                        saivingCircleList.add(circle);
                    }
                } else {
                    lastLocation = location;       //直近のロケーションデータを更新
                    insertPosDataToDB(location, "empty", "");
                    Circle circle = createCircle(new LatLng(location.getLatitude(), location.getLongitude()), 4.0, Color.BLUE);
                    saivingCircleList.add(circle);
                }
            }
        }
    }

    /**
     * 地点を取得するようのproviderが変更されたら、locationManagerの設定も変更する
     * @param provider
     * @param status
     * @param extras
     */
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Toast.makeText(getApplicationContext(), provider, Toast.LENGTH_LONG).show();
        mLocationManager.requestLocationUpdates(
                provider,
                LOCATION_UPDATE_MIN_TIME,
                LOCATION_UPDATE_MIN_DISTANCE,
                this);

        providerName = provider ;
        gpsTextCheck(provider) ;
    }

    public void gpsCheck() {
        Criteria criteria = new Criteria();
//        criteria.setAccuracy(Criteria.ACCURACY_LOW) ;//精度の指定
//        criteria.setAccuracy(Criteria.NO_REQUIREMENT) ;//精度の指定
        criteria.setAccuracy(Criteria.ACCURACY_FINE);//精度の指定
//        criteria.setPowerRequirement(Criteria.POWER_LOW); //消費電力の設定
        String provider = mLocationManager.getBestProvider(criteria, true);
        providerName = provider ;
        if (isSaving) {
            if (provider.equals("gps")) {
                providerText.setTextColor(Color.BLACK); //初期のカラー設定にする
                providerText.setText("<<gps>>\ngpsは有効です.");
                providerText.setBackgroundColor(Color.parseColor("#66CCFF"));
            } else {
                providerText.setBackgroundColor(Color.RED);
                providerText.setTextColor(Color.BLACK);
//            providerText.setText("no gps");
                providerText.setText("<<gps>>\ngpsを有効にしてください.");
            }
        }else{
            providerText.setTextColor(Color.BLACK); //初期のカラー設定にする
            providerText.setText("<<gps>>");
            providerText.setBackgroundColor(Color.parseColor("#66CCFF"));
        }

    }

    public void gpsTextCheck(String provider){
//        t.log("----------OnStart");

        if (isSaving) {
            if (provider.equals("gps")) {
                providerText.setTextColor(Color.BLACK); //初期のカラー設定にする
                providerText.setText("<<gps>>\ngpsは有効です.");
                providerText.setBackgroundColor(Color.parseColor("#66CCFF"));
            } else {
                providerText.setBackgroundColor(Color.RED);
                providerText.setTextColor(Color.BLACK);
//            providerText.setText("no gps");
                providerText.setText("<<gps>>\ngpsを有効にしてください.");
            }
        }else{
            providerText.setTextColor(Color.BLACK); //初期のカラー設定にする
            providerText.setText("<<gps>>");
            providerText.setBackgroundColor(Color.parseColor("#66CCFF"));
        }

//        providerText.setTextSize(t.resizeFontInButton(providerText));
    }
    @Override
    public void onProviderEnabled(String provider) {
        if (isSaving) {
            if (provider.equals("gps")) {
                providerText.setTextColor(Color.BLACK); //初期のカラー設定にする
                providerText.setText("<<gps>>\ngpsは有効です.");
                providerText.setBackgroundColor(Color.parseColor("#66CCFF"));
            } else {
                providerText.setBackgroundColor(Color.RED);
                providerText.setTextColor(Color.BLACK);
//            providerText.setText("no gps");
                providerText.setText("<<gps>>\ngpsを有効にしてください.");
            }
        }else{
            providerText.setTextColor(Color.BLACK); //初期のカラー設定にする
            providerText.setText("<<gps>>");
            providerText.setBackgroundColor(Color.parseColor("#66CCFF"));
        }
//        providerText.setTextSize(t.resizeFontInButton(providerText));
    }

    @Override
    public void onProviderDisabled(String provider) {
        if (isSaving) {
            if (provider.equals("gps")) {
                providerText.setTextColor(Color.BLACK); //初期のカラー設定にする
                providerText.setText("<<gps>>\ngpsは有効です.");
                providerText.setBackgroundColor(Color.parseColor("#66CCFF"));
            } else {
                providerText.setBackgroundColor(Color.RED);
                providerText.setTextColor(Color.BLACK);
//            providerText.setText("no gps");
                providerText.setText("<<gps>>\ngpsを有効にしてください.");
            }
        }else{
            providerText.setTextColor(Color.BLACK); //初期のカラー設定にする
            providerText.setText("<<gps>>");
            providerText.setBackgroundColor(Color.parseColor("#66CCFF"));
        }

//        providerName = provider ;
    }

    public void onGpsStatusChanged(int event){

    }

    private void showMessage(String message) {
//        TextView textView = (TextView)findViewById(R.id.message);
//        textView.setText(message);
    }

    private void showProvider(String provider) {
//        TextView textView = (TextView)findViewById(R.id.provider);
//        textView.setText("Provider : " + provider);
    }

    private void showNetworkEnabled(boolean isNetworkEnabled) {
//        TextView textView = (TextView)findViewById(R.id.enabled);
//        textView.setText("NetworkEnabled : " + valueOf(isNetworkEnabled));
    }

    //マーカーリストの中で始点と始点から最も遠いマーカーとの距離を返す
    private static float getMaxLengthBetweenFirstMarker(ArrayList markerList){
        Marker firstMarker =(Marker)(markerList.get(0)) ;
        LatLng firstMarkerPos = firstMarker.getPosition() ;
        float maxLength = 0 ;

        for(Object marker: markerList){
            if (getDistance(((Marker)marker).getPosition(), firstMarkerPos) > maxLength) {
                maxLength = (float)getDistance(((Marker) marker).getPosition(), firstMarkerPos);
            }
        }
        return maxLength ;

    }


    //マーカーリストの中で始点と始点から最も遠いマーカーとの距離を返す
    private static LatLng getFarthestMarkerFromFirstMarker(ArrayList markerList){
        Marker firstMarker =(Marker)(markerList.get(0)) ;
        LatLng firstMarkerPos = firstMarker.getPosition() ;
        Double maxLength = 0.0 ;
        LatLng farthestMarker = null ;

        for(Object marker: markerList){
            if (getDistance(((Marker)marker).getPosition(), firstMarkerPos) > maxLength) {
                maxLength = getDistance(((Marker) marker).getPosition(), firstMarkerPos);
                farthestMarker =((Marker) marker).getPosition() ;
            }
        }
        return farthestMarker;

    }

    /**
     * カメラのポジションを指定したマーカーの場所にする
     * @param
     */
    private static void moveCameraPos(ArrayList markerList){
        LatLng firstMarkerLatLng = ((Marker)markerList.get(0)).getPosition() ;
        LatLngBounds.Builder latLngBound = new LatLngBounds.Builder() ;
        for(Object marker: markerList) {
            latLngBound.include(((Marker) marker).getPosition())  ;
        }
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(latLngBound.build(), 70);
        mMap.moveCamera(cu) ;
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */

    //this method create map instance . maybe
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {

        Criteria criteria = new Criteria() ;
//        criteria.setAccuracy(Criteria.ACCURACY_LOW) ;//精度の指定
//        criteria.setAccuracy(Criteria.NO_REQUIREMENT) ;//精度の指定
        criteria.setAccuracy(Criteria.ACCURACY_FINE) ;//精度の指定
//        criteria.setPowerRequirement(Criteria.POWER_LOW); //消費電力の設定
        String provider = mLocationManager.getBestProvider(criteria, true) ;

        // 取得したロケーションプロバイダを表示
        t.toast("Provider: " + provider);

        //3Gやwifiから位置情報を取得できるかどうか
        mLocationManager.requestLocationUpdates(
//                    LocationManager.GPS_PROVIDER,
                provider,
                LOCATION_UPDATE_MIN_TIME,
                LOCATION_UPDATE_MIN_DISTANCE,
                this);

        providerName = provider ;   //あとで位置情報取得方法を描画するため

        final CameraPosition pos = new CameraPosition(new LatLng(35.7189, 139.7539),7, 0, 0) ;
        CameraUpdate camera = CameraUpdateFactory.newCameraPosition(pos) ;
        mMap.moveCamera(camera);

        mMap.setMyLocationEnabled(true);  //display data on the map

        //マーカーをクリックした時の処理
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                marker.hideInfoWindow();
                String name = marker.getTitle();
                Toast.makeText(getApplicationContext(), name, Toast.LENGTH_LONG).show();
                return false;
            }
        });

        //infowindowクリックした際の処理
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                //ダイアログを表示
                DialogFragment alertDlg = MyDialogFragment.newInstance(marker);
                alertDlg.show(getFragmentManager(), "test");
            }
        });

        //マップをタッチした時の処理
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                setMarkerListVisible(false);
                if (markerList.size() > 0){     //現在マーカーが表示されていれば
                    //リストの中のマーカーとクリックされたところの近いところを表示していく
                    outputMarkersNearClinck(latLng) ;

                }
            }
        });


    }

    /**
     * 引数の位置と近いところにあるワーカーを表示
     * @param clickPos
     */
    private void outputMarkersNearClinck(LatLng clickPos){
        for(Object m: markerList){
            LatLng mLatLng = ((Marker)m).getPosition() ;
//            t.toast(t.to_s(getDistance(mLatLng, clickPos))+", "+t.to_s(mMap.getCameraPosition().zoom)) ;
            if (getDistance(mLatLng, clickPos) < 0.003){      // @todo 距離について
//                t.toast(t.to_s(getDistance(mLatLng, clickPos))+", "+t.to_s(mMap.getCameraPosition().zoom)) ;
                ((Marker) m).setVisible(true);
                for (Object mm: markerList){
                    Marker marker = ((Marker)mm) ;
                    if (!marker.getTitle().equals("empty")) {
                        marker.setIcon(BitmapDescriptorFactory.defaultMarker(200)) ; // マーカーの色変更
                        marker.setVisible(true);
                    }
                }

            }
        }
    }

    /**
     「データ表示」を押した時のリストを出すクラス
     */
    public static class ListEachDateDialog extends DialogFragment{
        String dbDate;  //クリックされた日付を保存しとくよう
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState){
            Cursor c = db.query("posDB",null, null, null, null, null, null);
//            Cursor c = db.query("titleDB",null, null, null, null, null, null);        //タイトルと日付で関連付けしていたときのコード
            final CharSequence[] items = createItem(c) ;
//            final HashMap<CharSequence, String> itemsHash = createItemHash(c) ;
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()) ;
            builder.setItems(items, new DialogInterface.OnClickListener() {
                Cursor c = db.query("posDB",null, null, null, null, null, null);
                //                Cursor c = db.query("titleDB",null, null, null, null, null, null);   //タイトルと日付で関連付けしていたときのコード
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    float distance = 0 ;    //距離
                    LatLng lastLocation = null ;  //距離測定用

                    setMarkerListVisible(false);
                    deleteLineList(lineList);
                    markerList = new ArrayList<Marker>();   //マーカーの初期化
                    markerHash = new HashMap<Marker, Integer>() ;
//                    String dateData = itemsHash.get(items[i]) ;
                    String startTime = "" ;
                    String finishTime = "" ;
//                    int columSize = c.getColumnCount()  ;
                    int count = 0 ;
                    //items[i]の日付を持つデータをデータベースから取り出す
                    while(c.moveToNext()) {
                        if (c.getString(c.getColumnIndex("title")).equals(items[i])) {
//                            if (c.getString(c.getColumnIndex("date")).equals(dateData)) {

                            int id = c.getInt(c.getColumnIndex("_id"));
                            String lat = c.getString(c.getColumnIndex("lat"));
                            String lot = c.getString(c.getColumnIndex("lot"));
                            String posName = c.getString(c.getColumnIndex("posName"));
                            String posMemo = c.getString(c.getColumnIndex("posMemo"));
                            String time = c.getString(c.getColumnIndex("time"));
                            String date = c.getString(c.getColumnIndex("date"));
                            LatLng location = new LatLng(Float.valueOf(lat).floatValue(), Float.valueOf(lot).floatValue());
                            BitmapDescriptor icon = null ;


                            //時間関係のデータを保存
                            if (count == 0){
                                startTime = time;
                            }
                            count++ ;
                            finishTime = time;

                            if (!posName.equals("empty")) {   //タイトルが入力されていれば
//                                icon = BitmapDescriptorFactory.fromResource(R.drawable.icon_109850);      //自分の用意した画像用
                                icon = BitmapDescriptorFactory.defaultMarker(200) ;
                            }

                            //距離測定用
                            if (lastLocation != null) {
                                float[] result = {0, 0, 0};    //距離測定用
                                getDistance(lastLocation,location, result ) ;
                                distance += result[0] ;
//                                t.log("#####"+t.to_s(distance));
                            }

                            lastLocation = location ;

                            MarkerOptions options = createMarkerOptions(location, posName, time, icon);
                            mMarker = mMap.addMarker(options);

                            //もしタイトルが入力されていれば、マーカーを表示する
                            if (!posName.equals("empty"))
                                mMarker.setVisible(true);
                            else
                                mMarker.setVisible(false) ;

                            markerList.add(mMarker) ;
                            markerHash.put(mMarker, id) ;
//                          setMarkerVisible(true, id);
                        }
                    }

                    distance = distance/1000 ;
                    BigDecimal dis = new BigDecimal(distance) ;
                    dis = dis.setScale(1, BigDecimal.ROUND_HALF_UP) ;
                    debugText.setText(INFO_STRING + "約" + t.to_s(dis) + "km,   保存地点数: "+count+"\ns:"
                            +startTime+"\nf:"+finishTime);
                    debugText.setTextSize(TypedValue.COMPLEX_UNIT_PX, t.resizeFontInButton(debugText)+8);

                    drawLines(markerList) ;
//                    setMarkerListVisible(false);
                    moveCameraPos(markerList) ;

                }

            }) ;

            //onLongClickListerのための作業
            final AlertDialog builderForLongClick = builder.create() ;
            builderForLongClick.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogInterface) {
                    ListView lv = builderForLongClick.getListView() ;
                    lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                        @Override
                        public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {

                            DialogFragment alertDlg = DialogForTitleLongClick.newInstance(String.valueOf(items[i]) );
                            alertDlg.show(getFragmentManager(), "test");
                            getDialog().dismiss();

                            deleteLineList(lineList);
                            return false;
                        }
                    });
                }
            });
            return builderForLongClick;
//            return builder.create() ;
        }

        /**
         * 日付のリストを作る。
         * CharSequenceじゃないとダメみたいなのでCharSequence使う
         * @param c
         * @return
         */
        private CharSequence[] createItem(Cursor c){
            ArrayList<CharSequence> titleList = new ArrayList<CharSequence>() ;

            while(c.moveToNext()) {
                if (!titleList.contains(c.getString(c.getColumnIndex("title")))) {
                    titleList.add(c.getString(c.getColumnIndex("title")));
                }
            }

            final CharSequence[] items = new CharSequence[titleList.size()] ;
            for(int i =0  ;i <titleList.size() ;i++ ){
                items[i] =titleList.get(i) ;
            }
            return items ;
        }

        /**
         * idリスト作る
         * CharSequenceじゃないとダメみたいなのでCharSequence使う
         * @param c
         * @return
         */
        private CharSequence[] createItem2(Cursor c){
            ArrayList<CharSequence> titleList = new ArrayList<CharSequence>() ;

            while(c.moveToNext()) {
                if (!titleList.contains(c.getString(c.getColumnIndex("dataId")))) {
                    titleList.add(c.getString(c.getColumnIndex("dataId")));
                }
            }

            final CharSequence[] items = new CharSequence[titleList.size()] ;
            for(int i =0  ;i <titleList.size() ;i++ ){
                items[i] =titleList.get(i) ;
            }
            return items ;
        }

        //日付のリストを作る。CharSequenceじゃないとダメみたいなのでCharSequence使う
        private HashMap<CharSequence,String> createItemHash(Cursor c){
            ArrayList<String> dateList = new ArrayList<String>() ;
            ArrayList<CharSequence> titleList = new ArrayList<CharSequence>() ;
            HashMap<CharSequence, String> result = new HashMap<>() ;

            c.moveToFirst() ;
            dateList.add(c.getString(c.getColumnIndex("date"))) ;
            titleList.add(c.getString(c.getColumnIndex("title"))) ;
            while(c.moveToNext()) {
                dateList.add(c.getString(c.getColumnIndex("date"))) ;
                titleList.add(c.getString(c.getColumnIndex("title"))) ;
            }

            final CharSequence[] items = new CharSequence[dateList.size()] ;
            for(int i =0  ;i < dateList.size() ;i++ ){
                items[i] = titleList.get(i) ;
                result.put(items[i], dateList.get(i)) ;
            }

            return result;
        }
    }

    /**
     * markerをクリックした時のダイアログ
     */
    public static class MyDialogFragment extends DialogFragment{

        public static MyDialogFragment newInstance(Marker marker){
            MyDialogFragment myDialogFragment = new MyDialogFragment() ;
            Bundle bunlde = new Bundle() ;
            bunlde.putString("marker", marker.getTitle()) ;
            bunlde.putInt("id", markerHash.get(marker)) ;
            myDialogFragment.setArguments(bunlde);

            return myDialogFragment ;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState){
            CharSequence[] items = {"編集", "削除"} ;
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()) ;
            builder.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    switch (i) {
                        case 0:
                            Toast.makeText(getActivity(), "編集", Toast.LENGTH_LONG).show();
                            editData();
                            displayMarkerWindow(markerList);
                            break;
                        case 1:
                            Toast.makeText(getActivity(), "削除", Toast.LENGTH_LONG).show();
                            deleteData();
                            deleteLineList(lineList);   //現在描かれいるラインを消す
                            drawLines(markerList);      //もう一度ラインを絵画
                            break;
                        default:
                            break;

                    }
                    drawLines(markerList);  //ラインを書き直す
                }
            }) ;
            return builder.create() ;
        }

        /**
         *マーカーデータを編集できるメソッド
         */
        private void editData(){
            final int dataId = getArguments().getInt("id") ;
            final Marker marker = getMarkerById(dataId);


            LayoutInflater inflater = getActivity().getLayoutInflater() ;
            final View layout = inflater.inflate(  R.layout.save_pos_data,null);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("マーカーデータの編集");
            builder.setView(layout);
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    EditText posName = (EditText) layout.findViewById(R.id.edit_text);
//                    posName.setText(marker.getTitle());
//                    EditText posMemo = (EditText) layout.findViewById(R.id.edit_text2);
//                    posName.setText(marker.getSnippet());
                    String name = posName.getText().toString();
//                    String memo = posMemo.getText().toString();

//                    BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.icon_109850);
                    HashMap markerData = getMarkerDataById(dataId) ;
                    ContentValues values = new ContentValues();

//                    values.put("_id", String.valueOf(dataId)) ;
                    values.put("lat",valueOf(marker.getPosition().latitude));
                    values.put("lot",valueOf(marker.getPosition().longitude));
                    values.put("posName", valueOf(name));
                    values.put("posMemo", marker.getSnippet());
                    values.put("date", String.valueOf(markerData.get("date")));

                    db.update("posDB", values, "_id=\"" + String.valueOf(dataId) + "\"", null) ;
                    marker.hideInfoWindow();
                    marker.setTitle(name) ;
                    marker.showInfoWindow();
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            builder.create().show();

        }

        /**
         *マーカーデータを消すことができるメソッド
         */
        private void deleteData(){
            int dataId = getArguments().getInt("id") ;
            deleteMarker(markerList, dataId);

        }
    }

    /**
     * リストのタイトルを長押ししたときに呼び出される
     */
    public static class DialogForTitleLongClick extends DialogFragment{

        public static DialogForTitleLongClick newInstance(String title){
            DialogForTitleLongClick myDialogFragment = new DialogForTitleLongClick() ;
            Bundle bunlde = new Bundle() ;
            bunlde.putString("title", title) ;
            myDialogFragment.setArguments(bunlde);
            return myDialogFragment ;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState){
            CharSequence[] items = {"編集", "削除"} ;
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()) ;
            builder.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    switch (i) {
                        case 0:
                            Toast.makeText(getActivity(), "編集", Toast.LENGTH_LONG).show();
                            editData();
                            break;
                        case 1:
                            Toast.makeText(getActivity(), "削除", Toast.LENGTH_LONG).show();
                            deletePosDataByTitle(getArguments().getString("title"));

                            break;
                        default:
                            break;

                    }
//                    drawLines(markerList);  //ラインを書き直す
                }
            }) ;
            return builder.create() ;
        }

        /**
         *マーカーデータを編集することができる
         */
        private void editData(){
            final int dataId = getArguments().getInt("id") ;
            final Marker marker = getMarkerById(dataId);


            LayoutInflater inflater = getActivity().getLayoutInflater() ;
            final View layout = inflater.inflate(  R.layout.save_pos_data,null);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("タイトル編集");
            builder.setView(layout);
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    EditText posName = (EditText) layout.findViewById(R.id.edit_text);
//                    posName.setText(marker.getTitle());
//                    EditText posMemo = (EditText) layout.findViewById(R.id.edit_text2);
//                    posName.setText(marker.getSnippet());
                    String name = posName.getText().toString();
//                    String memo = posMemo.getText().toString();

//                    Cursor c = db.rawQuery("select * from posDB where _id in(\"" + getArguments().getString("title")  + "\") ;", null);

                    Cursor c = db.query("posDB",null, null, null, null, null, null);
                    c.moveToFirst() ;
                    //以下変えたほうがいいかも
                    while(c.moveToNext()) {
                        if (c.getString(c.getColumnIndex("title")).equals(getArguments().getString("title"))) {
                            int id = c.getInt(c.getColumnIndex("_id"));
                            String lat = c.getString(c.getColumnIndex("lat"));
                            String lot = c.getString(c.getColumnIndex("lot"));
                            String posName1 = c.getString(c.getColumnIndex("posName"));
                            String posMemo1 = c.getString(c.getColumnIndex("posMemo"));
                            String date = c.getString(c.getColumnIndex("date"));
                            LatLng location = new LatLng(Float.valueOf(lat).floatValue(), Float.valueOf(lot).floatValue());
                            BitmapDescriptor icon = null ;

                            ContentValues values = new ContentValues();

//                    values.put("_id", String.valueOf(dataId)) ;
                            values.put("lat", valueOf(location.latitude));
                            values.put("lot", valueOf(location.longitude));
                            values.put("posName", valueOf(posName1));
                            values.put("posMemo", valueOf(posMemo1));
                            values.put("date", String.valueOf(date));
                            values.put("title", String.valueOf(name)+":"+String.valueOf(date));

                            db.update("posDB", values, "_id=\"" + String.valueOf(id) + "\"", null);
                        }
                    }
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            builder.create().show();

        }



        private void deletePosDataByTitle(String title){

            db.delete("posDB", "title=\"" + String.valueOf(title) + "\"", null);
            deleteLineList();
//lineList.clear() ;
            mMap.clear() ;
            this.dismiss();

//            getDialog().dismiss();
        }
    }

    /**
     *
     * @param id
     * @return
     */
    private static Marker getMarkerById(int id){
        for(Map.Entry<Marker, Integer> entry : markerHash.entrySet()){
            if (entry.getValue() == id) {
                return entry.getKey();
            }
        }
        return null ;
    }

    /**
     *
     * @param id
     * @return
     */
    private static HashMap getMarkerDataById(int id){
        HashMap<String, String> markerData = new HashMap<String, String>() ;
        Cursor c = db.rawQuery("select * from posDB where _id in(\""+id+"\") ;", null) ;

        c.moveToFirst() ;
        String id_1 = c.getString(c.getColumnIndex("_id"));
        String lat = c.getString(c.getColumnIndex("lat"));
        String lot = c.getString(c.getColumnIndex("lot"));
        String posName = c.getString(c.getColumnIndex("posName"));
        String posMemo = c.getString(c.getColumnIndex("posMemo"));
        String date = c.getString(c.getColumnIndex("date")) ;

        markerData.put("_id", id_1) ;
        markerData.put("lat", lat) ;
        markerData.put("lot", lot) ;
        markerData.put("posName", posName) ;
        markerData.put("posMemo",posMemo) ;
        markerData.put("date",date) ;
        return markerData ;
    }

    /**
     *
     * @param date
     * @return
     */
    private static HashMap getTitleDataByDate(String date){
        HashMap<String, String> markerData = new HashMap<String, String>() ;
        Cursor c = db.rawQuery("select * from titleDB where date in(\"" + date + "\") ;", null) ;

        c.moveToFirst() ;
        String id_1 = c.getString(c.getColumnIndex("_id"));
        String date_1 = c.getString(c.getColumnIndex("date")) ;
        String title = c.getString(c.getColumnIndex("title")) ;

        markerData.put("_id", id_1) ;
        markerData.put("date",date_1) ;
        markerData.put("title",title) ;
        return markerData ;
    }

    /**
     * 日付リストをクリックした時に出てくるalert
     */
    public static class TitleDialog extends DialogFragment{

        public static TitleDialog newInstance(String date){
            TitleDialog  myDialogFragment = new TitleDialog() ;
            Bundle bundle= new Bundle() ;
            bundle.putString("date", date) ;
            myDialogFragment.setArguments(bundle);

            return myDialogFragment ;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState){
            CharSequence[] items = {"edit", "delete"} ;
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()) ;
            builder.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    switch (i) {
                        case 0:
                            Toast.makeText(getActivity(), "add or edit", Toast.LENGTH_LONG).show();
                            editTitle();
                            break;
                        case 1:
                            Toast.makeText(getActivity(), "delete", Toast.LENGTH_LONG).show();
//                            deleteData();
//                            deleteLineList(lineList);   //現在描かれいるラインを消す
//                            drawLines(markerList);      //もう一度ラインを絵画
                            break;
                        default:
                            break;

                    }
                }
            }) ;
            return builder.create() ;
        }

        /**
         *日付リストを長押しした時の処理
         */
        private void editTitle(){
            final String date = getArguments().getString("date") ;
            final HashMap<String, String> titleData = getTitleDataByDate(date) ;


            LayoutInflater inflater = getActivity().getLayoutInflater() ;
            final View layout = inflater.inflate(R.layout.save_pos_data, null);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("タイトル変更");
            builder.setView(layout);
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    EditText posName = (EditText) layout.findViewById(R.id.edit_text);
//                    posName.setText(marker.getTitle());
//                    EditText posMemo = (EditText) layout.findViewById(R.id.edit_text2);
//                    posName.setText(marker.getSnippet());
                    String title = posName.getText().toString();

//                    BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.icon_109850);
                    ContentValues values = new ContentValues();

                    values.put("_id", String.valueOf(titleData.get("_id"))) ;
                    values.put("date", String.valueOf(titleData.get("date")));
                    values.put("title", String.valueOf(title));

                    db.update("titleDB", values, "_id=\"" + String.valueOf(titleData.get("_id")) + "\"", null);
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            builder.create().show();

        }

        /**
         *マーカーデータを消すことができるメソッド
         */
        private void deleteData(){
            int dataId = getArguments().getInt("id") ;
            deleteMarker(markerList, dataId);
        }
    }
}



