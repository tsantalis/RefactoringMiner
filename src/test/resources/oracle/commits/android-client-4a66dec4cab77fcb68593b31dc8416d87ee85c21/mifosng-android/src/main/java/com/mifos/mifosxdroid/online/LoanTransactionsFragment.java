/*
 * This project is licensed under the open source MPL V2.
 * See https://github.com/openMF/android-client/blob/master/LICENSE.md
 */

package com.mifos.mifosxdroid.online;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

import com.mifos.mifosxdroid.R;
import com.mifos.mifosxdroid.adapters.LoanTransactionAdapter;
import com.mifos.mifosxdroid.core.MifosBaseFragment;
import com.mifos.objects.accounts.loan.LoanWithAssociations;
import com.mifos.utils.Constants;
import com.mifos.utils.MifosApplication;

import butterknife.ButterKnife;
import butterknife.InjectView;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class LoanTransactionsFragment extends MifosBaseFragment {

    @InjectView(R.id.elv_loan_transactions)
    ExpandableListView elv_loanTransactions;

    private int loanAccountNumber;

    private OnFragmentInteractionListener mListener;
    private View rootView;
    private SharedPreferences sharedPreferences;

    public static LoanTransactionsFragment newInstance(int loanAccountNumber) {
        LoanTransactionsFragment fragment = new LoanTransactionsFragment();
        Bundle args = new Bundle();
        args.putInt(Constants.LOAN_ACCOUNT_NUMBER, loanAccountNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            loanAccountNumber = getArguments().getInt(Constants.LOAN_ACCOUNT_NUMBER);
        }

        setHasOptionsMenu(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_loan_transactions, container, false);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        ButterKnife.inject(this, rootView);
        inflateLoanTransactions();
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            //mListener = (OnFragmentInteractionListener) activity;
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

    /**
     * Prepare the Screen's standard options menu to be displayed.  This is
     * called right before the menu is shown, every time it is shown.  You can
     * use this method to efficiently enable/disable items or otherwise
     * dynamically modify the contents.  See
     * {@link android.app.Activity#onPrepareOptionsMenu(android.view.Menu) Activity.onPrepareOptionsMenu}
     * for more information.
     *
     * @param menu The options menu as last shown or first initialized by
     *             onCreateOptionsMenu().
     * @see #setHasOptionsMenu
     * @see #onCreateOptionsMenu
     */
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        super.onPrepareOptionsMenu(menu);
    }

    public interface OnFragmentInteractionListener {

    }

    public void inflateLoanTransactions() {
        MifosApplication.getApi().loanService.getLoanWithTransactions(loanAccountNumber, new Callback<LoanWithAssociations>() {
            @Override
            public void success(LoanWithAssociations loanWithAssociations, Response response) {

                if (loanWithAssociations != null) {

                    Log.i("Transaction List Size", "" + loanWithAssociations.getTransactions().size());

                    LoanTransactionAdapter loanTransactionAdapter =
                            new LoanTransactionAdapter(getActivity(), loanWithAssociations.getTransactions());
                    elv_loanTransactions.setAdapter(loanTransactionAdapter);
                    elv_loanTransactions.setGroupIndicator(null);
                }

            }

            @Override
            public void failure(RetrofitError retrofitError) {

            }
        });
    }
}
