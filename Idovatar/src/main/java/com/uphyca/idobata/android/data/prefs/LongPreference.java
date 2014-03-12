
package com.uphyca.idobata.android.data.prefs;

import android.content.SharedPreferences;

public class LongPreference {

    private final SharedPreferences mSharedPreferences;
    private final String mKey;
    private final long mDefaultValue;

    public LongPreference(SharedPreferences sharedPreferences, String key) {
        this(sharedPreferences, key, 0L);
    }

    public LongPreference(SharedPreferences sharedPreferences, String key, long defaultValue) {
        mSharedPreferences = sharedPreferences;
        mKey = key;
        mDefaultValue = defaultValue;
    }

    public long get() {
        return mSharedPreferences.getLong(mKey, mDefaultValue);
    }

    public boolean isSet() {
        return mSharedPreferences.contains(mKey);
    }

    public void set(long value) {
        mSharedPreferences.edit()
                          .putLong(mKey, value)
                          .apply();
    }

    public void delete() {
        mSharedPreferences.edit()
                          .remove(mKey)
                          .apply();
    }
}
