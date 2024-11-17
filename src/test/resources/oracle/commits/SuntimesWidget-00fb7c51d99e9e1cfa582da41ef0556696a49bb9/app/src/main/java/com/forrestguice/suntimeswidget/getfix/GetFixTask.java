/**
    Copyright (C) 2014 Forrest Guice
    This file is part of SuntimesWidget.

    SuntimesWidget is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    SuntimesWidget is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with SuntimesWidget.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.forrestguice.suntimeswidget.getfix;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * An AsyncTask that registers a LocationListener, starts listening for
 * gps updates, and then waits a predetermined amount of time for a
 * good location fix to be acquired; updates progress.
 */
public class GetFixTask extends AsyncTask<String, Location, Location>
{
    public static final int MIN_ELAPSED = 1000 * 5;        // wait at least 5s before settling on a fix
    public static final int MAX_ELAPSED = 1000 * 60;       // wait at most a minute for a fix
    public static final int MAX_AGE = 1000 * 60 * 5;       // consider fixes over 5min be "too old"

    private WeakReference<GetFixHelper> helperRef;
    public GetFixTask(Context parent, GetFixHelper helper)
    {
        locationManager = (LocationManager)parent.getSystemService(Context.LOCATION_SERVICE);
        this.helperRef = new WeakReference<GetFixHelper>(helper);
    }

    /**
     * Property: minimum amount of time that must elapse while searching for a location.
     */
    private int minElapsed = MIN_ELAPSED;
    public int getMinElapsed()
    {
        return minElapsed;
    }
    public void setMinElapsed( int timeInMs )
    {
        minElapsed = timeInMs;
    }

    /**
     * Property: maximum amount of time that may elapsed while searching for a location.
     */
    private int maxElapsed = MAX_ELAPSED;
    public int getMaxElapsed()
    {
        return maxElapsed;
    }
    public void setMaxElapsed( int timeInMs )
    {
        maxElapsed = timeInMs;
    }

    /**
     * Property: maximum amount of time a fix may age before its considered out-of-date.
     */
    private int maxAge = MAX_AGE;
    public int getMaxAge()
    {
        return maxAge;
    }
    public void setMaxAge( int timeInMs )
    {
        maxAge = timeInMs;
    }

    private long startTime, stopTime, elapsedTime;
    private Location bestFix, lastFix;
    private LocationManager locationManager;
    private LocationListener locationListener = new LocationListener()
    {
        @Override
        public void onLocationChanged(Location location)
        {
            Log.d("GetFixTask", "onLocationChanged: " + location.toString());

            lastFix = location;
            if (isBetterFix(lastFix, bestFix))
            {
                bestFix = lastFix;
                onProgressUpdate(bestFix);
            }
        }

        private boolean isBetterFix(Location location, Location location2)
        {
            if (location2 == null)
            {
                return true;

            } else if (location != null) {
                if ((location.getTime() - location2.getTime()) > maxAge)
                {
                    return true;  // more than maxAge since last fix; assume the latest fix is better

                } else if (location.getAccuracy() < location2.getAccuracy()) {
                    return true;  // accuracy is a measure of radius of certainty; smaller values are more accurate
                }
            }
            return false;
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) { }

        @Override
        public void onProviderEnabled(String provider) { }

        @Override
        public void onProviderDisabled(String provider) { }
    };

    @Override
    protected void onPreExecute()
    {
        final GetFixHelper helper = helperRef.get();
        if (helper != null)
        {
            GetFixUI uiObj = helper.getUI();
            uiObj.onStart();
            uiObj.showProgress(true);
            uiObj.enableUI(false);
        }

        signalStarted();
        if (helper != null)
        {
            helper.gettingFix = true;
        }
        bestFix = null;
        elapsedTime = 0;
        startTime = stopTime = System.currentTimeMillis();
    }

    @Override
    protected Location doInBackground(String... params)
    {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable()
        {
            public void run()
            {
                try {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                    Log.d("GetFixTask", "started location listener; now requesting updates . . .");
                } catch (SecurityException e) {
                    Log.e("GetFixTask", "unable to start locationListener ... Permissions! we don't have them... checkPermissions should be called before using this task! " + e);
                }
            }
        });

        while (elapsedTime < maxElapsed && !isCancelled())
        {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // e.printStackTrace();   // silent
            }

            stopTime = System.currentTimeMillis();
            elapsedTime = stopTime - startTime;

            if (bestFix != null && elapsedTime > minElapsed)
            {
                break;
            }
        }
        return bestFix;
    }

    @Override
    protected void onProgressUpdate(Location... locations)
    {
        final GetFixHelper helper = helperRef.get();
        if (helper != null)
        {
            GetFixUI uiObj = helper.getUI();
            uiObj.updateUI(locations);
        }
    }

    @Override
    protected void onPostExecute(Location result)
    {
        try {
            locationManager.removeUpdates(locationListener);
            Log.d("GetFixTask", "stopped location listener");
        } catch (SecurityException e) {
            Log.e("GetFixTask", "unable to stop locationListener ... Permissions! we don't have them... checkPermissions should be called before using this task! " + e);
        }

        final GetFixHelper helper = helperRef.get();
        if (helper != null)
        {
            helper.gettingFix = false;

            GetFixUI uiObj = helper.getUI();
            uiObj.showProgress(false);
            uiObj.enableUI(true);
            uiObj.onResult(result, false);
        }
        signalFinished(result);
    }

    @Override
    protected void onCancelled(Location result)
    {
        try {
            locationManager.removeUpdates(locationListener);
            Log.d("GetFixTask", "stopped location listener");
        } catch (SecurityException e) {
            Log.e("GetFixTask", "unable to stop locationListener ... Permissions! we don't have them... checkPermissions should be called before using this task! " + e);
        }

        GetFixHelper helper = helperRef.get();
        if (helper != null)
        {
            helper.gettingFix = false;

            GetFixUI uiObj = helper.getUI();
            uiObj.showProgress(false);
            uiObj.enableUI(true);
            uiObj.onResult(result, true);
        }
        signalCancelled();
    }

    private ArrayList<GetFixTaskListener> listeners = new ArrayList<GetFixTaskListener>();
    public void addGetFixTaskListener( GetFixTaskListener listener )
    {
        if (!listeners.contains(listener))
        {
            listeners.add(listener);
        }
    }
    public void removeGetFixTaskListener( GetFixTaskListener listener )
    {
        listeners.remove(listener);
    }
    public void addGetFixTaskListeners( List<GetFixTaskListener> listeners )
    {
        for (GetFixTask.GetFixTaskListener listener : listeners)
        {
            addGetFixTaskListener(listener);
        }
    }

    protected void signalStarted()
    {
        for (GetFixTaskListener listener : listeners)
        {
            if (listener != null)
                listener.onStarted();
        }
    }
    protected void signalFinished(Location result)
    {
        for (GetFixTaskListener listener : listeners)
        {
            if (listener != null)
                listener.onFinished(result);
        }
    }
    protected void signalCancelled()
    {
        for (GetFixTaskListener listener : listeners)
        {
            if (listener != null)
                listener.onCancelled();
        }
    }

    public static abstract class GetFixTaskListener
    {
        public void onStarted() {}
        public void onFinished(Location result) {}
        public void onCancelled() {}
    }
}
