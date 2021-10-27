package com.application.mwalima.trailrunning;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
@RequiresApi(api = Build.VERSION_CODES.M)
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener, AdapterView.OnItemSelectedListener {

    private GoogleMap mMap;
    private LocationRequest locationRequest;
    private Location lastlocation;
    private GoogleApiClient client;
    private LatLng latLng;
    private double lat;
    private double lng;

    private Marker currentLocationmMarker;
    private List<Address> addressList;
    private FusedLocationProviderClient mFusedLocationClient;
    public static final int REQUEST_LOCATION_CODE = 99;
    private HashMap<String, DataSet> mLists = new HashMap<> ();
    protected int getLayoutId() {
        return R.layout.main;
    }

    public int radius;

    protected void start() {
//        // Set up the spinner/dropdown list
//        Spinner distance = findViewById (R.id.afstand);
//        ArrayAdapter<CharSequence> distance_adapter = ArrayAdapter.createFromResource (this,
//                R.array.distance_array, android.R.layout.simple_spinner_item);
//        distance_adapter.setDropDownViewResource (android.R.layout.simple_spinner_dropdown_item);
//        distance.setAdapter (distance_adapter);
        // Set up the spinner/terrain list
        Spinner terrain = findViewById (R.id.terrein);
        ArrayAdapter<CharSequence> terrain_adapter = ArrayAdapter.createFromResource (this,
                R.array.terrain_array, android.R.layout.simple_spinner_item);
        terrain_adapter.setDropDownViewResource (android.R.layout.simple_spinner_dropdown_item);
        terrain.setAdapter (terrain_adapter);

        // Set up the spinner/proximity list
        Spinner proximity = findViewById (R.id.proximity);
        ArrayAdapter<CharSequence> proximity_adapter = ArrayAdapter.createFromResource (this,
                R.array.proximity_array, android.R.layout.simple_spinner_item);
        proximity_adapter.setDropDownViewResource (android.R.layout.simple_spinner_dropdown_item);
        proximity.setAdapter (proximity_adapter);
        proximity.setOnItemSelectedListener (this);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate (savedInstanceState);
        setContentView (R.layout.activity_maps);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient (this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager ()
                .findFragmentById (R.id.map);
        mapFragment.getMapAsync (this);
        start();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission (this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        if (client == null) {
                            buildGoogleApiClient ();
                        }
                        mMap.setMyLocationEnabled (true);
                    }
                } else {
                    Toast.makeText (this, "Permission Denied", Toast.LENGTH_LONG).show ();
                }
        }
    }
    @Override
    public void onResume() {
        super.onResume ();
        if (client != null && mFusedLocationClient != null) {
            requestLocationUpdates ();
        } else {
            buildGoogleApiClient ();
        }
    }
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType (GoogleMap.MAP_TYPE_TERRAIN);
        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission (this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                mFusedLocationClient.requestLocationUpdates (locationRequest, locationCallback, Looper.myLooper ());
                mMap.setMyLocationEnabled (true);
            } else {
                checkLocationPermission ();
            }
        } else {
            mFusedLocationClient.requestLocationUpdates (locationRequest, locationCallback, Looper.myLooper ());
            mMap.setMyLocationEnabled (true);
        }
        start();
    }
    protected synchronized void buildGoogleApiClient() {
        client = new GoogleApiClient.Builder (this)
                .addConnectionCallbacks (this)
                .addOnConnectionFailedListener (this)
                .addApi (LocationServices.API).build ();
        client.connect ();
    }


    LocationCallback locationCallback = new LocationCallback () {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            List<Location> locationList = locationResult.getLocations ();
            Object dataTransfer[] = new Object[2];
            GetNearbyPlacesData getNearbyPlacesData = new GetNearbyPlacesData ();
            if (locationList.size () > 0) {
                //The last location in the list is the newest
                Location location = locationList.get (locationList.size () - 1);
                Log.i ("MapsActivity", "Location: " + location.getLatitude () + " " + location.getLongitude ());
                lastlocation = location;
                if (currentLocationmMarker != null) {
                    currentLocationmMarker.remove ();
                }
                //Place current location marker
                LatLng latLng = new LatLng (location.getLatitude (), location.getLongitude ());
                MarkerOptions markerOptions = new MarkerOptions ();
                markerOptions.position (latLng);
                markerOptions.title ("Current Position");
                markerOptions.icon (BitmapDescriptorFactory.defaultMarker (BitmapDescriptorFactory.HUE_BLUE));
                currentLocationmMarker = mMap.addMarker (markerOptions);
                mMap.clear ();
                mMap.addCircle (
                        new CircleOptions ()
                                .center (latLng)
                                .radius (500)
                                .strokeWidth (3f)
                                .strokeColor (Color.RED)
                                .fillColor (Color.argb (70, 150, 50, 50))
                );
                String all = "avoid=highways&mode=bicycling";
                String url = getUrl (location.getLatitude (), location.getLongitude (), all);
                dataTransfer[0] = mMap;
                dataTransfer[1] = url;
                getNearbyPlacesData.execute (dataTransfer);
                Toast.makeText (MapsActivity.this, "Toon dichtsbijzijnde route mogelijkheden", Toast.LENGTH_SHORT).show ();
                mMap.moveCamera (CameraUpdateFactory.newLatLngZoom (latLng, 15));
            }
        }
    };

    public void onLocationChanged(Location location) {
        lastlocation = location;
    }
    @Override
    public void onPause() {
        super.onPause ();
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates (locationCallback);
        }
    }
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }
    /**
     * Helper class - stores data sets and sources.
     */
    private class DataSet {
        private ArrayList<LatLng> mDataset;
        private String mUrl;
        public DataSet(ArrayList<LatLng> dataSet, String url) {
            this.mDataset = dataSet;
            this.mUrl = url;
        }
        public ArrayList<LatLng> getData() {
            return mDataset;
        }
        public String getUrl() {
            return mUrl;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int radius, long id) {

        this.radius = radius;

        switch (radius) {
            case 0:  this.radius = 500;
                Toast.makeText (MapsActivity.this, "proximity " + parent.getItemAtPosition(radius), Toast.LENGTH_SHORT).show ();
                break;
            case 1:  this.radius = 1000;
                Toast.makeText (MapsActivity.this, "proximity " + parent.getItemAtPosition(radius), Toast.LENGTH_SHORT).show ();
                break;
            case 2:  this.radius = 5000;
                Toast.makeText (MapsActivity.this, "proximity " + parent.getItemAtPosition(radius), Toast.LENGTH_SHORT).show ();
                break;
            case 3:  this.radius = 10000;
                Toast.makeText (MapsActivity.this, "proximity " + parent.getItemAtPosition(radius), Toast.LENGTH_SHORT).show ();
                break;
        }

   }


    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }
    public void GetPlace(View view) {
        Object dataTransfer[] = new Object[2];
        GetNearbyPlacesData getNearbyPlacesData = new GetNearbyPlacesData ();
        EditText tf_location = findViewById (R.id.onFocus);
        String Searchlocation = tf_location.getText ().toString ();
        switch (view.getId ()) {
            case R.id.B_search:
                if (!Searchlocation.equals ("")) {
                    Geocoder geocoder = new Geocoder (this);
                    try {
                        addressList = geocoder.getFromLocationName (Searchlocation, 1);
                        for (int i = 0; i < addressList.size (); i++) {
                            latLng = new LatLng (addressList.get (0).getLatitude (), addressList.get (0).getLongitude ());
                            lat = addressList.get (0).getLatitude ();
                            lng = addressList.get (0).getLongitude ();
                            MarkerOptions markerOptions = new MarkerOptions ();
                            markerOptions.position (latLng);
                            markerOptions.title (Searchlocation);
                            mMap.addMarker (markerOptions);
                        }
                        mMap.clear ();
                        //hier moet de zoek radius komen
                        mMap.addCircle (
                                new CircleOptions ()
                                        .center (latLng)
                                        .radius (this.radius)
                                        .strokeWidth (3f)
                                        .strokeColor (Color.RED)
                                        .fillColor (Color.argb (0x46, 40, 31, 107))
                        );
//                            hier moet de soort verharding van de weg komen
                        String mode = "Bicycle";
                        String url = getUrl (lat, lng, mode);
                        dataTransfer[0] = mMap;
                        dataTransfer[1] = url;
                        getNearbyPlacesData.execute (dataTransfer);
                        Toast.makeText (MapsActivity.this, "Toon dichtsbijzijnde mogelijkheden in " + Searchlocation, Toast.LENGTH_SHORT).show ();
                        mMap.moveCamera (CameraUpdateFactory.newLatLngZoom (latLng, 15.0f));
                        if(this.radius == 500) {
                            mMap.animateCamera (CameraUpdateFactory.zoomTo (15), this.radius, null);
                        }
                        if(this.radius == 1000) {
                            mMap.animateCamera (CameraUpdateFactory.zoomTo (14), this.radius, null);
                        }
                        if(this.radius == 5000) {
                            mMap.animateCamera (CameraUpdateFactory.zoomTo (12), this.radius, null);
                        }
                        if(this.radius == 10000) {
                            mMap.animateCamera (CameraUpdateFactory.zoomTo (11), this.radius, null);
                        }
                    } catch (IOException e) {
                        e.printStackTrace ();
                    }
                }
                break;
        }
    }

    private String getUrl(double latitude, double longitude, String nearbyPlace) {

        StringBuilder googlePlaceUrl = new StringBuilder ("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        googlePlaceUrl.append ("location=" + latitude + "," + longitude);
        googlePlaceUrl.append ("&radius=" + this.radius);
        googlePlaceUrl.append ("avoid=highways&mode=bicycling");
        googlePlaceUrl.append("&keyword=" + nearbyPlace);
//        googlePlaceUrl.append ("&keyword=" +nearbyPlace);
        googlePlaceUrl.append ("&sensor=true");
        googlePlaceUrl.append ("&key=" + "AIzaSyBxo_Umr5453x5ij04DVw-euVKNHxSEWPc");
        Log.d ("MapsActivity", "url = " + googlePlaceUrl.toString ());
        return googlePlaceUrl.toString ();
    }
    public void onConnected(@Nullable Bundle bundle) {
        requestLocationUpdates ();
    }
    @Override
    public void onConnectionSuspended(int i) {
    }
    public void requestLocationUpdates() {
        locationRequest = new LocationRequest ();
        locationRequest.setInterval (120000);
        locationRequest.setFastestInterval (120000);
        locationRequest.setPriority (LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission (this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
//            mFusedLocationClient.requestLocationUpdates (locationRequest, locationCallback, Looper.myLooper ());
        }
    }
    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission (this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale (this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions (this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_CODE);
            } else {
                ActivityCompat.requestPermissions (this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_CODE);
            }
            return false;
        } else
            return true;
    }
}
