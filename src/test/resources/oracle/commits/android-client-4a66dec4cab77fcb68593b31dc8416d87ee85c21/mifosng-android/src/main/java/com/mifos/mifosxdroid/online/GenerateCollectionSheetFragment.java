/*
 * This project is licensed under the open source MPL V2.
 * See https://github.com/openMF/android-client/blob/master/LICENSE.md
 */

package com.mifos.mifosxdroid.online;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.mifos.mifosxdroid.R;
import com.mifos.mifosxdroid.core.MifosBaseFragment;
import com.mifos.objects.group.Center;
import com.mifos.objects.group.CenterWithAssociations;
import com.mifos.objects.group.Group;
import com.mifos.objects.organisation.Office;
import com.mifos.objects.organisation.Staff;
import com.mifos.utils.MifosApplication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.InjectView;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class GenerateCollectionSheetFragment extends MifosBaseFragment {

    public static final String LIMIT = "limit";
    public static final String ORDER_BY = "orderBy";
    public static final String SORT_ORDER = "sortOrder";
    public static final String ASCENDING = "ASC";
    public static final String ORDER_BY_FIELD_NAME = "name";
    public static final String STAFF_ID = "staffId";
    @InjectView(R.id.sp_branch_offices)
    Spinner sp_offices;
    @InjectView(R.id.sp_loan_officers)
    Spinner sp_loan_officers;
    @InjectView(R.id.sp_centers)
    Spinner sp_centers;
    @InjectView(R.id.sp_groups)
    Spinner sp_groups;

    private View rootView;
    private SharedPreferences sharedPreferences;
    private HashMap<String, Integer> officeNameIdHashMap = new HashMap<String, Integer>();
    private HashMap<String, Integer> staffNameIdHashMap = new HashMap<String, Integer>();
    private HashMap<String, Integer> centerNameIdHashMap = new HashMap<String, Integer>();
    private HashMap<String, Integer> groupNameIdHashMap = new HashMap<String, Integer>();

    public GenerateCollectionSheetFragment() {
        // Required empty public constructor
    }

    public static GenerateCollectionSheetFragment newInstance() {

        GenerateCollectionSheetFragment generateCollectionSheetFragment = new GenerateCollectionSheetFragment();

        return generateCollectionSheetFragment;

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_generate_collection_sheet, container, false);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        ButterKnife.inject(this, rootView);
        inflateOfficeSpinner();
        return rootView;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

//        menu.findItem(R.id.mItem_search).setIcon(
//                new IconDrawable(getActivity(), MaterialIcons.md_search)
//                        .colorRes(Color.WHITE)
//                        .actionBarSize());

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.mItem_search) {

            getActivity().finish();
        }


        return super.onOptionsItemSelected(item);
    }

    public void inflateOfficeSpinner() {

        showProgress();

        MifosApplication.getApi().officeService.getAllOffices(new Callback<List<Office>>() {
            @Override
            public void success(List<Office> offices, Response response) {

                final List<String> officeNames = new ArrayList<String>();
                officeNames.add(getString(R.string.spinner_office));
                officeNameIdHashMap.put(getString(R.string.spinner_office), -1);
                for (Office office : offices) {
                    officeNames.add(office.getName());
                    officeNameIdHashMap.put(office.getName(), office.getId());
                }

                ArrayAdapter<String> officeAdapter = new ArrayAdapter<String>(getActivity(),
                        android.R.layout.simple_spinner_item, officeNames);

                officeAdapter.notifyDataSetChanged();

                officeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                sp_offices.setAdapter(officeAdapter);

                sp_offices.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                        int officeId = officeNameIdHashMap.get(officeNames.get(position));

                        if (officeId != -1) {

                            inflateStaffSpinner(officeId);
                            inflateCenterSpinner(officeId, -1);
                            inflateGroupSpinner(officeId, -1);

                        } else {

                            Toast.makeText(getActivity(), getString(R.string.error_select_office), Toast.LENGTH_SHORT).show();

                        }

                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });

                hideProgress();

            }

            @Override
            public void failure(RetrofitError retrofitError) {

                System.out.println(retrofitError.getLocalizedMessage());

                hideProgress();
            }
        });

    }

    public void inflateStaffSpinner(final int officeId) {


        MifosApplication.getApi().staffService.getStaffForOffice(officeId, new Callback<List<Staff>>() {
            @Override
            public void success(List<Staff> staffs, Response response) {

                final List<String> staffNames = new ArrayList<String>();

                staffNames.add(getString(R.string.spinner_staff));
                staffNameIdHashMap.put(getString(R.string.spinner_staff), -1);

                for (Staff staff : staffs) {
                    staffNames.add(staff.getDisplayName());
                    staffNameIdHashMap.put(staff.getDisplayName(), staff.getId());
                }


                ArrayAdapter<String> staffAdapter = new ArrayAdapter<String>(getActivity(),
                        android.R.layout.simple_spinner_item, staffNames);

                staffAdapter.notifyDataSetChanged();

                staffAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                sp_loan_officers.setAdapter(staffAdapter);

                sp_loan_officers.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                        int staffId = staffNameIdHashMap.get(staffNames.get(position));

                        if (staffId != -1) {

                            inflateCenterSpinner(officeId, staffId);
                            inflateGroupSpinner(officeId, staffId);

                        } else {

                            Toast.makeText(getActivity(), getString(R.string.error_select_staff), Toast.LENGTH_SHORT).show();

                        }

                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });


            }

            @Override
            public void failure(RetrofitError retrofitError) {

                System.out.println(retrofitError.getLocalizedMessage());


            }
        });


    }

    public void inflateCenterSpinner(final int officeId, int staffId) {

        Map<String, Object> params = new HashMap<String, Object>();

        params.put(LIMIT, -1);
        params.put(ORDER_BY, ORDER_BY_FIELD_NAME);
        params.put(SORT_ORDER, ASCENDING);
        if (staffId >= 0) {
            params.put(STAFF_ID, staffId);
        }

        MifosApplication.getApi().centerService.getAllCentersInOffice(officeId, params, new Callback<List<Center>>() {
            @Override
            public void success(List<Center> centers, Response response) {

                final List<String> centerNames = new ArrayList<String>();

                centerNames.add(getString(R.string.spinner_center));
                centerNameIdHashMap.put(getString(R.string.spinner_center), -1);

                for (Center center : centers) {
                    centerNames.add(center.getName());
                    centerNameIdHashMap.put(center.getName(), center.getId());
                }


                ArrayAdapter<String> centerAdapter = new ArrayAdapter<String>(getActivity(),
                        android.R.layout.simple_spinner_item, centerNames);

                centerAdapter.notifyDataSetChanged();

                centerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                sp_centers.setAdapter(centerAdapter);

                sp_centers.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                        int centerId = centerNameIdHashMap.get(centerNames.get(position));

                        if (centerId != -1) {

                            inflateGroupSpinner(centerId);

                        } else {

                            Toast.makeText(getActivity(), getString(R.string.error_select_center), Toast.LENGTH_SHORT).show();

                        }

                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });

            }

            @Override
            public void failure(RetrofitError retrofitError) {

                System.out.println(retrofitError.getLocalizedMessage());

            }
        });


    }

    public void inflateGroupSpinner(final int officeId, int staffId) {

        Map<String, Object> params = new HashMap<String, Object>();

        params.put(LIMIT, -1);
        params.put(ORDER_BY, ORDER_BY_FIELD_NAME);
        params.put(SORT_ORDER, ASCENDING);
        if (staffId >= 0) {
            params.put(STAFF_ID, staffId);
        }


        MifosApplication.getApi().groupService.getAllGroupsInOffice(officeId, params, new Callback<List<Group>>() {
            @Override
            public void success(List<Group> groups, Response response) {

                List<String> groupNames = new ArrayList<String>();

                groupNames.add(getString(R.string.spinner_group));
                groupNameIdHashMap.put(getString(R.string.spinner_group), -1);

                for (Group group : groups) {
                    groupNames.add(group.getName());
                    groupNameIdHashMap.put(group.getName(), group.getId());
                }


                ArrayAdapter<String> groupAdapter = new ArrayAdapter<String>(getActivity(),
                        android.R.layout.simple_spinner_item, groupNames);

                groupAdapter.notifyDataSetChanged();

                groupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                sp_groups.setAdapter(groupAdapter);

            }

            @Override
            public void failure(RetrofitError retrofitError) {

            }
        });


    }

    public void inflateGroupSpinner(final int centerId) {

        MifosApplication.getApi().centerService.getAllGroupsForCenter(centerId, new Callback<CenterWithAssociations>() {
            @Override
            public void success(CenterWithAssociations centerWithAssociations, Response response) {

                List<Group> groups = centerWithAssociations.getGroupMembers();

                List<String> groupNames = new ArrayList<String>();

                groupNames.add(getString(R.string.spinner_group));
                groupNameIdHashMap.put(getString(R.string.spinner_group), -1);

                for (Group group : groups) {
                    groupNames.add(group.getName());
                    groupNameIdHashMap.put(group.getName(), group.getId());
                }


                ArrayAdapter<String> groupAdapter = new ArrayAdapter<String>(getActivity(),
                        android.R.layout.simple_spinner_item, groupNames);

                groupAdapter.notifyDataSetChanged();

                groupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                sp_groups.setAdapter(groupAdapter);

            }

            @Override
            public void failure(RetrofitError retrofitError) {

            }
        });


    }


}
