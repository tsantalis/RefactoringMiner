/*
 * This project is licensed under the open source MPL V2.
 * See https://github.com/openMF/android-client/blob/master/LICENSE.md
 */

package com.mifos.mifosxdroid;

import android.content.Intent;
import android.os.Bundle;

import com.mifos.mifosxdroid.core.MifosBaseActivity;
import com.mifos.mifosxdroid.fragments.LoanFragment;


public class LoanActivity extends MifosBaseActivity {
    private int clientId;
    private Intent intentForExtras;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toolbar_container);
        init();
        setFragment();
    }

    private void init() {
        intentForExtras = getIntent();
        clientId = intentForExtras.getIntExtra("clientId", 0);
    }

    private void setFragment() {
        LoanFragment fragment = new LoanFragment();
        Bundle arguments = new Bundle();
        arguments.putInt("clientId", clientId);
        fragment.setArguments(arguments);
        replaceFragment(fragment, false, R.id.container);
    }
}
