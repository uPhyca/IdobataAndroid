/*
 * Copyright (C) 2014 uPhyca Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.uphyca.idobata.android.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;

import com.uphyca.idobata.ErrorListener;
import com.uphyca.idobata.Idobata;
import com.uphyca.idobata.IdobataError;
import com.uphyca.idobata.IdobataStream;
import com.uphyca.idobata.android.InjectionUtils;
import com.uphyca.idobata.android.R;
import com.uphyca.idobata.android.data.api.MessageFilter;
import com.uphyca.idobata.android.data.api.PollingInterval;
import com.uphyca.idobata.android.data.prefs.LongPreference;
import com.uphyca.idobata.android.ui.MainActivity;
import com.uphyca.idobata.event.ConnectionEvent;
import com.uphyca.idobata.event.MessageCreatedEvent;
import com.uphyca.idobata.model.User;

import java.io.IOException;
import java.util.Set;

import javax.inject.Inject;

/**
 * @author Sosuke Masui (masui@uphyca.com)
 */
public class IdobataService extends Service implements IdobataStream.Listener<MessageCreatedEvent>, IdobataStream.ConnectionListener, ErrorListener {

    private static final String TAG = "Idobata";
    private static final int OPEN = 0;

    @Inject
    Idobata mIdobata;

    @Inject
    AlarmManager mAlarmManager;

    @Inject
    NotificationManager mNotificationManager;

    @Inject
    Set<MessageFilter> mMessageFilters;

    @Inject
    @PollingInterval
    LongPreference mPollingIntervalPref;

    private Looper mServiceLooper;

    private ServiceHandler mServiceHandler;

    private User mUser;

    private IdobataStream mStream;

    @Override
    public void onCreate() {
        super.onCreate();
        ensureHandler();
        InjectionUtils.getObjectGraph(this)
                      .inject(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        PendingIntent pi = buildPendingStartServiceIntent();
        mAlarmManager.cancel(pi);
        mServiceHandler.sendEmptyMessage(OPEN);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        closeQuietly();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void closed(ConnectionEvent event) {
    }

    @Override
    public void opened(ConnectionEvent event) {
    }

    @Override
    public void onError(IdobataError error) {
    }

    @Override
    public void onEvent(MessageCreatedEvent event) {
        for (MessageFilter each : mMessageFilters) {
            if (each.isSubscribed(mUser, event)) {
                notifyEvent(event);
                return;
            }
        }
    }

    private void closeQuietly() {
        if (mStream == null) {
            return;
        }
        try {
            mStream.setConnectionListener(null)
                   .setErrorListener(null)
                   .close();
            mStream = null;
        } catch (IOException ignore) {
        }
    }

    private void executeNext() {
        long currentTime = SystemClock.elapsedRealtime();
        PendingIntent pi = buildPendingStartServiceIntent();
        mAlarmManager.set(AlarmManager.ELAPSED_REALTIME, currentTime + mPollingIntervalPref.get(), pi);
    }

    private void ensureHandler() {
        HandlerThread t = new HandlerThread("IdobataService", Process.THREAD_PRIORITY_BACKGROUND);
        t.start();
        mServiceLooper = t.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    private void open() throws IdobataError {
        if (mUser == null) {
            mUser = mIdobata.getSeed()
                            .getRecords()
                            .getUser();
        }
        if (mStream == null) {
            mStream = mIdobata.openStream()
                              .setErrorListener(this)
                              .setConnectionListener(this)
                              .subscribeMessageCreated(this);
        }
        mStream.open();
    }

    private CharSequence buildTitle(MessageCreatedEvent event) {
        return new StringBuilder().append(event.getOrganizationSlug())
                                  .append(' ')
                                  .append('/')
                                  .append(' ')
                                  .append(event.getRoomName());
    }

    private CharSequence buildText(MessageCreatedEvent event) {
        return new StringBuilder().append(event.getSenderName())
                                  .append(':')
                                  .append(' ')
                                  .append(event.getBodyPlain())
                                  .toString();
    }

    private PendingIntent buildPendingStartServiceIntent() {
        final Intent intent = new Intent(IdobataService.this, IdobataService.class);
        return PendingIntent.getService(IdobataService.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent buildPendingStartActivityIntent(MessageCreatedEvent event) {
        final Intent intent = new Intent(IdobataService.this, MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setData(Uri.parse(String.format("https://idobata.io/#/organization/%s/room/%s", event.getOrganizationSlug(), event.getRoomName())));
        return PendingIntent.getActivity(IdobataService.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private Notification buildNotification(PendingIntent pi, CharSequence title, CharSequence text) {
        return new NotificationCompat.Builder(IdobataService.this).setSmallIcon(R.drawable.ic_stat_notification)
                                                                  .setContentTitle(title)
                                                                  .setContentText(text)
                                                                  .setTicker(title)
                                                                  .setAutoCancel(true)
                                                                  .setContentIntent(pi)
                                                                  .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND | Notification.FLAG_SHOW_LIGHTS)
                                                                  .build();
    }

    private void notifyEvent(MessageCreatedEvent event) {
        PendingIntent pi = buildPendingStartActivityIntent(event);
        CharSequence title = buildTitle(event);
        CharSequence text = buildText(event);
        Notification notification = buildNotification(pi, title, text);
        mNotificationManager.notify(R.string.app_name, notification);
    }

    private final class ServiceHandler extends Handler {

        private ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            try {
                open();
            } catch (IdobataError idobataError) {
                onError(idobataError);
            } finally {
                executeNext();
            }
        }
    }
}
