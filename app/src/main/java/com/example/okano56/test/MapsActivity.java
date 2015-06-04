package com.example.okano56.test;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.view.View.OnClickListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.sql.Array;
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
    private ArrayList locationList;
    private ArrayList markerList;


    //locationデータ保存用のデータクラス
    public class LocationData {
        String lat;
        String lot;
        public LocationData(String lat,String lot) {
            this.lat = lat;
            this.lot = lot;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        locationList = new ArrayList<LocationData>();
        LocationData fisrt = new LocationData("0","0");
        locationList.add(fisrt);

        markerList = new ArrayList<Marker>();



        deleteDatabase("testtb");


        Log.e(TAG, "onLocationChanged.");
        //setContentView(R.layout.acivity_map2);
        setContentView(R.layout.activity_maps);
        //create database helper ??
        myhelper = new MyDBHelper(this);
        db = myhelper.getWritableDatabase();
        mLocationManager = (LocationManager)this.getSystemService(Service.LOCATION_SERVICE);  //位置データを取る用
        setUpMapIfNeeded();

        Button saveButton = (Button) findViewById(R.id.saveMapData);  //
        saveButton.setOnClickListener(new OnClickListener() {
                                          @Override
                                          public void onClick(View v){
                                              Toast.makeText(getApplicationContext(), "位置データを保存", Toast.LENGTH_LONG).show();

                                              LocationData lastLocation = (LocationData) locationList.get(locationList.size()-1);
                                              ContentValues values = new ContentValues();
                                              values.put("lat",lastLocation.lat);
                                              values.put("lot",lastLocation.lot);
                                              db.insert("testtb",null , values);
                                          }
                                      }
        );

        Button outputButton = (Button) findViewById(R.id.openMapData);      //マップデータを表示するボタンの実装
        outputButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                markerList = new ArrayList<Marker>();   //initialize markerList
                Cursor c = db.query("testtb",null, null, null, null, null, null);
                str = "データベース一覧\n";
                while(c.moveToNext()) {
                    str += c.getString(c.getColumnIndex("_id")) + ":" +
                            c.getString(c.getColumnIndex("lat")) + ":"+
                            c.getString(c.getColumnIndex("lot")) + "\n";

                    // マーカーを貼る緯度・経度
                    double lat =Double.parseDouble(c.getString(c.getColumnIndex("lat")));
                    double lot =Double.parseDouble(c.getString(c.getColumnIndex("lot")));
                    LatLng location = new LatLng(lat, lot);
                    // マーカーの設定
                    MarkerOptions options = new MarkerOptions();
                    options.position(location);
                    options.title("データOK");
                    options.snippet(location.toString());
                    // マップにマーカーを追加
                    mMarker = mMap.addMarker(options);
                    markerList.add(mMarker);
                }
                Log.e(TAG,str);
            }
        });

        Button deleteButton = (Button) findViewById(R.id.deleteButton);  //データベースのすべてのデータを削除する
        deleteButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //deleteDatabase("testtb");
                //delete all data in database
                Cursor c = db.query("testtb",null, null, null, null, null, null);
                String id;
                while(c.moveToNext()) {
                    id= c.getString(c.getColumnIndex("_id"));
                    db.delete("testtb", "_id="+id, null);
                }
                //delete markers
                for(int i = 0; i < markerList.size();i++){
                    Marker marker = (Marker)markerList.get(i);
                    marker.remove();
                }

                locationList = new ArrayList<LocationData>();
                LocationData fisrt = new LocationData("0","0");
                locationList.add(fisrt);
                str = "データベースを削除しました";
                Toast.makeText(getApplicationContext(), str, Toast.LENGTH_LONG).show();
            }


        });
        //requestLocationUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    //
    // Called when the location has changed.
    @Override
    public void onLocationChanged(Location location) {
        //Log.e(TAG, "onLocationChanged.");
        //showLocation(location);
        String lat = valueOf(location.getLatitude());
        String lot = valueOf(location.getLongitude());

        //もしこの座標がその前の座標と一致していなければ
        LocationData lastLocation = (LocationData) locationList.get(locationList.size() - 1);
        if ((lastLocation.lat != lat) || (lastLocation.lot != lot)) {
            LocationData locationData = new LocationData(lat, lot);
            locationList.add(locationData);
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
        //mMap.addMarker(new MarkerOptions().position(new LatLng(43, 31)).title("Marker"));
        //mMap.addMarker(new MarkerOptions().position(new LatLng(35.469716,139.629183)).title("Marker"));

        mMap.setTrafficEnabled(true); //display traffic data

//        MyLocationSource source = new MyLocationSource();
        //mMap.setLocationSource(source);

        //requestLocation data
        mLocationManager.requestLocationUpdates(
                //LocationManager.NETWORK_PROVIDER,
                LocationManager.GPS_PROVIDER,
                LOCATION_UPDATE_MIN_TIME,
                LOCATION_UPDATE_MIN_DISTANCE,
                this);

        //get location data
        Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        //check location data
        Log.e(TAG, location.getLatitude() + ",########" + location.getLongitude());
        CameraPosition sydney = new CameraPosition.Builder()
                .target(new LatLng(location.getLatitude(),location.getLongitude())).zoom(15.5f)
                .bearing(0).tilt(25).build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(sydney));
        mMap.setMyLocationEnabled(true);  //display data on the map


    }

    //I dont understand this class.maybe it's class postion data ??
    public class MyLocationSource implements LocationSource {
        @Override
        public void activate(LocationSource.OnLocationChangedListener listener) {

            // 好きな緯度・経度を設定した Location を作成

            Location location = new Location("MyLocation");
            //location.setLatitude(35.469716);
            location.setLatitude(location.getLatitude());
            //location.setLongitude(139.629183);
            location.setLongitude(location.getLongitude());
            System.out.println("location data "+location.getLongitude());
            location.setAccuracy(100); // 精度
            // Location に設定
            listener.onLocationChanged(location);
        }
        @Override
        public void deactivate() {
        }
    }
}


