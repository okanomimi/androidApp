package com.example.okano56.test;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
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
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static java.lang.String.valueOf;

public class MapsActivity extends FragmentActivity implements LocationListener{

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private static final String TAG = MapsActivity.class.getSimpleName();
    // 更新時間(目安)
    private static final int LOCATION_UPDATE_MIN_TIME = 0;
    // 更新距離(目安)
    private static final int LOCATION_UPDATE_MIN_DISTANCE = 0;
    private LocationManager mLocationManager;

    private  MyDBHelper myhelper ;   //to create database
    private  static SQLiteDatabase db;    //database
    private String str;
    private Marker mMarker;
    public static ArrayList markerList;
    private Location lastLocation;

    private PopupWindow markerPopupWindow ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        deleteDatabase("posDB");
        markerList = new ArrayList<Marker>();
        setContentView(R.layout.activity_maps);
        myhelper = new MyDBHelper(this);
        db = myhelper.getWritableDatabase();
        mLocationManager = (LocationManager)this.getSystemService(Service.LOCATION_SERVICE);  //位置データを取る用
        setUpMapIfNeeded();
        LocationManager lm = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE) ;
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 10, this);

        Button saveButton = (Button) findViewById(R.id.saveMapData);  //
        saveButton.setOnClickListener(new OnClickListener() {
                                          @Override
                                          public void onClick(View v){
                                              saveDialog();
                                          }
                                      }
        );

//        requestLocationUpdates();
        Button outputButton = (Button) findViewById(R.id.openMapData);      //マップデータを表示するボタンの実装
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
                //setMarkerListVisible(true);
                Log.e(TAG,str);
            }
        });

        Button deleteButton = (Button) findViewById(R.id.deleteButton);  //データベースのすべてのデータを削除する
        deleteButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteAllPosDatabese();   //delete posDB's data
                deleteMarkerList() ;   //delete markers
                str = "データベースを削除しました";
                Toast.makeText(getApplicationContext(), str, Toast.LENGTH_LONG).show();
            }
        });
        //requestLocationUpdates();
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
                insertPosDataToDB(name, memo);   //データベースに格納
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
    private void insertPosDataToDB(String name, String memo){
        Date date = new Date() ;
        DateFormat df = new SimpleDateFormat("yyyy/MM/dd") ;
        ContentValues values = new ContentValues();
        values.put("_id", String.valueOf(mMarker.getId())) ;
        values.put("lat",valueOf(lastLocation.getLatitude()));
        values.put("lot",valueOf(lastLocation.getLongitude()));
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

        Log.e(TAG, "onStatusChanged.");
        lastLocation = location;   //直近のlocationデータを保存
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

        //get location data
        Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

//        CameraPosition sydney = new CameraPosition.Builder()
//                .target(new LatLng(location.getLatitude(),location.getLongitude())).zoom(15.5f)
//                .bearing(0).tilt(25).build();
//        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(sydney));
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
                if (marker.isInfoWindowShown()) {
                    Log.e(TAG, "iffo");
                    DialogFragment alertDlg = MyDialogFragment.newInstance(marker);
                    alertDlg.show(getFragmentManager(), "test");
                }else
                    Log.e(TAG, "not") ;

//                marker.hideInfoWindow();
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
                                setMarkerVisible(true, id);
                            }
                    }
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
            bunlde.putString("id", marker.getId()) ;
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
                    switch (i){
                        case 0:
                            Toast.makeText(getActivity(), "edit", Toast.LENGTH_LONG).show() ;
                            break ;
                        case 1:
                            Toast.makeText(getActivity(), "delete", Toast.LENGTH_LONG).show() ;
                            delete_data() ;
                            break ;
                        default:
                            break ;

                    }
                }
            }) ;
            return builder.create() ;
        }

        /**
         *マーカーデータを消すことができるメソッド
         */
        private void delete_data(){
            String dataId = getArguments().getString("id") ;
            db.delete("posDB", "_id=\""+dataId+"\"", null);
            Marker marker ;
            for(int i = markerList.size()-1 ;i >= 0 ;i--){
                marker =  (Marker)markerList.get(i) ;
                if (marker.getId().equals(dataId)){
                    marker.remove();
                }
            }
        }
    }

    /**
     *
     *  Markerの吹き出しを自分用に変更したやつ。
     *
     */
    private class MyInfoAdaper implements GoogleMap.InfoWindowAdapter{
        private final View mWindow;
        private Button removeButton ;
        private Button editButton;

        //コンストラクタ
        public MyInfoAdaper(){
            mWindow = getLayoutInflater().inflate(R.layout.custom_info_window,null);
        }
        @Override
        public View getInfoWindow(Marker marker){
            render(marker,mWindow);
            return mWindow;
        }

        @Override
        public View getInfoContents(Marker marker){
            return null;
        }

        public void render(Marker marker,View view){
            if (marker.equals(mMarker)) {
            }
            TextView markersIdText = (TextView)view.findViewById(R.id.markersId) ;
            TextView title = (TextView)view.findViewById(R.id.title_text) ;
            TextView snippet = (TextView)view.findViewById(R.id.context_text) ;

            markersIdText.setText(marker.getId());
            title.setText(marker.getTitle()) ;
            snippet.setText(marker.getSnippet()) ;

            removeButton = (Button)view.findViewById(R.id.remove_button) ;
//            removeButton.setOnClickListener(this);
            removeButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(view == removeButton) {
                        Log.e(TAG, "removeButton")  ;
                    }else if (view == editButton){

                    }
                }
            });
        }

//        @Override
//        public void onClick(View view) {
//           if(view == removeButton) {
//              Log.e(TAG, "removeButton")  ;
//           }else if (view == editButton){
//
//           }
//
//        }
    }

}





