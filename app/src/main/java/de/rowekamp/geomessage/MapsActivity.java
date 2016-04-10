package de.rowekamp.geomessage;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap mMap;
    private LocationManager locationManager;
    private String provider;
    private Location currentLocation;
    private double targetLongitude;
    private double targetLatitude;
    private String contact;
    private String expiryDateHR;
    private boolean mapReady = false;
    private int radius;
    private long messageId;
    private Marker target;
    private Marker current;
    private Location targetLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Get data from mainActivity
        Intent intent = getIntent();
        targetLongitude = intent.getDoubleExtra("longitude", 0);
        targetLatitude = intent.getDoubleExtra("latitude", 0);
        targetLocation = new Location("");
        targetLocation.setLatitude(targetLatitude);
        targetLocation.setLongitude(targetLongitude);
        contact = intent.getStringExtra("contactName");
        messageId = intent.getLongExtra("messageId",0);
        if(contact == null || contact.equals("")){
            contact = intent.getStringExtra("phoneNumber");
        }
        radius = intent.getIntExtra("radius",5);


        //Location Manager
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, false);
        currentLocation = locationManager.getLastKnownLocation(provider);
    }

    @Override
    protected void onResume(){
        super.onResume();
        locationManager.requestLocationUpdates(provider, 400, 1, this);
    }

    @Override
    protected void onPause(){
        super.onPause();
        locationManager.removeUpdates(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng targetLatLng = new LatLng(targetLatitude, targetLongitude);
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL); //TODO use type defined in settings

        //target location
        MarkerOptions targetOptions = new MarkerOptions()
                .position(targetLatLng)
                .title(contact)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.comment_map_icon));
        target = mMap.addMarker(targetOptions);

        CircleOptions radiusOptions = new CircleOptions()
                .center(targetLatLng)
                .radius(radius)
                .strokeColor(Color.YELLOW);
        mMap.addCircle(radiusOptions);

        //current location
        MarkerOptions currentOptions = new MarkerOptions()
                .position(new LatLng(0,0))
                .visible(false)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.self_map_icon));
        current = mMap.addMarker(currentOptions);

        if(currentLocation != null){
            //current location
            current.setPosition(new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude()));
            current.setVisible(true);

            //add the distance between current location and target
            float distance = currentLocation.distanceTo(targetLocation);
            if (distance > 1000){
                target.setSnippet(distance/1000+"km");
            } else{
                target.setSnippet(distance+"m");
            }

            //calculate zoom level and place camera in the middle
            mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
                @Override
                public void onCameraChange(CameraPosition cameraPosition) {
                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    builder.include(new LatLng(targetLatitude,targetLongitude));
                    builder.include(new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude()));
                    LatLngBounds bounds = builder.build();
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 110));
                    mMap.setOnCameraChangeListener(null);
                }
            });
        }else{
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(targetLatLng,12));
        }
        target.showInfoWindow();
        mapReady = true;
    }

    @Override
    public void onLocationChanged(Location currentLocation) {
        this.currentLocation = currentLocation;
        float distance = currentLocation.distanceTo(targetLocation);
        if(mapReady){
            //update the distance between current location and target
            if (distance > 1000){
                target.setSnippet(distance/1000+"km");
            } else{
                target.setSnippet(distance+"m");
            }
            target.showInfoWindow();

            //update current location
            current.setPosition(new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude()));
            if(!current.isVisible()) {
                current.setVisible(true);
            }
        }
        if(distance <= radius){
            //fetch message from server
            Intent intent = new Intent(this, ViewActivity.class);
            intent.putExtra("messageId",messageId);
            intent.putExtra("latitude",currentLocation.getLatitude());
            intent.putExtra("longitude",currentLocation.getLongitude());
            intent.putExtra("sender",contact);
            startActivity(intent);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {
       Toast.makeText(this, "Enabled new provider " + provider, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderDisabled(String provider) {
       Toast.makeText(this, "Disabled provider " + provider, Toast.LENGTH_SHORT).show();
    }
}
