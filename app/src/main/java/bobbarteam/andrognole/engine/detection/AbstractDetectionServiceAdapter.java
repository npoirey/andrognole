package bobbarteam.andrognole.engine.detection;

import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import bobbarteam.andrognole.engine.detection.nfc.NFCDetectionServiceAdapter;

/**
 * Common interface to adapt a detection toward DetectionService
 */
public abstract class AbstractDetectionServiceAdapter extends IntentService {
    private static final String TAG = AbstractDetectionServiceAdapter.class.getSimpleName();

    protected AbstractDetectionServiceAdapter() {
        super("AbstractDetectionServiceAdapter");
    }

    /**
     * Notify a parking detection
     */
    protected void notifyParkingDetection(boolean askConfirmation){
        Log.i(TAG, "notifying parking");
        Context con = getApplicationContext();
        Intent srv = new Intent(con, DetectionService.class);
        if(askConfirmation) {
            srv.setAction(DetectionService.ACTION_DETECT);
        } else{
            srv.setAction(DetectionService.ACTION_FORCE);
        }
        con.startService(srv);
    }
}
