/*
 * This project is licensed under the open source MPL V2.
 * See https://github.com/openMF/android-client/blob/master/LICENSE.md
 */

package com.mifos.mifosxdroid.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;

import com.mifos.mifosxdroid.R;

/**
 * @author fomenkoo
 */
public class FontTextView extends TextView {

    public FontTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        parseAttributes(context, attrs);
    }

    public FontTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        parseAttributes(context, attrs);
    }

    public FontTextView(Context context) {
        super(context);
    }

    private void parseAttributes(Context context, AttributeSet attrs) {
        if (!isInEditMode()) {
            TypedArray values = context.obtainStyledAttributes(attrs, R.styleable.CustomFont);
            int typeface = values.getInt(R.styleable.CustomFont_typeface, Font.ROBOTO_MEDIUM.ordinal());
            Font font = Font.getFont(typeface);
            setTypeface(font.getTypeface());
        }
    }
}
