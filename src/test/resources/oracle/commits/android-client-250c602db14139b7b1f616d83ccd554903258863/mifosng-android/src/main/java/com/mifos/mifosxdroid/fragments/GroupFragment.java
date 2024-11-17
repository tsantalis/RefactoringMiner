/*
 * This project is licensed under the open source MPL V2.
 * See https://github.com/openMF/android-client/blob/master/LICENSE.md
 */

package com.mifos.mifosxdroid.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mifos.mifosxdroid.ClientActivity;
import com.mifos.mifosxdroid.OfflineCenterInputActivity;
import com.mifos.mifosxdroid.R;
import com.mifos.mifosxdroid.adapters.MifosGroupListAdapter;
import com.mifos.objects.db.MeetingCenter;
import com.mifos.objects.db.MifosGroup;
import com.mifos.services.RepaymentTransactionSyncService;
import com.orm.query.Condition;
import com.orm.query.Select;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class GroupFragment extends Fragment implements AdapterView.OnItemClickListener, RepaymentTransactionSyncService.SyncFinishListener {

    public static final String TAG = "Group Fragment";
    private final List<MifosGroup> groupList = new ArrayList<MifosGroup>();
    @InjectView(R.id.lv_group)
    ListView lv_group;
    @InjectView(R.id.progress_group)
    ProgressBar progressGroup;
    MifosGroupListAdapter adapter = null;
    View view;
    @InjectView(R.id.tv_empty_group)
    TextView tv_empty_group;
    private MenuItem syncItem;
    private String date;
    private long centerId;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_group, null);
        init();
        setHasOptionsMenu(true);
        ButterKnife.inject(this, view);

        return view;
    }

    private void init() {
        centerId = getActivity().getIntent().getLongExtra(CenterListFragment.CENTER_ID, -1);
    }
    @Override
    public void onResume() {
        super.onResume();
        setAdapter();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_sync, menu);
        try {
            syncItem = menu.findItem(R.id.action_sync);
            if (centerId != -1) {
                List<MeetingCenter> center = new ArrayList<MeetingCenter>();
                center.addAll(Select.from(MeetingCenter.class).where(Condition.prop("center_id").eq(centerId)).list());
                if (center.size() > 0 && center.get(0).getIsSynced() == 1)
                    syncItem.setEnabled(false);
            }
        } catch (IndexOutOfBoundsException e){
            e.printStackTrace();
            syncItem.setEnabled(false);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.action_sync) {
            LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View syncProgress = inflater.inflate(R.layout.sync_progress, null);
            MenuItemCompat.setActionView(item, syncProgress);
            if (centerId != -1) {
                RepaymentTransactionSyncService syncService = new RepaymentTransactionSyncService(this, centerId);
                syncService.syncRepayments(getActivity());
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void setAdapter() {
        groupList.clear();
        groupList.addAll(getAllGroups());
        if (adapter == null) {
            adapter = new MifosGroupListAdapter(getActivity(), groupList);
            lv_group.setAdapter(adapter);
        }
        lv_group.setOnItemClickListener(this);
        lv_group.setEmptyView(progressGroup);
        adapter.notifyDataSetChanged();
        if (groupList.size() == 0) {
            if (syncItem != null)
                MenuItemCompat.setActionView(syncItem, null);

            SharedPreferences preferences = getActivity().getSharedPreferences(OfflineCenterInputActivity.PREF_CENTER_DETAILS, Context.MODE_PRIVATE);
            date = preferences.getString(OfflineCenterInputActivity.TRANSACTION_DATE_KEY, null);
            tv_empty_group.setVisibility(View.VISIBLE);
            tv_empty_group.setText("There is no data for center " + centerId + " on " + date);
            progressGroup.setVisibility(View.GONE);

        } else
            tv_empty_group.setVisibility(View.GONE);
    }

    private List<MifosGroup> getAllGroups() {
        return Select.from(MifosGroup.class).where(Condition.prop("center_id").eq(centerId)).list();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        Intent intent = new Intent(getActivity(), ClientActivity.class);
        intent.putExtra("group_id", groupList.get(i).getId());
        Log.i(TAG, "onItemClick = Group ID:" + groupList.get(i).getId());
        startActivity(intent);
    }

    @Override
    public void onSyncFinish(String message, boolean isSyncable) {
        MenuItemCompat.setActionView(syncItem, null);
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
        if (isSyncable) {
            setCenterAsSynced();
        }
    }

    private void setCenterAsSynced() {
        if (centerId != -1) {
            List<MeetingCenter> center = Select.from(MeetingCenter.class).where(com.orm.query.Condition.prop("center_id").eq(centerId)).list();
            center.get(0).setIsSynced(1);
            center.get(0).save();
            getActivity().finish();
        }
    }
}
