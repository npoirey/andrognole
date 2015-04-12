package bobbarteam.andrognole;

import android.app.Fragment;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import bobbarteam.andrognole.engine.location.ParkingLocation;
import bobbarteam.andrognole.engine.location.ParkingLocationDataSource;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class ParkingInfoFragment extends Fragment {
    private static final String TAG = ParkingInfoFragment.class.getSimpleName();
    private DateTime date;
    private View view;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_parking_info, container, false);
        this.view = view;

        //find the last parking date, to display it
        ParkingLocationDataSource parkingLocationDataSource = new ParkingLocationDataSource(getActivity().getApplicationContext());
        parkingLocationDataSource.open();
        ParkingLocation lastParkingLocation = parkingLocationDataSource.getLastParkingLocation();
        if(lastParkingLocation != null) {
            Log.d(TAG, "parking position : " + lastParkingLocation);
            this.date = lastParkingLocation.getDate();
        } else {
            this.reset();
        }
        parkingLocationDataSource.close();
        return view;
    }

    @Override
    public void onResume() {
        Log.i(TAG, "resume");
        super.onResume();

        setDate(date);
    }


    public void setDate(DateTime date) {
        this.date = date;
        TextView printDate = (TextView) this.view.findViewById(R.id.print_date);
        if(date == null){
            printDate.setText(getString(R.string.parking_info_no_parking));
        }
        else{
            DateTimeFormatter formatterHours =
                    DateTimeFormat.forPattern(getString(R.string.parking_info_date_hour_format));
            int ageInDays = Days.daysBetween(date.withTimeAtStartOfDay(),
                    DateTime.now().withTimeAtStartOfDay()).getDays();

            Resources res = getResources();

            String hoursString = formatterHours.print(date);

            String dayString;
            if(ageInDays == 0){
                dayString = res.getString(R.string.parking_info_day_today);
            } else{
                dayString = String.format(res.getQuantityString(R.plurals.parking_info_day, ageInDays),
                        ageInDays);
            }
            printDate.setText( dayString + res.getString(R.string.parking_info_date_separator) + hoursString);
        }
    }

    public void reset() {
        this.date = null;
    }
}
