/*
 * This project is licensed under the open source MPL V2.
 * See https://github.com/openMF/android-client/blob/master/LICENSE.md
 */

package com.mifos.mifosxdroid;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mifos.exceptions.ShortOfLengthException;
import com.mifos.mifosxdroid.core.MifosBaseActivity;
import com.mifos.mifosxdroid.core.util.Toaster;
import com.mifos.mifosxdroid.online.DashboardFragmentActivity;
import com.mifos.objects.User;
import com.mifos.services.API;
import com.mifos.utils.Constants;
import com.mifos.utils.MifosApplication;

import org.apache.http.HttpStatus;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLHandshakeException;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import static android.view.View.GONE;
import static android.view.View.OnClickListener;
import static android.view.View.VISIBLE;

/**
 * Created by ishankhanna on 08/02/14.
 */
public class LoginActivity extends MifosBaseActivity implements Callback<User> {

    private final static String TAG = LoginActivity.class.getSimpleName();
    private static final String DOMAIN_NAME_REGEX_PATTERN = "^[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";
    private static final String IP_ADDRESS_REGEX_PATTERN = "^(\\d|[1-9]\\d|1\\d\\d|2([0-4]\\d|5[0-5]))\\.(\\d|[1-9]\\d|1\\d\\d|2([0-4]\\d|5[0-5]))\\.(\\d|[1-9]\\d|1\\d\\d|2([0-4]\\d|5[0-5]))\\.(\\d|[1-9]\\d|1\\d\\d|2([0-4]\\d|5[0-5]))$";
    public static final String PROTOCOL_HTTP = "http://";
    public static final String PROTOCOL_HTTPS = "https://";
    public static final String API_PATH = "/mifosng-provider/api/v1";
    SharedPreferences sharedPreferences;
    @InjectView(R.id.et_instanceURL)
    EditText et_instanceURL;
    @InjectView(R.id.et_username)
    EditText et_username;
    @InjectView(R.id.et_password)
    EditText et_password;
    @InjectView(R.id.bt_login)
    Button bt_login;
    @InjectView(R.id.tv_constructed_instance_url)
    TextView tv_constructed_instance_url;
    @InjectView(R.id.bt_connectionSettings)
    TextView bt_connectionSettings;
    @InjectView(R.id.et_tenantIdentifier)
    EditText et_tenantIdentifier;
    @InjectView(R.id.et_instancePort)
    EditText et_port;
    @InjectView(R.id.ll_connectionSettings)
    LinearLayout ll_connectionSettings;
    private String username;
    private String instanceURL;
    private String password;
    private Context context;
    private String authenticationToken;

    private Pattern domainNamePattern;
    private Matcher domainNameMatcher;
    private Pattern ipAddressPattern;
    private Matcher ipAddressMatcher;
    private Integer port = null;
    private API api;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        context = LoginActivity.this;

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String previouslyEnteredUrl = sharedPreferences.getString(Constants.INSTANCE_URL_KEY, getString(R.string.default_instance_url));
        String previouslyEnteredPort = sharedPreferences.getString(Constants.INSTANCE_PORT_KEY, "80");
        authenticationToken = sharedPreferences.getString(User.AUTHENTICATION_KEY, "NA");

        ButterKnife.inject(this);

        domainNamePattern = Pattern.compile(DOMAIN_NAME_REGEX_PATTERN);
        ipAddressPattern = Pattern.compile(IP_ADDRESS_REGEX_PATTERN);

        tv_constructed_instance_url.setText(PROTOCOL_HTTPS + previouslyEnteredUrl + API_PATH);
        et_instanceURL.setText(previouslyEnteredUrl);

        et_port.setInputType(InputType.TYPE_CLASS_NUMBER);
        if (!previouslyEnteredPort.equals("80")) {
            et_port.setText(previouslyEnteredPort);
        }

        bt_connectionSettings.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ll_connectionSettings.getVisibility() == VISIBLE)
                    ll_connectionSettings.setVisibility(GONE);
                else
                    ll_connectionSettings.setVisibility(VISIBLE);

            }
        });
        et_instanceURL.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                updateMyInstanceUrl();
            }
        });


        et_port.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateMyInstanceUrl();
            }
        });
    }

    private void updateMyInstanceUrl() {
        String textUnderConstruction;

        if (!et_port.getEditableText().toString().isEmpty()) {
            port = Integer.valueOf(et_port.getEditableText().toString().trim());
            textUnderConstruction = constructInstanceUrl(et_instanceURL.getEditableText().toString(), port);
        } else {
            port = null;
            textUnderConstruction = constructInstanceUrl(et_instanceURL.getEditableText().toString(), null);
        }

        tv_constructed_instance_url.setText(textUnderConstruction);

        if (!validateURL(et_instanceURL.getEditableText().toString())) {
            tv_constructed_instance_url.setTextColor(getResources().getColor(R.color.red));
        } else {
            tv_constructed_instance_url.setTextColor(getResources().getColor(R.color.deposit_green));
        }
    }

    public boolean validateUserInputs() throws ShortOfLengthException {

        String urlInputValue = et_instanceURL.getEditableText().toString();
        try {
            if (!validateURL(urlInputValue)) {
                return false;
            }
            String validDomain = sanitizeDomainNameInput(urlInputValue);
            if (!et_port.getEditableText().toString().trim().isEmpty()) {
                port = Integer.parseInt(et_port.getEditableText().toString());
            }
            String constructedURL = constructInstanceUrl(validDomain, port);
            tv_constructed_instance_url.setText(constructedURL);
            URL url = new URL(constructedURL);
            instanceURL = url.toURI().toString();
        } catch (MalformedURLException e) {
            throw new ShortOfLengthException("Instance URL", 5);
        } catch (URISyntaxException uriException) {
            throw new ShortOfLengthException("Instance URL", 5);
        }

        username = et_username.getEditableText().toString();
        if (username.length() < 5) {
            throw new ShortOfLengthException("Username", 5);
        }

        password = et_password.getEditableText().toString();
        if (password.length() < 6) {
            throw new ShortOfLengthException("Password", 6);
        }

        if (!et_tenantIdentifier.getEditableText().toString().isEmpty()) {

        }
        return true;
    }

    public String constructInstanceUrl(String validDomain, Integer port) {
        if (port != null) {
            return PROTOCOL_HTTPS + validDomain + ":" + port + API_PATH;
        } else {
            return PROTOCOL_HTTPS + validDomain + API_PATH;
        }
    }

    @Override
    public void success(User user, Response response) {
        ((MifosApplication) getApplication()).api = api;
        hideProgress();
        Toaster.show(findViewById(android.R.id.content), getString(R.string.toast_welcome) + " " + user.getUsername());
        saveLastAccessedInstanceUrl(instanceURL);
        saveLastAccessedInstanceDomainName(et_instanceURL.getEditableText().toString());
        if (!et_port.getEditableText().toString().trim().isEmpty()) {
            saveLastAccessedInstancePort(et_port.getEditableText().toString());
        }
        String lastAccessedTenantIdentifier =
                et_tenantIdentifier.getEditableText().toString().trim().isEmpty()
                        || et_tenantIdentifier.getEditableText() == null ? "default" : et_tenantIdentifier.getEditableText().toString().trim();
        saveLastAccessedTenant(lastAccessedTenantIdentifier);
        saveAuthenticationKey("Basic " + user.getBase64EncodedAuthenticationKey());
        Intent intent = new Intent(LoginActivity.this, DashboardFragmentActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void failure(RetrofitError retrofitError) {
        try {
            hideProgress();
            if (retrofitError.getCause() instanceof SSLHandshakeException) {
                promptUserToByPassTheSSLHandshake();
            } else if (retrofitError.getResponse().getStatus() == HttpStatus.SC_UNAUTHORIZED) {
                Toaster.show(findViewById(android.R.id.content), getString(R.string.error_login_failed));
            } else if (retrofitError.getResponse().getStatus() == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
                Toaster.show(findViewById(android.R.id.content), "Internal server error");
            }
        } catch (NullPointerException e) {
            Toaster.show(findViewById(android.R.id.content), getString(R.string.error_unknown));
        }
    }

    /**
     * This method should show a dialog box and ask the user
     * if he wants to use and unsafe connection. If he agrees
     * we must update our rest adapter to use an unsafe OkHttpClient
     * that trusts any damn thing.
     */
    private void promptUserToByPassTheSSLHandshake() {
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle("SSL Certificate Problem")
                .setMessage("There is a problem with your SSLCertificate, would you like to continue? This connection would be unsafe.")
                .setIcon(android.R.drawable.stat_sys_warning)
                .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        login(true);
                    }
                })
                .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .create();
        alertDialog.show();

    }

    @OnClick(R.id.bt_login)
    public void onLoginClick(Button button) {
        login(false);
    }

    private void login(boolean shouldByPassSSLSecurity) {
        try {
            if (validateUserInputs())
                showProgress("Logging In");
            api = new API(instanceURL, et_tenantIdentifier.getEditableText().toString().trim(), shouldByPassSSLSecurity);
            api.userAuthService.authenticate(username, password, this);
        } catch (ShortOfLengthException e) {
            Toaster.show(findViewById(android.R.id.content), e.toString());
        }
    }

    @OnEditorAction(R.id.et_password)
    public boolean passwordSubmitted(KeyEvent keyEvent) {
        if (keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            login(false);
            return true;
        }
        return false;
    }

    /**
     * After the user is authenticated the Base64
     * encoded auth token is saved in Shared Preferences
     * so that user can be logged in when returning to the app
     * even if the app is terminated from the background.
     *
     * @param authenticationKey
     */
    public void saveAuthenticationKey(String authenticationKey) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(User.AUTHENTICATION_KEY, authenticationKey);
        editor.apply();
    }

    /**
     * Stores the domain name in shared preferences
     * if the login was successful, so that it can be
     * referenced later or with multiple login/logouts
     * user doesn't need to type in the domain name
     * over and over again.
     *
     * @param instanceDomain
     */
    public void saveLastAccessedInstanceDomainName(String instanceDomain) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Constants.INSTANCE_DOMAIN_KEY, instanceDomain);
        editor.apply();
    }

    /**
     * Stores the complete instance URL in shared preferences
     * if the login was successful.
     *
     * @param instanceURL
     */
    public void saveLastAccessedInstanceUrl(String instanceURL) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Constants.INSTANCE_URL_KEY, instanceURL);
        editor.apply();
    }

    /**
     * Stores the port in shared preferences
     * if the login was successful, so that it can be
     * referenced later or with multiple login/logouts
     * user doesn't need to type in the instance port
     * over and over again.
     *
     * @param instancePort
     */
    public void saveLastAccessedInstancePort(String instancePort) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Constants.INSTANCE_PORT_KEY, instancePort);
        editor.apply();
    }

    /**
     * Stores the Tenant Identifier in shared preferences
     * if the login was successful, so that it can be
     * referenced later of with multiple login/logouts
     * user doesn't need to type in the tenant identifier
     * over and over again.
     *
     * @param tenantIdentifier
     */
    private void saveLastAccessedTenant(String tenantIdentifier) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Constants.TENANT_IDENTIFIER_KEY, tenantIdentifier);
        editor.apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.offline_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected: " + item.getItemId());
        switch (item.getItemId()) {
            case R.id.offline:
                startActivity(new Intent(this, OfflineCenterInputActivity.class));
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Removing protocol names and trailing slashes
     * from the user entered domain name.
     *
     * @param url
     * @return filteredString
     */
    public String sanitizeDomainNameInput(String url) {

        String filteredUrl;

        if (url.contains("https://")) {

            //Strip https:// from the URL
            filteredUrl = url.replace("https://", "");

        } else if (url.contains("http://")) {

            //String http:// from the URL
            filteredUrl = url.replace("http://", "");
        } else {

            //String URL doesn't include protocol
            filteredUrl = url;
        }

        if (filteredUrl.charAt(filteredUrl.length() - 1) == '/') {
            filteredUrl = filteredUrl.replace("/", "");
        }

        return filteredUrl;

    }

    /**
     * Validates Domain name entered by user
     * against valid domain name patterns
     * and also IP address patterns.
     *
     * @param hex
     * @return true if pattern is valid
     * and false otherwise
     */
    public boolean validateURL(final String hex) {

        domainNameMatcher = domainNamePattern.matcher(hex);
        ipAddressMatcher = ipAddressPattern.matcher(hex);
        if (domainNameMatcher.matches()) return true;
        if (ipAddressMatcher.matches()) return true;

        //TODO MAKE SURE YOU UPDATE THE REGEX to check for ports in the URL
        return false;
    }
}