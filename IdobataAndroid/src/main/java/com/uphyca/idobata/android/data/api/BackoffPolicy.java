
package com.uphyca.idobata.android.data.api;

public interface BackoffPolicy {

    void backoff();

    void reset();

    long getNextBackOffMillis();

    boolean isFailed();
}
