/*
 * This project is licensed under the open source MPL V2.
 * See https://github.com/openMF/android-client/blob/master/LICENSE.md
 */

package com.mifos.mifosxdroid.online;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.mifos.mifosxdroid.R;
import com.mifos.objects.client.Client;
import com.mifos.utils.FragmentConstants;

import java.util.List;

import butterknife.ButterKnife;

public class CentersActivity extends ActionBarActivity
        implements CenterListFragment.OnFragmentInteractionListener,
                   GroupListFragment.OnFragmentInteractionListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_centers);
        ButterKnife.inject(this);

        FragmentTransaction fragmentTransaction =  getSupportFragmentManager().beginTransaction();
        CenterListFragment centerListFragment = new CenterListFragment();
        fragmentTransaction.replace(R.id.center_container, centerListFragment);
        fragmentTransaction.commit();

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.centers, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void loadGroupsOfCenter(int centerId) {

        GroupListFragment groupListFragment = GroupListFragment.newInstance(centerId);
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.addToBackStack(FragmentConstants.FRAG_CENTER_LIST);
        fragmentTransaction.replace(R.id.center_container, groupListFragment);
        fragmentTransaction.commit();

    }

    @Override
    public void loadCollectionSheetForCenter(int centerId, String collectionDate, int calenderInstanceId) {
        CollectionSheetFragment collectionSheetFragment = CollectionSheetFragment.newInstance(centerId, collectionDate, calenderInstanceId);
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.addToBackStack(FragmentConstants.FRAG_CENTER_LIST);
        fragmentTransaction.replace(R.id.center_container, collectionSheetFragment);
        fragmentTransaction.commit();

    }

    @Override
    public void loadClientsOfGroup(List<Client> clientList) {
        ClientListFragment clientListFragment = ClientListFragment.newInstance(clientList, true);
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.addToBackStack(FragmentConstants.FRAG_GROUP_LIST);
        fragmentTransaction.replace(R.id.center_container, clientListFragment);
        fragmentTransaction.commitAllowingStateLoss();
    }
}
