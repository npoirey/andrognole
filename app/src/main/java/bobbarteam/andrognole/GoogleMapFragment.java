package bobbarteam.andrognole;

import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.app.Fragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import bobbarteam.andrognole.engine.location.LocationCallback;
import bobbarteam.andrognole.engine.location.LocationProvider;
import bobbarteam.andrognole.engine.location.ParkingLocation;
import bobbarteam.andrognole.engine.location.ParkingLocationDataSource;

/**
 * Created by jeremy on 11/03/15.
 */
public class GoogleMapFragment extends Fragment implements OnMapReadyCallback, LocationCallback {

    private static final String TAG = GoogleMapFragment.class.getSimpleName();
    private LocationProvider mLocationProvider;
    private ParkingLocation parking;
    private Location currentLocation;
    private MapView mapView;
    private GoogleMap map;
    private MarkerOptions parkingMarkerOptions;
    private Marker parkingMarker;
    private boolean autoMoveCamera;
    private int mapType;
    private boolean setToReset;
    //private boolean isEmulator = "goldfish".equals(Build.HARDWARE);

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        autoMoveCamera = sharedPref.getBoolean(getString(R.string.preferences_map_animate), true);
        mapType = sharedPref.getInt(getString(R.string.preferences_map_type), GoogleMap.MAP_TYPE_NORMAL);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_map, menu);
        super.onCreateOptionsMenu(menu, inflater);
        menu.findItem(R.id.menu_map_auto_move_camera).setChecked(autoMoveCamera);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                // Not implemented here
                return false;
            case R.id.menu_map_type:
                Log.d(TAG, "changing map type");
                if(map != null) {
                    mapType = map.getMapType();
                    if (mapType == GoogleMap.MAP_TYPE_NORMAL)
                        map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                    else
                        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                    mapType = map.getMapType();
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    sharedPref.edit().putInt(getString(R.string.preferences_map_type), mapType).apply();
                }
                return true;
            case R.id.menu_map_auto_move_camera:
                autoMoveCamera = !autoMoveCamera;
                Log.d(TAG, "switching auto camera mouvement to " + autoMoveCamera);
                item.setChecked(autoMoveCamera);
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                sharedPref.edit().putBoolean(getString(R.string.preferences_map_animate), autoMoveCamera).apply();
                return true;
            default:
                break;
        }
        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        View v = inflater.inflate(R.layout.fragment_map, container, false);
        MapsInitializer.initialize(getActivity());

        // Creation of the LocationProvider to get back localisation changes
        mLocationProvider = new LocationProvider(getActivity(), this);

        // Gets the MapView from the XML layout and creates it
        mapView = (MapView) v.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        return v;
    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        mapView.onResume();
        mapView.getMapAsync(this);

        // if setToReset, hardReset the map
        if (setToReset){
            hardReset();
        }

        //connection to position changes
        mLocationProvider.connect();
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
        mapView.onPause();

        // reset map for marker creation
        resetMap();

        // disconnect from the localisation provider
        mLocationProvider.disconnect();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        Log.i(TAG, "onLowMemory");
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onMapReady(GoogleMap gMap) {
        Log.i(TAG, "Map ready");
        // Set UI settings on the Google map
        map = gMap;
        map.setMyLocationEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.getUiSettings().setCompassEnabled(true);
        map.getUiSettings().setAllGesturesEnabled(true);
        map.getUiSettings().setMapToolbarEnabled(true);
        map.setMapType(mapType);
        checkParkingLocation();
    }

    /**
     * Check the parking location.
     * If parking location is null, we get the last one, else juste draw the current parking position
     */
    public void checkParkingLocation(){
        // If parking is null, we don't get any position so we try to get the last one and draw it on the map
        if (parking == null) {
            Log.i(TAG, "Parking postion null -> get the last one");
            getLastParkingPosition();
        } else {
            // We just draw the position on the map
            Log.i(TAG, "Parking postion not null -> draw the current parking position");
            setParking(parking);
        }
    }

    /**
     * handle a new current location by editing the compass
     * @param location
     */
    @Override
    public void handleNewLocation(Location location) {
        Log.i(TAG, "New Location handle on the GoogleMapFragment: " + location.toString());
        currentLocation = location;
        if(autoMoveCamera)
            moveCamera();
    }

    /**
     * Get the last parking location  in the database and set on the map if the result return a location
     */
    public void getLastParkingPosition(){
        Log.i(TAG, "Get the last parking position in the database");
        ParkingLocationDataSource parkingLocationDataSource = new ParkingLocationDataSource(getActivity().getApplicationContext());
        parkingLocationDataSource.open();
        ParkingLocation lastParkingLocation = parkingLocationDataSource.getLastParkingLocation();
        parkingLocationDataSource.close();
        // We find a location -> draw it on the map
        if (lastParkingLocation != null) {
            Log.i(TAG, "Last parking position find");
            setParking(lastParkingLocation);
        }
    }

    /**
     * Draw a marker for the parking location
     * @param p
     */
    public void setParking(ParkingLocation p) {
        Log.i(TAG, "Set the marker on the map to ParkingLocation : " + p.toString());
        // store the parking location
        parking = p;
        // The fragment is added on the activity so we can draw the marker without null exception
        if (this.isAdded()) {
            Log.i(TAG, "GoogleMapFragment added to the map, we can draw the marker");
            if (parkingMarkerOptions == null) {
                Log.i(TAG, "ParkingMarkerOptions is null");
                parkingMarkerOptions = new MarkerOptions().title(getString(R.string.marker_title))
                        .snippet(getString(R.string.marker_text))
                        .position(parking.getLatLng());
                parkingMarker = map.addMarker(parkingMarkerOptions);
            } else {
                Log.i(TAG, "ParkingMarkerOptions is not null");
                parkingMarker.setPosition(p.getLatLng());
            }
            moveCamera();
        }
    }

    /**
     * Move the camera on the different on the map
     */
    public void moveCamera(){
        LatLng location;
        Log.d(TAG, "Animating camera");
        // if the parking location is not null
        if (parking != null){

            // if the current location is not null, zoom with bounds
            if (currentLocation != null){
                location = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                // Get the distance between currentLocation and parkingLocation
                Location locParking = new Location("");
                locParking.setLatitude(parking.getLatitude());
                locParking.setLongitude(parking.getLongitude());


                // If the distance is > @value/min_bound_distance, we set a LatLngBound zoom
                if (currentLocation.distanceTo(locParking) > getResources().getInteger(R.integer.min_bound_distance)) {
                    LatLngBounds.Builder bounds = new LatLngBounds.Builder();
                    bounds.include(location);
                    bounds.include(parking.getLatLng());
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 150));
                // If not, zoom to the currentLocation
                } else {
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(location, getResources().getInteger(R.integer.default_zoom)));
                }
            // if the current position is null, zoom on parking location
            } else {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(parking.getLatLng(), getResources().getInteger(R.integer.default_zoom)));
            }
        } else {
            // if parking location is null and current not, zoom on currentLocation
            if (currentLocation != null){
                location = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(location, getResources().getInteger(R.integer.default_zoom)));
            }
        }
    }

    /**
     * Reset map when we pause the fragment to get clear map on the next resume
     */
    public void resetMap(){
        Log.i(TAG, "Soft reset call");
        if(map!=null)
            map.clear();
        parkingMarkerOptions = null;
    }

    /**
     * Hard reset for the reset database action in the settings
     */
    public void hardReset(){
        Log.i(TAG, "Hard reset call");
        map.clear();
        parkingMarkerOptions = null;
        parking = null;
        setToReset = false;
    }

    /**
     * Just set setToReset to true when fragment is not added to the MainActivity
     */
    public void setToReset(){
        Log.i(TAG, "setToReset call");
        setToReset = true;
    }
}
