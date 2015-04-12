package bobbarteam.andrognole.engine.detection.nfc;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * activité nécéssaire pour la detection du NFC, elle se charge uniquement de lancer le service
 * lié et n'a pas pour vocation d'être affichée
 */
public class NFCDetectionActivity extends Activity {
    private static final String TAG = NFCDetectionActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Detection Activity created");
        Context con = getApplicationContext();
        Intent srv = new Intent(con, NFCDetectionServiceAdapter.class);
        Log.i(TAG, "starting nfc service");
        con.startService(srv);
        Log.i(TAG, "nfc service started");
        finish();
    }
}
