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
    private static Location mMyLocation = null;
    private static boolean mMyLocationCentering = false;

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
        setContentView(R.layout.acivity_map2);
        //setUpMapIfNeeded();

        mLocationManager = (LocationManager)this.getSystemService(Service.LOCATION_SERVICE);
        requestLocationUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //setUpMapIfNeeded();
        //setUpLocation(true);
    }
    // Called when the location has changed.
    @Override
    public void onLocationChanged(Location location) {
        Log.e(TAG, "onLocationChanged.");
        showLocation(location);
    }

    // Called when the provider status changed.
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.e(TAG, "onStatusChanged.");
        showProvider(provider);
        switch (status) {
            case LocationProvider.OUT_OF_SERVICE:
                // if the provider is out of service, and this is not expected to change in the near future.
                String outOfServiceMessage = provider +"が圏外になっていて取得できません。";
                showMessage(outOfServiceMessage);
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                // if the provider is temporarily unavailable but is expected to be available shortly.
                String temporarilyUnavailableMessage = "一時的に" + provider + "が利用できません。もしかしたらすぐに利用できるようになるかもです。";
                showMessage(temporarilyUnavailableMessage);
                break;
            case LocationProvider.AVAILABLE:
                // if the provider is currently available.
                if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
                    String availableMessage = provider + "が利用可能になりました。";
                    showMessage(availableMessage);
                    requestLocationUpdates();
                }
                break;
        }
    }
    // Called when the provider is enabled by the user.
    @Override
    public void onProviderEnabled(String provider) {
        Log.e(TAG, "onProviderEnabled.");
        String message = provider + "が有効になりました。";
        showMessage(message);
        showProvider(provider);
        if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
            requestLocationUpdates();
        }
    }

    // Called when the provider is disabled by the user.
    @Override
    public void onProviderDisabled(String provider) {
        Log.e(TAG, "onProviderDisabled.");
        showProvider(provider);
        if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
            String message = provider + "が無効になってしまいました。";
            showMessage(message);
        }
    }

    private void requestLocationUpdates() {
        Log.e(TAG, "requestLocationUpdates()");
        showProvider(LocationManager.NETWORK_PROVIDER);
        boolean isNetworkEnabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        showNetworkEnabled(isNetworkEnabled);
        if (isNetworkEnabled) {
            showMessage("DDDDDDDD");
            mLocationManager.requestLocationUpdates(
                    //LocationManager.NETWORK_PROVIDER,
                    LocationManager.GPS_PROVIDER,
                    LOCATION_UPDATE_MIN_TIME,
                    LOCATION_UPDATE_MIN_DISTANCE,
                    this);
            //Location location = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            showMessage("location"+location);
            if (location != null) {
                showLocation(location);
            }
        } else {
            String message = "Networkが無効になっています。";
            showMessage(message);
        }
    }
    private void showLocation(Location location) {
        double longitude = location.getLongitude();
        double latitude = location.getLatitude();
        long time = location.getTime();
        Date date = new Date(time);
        DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss:SSS");
        String dateFormatted = formatter.format(date);
        TextView longitudeTextView = (TextView)findViewById(R.id.longitude);
        longitudeTextView.setText("Longitude : " + String.valueOf(longitude));
        TextView latitudeTextView = (TextView)findViewById(R.id.latitude);
        latitudeTextView.setText("Latitude : " + String.valueOf(latitude));
        TextView geoTimeTextView = (TextView)findViewById(R.id.geo_time);
        geoTimeTextView.setText("取得時間 : " + dateFormatted);
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

        /*CameraPosition sydney = new CameraPosition.Builder()
                .target(new LatLng(35.469716,139.629183)).zoom(15.5f)
                .bearing(0).tilt(25).build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(sydney));
*/
        mMap.setTrafficEnabled(true);

        MyLocationSource source = new MyLocationSource();
        mMap.setLocationSource(source);


        mMap.setMyLocationEnabled(true);

    }

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


