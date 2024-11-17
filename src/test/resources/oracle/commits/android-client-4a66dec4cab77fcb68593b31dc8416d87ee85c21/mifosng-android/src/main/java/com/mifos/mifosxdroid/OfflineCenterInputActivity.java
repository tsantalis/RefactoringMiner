/*
 * This project is licensed under the open source MPL V2.
 * See https://github.com/openMF/android-client/blob/master/LICENSE.md
 */

package com.mifos.mifosxdroid;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

import com.mifos.mifosxdroid.core.MifosBaseActivity;
import com.mifos.mifosxdroid.core.util.Toaster;

import java.util.Calendar;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class OfflineCenterInputActivity extends MifosBaseActivity implements DatePickerDialog.OnDateSetListener {
    public static String PREF_CENTER_DETAILS = "pref_center_details";
    public static String STAFF_ID_KEY = "pref_staff_id";
    public static String BRANCH_ID_KEY = "pref_branch_id";
    public static String TRANSACTION_DATE_KEY = "pref_transaction_date";
    @InjectView(R.id.et_staff_id)
    EditText etStaffId;
    @InjectView(R.id.et_branch_id)
    EditText etBranchId;
    @InjectView(R.id.tv_select_date)
    TextView tvSelectDate;
    private String date;
    private int staffId;
    private int branchId;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isCenterIdAvailable()) {
            finishAndStartCenterListActivity();
        }
        setContentView(R.layout.activity_center_details);
        ButterKnife.inject(this);
        showBackButton();
    }

    private boolean isCenterIdAvailable() {
        SharedPreferences preferences = getSharedPreferences(OfflineCenterInputActivity.PREF_CENTER_DETAILS, Context.MODE_PRIVATE);
        int centerId = preferences.getInt(OfflineCenterInputActivity.STAFF_ID_KEY, -1);
        if (centerId != -1)
            return true;
        else
            return false;

    }

    @OnClick(R.id.tv_select_date)
    public void OnSelectDate(TextView textView) {
        createDatePicker(this, this);
    }

    @OnClick(R.id.btnSave)
    public void OnClickSave(Button button) {
        if (getData()) {
            saveCenterIdToPref();
            finishAndStartCenterListActivity();
        }
    }

    private void finishAndStartCenterListActivity() {
        finish();
        Intent intent = new Intent(this, CenterListActivity.class);
        startActivity(intent);
    }

    private boolean getData() {
        boolean isAllDetailsFilled = true;
        if (etStaffId.getText().toString().length() > 0
                && tvSelectDate.getText().toString().length() > 0
                && etBranchId.getText().toString().length() > 0) {
            staffId = Integer.parseInt(etStaffId.getEditableText().toString());
            date = tvSelectDate.getText().toString();
            branchId = Integer.parseInt(etBranchId.getEditableText().toString());
        } else {
            isAllDetailsFilled = false;
            Toaster.show(findViewById(android.R.id.content), "Please fill all the details");
        }
        return isAllDetailsFilled;
    }

    private void createDatePicker(Context context, DatePickerDialog.OnDateSetListener dateSetListener) {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog datePickerDialog = new DatePickerDialog(context, dateSetListener, year, month, day);
        datePickerDialog.show();
    }

    @Override
    public void onDateSet(DatePicker datePicker, int year, int monthOfYear,
                          int dayOfMonth) {
        StringBuilder date = new StringBuilder();
        date.append(dayOfMonth);
        date.append("-");
        date.append(monthOfYear + 1);
        date.append("-");
        date.append(year);
        tvSelectDate.setText(date.toString());
    }

    private void saveCenterIdToPref() {
        SharedPreferences preferences = getSharedPreferences(PREF_CENTER_DETAILS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(STAFF_ID_KEY, staffId);
        editor.putInt(BRANCH_ID_KEY, branchId);
        editor.putString(TRANSACTION_DATE_KEY, date);
        editor.commit();
    }
}
