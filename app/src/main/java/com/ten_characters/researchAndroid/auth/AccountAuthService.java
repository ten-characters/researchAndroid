package com.ten_characters.researchAndroid.auth;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by austin on 31/05/15.
 */
public class AccountAuthService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        PalletAuthenticator authenticator = new PalletAuthenticator(this);
        return authenticator.getIBinder();
    }
}
