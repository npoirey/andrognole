package bobbarteam.andrognole.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import bobbarteam.andrognole.R;
import bobbarteam.andrognole.widget.WidgetClick;

/**
 * Created by vivien on 19/03/15.
 */
public class CompassWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int i = 0; i < appWidgetIds.length; i++) {
            int appWidgetId = appWidgetIds[i];

            Intent intent = new Intent(context, WidgetClick.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_compass);
            views.setOnClickPendingIntent(R.id.widget_compass, pendingIntent);
            appWidgetManager.updateAppWidget(appWidgetId, views);

        }
    }
}
