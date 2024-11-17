/*
 * This project is licensed under the open source MPL V2.
 * See https://github.com/openMF/android-client/blob/master/LICENSE.md
 */

package com.mifos.mifosxdroid;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;

import com.mifos.mifosxdroid.fragments.ClientFragment;


public class ClientActivity extends ActionBarActivity
{
    private long groupId;
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
        groupId = intentForExtras.getLongExtra("group_id",0);
    }
    private void setFragment()
    {
        FragmentTransaction fragmentTransaction =  getSupportFragmentManager().beginTransaction();
        ClientFragment fragment = new ClientFragment();
        Bundle arguments = new Bundle();
        arguments.putLong("group_id",groupId);
        fragment.setArguments(arguments);
        fragmentTransaction.replace(R.id.global_container,fragment).commit();
    }
}