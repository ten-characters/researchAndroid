package com.ten_characters.researchAndroid.activities;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.ten_characters.researchAndroid.GlobalApp;
import com.ten_characters.researchAndroid.R;
import com.ten_characters.researchAndroid.auth.AccountUtility;
import com.ten_characters.researchAndroid.userInfo.User;
import com.ten_characters.researchAndroid.server.OnTaskCompleted;
import com.ten_characters.researchAndroid.server.PalletServer;
import com.ten_characters.researchAndroid.server.ServerUtility;

import org.json.JSONException;
import org.json.JSONObject;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

import static com.ten_characters.researchAndroid.server.ServerUtility.*;

public class AuthActivity extends AccountAuthenticatorActivity implements OnTaskCompleted {

    private static final String LOG_TAG = AuthActivity.class.getSimpleName();

    // Member Variables
    private AccountManager mAccountManager;
    private String mAuthTokenType;
    private boolean isLoggingOut = false;
    PalletServer server;

    // Keys for reference
    public static final String PARAM_USER_PASS = "USER_PASS";

    public static final String ARG_AUTH_TYPE = "AUTH_TYPE";
    public static final String ARG_ACCOUNT_TYPE = "ACCOUNT_TYPE";
    public static final String ARG_ACCOUNT_NAME = "ACCOUNT_NAME";
    public static final String ARG_IS_ADDING_NEW_ACCOUNT = "IS_ADDING_ACCOUNT";

    public static final String KEY_ERROR_MESSAGE = "ERROR";

    public static final String ERROR_MESSAGE_EMAIL = "Invalid Email format";
    public static final String ERROR_MESSAGE_PASSWORD = "Invalid Password format";

    /** This is used to implement our custom font, initialized in the GlobalApp.java file*/
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_auth);

        // On some phones (cough @ samsung) don't render our photos unless they are scaled
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2;
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.pallet_logo_large, options);
        ((ImageView) findViewById(R.id.pallet_logo_imageview)).setImageBitmap(bm);

        // Account managery stuff
        mAccountManager = AccountManager.get(getBaseContext());

        // Get the Auth Token Type if not already gotten
        if (mAuthTokenType == null) {
            mAuthTokenType = AccountUtility.AUTHTOKEN_TYPE_FULL_ACCESS;
        }

        // Check if the account name is stored and display it if true
        String accountName = getIntent().getStringExtra(ARG_ACCOUNT_NAME);

        // Check to see if they have an account already in the phone!
        if (accountName == null && mAccountManager.getAccountsByType(AccountUtility.ACCOUNT_TYPE).length != 0) {
            accountName = mAccountManager.getAccountsByType(AccountUtility.ACCOUNT_TYPE)[0].name;
        }

        if (accountName != null) {
            ((TextView) findViewById(R.id.account_username)).setText(accountName);
        }

        // Initialize server with callback listener
        server = new PalletServer(this, this);

        Intent i = getIntent();
        if (i.hasExtra(AccountUtility.INTENT_LOGOUT)) {
                isLoggingOut = true;
                server.logout();
        } if (i.hasExtra(AccountUtility.INTENT_UNAUTHENTICATED)) {
            ((GlobalApp)getApplication()).voidUser();
        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
    }

    /** Our button click listener, which all the buttons from the layout map to
     * Just determines which button was clicked and responds accordingly */
    public void onButtonClick(View button) {
        switch (button.getId()) {
            case R.id.account_login_button:
                submit();
                break;
            case R.id.account_register_button:
                // Launch the Register interface in a webView straight from the server
                // To keep it as up to date as possible
                Uri uri = Uri.parse(BASE_WEB_URL).buildUpon()
                        .appendPath(REGISTER_EXT)
                        .appendPath(DRIVER_EXT)
                        .build();
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
                break;
            default:
                Log.d(LOG_TAG, "Clicked an unmapped button!");
        }
    }

    /** Takes the user info from the Authentication Screen
     * Contacts the Server through an AsycTask
     * */
    private void submit() {
        final String username = ((EditText) findViewById(R.id.account_username)).getText().toString();
        final String password = ((EditText) findViewById(R.id.account_password)).getText().toString();

        // Validate fields
        String error = null;
        if(!username.contains("@") || !username.contains(".")) {
            // Not an email
            error = ERROR_MESSAGE_EMAIL;
        }
        else if(TextUtils.isEmpty(password)) {
            // No password
            error = ERROR_MESSAGE_PASSWORD;
        }
        if(error != null)
        {
            Toast.makeText(getBaseContext(), error, Toast.LENGTH_SHORT).show();
            // Don't attempt to login if there is an error
            return;
        }

        final String accountType = getIntent().getStringExtra(ARG_ACCOUNT_TYPE);
        new AsyncTask<Void, Void, Intent>() {
            @Override
            protected Intent doInBackground(Void... params) {
                Log.v(LOG_TAG, "Starting user authentication!");
                // Try to Authenticate using the server
                // Bundle up that data
                Bundle data = new Bundle();
                try {
                    String token = AccountUtility.sServerAuthenticate.userSignIn(username, password, mAuthTokenType);
                    data.putString(AccountManager.KEY_ACCOUNT_NAME, username);
                    data.putString(AccountManager.KEY_ACCOUNT_TYPE, accountType);
                    data.putString(AccountManager.KEY_AUTHTOKEN, token);
                    AccountUtility.saveToken(getApplicationContext(), token);
                    data.putString(PARAM_USER_PASS, password);
                } catch (Exception e) {
                    data.putString(KEY_ERROR_MESSAGE, e.getMessage());
                }

                final Intent result = new Intent();
                result.putExtras(data);
                return result;
            }

            @Override
            protected void onPostExecute(Intent intent) {
                // Don't mark the login as complete if there was an error produced
                if(intent.hasExtra(KEY_ERROR_MESSAGE)) {
                    if (intent.getStringExtra(KEY_ERROR_MESSAGE) != null) {
                        Log.e(LOG_TAG, intent.getStringExtra(KEY_ERROR_MESSAGE));
                        Toast.makeText(getBaseContext(), "There was a problem logging you in!", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e(LOG_TAG, "Failed user login!");
                        Toast.makeText(getBaseContext(), "Either username or password is incorrect!", Toast.LENGTH_SHORT).show();
                    }
                }
                else
                    finishLogin(intent);
            }
        }.execute();
    }

    private void finishLogin(Intent intent) {
        Log.v(LOG_TAG, "Finished user authentication!");
        // Check if the auth token is null! This means the user was not successfully logged in
        String token = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN);
        // If the auth wasn't a success, don't add them to the accounts and ask them to login again!
        if (token != null) {
            // Put token in shared Preferences for now!
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            preferences.edit().putString(AUTH_TOKEN_KEY, token).apply();

            // Build the account
            String username = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            String password = intent.getStringExtra(PARAM_USER_PASS);
            final Account newAccount;

            //
            if (intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE) == null)
                newAccount = new Account(username, AccountUtility.ACCOUNT_TYPE);
            else
                newAccount = new Account(username, intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE));

            // Checks if the account is being added to the device or just a re-login
            // Potential Extra value, default value == false
            // Get all accounts and check if they match up
            boolean isAddingAccount = true;
            Account[] accounts = mAccountManager.getAccountsByType(AccountUtility.ACCOUNT_TYPE);
            for(Account a : accounts) {
                if (a.name.equals(newAccount.name))
                    isAddingAccount = false;
            }
            if (getIntent().getBooleanExtra(ARG_IS_ADDING_NEW_ACCOUNT, isAddingAccount)) {
                Log.d(LOG_TAG, "Adding user account!");
                String authtoken = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN);
                String authtokenType = mAuthTokenType;

                // Delete all other accounts but the most recently signed in
                // Only allow one account per phone
                for(Account a : accounts) {
                    if (a.type.intern().equals(AccountUtility.ACCOUNT_TYPE))
                        if (Build.VERSION.SDK_INT >= 22)
                            mAccountManager.removeAccountExplicitly(a);
                        else
                            mAccountManager.removeAccount(a, null, null);
                }

                // Creates the account on the device itself
                mAccountManager.addAccountExplicitly(newAccount, password, intent.getBundleExtra(AccountManager.KEY_USERDATA));
                mAccountManager.setAuthToken(newAccount, authtokenType, authtoken);

            } else {
                Log.d(LOG_TAG, "User is re-logging in!");
                mAccountManager.setPassword(newAccount, password);
            }

            setAccountAuthenticatorResult(intent.getExtras());
            setResult(RESULT_OK, intent);
            // Update the stored info from the server
            server.getCurrentUser();
        } else {
            // Create toast notification to show them that the authentication failed
            Toast.makeText(getBaseContext(), "Either username or password is incorrect!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onTaskCompleted(JSONObject result) {
        try {
            if (result.has(ServerUtility.ERROR_CODE_KEY)) {
                switch (result.getString(ServerUtility.ERROR_CODE_KEY)){
                    case "403":
                        // We've got a serious problem if this happens. They literally just logged in.
                        Log.wtf(LOG_TAG, "They literally just logged in.");
                        break;
                    case "500":
                        Toast.makeText(getApplicationContext(), "Server error!", Toast.LENGTH_SHORT).show();
                        break;
                }
            } else {
                // Is logging in!
                if (!isLoggingOut) {
                    // Track the login!
                    User user = new User(this, result.getJSONObject(ServerUtility.USER_KEY));

                    ((GlobalApp) getApplication()).getDefaultTracker().send(new HitBuilders.EventBuilder()
                                    .setCategory("Login")
                                    .setAction(user.getEmail())
                                    .build()
                    );

                    // If all is good store to the global application!
                    ((GlobalApp) getApplication()).setCurrentUser(user);
                    // Launch an intent for the main activity to refresh everything
                    startActivity(new Intent(AuthActivity.this, MainActivity.class));
                } else {
                    ((GlobalApp) getApplication()).stopTracking();
                    ((GlobalApp) getApplication()).voidUser();
                    isLoggingOut = false;
                }
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Problem getting data from JSON!", e);
        }
    }

}
