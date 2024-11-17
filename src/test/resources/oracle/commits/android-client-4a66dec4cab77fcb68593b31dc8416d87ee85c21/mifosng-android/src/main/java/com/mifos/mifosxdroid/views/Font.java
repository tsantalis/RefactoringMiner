/*
 * This project is licensed under the open source MPL V2.
 * See https://github.com/openMF/android-client/blob/master/LICENSE.md
 */

package com.mifos.mifosxdroid.views;

import android.graphics.Typeface;

import com.mifos.utils.MifosApplication;

import java.io.File;

/**
 * @author fomenkoo
 */
public enum Font {
    ROBOTO_LIGHT("fonts", "Roboto-Light.ttf"),
    ROBOTO_MEDIUM("fonts", "Roboto-Regular.ttf"),
    ROBOTO_BOLD("fonts", "Roboto-Bold.ttf");

    private String fontName;
    private Typeface typeface;

    Font(String pathToFont, String fontName) {
        this.fontName = fontName;
        typeface = MifosApplication.typefaceManager.get(fontName.hashCode());
        if (typeface == null) {
            typeface = Typeface.createFromAsset(MifosApplication.getSugarContext().getApplicationContext().getAssets(), pathToFont + File.separator + fontName);
            MifosApplication.typefaceManager.put(fontName.hashCode(), typeface);
        }
    }

    public Typeface getTypeface() {
        return typeface;
    }

    public String getFontName() {
        return fontName;
    }

    public static Font getFont(int typeface) {
        return values()[typeface];
    }
}
