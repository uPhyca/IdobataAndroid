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
import android.os.Process;
import android.support.v4.app.NotificationCompat;

import com.uphyca.idobata.ErrorListener;
import com.uphyca.idobata.Idobata;
import com.uphyca.idobata.IdobataError;
import com.uphyca.idobata.IdobataStream;
import com.uphyca.idobata.android.InjectionUtils;
import com.uphyca.idobata.android.R;
import com.uphyca.idobata.android.data.api.BackoffPolicy;
import com.uphyca.idobata.android.data.api.Environment;
import com.uphyca.idobata.android.data.api.MessageFilter;
import com.uphyca.idobata.android.data.api.PollingInterval;
import com.uphyca.idobata.android.data.api.StreamConnection;
import com.uphyca.idobata.android.data.prefs.LongPreference;
import com.uphyca.idobata.android.ui.MainActivity;
import com.uphyca.idobata.event.ConnectionEvent;
import com.uphyca.idobata.event.MessageCreatedEvent;
import com.uphyca.idobata.model.Message;
import com.uphyca.idobata.model.MessageBean;
import com.uphyca.idobata.model.Organization;
import com.uphyca.idobata.model.Records;
import com.uphyca.idobata.model.Room;
import com.uphyca.idobata.model.Seed;
import com.uphyca.idobata.model.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static com.uphyca.idobata.android.data.IdobataUtils.findOrganizationById;
import static com.uphyca.idobata.android.data.IdobataUtils.findRoomById;

/**
 * @author Sosuke Masui (masui@uphyca.com)
 */
public class IdobataService extends Service implements IdobataStream.Listener<MessageCreatedEvent>, IdobataStream.ConnectionListener, ErrorListener {

    private static final int OPEN = 0;
    private static final long DELAY_MILLIS_TO_RESTART = TimeUnit.SECONDS.toMillis(5);

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

    @Inject
    Environment mEnvironment;

    @Inject
    @StreamConnection
    BackoffPolicy mBackoffPolicy;

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
        executeAt(mEnvironment.elapsedRealtime() + mPollingIntervalPref.get());
        mServiceHandler.sendEmptyMessage(OPEN);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        cancelPolling();
        closeQuietly();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void closed(ConnectionEvent event) {
        retryToConnect();
    }

    @Override
    public void opened(ConnectionEvent event) {
        mBackoffPolicy.reset();
        try {
            notifyUnreadMessages();
        } catch (IdobataError idobataError) {
            idobataError.printStackTrace();
        }
    }

    @Override
    public void onError(IdobataError error) {
        retryToConnect();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        long cur = mEnvironment.elapsedRealtime();
        executeAt(cur + DELAY_MILLIS_TO_RESTART);
    }

    @Override
    public void onEvent(MessageCreatedEvent event) {
        Message message = eventToMessage(event);
        filterAndNotifyMessage(message, event.getOrganizationSlug(), event.getRoomName());
    }

    private Message eventToMessage(MessageCreatedEvent event) {
        Message message = new MessageBean();
        message.setBody(event.getBody());
        message.setRoomId(event.getRoomId());
        message.setBodyPlain(event.getBodyPlain());
        message.setSenderName(event.getSenderName());
        message.setSenderId(event.getSenderId());
        message.setMentions(event.getMentions());
        message.setCreatedAt(event.getCreatedAt());
        message.setId(event.getId());
        message.setImageUrls(event.getImageUrls());
        message.setMultiline(event.isMultiline());
        message.setSenderIconUrl(event.getSenderIconUrl());
        message.setSenderType(event.getSenderType());
        return message;
    }

    private void notifyUnreadMessages() throws IdobataError {
        List<String> unreadMessageIds = new ArrayList<String>();
        Seed seed = mIdobata.getSeed();
        Records records = seed.getRecords();
        List<Room> rooms = records.getRooms();
        List<Organization> organizations = records.getOrganizations();
        for (Room room : rooms) {
            unreadMessageIds.addAll(room.getUnreadMessageIds());
        }

        List<Message> unreadMessages = mIdobata.getMessages(unreadMessageIds);
        for (Message message : unreadMessages) {

            //bodyPlain is always null when getting message from /api/messages
            message.setBodyPlain(stripTags(message.getBody()));

            Room room = findRoomById(message.getRoomId(), rooms);
            Organization organization = findOrganizationById(room.getOrganizationId(), organizations);
            filterAndNotifyMessage(message, organization.getSlug(), room.getName());
        }
    }

    private String stripTags(String s) {
        return s.replaceAll("<(\"[^\"]*\"|'[^']*'|[^'\">])*>", "");
    }

    private void filterAndNotifyMessage(Message message, String organizationSlug, String roomName) {
        for (MessageFilter each : mMessageFilters) {
            if (each.isSubscribed(mUser, message)) {
                notifyMessage(message, organizationSlug, roomName);
                return;
            }
        }
    }

    private void cancelPolling() {
        PendingIntent pi = buildPendingStartServiceIntent();
        mAlarmManager.cancel(pi);
    }

    private void retryToConnect() {
        if (mBackoffPolicy.isFailed()) {
            stopSelf();
            return;
        }

        long nextBackOffMillis = mBackoffPolicy.getNextBackOffMillis();
        try {
            executeAt(nextBackOffMillis);
        } finally {
            mBackoffPolicy.backoff();
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

    private void executeAt(long triggerAtMillis) {
        PendingIntent pi = buildPendingStartServiceIntent();
        mAlarmManager.cancel(pi);
        mAlarmManager.set(AlarmManager.ELAPSED_REALTIME, triggerAtMillis, pi);
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

    private CharSequence buildTitle(String organizationSlug, String roomName) {
        return new StringBuilder().append(organizationSlug)
                                  .append(' ')
                                  .append('/')
                                  .append(' ')
                                  .append(roomName);
    }

    private CharSequence buildText(Message message) {
        return new StringBuilder().append(message.getSenderName())
                                  .append(':')
                                  .append(' ')
                                  .append(message.getBodyPlain())
                                  .toString();
    }

    private PendingIntent buildPendingStartServiceIntent() {
        final Intent intent = new Intent(IdobataService.this, IdobataService.class);
        return PendingIntent.getService(IdobataService.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent buildPendingStartActivityIntent(Message message, String organizationSlug, String roomName) {

        final Intent intent = new Intent(IdobataService.this, MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setData(Uri.parse(String.format("https://idobata.io/#/organization/%s/room/%s", organizationSlug, roomName)));
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

    private void notifyMessage(Message message, String organizationSlug, String roomName) {
        PendingIntent pi = buildPendingStartActivityIntent(message, organizationSlug, roomName);
        CharSequence title = buildTitle(organizationSlug, roomName);
        CharSequence text = buildText(message);
        Notification notification = buildNotification(pi, title, text);
        mNotificationManager.notify(R.string.app_name, notification);
    }

    private final class ServiceHandler extends Handler {

        private ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(android.os.Message msg) {
            try {
                open();
            } catch (IdobataError idobataError) {
                onError(idobataError);
            }
        }
    }
}
