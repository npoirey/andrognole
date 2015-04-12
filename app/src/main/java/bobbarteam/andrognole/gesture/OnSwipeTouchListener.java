package bobbarteam.andrognole.gesture;

import android.content.Context;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

/**
 * Created by jeremy on 18/03/15.
 */
public class OnSwipeTouchListener implements View.OnTouchListener {

    private final GestureDetector gestureDetector;
    private int width;
    private int limit;


    public OnSwipeTouchListener(Context context) {
        gestureDetector = new GestureDetector(context, new GestureListener());
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager window = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        window.getDefaultDisplay().getMetrics(metrics);
        width = metrics.widthPixels;
        limit = width * 10 / 100;
    }

    public void onSwipeTowardLeft() {
    }

    public void onSwipeTowardRight() {
    }

    public boolean onTouch(View v, MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    private final class GestureListener extends GestureDetector.SimpleOnGestureListener {

        private static final int SWIPE_DISTANCE_THRESHOLD = 10;
        private static final int SWIPE_VELOCITY_THRESHOLD = 10;
        private final String TAG = GestureListener.class.getSimpleName();


        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            Log.i(TAG, "Swipe " + String.valueOf(e1.getX()) + "  " + limit);
            if (e1.getX() < limit || e1.getX() > width-limit) {
                float distanceX = e2.getX() - e1.getX();
                float distanceY = e2.getY() - e1.getY();
                if (Math.abs(distanceX) > Math.abs(distanceY) && Math.abs(distanceX) > SWIPE_DISTANCE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (distanceX > 0) {
                        Log.i(TAG, "onSwipeTowardRight");
                        onSwipeTowardRight();
                    } else {
                        Log.i(TAG, "onSwipeTowardLeft");
                        onSwipeTowardLeft();
                    }
                    return true;
                }
                return false;
            }
            return false;
        }
    }
}