package bobbarteam.andrognole.engine.location;

import android.location.Location;

public abstract interface LocationCallback {
    public void handleNewLocation(Location location);
}
