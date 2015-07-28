package com.example.okano56.test;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Service;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

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
    private LocationManager mLocationManager;

    private MyDBHelper myDBHelper;   //to create database
    private static SQLiteDatabase db;    //database
    private String str;
    private static Marker mMarker;
    public static ArrayList markerList;
    public static ArrayList lineList;
    private Location lastLocation;      //直近のポジションデータ保存用
    private boolean isSave = false ;
    private static HashMap<Marker,Integer> markerHash ;   //markerにデータベースと同じidを設定できないので、これで代用
    private int viewWidth ;

    private ArrayList<Long> oneTimeSaivingIdList  ;      //一度saveボタンを押して保存するモードに入ってる時
    private Button saveButton ;
    private Button outputButton ;
    private Button deleteButton ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        t = new TkyJavaLibs(getApplicationContext()) ;     //自分用のライブラリ
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
        mLocationManager = (LocationManager)this.getSystemService(Service.LOCATION_SERVICE);  //位置データを取る用
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        setUpMapIfNeeded();

        //直近に取得したGPSデータを取得する。
        Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        lastLocation = location ;


        //「データを保存する」ボタン
        saveButton = (Button) findViewById(R.id.saveMapData);  //
//        saveButton.setTextSize(resizeFont(saveButton)) ;
        saveButton.setTextSize(t.resizeFontInButton(saveButton, viewWidth/3)) ;
        saveButton.setOnClickListener(new OnClickListener() {
                                          @Override
                                          public void onClick(View v){
                                              if (isSave) {
                                                  isSave = false ;
                                                  Toast.makeText(getApplicationContext(), "保存終了", Toast.LENGTH_LONG).show();
//                                                  saveButton.setBackgroundColor(Color.WHITE);
                                                  saveButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.normal_button));

                                                  saveOneTimeSave() ;
                                                  saveButton.setText("start") ;
                                              }else{
                                                  isSave = true  ;
                                                  oneTimeSaivingIdList = new ArrayList<Long>() ;
                                                  Toast.makeText(getApplicationContext(), "保存開始", Toast.LENGTH_LONG).show();
//                                                  saveButton.setBackgroundColor(Color.GRAY);
                                                  saveButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.saving_botton));
                                                  //現在までで保存しているマーカーを保存してく
                                                  saveButton.setText("finish") ;
                                              }
                                          }
                                      }
        );


        //マップデータを表示するボタンの実装
        outputButton = (Button) findViewById(R.id.openMapData);

//        outputButton.setTextSize(resizeFont(outputButton)) ;
       outputButton.setTextSize(t.resizeFontInButton(outputButton, viewWidth / 3)) ;
//        outputButton.setBackgroundColor(Color.WHITE);
        outputButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment alertDialog = new ListEachDateDialog() ;
                alertDialog.show(getFragmentManager(), "ddd") ;
            }
        });

        //データベースのすべてのデータを削除する
        deleteButton = (Button) findViewById(R.id.deleteButton);
//        deleteButton.setTextSize(resizeFont(deleteButton)) ;
        deleteButton.setTextSize(t.resizeFontInButton(deleteButton, viewWidth / 3)) ;

//        deleteButton.setBackgroundColor(Color.WHITE);
        deleteButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteAllPosDatabase();   //delete posDB's data
                deleteMarkerList();   //delete markers
                deleteLineList(lineList);
                str = "データベースを削除しました";
                Toast.makeText(getApplicationContext(), str, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     *
     * @param isVisible
     */
    private static void setMarkerListVisible(Boolean isVisible){
        for(int i = markerList.size() -1 ; i >= 0;i--){
            Marker marker = (Marker)markerList.get(i);
            marker.setVisible(isVisible);    //各マーカーの表示
        }
    }

    /**
     *
     * @param isVisible
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
     *
     */
    private  static void hideInfoWindows(){
        for(int i = markerList.size() -1 ; i >= 0;i--){
            Marker marker = (Marker)markerList.get(i);
            marker.hideInfoWindow();
        }
    }

    /**
     *
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
     *
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

        return distance;
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
     * @param
     */
    private static void drawLines(ArrayList<Marker> markerList){
        int markerNum = 0 ;
        lineList = new ArrayList<Polyline>() ;
        LatLng from = null ;
        LatLng to = null ;
        for(Marker marker: markerList){
            if (markerNum == 0){    //スタート地点のマーカーなら
                from = marker.getPosition()  ;
//                marker.setIcon(BitmapDescriptorFactory.defaultMarker(100)) ; // スタート地点
                marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)) ; // スタート地点
                marker.setVisible(true);
            }else{
                to = marker.getPosition() ;
                Polyline line = mMap.addPolyline(new PolylineOptions()
                                .add(from, to)
                                .width(3)
                                .color(Color.RED)
                ) ;
                lineList.add(line) ;
                from = to ;
            }
            markerNum++ ;
            if (markerNum == markerList.size()){       //最後マーカなら
              marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)) ; // スタート地点
              marker.setVisible(true);
            }
        }
    }

    private static void deleteLineList(ArrayList<Polyline> lineList){
        for(Polyline line: lineList){
            line.setVisible(false);
        }
        lineList = null ;
    }

    private static void deleteMarker(ArrayList<Marker> markerList, int dataId){
        Marker marker = getMarkerById(dataId);
        markerList.remove((markerList.indexOf(marker))) ;       //リストから削除
        marker.remove();
        db.delete("posDB", "_id=\"" + String.valueOf(dataId) + "\"", null);
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
       //dialog表示、タイトルを表示
        // カスタムビューを設定

        LayoutInflater inflater = this.getLayoutInflater() ;
            final View layout = inflater.inflate(  R.layout.save_pos_data,null);
       AlertDialog.Builder inputTitle = new AlertDialog.Builder(this)  ;
       inputTitle.setTitle("タイトル入力") ;
        inputTitle.setView(layout) ;
        inputTitle.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // OK ボタンクリック処理
                // ID と PASSWORD を取得
                EditText id
                        = (EditText) layout.findViewById(R.id.edit_text);
                EditText pass
                        = (EditText) layout.findViewById(R.id.edit_text2);
                String title = id.getText().toString();

                //ここで全部処理すべし。
                for(Long oneTimeId: oneTimeSaivingIdList) {
                    Cursor c = db.rawQuery("select * from posDB where _id in(\"" + t.to_s(oneTimeId) + "\") ;", null);
                    c.moveToFirst() ;
                    ContentValues values = new ContentValues() ;

                    String id_1 = c.getString(c.getColumnIndex("_id"));
                    String lat = c.getString(c.getColumnIndex("lat"));
                    String lot = c.getString(c.getColumnIndex("lot"));
                    String name = c.getString(c.getColumnIndex("posName"));
                    String memo = c.getString(c.getColumnIndex("posMemo"));
                    String date = c.getString(c.getColumnIndex("date")) ;

                    values.put("lat",valueOf(lat));
                    values.put("lot",valueOf(valueOf(lot)));
                    values.put("posName", valueOf(name));
                    values.put("posMemo", valueOf(memo));
                    values.put("date", valueOf(date));
                    values.put("title", valueOf(title)+":"+valueOf(date));
//                    values.put("title", valueOf(title));

                    db.update("posDB", values, "_id=\"" + String.valueOf(oneTimeId) + "\"", null);
                }
            }
        });
        inputTitle.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Cancel ボタンクリック処理
            }
        });

        // 表示
        inputTitle.create().show();
       //入力されたタイトルをデータに保存していく
    }

    /**
     * posデータをDBに格納する
     * @param name
     * @param memo
     */
    private void insertPosDataToDB(Location location, String name, String memo){
        Date date = new Date() ;
        DateFormat df = new SimpleDateFormat("yyyy/MM/dd") ;
        ContentValues values = new ContentValues();
        values.put("lat",valueOf(location.getLatitude()));
        values.put("lot",valueOf(location.getLongitude()));
        values.put("posName", valueOf(name));
        values.put("posMemo", valueOf(memo));
        values.put("date", valueOf(df.format(date)));
        values.put("title", valueOf(df.format(date)));
        oneTimeSaivingIdList.add(db.insert("posDB", null, values));     //idを保存していく

        //日付とタイトルデータを入れる。ここではタイトルは日付と同じにする
        //その日付データが存在していなければ、以下を実行する
        if (!isContationInDB(db, "titleDB", "date", valueOf(df.format(date)))) {
            ContentValues valuesOfTitle = new ContentValues() ;
            valuesOfTitle.put("date", valueOf(df.format(date)));
            valuesOfTitle.put("title", valueOf(df.format(date)));
            db.insert("titleDB", null, valuesOfTitle);
        }
    }

    //データベースに引数データ存在しているかを確認するメソッド
    public boolean isContationInDB(SQLiteDatabase database,  String table, String coloum, String recordName){
        Cursor c = database.rawQuery("select * from "+table+" where "+coloum+" in(\"" + recordName+ "\") ;", null) ;
        if (c.getCount() <= 0)      //データがデータベースに存在しなければ
            return false ;

        return true ;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    // Called when the location has changed.
    @Override
    public void onLocationChanged(Location location) {

//        Toast.makeText(getApplicationContext(), "onLocationChanged", Toast.LENGTH_LONG).show();
//                Log.e(TAG, String.valueOf(getDistance(lastLocation, location)*100.0));
//        Log.e(TAG, "onStatusChanged.");
        //@note あとで住所を入力できるように
        if (isSave) {
            if (lastLocation != null) {
//                Log.e(TAG, String.valueOf(getDistance(lastLocation, location)*100.0));
                //@note あとで住所を入力できるように
                if (getDistance(lastLocation, location)*100.0 > 0.3) {  //座標のズレが誤差以上であれば保存
                    insertPosDataToDB(location, "empty", "");
                    lastLocation = location;       //直近のロケーションデータを更新
                    t.toast("位置情報の保存");
                }
            }else {
                lastLocation = location;       //直近のロケーションデータを更新
                insertPosDataToDB(location, "test", "test");
            }
        }
    }

    // Called when the provider status changed.
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Toast.makeText(getApplicationContext(), provider, Toast.LENGTH_LONG).show();
//        mLocationManager.requestLocationUpdates(
//                provider,
//                LOCATION_UPDATE_MIN_TIME,
//                LOCATION_UPDATE_MIN_DISTANCE,
//                this);
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {

    }


    private void showMessage(String message) {
        TextView textView = (TextView)findViewById(R.id.message);
        textView.setText(message);
    }

    private void showProvider(String provider) {
        TextView textView = (TextView)findViewById(R.id.provider);
        textView.setText("Provider : " + provider);
    }

    private void showNetworkEnabled(boolean isNetworkEnabled) {
        TextView textView = (TextView)findViewById(R.id.enabled);
        textView.setText("NetworkEnabled : " + valueOf(isNetworkEnabled));
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
//        float maxLength = getMaxLengthBetweenFirstMarker(markerList);
        LatLng firstMarkerLatLng = ((Marker)markerList.get(0)).getPosition() ;
//        t.log(t.to_s(maxLength));
//        LatLngBounds.Builder latLngBound = new LatLngBounds(firstMarkerLatLng, getFarthestMarkerFromFirstMarker(markerList)) ;
        LatLngBounds.Builder latLngBound = new LatLngBounds.Builder() ;
        for(Object marker: markerList) {
            latLngBound.include(((Marker) marker).getPosition())  ;
        }
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(latLngBound.build(), 70);
        mMap.moveCamera(cu) ;
//        latLngBound.including(getFarthestMarkerFromFirstMarker(markerList)) ;
//        Marker firstMarker = (Marker)markerList.get(0) ;
//        CameraPosition sydney = new CameraPosition.Builder()
//                .target(firstMarker.getPosition()).zoom(maxLength)
//                .bearing(0).tilt(25).build();
//        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(sydney));
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
        //3Gやwifiから位置情報を取得できるかどうか
        if(mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    LOCATION_UPDATE_MIN_TIME,
                    LOCATION_UPDATE_MIN_DISTANCE,
                    this);
        }
//        else {
//            mLocationManager.requestLocationUpdates(
//                    LocationManager.NETWORK_PROVIDER,
//                    LOCATION_UPDATE_MIN_TIME,
//                    LOCATION_UPDATE_MIN_DISTANCE,
//                    this);
//        }
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
                    for(Object m: markerList){
                       LatLng mLatLng = ((Marker)m).getPosition() ;
                       if (getDistance(mLatLng, latLng) < 0.001){
                            t.log(t.to_s(getDistance(mLatLng, latLng))) ;
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
            }
        });
    }

    /**
     「データ表示」を押した時のリストを出すクラス
     */
    public static class ListEachDateDialog extends DialogFragment{
        String dbDate;  //クリックされた日付を保存しとくよう
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState){
            Cursor c = db.query("posDB",null, null, null, null, null, null);
//            Cursor c = db.query("titleDB",null, null, null, null, null, null);

            final CharSequence[] items = createItem(c) ;
//            final HashMap<CharSequence, String> itemsHash = createItemHash(c) ;

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()) ;
            builder.setItems(items, new DialogInterface.OnClickListener() {
                Cursor c = db.query("posDB",null, null, null, null, null, null);
//                Cursor c = db.query("titleDB",null, null, null, null, null, null);
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    setMarkerListVisible(false);
                    deleteLineList(lineList);
                    markerList = new ArrayList<Marker>();   //マーカーの初期化
                    markerHash = new HashMap<Marker, Integer>() ;
//                    String dateData = itemsHash.get(items[i]) ;
                    //items[i]の日付を持つデータをデータベースから取り出す
                    while(c.moveToNext()) {
                        if (c.getString(c.getColumnIndex("title")).equals(items[i])) {
//                            if (c.getString(c.getColumnIndex("date")).equals(dateData)) {
                            int id = c.getInt(c.getColumnIndex("_id"));
                            String lat = c.getString(c.getColumnIndex("lat"));
                            String lot = c.getString(c.getColumnIndex("lot"));
                            String posName = c.getString(c.getColumnIndex("posName"));

                            String posMemo = c.getString(c.getColumnIndex("posMemo"));
                            LatLng location = new LatLng(Float.valueOf(lat).floatValue(), Float.valueOf(lot).floatValue());
                            BitmapDescriptor icon = null ;
                            if (!posName.equals("empty")) {   //タイトルが入力されていれば
//                                icon = BitmapDescriptorFactory.fromResource(R.drawable.icon_109850);
                                icon = BitmapDescriptorFactory.defaultMarker(200) ;
                            }
                            MarkerOptions options = createMarkerOptions(location, posName, posMemo, icon);

                            mMarker = mMap.addMarker(options);
                            //もしタイトルが入力されていれば、マーカーを表示する
                            if (!posName.equals("empty")){
                                mMarker.setVisible(true);
                            }else{
                                mMarker.setVisible(false) ;
                            }
                            markerList.add(mMarker) ;
                            markerHash.put(mMarker, id) ;
//                                setMarkerVisible(true, id);
                        }
                    }
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
                            DialogFragment alertDlg = TitleDialog.newInstance(String.valueOf(items[i]));
                            alertDlg.show(getFragmentManager(), "test");
                            return true;
                        }
                    });
                }
            });
            return builderForLongClick;
//            return builder.create() ;
        }

        //日付のリストを作る。CharSequenceじゃないとダメみたいなのでCharSequence使う
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
            CharSequence[] items = {"edit", "delete"} ;
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()) ;
            builder.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    switch (i) {
                        case 0:
                            Toast.makeText(getActivity(), "edit", Toast.LENGTH_LONG).show();
                            editData();
                            break;
                        case 1:
                            Toast.makeText(getActivity(), "delete", Toast.LENGTH_LONG).show();
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
         *マーカーデータを消すことができるメソッド
         */
        private void editData(){
            final int dataId = getArguments().getInt("id") ;
            final Marker marker = getMarkerById(dataId);


            LayoutInflater inflater = getActivity().getLayoutInflater() ;
            final View layout = inflater.inflate(  R.layout.save_pos_data,null);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("test");
            builder.setView(layout);
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    EditText posName = (EditText) layout.findViewById(R.id.edit_text);
//                    posName.setText(marker.getTitle());
                    EditText posMemo = (EditText) layout.findViewById(R.id.edit_text2);
//                    posName.setText(marker.getSnippet());
                    String name = posName.getText().toString();
                    String memo = posMemo.getText().toString();

//                    BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.icon_109850);
                    HashMap markerData = getMarkerDataById(dataId) ;
                    ContentValues values = new ContentValues();

//                    values.put("_id", String.valueOf(dataId)) ;
                    values.put("lat",valueOf(marker.getPosition().latitude));
                    values.put("lot",valueOf(marker.getPosition().longitude));
                    values.put("posName", valueOf(name));
                    values.put("posMemo", valueOf(memo));
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
                    EditText posMemo = (EditText) layout.findViewById(R.id.edit_text2);
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





