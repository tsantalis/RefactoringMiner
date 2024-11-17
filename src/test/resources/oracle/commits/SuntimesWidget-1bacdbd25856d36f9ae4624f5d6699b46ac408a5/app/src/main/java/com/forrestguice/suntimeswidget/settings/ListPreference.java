/**
    Copyright (C) 2016 Forrest Guice
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

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;

/**
 * A custom ListPreference to work around some bugs pre api 19.
 * @see "https://android.googlesource.com/platform/frameworks/base/+/94c02a1a1a6d7e6900e5a459e9cc699b9510e5a2"
 */
public class ListPreference extends android.preference.ListPreference
{
    public ListPreference(Context context)
    {
        super(context);
    }

    public ListPreference(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    @TargetApi(21)
    public ListPreference(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(21)
    public ListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * the bug: listpref summary doesn't update when value is changed (pre api 19)
     * this solution based on answer provided by "Dreaming in Code" @stackoverflow
     * :: from http://stackoverflow.com/a/21642401/4721910
     * :: based on https://android.googlesource.com/platform/frameworks/base/+/94c02a1a1a6d7e6900e5a459e9cc699b9510e5a2
     * @param value
     */
    @Override
    public void setValue(String value)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
            super.setValue(value);  // fixed in api 19 (kitkat); call through to super

        } else {
            boolean isChanged = !TextUtils.equals(getValue(), value);
            super.setValue(value);

            if (isChanged)  // pre api 19; we need to make the missing call to notifyChanged
            {
                notifyChanged();
            }
        }
    }

    /**
     * the bug: listpref doesn't format %s summary values (pre api 11)
     * @return
     */
    @Override
    public CharSequence getSummary()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
        {
            return super.getSummary();  // fixed in api 11 (honeycomb); call through to super

        } else {
            CharSequence summary = super.getSummary();
            if (summary != null)
            {
                CharSequence displayValue = getEntry();
                if (displayValue != null)    // pre api 11; we need to format the summary (%s)
                {
                    return String.format(summary.toString(), displayValue);
                }
            }
            return summary;
        }
    }
}
