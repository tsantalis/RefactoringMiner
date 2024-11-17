/*
 * This project is licensed under the open source MPL V2.
 * See https://github.com/openMF/android-client/blob/master/LICENSE.md
 */

package com.mifos.mifosxdroid;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.crashlytics.android.Crashlytics;
import com.mifos.mifosxdroid.core.BaseActivity;
import com.mifos.mifosxdroid.online.DashboardFragmentActivity;
import com.mifos.objects.User;
import com.mifos.services.API;
import com.mifos.utils.Constants;
import com.mifos.utils.MifosApplication;


/**
 * This is the First Activity which can be used for initial checks, inits at app Startup
 */

public class SplashScreenActivity extends BaseActivity {

    private SharedPreferences sharedPreferences;
    private String authenticationToken;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Crashlytics.start(this);

        if (savedInstanceState == null) {
            replaceFragment(new PlaceholderFragment(), false, R.id.container);
        }

        context = SplashScreenActivity.this.getApplicationContext();
        Constants.applicationContext = getApplicationContext();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        authenticationToken = sharedPreferences.getString(User.AUTHENTICATION_KEY, "NA");

        /**
         * Authentication Token is checked,
         * if NA(Not Available) User will have to login
         * else User Redirected to Dashboard
         */
        if (authenticationToken.equals("NA")) {
            //if authentication key is not present
            startActivity(new Intent(SplashScreenActivity.this, LoginActivity.class));
        } else {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            String instanceURL = sharedPreferences.getString(Constants.INSTANCE_URL_KEY, null);
            String tenantIdentifier = sharedPreferences.getString(Constants.TENANT_IDENTIFIER_KEY, null);

            ((MifosApplication) getApplication()).api = new API(instanceURL, tenantIdentifier, false);
            //if authentication key is present open dashboard
            startActivity(new Intent(SplashScreenActivity.this, DashboardFragmentActivity.class));
        }

        finish();

    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_splash, container, false);
            return rootView;
        }
    }

}
