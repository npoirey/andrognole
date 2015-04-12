package bobbarteam.andrognole.engine.location;

import com.google.android.gms.maps.model.LatLng;
import android.os.Parcel;
import android.os.Parcelable;

import org.joda.time.DateTime;


/**
 * A parking location
 * link a place (latlong) with a date
 */
public class ParkingLocation implements Parcelable {
    private long id;
    private DateTime date;
    private double latitude;
    private double longitude;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public DateTime getDate() {
        return date;
    }

    public void setDate(DateTime date) {
        this.date = date;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public LatLng getLatLng(){
        return new LatLng(latitude, longitude);
    }

    // Will be used by the ArrayAdapter in the ListView
    @Override
    public String toString() {
        return "[" + latitude +", " + longitude + "] the " + date + " (id:" + id +")";
    }

    /** Used to give additional hints on how to process the received parcel.*/
    @Override
    public int describeContents() {
        // ignore for now
        return 0;
    }

    @Override
    public void writeToParcel(Parcel pc, int flags) {
        pc.writeLong(id);
        pc.writeLong(date.getMillis());
        pc.writeDouble(latitude);
        pc.writeDouble(longitude);
    }

    /** Static field used to regenerate object, individually or as arrays */
    public static final Parcelable.Creator<ParkingLocation> CREATOR =
            new Parcelable.Creator<ParkingLocation>() {
        public ParkingLocation createFromParcel(Parcel pc) {
            return new ParkingLocation(pc);
        }
        public ParkingLocation[] newArray(int size) {
            return new ParkingLocation[size];
        }
    };

    /**Ctor from Parcel, reads back fields IN THE ORDER they were written */
    public ParkingLocation(Parcel pc){
        id         = pc.readLong();
        date = new DateTime(pc.readLong());
        latitude        =  pc.readDouble();
        longitude      = pc.readDouble();
    }

    public ParkingLocation(){}
}
