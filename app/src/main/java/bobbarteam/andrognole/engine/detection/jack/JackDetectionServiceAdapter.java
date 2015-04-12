package bobbarteam.andrognole.engine.detection.jack;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import bobbarteam.andrognole.R;
import bobbarteam.andrognole.engine.detection.AbstractDetectionServiceAdapter;

/**
 * Created by Sacapuces on 3/15/2015.
 */
public class JackDetectionServiceAdapter extends AbstractDetectionServiceAdapter {
    private static final String TAG = JackDetectionServiceAdapter.class.getSimpleName();

    public JackDetectionServiceAdapter() {
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "onHandleIntent");
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean jackEnabled = sharedPref.getBoolean(getString(R.string.preferences_jack_active), false);
        boolean askConfirmation = sharedPref.getBoolean(getString(R.string.preferences_jack_ask_confirmation), true);
        Log.i(TAG, "going noisy. jackEnabled:"+jackEnabled + " askConfirmation:" + askConfirmation);
        if(jackEnabled) {
            notifyParkingDetection(askConfirmation);
        }
    }
}
