package fr.cph.chicago.util

import android.location.Location
import android.os.Looper
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import fr.cph.chicago.entity.Position

class GPSUtil(private val googleApiClient: GoogleApiClient) {
    private val locationRequest: LocationRequest = LocationRequest()

    init {
        locationRequest.interval = MIN_TIME_BW_UPDATES
        locationRequest.fastestInterval = FASTEST_INTERVAL
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    // Getting GPS status
    val location: Position?
        @Throws(SecurityException::class)
        get() {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, {}, Looper.getMainLooper())
            val location: Location? = LocationServices.FusedLocationApi.getLastLocation(googleApiClient)
            return if (location == null) null else Position(location.latitude, location.longitude)
        }

    companion object {
        // The fastest refresh interval
        private val FASTEST_INTERVAL: Long = 1000
        // The minimum time between updates in milliseconds
        private val MIN_TIME_BW_UPDATES = (1000 * 10).toLong() // 10 sec
    }
}
