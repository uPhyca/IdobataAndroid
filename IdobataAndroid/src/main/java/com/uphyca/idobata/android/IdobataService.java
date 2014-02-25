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

package com.uphyca.idobata.android;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.uphyca.idobata.Idobata;
import com.uphyca.idobata.IdobataError;
import com.uphyca.idobata.IdobataStream;
import com.uphyca.idobata.event.MessageCreatedEvent;
import com.uphyca.idobata.model.Seed;
import com.uphyca.idobata.model.User;

import java.io.IOException;

import javax.inject.Inject;

/**
 * @author Sosuke Masui (masui@uphyca.com)
 */
public class IdobataService extends Service {

    @Inject
    Idobata mIdobata;

    private IdobataStream mStream;
    private Seed mSeed;

    @Override
    public void onCreate() {
        super.onCreate();
        InjectionUtils.getObjectGraph(this)
                      .inject(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        subscribe();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mStream != null) {
            try {
                mStream.close();
            } catch (IOException ignore) {
            }
        }
        super.onDestroy();
    }

    private void subscribe() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mSeed = mIdobata.getSeed();
                    open();
                } catch (IdobataError idobataError) {
                    idobataError.printStackTrace();
                    stopSelf();
                }
            }
        }).start();
    }

    private void open() throws IdobataError {
        IdobataStream.Listener<MessageCreatedEvent> notifier = new IdobataStream.Listener<MessageCreatedEvent>() {
            @Override
            public void onEvent(MessageCreatedEvent event) {
                PendingIntent pi = buildPendingIntent();
                CharSequence title = buildTitle(event);
                CharSequence text = buildText(event);
                Notification notification = buildNotification(pi, title, text);
                NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                notificationManager.notify(R.string.app_name, notification);
            }
        };
        mStream = mIdobata.openStream()
                          .subscribeMessageCreated(new MentionFilter(mSeed.getRecords()
                                                                          .getUser(), notifier));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private CharSequence buildTitle(MessageCreatedEvent event) {
        return new StringBuilder().append(event.getOrganizationSlug())
                                  .append('/')
                                  .append(event.getRoomName());
    }

    private CharSequence buildText(MessageCreatedEvent event) {
        return new StringBuilder().append(event.getSenderName())
                                  .append(':')
                                  .append(' ')
                                  .append(event.getBodyPlain())
                                  .toString();
    }

    private PendingIntent buildPendingIntent() {
        final Intent intent = new Intent(IdobataService.this, MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        return PendingIntent.getActivity(IdobataService.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private Notification buildNotification(PendingIntent pi, CharSequence title, CharSequence text) {
        return new NotificationCompat.Builder(IdobataService.this).setSmallIcon(R.drawable.ic_stat_notification)
                                                                  .setContentTitle(title)
                                                                  .setContentText(text)
                                                                  .setAutoCancel(true)
                                                                  .setContentIntent(pi)
                                                                  .build();
    }

    private static class MentionFilter implements IdobataStream.Listener<MessageCreatedEvent> {

        private final User mUser;
        private final IdobataStream.Listener<MessageCreatedEvent> mDelegate;

        private MentionFilter(User user, IdobataStream.Listener<MessageCreatedEvent> delegate) {
            mUser = user;
            mDelegate = delegate;
        }

        @Override
        public void onEvent(MessageCreatedEvent event) {
            if (!event.getMentions()
                      .contains(mUser.getId())) {
                return;
            }
            mDelegate.onEvent(event);
        }
    }
}
