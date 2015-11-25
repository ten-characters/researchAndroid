package com.ten_characters.researchAndroid.auth;

/**
 * Created by austin on 1/06/15.
 */
public interface ServerAuthenticate {
    String userSignIn(final String user, final String pass, String authType) throws Exception;
}
