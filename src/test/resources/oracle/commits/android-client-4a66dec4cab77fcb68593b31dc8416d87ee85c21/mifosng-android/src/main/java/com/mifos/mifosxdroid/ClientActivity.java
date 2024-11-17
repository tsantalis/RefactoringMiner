/*
 * This project is licensed under the open source MPL V2.
 * See https://github.com/openMF/android-client/blob/master/LICENSE.md
 */

package com.mifos.mifosxdroid;

import android.content.Intent;
import android.os.Bundle;

import com.mifos.mifosxdroid.core.MifosBaseActivity;
import com.mifos.mifosxdroid.fragments.ClientFragment;


public class ClientActivity extends MifosBaseActivity {

    private long groupId;
    private Intent intentForExtras;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toolbar_container);
        init();
        setFragment();
    }

    private void init() {
        intentForExtras = getIntent();
        groupId = intentForExtras.getLongExtra("group_id", 0);
    }

    private void setFragment() {
        ClientFragment fragment = new ClientFragment();
        Bundle arguments = new Bundle();
        arguments.putLong("group_id", groupId);
        fragment.setArguments(arguments);
        replaceFragment(fragment, false, R.id.container);
    }
}