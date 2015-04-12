package bobbarteam.andrognole.widget;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import bobbarteam.andrognole.R;
import bobbarteam.andrognole.engine.detection.DetectionService;

/**
 * Created by vivien on 19/03/15.
 */
public class WidgetClick extends Activity
{
    private static final int NOTIFICATION_ID = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context con = getApplicationContext();
        Intent srv = new Intent(con, DetectionService.class);
        srv.setAction(DetectionService.ACTION_FORCE);
        con.startService(srv);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setAutoCancel(true)
                        .setContentTitle(getString(R.string.notification_title));

        final NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());

        finish();
    }
}
