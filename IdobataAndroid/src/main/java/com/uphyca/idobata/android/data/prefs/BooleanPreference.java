
package com.uphyca.idobata.android.data.prefs;

import android.content.SharedPreferences;

public class BooleanPreference {

    private final SharedPreferences mSharedPreferences;
    private final String mKey;
    private final boolean mDefaultValue;

    public BooleanPreference(SharedPreferences sharedPreferences, String key) {
        this(sharedPreferences, key, false);
    }

    public BooleanPreference(SharedPreferences sharedPreferences, String key, boolean defaultValue) {
        mSharedPreferences = sharedPreferences;
        mKey = key;
        mDefaultValue = defaultValue;
    }

    public boolean get() {
        return mSharedPreferences.getBoolean(mKey, mDefaultValue);
    }

    public boolean isSet() {
        return mSharedPreferences.contains(mKey);
    }

    public void set(boolean value) {
        mSharedPreferences.edit()
                          .putBoolean(mKey, value)
                          .apply();
    }

    public void delete() {
        mSharedPreferences.edit()
                          .remove(mKey)
                          .apply();
    }
}
