package bobbarteam.andrognole;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.util.Log;

import org.joda.time.DateTime;


import bobbarteam.andrognole.configuration.AppPreferences;
import bobbarteam.andrognole.engine.detection.DetectionService;
import bobbarteam.andrognole.engine.location.ParkingLocation;
import bobbarteam.andrognole.engine.location.ParkingLocationDataSource;
import bobbarteam.andrognole.gesture.OnSwipeTouchListener;

public class MainActivity extends Activity {

    GoogleMapFragment googleFragment;
    CompassFragment compassFragment;
    ParkingInfoFragment parkingInfoFragment;
    ButtonFragment buttonFragment;
    SwipeLeftFragment swipeLeftFragment;
    SwipeRightFragment swipeRightFragment;
    private static final String TAG = MainActivity.class.getSimpleName();
    private int nbParkingLocation;
    private int index;
    private boolean isRunning;
    // Our handler for received Intents about a new parking location
    private BroadcastReceiver mParkingBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int cas = intent.getIntExtra("case", 0);
            Log.i(TAG, "Received a broadcast. case:" + cas);
            if(cas == 1){
                Log.i(TAG, "Received a broadcast about a new parking location");
                // Get extra data included in the Intent
                ParkingLocation parkingLocation = intent.getParcelableExtra("parkingLocation");
                nbParkingLocation = Math.min(nbParkingLocation+1, ParkingLocationDataSource.PARKING_LIMIT);
                index = nbParkingLocation - 1;
                setCurrentParking(parkingLocation);
                parkingInfoFragment.setDate(parkingLocation.getDate());
                refreshSwipeFragment();
            } else if (cas == 2){
                Log.i(TAG, "Received a broadcast about reset database");
                if(googleFragment.isAdded())
                    googleFragment.hardReset();
                else
                    googleFragment.setToReset();
                if(compassFragment.isAdded())
                    compassFragment.resetCompass();
                else
                    compassFragment.setToReset();
                parkingInfoFragment.reset();
                index = 0;
                nbParkingLocation = 0;
                refreshSwipeFragment();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate)");

        setContentView(R.layout.main_activity);

        nbParkingLocation = getCountParkingPosition();
        index = nbParkingLocation - 1;

        this.findViewById(R.id.swipe_left_fragment).setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeTowardRight() {
                Log.i(TAG, "Swipe Right " + index + "  " + nbParkingLocation);
                if (index > 0) {
                    index--;
                    ParkingLocation p = getParkingPositionAtIndex();
                    refreshSwipeFragment();
                    if (p != null) {
                        parkingInfoFragment.setDate(p.getDate());
                    } else {
                        parkingInfoFragment.reset();
                    }
                    setCurrentParking(p);
                }
            }
        });
        this.findViewById(R.id.swipe_right_fragment).setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeTowardLeft() {
                Log.i(TAG, "Swipe Left " + index + "  " + nbParkingLocation);
                if (index < nbParkingLocation - 1) {
                    index++;
                    ParkingLocation p = getParkingPositionAtIndex();
                    refreshSwipeFragment();
                    if (p != null) {
                        parkingInfoFragment.setDate(p.getDate());
                    } else {
                        parkingInfoFragment.reset();
                    }
                    setCurrentParking(p);
                }
            }
        });

        if (savedInstanceState == null) {
            // Récupérez le FragmentManager
            FragmentManager fm = getFragmentManager();
            // Trouver si le fragment que nous souhaitons afficher appartient à la backstack
            googleFragment = (GoogleMapFragment) fm.findFragmentByTag("GoogleMapFragment");
            compassFragment = (CompassFragment) fm.findFragmentByTag("CompassFragment");
            buttonFragment = (ButtonFragment) fm.findFragmentByTag("buttonFragment");
            swipeLeftFragment = (SwipeLeftFragment) fm.findFragmentByTag("swipeFragment");
            swipeRightFragment = (SwipeRightFragment) fm.findFragmentByTag("swipeFragment");

            //preferenceFragment = (AppPreferences) fm.findFragmentByTag("AppFragment");
            if (null == googleFragment) {
                googleFragment = new GoogleMapFragment();
            }
            if (null == compassFragment) {
                compassFragment = new CompassFragment();
            }
            if (null == parkingInfoFragment) {
                parkingInfoFragment = new ParkingInfoFragment();
            }
            if (null == buttonFragment) {
                buttonFragment = new ButtonFragment();
            }
            if (null == swipeLeftFragment) {
                swipeLeftFragment = new SwipeLeftFragment();
            }
            if (null == swipeRightFragment) {
                swipeRightFragment = new SwipeRightFragment();
            }

            // Ajoutez les fragments à son layout et effectuez le commit
            if (isOnline()){
                fm.beginTransaction().add(R.id.main_frame_layout, googleFragment).commit();
                refreshSwipeFragment();
            } else {
                fm.beginTransaction().add(R.id.main_frame_layout, compassFragment).commit();
                refreshSwipeFragment();
            }
            fm.beginTransaction().add(R.id.parking_info_frame_layout, parkingInfoFragment).commit();
            fm.beginTransaction().add(R.id.button_frame_layout, buttonFragment).commit();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        isRunning = true;
        // Get count of parking location in the datastore
        nbParkingLocation = getCountParkingPosition();
        refreshSwipeFragment();

        //register to received message of new parking location
        LocalBroadcastManager.getInstance(this).registerReceiver(mParkingBroadcastReceiver,
                new IntentFilter(DetectionService.INTENT_NEW_PARKING));
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
        isRunning = false;
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mParkingBroadcastReceiver);
    }


    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                showSettings();
                return true;
            default:
                return false;
        }
    }

    public boolean isOnline() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    public void changeMode(View v) {
        // Is the toggle on?
        boolean on = ((Switch) findViewById(R.id.switch_mode)).isChecked();
        if (on) {
            replaceFragment(R.id.main_frame_layout, compassFragment);
        } else {
            replaceFragment(R.id.main_frame_layout, googleFragment);
        }
    }

    public void replaceFragment(int id, Fragment fragment){
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(id, fragment);
        ft.addToBackStack(fragment.getClass().toString());
        ft.commit();
    }

    public void showSettings(){
        Intent intent = new Intent(this, AppPreferences.class);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            if (fm.getBackStackEntryAt(fm.getBackStackEntryCount()-1).getName().equals("before_setting")) {
                fm.popBackStack();
            } else if (fm.findFragmentByTag("before_setting") == null){
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }

    public void savePosition(View view){
        Context con = getApplicationContext();
        Intent srv = new Intent(con, DetectionService.class);
        srv.setAction(DetectionService.ACTION_FORCE);
        con.startService(srv);
    }

    public ParkingLocation getParkingPositionAtIndex(){
        //find the parking location
        ParkingLocationDataSource parkingLocationDataSource = new ParkingLocationDataSource(getApplicationContext());
        parkingLocationDataSource.open();
        ParkingLocation parkingLocationByIndex = parkingLocationDataSource.getParkingLocationByIndex(index);
        parkingLocationDataSource.close();
        Log.i(TAG, "Swipe Get an old location (index : " + index);
        if (parkingLocationByIndex != null) {
            Log.i(TAG, "Swipe "+parkingLocationByIndex.toString());
            return parkingLocationByIndex;
        } else {
            Log.i(TAG, "Swipe NULL value");
            return null;
        }
    }

    public int getCountParkingPosition(){
        //find the parking location
        ParkingLocationDataSource parkingLocationDataSource = new ParkingLocationDataSource(getApplicationContext());
        parkingLocationDataSource.open();
        int i = parkingLocationDataSource.getCountParkingLocation();
        parkingLocationDataSource.close();
        return i;
    }

    public void setCurrentParking(ParkingLocation parkingLocation){
        googleFragment.setParking(parkingLocation);
        compassFragment.setParking(parkingLocation);
    }

    public void refreshSwipeFragment(){
        if(isRunning){
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            Log.i(TAG, "refresh swipe fragments " + index + " " + nbParkingLocation);

            if(!swipeLeftFragment.isAdded() && !swipeRightFragment.isAdded()){
                ft.add(R.id.swipe_left_fragment, swipeLeftFragment);
                ft.add(R.id.swipe_right_fragment, swipeRightFragment);
                ft.commit();
                ft = getFragmentManager().beginTransaction();
            }

            //cas 1 : aucune position ou une seule
            if(nbParkingLocation <= 1){
                ft.detach(swipeLeftFragment);
                ft.detach(swipeRightFragment);
            }
            else {
                //cas 2 : on est à la plus ancienne position
                if(index == 0){
                    ft.detach(swipeLeftFragment);
                    ft.attach(swipeRightFragment);
                }

                //cas 3 : on est à la plus récente position
                else if(index >= nbParkingLocation - 1){
                    ft.attach(swipeLeftFragment);
                    ft.detach(swipeRightFragment);
                }

                //cas 4 : on est a une position intermediaire
                else{
                    ft.attach(swipeLeftFragment);
                    ft.attach(swipeRightFragment);
                }
            }
            ft.commit();
        }
    }

}
