package bobbarteam.andrognole;

import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

/**
 * Created by jeremy on 11/03/15.
 */
public class ButtonFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_button, container, false);

        if (((MainActivity) getActivity()).isOnline()){
            ((Switch) view.findViewById(R.id.switch_mode)).setChecked(false);
        } else {
            ((Switch) view.findViewById(R.id.switch_mode)).setChecked(true);
        }

        return view;
    }
}
