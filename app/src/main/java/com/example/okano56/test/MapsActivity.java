package com.example.okano56.test;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Service;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.valueOf;

public class MapsActivity extends FragmentActivity implements LocationListener{

    private static GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private static final String TAG = MapsActivity.class.getSimpleName();
    // 更新時間(目安)
    private static final int LOCATION_UPDATE_MIN_TIME = 0;
    // 更新距離(目安)
    private static final int LOCATION_UPDATE_MIN_DISTANCE = 0;
    private LocationManager mLocationManager;

    private  MyDBHelper myhelper ;   //to create database
    private  static SQLiteDatabase db;    //database
    private String str;
    private static Marker mMarker;
    public static ArrayList markerList;
    private Location lastLocation;      //直近のポジションデータ保存用
    private boolean isSave = false ;
    private static HashMap<Marker, String> markerHash ;   //markerにデータベースと同じidを設定できないので、これで代用
    private int markerId ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        markerHash  = new HashMap<Marker, String>();
        markerId = 0 ;
//        deleteDatabase("posDB");
        markerList = new ArrayList<Marker>();
        setContentView(R.layout.activity_maps);
        myhelper = new MyDBHelper(this);
        db = myhelper.getWritableDatabase();
        mLocationManager = (LocationManager)this.getSystemService(Service.LOCATION_SERVICE);  //位置データを取る用
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        setUpMapIfNeeded();

        //直近に取得したGPSデータを取得する。
        Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        lastLocation = location ;

        //3Gやwifiから位置情報を取得できるかどうか
        if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
            Log.e(TAG, "netOk") ;

        //GPSから位置情報を取得できるかどうか
        if(mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            Log.e(TAG, "gpsOk") ;

        //「データを保存する」ボタン
        Button saveButton = (Button) findViewById(R.id.saveMapData);  //
        saveButton.setOnClickListener(new OnClickListener() {
                                          @Override
                                          public void onClick(View v){
                                              isSave = true ;
                                              Toast.makeText(getApplicationContext(), "保存開始", Toast.LENGTH_LONG).show();
//                                              saveDialog();
                                          }
                                      }
        );

        //「保存終了」ボタン
        Button endSaveButton = (Button) findViewById(R.id.endSave) ;
        endSaveButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                isSave = false  ;
                Toast.makeText(getApplicationContext(), "保存終了", Toast.LENGTH_LONG).show();
            }
        });
        //マップデータを表示するボタンの実装
        Button outputButton = (Button) findViewById(R.id.openMapData);
        outputButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment alertDialog = new ListEachDateDialog() ;
                alertDialog.show(getFragmentManager(), "ddd") ;
                Cursor c = db.query("posDB",null, null, null, null, null, null);
                str = "データベース一覧\n";
                while(c.moveToNext()) {
                    str += c.getString(c.getColumnIndex("_id")) + ":" +
                            c.getString(c.getColumnIndex("lat")) + ":"+
                            c.getString(c.getColumnIndex("lot")) + "\n";
                }
                Log.e(TAG, str);
            }
        });

        //データベースのすべてのデータを削除する
        Button deleteButton = (Button) findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteAllPosDatabese();   //delete posDB's data
                deleteMarkerList();   //delete markers
                str = "データベースを削除しました";
                Toast.makeText(getApplicationContext(), str, Toast.LENGTH_LONG).show();
            }
        });
    }

    //locationデータを保存するようのダイアログ
    private void saveDialog(){
        LayoutInflater inflater = (LayoutInflater)this
                .getSystemService(LAYOUT_INFLATER_SERVICE);
        final View layout = inflater.inflate(
                R.layout.save_pos_data,
                (ViewGroup) findViewById(R.id.layout_root)
        );

        //アラーとダイアログの生成
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("test");
        builder.setView(layout);
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                EditText posName = (EditText) layout.findViewById(R.id.edit_text);
                EditText posMemo = (EditText) layout.findViewById(R.id.edit_text2);
                String name = posName.getText().toString();
                String memo = posMemo.getText().toString();

                BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.icon_109850);

                LatLng location = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());

                // マーカーの設定
                MarkerOptions options = createMarkerOptions(location, name, memo, icon);

                // マップにマーカーを追加
                mMarker = mMap.addMarker(options);
                markerList.add(mMarker) ;
//                insertPosDataToDB(name, memo);   //データベースに格納
                Toast.makeText(getApplicationContext(), "位置データを保存", Toast.LENGTH_LONG).show();
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

        Log.e(TAG, "dadadadada") ;
        for(int i = markerList.size() -1 ; i >= 0;i--){
            Marker marker = (Marker)markerList.get(i);
            if (marker.getId().equals(markerId)) {
                marker.setVisible(isVisible);    //各マーカーの表示
            }
        }
    }


    private  static void hideInfoWindows(){
        for(int i = markerList.size() -1 ; i >= 0;i--){
            Marker marker = (Marker)markerList.get(i);
            marker.hideInfoWindow();
        }
    }

    protected double getDistance(Location location,Location location2) {
        double x = location.getLatitude() ;
        double y = location.getLongitude() ;
        double x2 = location2.getLatitude() ;
        double y2 = location2.getLongitude() ;
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
    private void deleteAllPosDatabese(){
        Cursor c = db.query("posDB",null, null, null, null, null, null);
        String id;
        while(c.moveToNext()) {
            id= c.getString(c.getColumnIndex("_id"));
            db.delete("posDB", "_id=\""+id+"\"", null);
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
        if (icon == null) {
            options.icon(icon);
        }
        options.snippet(memo);
        options.visible(false) ;    //この段階では非表示

        return options ;
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
//        values.put("_id", String.valueOf(mMarker.getId())) ;
        values.put("_id", String.valueOf(markerId++)) ;
        values.put("lat",valueOf(location.getLatitude()));
        values.put("lot",valueOf(location.getLongitude()));
        values.put("posName", valueOf(name));
        values.put("posMemo", valueOf(memo));
        values.put("date", valueOf(df.format(date)));
        db.insert("posDB", null, values);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    // Called when the location has changed.
    @Override
    public void onLocationChanged(Location location) {

//        Log.e(TAG, "onStatusChanged.");
        //@note あとで住所を入力できるように
        if (isSave) {

            if (lastLocation != null) {

//                Log.e(TAG, String.valueOf(getDistance(lastLocation, location)*100.0));
                //@note あとで住所を入力できるように
                if (getDistance(lastLocation, location)*100.0 > 0.1) {  //座標のズレが誤差以上であれば保存
                    insertPosDataToDB(location, "test", "test");
                    lastLocation = location;       //直近のロケーションデータを更新
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
        //Log.e(TAG, "onStatusChanged.");

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

    /**
     * カメラのポジションを指定したマーカーの場所にする
     * @param marker
     */
    private static void cameraPosToMarker(Marker marker){
        CameraPosition sydney = new CameraPosition.Builder()
                .target(marker.getPosition()).zoom(15.5f)
                .bearing(0).tilt(25).build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(sydney));
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
        mMap.setTrafficEnabled(true); //display traffic data

        //requestLocation data
        mLocationManager.requestLocationUpdates(
                //LocationManager.NETWORK_PROVIDER,
                LocationManager.GPS_PROVIDER,
                LOCATION_UPDATE_MIN_TIME,
                LOCATION_UPDATE_MIN_DISTANCE,
                this);
//         if (mLocationManager == null){         //GPSが無効であれば
//                mLocationManager.requestLocationUpdates(
//                LocationManager.NETWORK_PROVIDER,
//                LOCATION_UPDATE_MIN_TIME,
//                LOCATION_UPDATE_MIN_DISTANCE,
//                this);
//         }

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
    }

    /**
     「データ表示」を押した時のリストを出すクラス
     */
    public static class ListEachDateDialog extends DialogFragment{
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState){
            Cursor c = db.query("posDB",null, null, null, null, null, null);

            final  CharSequence[] items = createItem(c) ;

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()) ;
            builder.setItems(items, new DialogInterface.OnClickListener() {
                Cursor c = db.query("posDB",null, null, null, null, null, null);
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    setMarkerListVisible(false);
                    markerList = new ArrayList<Marker>();   //マーカーの初期化
                    //items[i]の日付を持つデータをデータベースから取り出す
                    while(c.moveToNext()) {
                        if (c.getString(c.getColumnIndex("date")).equals(items[i])) {
                            Log.e(TAG, "TTTTTTT") ;
                            String id = c.getString(c.getColumnIndex("_id"));
                            String lat = c.getString(c.getColumnIndex("lat"));
                            String lot = c.getString(c.getColumnIndex("lot"));
                            String posName = c.getString(c.getColumnIndex("posName"));
                            String posMemo = c.getString(c.getColumnIndex("posMemo"));
                            LatLng location = new LatLng(Float.valueOf(lat).floatValue(), Float.valueOf(lot).floatValue());
                            MarkerOptions options = createMarkerOptions(location, posName, posMemo, null);
                            mMarker = mMap.addMarker(options);
                            markerList.add(mMarker) ;
                            markerHash.put(mMarker, id) ;
//                                setMarkerVisible(true, id);
                        }
                    }
                    setMarkerListVisible(true);

                    Marker firstMarker = (Marker) markerList.get(0);
                    cameraPosToMarker(firstMarker) ;

                }
            }) ;
            return builder.create() ;
        }

        private CharSequence[] createItem(Cursor c){
            ArrayList<CharSequence> dateList = new ArrayList<CharSequence>() ;

            while(c.moveToNext()) {
                if (!dateList.contains(c.getString(c.getColumnIndex("date")))) {
                    dateList.add(c.getString(c.getColumnIndex("date"))) ;
                }
            }

            final CharSequence[] items = new CharSequence[dateList.size()] ;
            for(int i =0  ;i < dateList.size() ;i++ ){
                items[i] = dateList.get(i) ;
            }

            return items ;
        }
    }

    public static class MyDialogFragment extends DialogFragment{

        public static MyDialogFragment newInstance(Marker marker){
            MyDialogFragment myDialogFragment = new MyDialogFragment() ;
            Bundle bunlde = new Bundle() ;
            bunlde.putString("marker", marker.getTitle()) ;
            bunlde.putString("id", markerHash.get(marker)) ;
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
                            break;
                        default:
                            break;

                    }
                }
            }) ;
            return builder.create() ;
        }

        /**
         *マーカーデータを消すことができるメソッド
         */
        private void editData(){
            final String dataId = getArguments().getString("id") ;
            final Marker marker = getMarkerById(dataId);


//            LayoutInflater inflater = LayoutInflater.from(this) ;
            LayoutInflater inflater = getActivity().getLayoutInflater() ;
            final View layout = inflater.inflate(  R.layout.save_pos_data,null);
//            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

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

                    values.put("_id", String.valueOf(dataId)) ;
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
            String dataId = getArguments().getString("id") ;
            db.delete("posDB", "_id=\""+dataId+"\"", null);
            Marker marker = getMarkerById(dataId);
            marker.remove();
        }
    }

    /**
     * 引数のIDとマーカーについてるIDが一致していれば、取得。
     * @param id
     * @return
     */
//    private static Marker getMarkerById(String id){
//        Marker marker = null;
//            for(int i = markerList.size()-1 ;i >= 0 ;i--){
//                marker =  (Marker)markerList.get(i) ;
//                if (marker.getId().equals(id)){
//                    marker.remove();
//                }
//            }
//        if (marker == null)
//               Log.e(TAG, "Err marker is null")  ;
//        return marker ;
//    }

    private static Marker getMarkerById(String id){
        for(Map.Entry<Marker, String> entry : markerHash.entrySet()){
            if (entry.getValue() == id) {
                return entry.getKey();
            }
        }
        return null ;
    }
    private static HashMap getMarkerDataById(String id){
        HashMap<String, String> markerData = new HashMap<String, String>() ;
        Cursor c = db.rawQuery("select * from posDB where _id in(\""+id+"\") ;", null) ;
//        db.delete("posDB", "_id=\""+id+"\"", null);
        c.moveToFirst() ;
        String id_1 = c.getString(c.getColumnIndex("_id"));
        String lat = c.getString(c.getColumnIndex("lat"));
        String lot = c.getString(c.getColumnIndex("lot"));
        String posName = c.getString(c.getColumnIndex("posName"));
        String posMemo = c.getString(c.getColumnIndex("posMemo"));
        String date = c.getString(c.getColumnIndex("date")) ;
        Log.e(TAG, id_1+"posName:"+posName)  ;
        markerData.put("_id", id_1) ;
        markerData.put("lat", lat) ;
        markerData.put("lot", lot) ;
        markerData.put("posName", posName) ;
        markerData.put("posMemo",posMemo) ;
        markerData.put("date",date) ;
        return markerData ;
    }

}





