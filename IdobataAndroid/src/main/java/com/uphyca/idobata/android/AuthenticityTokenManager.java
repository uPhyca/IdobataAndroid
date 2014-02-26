
package com.uphyca.idobata.android;

import com.uphyca.idobata.AuthenticityTokenHandler;

public class AuthenticityTokenManager implements AuthenticityTokenHandler {

    private final StringPreference mAuthenticityTokenPref;

    public AuthenticityTokenManager(StringPreference authenticityTokenPref) {
        mAuthenticityTokenPref = authenticityTokenPref;
    }

    @Override
    public String get() {
        return mAuthenticityTokenPref.get();
    }

    @Override
    public void set(String authenticityToken) {
        mAuthenticityTokenPref.set(authenticityToken);
    }
}
