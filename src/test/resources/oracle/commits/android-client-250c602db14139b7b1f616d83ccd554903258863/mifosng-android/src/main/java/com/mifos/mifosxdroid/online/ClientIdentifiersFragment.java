/*
 * This project is licensed under the open source MPL V2.
 * See https://github.com/openMF/android-client/blob/master/LICENSE.md
 */

package com.mifos.mifosxdroid.online;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.mifos.mifosxdroid.R;
import com.mifos.mifosxdroid.adapters.IdentifierListAdapter;
import com.mifos.objects.noncore.Identifier;
import com.mifos.utils.Constants;
import com.mifos.utils.MifosApplication;
import com.mifos.utils.SafeUIBlockingUtility;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class ClientIdentifiersFragment extends Fragment {

    @InjectView(R.id.lv_identifiers)
    ListView lv_identifiers;

    private OnFragmentInteractionListener mListener;

    View rootView;

    SafeUIBlockingUtility safeUIBlockingUtility;

    ActionBarActivity activity;

    SharedPreferences sharedPreferences;

    ActionBar actionBar;

    int clientId;


    public static ClientIdentifiersFragment newInstance(int clientId) {
        ClientIdentifiersFragment fragment = new ClientIdentifiersFragment();
        Bundle args = new Bundle();
        args.putInt(Constants.CLIENT_ID, clientId);
        fragment.setArguments(args);
        return fragment;
    }
    public ClientIdentifiersFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

            clientId = getArguments().getInt(Constants.CLIENT_ID);

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_client_identifiers, container, false);
        activity = (ActionBarActivity) getActivity();
        safeUIBlockingUtility = new SafeUIBlockingUtility(getActivity());
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        actionBar = activity.getSupportActionBar();
        ButterKnife.inject(this, rootView);
        actionBar.setTitle(getString(R.string.identifiers));

        loadIdentifiers();

        return rootView;

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
//            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    public interface OnFragmentInteractionListener {

    }

    public void loadIdentifiers() {

        safeUIBlockingUtility.safelyBlockUI();

        ((MifosApplication) getActivity().getApplicationContext()).api.identifierService.getListOfIdentifiers(clientId, new Callback<List<Identifier>>() {
            @Override
            public void success(List<Identifier> identifiers, Response response) {

                if (identifiers != null && identifiers.size()>0) {

                    IdentifierListAdapter identifierListAdapter = new IdentifierListAdapter(getActivity(), identifiers, clientId);
                    lv_identifiers.setAdapter(identifierListAdapter);

                } else {

                    Toast.makeText(getActivity(), getString(R.string.message_no_identifiers_available), Toast.LENGTH_SHORT).show();
                }

                safeUIBlockingUtility.safelyUnBlockUI();
            }

            @Override
            public void failure(RetrofitError retrofitError) {

                System.out.print(retrofitError.getLocalizedMessage());
                safeUIBlockingUtility.safelyUnBlockUI();
            }
        });

    }
}
