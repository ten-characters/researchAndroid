package com.ten_characters.researchAndroid.auth;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.ten_characters.researchAndroid.activities.AuthActivity;
import static com.ten_characters.researchAndroid.auth.AccountUtility.sServerAuthenticate;

/**
 * Created by austin on 31/05/15.
 */
public class PalletAuthenticator extends AbstractAccountAuthenticator {

    private static final String LOG_TAG = PalletAuthenticator.class.getSimpleName();

    private Context mContext;

    public PalletAuthenticator(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        return null;
    }

    /** Creates a bundle to start our accountAuthActivity when a user creates a new account on their phonical device */
    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType,
                             String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {

        final Intent intent = new Intent(mContext, AuthActivity.class);

        intent.putExtra(AuthActivity.ARG_ACCOUNT_TYPE, accountType);
        intent.putExtra(AuthActivity.ARG_AUTH_TYPE, authTokenType);
        intent.putExtra(AuthActivity.ARG_IS_ADDING_NEW_ACCOUNT, true);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);

        return bundle;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account,
                               String authTokenType, Bundle options) throws NetworkErrorException {

        // Get the username and password using the Account Manager
        final AccountManager accountManager = AccountManager.get(mContext);

        String authToken = accountManager.peekAuthToken(account, authTokenType);

        // Try again to get an auth token if we do not have one already
        if(TextUtils.isEmpty(authToken)) {
            // Try to get Credentials from the account
            final String password = accountManager.getPassword(account);
            if(password != null) {
                // Try to authenticate the account with the server
                try {
                    authToken  = sServerAuthenticate.userSignIn(account.name, password, authTokenType);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Problem getting auth token.", e);
                }

            }
        }

        // If we did indeed authenticate the user, return the result
        if(!TextUtils.isEmpty(authToken)) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
            return result;
        }

        // If the user was not authenticated
        // --> ask the user to re-login
        // aka launch our authentication activity
        final Intent intent = new Intent(mContext, AuthActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        intent.putExtra(AuthActivity.ARG_ACCOUNT_TYPE, account.type);
        intent.putExtra(AuthActivity.ARG_AUTH_TYPE, authTokenType);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
        return null;
    }
}
