/*
 * This project is licensed under the open source MPL V2.
 * See https://github.com/openMF/android-client/blob/master/LICENSE.md
 */

package com.mifos.mifosxdroid.online;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.mifos.mifosxdroid.R;
import com.mifos.mifosxdroid.core.util.Toaster;
import com.mifos.objects.SearchedEntity;
import com.mifos.utils.Constants;
import com.mifos.utils.MifosApplication;
import com.mifos.utils.SafeUIBlockingUtility;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class ClientSearchFragment extends Fragment implements AdapterView.OnItemClickListener {

    private static final String TAG = ClientSearchFragment.class.getSimpleName();

    @InjectView(R.id.et_search_by_id)
    EditText et_searchById;

    @InjectView(R.id.bt_searchClient)
    Button bt_searchClient;

    @InjectView(R.id.lv_searchResults)
    ListView lv_searchResults;

    List<String> clientNames = new ArrayList<String>();
    List<Integer> clientIds = new ArrayList<Integer>();
    SafeUIBlockingUtility safeUIBlockingUtility;
    private String searchQuery;

    InputMethodManager inputMethodManager;

    public static ClientSearchFragment newInstance() {
        ClientSearchFragment fragment = new ClientSearchFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_client_search, null);
        ButterKnife.inject(this, rootView);
        return rootView;
    }

    @OnClick(R.id.bt_searchClient)
    public void performSearch() {
        if (!et_searchById.getEditableText().toString().trim().isEmpty()) {
            searchQuery = et_searchById.getEditableText().toString().trim();
            findClients(searchQuery);
        } else {
            Toaster.show(et_searchById, "No Search Query Entered!");
        }
    }

    public void findClients(final String clientName) {
        safeUIBlockingUtility = new SafeUIBlockingUtility(getActivity());
        safeUIBlockingUtility.safelyBlockUI();
        ((MifosApplication) getActivity().getApplicationContext()).api.searchService.searchClientsByName(clientName, new Callback<List<SearchedEntity>>() {
            @Override
            public void success(List<SearchedEntity> searchedEntities, Response response) {

                if (!searchedEntities.isEmpty()) {
                    Iterator<SearchedEntity> iterator = searchedEntities.iterator();
                    clientNames.clear();
                    clientIds.clear();
                    while (iterator.hasNext()) {
                        SearchedEntity searchedEntity = iterator.next();
                        clientNames.add("#" + searchedEntity.getEntityId() + " - " + searchedEntity.getEntityName());
                        clientIds.add(searchedEntity.getEntityId());
                    }

                    String[] clientNamesArrayForAdapter = clientNames.toArray(new String[clientNames.size()]);

                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                            android.R.layout.simple_list_item_1, clientNamesArrayForAdapter);

                    lv_searchResults.setAdapter(adapter);
                    lv_searchResults.setOnItemClickListener(ClientSearchFragment.this);

                    //If the search query returned one or more results close the keyboard
                    hideKeyboard();
                } else {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
                    alertDialogBuilder.setTitle("Message");
                    alertDialogBuilder
                            .setMessage("No results found for entered query")
                            .setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    }).create().show();
                }
                safeUIBlockingUtility.safelyUnBlockUI();
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                safeUIBlockingUtility.safelyUnBlockUI();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        //Fragment getting detached, keyboard if open must be hidden
        hideKeyboard();
        super.onPause();
    }

    /*
        There is a need for this method in the following cases :
        1. If user entered a search query and went out of the app.
        2. If user entered a search query and got some search results and went out of the app.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        try {
            String queryString = et_searchById.getEditableText().toString();

            if (queryString != null && !(queryString.equals(""))) {
                outState.putString(TAG + et_searchById.getId(), queryString);
            }

        } catch (NullPointerException npe) {
            //Looks like edit text didn't get initialized properly
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            String queryString = savedInstanceState.getString(TAG + et_searchById.getId());
            if (queryString != null && !(queryString.equals(""))) {
                et_searchById.setText(queryString);
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        Intent clientActivityIntent = new Intent(getActivity(), ClientActivity.class);
        clientActivityIntent.putExtra(Constants.CLIENT_ID, clientIds.get(i));
        startActivity(clientActivityIntent);

    }

    public void hideKeyboard() {
        inputMethodManager.hideSoftInputFromWindow(et_searchById.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
    }
}
