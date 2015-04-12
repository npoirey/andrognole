package bobbarteam.andrognole.engine.detection;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import org.joda.time.DateTime;

import java.util.Timer;
import java.util.TimerTask;

import bobbarteam.andrognole.R;
import bobbarteam.andrognole.engine.location.LocationCallback;
import bobbarteam.andrognole.engine.location.LocationProvider;
import bobbarteam.andrognole.engine.location.ParkingLocation;
import bobbarteam.andrognole.engine.location.ParkingLocationDataSource;

public class DetectionService  extends IntentService implements LocationCallback {
    private static final String TAG = DetectionService.class.getSimpleName();
    Handler mMainThreadHandler = null;
    private LocationProvider mLocationProvider;
    private static ParkingLocation lastParkingLocation;
    private static final int NOTIFICATION_ID = 1;

    public static final String ACTION_DISMISS = "DISMISS";
    public static final String ACTION_VALIDATE = "VALIDATE";
    public static final String ACTION_DETECT = "DETECT";
    public static final String ACTION_FORCE = "FORCE";

    public static final String INTENT_NEW_PARKING = "INTENT_NEW_PARKING";

    public static boolean activateNotification = true;
    private DateTime maxTime;
    private float targetAccuracy;
    private Timer timer;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     */
    public DetectionService() {
        super(DetectionService.class.getName());
        mMainThreadHandler = new Handler();
    }

    public void handleNewLocation(final Location location) {
        Log.d(TAG, "received a new location : " + location.toString());
        prepareLocation(location);
        //if we didn't get any location before the timer ends we wait to have at least one
        if (DateTime.now().isAfter(maxTime)) {
            Log.i(TAG, "received the location after max delay, using it no matter the accuracy" + location.getAccuracy());
            stopHandlingLocations();
        }
        // if the accuracy is good enough we can stop
        else if(location.hasAccuracy() && location.getAccuracy() <= targetAccuracy){
            Log.i(TAG, "received the location with accuracy " + location.getAccuracy() +". stopping");
            stopHandlingLocations();
        }
    }

    private void prepareLocation(final Location location){
        lastParkingLocation = new ParkingLocation();
        lastParkingLocation.setDate(new DateTime());
        lastParkingLocation.setLatitude(location.getLatitude());
        lastParkingLocation.setLongitude(location.getLongitude());
    }

    private void storeLocation(){
        if(lastParkingLocation != null) {
            Log.i(TAG, "storing location : " + lastParkingLocation);
            ParkingLocationDataSource datasource = new ParkingLocationDataSource(getApplicationContext());
            datasource.open();
            lastParkingLocation = datasource.createParkingLocation(lastParkingLocation);
            datasource.close();
            mMainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), getString(R.string.toast_position_stored), Toast.LENGTH_LONG).show();
                }
            });
            //launch an intent broadcasting a new position has been saved
            Log.d(TAG, "Broadcasting new parking location");
            Intent intent = new Intent(INTENT_NEW_PARKING);
            intent.putExtra("parkingLocation", lastParkingLocation);
            intent.putExtra("case", 1);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
        stopSelf();
    }

    private void startHandlingLocations(){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        int maxDelay = Integer.valueOf(sharedPref.getString(getString(R.string.preferences_general_max_delay), "10"));
        targetAccuracy = Float.valueOf(sharedPref.getString(getString(R.string.preferences_general_desired_accuracy), "10"));
        maxTime = DateTime.now().plusSeconds(maxDelay);
        Log.d(TAG,"starting handling location with maxDelay="+maxDelay +" and accuracy="+targetAccuracy);
        mLocationProvider.connect();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Log.d(TAG, "max delay spent");
                if(lastParkingLocation != null){
                    Log.d(TAG, "trying to store location after max delay");
                    stopHandlingLocations();
                }
            }
        }, maxDelay * 1000);
    }

    private void stopHandlingLocations(){
        mLocationProvider.disconnect();
        timer.cancel();
        if (activateNotification) {
            createParkingNotification();
        } else {
            storeLocation();
        }
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "created");
        super.onCreate();
        timer = new Timer();
        mLocationProvider = new LocationProvider(this, this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, "handling intent : " + action);
        if(action != null){
            if(action.equals(ACTION_DETECT)){
                Log.i(TAG, "intent for a new detection");
                activateNotification = true;
                //connect the location provider and wait for callback
                startHandlingLocations();
                //mLocationProvider.connect();
            } else if(action.equals(ACTION_FORCE)){
                Log.i(TAG, "intent for forcing insertion");
                activateNotification = false;
                //connect the location provider and wait for callback
                //mLocationProvider.connect();
                startHandlingLocations();
            } else if(action.equals(ACTION_DISMISS)){
                hideNotification();
                stopSelf();
            } else if(action.equals(ACTION_VALIDATE)){
                hideNotification();
                storeLocation();
            }
        }
    }

    private void hideNotification(){
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(NOTIFICATION_ID);
        Log.i(TAG, "canceled notification");
    }

    private void createParkingNotification(){

        Intent validateIntent = new Intent(this, DetectionService.class);
        validateIntent.setAction(ACTION_VALIDATE);
        PendingIntent validatePendingIntent =
                PendingIntent.getService(getApplicationContext(), 0, validateIntent, 0);

        Intent dismissIntent = new Intent(this, DetectionService.class);
        dismissIntent.setAction(ACTION_DISMISS);
        PendingIntent dismissPendingIntent =
                PendingIntent.getService(getApplicationContext(), 0, dismissIntent, 0);

        Bitmap largeIcon= (((BitmapDrawable) this.getResources().getDrawable(R.drawable.ic_launcher)).getBitmap());

        NotificationCompat.Builder mBuilder =
            new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(largeIcon)
                .setAutoCancel(true)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text))
                .setContentIntent(validatePendingIntent)
                .setPriority(Notification.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(R.drawable.ic_action_delete,
                        getString(R.string.notification_text_dismiss),
                        dismissPendingIntent)
                .addAction(R.drawable.ic_action_done,
                        getString(R.string.notification_text_validation),
                        validatePendingIntent);



        final NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
}
