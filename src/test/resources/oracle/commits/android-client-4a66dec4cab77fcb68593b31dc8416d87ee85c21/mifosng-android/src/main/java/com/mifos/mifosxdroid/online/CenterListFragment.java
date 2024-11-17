/*
 * This project is licensed under the open source MPL V2.
 * See https://github.com/openMF/android-client/blob/master/LICENSE.md
 */

package com.mifos.mifosxdroid.online;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.mifos.mifosxdroid.R;
import com.mifos.mifosxdroid.adapters.CentersListAdapter;
import com.mifos.mifosxdroid.core.MifosBaseFragment;
import com.mifos.mifosxdroid.uihelpers.MFDatePicker;
import com.mifos.objects.group.Center;
import com.mifos.objects.group.CenterWithAssociations;
import com.mifos.utils.MifosApplication;

import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by ishankhanna on 11/03/14.
 */
public class CenterListFragment extends MifosBaseFragment {

    private static final String TAG = "CenterListFragment";

    private View rootView;
    private ListView lv_centers_list;
    private SharedPreferences sharedPreferences;
    private List<Center> centers;
    private CentersListAdapter centersListAdapter;
    private OnFragmentInteractionListener mListener;

    public CenterListFragment(){

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_centers_list,container,false);


        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        setupUI();


        showProgress();
        ((MifosApplication)getActivity().getApplication()).api.centerService.getAllCenters(new Callback<List<Center>>() {
            @Override
            public void success(final List<Center> centers, Response response) {
                centersListAdapter = new CentersListAdapter(getActivity(), centers);

                lv_centers_list.setAdapter(centersListAdapter);

                lv_centers_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                        mListener.loadGroupsOfCenter(centers.get(i).getId());

                    }
                });

                lv_centers_list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {

                        showProgress();

                        ((MifosApplication)getActivity().getApplication()).api.centerService.getCenterWithGroupMembersAndCollectionMeetingCalendar(centers.get(position).getId(), new Callback<CenterWithAssociations>() {
                            @Override
                            public void success(final CenterWithAssociations centerWithAssociations, Response response) {

                                hideProgress();
                                MFDatePicker mfDatePicker = new MFDatePicker();
                                mfDatePicker.setOnDatePickListener(new MFDatePicker.OnDatePickListener() {
                                    @Override
                                    public void onDatePicked(String date) {

                                        mListener.loadCollectionSheetForCenter(centers.get(position).getId(), date, centerWithAssociations.getCollectionMeetingCalendar().getId());

                                    }
                                });
                                mfDatePicker.show(getActivity().getSupportFragmentManager(), MFDatePicker.TAG);

                            }

                            @Override
                            public void failure(RetrofitError retrofitError) {
                                hideProgress();
                                Toast.makeText(getActivity(), "Cannot Generate Collection Sheet, There was some problem!", Toast.LENGTH_SHORT).show();
                            }
                        });

                        return true;
                    }
                });
                hideProgress();
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                hideProgress();
            }
        });


        return rootView;
    }

    public void setupUI(){

        lv_centers_list = (ListView) rootView.findViewById(R.id.lv_center_list);

    }

    public interface OnFragmentInteractionListener {

        public void loadGroupsOfCenter(int centerId);
        public void loadCollectionSheetForCenter(int centerId, String collectionDate, int calenderInstanceId);

    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.mItem_search) {

            getActivity().finish();

        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

}