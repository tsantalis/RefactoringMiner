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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.mifos.mifosxdroid.R;
import com.mifos.mifosxdroid.adapters.LoanRepaymentScheduleAdapter;
import com.mifos.objects.accounts.loan.LoanWithAssociations;
import com.mifos.objects.accounts.loan.Period;
import com.mifos.objects.accounts.loan.RepaymentSchedule;
import com.mifos.utils.Constants;
import com.mifos.utils.MifosApplication;
import com.mifos.utils.SafeUIBlockingUtility;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class LoanRepaymentScheduleFragment extends Fragment {

    View rootView;
    SafeUIBlockingUtility safeUIBlockingUtility;
    ActionBarActivity activity;
    SharedPreferences sharedPreferences;
    ActionBar actionBar;
    @InjectView(R.id.lv_repayment_schedule)
    ListView lv_repaymentSchedule;
    @InjectView(R.id.tv_total_paid)
    TextView tv_totalPaid;
    @InjectView(R.id.tv_total_upcoming)
    TextView tv_totalUpcoming;
    @InjectView(R.id.tv_total_overdue)
    TextView tv_totalOverdue;
    @InjectView(R.id.flrs_footer)
    View flrs_footer;
    private int loanAccountNumber;
    private OnFragmentInteractionListener mListener;

    public LoanRepaymentScheduleFragment() {
        // Required empty public constructor
    }

    public static LoanRepaymentScheduleFragment newInstance(int loanAccountNumber) {
        LoanRepaymentScheduleFragment fragment = new LoanRepaymentScheduleFragment();
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        rootView = inflater.inflate(R.layout.fragment_loan_repayment_schedule, container, false);
        activity = (ActionBarActivity) getActivity();
        safeUIBlockingUtility = new SafeUIBlockingUtility(LoanRepaymentScheduleFragment.this.getActivity());
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        actionBar = activity.getSupportActionBar();
        actionBar.setTitle(getResources().getString(R.string.loan_repayment_schedule));
        ButterKnife.inject(this, rootView);

        inflateRepaymentSchedule();

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            // mListener = (OnFragmentInteractionListener) activity;
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

    public void inflateRepaymentSchedule() {

        ((MifosApplication) getActivity().getApplicationContext()).api.loanService.getLoanRepaymentSchedule(loanAccountNumber, new Callback<LoanWithAssociations>() {
            @Override
            public void success(LoanWithAssociations loanWithAssociations, Response response) {

                List<Period> listOfActualPeriods = loanWithAssociations.getRepaymentSchedule().getlistOfActualPeriods();

                LoanRepaymentScheduleAdapter loanRepaymentScheduleAdapter =
                        new LoanRepaymentScheduleAdapter(getActivity(), listOfActualPeriods);
                lv_repaymentSchedule.setAdapter(loanRepaymentScheduleAdapter);

                String totalRepaymentsCompleted = getResources().getString(R.string.complete) + " : ";
                String totalRepaymentsOverdue = getResources().getString(R.string.overdue) + " : ";
                String totalRepaymentsPending = getResources().getString(R.string.pending) + " : ";
                //Implementing the Footer here
                tv_totalPaid.setText(totalRepaymentsCompleted + String.valueOf(
                        RepaymentSchedule.getNumberOfRepaymentsComplete(listOfActualPeriods)
                ));

                tv_totalOverdue.setText(totalRepaymentsOverdue + String.valueOf(
                        RepaymentSchedule.getNumberOfRepaymentsOverDue(listOfActualPeriods)
                ));

                tv_totalUpcoming.setText(totalRepaymentsPending + String.valueOf(
                        RepaymentSchedule.getNumberOfRepaymentsPending(listOfActualPeriods)
                ));

            }

            @Override
            public void failure(RetrofitError retrofitError) {

                Log.i(getActivity().getLocalClassName(), retrofitError.getLocalizedMessage());
            }
        });


    }

    public interface OnFragmentInteractionListener {

    }

}
