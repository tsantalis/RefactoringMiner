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
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.widget.Toast;

import com.forrestguice.suntimeswidget.calculator.SuntimesCalculatorDescriptor;
import com.forrestguice.suntimeswidget.getfix.ClearPlacesTask;
import com.forrestguice.suntimeswidget.getfix.ExportPlacesTask;
import com.forrestguice.suntimeswidget.settings.AppSettings;
import com.forrestguice.suntimeswidget.settings.WidgetSettings;

import java.io.File;
import java.util.List;

/**
 * A preferences activity for the main app;
 * @see SuntimesConfigActivity for widget configuration.
 */
public class SuntimesSettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    final static String ACTION_PREFS_GENERAL = "com.forrestguice.suntimeswidget.PREFS_GENERAL";
    final static String ACTION_PREFS_LOCALE = "com.forrestguice.suntimeswidget.PREFS_LOCALE";
    final static String ACTION_PREFS_UI = "com.forrestguice.suntimeswidget.PREFS_UI";
    final static String ACTION_PREFS_WIDGETLIST = "com.forrestguice.suntimeswidget.PREFS_WIDGETLIST";
    final static String ACTION_PREFS_PLACES = "com.forrestguice.suntimeswidget.PREFS_PLACES";

    private Context context;
    private PlacesPrefsBase placesPrefBase = null;

    public SuntimesSettingsActivity()
    {
        super();
    }

    @Override
    public void onCreate(Bundle icicle)
    {
        context = SuntimesSettingsActivity.this;
        setTheme(AppSettings.loadTheme(this));
        super.onCreate(icicle);
        initLocale();
        initLegacyPrefs();
    }

    /**
     * legacy pref api used for pre honeycomb devices, while honeycomb+ uses the fragment based api.
     */
    private void initLegacyPrefs()
    {
        String action = getIntent().getAction();
        if (action != null)
        {
            if (action.equals(ACTION_PREFS_GENERAL))
            {
                //noinspection deprecation
                addPreferencesFromResource(R.xml.preference_general);
                initPref_general();

            } else if (action.equals(ACTION_PREFS_LOCALE)) {
                //noinspection deprecation
                addPreferencesFromResource(R.xml.preference_locale);
                initPref_locale();

            } else if (action.equals(ACTION_PREFS_UI)) {
                //noinspection deprecation
                addPreferencesFromResource(R.xml.preference_userinterface);
                initPref_ui();

            } else if (action.equals(ACTION_PREFS_PLACES)) {
                //noinspection deprecation
                addPreferencesFromResource(R.xml.preference_places);
                initPref_places();

            } else if (action.equals(ACTION_PREFS_WIDGETLIST)) {
                Intent intent = new Intent(this, SuntimesWidgetListActivity.class);
                startActivity(intent);
                finish();

            } else {
                Log.w("initLegacyPrefs", "unhandled action: " + action);
            }

        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            //noinspection deprecation
            addPreferencesFromResource(R.xml.preference_headers_legacy);
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        initLocale();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.registerOnSharedPreferenceChangeListener(this);

        if (placesPrefBase != null)
        {
            placesPrefBase.onResume();
        }
    }

    @Override
    public void onPause()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onStop()
    {
        super.onStop();
        if (placesPrefBase != null)
        {
            placesPrefBase.onStop();
        }
    }

    private void initLocale()
    {
        boolean localeChanged = AppSettings.initLocale(this);
        WidgetSettings.initDefaults(context);

        AppSettings.initDisplayStrings(context);
        WidgetSettings.initDisplayStrings(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && localeChanged)
        {
            invalidateHeaders();
        }
    }

    /**
     * @param target
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
        {
            loadHeadersFromResource(R.xml.preference_headers, target);

            TypedValue typedValue = new TypedValue();   // force styled icons on headers
            int[] icActionAttr = new int[] { R.attr.icActionSettings };
            TypedArray a = obtainStyledAttributes(typedValue.data, icActionAttr);
            int settingsIcon = a.getResourceId(0, R.drawable.ic_action_settings);
            a.recycle();

            for (Header header : target)
            {
                if (header.iconRes == 0)
                {
                    header.iconRes = settingsIcon;
                }
            }
        }
    }

    /**
     * @param fragmentName
     * @return
     */
    @Override
    protected boolean isValidFragment(String fragmentName)
    {
        return GeneralPrefsFragment.class.getName().equals(fragmentName) ||
               LocalePrefsFragment.class.getName().equals(fragmentName) ||
               UIPrefsFragment.class.getName().equals(fragmentName) ||
               PlacesPrefsFragment.class.getName().equals(fragmentName);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        if (key.equals(AppSettings.PREF_KEY_LOCALE) || key.equals(AppSettings.PREF_KEY_LOCALE_MODE))
        {
            Log.d("SettingsActivity", "Locale change detected; restarting activity");
            AppSettings.initLocale(this);
            SuntimesWidget.updateWidgets(this);
            SuntimesWidget1.updateWidgets(this);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            {
                invalidateHeaders();
                recreate();

            } else {
                finish();
                startActivity(getIntent());
            }
        }
    }

    //////////////////////////////////////////////////
    //////////////////////////////////////////////////

    /**
     * General Prefs
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPrefsFragment extends PreferenceFragment
    {
        private ListPreference calculatorPref;

        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            AppSettings.initLocale(getActivity());
            Log.i("GeneralPrefsFragment", "Arguments: " + getArguments());

            PreferenceManager.setDefaultValues(getActivity(), R.xml.preference_general, false);
            addPreferencesFromResource(R.xml.preference_general);

            calculatorPref = (ListPreference) findPreference("appwidget_0_general_calculator");
            initPref_general(GeneralPrefsFragment.this);
        }

        @Override
        @TargetApi(23)
        public void onAttach(Context context)
        {
            super.onAttach(context);
            loadPref_general(context, calculatorPref);
        }

        @Override
        public void onAttach(Activity activity)
        {
            super.onAttach(activity);
            loadPref_general(activity, calculatorPref);
        }
    }

    /**
     * init legacy prefs
     */
    private void initPref_general()
    {
        //String key = AppSettings.PREF_KEY_GETFIX_MAXAGE;
        //ListPreference gpsMaxAgePref = (ListPreference)findPreference(key);

        //key = AppSettings.PREF_KEY_GETFIX_MAXELAPSED;
        //ListPreference gpsMaxElapsedPref = (ListPreference)findPreference(key);

        String key = WidgetSettings.PREF_PREFIX_KEY + "0" + WidgetSettings.PREF_PREFIX_KEY_GENERAL + WidgetSettings.PREF_KEY_GENERAL_CALCULATOR;
        ListPreference calculatorPref = (ListPreference)findPreference(key);
        if (calculatorPref != null)
        {
            initPref_general(calculatorPref);
            loadPref_general(this, calculatorPref);
        }
    }
    private static void initPref_general(PreferenceFragment fragment)
    {
        String key = WidgetSettings.PREF_PREFIX_KEY + "0" + WidgetSettings.PREF_PREFIX_KEY_GENERAL + WidgetSettings.PREF_KEY_GENERAL_CALCULATOR;
        ListPreference calculatorPref = (ListPreference) fragment.findPreference(key);
        if (calculatorPref != null)
        {
            initPref_general(calculatorPref);
            loadPref_general(fragment.getActivity(), calculatorPref);
        }
    }
    private static void initPref_general(ListPreference calculatorPref)
    {
        SuntimesCalculatorDescriptor[] calculators = SuntimesCalculatorDescriptor.values();
        String[] calculatorEntries = new String[calculators.length];
        String[] calculatorValues = new String[calculators.length];

        int i = 0;
        for (SuntimesCalculatorDescriptor calculator : calculators)
        {
            calculatorEntries[i] = calculatorValues[i] = calculator.name();
            i++;
        }

        calculatorPref.setEntries(calculatorEntries);
        calculatorPref.setEntryValues(calculatorValues);
    }
    private static void loadPref_general(Context context, ListPreference calculatorPref)
    {
        if (context != null && calculatorPref != null)
        {
            SuntimesCalculatorDescriptor currentMode = WidgetSettings.loadCalculatorModePref(context, 0);
            int currentIndex = calculatorPref.findIndexOfValue(currentMode.name());
            calculatorPref.setValueIndex(currentIndex);
            //Log.d("SuntimesSettings", "current mode: " + currentMode + " (" + currentIndex + ")");
        }
    }

    //////////////////////////////////////////////////
    //////////////////////////////////////////////////

    /**
     * Locale Prefs
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class LocalePrefsFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            AppSettings.initLocale(getActivity());
            Log.i("LocalePrefsFragment", "Arguments: " + getArguments());

            PreferenceManager.setDefaultValues(getActivity(), R.xml.preference_locale, false);
            addPreferencesFromResource(R.xml.preference_locale);

            initPref_locale(LocalePrefsFragment.this);
        }
    }

    /**
     * init legacy prefs
     */
    private void initPref_locale()
    {
        //String key = AppSettings.PREF_KEY_LOCALE_MODE;
        //ListPreference modePref = (ListPreference)findPreference(key);
        //legacyPrefs.put(key, new LegacyListPref(modePref));

        String key = AppSettings.PREF_KEY_LOCALE;
        ListPreference localePref = (ListPreference)findPreference(key);
        //legacyPrefs.put(key, new LegacyListPref(localePref));

        initPref_locale(this, localePref);
    }
    private static void initPref_locale(PreferenceFragment fragment)
    {
        Preference localePref = (Preference)fragment.findPreference(AppSettings.PREF_KEY_LOCALE);
        initPref_locale(fragment.getActivity(), localePref);
    }
    private static void initPref_locale(Activity activity, Preference localePref)
    {
        AppSettings.LocaleMode localeMode = AppSettings.loadLocaleModePref(activity);
        localePref.setEnabled(localeMode == AppSettings.LocaleMode.CUSTOM_LOCALE);
    }

    //////////////////////////////////////////////////
    //////////////////////////////////////////////////

    /**
     * Places Prefs
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class PlacesPrefsFragment extends PreferenceFragment
    {
        private PlacesPrefsBase base;

        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            AppSettings.initLocale(getActivity());
            Log.i("PlacesPrefsFragment", "Arguments: " + getArguments());
            setRetainInstance(true);

            PreferenceManager.setDefaultValues(getActivity(), R.xml.preference_places, false);
            addPreferencesFromResource(R.xml.preference_places);

            Preference clearPlacesPref = (Preference)findPreference("places_clear");
            Preference exportPlacesPref = (Preference)findPreference("places_export");
            base = new PlacesPrefsBase(getActivity(), clearPlacesPref, exportPlacesPref);
        }

        @Override
        public void onStop()
        {
            super.onStop();
            base.onStop();
        }

        @Override
        public void onResume()
        {
            super.onResume();
            base.onResume();
        }

        @Override
        @TargetApi(23)
        public void onAttach(Context context)
        {
            super.onAttach(context);
            if (base != null)
            {
                base.setParent(context);
            }
        }

        @Override
        public void onAttach(Activity activity)
        {
            super.onAttach(activity);
            if (base != null)
            {
                base.setParent(activity);
            }
        }
    }

    /**
     * Places Prefs - Base
     */
    private static class PlacesPrefsBase
    {
        public static final String KEY_ISCLEARING = "isclearing";
        public static final String KEY_ISEXPORTING = "isexporting";

        private Context myParent;
        private ProgressDialog progress;

        private ClearPlacesTask clearPlacesTask = null;
        private boolean isClearing = false;

        private ExportPlacesTask exportPlacesTask = null;
        private boolean isExporting = false;

        public PlacesPrefsBase(Context context, Preference clearPref, Preference exportPref)
        {
            myParent = context;
            clearPref.setOnPreferenceClickListener(onClickClearPlaces);
            exportPref.setOnPreferenceClickListener(onClickExportPlaces);
        }

        public void setParent( Context context )
        {
            myParent = context;
        }

        public void showProgressClearing()
        {
            progress = ProgressDialog.show(myParent, myParent.getString(R.string.locationcleared_dialog_title), myParent.getString(R.string.locationcleared_dialog_message), true);
        }

        public void showProgressExporting()
        {
            progress = ProgressDialog.show(myParent, myParent.getString(R.string.locationexport_dialog_title), myParent.getString(R.string.locationexport_dialog_message), true);
        }

        public void dismissProgress()
        {
            if (progress != null && progress.isShowing())
            {
                progress.dismiss();
            }
        }

        /**
         * Export Places (click handler)
         */
        private Preference.OnPreferenceClickListener onClickExportPlaces = new Preference.OnPreferenceClickListener()
        {
            public boolean onPreferenceClick(Preference preference)
            {
                if (myParent != null)
                {
                    exportPlacesTask = new ExportPlacesTask(myParent, "SuntimesPlaces", true, true);  // export to external cache
                    exportPlacesTask.setTaskListener(exportPlacesListener);
                    exportPlacesTask.execute();
                    return true;
                }
                return false;
            }
        };

        /**
         * Export Places (task handler)
         */
        private ExportPlacesTask.TaskListener exportPlacesListener = new ExportPlacesTask.TaskListener()
        {
            @Override
            public void onStarted()
            {
                isExporting = true;
                showProgressExporting();
            }

            @Override
            public void onFinished(ExportPlacesTask.ExportResult results)
            {
                exportPlacesTask = null;
                isExporting = false;
                dismissProgress();

                if (results.getResult())
                {
                    String successMessage = myParent.getString(R.string.msg_export_success, results.getExportFile().getAbsolutePath());
                    Toast.makeText(myParent.getApplicationContext(), successMessage, Toast.LENGTH_LONG).show();

                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND);
                    shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(results.getExportFile()));
                    shareIntent.setType("text/csv");
                    myParent.startActivity(Intent.createChooser(shareIntent, myParent.getResources().getText(R.string.msg_export_to)));

                } else {
                    File file = results.getExportFile();
                    String path = ((file != null) ? file.getAbsolutePath() : "<path>");
                    String failureMessage = myParent.getString(R.string.msg_export_failure, path);
                    Toast.makeText(myParent.getApplicationContext(), failureMessage, Toast.LENGTH_LONG).show();
                }
            }
        };

        /**
         * Clear Places (click handler)
         */
        private Preference.OnPreferenceClickListener onClickClearPlaces = new Preference.OnPreferenceClickListener()
        {
            public boolean onPreferenceClick(Preference preference)
            {
                if (myParent != null)
                {
                    AlertDialog.Builder confirm = new AlertDialog.Builder(myParent)
                            .setTitle(myParent.getString(R.string.locationclear_dialog_title))
                            .setMessage(myParent.getString(R.string.locationclear_dialog_message))
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(myParent.getString(R.string.locationclear_dialog_ok), new DialogInterface.OnClickListener()
                            {
                                public void onClick(DialogInterface dialog, int whichButton)
                                {
                                    clearPlacesTask = new ClearPlacesTask(myParent);
                                    clearPlacesTask.setTaskListener(clearPlacesListener);
                                    clearPlacesTask.execute((Object[]) null);
                                }
                            })
                            .setNegativeButton(myParent.getString(R.string.locationclear_dialog_cancel), null);

                    confirm.show();
                    return true;
                }
                return false;
            }
        };

        /**
         * Clear Places (task handler)
         */
        private ClearPlacesTask.TaskListener clearPlacesListener = new ClearPlacesTask.TaskListener()
        {
            @Override
            public void onStarted()
            {
                isClearing = true;
                showProgressClearing();
            }

            @Override
            public void onFinished(Boolean result)
            {
                clearPlacesTask = null;
                isClearing = false;
                dismissProgress();
                Toast.makeText(myParent, myParent.getString(R.string.locationcleared_toast_success), Toast.LENGTH_LONG).show();
            }
        };

        private void onStop()
        {
            if (isClearing && clearPlacesTask != null)
            {
                clearPlacesTask.pauseTask();
                clearPlacesTask.clearTaskListener();
            }

            if (isExporting && exportPlacesTask != null)
            {
                exportPlacesTask.pauseTask();
                exportPlacesTask.clearTaskListener();
            }

            dismissProgress();
        }

        private void onResume()
        {

            if (isClearing && clearPlacesTask != null && clearPlacesTask.isPaused())
            {
                clearPlacesTask.setTaskListener(clearPlacesListener);
                showProgressClearing();
                clearPlacesTask.resumeTask();
            }

            if (isExporting && exportPlacesTask != null)
            {
                exportPlacesTask.setTaskListener(exportPlacesListener);
                showProgressExporting();
                exportPlacesTask.resumeTask();
            }
        }
    }

    /**
     * init legacy prefs
     */
    private void initPref_places()
    {
        Preference clearPlacesPref = (Preference)findPreference("places_clear");
        Preference exportPlacesPref = (Preference)findPreference("places_export");
        placesPrefBase = new PlacesPrefsBase(this, clearPlacesPref, exportPlacesPref);
    }

    //////////////////////////////////////////////////
    //////////////////////////////////////////////////

    /**
     * User Interface Prefs
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class UIPrefsFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            AppSettings.initLocale(getActivity());
            Log.i("UIPrefsFragment", "Arguments: " + getArguments());

            PreferenceManager.setDefaultValues(getActivity(), R.xml.preference_userinterface, false);
            addPreferencesFromResource(R.xml.preference_userinterface);
        }
    }

    /**
     * init legacy prefs
     */
    private void initPref_ui()
    {
        //String key = AppSettings.PREF_KEY_APPEARANCE_THEME;
        //ListPreference themePref = (ListPreference)findPreference(key);

        //key = AppSettings.PREF_KEY_UI_CLOCKTAPACTION;
        //ListPreference clockTapPref = (ListPreference)findPreference(key);

        //key = AppSettings.PREF_KEY_UI_DATETAPACTION;
        //ListPreference dateTapPref = (ListPreference)findPreference(key);

        //key = AppSettings.PREF_KEY_UI_NOTETAPACTION;
        //ListPreference noteTapPref = (ListPreference)findPreference(key);
    }

}
