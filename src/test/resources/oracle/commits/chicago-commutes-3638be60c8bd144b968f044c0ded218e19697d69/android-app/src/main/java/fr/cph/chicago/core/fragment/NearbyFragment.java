/**
 * Copyright 2017 Carl-Philipp Harmant
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.cph.chicago.core.fragment;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindString;
import butterknife.BindView;
import fr.cph.chicago.R;
import fr.cph.chicago.core.App;
import fr.cph.chicago.core.adapter.SlidingUpAdapter;
import fr.cph.chicago.core.listener.OnMarkerClickListener;
import fr.cph.chicago.data.BusData;
import fr.cph.chicago.data.DataHolder;
import fr.cph.chicago.data.MarkerDataHolder;
import fr.cph.chicago.data.TrainData;
import fr.cph.chicago.entity.BikeStation;
import fr.cph.chicago.entity.BusStop;
import fr.cph.chicago.entity.Position;
import fr.cph.chicago.entity.Station;
import fr.cph.chicago.util.GPSUtil;
import fr.cph.chicago.util.Util;
import io.realm.Realm;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static fr.cph.chicago.Constants.GPS_ACCESS;

/**
 * Nearby Fragment
 *
 * @author Carl-Philipp Harmant
 * @version 1
 */
@SuppressWarnings("WeakerAccess")
public class NearbyFragment extends AbstractFragment implements EasyPermissions.PermissionCallbacks {

    @BindView(R.id.activity_bar)
    ProgressBar progressBar;
    @BindView(R.id.sliding_layout)
    SlidingUpPanelLayout slidingUpPanelLayout;
    @BindView(R.id.loading_layout_container)
    LinearLayout layoutContainer;

    @BindString(R.string.bundle_bike_stations)
    String bundleBikeStations;

    private SupportMapFragment mapFragment;

    private GoogleApiClient googleApiClient;
    private SlidingUpAdapter slidingUpAdapter;
    private MarkerDataHolder markerDataHolder;

    public SlidingUpPanelLayout getSlidingUpPanelLayout() {
        return slidingUpPanelLayout;
    }

    public LinearLayout getLayoutContainer() {
        return layoutContainer;
    }

    public SlidingUpAdapter getSlidingUpAdapter() {
        return slidingUpAdapter;
    }

    @NonNull
    public static NearbyFragment newInstance(final int sectionNumber) {
        return (NearbyFragment) fragmentWithBundle(new NearbyFragment(), sectionNumber);
    }

    @Override
    public final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.checkTrainData(activity);
        App.checkBusData(activity);
        Util.trackScreen(getContext(), getString(R.string.analytics_nearby_fragment));
    }

    @Override
    public final View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_nearby, container, false);
        if (!activity.isFinishing()) {
            setBinder(rootView);
            slidingUpAdapter = new SlidingUpAdapter(this);
            markerDataHolder = new MarkerDataHolder();
            showProgress(true);
        }
        return rootView;
    }

    @Override
    public final void onStart() {
        super.onStart();
        googleApiClient = new GoogleApiClient.Builder(activity)
            .addApi(LocationServices.API)
            .build();
        final GoogleMapOptions options = new GoogleMapOptions();
        final CameraPosition camera = new CameraPosition(Util.CHICAGO, 7, 0, 0);
        options.camera(camera);
        mapFragment = SupportMapFragment.newInstance(options);
        mapFragment.setRetainInstance(true);
        final FragmentManager fm = activity.getSupportFragmentManager();
        loadNearbyIfAllowed();
        fm.beginTransaction().replace(R.id.map, mapFragment).commit();
    }

    @Override
    public final void onResume() {
        super.onResume();
        slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
    }

    public void updateMarkersAndModel(
        @NonNull final List<BusStop> busStops,
        @NonNull final List<Station> trainStation,
        @NonNull final List<BikeStation> bikeStations) {
        if (isAdded()) {
            mapFragment.getMapAsync(googleMap -> {
                googleMap.getUiSettings().setMyLocationButtonEnabled(true);
                googleMap.getUiSettings().setZoomControlsEnabled(false);
                googleMap.getUiSettings().setMapToolbarEnabled(false);

                markerDataHolder.clear();

                final BitmapDescriptor bitmapDescriptorBus = createStop(getContext(), R.drawable.bus_stop_icon);
                final BitmapDescriptor bitmapDescriptorTrain = createStop(getContext(), R.drawable.train_station_icon);
                final BitmapDescriptor bitmapDescriptorBike = createStop(getContext(), R.drawable.bike_station_icon);

                Stream.of(busStops)
                    .forEach(busStop -> {
                        final LatLng point = new LatLng(busStop.getPosition().getLatitude(), busStop.getPosition().getLongitude());
                        final MarkerOptions markerOptions = new MarkerOptions()
                            .position(point)
                            .title(busStop.getName())
                            .icon(bitmapDescriptorBus);
                        final Marker marker = googleMap.addMarker(markerOptions);
                        markerDataHolder.addData(marker, busStop);
                    });

                Stream.of(trainStation)
                    .forEach(station ->
                        Stream.of(station.getStopsPosition())
                            .findFirst()
                            .ifPresent(position -> {
                                final MarkerOptions markerOptions = new MarkerOptions()
                                    .position(new LatLng(position.getLatitude(), position.getLongitude()))
                                    .title(station.getName())
                                    .icon(bitmapDescriptorTrain);
                                final Marker marker = googleMap.addMarker(markerOptions);
                                markerDataHolder.addData(marker, station);
                            })
                    );

                Stream.of(bikeStations)
                    .forEach(station -> {
                        final MarkerOptions markerOptions = new MarkerOptions()
                            .position(new LatLng(station.getLatitude(), station.getLongitude()))
                            .title(station.getName())
                            .icon(bitmapDescriptorBike);
                        final Marker marker = googleMap.addMarker(markerOptions);
                        markerDataHolder.addData(marker, station);
                    });

                showProgress(false);
                googleMap.setOnMarkerClickListener(new OnMarkerClickListener(markerDataHolder, NearbyFragment.this));
            });
        }
    }

    private BitmapDescriptor createStop(@Nullable final Context context, @DrawableRes final int icon) {
        if (context != null) {
            final int px = context.getResources().getDimensionPixelSize(R.dimen.icon_shadow_2);
            final Bitmap bitMapBusStation = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
            final Canvas canvas = new Canvas(bitMapBusStation);
            final Drawable shape = ContextCompat.getDrawable(context, icon);
            shape.setBounds(0, 0, px, bitMapBusStation.getHeight());
            shape.draw(canvas);
            return BitmapDescriptorFactory.fromBitmap(bitMapBusStation);
        } else {
            return BitmapDescriptorFactory.defaultMarker();
        }
    }

    public void showProgress(final boolean show) {
        if (isAdded()) {
            if (show) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(50);
            } else {
                progressBar.setVisibility(View.GONE);
            }
        }
    }

    private class LoadNearbyTask extends AsyncTask<Void, Void, Position> {

        private List<BusStop> busStops;
        private List<Station> trainStations;
        private List<BikeStation> bikeStations;

        private LoadNearbyTask() {
            busStops = new ArrayList<>();
            trainStations = new ArrayList<>();
        }

        @Override
        protected final Position doInBackground(final Void... params) {
            bikeStations = activity.getIntent().getExtras().getParcelableArrayList(bundleBikeStations);

            if (!googleApiClient.isConnected()) {
                googleApiClient.blockingConnect();
            }

            final BusData busData = DataHolder.INSTANCE.getBusData();
            final TrainData trainData = DataHolder.INSTANCE.getTrainData();

            final GPSUtil gpsUtil = new GPSUtil(googleApiClient);
            final Position position = gpsUtil.getLocation();
            if (position != null) {
                final Realm realm = Realm.getDefaultInstance();
                busStops = busData.readNearbyStops(realm, position);
                realm.close();
                trainStations = trainData.readNearbyStation(position);
                // FIXME: wait for bike stations to be loaded
                bikeStations = bikeStations != null
                    ? BikeStation.Companion.readNearbyStation(bikeStations, position)
                    : new ArrayList<>();
            }
            return position;
        }

        @Override
        protected final void onPostExecute(final Position result) {
            Util.centerMap(mapFragment, result);
            updateMarkersAndModel(busStops, trainStations, bikeStations);
        }
    }

    @AfterPermissionGranted(GPS_ACCESS)
    private void loadNearbyIfAllowed() {
        if (EasyPermissions.hasPermissions(getContext(), ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)) {
            startLoadingNearby();
        } else {
            EasyPermissions.requestPermissions(this, "To access that feature, we need to access your current location", GPS_ACCESS, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        startLoadingNearby();
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        showProgress(false);
    }

    private void startLoadingNearby() {
        if (Util.isNetworkAvailable(getContext())) {
            showProgress(true);
            new LoadNearbyTask().execute();
        } else {
            Util.showNetworkErrorMessage(activity);
            showProgress(false);
        }
    }
}
