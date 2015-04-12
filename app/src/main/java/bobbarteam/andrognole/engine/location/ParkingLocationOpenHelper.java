package bobbarteam.andrognole.engine.location;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Helper class to open connection to parking locations db
 */
public class ParkingLocationOpenHelper extends SQLiteOpenHelper {
    public static final String TAG = ParkingLocationOpenHelper.class.getSimpleName();

    private static final String DATABASE_NAME = "parkinglocations.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_PARKING_LOCATION = "parking_location";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";


    // Database creation sql statement
    private static final String DATABASE_CREATE_STRING = "create table "
            + TABLE_PARKING_LOCATION + "("
            + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_DATE + " integer, "
            + COLUMN_LATITUDE + " double, "
            + COLUMN_LONGITUDE + " double "
            + ");";

    /** Constructor */
    public ParkingLocationOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DATABASE_CREATE_STRING);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG,
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PARKING_LOCATION);
        onCreate(db);
    }
}
