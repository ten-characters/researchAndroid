package com.ten_characters.researchAndroid.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.ten_characters.researchAndroid.server.PalletServer;
import com.ten_characters.researchAndroid.server.ServerUtility;

/**
 * Created by austin on 1/06/15.
 */
public class AccountUtility {

    private static final String LOG_TAG = AccountUtility.class.getSimpleName();

    public static final String INTENT_LOGOUT = "logout";
    public static final String INTENT_UNAUTHENTICATED = "unauthenticated";


    public static final String ACCOUNT_TYPE = "com.truckpallet.pallet";
    public static final String ACCOUNT_NAME = "Pallet";

    /**
     * AUTH TOKENS
     */
    // For when a user is NOT logged in
    //public static final String AUTHTOKEN_TYPE_VIEW_ONLY = "View only";
    //public static final String AUTHTOKEN_TYPE_VIEW_ONLY_LABEL = "View only access to Pallet";

    // For when a user IS logged in
    public static final String AUTHTOKEN_TYPE_FULL_ACCESS = "Full access";
    public static final String AUTHTOKEN_TYPE_FULL_ACCESS_LABEL = "Full access to Pallet";

    public static final ServerAuthenticate sServerAuthenticate = new PalletServer();

    public static void saveToken(Context context, String token) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().remove(ServerUtility.AUTH_TOKEN_KEY).apply();
        preferences.edit().putString(ServerUtility.AUTH_TOKEN_KEY, token).apply();
    }

    public static String getToken(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(ServerUtility.AUTH_TOKEN_KEY, null);
    }
}


