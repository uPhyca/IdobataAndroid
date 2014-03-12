
package com.uphyca.idobata.android.data;

import com.uphyca.idobata.android.data.api.BackoffPolicy;
import com.uphyca.idobata.android.data.api.Environment;

public class ExponentialBackoff implements BackoffPolicy {

    private static final double MULTPLIER = 2;

    private final Environment mEnvironment;
    private final long mInitialSleepTimeMillis;
    private final int mMaxTries;

    private long mSleepTimeMillis;
    private int mBackoffCount;

    public ExponentialBackoff(Environment environment, long initialSleepMillis, int maxTries) {
        this.mEnvironment = environment;
        this.mInitialSleepTimeMillis = initialSleepMillis;
        this.mMaxTries = maxTries;
        reset();
    }

    @Override
    public void backoff() {
        mSleepTimeMillis *= MULTPLIER;
        ++mBackoffCount;
    }

    @Override
    public void reset() {
        mSleepTimeMillis = mInitialSleepTimeMillis;
        mBackoffCount = 0;
    }

    @Override
    public long getNextBackOffMillis() {
        return mEnvironment.elapsedRealtime() + mSleepTimeMillis;
    }

    @Override
    public boolean isFailed() {
        return mBackoffCount >= mMaxTries;
    }
}