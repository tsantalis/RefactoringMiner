package fr.cph.chicago.util;

import android.location.Location;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.annimon.stream.Optional;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import fr.cph.chicago.entity.Position;

public class GPSUtil {
    // The fastest refresh interval
    private static final long FASTEST_INTERVAL = 1000;
    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000 * 10; // 10 sec

    private final GoogleApiClient googleApiClient;
    private final LocationRequest locationRequest;

    public GPSUtil(@NonNull final GoogleApiClient googleApiClient) {
        this.googleApiClient = googleApiClient;
        locationRequest = new LocationRequest();
        locationRequest.setInterval(MIN_TIME_BW_UPDATES);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @NonNull
    public Optional<Position> getLocation() throws SecurityException {
        // Getting GPS status
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, location -> {
        }, Looper.getMainLooper());
        final Location location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        Position position = null;
        if (location != null) {
            position = new Position();
            position.setLatitude(location.getLatitude());
            position.setLongitude(location.getLongitude());
        }
        return Optional.ofNullable(position);
    }
}
