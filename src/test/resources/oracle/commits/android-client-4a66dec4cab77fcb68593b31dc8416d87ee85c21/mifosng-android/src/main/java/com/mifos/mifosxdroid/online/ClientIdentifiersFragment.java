/*
 * This project is licensed under the open source MPL V2.
 * See https://github.com/openMF/android-client/blob/master/LICENSE.md
 */

package com.mifos.mifosxdroid.online;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.mifos.mifosxdroid.R;
import com.mifos.mifosxdroid.adapters.IdentifierListAdapter;
import com.mifos.mifosxdroid.core.MifosBaseFragment;
import com.mifos.objects.noncore.Identifier;
import com.mifos.utils.Constants;
import com.mifos.utils.MifosApplication;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class ClientIdentifiersFragment extends MifosBaseFragment {

    @InjectView(R.id.lv_identifiers)
    ListView lv_identifiers;

    private View rootView;
    private SharedPreferences sharedPreferences;
    private int clientId;

    public static ClientIdentifiersFragment newInstance(int clientId) {
        ClientIdentifiersFragment fragment = new ClientIdentifiersFragment();
        Bundle args = new Bundle();
        args.putInt(Constants.CLIENT_ID, clientId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
            clientId = getArguments().getInt(Constants.CLIENT_ID);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_client_identifiers, container, false);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        ButterKnife.inject(this, rootView);
        setToolbarTitle(getString(R.string.identifiers));
        loadIdentifiers();
        return rootView;
    }


    public void loadIdentifiers() {
        showProgress("Working...");
        MifosApplication.getApi().identifierService.getListOfIdentifiers(clientId, new Callback<List<Identifier>>() {
            @Override
            public void success(List<Identifier> identifiers, Response response) {
                if (identifiers != null && identifiers.size() > 0) {
                    IdentifierListAdapter identifierListAdapter = new IdentifierListAdapter(getActivity(), identifiers, clientId);
                    lv_identifiers.setAdapter(identifierListAdapter);
                } else {
                    Toast.makeText(getActivity(), getString(R.string.message_no_identifiers_available), Toast.LENGTH_SHORT).show();
                }
                hideProgress();
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                hideProgress();
            }
        });
    }
}
