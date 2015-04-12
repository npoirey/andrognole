package bobbarteam.andrognole;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import bobbarteam.andrognole.engine.location.LocationCallback;
import bobbarteam.andrognole.engine.location.LocationProvider;
import bobbarteam.andrognole.engine.location.ParkingLocation;
import bobbarteam.andrognole.engine.location.ParkingLocationDataSource;

public class CompassFragment extends Fragment implements SensorEventListener, LocationCallback {
    private static final String TAG = CompassFragment.class.getSimpleName();
    // define the display assembly compass picture
    private ImageView image;

    // record the compass picture angle turned
    private float azimuth;

    // device sensor manager
    private SensorManager mSensorManager;
    private Sensor mOrientation;

    // device location manager
    private Location currentLocation;
    private Location parkingLocation;
    private LocationProvider mLocationProvider;
    private float currentDegree;
    TextView remainingDistance;
    private boolean setToReset = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "createView");
        View v = inflater.inflate(R.layout.fragment_compass, container, false);

        // Image that will rotate
        image = (ImageView) v.findViewById(R.id.imageViewCompass);
        // initialize your android device sensor capabilities
        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

        mLocationProvider = new LocationProvider(getActivity(), this);

        // TextView that will tell the user what degree is he heading
        remainingDistance = (TextView) v.findViewById(R.id.remainingDistance);

        return v;
    }

    @Override
    public void onResume() {
        Log.i(TAG, "resume");
        super.onResume();

        if (parkingLocation == null) {
            //find the parking location
            ParkingLocationDataSource parkingLocationDataSource = new ParkingLocationDataSource(getActivity().getApplicationContext());
            parkingLocationDataSource.open();
            ParkingLocation lastParkingLocation = parkingLocationDataSource.getLastParkingLocation();
            if (lastParkingLocation != null) {
                Log.d(TAG, "parking position : " + lastParkingLocation);
                parkingLocation = new Location("");
                parkingLocation.setLongitude(lastParkingLocation.getLongitude());
                parkingLocation.setLatitude(lastParkingLocation.getLatitude());
            }
            parkingLocationDataSource.close();
        }

        mSensorManager.registerListener(this, mOrientation, SensorManager.SENSOR_DELAY_NORMAL);

        //register to position changes
        mLocationProvider.connect();
        if(setToReset){
            resetCompass();
        }
    }

    @Override
    public void onPause() {
        Log.i(TAG, "pause");
        super.onPause();

        // to stop the listener and save battery
        mSensorManager.unregisterListener(this);
        mLocationProvider.disconnect();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        azimuth = event.values[0];
        refreshCompassOrientation();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // not in use
    }

    /**
     * handle a new current location by editing the compass
     * @param location
     */
    @Override
    public void handleNewLocation(Location location) {
        Log.i(TAG, "New Location handle : COMPASS");
        currentLocation = location;
        refreshCompass();
    }

    public void setParking(ParkingLocation p) {
        Log.i(TAG, "setting parking : " + p);
        parkingLocation = new Location("");
        parkingLocation.setLongitude(p.getLongitude());
        parkingLocation.setLatitude(p.getLatitude());
        refreshCompass();
    }

    private void refreshCompass(){
        refreshCompassOrientation();
        refreshRemainingDistance();
    }

    private void refreshRemainingDistance() {
        if(currentLocation != null && parkingLocation!= null) {
            int distanceInMeter = (int) currentLocation.distanceTo(parkingLocation);
            if(distanceInMeter > 1000){
                // will print 1254m as 1.25km
                int km = distanceInMeter / 1000;
                int m = distanceInMeter % 1000;

                remainingDistance.setText( km + getString(R.string.number_decimal_separator)
                        + m/10 + " km");
            }
            else{
                remainingDistance.setText(Integer.toString(distanceInMeter) + " m");
            }
        }
    }

    private void refreshCompassOrientation() {
        if(currentLocation != null && parkingLocation!= null){
            // bearing = angle from north, dir. east
            float bearing = currentLocation.bearingTo(parkingLocation); // (it's already in degrees)
            if(bearing <0)
                bearing += 360;
            float direction = (360 - azimuth + bearing) % 360;

            Log.v(TAG, "direction:" + direction + "azimut:" + azimuth + "bearing:" + bearing);

            // to make smooth transition from 359 to 0 degree
            if((currentDegree < 90 && direction > 270)){
                currentDegree += 360;
            }
            else if(currentDegree > 270 && direction < 90) {
                currentDegree -= 360;
            }

            // create a rotation animation (reverse turn degree degrees)
            RotateAnimation ra = new RotateAnimation(
                    currentDegree,
                    direction,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f);

            ra.setDuration(210);
            ra.setFillAfter(true);
            image.startAnimation(ra);
            currentDegree = direction;
        }
        else{
            Log.v(TAG, "can't refresh compasss orientation. currentLocation:"+currentLocation + " parkingLocation:" + parkingLocation);
        }
    }

    public void resetCompass(){
        parkingLocation = null;
        remainingDistance.setText("0 m");
        // create a rotation animation (reverse turn degree degrees)
        RotateAnimation ra = new RotateAnimation(
                currentDegree,
                0,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);

        ra.setDuration(210);
        ra.setFillAfter(true);
        image.startAnimation(ra);
        currentDegree = 0;
        setToReset = false;
    }

    public void setToReset(){
        setToReset = true;
    }
}
