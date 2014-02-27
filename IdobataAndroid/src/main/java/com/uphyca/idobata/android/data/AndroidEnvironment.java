
package com.uphyca.idobata.android.data;

import android.os.SystemClock;

import com.uphyca.idobata.android.data.api.Environment;

public class AndroidEnvironment implements Environment {

    @Override
    public long elapsedRealtime() {
        return SystemClock.elapsedRealtime();
    }

    @Override
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
