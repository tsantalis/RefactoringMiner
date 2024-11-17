/*
 * This project is licensed under the open source MPL V2.
 * See https://github.com/openMF/android-client/blob/master/LICENSE.md
 */

package com.mifos.mifosxdroid.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.mifos.mifosxdroid.R;
import com.mifos.mifosxdroid.adapters.LoanListAdapter;
import com.mifos.objects.db.Loan;
import com.orm.query.Condition;
import com.orm.query.Select;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class LoanFragment extends Fragment
{
    @InjectView(R.id.lv_loan)
    ListView lv_loans;
    LoanListAdapter adapter = null;
    private int clientId;
    final private String tag = getClass().getSimpleName();
    final List<Loan> loansClientHave = new ArrayList<Loan>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_loan, null);
        ButterKnife.inject(this, view);
        getClientId();
        setAdapter();
        return view;
    }

    private int getClientId()
    {
        clientId = getArguments().getInt("clientId", 0);
        return clientId;
    }

    private void setAdapter()
    {
        getLoans();
        if (adapter == null)
            adapter = new LoanListAdapter(getActivity(), loansClientHave);
        lv_loans.setAdapter(adapter);
    }

    private List<Loan> getLoans()
    {
        loansClientHave.clear();
        List<Loan> loanList = Select.from(Loan.class).where(Condition.prop("client").eq(clientId)).list();
        Log.i(tag, "Looking for loan with client ID:" + clientId);
        Log.i(tag, "Loans in ClientFragment from DB:" + loanList.toString());

        loansClientHave.addAll(loanList);
        return loansClientHave;
    }
}
