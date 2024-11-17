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

package com.forrestguice.suntimeswidget.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;

import com.forrestguice.suntimeswidget.R;
import com.forrestguice.suntimeswidget.calculator.SuntimesDataset;

import java.util.Locale;

public class AppSettings
{
    public static final String THEME_DARK = "dark";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DAYNIGHT = "daynight";

    public static final String PREF_KEY_APPEARANCE_THEME = "app_appearance_theme";
    public static final String PREF_DEF_APPEARANCE_THEME = THEME_DARK;

    public static final String PREF_KEY_LOCALE_MODE = "app_locale_mode";
    public static final LocaleMode PREF_DEF_LOCALE_MODE = LocaleMode.SYSTEM_LOCALE;

    public static final String PREF_KEY_LOCALE = "app_locale";
    public static final String PREF_DEF_LOCALE = "en";

    public static final String PREF_KEY_UI_DATETAPACTION = "app_ui_datetapaction";
    public static final DateTapAction PREF_DEF_UI_DATETAPACTION = DateTapAction.CONFIG_DATE;

    public static final String PREF_KEY_UI_CLOCKTAPACTION = "app_ui_clocktapaction";
    public static final ClockTapAction PREF_DEF_UI_CLOCKTAPACTION = ClockTapAction.ALARM;

    public static final String PREF_KEY_UI_NOTETAPACTION = "app_ui_notetapaction";
    public static final ClockTapAction PREF_DEF_UI_NOTETAPACTION = ClockTapAction.NEXT_NOTE;

    /**
     * Language modes (system, user defined)
     */
    public static enum LocaleMode
    {
        SYSTEM_LOCALE("System Locale"),
        CUSTOM_LOCALE("Custom Locale");

        private String displayString;

        private LocaleMode( String displayString )
        {
            this.displayString = displayString;
        }

        public String getDisplayString()
        {
            return displayString;
        }

        public void setDisplayString( String displayString )
        {
            this.displayString = displayString;
        }
        public static void initDisplayStrings( Context context )
        {
            String[] labels = context.getResources().getStringArray(R.array.localeMode_display);
            SYSTEM_LOCALE.setDisplayString(labels[0]);
            CUSTOM_LOCALE.setDisplayString(labels[1]);
        }
    }

    /**
     * Preference: locale mode
     */
    public static LocaleMode loadLocaleModePref( Context context )
    {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return loadLocaleModePref(pref);
    }

    public static LocaleMode loadLocaleModePref( SharedPreferences pref )
    {
        String modeString = pref.getString(PREF_KEY_LOCALE_MODE, PREF_DEF_LOCALE_MODE.name());

        LocaleMode localeMode;
        try {
            localeMode = LocaleMode.valueOf(modeString);

        } catch (IllegalArgumentException e) {
            localeMode = PREF_DEF_LOCALE_MODE;
        }
        return localeMode;
    }

    /**
     * Preference: custom locale
     */
    public static String loadLocalePref( Context context )
    {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getString(PREF_KEY_LOCALE, PREF_DEF_LOCALE);
    }

    /**
     * @return true if locale was changed by init, false otherwise
     */
    public static boolean initLocale( Context context )
    {
        AppSettings.LocaleMode localeMode = AppSettings.loadLocaleModePref(context);
        if (localeMode == AppSettings.LocaleMode.CUSTOM_LOCALE)
        {
            return AppSettings.loadLocale(context, AppSettings.loadLocalePref(context));

        } else {
            return resetLocale(context);
        }
    }

    /**
     * @return true if the locale was changed by reset, false otherwise
     */
    public static boolean resetLocale( Context context )
    {
        if (systemLocale != null)
        {
            Log.d("resetLocale", "locale reset to " + systemLocale);
            return loadLocale(context, systemLocale);
        }
        return false;
    }

    private static String systemLocale = null;  // null until locale is overridden w/ loadLocale
    public static String getSystemLocale()
    {
        if (systemLocale == null)
        {
            systemLocale = Locale.getDefault().getLanguage();
        }
        return systemLocale;
    }
    public static Locale getLocale()
    {
        return Locale.getDefault();
    }

    public static boolean loadLocale( Context context, String localeCode )
    {
        Resources resources = context.getApplicationContext().getResources();
        Configuration config = resources.getConfiguration();
        DisplayMetrics metrics = resources.getDisplayMetrics();

        if (systemLocale == null)
        {
            systemLocale = Locale.getDefault().getLanguage();
        }
        Locale customLocale = new Locale(localeCode);

        Locale.setDefault(customLocale);
        config.locale = customLocale;
        resources.updateConfiguration(config, metrics);

        Log.d("loadLocale", "locale loaded " + localeCode);
        return true;
    }

    /**
     * Is the current locale right-to-left?
     * @param context
     * @return true the locale is right-to-left, false the locale is left-to-right
     */
    public static boolean isLocaleRtl(Context context)
    {
        return context.getResources().getBoolean(R.bool.is_rtl);
    }

    /**
     * Actions that can be performed when the clock is clicked.
     */
    public static enum ClockTapAction
    {
        NOTHING("Do Nothing"),
        ALARM("Set an Alarm"),
        NEXT_NOTE("Show next note"),
        PREV_NOTE("Show previous note");

        private String displayString;

        private ClockTapAction(String displayString)
        {
            this.displayString = displayString;
        }

        public String toString()
        {
            return displayString;
        }

        public String getDisplayString()
        {
            return displayString;
        }

        public void setDisplayString( String displayString )
        {
            this.displayString = displayString;
        }

        public static void initDisplayStrings( Context context )
        {
            String[] labels = context.getResources().getStringArray(R.array.clockTapActions_display);
            NOTHING.setDisplayString(labels[0]);
            ALARM.setDisplayString(labels[1]);
            NEXT_NOTE.setDisplayString(labels[2]);
            PREV_NOTE.setDisplayString(labels[3]);
        }
    }

    /**
     * Actions that can be performed when the date field is clicked.
     */
    public static enum DateTapAction
    {
        NOTHING("Do Nothing"),
        SWAP_CARD("Swap Cards"),
        SHOW_CALENDAR("Show Calendar"),
        CONFIG_DATE("Set Custom Date");

        private String displayString;

        private DateTapAction(String displayString)
        {
            this.displayString = displayString;
        }

        public String toString()
        {
            return displayString;
        }

        public String getDisplayString()
        {
            return displayString;
        }

        public void setDisplayString( String displayString )
        {
            this.displayString = displayString;
        }

        public static void initDisplayStrings( Context context )
        {
            String[] labels = context.getResources().getStringArray(R.array.dateTapActions_display);
            NOTHING.setDisplayString(labels[0]);
            SWAP_CARD.setDisplayString(labels[1]);
            SHOW_CALENDAR.setDisplayString(labels[2]);
            CONFIG_DATE.setDisplayString(labels[3]);
        }
    }

    /**
     * Preference: the action that is performed when the clock ui is clicked/tapped
     */
    public static ClockTapAction loadClockTapActionPref( Context context )
    {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String modeString = pref.getString(PREF_KEY_UI_CLOCKTAPACTION, PREF_DEF_UI_CLOCKTAPACTION.name());

        ClockTapAction actionMode;
        try {
            actionMode = ClockTapAction.valueOf(modeString);

        } catch (IllegalArgumentException e) {
            actionMode = PREF_DEF_UI_CLOCKTAPACTION;
        }
        return actionMode;
    }

    /**
     * Preference: the action that is performed when the date field is clicked/tapped
     */
    public static DateTapAction loadDateTapActionPref( Context context )
    {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String modeString = pref.getString(PREF_KEY_UI_DATETAPACTION, PREF_DEF_UI_DATETAPACTION.name());

        DateTapAction actionMode;
        try {
            actionMode = DateTapAction.valueOf(modeString);

        } catch (IllegalArgumentException e) {
            actionMode = PREF_DEF_UI_DATETAPACTION;
        }
        return actionMode;
    }

    /**
     * Preference: the action that is performed when the note ui is clicked/tapped
     */
    public static ClockTapAction loadNoteTapActionPref( Context context )
    {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String modeString = pref.getString(PREF_KEY_UI_NOTETAPACTION, PREF_DEF_UI_NOTETAPACTION.name());
        ClockTapAction actionMode;

        try {
            actionMode = ClockTapAction.valueOf(modeString);

        } catch (IllegalArgumentException e) {
            actionMode = PREF_DEF_UI_NOTETAPACTION;
        }
        return actionMode;
    }

    /**
     * @param context an application context
     * @return a theme identifier
     */
    public static String loadThemePref(Context context)
    {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getString(PREF_KEY_APPEARANCE_THEME, PREF_DEF_APPEARANCE_THEME);
    }

    public static int loadTheme(Context context)
    {
        return loadTheme(context, null);
    }
    public static int loadTheme(Context context, SuntimesDataset dataset)
    {
        int styleID = R.style.AppTheme_Dark;
        String themeName = loadThemePref(context);
        if (themeName != null)
        {
            if (themeName.equals(THEME_LIGHT))
            {
                styleID = R.style.AppTheme_Light;

            } else if (themeName.equals(THEME_DARK)) {
                styleID = R.style.AppTheme_Dark;

            } else if (themeName.equals(THEME_DAYNIGHT)) {
                if (dataset != null)
                {
                    styleID = (dataset.isDay() ? R.style.AppTheme_Light : R.style.AppTheme_Dark);
                }
            }
        }
        return styleID;
    }

    public static void initDisplayStrings( Context context )
    {
        LocaleMode.initDisplayStrings(context);
        ClockTapAction.initDisplayStrings(context);
    }

}
