package bobbarteam.andrognole.engine.detection.nfc;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import bobbarteam.andrognole.R;
import bobbarteam.andrognole.engine.detection.AbstractDetectionServiceAdapter;

/**
 * Created by sacapuces on 13/02/15.
 */
public class NFCDetectionServiceAdapter extends AbstractDetectionServiceAdapter {
    private static final String TAG = NFCDetectionServiceAdapter.class.getSimpleName();

    public NFCDetectionServiceAdapter() {
    }

    /**
     * The IntentService calls this method from the default worker thread with
     * the intent that started the service. When this method returns, IntentService
     * stops the service, as appropriate.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "onHandleIntent");
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean nfcEnabled = sharedPref.getBoolean(getString(R.string.preferences_nfc_active), false);
        Log.i(TAG, "scanned NFC. nfcEnabled:"+nfcEnabled);
        if(nfcEnabled) {
            notifyParkingDetection(false);
        }
    }
}
