/*
 * This project is licensed under the open source MPL V2.
 * See https://github.com/openMF/android-client/blob/master/LICENSE.md
 */

package com.mifos.mifosxdroid.online;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.mifos.mifosxdroid.R;
import com.mifos.mifosxdroid.SplashScreenActivity;
import com.mifos.mifosxdroid.core.MifosBaseActivity;
import com.mifos.objects.User;
import com.mifos.utils.Constants;

/**
 * Logout activity.
 */
public class LogoutActivity extends MifosBaseActivity {
    public final static String TAG = LogoutActivity.class.getSimpleName();
    public static final String NA = "NA";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logout(this);
    }

    public void logout(Context context) {
        Log.d(TAG, "logout");
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(User.AUTHENTICATION_KEY, NA);
        editor.putString(Constants.INSTANCE_URL_KEY, getString(R.string.default_instance_url));
        editor.commit();
        editor.apply();
        startActivity(new Intent(LogoutActivity.this, SplashScreenActivity.class));
    }
}
