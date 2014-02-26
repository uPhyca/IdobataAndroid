
package com.uphyca.idobata.android;

import android.content.SharedPreferences;

public class StringPreference {

    private final SharedPreferences mSharedPreferences;
    private final String mKey;
    private final String mDefaultValue;

    public StringPreference(SharedPreferences sharedPreferences, String key) {
        this(sharedPreferences, key, null);
    }

    public StringPreference(SharedPreferences sharedPreferences, String key, String defaultValue) {
        mSharedPreferences = sharedPreferences;
        mKey = key;
        mDefaultValue = defaultValue;
    }

    public String get() {
        return mSharedPreferences.getString(mKey, mDefaultValue);
    }

    public boolean isSet() {
        return mSharedPreferences.contains(mKey);
    }

    public void set(String value) {
        mSharedPreferences.edit()
                          .putString(mKey, value)
                          .apply();
    }

    public void delete() {
        mSharedPreferences.edit()
                          .remove(mKey)
                          .apply();
    }
}
