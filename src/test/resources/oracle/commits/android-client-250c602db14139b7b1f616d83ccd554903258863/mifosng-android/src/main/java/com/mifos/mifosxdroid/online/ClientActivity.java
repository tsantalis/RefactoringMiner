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
import com.mifos.objects.accounts.loan.LoanWithAssociations;
import com.mifos.objects.accounts.savings.DepositType;
import com.mifos.objects.accounts.savings.SavingsAccountWithAssociations;
import com.mifos.utils.Constants;
import com.mifos.utils.FragmentConstants;

import butterknife.ButterKnife;

public class ClientActivity extends ActionBarActivity implements ClientDetailsFragment.OnFragmentInteractionListener,
        LoanAccountSummaryFragment.OnFragmentInteractionListener,
        LoanRepaymentFragment.OnFragmentInteractionListener,
        SavingsAccountSummaryFragment.OnFragmentInteractionListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_global_container_layout);
        ButterKnife.inject(this);
        int clientId = getIntent().getExtras().getInt(Constants.CLIENT_ID);
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        ClientDetailsFragment clientDetailsFragment = ClientDetailsFragment.newInstance(clientId);
        fragmentTransaction.replace(R.id.global_container, clientDetailsFragment, FragmentConstants.FRAG_CLIENT_DETAILS).commit();
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.client, menu);
        return true;
    }

    /**
     * This method is called EVERY TIME the menu button is pressed
     * on the action bar. So All dynamic changes in the menu are
     * done here.
     *
     * @param menu Current Menu in the Layout
     * @return true if the menu was successfully prepared
     */

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        return super.onPrepareOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        return super.onOptionsItemSelected(item);
    }



    /*
     * Called when a Loan Account is Selected
     * from the list of Loan Accounts on Client Details Fragment
     * It displays the summary of the Selected Loan Account
     */

    @Override
    public void loadLoanAccountSummary(int loanAccountNumber) {

        LoanAccountSummaryFragment loanAccountSummaryFragment
                = LoanAccountSummaryFragment.newInstance(loanAccountNumber);
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.addToBackStack(FragmentConstants.FRAG_CLIENT_DETAILS);
        fragmentTransaction.replace(R.id.global_container, loanAccountSummaryFragment, FragmentConstants.FRAG_LOAN_ACCOUNT_SUMMARY).commit();

    }

    /*
     * Called when a Savings Account is Selected
     * from the list of Savings Accounts on Client Details Fragment
     *
     * It displays the summary of the Selected Savings Account
     */

    @Override
    public void loadSavingsAccountSummary(int savingsAccountNumber, DepositType accountType) {
        SavingsAccountSummaryFragment savingsAccountSummaryFragment
                = SavingsAccountSummaryFragment.newInstance(savingsAccountNumber, accountType);
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.addToBackStack(FragmentConstants.FRAG_CLIENT_DETAILS);
        fragmentTransaction.replace(R.id.global_container, savingsAccountSummaryFragment, FragmentConstants.FRAG_SAVINGS_ACCOUNT_SUMMARY).commit();
    }

    /*
     * Called when the make the make repayment button is clicked
     * in the Loan Account Summary Fragment.
     *
     * It will display the Loan Repayment Fragment where
     * the Information of the repayment has to be filled in.
     */
    @Override
    public void makeRepayment(LoanWithAssociations loan) {

        LoanRepaymentFragment loanRepaymentFragment = LoanRepaymentFragment.newInstance(loan);
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.addToBackStack(FragmentConstants.FRAG_LOAN_ACCOUNT_SUMMARY);
        fragmentTransaction.replace(R.id.global_container, loanRepaymentFragment).commit();
    }

    /*
     * Called when the Repayment Schedule option from the Menu is
     * clicked
     *
     * It will display the Complete Loan Repayment Schedule.
     */

    @Override
    public void loadRepaymentSchedule(int loanId) {

        LoanRepaymentScheduleFragment loanRepaymentScheduleFragment = LoanRepaymentScheduleFragment.newInstance(loanId);
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.addToBackStack(FragmentConstants.FRAG_LOAN_ACCOUNT_SUMMARY);
        fragmentTransaction.replace(R.id.global_container, loanRepaymentScheduleFragment).commit();

    }

    /*
     * Called when the Transactions option from the Menu is clicked
     *
     * It will display all the Transactions associated with the Loan
     * and also their details
     */

    @Override
    public void loadLoanTransactions(int loanId) {

        LoanTransactionsFragment loanTransactionsFragment = LoanTransactionsFragment.newInstance(loanId);
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.addToBackStack(FragmentConstants.FRAG_LOAN_ACCOUNT_SUMMARY);
        fragmentTransaction.replace(R.id.global_container, loanTransactionsFragment).commit();

    }

    /*
     * Called when the make the make deposit button is clicked
     * in the Savings Account Summary Fragment.
     *
     * It will display the Transaction Fragment where the information
     * of the transaction has to be filled in.
     *
     * The transactionType defines if the transaction is a Deposit or a Withdrawal
     *
    */
    @Override
    public void doTransaction(SavingsAccountWithAssociations savingsAccountWithAssociations, String transactionType, DepositType accountType) {

        SavingsAccountTransactionFragment savingsAccountTransactionFragment =
                SavingsAccountTransactionFragment.newInstance(savingsAccountWithAssociations, transactionType, accountType);
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.addToBackStack(FragmentConstants.FRAG_SAVINGS_ACCOUNT_SUMMARY);
        fragmentTransaction.replace(R.id.global_container, savingsAccountTransactionFragment).commit();
    }
}
