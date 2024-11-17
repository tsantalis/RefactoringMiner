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
package com.forrestguice.suntimeswidget;

import android.appwidget.AppWidgetManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.forrestguice.suntimeswidget.getfix.GetFixDatabaseAdapter;
import com.forrestguice.suntimeswidget.getfix.GetFixHelper;
import com.forrestguice.suntimeswidget.getfix.GetFixTask;
import com.forrestguice.suntimeswidget.getfix.GetFixUI;
import com.forrestguice.suntimeswidget.getfix.GetFixUI1;
import com.forrestguice.suntimeswidget.getfix.GetFixUI2;
import com.forrestguice.suntimeswidget.getfix.LocationListTask;
import com.forrestguice.suntimeswidget.settings.WidgetSettings;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Pattern;

public class LocationConfigView extends LinearLayout
{
    public static final String KEY_DIALOGMODE = "dialogmode";
    public static final String KEY_LOCATION_MODE = "locationMode";
    public static final String KEY_LOCATION_LATITUDE = "locationLatitude";
    public static final String KEY_LOCATION_LONGITUDE = "locationLongitude";
    public static final String KEY_LOCATION_LABEL = "locationLabel";

    private FragmentActivity myParent;
    private boolean isInitialized = false;

    public LocationConfigView(Context context)
    {
        super(context);
    }

    public LocationConfigView(Context context, AttributeSet attribs)
    {
        super(context, attribs);
    }

    public void init(FragmentActivity context, boolean asDialog)
    {
        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate((asDialog ? R.layout.layout_dialog_location2 : R.layout.layout_settings_location2), this);
        myParent = context;
        initViews(context);

        loadSettings(context);
        setMode(mode);
        populateLocationList();
        isInitialized = true;
    }

    public void init(FragmentActivity context, boolean asDialog, int appWidgetId)
    {
        this.appWidgetId = appWidgetId;
        init(context, asDialog);
    }

    public boolean isInitialized() { return isInitialized; }

    public WidgetSettings.Location getLocation()
    {
        String name = text_locationName.getText().toString();
        String latitude = text_locationLat.getText().toString();
        String longitude = text_locationLon.getText().toString();

        try {
            @SuppressWarnings("UnusedAssignment")
            BigDecimal lat = new BigDecimal(latitude);

            @SuppressWarnings("UnusedAssignment")
            BigDecimal lon = new BigDecimal(longitude);

        } catch (NumberFormatException e) {
            Log.e("getLocation", "invalid location! falling back to default; " + e.toString());
            name = WidgetSettings.PREF_DEF_LOCATION_LABEL;
            latitude = WidgetSettings.PREF_DEF_LOCATION_LATITUDE;
            longitude = WidgetSettings.PREF_DEF_LOCATION_LONGITUDE;
        }

        return new WidgetSettings.Location(name, latitude, longitude);
    }

    public WidgetSettings.LocationMode getLocationMode()
    {
        final WidgetSettings.LocationMode[] locationModes = WidgetSettings.LocationMode.values();
        WidgetSettings.LocationMode locationMode = locationModes[ spinner_locationMode.getSelectedItemPosition() ];
        return locationMode;
    }

    /**
     * Property: appwidget id
     */
    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    public int getAppWidgetId()
    {
        return appWidgetId;
    }
    public void setAppWidgetId(int value)
    {
        appWidgetId = value;
        loadSettings(myParent);
    }

    /**
     * Property: hide title
     */
    private boolean hideTitle = false;
    public boolean getHideTitle() { return hideTitle; }
    public void setHideTitle(boolean value)
    {
        hideTitle = value;
        TextView groupTitle = (TextView)findViewById(R.id.appwidget_location_grouptitle);
        groupTitle.setVisibility( (hideTitle ? View.GONE : View.VISIBLE) );
    }

    /**
     * Property: auto mode allowed
     */
    private boolean autoAllowed = true;
    public boolean getAutoAllowed() { return autoAllowed; }
    public void setAutoAllowed(boolean value)
    {
        autoAllowed = value;
    }

    /** Property: mode (auto, select, edit/add) */
    private LocationViewMode mode = LocationViewMode.MODE_CUSTOM_SELECT;
    public LocationViewMode getMode()
    {
        return mode;
    }
    public void setMode( LocationViewMode mode )
    {
        Log.d("DEBUG", "LocationViewMode setMode " + mode.name());
        FrameLayout autoButtonLayout = (FrameLayout)findViewById(R.id.appwidget_location_auto_layout);

        if (this.mode != mode)
        {
            getFixHelper.cancelGetFix();
        }

        this.mode = mode;
        switch (mode)
        {
            case MODE_AUTO:
                labl_locationLon.setEnabled(false);
                text_locationLon.setEnabled(false);
                labl_locationLat.setEnabled(false);
                text_locationLat.setEnabled(false);
                inputOverlay.setVisibility(View.VISIBLE);

                labl_locationName.setEnabled(false);
                text_locationName.setEnabled(false);

                spin_locationName.setSelection(GetFixDatabaseAdapter.findPlaceByName(myParent.getString(R.string.gps_lastfix_title_found), getFixAdapter.getCursor()));
                spin_locationName.setEnabled(false);
                flipper.setDisplayedChild(1);

                autoButtonLayout.setVisibility(View.VISIBLE);
                button_edit.setVisibility(View.GONE);
                button_save.setVisibility(View.GONE);
                flipper2.setDisplayedChild(1);
                break;

            case MODE_CUSTOM_ADD:
            case MODE_CUSTOM_EDIT:
                labl_locationLon.setEnabled(true);
                text_locationLon.setEnabled(true);
                labl_locationLat.setEnabled(true);
                text_locationLat.setEnabled(true);
                inputOverlay.setVisibility(View.GONE);

                labl_locationName.setEnabled(true);
                text_locationName.setEnabled(true);
                spin_locationName.setEnabled(false);
                flipper.setDisplayedChild(0);
                text_locationName.requestFocus();

                autoButtonLayout.setVisibility(View.GONE);
                button_edit.setVisibility(View.GONE);
                button_save.setVisibility(View.VISIBLE);
                flipper2.setDisplayedChild(0);
                break;

            case MODE_CUSTOM_SELECT:
            default:
                labl_locationLon.setEnabled(false);
                text_locationLon.setEnabled(false);
                labl_locationLat.setEnabled(false);
                text_locationLat.setEnabled(false);
                inputOverlay.setVisibility(View.VISIBLE);

                labl_locationName.setEnabled(true);
                text_locationName.setEnabled(false);
                spin_locationName.setEnabled(true);
                flipper.setDisplayedChild(1);

                autoButtonLayout.setVisibility(View.GONE);
                button_edit.setVisibility(View.VISIBLE);
                button_save.setVisibility(View.GONE);
                flipper2.setDisplayedChild(1);
                break;
        }
    }

    private ViewFlipper flipper, flipper2;
    private Spinner spinner_locationMode;

    private TextView labl_locationLat;
    private EditText text_locationLat;

    private TextView labl_locationLon;
    private EditText text_locationLon;

    private LinearLayout layout_locationName;
    private TextView labl_locationName;
    private Spinner spin_locationName;
    private EditText text_locationName;
    private View inputOverlay;

    private ImageButton button_edit;
    private ImageButton button_save;

    private ImageButton button_getfix;
    private ProgressBar progress_getfix;
    private GetFixUI getFixUI_editMode;

    private ImageButton button_auto;
    private ProgressBar progress_auto;
    private GetFixUI getFixUI_autoMode;

    private GetFixHelper getFixHelper;
    private SimpleCursorAdapter getFixAdapter;

    /**
     *
     * @param context
     */
    protected void initViews( Context context )
    {
        Log.d("DEBUG", "LocationConfigView initViews");
        WidgetSettings.initDisplayStrings(context);

        flipper = (ViewFlipper)findViewById(R.id.view_flip);
        flipper.setInAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in));
        flipper.setOutAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_out));

        flipper2 = (ViewFlipper)findViewById(R.id.view_flip2);
        flipper2.setInAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in));
        flipper2.setOutAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_out));

        ArrayAdapter<WidgetSettings.LocationMode> spinner_locationModeAdapter = new LocationModeAdapter(myParent, WidgetSettings.LocationMode.values());
        spinner_locationModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner_locationMode = (Spinner)findViewById(R.id.appwidget_location_mode);
        spinner_locationMode.setAdapter(spinner_locationModeAdapter);
        spinner_locationMode.setOnItemSelectedListener(onLocationModeSelected);

        layout_locationName = (LinearLayout) findViewById(R.id.appwidget_location_name_layout);
        labl_locationName = (TextView) findViewById(R.id.appwidget_location_name_label);
        text_locationName = (EditText) findViewById(R.id.appwidget_location_name);

        String[] from = new String[] {"name"};
        int[] to = new int[] {android.R.id.text1};
        getFixAdapter = new SimpleCursorAdapter(myParent, R.layout.layout_listitem_locations, null, from, to);
        getFixAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spin_locationName = (Spinner)findViewById(R.id.appwidget_location_nameSelect);
        spin_locationName.setAdapter(getFixAdapter);
        spin_locationName.setOnItemSelectedListener(onCustomLocationSelected);

        labl_locationLat = (TextView)findViewById(R.id.appwidget_location_lat_label);
        text_locationLat = (EditText)findViewById(R.id.appwidget_location_lat);

        inputOverlay = findViewById(R.id.appwidget_location_latlon_overlay);
        inputOverlay.setVisibility(View.GONE);
        inputOverlay.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (mode == LocationViewMode.MODE_CUSTOM_SELECT)
                {
                    setMode(LocationViewMode.MODE_CUSTOM_EDIT);
                }
            }
        });

        labl_locationLon = (TextView)findViewById(R.id.appwidget_location_lon_label);
        text_locationLon = (EditText)findViewById(R.id.appwidget_location_lon);

        // custom mode: toggle edit mode
        button_edit = (ImageButton)findViewById(R.id.appwidget_location_edit);
        button_edit.setOnClickListener(onEditButtonClicked);

        // custom mode: save location
        button_save = (ImageButton)findViewById(R.id.appwidget_location_save);
        button_save.setOnClickListener(onSaveButtonClicked);

        // custom mode: get GPS fix
        progress_getfix = (ProgressBar)findViewById(R.id.appwidget_location_getfixprogress);
        progress_getfix.setVisibility(View.GONE);

        button_getfix = (ImageButton)findViewById(R.id.appwidget_location_getfix);
        getFixUI_editMode = new GetFixUI1(text_locationName, text_locationLat, text_locationLon, progress_getfix, button_getfix);

        button_getfix.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                getFixHelper.getFix(0);
            }
        });

        // auto mode: get GPS fix
        progress_auto = (ProgressBar)findViewById(R.id.appwidget_location_auto_progress);
        progress_auto.setVisibility(View.GONE);

        button_auto = (ImageButton)findViewById(R.id.appwidget_location_auto);
        getFixUI_autoMode = new GetFixUI2(text_locationName, text_locationLat, text_locationLon, progress_auto, button_auto);
        button_auto.setOnClickListener(onAutoButtonClicked);

        getFixHelper = new GetFixHelper(myParent, getFixUI_editMode);    // 0; getFixUI_editMode
        getFixHelper.addUI(getFixUI_autoMode);                           // 1; getFixUI_autoMode
        updateGPSButtonIcons();
    }

    public void updateGPSButtonIcons()
    {
        int icon = GetFixUI.ICON_GPS_SEARCHING;
        if (!isInEditMode())
        {
            if (!getFixHelper.isGPSEnabled())
            {
                icon = GetFixUI.ICON_GPS_DISABLED;

            } else if (getFixHelper.gotFix) {
                icon = GetFixUI.ICON_GPS_FOUND;
            }
        }
        button_getfix.setImageResource(icon);
        button_auto.setImageResource(icon);
    }

    public void onResume()
    {
        Log.d("DEBUG", "LocationConfigView onResume");
        updateGPSButtonIcons();
        getFixHelper.onResume();
    }

    /**
     * @param location
     */
    private void updateViews(WidgetSettings.Location location)
    {
        text_locationLat.setText(location.getLatitude());
        text_locationLon.setText(location.getLongitude());
        text_locationName.setText(location.getLabel());
    }

    /**
     *
     */
    protected void loadSettings(Context context)
    {
        Log.d("DEBUG", "LocationConfigView loadSettings (prefs)");
        if (isInEditMode())
            return;

        WidgetSettings.LocationMode locationMode = WidgetSettings.loadLocationModePref(context, appWidgetId);
        if (locationMode == WidgetSettings.LocationMode.CURRENT_LOCATION && !autoAllowed)
        {
            spinner_locationMode.setSelection(LocationViewMode.MODE_CUSTOM_SELECT.ordinal());
        } else {
            spinner_locationMode.setSelection(locationMode.ordinal());
        }

        WidgetSettings.Location location = WidgetSettings.loadLocationPref(context, appWidgetId);
        updateViews(location);
    }

    /**
     *
     */
    protected void loadSettings(Context context, Bundle bundle )
    {
        Log.d("DEBUG", "LocationConfigView loadSettings (bundle)");

        // restore LocationMode spinner
        String modeString = bundle.getString(KEY_LOCATION_MODE);
        if (modeString != null)
        {
            WidgetSettings.LocationMode locationMode;
            try {
                locationMode = WidgetSettings.LocationMode.valueOf(modeString);
            } catch (IllegalArgumentException e) {
                locationMode = WidgetSettings.PREF_DEF_LOCATION_MODE;
            }
            spinner_locationMode.setSelection(locationMode.ordinal());

        } else {
            spinner_locationMode.setSelection(WidgetSettings.PREF_DEF_LOCATION_MODE.ordinal());
        }

        // restore location text fields
        String label = bundle.getString(KEY_LOCATION_LABEL);
        String longitude = bundle.getString(KEY_LOCATION_LONGITUDE);
        String latitude = bundle.getString(KEY_LOCATION_LATITUDE);
        WidgetSettings.Location location;
        if (longitude != null && latitude != null)
        {
            location = new WidgetSettings.Location(label, latitude, longitude);

        } else {
            Log.w("LocationConfigView", "Bundle contained null lat or lon; falling back to saved prefs.");
            location = WidgetSettings.loadLocationPref(context, appWidgetId);
        }
        updateViews(location);

        // restore dialog (sub)state
        String viewModeString = bundle.getString(KEY_DIALOGMODE);
        if (viewModeString != null)
        {
            LocationViewMode viewMode;
            try {
                viewMode = LocationViewMode.valueOf(viewModeString);
            } catch (IllegalArgumentException e) {
                Log.d("DEBUG", "Bundle contained bad viewModeString! " + e.toString());
                viewMode = LocationViewMode.MODE_CUSTOM_SELECT;
            }
            setMode(viewMode);
        }

        getFixHelper.loadSettings(bundle);
    }

    /**
     * @param context
     * @param data
     */
    protected void loadSettings(Context context, Uri data )
    {
        Log.d("DEBUG", "LocationConfigView loadSettings (uri)");
        loadSettings(context, bundleData(data, context.getString(R.string.gps_lastfix_title_set)));
    }

    /**
     *
     */
    protected boolean saveSettings(Context context)
    {
        Log.d("DEBUG", "LocationConfigView loadSettings (prefs)");

        WidgetSettings.LocationMode locationMode = getLocationMode();
        WidgetSettings.saveLocationModePref(context, appWidgetId, locationMode);

        if (validateInput())
        {
            String latitude = text_locationLat.getText().toString();
            String longitude = text_locationLon.getText().toString();
            String name = text_locationName.getText().toString();
            WidgetSettings.Location location = new WidgetSettings.Location(name, latitude, longitude);
            WidgetSettings.saveLocationPref(context, appWidgetId, location);
            return true;
        }
        return false;
    }

    /**
     * @param bundle
     * @return
     */
    protected boolean saveSettings(Bundle bundle)
    {
        Log.d("DEBUG", "LocationConfigView saveSettings (bundle)");

        WidgetSettings.LocationMode locationMode = getLocationMode();
        String latitude = text_locationLat.getText().toString();
        String longitude = text_locationLon.getText().toString();
        String name = text_locationName.getText().toString();

        bundle.putString(KEY_DIALOGMODE, mode.name());
        bundle.putString(KEY_LOCATION_MODE, locationMode.name());
        bundle.putString(KEY_LOCATION_LATITUDE, latitude);
        bundle.putString(KEY_LOCATION_LONGITUDE, longitude);
        bundle.putString(KEY_LOCATION_LABEL, name);

        getFixHelper.saveSettings(bundle);
        return true;
    }

    public static Bundle bundleData( Uri data, String label )
    {
        String lat = "";
        String lon = "";

        if (data.getScheme().equals("geo"))
        {
            String dataString = data.getSchemeSpecificPart();
            String[] dataParts = dataString.split(Pattern.quote("?"));
            if (dataParts.length > 0)
            {
                String geoPath = dataParts[0];
                String[] geoParts = geoPath.split(Pattern.quote(","));
                if (geoParts.length >= 2)
                {
                    lat = geoParts[0];
                    lon = geoParts[1];
                }
            }
        }

        Bundle bundle = new Bundle();
        bundle.putString(KEY_DIALOGMODE, LocationViewMode.MODE_CUSTOM_ADD.name());
        bundle.putString(KEY_LOCATION_MODE, WidgetSettings.LocationMode.CUSTOM_LOCATION.name());
        bundle.putString(KEY_LOCATION_LATITUDE, lat);
        bundle.putString(KEY_LOCATION_LONGITUDE, lon);
        bundle.putString(KEY_LOCATION_LABEL, label);
        return bundle;
    }

    /**
     * Cancel any running getfix tasks.
     */
    public void cancelGetFix()
    {
        getFixHelper.cancelGetFix();
    }

    /**
     * Dismiss any "enable GPS" prompts.
     */
    //public void dismissGPSEnabledPrompt() { getFixHelper.dismissGPSEnabledPrompt(); }

    /**
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        getFixHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     *
     */
    protected void populateLocationList()
    {
        LocationListTask task = new LocationListTask(myParent, getLocation());
        task.setTaskListener( new LocationListTask.LocationListTaskListener()
        {
            @Override
            public void onLoaded(@NonNull Cursor result, int selectedIndex)
            {
                 getFixAdapter.changeCursor(result);
                 spin_locationName.setSelection(selectedIndex);
            }
        });
        task.execute((Object[]) null);
    }

    /**
     * A ListAdapter of WidgetListItems.
     */
    /**public static class LocationListAdapter extends ArrayAdapter<WidgetSettings.Location>
    {
        private Context context;
        private ArrayList<WidgetSettings.Location> locations;

        public LocationListAdapter(Context context, ArrayList<WidgetSettings.Location> locations)
        {
            super(context, R.layout.layout_listitem_locations, locations);
            this.context = context;
            this.locations = locations;
        }

        public LocationListAdapter(Context context, WidgetSettings.Location[] locations)
        {
            super(context, R.layout.layout_listitem_locations, locations);
            this.context = context;
            this.locations = new ArrayList<>();
            for (WidgetSettings.Location location : locations)
            {
                this.locations.add(location);
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            return listItemView(position, convertView, parent);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent)
        {
            return listItemView(position, convertView, parent);
        }

        private View listItemView(int position, View convertView, ViewGroup parent)
        {
            WidgetSettings.Location item = locations.get(position);

            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.layout_listitem_locations, parent, false);

            //ImageView icon = (ImageView) view.findViewById(android.R.id.icon1);
            //icon.setImageResource(item.getIcon());

            TextView text = (TextView) view.findViewById(android.R.id.text1);
            text.setText(item.getLabel());

            //TextView text2 = (TextView) view.findViewById(android.R.id.text2);
            //text2.setText(item.toString());

            return view;
        }
    }*/

    /**
     * Check text fields for validity; as a side-effect sets an error message on fields with invalid
     * values.
     * @return true if all fields valid, false otherwise
     */
    public boolean validateInput()
    {
        boolean isValid = true;

        String latitude = text_locationLat.getText().toString();
        try {
            BigDecimal lat = new BigDecimal(latitude);
            if (lat.intValue() < -90 || lat.intValue() > 90)
            {
                isValid = false;
                text_locationLat.setError(myParent.getString(R.string.location_dialog_error_lat));
            }

        } catch (NumberFormatException e1) {
            isValid = false;
            text_locationLat.setError(myParent.getString(R.string.location_dialog_error_lat));
        }

        String longitude = text_locationLon.getText().toString();
        try {
            BigDecimal lon = new BigDecimal(longitude);
            if (lon.intValue() < -180 || lon.intValue() > 180)
            {
                isValid = false;
                text_locationLon.setError(myParent.getString(R.string.location_dialog_error_lon));
            }

        } catch (NumberFormatException e2) {
            isValid = false;
            text_locationLon.setError(myParent.getString(R.string.location_dialog_error_lon));
        }

        return isValid;
    }

    /**
     * Enum of possible ui states; auto mode, custom (select), custom (add), custom (edit) modes.
     */
    public static enum LocationViewMode
    {
        MODE_AUTO(), MODE_CUSTOM_SELECT(), MODE_CUSTOM_ADD(), MODE_CUSTOM_EDIT();
        private LocationViewMode() {}

        public String toString()
        {
            return this.name();
        }

        public int ordinal( LocationViewMode[] array )
        {
            for (int i=0; i<array.length; i++)
            {
                if (array[i].name().equals(this.name()))
                {
                    return i;
                }
            }
            return -1;
        }
    }

    /**
     * Copy the location in decimal degrees (DD) to clipboard (locale invariant `lat, lon`)
     */
    public void copyLocationToClipboard(Context context)
    {
        copyLocationToClipboard(context, false);
    }
    public void copyLocationToClipboard(Context context, boolean silent)
    {
        WidgetSettings.Location location = getLocation();
        String clipboardText = location.toString();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
        {
            ClipboardManager clipboard = (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("lat, lon", clipboardText);
            clipboard.setPrimaryClip(clip);

        } else {
            @SuppressWarnings("deprecation")
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(clipboardText);
        }

        if (!silent)
        {
            Toast copiedMsg = Toast.makeText(context, Html.fromHtml(context.getString(R.string.location_dialog_toast_copied, clipboardText)), Toast.LENGTH_LONG);
            copiedMsg.show();
        }
    }

    /**
     * the location mode (auto, custom) has been selected from a spinner.
     */
    private Spinner.OnItemSelectedListener onLocationModeSelected = new Spinner.OnItemSelectedListener()
    {
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
        {
            final WidgetSettings.LocationMode[] locationModes = WidgetSettings.LocationMode.values();
            WidgetSettings.LocationMode locationMode = locationModes[parent.getSelectedItemPosition()];
            Log.d("DEBUG", "onLocationModeSelected " + locationMode.name());

            LocationViewMode dialogMode;
            if (locationMode == WidgetSettings.LocationMode.CUSTOM_LOCATION)
            {
                if (mode != LocationViewMode.MODE_CUSTOM_SELECT &&
                    mode != LocationViewMode.MODE_CUSTOM_ADD &&
                    mode != LocationViewMode.MODE_CUSTOM_EDIT)
                {
                    dialogMode = LocationViewMode.MODE_CUSTOM_SELECT;
                    setMode(dialogMode);
                }

            } else {
                if (mode == LocationViewMode.MODE_CUSTOM_ADD ||
                    mode == LocationViewMode.MODE_CUSTOM_EDIT)
                {
                    populateLocationList();  // triggers 'add place'
                }

                dialogMode = LocationViewMode.MODE_AUTO;
                setMode(dialogMode);
            }
        }
        public void onNothingSelected(AdapterView<?> parent) {}
    };

    /**
     * a custom location has been selected from a spinner.
     */
    private Spinner.OnItemSelectedListener onCustomLocationSelected = new Spinner.OnItemSelectedListener()
    {
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
        {
            Cursor cursor = getFixAdapter.getCursor();
            cursor.moveToPosition(position);

            if (cursor.getColumnCount() >= 3)
            {
                updateViews(new WidgetSettings.Location(cursor.getString(1), cursor.getString(2), cursor.getString(3)));
            }
        }
        public void onNothingSelected(AdapterView<?> parent) {}
    };

    /**
     * the custom location edit button has been clicked.
     */
    private View.OnClickListener onEditButtonClicked = new View.OnClickListener()
    {
        @Override
        public void onClick(View view)
        {
            setMode(LocationViewMode.MODE_CUSTOM_EDIT);
        }
    };

    /**
     * the custom location save button has been clicked.
     */
    private View.OnClickListener onSaveButtonClicked = new View.OnClickListener()
    {
        @Override
        public void onClick(View view)
        {
            final boolean validInput = validateInput();
            if (validInput)
            {
                setMode(LocationViewMode.MODE_CUSTOM_SELECT);
                populateLocationList();
            }

            final GetFixTask.GetFixTaskListener cancelGetFixListener = new GetFixTask.GetFixTaskListener()
            {
                @Override
                public void onCancelled()
                {
                    if (validInput)
                    {
                        setMode(LocationViewMode.MODE_CUSTOM_SELECT);
                        populateLocationList();
                    }
                }
            };
            getFixHelper.removeGetFixTaskListener(cancelGetFixListener);
            getFixHelper.addGetFixTaskListener(cancelGetFixListener);
            getFixHelper.cancelGetFix();
        }
    };

    /**
     * the auto location button has been clicked.
     */
    private View.OnClickListener onAutoButtonClicked = new OnClickListener()
    {
        @Override
        public void onClick(View view)
        {
            getFixHelper.getFix(1);
        }
    };

    /**
     *
     */
    private class LocationModeAdapter extends ArrayAdapter<WidgetSettings.LocationMode>
    {
        private Context context;
        private ArrayList<WidgetSettings.LocationMode> modes;

        public LocationModeAdapter(Context context, ArrayList<WidgetSettings.LocationMode> modes)
        {
            super(context, R.layout.layout_listitem_locations, modes);
            this.context = context;
            this.modes = modes;
        }

        public LocationModeAdapter(Context context, WidgetSettings.LocationMode[] modes)
        {
            super(context, R.layout.layout_listitem_locations, modes);
            this.context = context;
            this.modes = new ArrayList<WidgetSettings.LocationMode>();
            Collections.addAll(this.modes, modes);
        }

        @Override
        public boolean areAllItemsEnabled()
        {
           return autoAllowed;
        }

        @Override
        public boolean isEnabled(int position)
        {
            //noinspection RedundantIfStatement
            if (position == 0 && !autoAllowed)
                return false;
            else return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            return listItemView(position, convertView, parent);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent)
        {
            return listItemView(position, convertView, parent);
        }

        private View listItemView(int position, View convertView, ViewGroup parent)
        {
            WidgetSettings.LocationMode item = modes.get(position);

            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.layout_listitem_locations, parent, false);

            //ImageView icon = (ImageView) view.findViewById(android.R.id.icon1);
            //icon.setImageResource(item.getIcon());

            TextView text = (TextView) view.findViewById(android.R.id.text1);
            text.setText(item.getDisplayString());

            if (item == WidgetSettings.LocationMode.CURRENT_LOCATION && !autoAllowed)
            {
                text.setTypeface(text.getTypeface(), Typeface.ITALIC);
                text.setPaintFlags(text.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                text.setTextColor(text.getHintTextColors());
                view.setEnabled(false);
            }

            //TextView text2 = (TextView) view.findViewById(android.R.id.text2);
            //text2.setText(item.toString());

            return view;
        }
    }

}
