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

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.forrestguice.suntimeswidget.R;
import com.forrestguice.suntimeswidget.settings.WidgetSettings;

import java.util.ArrayList;

/**
 * A helper class that helps to manage a GetFixTask; has methods for starting/stopping the task;
 * allows a single task to run at a time.
 */
public class GetFixHelper
{
    public static final String KEY_LOCATION_GETTINGFIX = "gettingfix";
    public static final String KEY_LOCATION_GOTFIX = "gotfix";
    public static final String KEY_LOCATION_UIINDEX = "uiindex";

    public static final String DIALOGTAG_ENABLEGPS = "enablegps";
    public static final String DIALOGTAG_KEEPTRYING = "keeptrying";

    public static final int REQUEST_GETFIX_LOCATION = 1;

    public static final String PREF_KEY_GETFIX_MINELAPSED = "getFix_minElapsed";
    public static final String PREF_KEY_GETFIX_MAXELAPSED = "getFix_maxElapsed";
    public static final String PREF_KEY_GETFIX_MAXAGE = "getFix_maxAge";

    public GetFixTask getFixTask = null;
    public Location fix = null;

    public boolean gettingFix = false;
    public boolean wasGettingFix = false;
    public boolean gotFix = false;

    private FragmentActivity myParent;
    private ArrayList<GetFixUI> uiObj = new ArrayList<GetFixUI>();
    private int uiIndex = 0;

    public GetFixHelper(FragmentActivity parent, GetFixUI ui)
    {
        myParent = parent;
        addUI(ui);
    }

    /**
     * Get a fix; main entry point for GPS "get fix" button in location settings.
     * Spins up a GetFixTask; allows only one such task to execute at a time.
     */
    public void getFix()
    {
        if (!gettingFix)
        {
            if (hasGPSPermissions(myParent, REQUEST_GETFIX_LOCATION))
            {
                if (isGPSEnabled())
                {
                    SharedPreferences prefs = myParent.getSharedPreferences(WidgetSettings.PREFS_WIDGET, 0);
                    getFixTask = new GetFixTask(myParent, this);

                    int minElapsed = prefs.getInt(PREF_KEY_GETFIX_MINELAPSED, GetFixTask.MIN_ELAPSED);
                    getFixTask.setMinElapsed(minElapsed);

                    int maxElapsed = prefs.getInt(PREF_KEY_GETFIX_MAXELAPSED, GetFixTask.MAX_ELAPSED);
                    getFixTask.setMaxElapsed(maxElapsed);

                    int maxAge = prefs.getInt(PREF_KEY_GETFIX_MAXAGE, GetFixTask.MAX_AGE);
                    getFixTask.setMaxAge(maxAge);

                    getFixTask.addGetFixTaskListeners(listeners);
                    getFixTask.addGetFixTaskListener( new GetFixTask.GetFixTaskListener()
                    {
                        @Override
                        public void onFinished(Location result)
                        {
                            fix = result;
                            gotFix = (fix != null);

                            if (!getFixTask.isCancelled() && !gotFix)
                            {
                                showKeepSearchingPrompt();
                            }
                        }
                    });
                    getFixTask.execute();

                } else {
                    Log.w("GetFixHelper", "getFix called while GPS disabled; showing a prompt");
                    showGPSEnabledPrompt();
                }
            } else {
                Log.w("GetFixHelper", "getFix called without GPS permissions! ignored");
            }
        } else {
            Log.w("GetFixHelper", "getFix called while already running! ignored");
        }
    }
    public void getFix( int i )
    {
        if (!gettingFix)
        {
            setUiIndex( i );
            getFix();

        } else {
            Log.w("GetFixHelper", "getFix called while already running! ignored");
        }
    }

    /**
     * Cancel acquiring a location fix (cancels running task(s)).
     */
    public void cancelGetFix()
    {
        if (gettingFix && getFixTask != null)
        {
            Log.d("GetFixHelper", "Canceling getFix");
            getFixTask.cancel(true);
        }
    }

    public boolean hasGPSPermissions(FragmentActivity activity, int requestID)
    {
        int permission = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION);
        boolean hasPermission = (permission == PackageManager.PERMISSION_GRANTED);
        Log.d("hasGPSPermissions", "" + hasPermission);

        if (!hasPermission)
        {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, requestID);
        }

        return hasPermission;
    }

    public boolean isGettingFix()
    {
        return gettingFix;
    }

    public GetFixUI getUI()
    {
        return uiObj.get(uiIndex);
    }

    /**
     * @param i the UI index (default 0)
     * @return a GetFixUI instance
     */
    public GetFixUI getUI( int i )
    {
        if (i >= 0 && i < uiObj.size())
            return uiObj.get(i);
        else return uiObj.get(0);
    }

    /**
     * @param ui the ui obj to be added to and managed by the helper
     */
    public void addUI( GetFixUI ui )
    {
        uiObj.add(ui);
    }

    /**
     * @return the number of ui objects managed by this helper
     */
    public int numUI()
    {
        return uiObj.size();
    }

    /**
     * @return the index of the ui object currently that is currently set to be used
     */
    public int getUiIndex()
    {
        return uiIndex;
    }

    /**
     * Set the index of the ui object to use when getting a fix.
     * @param i the ui obj index
     * @return true the index was set, false failed to set
     */
    public boolean setUiIndex(int i)
    {
        if (uiIndex >= 0 && uiIndex < uiObj.size())
        {
            uiIndex = i;
            return true;

        } else {
            Log.w("GetFixHelper", "setUiIndex was called with a negative value! " + i);
            return false;
        }
    }

    private ArrayList<GetFixTask.GetFixTaskListener> listeners = new ArrayList<GetFixTask.GetFixTaskListener>();
    public void addGetFixTaskListener( GetFixTask.GetFixTaskListener listener )
    {
        if (!listeners.contains(listener))
        {
            listeners.add(listener);
            if (getFixTask != null)
            {
                getFixTask.addGetFixTaskListener(listener);
            }
        }
    }
    public void removeGetFixTaskListener( GetFixTask.GetFixTaskListener listener )
    {
        listeners.remove(listener);
        if (getFixTask != null)
        {
            getFixTask.removeGetFixTaskListener(listener);
        }
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        switch (requestCode)
        {
            case REQUEST_GETFIX_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    getFix();
                }
                break;
        }
    }

    public void loadSettings( Bundle bundle )
    {
        if (bundle == null)
            return;

        Log.d("DEBUG", "GetFixHelper loadSettings (bundle)");
        wasGettingFix = bundle.getBoolean(KEY_LOCATION_GETTINGFIX);
        gotFix = bundle.getBoolean(KEY_LOCATION_GOTFIX);
        setUiIndex(bundle.getInt(KEY_LOCATION_UIINDEX));
    }

    public void saveSettings( Bundle bundle )
    {
        Log.d("DEBUG", "GetFixHelper saveSettings (bundle)");
        bundle.putBoolean(KEY_LOCATION_GETTINGFIX, gettingFix);
        bundle.putBoolean(KEY_LOCATION_GOTFIX, gotFix);
        bundle.putInt(KEY_LOCATION_UIINDEX, uiIndex);
    }

    public void onResume()
    {
        Log.d("DEBUG", "GetFixHelper onResume");
        FragmentManager fragments = myParent.getSupportFragmentManager();

        KeepTryingDialog keepTryingDialog = (KeepTryingDialog) fragments.findFragmentByTag(DIALOGTAG_KEEPTRYING);
        if (keepTryingDialog != null)
        {
            keepTryingDialog.setHelper(this);
        }

        EnableGPSDialog enableGPSDialog = (EnableGPSDialog) fragments.findFragmentByTag(DIALOGTAG_ENABLEGPS);
        if (enableGPSDialog != null)
        {
            enableGPSDialog.setHelper(this);
        }

        if (wasGettingFix)
        {
            Log.d("DEBUG", "GetFixHelper was previously getting fix... restarting");
            getFix();
        }
    }

    /**
     * Keep trying dialog fragment; "No fix found. Keep searching? yes, no"
     */
    public static class KeepTryingDialog extends DialogFragment
    {
        private GetFixHelper helper;
        public GetFixHelper getHelper() { return helper; }
        public void setHelper( GetFixHelper helper ) { this.helper = helper; }

        @NonNull @Override
        public Dialog onCreateDialog(Bundle savedInstanceState)
        {
            final Activity context = getActivity();
            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(context.getString(R.string.gps_keeptrying_msg))
                    .setCancelable(false)
                    .setPositiveButton(context.getString(R.string.gps_keeptrying_ok), new DialogInterface.OnClickListener()
                    {
                        public void onClick(final DialogInterface dialog, final int id)
                        {
                            helper.getFix();
                        }
                    })
                    .setNegativeButton(context.getString(R.string.gps_keeptrying_cancel), new DialogInterface.OnClickListener()
                    {
                        public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id)
                        {
                            dialog.cancel();
                        }
                    });
            return builder.create();
        }
    }

    public void showKeepSearchingPrompt()
    {
        final KeepTryingDialog dialog = new KeepTryingDialog();
        dialog.setHelper(this);
        dialog.show(myParent.getSupportFragmentManager(), DIALOGTAG_KEEPTRYING);
    }

    /**
     * Enable GPS alert dialog fragment; "Enable GPS? yes, no"
     */
    public static class EnableGPSDialog extends DialogFragment
    {
        public EnableGPSDialog() {}

        private GetFixHelper helper;
        public void setHelper(GetFixHelper helper)
        {
            this.helper = helper;
        }

        @NonNull @Override
        public Dialog onCreateDialog(Bundle savedInstanceState)
        {
            final Activity context = getActivity();
            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(context.getString(R.string.gps_dialog_msg))
                    .setCancelable(false)
                    .setPositiveButton(context.getString(R.string.gps_dialog_ok), new DialogInterface.OnClickListener()
                    {
                        public void onClick(final DialogInterface dialog, final int id)
                        {
                            context.startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    })
                    .setNegativeButton(context.getString(R.string.gps_dialog_cancel), new DialogInterface.OnClickListener()
                    {
                        public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id)
                        {
                            dialog.cancel();
                        }
                    });
            return builder.create();
        }
    }

    public boolean isGPSEnabled()
    {
        return isGPSEnabled(myParent);
    }
    public static boolean isGPSEnabled(Context context)
    {
        LocationManager locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public void showGPSEnabledPrompt()
    {
        final EnableGPSDialog dialog = new EnableGPSDialog();
        dialog.setHelper(this);
        dialog.show(myParent.getSupportFragmentManager(), DIALOGTAG_ENABLEGPS);
    }

}
