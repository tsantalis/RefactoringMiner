package com.mifos.mifosxdroid.tests;

import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.test.ViewAsserts;
import android.test.suitebuilder.annotation.SmallTest;
import android.test.suitebuilder.annotation.Suppress;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import com.mifos.mifosxdroid.R;
import com.mifos.mifosxdroid.online.ClientActivity;
import com.mifos.mifosxdroid.online.ClientDetailsFragment;
import com.mifos.utils.Constants;
import com.mifos.utils.FragmentConstants;

/**
 * Created by Gabriel Esteban on 07/12/14.
 */
@Suppress // TODO: Fix NPE
public class ClientDetailsFragmentTest extends ActivityInstrumentationTestCase2<ClientActivity> {

    ClientActivity clientActivity;
    ClientDetailsFragment detailsFragment;

    ImageView iv_client_image;
    TextView tv_full_name;
    TableLayout tbl_client_details;
    RelativeLayout loans, savings, recurring;

    public ClientDetailsFragmentTest() {
        super(ClientActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Intent clientActivityIntent = new Intent();
        clientActivityIntent.putExtra(Constants.CLIENT_ID, "000000001");
        setActivityIntent(clientActivityIntent);

        Constants.applicationContext = getInstrumentation().getTargetContext().getApplicationContext();
        clientActivity = getActivity();

        //waiting for the API
        Thread.sleep(2000);

        detailsFragment = (ClientDetailsFragment) getActivity().getSupportFragmentManager().findFragmentByTag(FragmentConstants.FRAG_CLIENT_DETAILS);

        iv_client_image = (ImageView) clientActivity.findViewById(R.id.iv_clientImage);
        tv_full_name = (TextView) clientActivity.findViewById(R.id.tv_fullName);
        tbl_client_details = (TableLayout) clientActivity.findViewById(R.id.tbl_clientDetails);
        loans = (RelativeLayout) clientActivity.findViewById(R.id.account_accordion_section_loans);
        savings = (RelativeLayout) clientActivity.findViewById(R.id.account_accordion_section_savings);
        recurring = (RelativeLayout) clientActivity.findViewById(R.id.account_accordion_section_recurring);
    }

    @SmallTest
    public void testFragmentIsNotNull(){
        assertNotNull(detailsFragment);
    }

    @SmallTest
    public void testViewsAreNotNull() {
        assertNotNull(iv_client_image);
        assertNotNull(tv_full_name);
        assertNotNull(tbl_client_details);
        assertNotNull(loans);
        assertNotNull(savings);
        assertNotNull(recurring);
    }

    @SmallTest
    public void testViewsAreOnTheScreen() {
        final View decorView = clientActivity.getWindow().getDecorView();

        ViewAsserts.assertOnScreen(decorView, iv_client_image);
        ViewAsserts.assertOnScreen(decorView, tv_full_name);
        ViewAsserts.assertOnScreen(decorView, tbl_client_details);
        ViewAsserts.assertOnScreen(decorView, loans);
        ViewAsserts.assertOnScreen(decorView, savings);
        ViewAsserts.assertOnScreen(decorView, recurring);
    }

    @SmallTest
    public void testClientDocumentsFragmentShowed() throws InterruptedException {
        //clicking the button
        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);
        getInstrumentation().invokeMenuActionSync(clientActivity, ClientDetailsFragment.MENU_ITEM_DOCUMENTS, 0);

        //if something is wrong, invokeMenuActionSync will take an exception

        //waiting for the API
        Thread.sleep(2000);

        this.sendKeys(KeyEvent.KEYCODE_BACK);
    }

    @SmallTest
    public void testClientIdentifiersFragmentShowed() throws InterruptedException {
        //clicking the button
        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);
        getInstrumentation().invokeMenuActionSync(clientActivity, ClientDetailsFragment.MENU_ITEM_IDENTIFIERS, 0);

        //if something is wrong, invokeMenuActionSync will take an exception

        //waiting for the API
        Thread.sleep(2000);

        this.sendKeys(KeyEvent.KEYCODE_BACK);
    }
}
