package bobbarteam.andrognole.engine.location;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import bobbarteam.andrognole.R;

/**
 * DAO class for accessing parking locations
 */
public class ParkingLocationDataSource {
    public static final String TAG = ParkingLocationDataSource.class.getSimpleName();
    public static final int PARKING_LIMIT = 5;

    // Database fields
    private SQLiteDatabase database;
    private ParkingLocationOpenHelper dbHelper;
    private String[] allColumns = { ParkingLocationOpenHelper.COLUMN_ID,
            ParkingLocationOpenHelper.COLUMN_DATE,
            ParkingLocationOpenHelper.COLUMN_LATITUDE,
            ParkingLocationOpenHelper.COLUMN_LONGITUDE};

    /**
     * constructor
     * @param context
     */
    public ParkingLocationDataSource(Context context) {
        dbHelper = new ParkingLocationOpenHelper(context);
    }

    /**
     * open the connection to the db
     * @throws SQLException
     */
    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    /**
     * close the connection to the db
     */
    public void close() {
        dbHelper.close();
    }

    /**
     * create a new parkingLocation in db
     * @param parkingLocation
     * @return
     */
    public ParkingLocation createParkingLocation(ParkingLocation parkingLocation) {
        Log.i(TAG, "creating new parking location, timestamp " + parkingLocation.getDate().getMillis());
        ContentValues values = new ContentValues();
        values.put(ParkingLocationOpenHelper.COLUMN_DATE, parkingLocation.getDate().getMillis());
        values.put(ParkingLocationOpenHelper.COLUMN_LATITUDE, parkingLocation.getLatitude());
        values.put(ParkingLocationOpenHelper.COLUMN_LONGITUDE, parkingLocation.getLongitude());

        long insertId = database.insert(ParkingLocationOpenHelper.TABLE_PARKING_LOCATION,
                null, values);
        Cursor cursor = database.query(ParkingLocationOpenHelper.TABLE_PARKING_LOCATION,
                allColumns, ParkingLocationOpenHelper.COLUMN_ID + " = " + insertId, null,
                null, null, null);
        cursor.moveToFirst();
        ParkingLocation newParkingLocation = cursorToParkingLocation(cursor);
        cursor.close();
        maintainMaxNumberOfEntries();
        return newParkingLocation;
    }

    /**
     * delete a parking location
     * @param parkingLocation
     */
    public void deleteParkingLocation(ParkingLocation parkingLocation) {
        long id = parkingLocation.getId();
        Log.i(TAG, "parkingLocation deleted with id: " + id);
        database.delete(ParkingLocationOpenHelper.TABLE_PARKING_LOCATION,
                ParkingLocationOpenHelper.COLUMN_ID
                + " = " + id, null);
    }

    /**
     * delete a parking location
     */
    public void deleteAllParkingLocation() {
        database.delete(ParkingLocationOpenHelper.TABLE_PARKING_LOCATION, null, null);
    }

    /**
     * get the list of all parking locations
     * @return
     */
    public List<ParkingLocation> getAllParkingLocation() {
        List<ParkingLocation> parkingLocations = new ArrayList<ParkingLocation>();

        Cursor cursor = database.query(ParkingLocationOpenHelper.TABLE_PARKING_LOCATION,
                allColumns, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            ParkingLocation parkingLocation = cursorToParkingLocation(cursor);
            parkingLocations.add(parkingLocation);
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return parkingLocations;
    }

    /**
     * get the count of all parking locations
     * @return
     */
    public int getCountParkingLocation() {
        Cursor cursor = database.query(ParkingLocationOpenHelper.TABLE_PARKING_LOCATION,
                allColumns, null, null, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    /**
     * get a parking location from index
     * @return
     */
    public ParkingLocation getParkingLocationByIndex(int index) {
        Cursor cursor = database.query(ParkingLocationOpenHelper.TABLE_PARKING_LOCATION,
                null, null, null, null, null, ParkingLocationOpenHelper.COLUMN_DATE + " ASC", PARKING_LIMIT+"");
        ParkingLocation parkingLocation = null;
        if(index >= 0 && cursor.moveToPosition(index) ){
            parkingLocation = cursorToParkingLocation(cursor);
        }
        cursor.close();
        return parkingLocation;
    }

    /**
     * return the last parking location based on date
     * @return the parking location or null if there is none
     */
    public ParkingLocation getLastParkingLocation(){
        Cursor cursor = database.query(ParkingLocationOpenHelper.TABLE_PARKING_LOCATION,
                null, null, null, null, null, ParkingLocationOpenHelper.COLUMN_DATE + " DESC", "1");
        if(cursor.getCount() < 1){
            return null;
        }
        cursor.moveToFirst();
        ParkingLocation parkingLocation = cursorToParkingLocation(cursor);
        cursor.close();
        return parkingLocation;
    }

    public ParkingLocation cursorToParkingLocation(Cursor cursor) {
        ParkingLocation parkingLocation = new ParkingLocation();
        parkingLocation.setId(cursor.getLong(0));
        parkingLocation.setDate(new DateTime(cursor.getLong(1)));
        parkingLocation.setLatitude(cursor.getDouble(2));
        parkingLocation.setLongitude(cursor.getDouble(3));
        return parkingLocation;
    }

    private void maintainMaxNumberOfEntries(){
        Cursor cursor = database.query(ParkingLocationOpenHelper.TABLE_PARKING_LOCATION,
                null, null, null, null, null, ParkingLocationOpenHelper.COLUMN_DATE + " DESC", null);
        Log.i(TAG, "currently "+ cursor.getCount() +" entries");
        if(cursor.getCount()>PARKING_LIMIT){
            Log.i(TAG, "Parking limit exceded ("+cursor.getCount()+" in db and max is " + PARKING_LIMIT+")" + ", will proceed to deletions");
            cursor.moveToPosition(PARKING_LIMIT);
            long minTimestampAllowed = cursor.getLong(cursor.getColumnIndex(ParkingLocationOpenHelper.COLUMN_DATE));

            Log.i(TAG, "deleting entries older than timestamp "+ minTimestampAllowed + " date: " + (new DateTime(minTimestampAllowed)));
            int count = database.delete(ParkingLocationOpenHelper.TABLE_PARKING_LOCATION, ParkingLocationOpenHelper.COLUMN_DATE + "<=" + minTimestampAllowed, null);
            Log.i(TAG, "deleted "+ count + " entries");
        }
        cursor.close();
    }
}
