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

import android.annotation.TargetApi;
import android.appwidget.AppWidgetManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.os.Parcelable;
import android.provider.CalendarContract;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.forrestguice.suntimeswidget.calculator.SuntimesData;
import com.forrestguice.suntimeswidget.calculator.SuntimesDataset;
import com.forrestguice.suntimeswidget.getfix.GetFixHelper;
import com.forrestguice.suntimeswidget.getfix.GetFixUI;
import com.forrestguice.suntimeswidget.notes.NoteChangedListener;
import com.forrestguice.suntimeswidget.notes.NoteData;
import com.forrestguice.suntimeswidget.notes.SuntimesNotes;
import com.forrestguice.suntimeswidget.notes.SuntimesNotes3;
import com.forrestguice.suntimeswidget.settings.AppSettings;
import com.forrestguice.suntimeswidget.settings.SolarEvents;
import com.forrestguice.suntimeswidget.settings.WidgetSettings;
import com.forrestguice.suntimeswidget.settings.WidgetTimezones;

import java.lang.reflect.Method;
import java.text.DateFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

public class SuntimesActivity extends AppCompatActivity
{
    public static final String KEY_UI_CARDISTOMORROW = "cardIsTomorrow";
    public static final String KEY_UI_USERSWAPPEDCARD = "userSwappedCard";

    private static final String DIALOGTAG_TIMEZONE = "timezone";
    private static final String DIALOGTAG_ALARM = "alarm";
    private static final String DIALOGTAG_ABOUT = "about";
    private static final String DIALOGTAG_HELP = "help";
    private static final String DIALOGTAG_LOCATION = "location";
    private static final String DIALOGTAG_DATE = "dateselect";

    protected static SuntimesUtils utils = new SuntimesUtils();

    private ActionBar actionBar;
    private Menu actionBarMenu;

    private GetFixHelper getFixHelper;

    private WidgetSettings.Location location;
    private SuntimesNotes notes;
    private SuntimesDataset dataset;

    // clock views
    private TextView txt_time;
    private TextView txt_time_suffix;
    private TextView txt_timezone;

    // note views
    private ProgressBar note_progress;
    private ViewFlipper note_flipper;
    private Animation anim_note_inPrev;
    private Animation anim_note_inNext;
    private Animation anim_note_outNext;
    private Animation anim_note_outPrev;

    private ImageView ic_time1_note;
    private TextView txt_time1_note1;
    private TextView txt_time1_note2;
    private TextView txt_time1_note3;

    private ImageView ic_time2_note;
    private TextView txt_time2_note1;
    private TextView txt_time2_note2;
    private TextView txt_time2_note3;

    // time card views
    private ViewFlipper card_flipper;
    private Animation anim_card_inPrev;
    private Animation anim_card_inNext;
    private Animation anim_card_outNext;
    private Animation anim_card_outPrev;

    private ImageButton btn_flipperNext_today;
    private ImageButton btn_flipperPrev_today;
    private ImageButton btn_flipperNext_tomorrow;
    private ImageButton btn_flipperPrev_tomorrow;

    private TextView txt_date;
    private TextView txt_sunrise_actual;
    private TextView txt_sunrise_civil;
    private TextView txt_sunrise_nautical;
    private TextView txt_sunrise_astro;
    private TextView txt_sunset_actual;
    private TextView txt_sunset_civil;
    private TextView txt_sunset_nautical;
    private TextView txt_sunset_astro;
    private TextView txt_solarnoon;

    private LinearLayout layout_daylength;
    private TextView txt_daylength;
    private TextView txt_lightlength;

    private TextView txt_date2;
    private TextView txt_sunrise2_actual;
    private TextView txt_sunrise2_civil;
    private TextView txt_sunrise2_nautical;
    private TextView txt_sunrise2_astro;
    private TextView txt_sunset2_actual;
    private TextView txt_sunset2_civil;
    private TextView txt_sunset2_nautical;
    private TextView txt_sunset2_astro;
    private TextView txt_solarnoon2;

    private LinearLayout layout_daylength2;
    private TextView txt_daylength2;
    private TextView txt_lightlength2;

    private boolean isRtl = false;
    private boolean userSwappedCard = false;
    private HashMap<SolarEvents.SolarEventField, TextView> timeFields;

    private boolean showWarnings = false;
    private boolean showDateWarning = false, showTimezoneWarning = false;
    private Snackbar timezoneWarning = null, dateWarning = null;

    public SuntimesActivity()
    {
        super();
    }

    /**
     * OnCreate: the Activity initially created
     * @param savedState
     */
    @Override
    public void onCreate(Bundle savedState)
    {
        Context context = SuntimesActivity.this;
        calculateData(context);

        setTheme(AppSettings.loadTheme(this, dataset));
        GetFixUI.themeIcons(this);

        super.onCreate(savedState);
        setResult(RESULT_CANCELED);

        initLocale(this);  // must follow super.onCreate or locale is reverted
        setContentView(R.layout.layout_main);
        initViews(context);

        initGetFix();
        getFixHelper.loadSettings(savedState);
        notes.resetNoteIndex();

        Intent intent = getIntent();
        Uri data = intent.getData();
        if (data != null)
        {
            intent.setData(null);
            configLocation(data);
        }
    }

    private void initLocale( Context context )
    {
        AppSettings.initLocale(this);
        isRtl = AppSettings.isLocaleRtl(this);

        WidgetSettings.initDefaults(context);        // locale specific defaults

        SuntimesUtils.initDisplayStrings(context);   // locale specific strings
        AppSettings.initDisplayStrings(context);
        WidgetSettings.initDisplayStrings(context);

        initAnimations(context);                     // locale specific animations
    }

    /**
     * OnStart: the Activity becomes visible
     */
    @Override
    public void onStart()
    {
        super.onStart();
        calculateData(SuntimesActivity.this);
        updateViews(SuntimesActivity.this);
    }

    /**
     * OnResume: the user is now interacting w/ the Activity (running state)
     */
    @Override
    public void onResume()
    {
        super.onResume();
        updateActionBar(this);
        getFixHelper.onResume();

        // restore open dialogs
        FragmentManager fragments = getSupportFragmentManager();
        TimeZoneDialog timezoneDialog = (TimeZoneDialog) fragments.findFragmentByTag(DIALOGTAG_TIMEZONE);
        if (timezoneDialog != null)
        {
            timezoneDialog.setOnAcceptedListener(onConfigTimeZone);
            Log.d("DEBUG", "TimeZoneDialog listeners restored.");
        }

        AlarmDialog alarmDialog = (AlarmDialog) fragments.findFragmentByTag(DIALOGTAG_ALARM);
        if (alarmDialog != null)
        {
            alarmDialog.setData(dataset);
            alarmDialog.setOnAcceptedListener(alarmDialog.scheduleAlarmClickListener);
            Log.d("DEBUG", "AlarmDialog listeners restored.");
        }

        LocationConfigDialog locationDialog = (LocationConfigDialog) fragments.findFragmentByTag(DIALOGTAG_LOCATION);
        if (locationDialog != null)
        {
            locationDialog.setOnAcceptedListener( onConfigLocation(locationDialog) );
            Log.d("DEBUG", "LocationDialog listeners restored.");
        }

        //TimeDateDialog dateDialog = (TimeDateDialog) fragments.findFragmentByTag(DIALOGTAG_DATE);
        //if (dateDialog != null)
        //{
            // TODO
        //}
    }

    /**
     * OnPause: the user about to interact w/ another Activity
     */
    @Override
    public void onPause()
    {
        super.onPause();
    }

    @Override
    public void onSaveInstanceState( Bundle outState )
    {
        outState.putBoolean(KEY_UI_USERSWAPPEDCARD, userSwappedCard);
        outState.putBoolean(KEY_UI_CARDISTOMORROW, (card_flipper.getDisplayedChild() != 0));
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState)
    {
        userSwappedCard = savedInstanceState.getBoolean(KEY_UI_USERSWAPPEDCARD, false);
        boolean cardIsTomorrow = savedInstanceState.getBoolean(KEY_UI_CARDISTOMORROW, false);
        card_flipper.setDisplayedChild((cardIsTomorrow ? 1 : 0));
    }

    /**
     * OnStop: the Activity no longer visible
     */
    @Override
    public void onStop()
    {
        stopTimeTask();
        getFixHelper.cancelGetFix();
        super.onStop();
    }

    /**
     * OnDestroy: the activity destroyed
     */
    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }

    /**
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        FragmentManager fragments = getSupportFragmentManager();
        LocationConfigDialog locationDialog = (LocationConfigDialog) fragments.findFragmentByTag(DIALOGTAG_LOCATION);
        if (locationDialog != null)
        {
            locationDialog.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

        WidgetSettings.LocationMode locationMode = WidgetSettings.loadLocationModePref(this, 0);
        if (locationMode == WidgetSettings.LocationMode.CURRENT_LOCATION)
        {
            getFixHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * initialize ui/views
     * @param context
     */
    protected void initViews(Context context)
    {
        initActionBar(context);
        initClockViews(context);
        initNoteViews(context);
        initCardViews(context);
    }

    /**
     * initialize the actionbar
     */
    private void initActionBar(Context context)
    {
        Toolbar menuBar = (Toolbar) findViewById(R.id.app_menubar);
        setSupportActionBar(menuBar);
        actionBar = getSupportActionBar();
    }

    /**
     * initialize gps helper
     */
    private void initGetFix()
    {
        getFixHelper = new GetFixHelper(this, new GetFixUI()
        {
            private MenuItem refreshItem = null;

            @Override
            public void enableUI(boolean value)
            {
                if (refreshItem != null)
                {
                    refreshItem.setEnabled(value);
                }
            }

            @Override
            public void updateUI(Location... locations)
            {
                WidgetSettings.Location location = new WidgetSettings.Location(getString(R.string.gps_lastfix_title_found), locations[0]);
                actionBar.setSubtitle(location.toString());
            }

            @Override
            public void showProgress(boolean showProgress)
            {
                note_progress.setVisibility((showProgress ? View.VISIBLE : View.GONE));
            }

            @Override
            public void onStart()
            {
                invalidateData(SuntimesActivity.this);

                refreshItem = actionBarMenu.findItem(R.id.action_location_refresh);
                if (refreshItem != null)
                {
                    actionBar.setTitle(getString(R.string.gps_lastfix_title_searching));
                    actionBar.setSubtitle("");
                    refreshItem.setIcon(GetFixUI.ICON_GPS_SEARCHING);
                }
            }

            @Override
            public void onResult(Location result, boolean wasCancelled)
            {
                if (refreshItem != null)
                {
                    refreshItem.setIcon((result != null) ? ICON_GPS_FOUND :
                            (getFixHelper.isLocationEnabled() ? ICON_GPS_FOUND
                                                              : ICON_GPS_DISABLED));

                    if (result != null)
                    {
                        WidgetSettings.Location location = new WidgetSettings.Location(getString(R.string.gps_lastfix_title_found), result);
                        WidgetSettings.saveLocationPref(SuntimesActivity.this, 0, location);

                    } else {
                        String msg = (wasCancelled ? getString(R.string.gps_lastfix_toast_cancelled) : getString(R.string.gps_lastfix_toast_notfound));
                        Toast errorMsg = Toast.makeText(SuntimesActivity.this, msg, Toast.LENGTH_LONG);
                        errorMsg.show();
                    }
                    SuntimesActivity.this.calculateData(SuntimesActivity.this);
                    SuntimesActivity.this.updateViews(SuntimesActivity.this);
                }
            }
        });
    }

    /**
     * update actionbar items; shouldn't be called until after the menu is inflated.
     */
    private void updateActionBar(Context context)
    {
        if (actionBarMenu != null)
        {
            MenuItem refreshItem = actionBarMenu.findItem(R.id.action_location_refresh);
            if (refreshItem != null)
            {
                WidgetSettings.LocationMode mode = WidgetSettings.loadLocationModePref(context, 0);
                if (mode != WidgetSettings.LocationMode.CURRENT_LOCATION)
                {
                    refreshItem.setVisible(false);

                } else
                {
                    refreshItem.setIcon((getFixHelper.isLocationEnabled() ? GetFixUI.ICON_GPS_FOUND : GetFixUI.ICON_GPS_DISABLED));
                    refreshItem.setVisible(true);
                }
            }
        }
    }

    /**
     * initialize the note flipper and associated views
     * @param context
     */
    private void initNoteViews(Context context)
    {
        note_progress = (ProgressBar) findViewById(R.id.info_note_progress);
        if (note_progress != null)
        {
            note_progress.setVisibility(View.GONE);
        }

        note_flipper = (ViewFlipper) findViewById(R.id.info_note_flipper);
        if (note_flipper != null)
        {
            note_flipper.setOnTouchListener(noteTouchListener);
            note_flipper.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                { /* DO NOTHING HERE (but we still need this listener) */ }
            });

        } else {
            Log.w("initNoteViews", "Failed to set touchListener; note_flipper is null!");
        }

        LinearLayout note1 = (LinearLayout) findViewById(R.id.info_time_note1);
        if (note1 != null)
        {
            txt_time1_note1 = (TextView) note1.findViewById(R.id.text_timenote1);
            txt_time1_note2 = (TextView) note1.findViewById(R.id.text_timenote2);
            txt_time1_note3 = (TextView) note1.findViewById(R.id.text_timenote3);
            ic_time1_note = (ImageView) note1.findViewById(R.id.icon_timenote);
            ic_time1_note.setVisibility(View.INVISIBLE);

        } else {
            Log.w("initNoteViews", "Failed to init note layout1; was null!");
        }

        LinearLayout note2 = (LinearLayout) findViewById(R.id.info_time_note2);
        if (note2 != null)
        {
            txt_time2_note1 = (TextView) note2.findViewById(R.id.text_timenote1);
            txt_time2_note2 = (TextView) note2.findViewById(R.id.text_timenote2);
            txt_time2_note3 = (TextView) note2.findViewById(R.id.text_timenote3);
            ic_time2_note = (ImageView) note2.findViewById(R.id.icon_timenote);
            ic_time2_note.setVisibility(View.INVISIBLE);

        } else {
            Log.w("initNoteViews", "Failed to init note layout2; was null!");
        }
    }

    /**
     * initialize the card flipper and associated views
     * @param context
     */
    private void initCardViews(Context context)
    {
        timeFields = new HashMap<SolarEvents.SolarEventField, TextView>();
        card_flipper = (ViewFlipper) findViewById(R.id.info_time_flipper);
        if (card_flipper != null)
        {
            card_flipper.setOnTouchListener(timeCardTouchListener);
        } else {
            Log.w("initCardViews", "Failed to set touchListener; card_flipper was null!");
        }

        // Today's times
        LinearLayout viewToday = (LinearLayout)findViewById(R.id.info_time_all_today);
        if (viewToday != null)
        {
            txt_date = (TextView) viewToday.findViewById(R.id.text_date);
            txt_date.setOnClickListener(dateTapClickListener(false));

            txt_sunrise_actual = (TextView) viewToday.findViewById(R.id.text_time_sunrise_actual);
            txt_sunset_actual = (TextView) viewToday.findViewById(R.id.text_time_sunset_actual);
            timeFields.put(new SolarEvents.SolarEventField(SolarEvents.SUNRISE, false), txt_sunrise_actual);
            timeFields.put(new SolarEvents.SolarEventField(SolarEvents.SUNSET, false), txt_sunset_actual);

            txt_sunrise_civil = (TextView) viewToday.findViewById(R.id.text_time_sunrise_civil);
            txt_sunset_civil = (TextView) viewToday.findViewById(R.id.text_time_sunset_civil);
            timeFields.put(new SolarEvents.SolarEventField(SolarEvents.MORNING_CIVIL, false), txt_sunrise_civil);
            timeFields.put(new SolarEvents.SolarEventField(SolarEvents.EVENING_CIVIL, false), txt_sunset_civil);

            txt_sunrise_nautical = (TextView) viewToday.findViewById(R.id.text_time_sunrise_nautical);
            txt_sunset_nautical = (TextView) viewToday.findViewById(R.id.text_time_sunset_nautical);
            timeFields.put(new SolarEvents.SolarEventField(SolarEvents.MORNING_NAUTICAL, false), txt_sunrise_nautical);
            timeFields.put(new SolarEvents.SolarEventField(SolarEvents.EVENING_NAUTICAL, false), txt_sunset_nautical);

            txt_sunrise_astro = (TextView) viewToday.findViewById(R.id.text_time_sunrise_astro);
            txt_sunset_astro = (TextView) viewToday.findViewById(R.id.text_time_sunset_astro);
            timeFields.put(new SolarEvents.SolarEventField(SolarEvents.MORNING_ASTRONOMICAL, false), txt_sunrise_astro);
            timeFields.put(new SolarEvents.SolarEventField(SolarEvents.EVENING_ASTRONOMICAL, false), txt_sunset_astro);

            txt_solarnoon = (TextView) viewToday.findViewById(R.id.text_time_noon);
            timeFields.put(new SolarEvents.SolarEventField(SolarEvents.NOON, false), txt_solarnoon);

            layout_daylength = (LinearLayout) viewToday.findViewById(R.id.layout_daylength);
            txt_daylength = (TextView) viewToday.findViewById(R.id.text_daylength);
            txt_lightlength = (TextView) viewToday.findViewById(R.id.text_lightlength);

            btn_flipperNext_today = (ImageButton)viewToday.findViewById(R.id.info_time_nextbtn);
            btn_flipperNext_today.setOnClickListener(onNextCardClick);
            btn_flipperNext_today.setOnTouchListener(new View.OnTouchListener()
            {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent)
                {
                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                        btn_flipperNext_today.setColorFilter(ContextCompat.getColor(SuntimesActivity.this, R.color.btn_tint_pressed));
                    } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                        btn_flipperNext_today.setColorFilter(null);
                    }
                    return false;
                }
            });

            btn_flipperPrev_today = (ImageButton)viewToday.findViewById(R.id.info_time_prevbtn);
            btn_flipperPrev_today.setOnClickListener(onPrevCardClick);
            btn_flipperPrev_today.setVisibility(View.GONE);

        } else {
            Log.w("initCardViews", "Failed to init card layout1; was null!");
        }

        // Tomorrow's times
        LinearLayout viewTomorrow = (LinearLayout)findViewById(R.id.info_time_all_tomorrow);
        if (viewTomorrow != null)
        {
            txt_date2 = (TextView) viewTomorrow.findViewById(R.id.text_date);
            txt_date2.setOnClickListener(dateTapClickListener(true));

            txt_sunrise2_actual = (TextView) viewTomorrow.findViewById(R.id.text_time_sunrise_actual);
            txt_sunset2_actual = (TextView) viewTomorrow.findViewById(R.id.text_time_sunset_actual);
            timeFields.put(new SolarEvents.SolarEventField(SolarEvents.SUNRISE, true), txt_sunrise2_actual);
            timeFields.put(new SolarEvents.SolarEventField(SolarEvents.SUNSET, true), txt_sunset2_actual);

            txt_sunrise2_civil = (TextView) viewTomorrow.findViewById(R.id.text_time_sunrise_civil);
            txt_sunset2_civil = (TextView) viewTomorrow.findViewById(R.id.text_time_sunset_civil);
            timeFields.put(new SolarEvents.SolarEventField(SolarEvents.MORNING_CIVIL, true), txt_sunrise2_civil);
            timeFields.put(new SolarEvents.SolarEventField(SolarEvents.EVENING_CIVIL, true), txt_sunset2_civil);

            txt_sunrise2_nautical = (TextView) viewTomorrow.findViewById(R.id.text_time_sunrise_nautical);
            txt_sunset2_nautical = (TextView) viewTomorrow.findViewById(R.id.text_time_sunset_nautical);
            timeFields.put(new SolarEvents.SolarEventField(SolarEvents.MORNING_NAUTICAL, true), txt_sunrise2_nautical);
            timeFields.put(new SolarEvents.SolarEventField(SolarEvents.EVENING_NAUTICAL, true), txt_sunset2_nautical);

            txt_sunrise2_astro = (TextView) viewTomorrow.findViewById(R.id.text_time_sunrise_astro);
            txt_sunset2_astro = (TextView) viewTomorrow.findViewById(R.id.text_time_sunset_astro);
            timeFields.put(new SolarEvents.SolarEventField(SolarEvents.MORNING_ASTRONOMICAL, true), txt_sunrise2_astro);
            timeFields.put(new SolarEvents.SolarEventField(SolarEvents.EVENING_ASTRONOMICAL, true), txt_sunset2_astro);

            txt_solarnoon2 = (TextView) viewTomorrow.findViewById(R.id.text_time_noon);
            timeFields.put(new SolarEvents.SolarEventField(SolarEvents.NOON, true), txt_solarnoon2);

            layout_daylength2 = (LinearLayout) viewTomorrow.findViewById(R.id.layout_daylength);
            txt_daylength2 = (TextView) viewTomorrow.findViewById(R.id.text_daylength);
            txt_lightlength2 = (TextView) viewTomorrow.findViewById(R.id.text_lightlength);

            btn_flipperNext_tomorrow = (ImageButton)viewTomorrow.findViewById(R.id.info_time_nextbtn);
            btn_flipperNext_tomorrow.setOnClickListener(onNextCardClick);
            btn_flipperNext_tomorrow.setVisibility(View.GONE);

            btn_flipperPrev_tomorrow = (ImageButton)viewTomorrow.findViewById(R.id.info_time_prevbtn);
            btn_flipperPrev_tomorrow.setOnClickListener(onPrevCardClick);
            btn_flipperPrev_tomorrow.setOnTouchListener(new View.OnTouchListener()
            {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent)
                {
                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                    {
                        btn_flipperPrev_tomorrow.setColorFilter(ContextCompat.getColor(SuntimesActivity.this, R.color.btn_tint_pressed));
                    } else if (motionEvent.getAction() == MotionEvent.ACTION_UP)
                    {
                        btn_flipperPrev_tomorrow.setColorFilter(null);
                    }
                    return false;
                }
            });

            initTimeFields();

        } else {
            Log.w("initCardViews", "Failed to init card layout2; was null!");
        }

        stretchTableRule();
    }

    /**
     * initialize the clock ui
     * @param context
     */
    private void initClockViews(Context context)
    {
        LinearLayout clockLayout = (LinearLayout) findViewById(R.id.layout_clock);
        if (clockLayout != null)
        {
            clockLayout.setOnClickListener(onClockClick);
        }

        txt_time = (TextView) findViewById(R.id.text_time);
        txt_time_suffix = (TextView) findViewById(R.id.text_time_suffix);
        txt_timezone = (TextView) findViewById(R.id.text_timezone);
    }

    /**
     * initialize view animations
     * @param context
     */
    private void initAnimations(Context context)
    {
        anim_note_inPrev = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        anim_note_inNext = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        anim_note_outPrev = AnimationUtils.loadAnimation(this, R.anim.fade_out);
        anim_note_outNext = AnimationUtils.loadAnimation(this, R.anim.fade_out);
        //anim_note_outPrev = AnimationUtils.loadAnimation(this, R.anim.slide_out_right);
        //anim_note_outNext = AnimationUtils.loadAnimation(this, R.anim.slide_out_left);

        anim_card_inPrev = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        anim_card_inNext = AnimationUtils.loadAnimation(this, R.anim.fade_in);

        anim_card_outPrev = AnimationUtils.loadAnimation(this, (isRtl ? R.anim.slide_out_left : R.anim.slide_out_right));
        anim_card_outNext = AnimationUtils.loadAnimation(this, (isRtl ? R.anim.slide_out_right : R.anim.slide_out_left));
    }

    /**
     * Initialize note object and onChanged listener.
     */
    private void initNotes()
    {
        notes = new SuntimesNotes3();
        notes.init(this, dataset);
        notes.setOnChangedListener(new NoteChangedListener()
        {
            @Override
            public void onNoteChanged(NoteData note, int transition)
            {
                updateNoteUI(note, transition);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actionbar, menu);
        actionBarMenu = menu;
        updateActionBar(this);
        return true;
    }

    @Override
    protected boolean onPrepareOptionsPanel(View view, Menu menu)
    {
        forceActionBarIcons(menu);
        return super.onPrepareOptionsPanel(view, menu);
    }

    /**
     * from http://stackoverflow.com/questions/18374183/how-to-show-icons-in-overflow-menu-in-actionbar
     */
    private void forceActionBarIcons(Menu menu)
    {
        if (menu != null)
        {
            if (menu.getClass().getSimpleName().equals("MenuBuilder"))
            {
                try {
                    Method m = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
                    m.setAccessible(true);
                    m.invoke(menu, true);

                } catch (Exception e) {
                    Log.e("SuntimesActivity", "failed to set show overflow icons", e);
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_settings:
                showSettings();
                return true;

            case R.id.action_about:
                showAbout();
                return true;

            case R.id.action_help:
                showHelp();
                return true;

            case R.id.action_location_add:
                configLocation();
                return true;

            case R.id.action_location_refresh:
                refreshLocation();
                return false;

            case R.id.action_location_show:
                showMap();
                return true;

            case R.id.action_timezone:
                configTimeZone();
                return true;

            case R.id.action_date:
                configDate();
                return true;

            case R.id.action_alarm:
                scheduleAlarm();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Select a date other than today.
     */
    private void configDate()
    {
        final TimeDateDialog datePicker = new TimeDateDialog();
        datePicker.setOnAcceptedListener(onConfigDate);
        datePicker.show(getSupportFragmentManager(), DIALOGTAG_DATE);
    }
    DialogInterface.OnClickListener onConfigDate = new DialogInterface.OnClickListener()
    {
        @Override
        public void onClick(DialogInterface dialogInterface, int i)
        {
            calculateData(SuntimesActivity.this);
            updateViews(SuntimesActivity.this);
        }
    };


    /**
     * Refresh location (current location mode).
     */
    protected void refreshLocation()
    {
        getFixHelper.getFix();
    }

    /**
     * Configure location.
     */
    protected void configLocation()
    {
        configLocation(null);
    }
    protected void configLocation( Uri data )
    {
        final LocationConfigDialog locationDialog = new LocationConfigDialog();
        locationDialog.setData(data);
        locationDialog.setHideTitle(true);
        locationDialog.setOnAcceptedListener(onConfigLocation(locationDialog));

        getFixHelper.cancelGetFix();
        locationDialog.show(getSupportFragmentManager(), DIALOGTAG_LOCATION);
    }
    protected DialogInterface.OnClickListener onConfigLocation( final LocationConfigDialog dialog )
    {
        return new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                calculateData(SuntimesActivity.this);
                updateActionBar(SuntimesActivity.this);
                updateViews(SuntimesActivity.this);

                WidgetSettings.LocationMode locationMode = dialog.getDialogContent().getLocationMode();
                if (locationMode == WidgetSettings.LocationMode.CURRENT_LOCATION)
                {
                    getFixHelper.getFix();
                }
            }
        };
    }



    /**
     * Configure time zone.
     */
    protected void configTimeZone()
    {
        TimeZoneDialog timezoneDialog = new TimeZoneDialog();
        timezoneDialog.setOnAcceptedListener(onConfigTimeZone);
        timezoneDialog.show(getSupportFragmentManager(), DIALOGTAG_TIMEZONE);
    }
    DialogInterface.OnClickListener onConfigTimeZone = new DialogInterface.OnClickListener()
    {
        @Override
        public void onClick(DialogInterface dialogInterface, int i)
        {
            calculateData(SuntimesActivity.this);
            updateViews(SuntimesActivity.this);
        }
    };

    /**
     * Show the location on a map.
     * Intent filtering code based off answer by "gumberculese";
     * http://stackoverflow.com/questions/5734678/custom-filtering-of-intent-chooser-based-on-installed-android-package-name
     */
    protected void showMap()
    {
        Intent mapIntent = new Intent(Intent.ACTION_VIEW);
        mapIntent.setData(location.getUri());
        //if (mapIntent.resolveActivity(getPackageManager()) != null)
        //{
        //    startActivity(mapIntent);
        //}

        String myPackage = "com.forrestguice.suntimeswidget";
        List<ResolveInfo> info = getPackageManager().queryIntentActivities(mapIntent, 0);
        List<Intent> geoIntents = new ArrayList<Intent>();

        if (!info.isEmpty())
        {
            for (ResolveInfo resolveInfo : info)
            {
                String packageName = resolveInfo.activityInfo.packageName;
                if (!TextUtils.equals(packageName, myPackage))
                {
                    Intent geoIntent = new Intent(Intent.ACTION_VIEW);
                    geoIntent.setPackage(packageName);
                    geoIntent.setData(location.getUri());
                    geoIntents.add(geoIntent);
                }
            }
        }

        if (geoIntents.size() > 0)
        {
            Intent chooserIntent = Intent.createChooser(geoIntents.remove(0), getString(R.string.configAction_mapLocation_chooser));
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, geoIntents.toArray(new Parcelable[geoIntents.size()]));
            startActivity(chooserIntent);

        } else {
            Toast noAppError = Toast.makeText(this, getString(R.string.configAction_mapLocation_noapp), Toast.LENGTH_LONG);
            noAppError.show();
        }
    }

    /**
     * Show the help dialog.
     */
    protected void showHelp()
    {
        String topic1 = getString(R.string.help_general_timeMode);
        String topic2 = getString(R.string.help_general_daylength);
        String helpText = getString(R.string.help_general, topic1, topic2);

        HelpDialog helpDialog = new HelpDialog();
        helpDialog.setContent(helpText);
        helpDialog.show(getSupportFragmentManager(), DIALOGTAG_HELP);
    }

    /**
     * Show the about dialog.
     */
    protected void showAbout()
    {
        AboutDialog aboutDialog = new AboutDialog();
        aboutDialog.show(getSupportFragmentManager(), DIALOGTAG_ABOUT);
    }

    /**
     * Show application settings.
     */
    protected void showSettings()
    {
        Intent settingsIntent = new Intent(this, SuntimesSettingsActivity.class);
        startActivity(settingsIntent);
    }

    /**
     * Show the alarm dialog.
     */
    protected void scheduleAlarm()
    {
        scheduleAlarm(null);
    }
    protected void scheduleAlarm( SolarEvents selected )
    {
        if (dataset.isCalculated())
        {
            AlarmDialog alarmDialog = new AlarmDialog();
            alarmDialog.setData(dataset);
            alarmDialog.setChoice(selected);
            alarmDialog.setOnAcceptedListener(alarmDialog.scheduleAlarmClickListener);
            alarmDialog.show(getSupportFragmentManager(), DIALOGTAG_ALARM);

        } else {
            String msg = getString(R.string.schedalarm_dialog_error2);
            Toast errorMsg = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
            errorMsg.show();
        }
    }

    protected void scheduleAlarmFromNote()
    {
        //scheduleAlarmFromNote(notes.getNote());
        scheduleAlarm(notes.getNote().noteMode);
    }

    protected void scheduleAlarmFromNote(NoteData note)
    {
        String alarmLabel = note.noteText;
        Calendar calendar = dataset.now();
        calendar.setTimeInMillis(note.time.getTime());
        AlarmDialog.scheduleAlarm(this, alarmLabel, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
    }

    /**
     *
     * @param context
     */
    private void initData( Context context )
    {
        SuntimesData data_actualTime = new SuntimesData(context, AppWidgetManager.INVALID_APPWIDGET_ID);
        data_actualTime.setCompareMode(WidgetSettings.CompareMode.TOMORROW);
        data_actualTime.setTimeMode(WidgetSettings.TimeMode.OFFICIAL);

        SuntimesData data_civilTime = new SuntimesData(data_actualTime);
        data_civilTime.setTimeMode(WidgetSettings.TimeMode.CIVIL);

        SuntimesData data_nauticalTime = new SuntimesData(data_actualTime);
        data_nauticalTime.setTimeMode(WidgetSettings.TimeMode.NAUTICAL);

        SuntimesData data_astroTime = new SuntimesData(data_actualTime);
        data_astroTime.setTimeMode(WidgetSettings.TimeMode.ASTRONOMICAL);

        SuntimesData data_noon = new SuntimesData(data_actualTime);
        data_noon.setTimeMode(WidgetSettings.TimeMode.NOON);

        dataset = new SuntimesDataset(data_actualTime, data_civilTime, data_nauticalTime, data_astroTime, data_noon);
    }

    protected void calculateData( Context context )
    {
        initData(context);
        dataset.calculateData();
        initNotes();
    }

    protected void invalidateData( Context context )
    {
        dataset.invalidateCalculation();
        updateViews(context);
    }

    protected void updateViews( Context context )
    {
        stopTimeTask();

        showWarnings = AppSettings.loadShowWarningsPref(this);
        showDateWarning = false;
        showTimezoneWarning = false;

        location = WidgetSettings.loadLocationPref(context, AppWidgetManager.INVALID_APPWIDGET_ID);
        String locationTitle = location.getLabel();
        String locationSubtitle = location.toString();

        if (actionBar != null)
        {
            actionBar.setTitle(locationTitle);
            actionBar.setSubtitle(locationSubtitle);
        }

        // today's view
        SuntimesUtils.TimeDisplayText sunriseString_actualTime = utils.calendarTimeShortDisplayString(context, dataset.dataActual.sunriseCalendarToday());
        SuntimesUtils.TimeDisplayText sunriseString_civilTime = utils.calendarTimeShortDisplayString(context, dataset.dataCivil.sunriseCalendarToday());
        SuntimesUtils.TimeDisplayText sunriseString_nauticalTime = utils.calendarTimeShortDisplayString(context, dataset.dataNautical.sunriseCalendarToday());
        SuntimesUtils.TimeDisplayText sunriseString_astroTime = utils.calendarTimeShortDisplayString(context, dataset.dataAstro.sunriseCalendarToday());
        SuntimesUtils.TimeDisplayText noonString = utils.calendarTimeShortDisplayString(context, dataset.dataNoon.sunriseCalendarToday() );
        SuntimesUtils.TimeDisplayText sunsetString_actualTime = utils.calendarTimeShortDisplayString(context, dataset.dataActual.sunsetCalendarToday());
        SuntimesUtils.TimeDisplayText sunsetString_civilTime = utils.calendarTimeShortDisplayString(context, dataset.dataCivil.sunsetCalendarToday());
        SuntimesUtils.TimeDisplayText sunsetString_nauticalTime = utils.calendarTimeShortDisplayString(context, dataset.dataNautical.sunsetCalendarToday());
        SuntimesUtils.TimeDisplayText sunsetString_astroTime = utils.calendarTimeShortDisplayString(context, dataset.dataAstro.sunsetCalendarToday());

        // tomorrow's view
        SuntimesUtils.TimeDisplayText sunriseString_actualTime2 = utils.calendarTimeShortDisplayString(context, dataset.dataActual.sunriseCalendarOther());
        SuntimesUtils.TimeDisplayText sunriseString_civilTime2 = utils.calendarTimeShortDisplayString(context, dataset.dataCivil.sunriseCalendarOther());
        SuntimesUtils.TimeDisplayText sunriseString_nauticalTime2 = utils.calendarTimeShortDisplayString(context, dataset.dataNautical.sunriseCalendarOther());
        SuntimesUtils.TimeDisplayText sunriseString_astroTime2 = utils.calendarTimeShortDisplayString(context, dataset.dataAstro.sunriseCalendarOther());
        SuntimesUtils.TimeDisplayText noonString2 = utils.calendarTimeShortDisplayString(context, dataset.dataNoon.sunriseCalendarOther() );
        SuntimesUtils.TimeDisplayText sunsetString_actualTime2 = utils.calendarTimeShortDisplayString(context, dataset.dataActual.sunsetCalendarOther());
        SuntimesUtils.TimeDisplayText sunsetString_civilTime2 = utils.calendarTimeShortDisplayString(context, dataset.dataCivil.sunsetCalendarOther());
        SuntimesUtils.TimeDisplayText sunsetString_nauticalTime2 = utils.calendarTimeShortDisplayString(context, dataset.dataNautical.sunsetCalendarOther());
        SuntimesUtils.TimeDisplayText sunsetString_astroTime2 = utils.calendarTimeShortDisplayString(context, dataset.dataAstro.sunsetCalendarOther());

        if (dataset.isCalculated())
        {
            txt_sunrise_actual.setText(sunriseString_actualTime.toString());
            txt_sunrise_civil.setText(sunriseString_civilTime.toString());
            txt_sunrise_nautical.setText(sunriseString_nauticalTime.toString());
            txt_sunrise_astro.setText(sunriseString_astroTime.toString());
            txt_solarnoon.setText(noonString.toString());
            txt_sunset_actual.setText(sunsetString_actualTime.toString());
            txt_sunset_civil.setText(sunsetString_civilTime.toString());
            txt_sunset_nautical.setText(sunsetString_nauticalTime.toString());
            txt_sunset_astro.setText(sunsetString_astroTime.toString());

            txt_sunrise2_actual.setText(sunriseString_actualTime2.toString());
            txt_sunrise2_civil.setText(sunriseString_civilTime2.toString());
            txt_sunrise2_nautical.setText(sunriseString_nauticalTime2.toString());
            txt_sunrise2_astro.setText(sunriseString_astroTime2.toString());
            txt_solarnoon2.setText(noonString2.toString());
            txt_sunset2_actual.setText(sunsetString_actualTime2.toString());
            txt_sunset2_civil.setText(sunsetString_civilTime2.toString());
            txt_sunset2_nautical.setText(sunsetString_nauticalTime2.toString());
            txt_sunset2_astro.setText(sunsetString_astroTime2.toString());

            SuntimesUtils.TimeDisplayText dayLengthDisplay = utils.timeDeltaLongDisplayString(0, dataset.dataActual.dayLengthToday());
            dayLengthDisplay.setSuffix("");
            txt_daylength.setText(dayLengthDisplay.toString());

            SuntimesUtils.TimeDisplayText lightLengthDisplay = utils.timeDeltaLongDisplayString(0, dataset.dataCivil.dayLengthToday());
            lightLengthDisplay.setSuffix("");
            txt_lightlength.setText(lightLengthDisplay.toString());

            SuntimesUtils.TimeDisplayText dayLengthDisplay2 = utils.timeDeltaLongDisplayString(0, dataset.dataActual.dayLengthOther());
            dayLengthDisplay2.setSuffix("");
            txt_daylength2.setText(dayLengthDisplay2.toString());

            SuntimesUtils.TimeDisplayText lightLengthDisplay2 = utils.timeDeltaLongDisplayString(0, dataset.dataCivil.dayLengthOther());
            lightLengthDisplay2.setSuffix("");
            txt_lightlength2.setText(lightLengthDisplay2.toString());

        } else {
            String notCalculated = getString(R.string.time_loading);
            txt_sunrise_actual.setText(notCalculated);
            txt_sunrise_civil.setText(notCalculated);
            txt_sunrise_nautical.setText(notCalculated);
            txt_sunrise_astro.setText(notCalculated);
            txt_solarnoon.setText(notCalculated);
            txt_sunset_actual.setText(notCalculated);
            txt_sunset_civil.setText(notCalculated);
            txt_sunset_nautical.setText(notCalculated);
            txt_sunset_astro.setText(notCalculated);

            txt_sunrise2_actual.setText(notCalculated);
            txt_sunrise2_civil.setText(notCalculated);
            txt_sunrise2_nautical.setText(notCalculated);
            txt_sunrise2_astro.setText(notCalculated);
            txt_solarnoon2.setText(notCalculated);
            txt_sunset2_actual.setText(notCalculated);
            txt_sunset2_civil.setText(notCalculated);
            txt_sunset2_nautical.setText(notCalculated);
            txt_sunset2_astro.setText(notCalculated);
        }

        //
        // clock & date
        //
        Date data_date = dataset.dataActual.date();
        Date data_date2 = dataset.dataActual.dateOther();

        //DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getApplicationContext());       // 4/11/2016
        DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(getApplicationContext());   // Apr 11, 2016
        //DateFormat dateFormat = android.text.format.DateFormat.getLongDateFormat(getApplicationContext());   // April 11, 2016

        String thisString = getString(R.string.today);
        String otherString = getString(R.string.tomorrow);

        if (dataset.dataActual.todayIsNotToday())
        {
            Calendar now = dataset.now();
            WidgetSettings.DateInfo nowInfo = new WidgetSettings.DateInfo(now);
            WidgetSettings.DateInfo dataInfo = new WidgetSettings.DateInfo(dataset.dataActual.calendar());
            if (!nowInfo.equals(dataInfo))
            {
                Date time = now.getTime();
                if (data_date.after(time))
                {
                    thisString = getString(R.string.future_today);
                    otherString = getString(R.string.future_tomorrow);

                } else if (data_date.before(time)) {
                    thisString = getString(R.string.past_today);
                    otherString = getString(R.string.past_tomorrow);
                    showDateWarning = true;
                }
            }
        }

        // date fields
        ImageSpan dateWarning = (showWarnings && showDateWarning) ? SuntimesUtils.createWarningSpan(this, txt_date.getTextSize()) : null;
        String dateString = getString(R.string.dateField, thisString, dateFormat.format(data_date));
        SpannableStringBuilder dateSpan = SuntimesUtils.createSpan(dateString, dateWarning);
        txt_date.setText(dateSpan);

        String date2String = getString(R.string.dateField, otherString, dateFormat.format(data_date2));
        SpannableStringBuilder date2Span = SuntimesUtils.createSpan(date2String, dateWarning);
        txt_date2.setText(date2Span);

        // timezone field
        TimeZone timezone = TimeZone.getTimeZone(dataset.timezone());
        int actualOffset = timezone.getOffset(dataset.date().getTime()) / (1000 * 60);                // actual timezone offset in minutes
        int roughOffset = (int)Math.round(dataset.location().getLongitudeAsDouble() * 24 * 60 / 360); // projected offset offset in minutes
        int offsetTolerance = 2 * 60;    // tolerance in minutes
        int offsetDiff = Math.abs(roughOffset - actualOffset);
        Log.d("DEBUG", "offsets: " + actualOffset + ", " + roughOffset );
        Log.d("DEBUG", "timezone offset difference: " +  offsetDiff +" [" + offsetTolerance + "]");

        showTimezoneWarning = (offsetDiff > offsetTolerance);
        //boolean showTimezoneWarning = (!dataset.timezone().equals(TimeZone.getDefault().getID()));

        ImageSpan timezoneWarning = (showWarnings && showTimezoneWarning) ? SuntimesUtils.createWarningSpan(this, txt_timezone.getTextSize()) : null;
        String timezoneString = getString(R.string.timezoneField, dataset.timezone());
        SpannableStringBuilder timezoneSpan = SuntimesUtils.createSpan(timezoneString, timezoneWarning);
        txt_timezone.setText(timezoneSpan);

        showDayLength(dataset.isCalculated());
        showNotes(dataset.isCalculated());
        showSnackWarnings();

        startTimeTask();
    }

    private void showSnackWarnings()
    {
        if (showWarnings && showTimezoneWarning)
        {
            timezoneWarning = createSnackTimezoneWarning();
            timezoneWarning.show();
            return;
        }

        if (showWarnings && showDateWarning)
        {
            dateWarning = createSnackDateWarning();
            dateWarning.show();
            return;
        }

        // no warnings shown; clear previous (stale) messages
        if (timezoneWarning != null && timezoneWarning.isShown())
        {
            timezoneWarning.dismiss();
            timezoneWarning = null;
        }
        if (dateWarning != null && dateWarning.isShown())
        {
            dateWarning.dismiss();
            dateWarning = null;
        }
    }

    private Snackbar createSnackDateWarning()
    {
        ImageSpan warningIcon = SuntimesUtils.createWarningSpan(this, txt_date.getTextSize());
        SpannableStringBuilder message = SuntimesUtils.createSpan(getString(R.string.dateWarning), warningIcon);
        Snackbar warningBar = Snackbar.make(card_flipper, message, Snackbar.LENGTH_INDEFINITE);

        warningBar.setAction(getString(R.string.configAction_setDate), new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                configDate();
            }
        });
        return warningBar;
    }

    private Snackbar createSnackTimezoneWarning()
    {
        ImageSpan warningIcon = SuntimesUtils.createWarningSpan(this, txt_date.getTextSize());
        SpannableStringBuilder message = SuntimesUtils.createSpan(getString(R.string.timezoneWarning), warningIcon);
        Snackbar warningBar = Snackbar.make(card_flipper, message, Snackbar.LENGTH_INDEFINITE);

        warningBar.setAction(getString(R.string.configAction_setTimeZone), new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                configTimeZone();
            }
        });
        return warningBar;
    }

    /**
     * Start updates to the clock ui.
     */
    private void startTimeTask()
    {
        txt_time.post(updateTimeTask);
    }

    /**
     * Stop updates to the clock ui.
     */
    private void stopTimeTask()
    {
        txt_time.removeCallbacks(updateTimeTask);
    }

    /**
     * Clock ui update rate; once every few seconds.
     */
    public static int UPDATE_RATE = 3000;

    /**
     * Update the clock ui at regular intervals to reflect current time (and note).
     */
    private Runnable updateTimeTask = new Runnable()
    {
        @Override
        public void run()
        {
            updateTimeViews(SuntimesActivity.this);
            txt_time.postDelayed(this, UPDATE_RATE);
        }
    };

    /**
     * Update the clock ui to reflect current time.
     * @param context the Activity context
     */
    protected void updateTimeViews(Context context)
    {
        Calendar now = dataset.now();
        SuntimesUtils.TimeDisplayText timeText = utils.calendarTimeShortDisplayString(this, now);
        txt_time.setText(timeText.getValue());
        txt_time_suffix.setText(timeText.getSuffix());
        notes.updateNote(context, now);
    }

    /**
     * onTouch swipe between the prev/next items in the view_flipper
     * @param event
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        return super.onTouchEvent(event);
    }

    /**
     * viewFlipper "note" onTouchListener; swipe between available notes
     */
    private View.OnTouchListener noteTouchListener = new View.OnTouchListener()
    {
        public int MOVE_SENSITIVITY = 25;
        public int FLING_SENSITIVITY = 10;
        public float firstTouchX, secondTouchX;

        @Override
        public boolean onTouch(View view, MotionEvent event)
        {
            switch (event.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                    firstTouchX = event.getX();
                    break;

                case MotionEvent.ACTION_UP:
                    secondTouchX = event.getX();
                    if ((firstTouchX - secondTouchX) >= FLING_SENSITIVITY)
                    {
                        userSwappedCard = false;
                        if (isRtl)
                            notes.showPrevNote();
                        else notes.showNextNote();    // swipe right: next

                    } else if ((secondTouchX - firstTouchX) > FLING_SENSITIVITY) {
                        userSwappedCard = false;
                        if (isRtl)
                            notes.showNextNote();
                        else notes.showPrevNote();   // swipe left: prev

                    } else {                    // click: user defined
                        AppSettings.ClockTapAction action = AppSettings.loadNoteTapActionPref(SuntimesActivity.this);
                        switch (action)
                        {
                            case NOTHING:
                                break;

                            case ALARM:
                                scheduleAlarmFromNote();
                                break;

                            case PREV_NOTE:
                                userSwappedCard = false;
                                notes.showPrevNote();
                                break;

                            case NEXT_NOTE:
                            default:
                                userSwappedCard = false;
                                notes.showNextNote();
                                break;
                        }
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    final View currentView = note_flipper.getCurrentView();
                    int moveDeltaX = (isRtl ? (int)(firstTouchX - event.getX()) : (int)(event.getX() - firstTouchX));
                    if (Math.abs(moveDeltaX) < MOVE_SENSITIVITY)
                    {
                        currentView.layout(moveDeltaX, currentView.getTop(), currentView.getWidth(), currentView.getBottom());
                    }
                    break;
            }
            return false;
        }
    };

    /**
     * viewFlipper "time card" onTouchListener; swipe left/right between viewflipper layouts (today/tomorrow)
     */
    private View.OnTouchListener timeCardTouchListener = new View.OnTouchListener()
    {
        public int MOVE_SENSITIVITY = 150;
        public int FLING_SENSITIVITY = 25;
        public float firstTouchX, secondTouchX;

        @Override
        public boolean onTouch(View view, MotionEvent event)
        {
            switch (event.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                    firstTouchX = event.getX();
                    break;

                case MotionEvent.ACTION_UP:
                    secondTouchX = event.getX();
                    if ((secondTouchX - firstTouchX) > FLING_SENSITIVITY)
                    {   // swipe right; back to previous view
                        userSwappedCard = (isRtl ? showNextCard() : showPreviousCard());

                    } else if (firstTouchX - secondTouchX > FLING_SENSITIVITY) {
                        // swipe left; advance to next view
                        userSwappedCard = (isRtl ? showPreviousCard() : showNextCard());

                    } else {
                        // swipe cancel; reset current view
                        final View currentView = card_flipper.getCurrentView();
                        currentView.layout(0, currentView.getTop(), currentView.getWidth(), currentView.getBottom());
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    float currentTouchX = event.getX();
                    int moveDelta = (int)(currentTouchX - firstTouchX);
                    boolean isSwipeRight = (moveDelta > 0);

                    final View currentView = card_flipper.getCurrentView();
                    int currentIndex = card_flipper.getDisplayedChild();

                    int otherIndex;
                    if (isRtl)
                    {
                        otherIndex = (isSwipeRight ? currentIndex + 1 : currentIndex - 1);
                    } else {
                        otherIndex = (isSwipeRight ? currentIndex - 1 : currentIndex + 1);
                    }

                    if (otherIndex >= 0 && otherIndex < card_flipper.getChildCount())
                    {
                        // in-between child views; flip between them
                        currentView.layout( moveDelta, currentView.getTop(),
                                moveDelta + currentView.getWidth(), currentView.getBottom() );

                        // extended movement; manually trigger swipe/fling
                        if (moveDelta > MOVE_SENSITIVITY || moveDelta < MOVE_SENSITIVITY * -1)
                        {
                            event.setAction(MotionEvent.ACTION_UP);
                            return onTouch(view, event);
                        }

                    } else {
                        // at-a-boundary (the first/last view);
                        // TODO: animate somehow to let user know there aren't additional views
                    }

                    break;
            }

            return true;
        }
    };

    /**
     * Show the 'next' set of data displayed by the main view_flipper.
     */
    public boolean showNextCard()
    {
        if (hasNextCard())
        {
            card_flipper.setOutAnimation(anim_card_outNext);
            card_flipper.setInAnimation(anim_card_inNext);
            card_flipper.showNext();
            return true;
        }
        return false;
    }

    public boolean hasNextCard()
    {
        int current = card_flipper.getDisplayedChild();
        return ((current + 1) < card_flipper.getChildCount());
    }

    /**
     * Show the 'previous' set of data displayed by the main view_flipper.
     */
    public boolean showPreviousCard()
    {
        if (hasPreviousCard())
        {
            card_flipper.setOutAnimation(anim_card_outPrev);
            card_flipper.setInAnimation(anim_card_inPrev);
            card_flipper.showPrevious();
            return true;
        }
        return false;
    }

    public boolean hasPreviousCard()
    {
        int current = card_flipper.getDisplayedChild();
        int prev = current - 1;
        return (prev >= 0);
    }

    View.OnClickListener onNextCardClick = new View.OnClickListener()
    {
        @Override
        public void onClick(View view)
        {
            userSwappedCard = showNextCard();
        }
    };

    View.OnClickListener onPrevCardClick = new View.OnClickListener()
    {
        @Override
        public void onClick(View view)
        {
            userSwappedCard = showPreviousCard();
        }
    };

    View.OnClickListener onNextNoteClick = new View.OnClickListener()
    {
        @Override
        public void onClick(View view)
        {
            userSwappedCard = false;
            notes.showNextNote();
        }
    };

    View.OnClickListener onPrevNoteClick = new View.OnClickListener()
    {
        @Override
        public void onClick(View view)
        {
            userSwappedCard = false;
            notes.showPrevNote();
        }
    };

    View.OnClickListener onClockClick = new View.OnClickListener()
    {
        @Override
        public void onClick(View view)
        {
            AppSettings.ClockTapAction action = AppSettings.loadClockTapActionPref(SuntimesActivity.this);
            if (action == AppSettings.ClockTapAction.NOTHING)
            {
                return;
            }

            if (action == AppSettings.ClockTapAction.ALARM)
            {
                scheduleAlarm();
                return;
            }

            if (action == AppSettings.ClockTapAction.NEXT_NOTE)
            {
                userSwappedCard = false;
                notes.showNextNote();
                return;
            }

            if (action == AppSettings.ClockTapAction.PREV_NOTE)
            {
                userSwappedCard = false;
                notes.showPrevNote();
                return;
            }

            Log.w("SuntimesActivity", "Unrecognized ClockTapAction (so doing nothing)" );
        }
    };

    protected void showDayLength( boolean value )
    {
        layout_daylength.setVisibility( (value ? View.VISIBLE : View.INVISIBLE) );
        layout_daylength2.setVisibility( (value ? View.VISIBLE : View.INVISIBLE) );
    }

    protected void showNotes( boolean value )
    {
        note_flipper.setVisibility( (value ? View.VISIBLE : View.INVISIBLE) );
    }

    /**
     * @param tomorrow
     * @return
     */
    private View.OnClickListener dateTapClickListener( final boolean tomorrow )
    {
        return new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                AppSettings.DateTapAction action = AppSettings.loadDateTapActionPref(SuntimesActivity.this);
                switch (action)
                {
                    case NOTHING:
                        break;

                    case CONFIG_DATE:
                        configDate();
                        break;

                    case SHOW_CALENDAR:
                        showCalendar();
                        break;

                    case SWAP_CARD:
                    default:
                        if (tomorrow)
                        {
                            userSwappedCard = showPreviousCard();
                        } else {
                            userSwappedCard = showNextCard();
                        }
                        break;
                }
            }
        };
    }

    private void initTimeFields()
    {
        /**for (SolarEvents.SolarEventField key : timeFields.keySet())
        {
            TextView field = timeFields.get(key);
            field.setOnClickListener(createTimeFieldClickListener(key));
        }*/
    }

    private View.OnClickListener createTimeFieldClickListener( final SolarEvents.SolarEventField event )
    {
        return new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Log.d("DEBUG", "TimeField clicked: " + event.toString());
                notes.showNote(event);
            }
        };
    }

    public void highlightTimeField( SolarEvents.SolarEventField highlightField )
    {
        int nextCardOffset = 0;
        int currentCard = this.card_flipper.getDisplayedChild();

        for (SolarEvents.SolarEventField field : timeFields.keySet())
        {
            TextView txtField = timeFields.get(field);
            if (txtField != null)
            {
                if (field.equals(highlightField))
                {
                    txtField.setTypeface(txtField.getTypeface(), Typeface.BOLD);
                    txtField.setPaintFlags(txtField.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

                    if (currentCard == 0 && field.tomorrow)
                    {
                        nextCardOffset = 1;

                    } else if (currentCard == 1 && !field.tomorrow) {
                        nextCardOffset = -1;
                    }

                } else {
                    txtField.setTypeface(Typeface.create(txtField.getTypeface(), Typeface.NORMAL), Typeface.NORMAL);
                    txtField.setPaintFlags(txtField.getPaintFlags() & (~Paint.UNDERLINE_TEXT_FLAG));
                }
            }
        }

        if (!userSwappedCard)
        {
            if (nextCardOffset > 0)
            {
                showNextCard();

            } else if (nextCardOffset < 0) {
                showPreviousCard();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void showCalendar()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        {
            long startMillis = dataset.now().getTimeInMillis();
            Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
            builder.appendPath("time");
            ContentUris.appendId(builder, startMillis);
            Intent intent = new Intent(Intent.ACTION_VIEW).setData(builder.build());
            startActivity(intent);
        }
    }

    private void adjustNoteIconSize(NoteData note, ImageView icon)
    {
        Resources resources = getResources();
        int iconWidth = (int)resources.getDimension(R.dimen.sunIconLarge_width);
        int iconHeight = ((note.noteIconResource == R.drawable.ic_noon_large) ? iconWidth : (int)resources.getDimension(R.dimen.sunIconLarge_height));

        ViewGroup.LayoutParams iconParams = icon.getLayoutParams();
        iconParams.width = iconWidth;
        iconParams.height = iconHeight;
    }

    protected void updateNoteUI( NoteData note, int transition )
    {
        if (note_flipper.getDisplayedChild() == 0)
        {
            // currently using view1, ready view2
            ic_time2_note.setBackgroundResource(note.noteIconResource);
            adjustNoteIconSize(note, ic_time2_note);
            ic_time2_note.setVisibility(View.VISIBLE);
            txt_time2_note1.setText(note.timeText.toString());
            txt_time2_note2.setText(note.prefixText);
            txt_time2_note3.setText(note.noteText);
            txt_time2_note3.setTextColor(note.noteColor);

        } else {
            // currently using view2, ready view1
            ic_time1_note.setBackgroundResource(note.noteIconResource);
            adjustNoteIconSize(note, ic_time1_note);
            ic_time1_note.setVisibility(View.VISIBLE);
            txt_time1_note1.setText(note.timeText.toString());
            txt_time1_note2.setText(note.prefixText);
            txt_time1_note3.setText(note.noteText);
            txt_time1_note3.setTextColor(note.noteColor);
        }

        if (transition == NoteChangedListener.TRANSITION_NEXT)
        {
            note_flipper.setInAnimation(anim_note_inNext);
            note_flipper.setOutAnimation(anim_note_outNext);
            note_flipper.showNext();

        } else {
            note_flipper.setInAnimation(anim_note_inPrev);
            note_flipper.setOutAnimation(anim_note_outPrev);
            note_flipper.showPrevious();
        }

        highlightTimeField(new SolarEvents.SolarEventField(note.noteMode, note.tomorrow));
    }


    /**
     * Stretch the horizontal rule to match the actual table width.. this is a hack to work around
     * unwanted stretching of the GridLayout columns when setting the hr to match_parent or fill_parent.
     */
    private void stretchTableRule()
    {
        LinearLayout[] cards = new LinearLayout[2];
        cards[0] = (LinearLayout)findViewById(R.id.info_time_all_today);
        cards[1] = (LinearLayout)findViewById(R.id.info_time_all_tomorrow);
        for (LinearLayout card : cards)                                        // for each card
        {
            View tableRule = card.findViewById(R.id.table_rule);
            if (tableRule != null)
            {
                LinearLayout[] cols = new LinearLayout[3];
                cols[0] = (LinearLayout) card.findViewById(R.id.table_head_date);
                cols[1] = (LinearLayout) card.findViewById(R.id.table_head_rise);
                cols[2] = (LinearLayout) card.findViewById(R.id.table_head_set);

                int tableWidth = 0;
                for (LinearLayout col : cols)                   // add up the measured column widths
                {
                    if (col != null)
                    {
                        col.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        tableWidth += col.getMeasuredWidth();
                    }
                }

                ViewGroup.LayoutParams tableRuleParams = tableRule.getLayoutParams();
                tableRuleParams.width = tableWidth;
                tableRule.setLayoutParams(tableRuleParams);    // and adjust the horizontal rule width
            }
        }
    }

}
