package com.example.okano56.test;

import android.app.Service;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MapsActivity extends FragmentActivity implements LocationListener{

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    private static final String TAG = MapsActivity.class.getSimpleName();
    // 更新時間(目安)
    private static final int LOCATION_UPDATE_MIN_TIME = 0;
    // 更新距離(目安)
    private static final int LOCATION_UPDATE_MIN_DISTANCE = 0;

    private LocationManager mLocationManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.e(TAG, "onLocationChanged.");
        //setContentView(R.layout.acivity_map2);
        setContentView(R.layout.activity_maps);

        mLocationManager = (LocationManager)this.getSystemService(Service.LOCATION_SERVICE);  //位置データを取る用
        setUpMapIfNeeded();

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
        Log.e(TAG, "onLocationChanged.");
        //showLocation(location);
    }

    // Called when the provider status changed.
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.e(TAG, "onStatusChanged.");
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
        textView.setText("NetworkEnabled : " + String.valueOf(isNetworkEnabled));
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
        Log.e(TAG,location.getLatitude()+",########"+location.getLongitude());
        mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(),location.getLongitude())).title("okano"));
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


