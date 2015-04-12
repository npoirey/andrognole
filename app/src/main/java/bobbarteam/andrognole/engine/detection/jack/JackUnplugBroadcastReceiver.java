package bobbarteam.andrognole.engine.detection.jack;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Created by Sacapuces on 3/15/2015.
 */
public class JackUnplugBroadcastReceiver extends BroadcastReceiver{
    private static final String TAG = JackUnplugBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {

        if(intent.getAction().equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)){
            Intent service = new Intent(context, JackDetectionServiceAdapter.class);
            context.startService(service);
        }
    }
}
