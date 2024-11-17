/*
 * This project is licensed under the open source MPL V2.
 * See https://github.com/openMF/android-client/blob/master/LICENSE.md
 */

package com.mifos.mifosxdroid;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;

import com.mifos.mifosxdroid.fragments.LoanFragment;


public class LoanActivity extends ActionBarActivity
{
    private int clientId;
    private Intent intentForExtras;
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_global_container_layout);
        init();
        setFragment();
    }
    private void init()
    {
        intentForExtras = getIntent();
        clientId = intentForExtras.getIntExtra("clientId",0);
    }
    private void setFragment()
    {
        FragmentTransaction fragmentTransaction =  getSupportFragmentManager().beginTransaction();
        LoanFragment fragment = new LoanFragment();
        Bundle arguments = new Bundle();
        arguments.putInt("clientId", clientId);
        fragment.setArguments(arguments);
        fragmentTransaction.replace(R.id.global_container,fragment).commit();
    }
}
