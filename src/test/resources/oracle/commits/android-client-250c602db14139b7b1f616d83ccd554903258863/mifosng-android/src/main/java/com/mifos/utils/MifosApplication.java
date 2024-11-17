/*
 * This project is licensed under the open source MPL V2.
 * See https://github.com/openMF/android-client/blob/master/LICENSE.md
 */

package com.mifos.utils;

import android.graphics.Typeface;

import com.mifos.services.API;
import com.orm.SugarApp;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ishankhanna on 13/03/15.
 */
public class MifosApplication extends SugarApp {

    // Contains fonts to re-user
    public static final Map<Integer, Typeface> typefaceManager = new HashMap<>();

    public API api;

}
