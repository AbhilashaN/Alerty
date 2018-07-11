package com.example.android.alerty;

import android.*;
import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryDataEventListener;
import com.firebase.geofire.GeoQueryEventListener;
import com.firebase.geofire.LocationCallback;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

import static com.example.android.alerty.SignInActivity.ID;

@TargetApi(23)
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationChangeListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ActivityCompat.OnRequestPermissionsResultCallback, com.google.android.gms.location.LocationListener,PermissionUtils.PermissionResultCallback  {
    private static final String TAG = MapsActivity.class.getSimpleName();
    private final static int PLAY_SERVICES_REQUEST = 1000;
    private final static int REQUEST_CHECK_SETTINGS = 2000;
    private int MY_DATA_CHECK_CODE = 0;
    public static TextToSpeech myTTS;
    private static int DISPLACEMENT = 20;
    public static Location mLastLocation;
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private String mUsername;
    public double latitude;
    public double longitude;
    public double lat;
    public double lon;
    public Boolean isPermissionGranted = false;
    private GoogleMap mMap;
    public static final String ANONYMOUS = "anonymous";
    DatabaseReference ref;
    GeoFire geoFire;
    GeoQuery geoQuery;
    MarkerOptions marker;
    private Integer num=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mUsername = ANONYMOUS;
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build();
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();


        ref = FirebaseDatabase.getInstance().getReference();
        geoFire = new GeoFire(ref);
        myTTS.speak("Be Alert! Be Safe!", TextToSpeech.QUEUE_ADD, null, "DEFAULT");
        setUpLocation();
        //permissionUtils = new PermissionUtils(MapsActivity.this);

        //permissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        //pemissions.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);

        //permissionUtils.check_permission(permissions, "Need GPS permission for getting your location", 1);
//        if (checkPlayServices()) {
//            buildGoogleApiClient();
//        }
        //getLocation();


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.sign_out_menu:
                mFirebaseAuth.signOut();
                Auth.GoogleSignInApi.signOut(mGoogleApiClient);
                mUsername = ANONYMOUS;
                startActivity(new Intent(this, SignInActivity.class));
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    //needed
    private void setUpLocation() {
        if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) && (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)){
            //Requesting run-time permission
            ActivityCompat.requestPermissions(this,new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},REQUEST_CHECK_SETTINGS);

        }
        else{
            if (checkPlayServices()){
                buildGoogleApiClient();

            }
        }
    }
    protected synchronized void buildGoogleApiClient() {

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT); //min displacement (in metres) between location updates
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult locationSettingsResult) {

                final Status status = locationSettingsResult.getStatus();

                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        displayLocation();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {

                            status.startResolutionForResult(MapsActivity.this, REQUEST_CHECK_SETTINGS);

                        } catch (IntentSender.SendIntentException e) {
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                }
            }
        });
    }


    private void displayLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;

        }
        if (mGoogleApiClient.isConnected()) {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        }
        if (mLastLocation != null) {
            latitude = mLastLocation.getLatitude();
            longitude = mLastLocation.getLongitude();
            LatLng you = new LatLng(latitude, longitude);
            final Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.car1);
            marker = new MarkerOptions().position(you).title("Me");
            mMap.addMarker(marker);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(you, 15.0f)); //15.0f refers to the level of zooming
            LatLng circle = new LatLng(latitude, longitude);
            mMap.addCircle(new CircleOptions()
                    .center(circle)
                    .radius(500) //500 m
                    .fillColor(0x220000FF)
                    .strokeColor(Color.BLUE)
                    .strokeWidth(5.0f));
            geoFire.setLocation(ID, new GeoLocation(latitude, longitude), new GeoFire.CompletionListener() {
                @Override
                public void onComplete(String key, DatabaseError error) {
//                    if (error!=null){
//                        showToast("error");
//                    } else {
//                        showToast("saved");
//                    }
                }


            });


            GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(circle.latitude, circle.longitude), 0.5f); //0.5 km
            geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                @Override
                public void onKeyEntered(String key, GeoLocation location) {
                    if (key != ID) {
                        showToast(String.format("Vehicle entered the search area at [%f,%f]", location.latitude, location.longitude));
                        num++;
                        geoFire.setLocation(key, new GeoLocation(location.latitude, location.longitude), new GeoFire.CompletionListener() {
                            @Override
                            public void onComplete(String key, DatabaseError error) {
                                if (error != null) {
                                    showToast("error");
                                } else {

                                    //showToast("SAVED");
                                }
                            }

                        });
                        LatLng vehicle = new LatLng(10.5, 78);
                        MarkerOptions veh = new MarkerOptions().position(vehicle).title("Vehicle").icon(BitmapDescriptorFactory.fromBitmap(bm));

                        mMap.addMarker(veh);

                    }
                    {
                        showToast("There are " + num + " vehicles within 500m radius");

                        myTTS.speak("There are " + num + " vehicles within 500m radius", TextToSpeech.QUEUE_ADD, null, "DEFAULT");
                    }

                }


                @Override
                public void onKeyExited(String key) {
                    showToast(String.format("Vehcile %s is no longer in the search area", key));
                    showToast("There are " + num + " vehicles within 500m radius");
                    geoFire.removeLocation(key);
                    myTTS.speak("There are " + num + " vehicles within 500m radius", TextToSpeech.QUEUE_ADD, null, "DEFAULT");


                    num--;
                }

                @Override
                public void onKeyMoved(String key, GeoLocation location) {
                }

                @Override
                public void onGeoQueryReady() {
                }

                @Override
                public void onGeoQueryError(DatabaseError error) {
                }
            });
//            if (ref != null) {
//                geoFire.setLocation("Me", new GeoLocation(latitude, longitude), new GeoFire.CompletionListener() {
//                    @Override
//                    public void onComplete(String key, com.google.firebase.database.DatabaseError error) {
////                    if (mCurrent != null) {
////                        mCurrent.remove();
////                        mCurrent = mMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title("Me"));
////                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 15.0f));
////                    }
//                    }
//                });
//            }
//
//        } else {
//            Log.d("MapsActivity", "Can not get your location");
//        }
        }
    }


//
//    private void createLocationRequest() {
//
//        mLocationRequest = new LocationRequest();
//        mLocationRequest.setInterval(10000);
//        mLocationRequest.setFastestInterval(5000);
//        mLocationRequest.setSmallestDisplacement(DISPLACEMENT); //min displacement (in metres) between location updates
//        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//
//    }

    private void showToast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }
    //needed
//    protected synchronized void buildGoogleApiClient() {
//        showToast("building");
//
//        mGoogleApiClient = new GoogleApiClient.Builder(this)
//                .addConnectionCallbacks(this)
//                .addOnConnectionFailedListener(this)
//                .addApi(LocationServices.API)
//                .build();
//        mGoogleApiClient.connect();
//        mLocationRequest = new LocationRequest();
//        mLocationRequest.setInterval(10000);
//        mLocationRequest.setFastestInterval(5000);
//        mLocationRequest.setSmallestDisplacement(DISPLACEMENT); //min displacement (in metres) between location updates
//        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//    }

    @Override
    protected void onStart() {
        if (mGoogleApiClient!=null){
            mGoogleApiClient.connect();
        }
        super.onStart();

    }
    @Override
    protected void onDestroy(){
        myTTS.shutdown();
        mGoogleApiClient.disconnect();
        super.onDestroy();
    }
    @Override
    public void onConnected(@Nullable Bundle bundle) {
       getLocation();
    }

    private void getLocation() {
        if (isPermissionGranted) {

            try
            {
                if (mGoogleApiClient.isConnected()){
                    mLastLocation = LocationServices.FusedLocationApi
                            .getLastLocation(mGoogleApiClient);}
            }
            catch (SecurityException e)
            {
                e.printStackTrace();
            }

        }

    }


    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();

    }
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
                + connectionResult.getErrorCode());

    }

    private boolean checkPlayServices() {

        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();

        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this);

        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(this,resultCode,
                        PLAY_SERVICES_REQUEST).show();
            } else {
                Toast.makeText(getApplicationContext(),
                        "This device is not supported.", Toast.LENGTH_LONG)
                        .show();
                finish();
            }
            return false;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mGoogleApiClient!=null)
            mGoogleApiClient.connect();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode){
            case REQUEST_CHECK_SETTINGS:
                if (grantResults.length > 0 &&grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if (checkPlayServices()){
                        buildGoogleApiClient();

                    }

                }
                break;
        }

    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //LatLng circle = new LatLng(10.7574267,78.8214082);
//        LatLng circle = new LatLng(latitude,longitude);
//        mMap.addCircle(new CircleOptions()
//                       .center(circle)
//                        .radius(500) //500 m
//                        .fillColor(0x220000FF)
//                       .strokeColor(Color.BLUE)
//                        .strokeWidth(5.0f));




    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation=location;
        displayLocation();
    }


    @Override
    public void PermissionGranted(int request_code) {

    }

    @Override
    public void PartialPermissionGranted(int request_code, ArrayList<String> granted_permissions) {

    }

    @Override
    public void PermissionDenied(int request_code) {

    }

    @Override
    public void NeverAskAgain(int request_code) {

    }

    @Override
    public boolean onMyLocationButtonClick() {
        return false;
    }

    @Override
    public void onMyLocationChange(Location location) {
        mLastLocation = location;
        displayLocation();
    }


//    public void speakwords(String words){
//        if(myTTS != null) {
//            myTTS.speak(words, TextToSpeech.QUEUE_ADD, null, "DEFAULT");
//            showToast("speaking");
//        }
//    }



}

