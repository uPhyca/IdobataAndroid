
package com.uphyca.idobata.android.data;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;

public class AndroidExecutor implements Executor {

    private final Handler mHandler;

    public AndroidExecutor() {
        mHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void execute(Runnable command) {
        mHandler.post(command);
    }
}
