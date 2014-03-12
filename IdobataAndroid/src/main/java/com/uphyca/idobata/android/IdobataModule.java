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

import android.app.AlarmManager;
import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.CookieManager;

import com.squareup.okhttp.HttpResponseCache;
import com.squareup.okhttp.OkHttpClient;
import com.uphyca.idobata.CookieAuthenticator;
import com.uphyca.idobata.Idobata;
import com.uphyca.idobata.IdobataBuilder;
import com.uphyca.idobata.RequestInterceptor;
import com.uphyca.idobata.android.data.AndroidEnvironment;
import com.uphyca.idobata.android.data.AndroidExecutor;
import com.uphyca.idobata.android.data.CookieHandlerAdapter;
import com.uphyca.idobata.android.data.ExponentialBackoff;
import com.uphyca.idobata.android.data.MentionsFilter;
import com.uphyca.idobata.android.data.OkClient;
import com.uphyca.idobata.android.data.api.BackoffPolicy;
import com.uphyca.idobata.android.data.api.Environment;
import com.uphyca.idobata.android.data.api.Main;
import com.uphyca.idobata.android.data.api.MessageFilter;
import com.uphyca.idobata.android.data.api.Networking;
import com.uphyca.idobata.android.data.api.NotificationsEffectsLEDFlash;
import com.uphyca.idobata.android.data.api.NotificationsEffectsSound;
import com.uphyca.idobata.android.data.api.NotificationsEffectsVibrate;
import com.uphyca.idobata.android.data.api.NotificationsMentions;
import com.uphyca.idobata.android.data.api.PollingInterval;
import com.uphyca.idobata.android.data.api.StreamConnection;
import com.uphyca.idobata.android.data.prefs.BooleanPreference;
import com.uphyca.idobata.android.data.prefs.LongPreference;
import com.uphyca.idobata.android.service.IdobataService;
import com.uphyca.idobata.android.service.PostImageService;
import com.uphyca.idobata.android.service.PostTextService;
import com.uphyca.idobata.android.service.PostTouchService;
import com.uphyca.idobata.android.ui.MainActivity;
import com.uphyca.idobata.android.ui.NotificationsSettingActivity;
import com.uphyca.idobata.android.ui.OssLicensesActivity;
import com.uphyca.idobata.android.ui.SendTo;
import com.uphyca.idobata.http.Client;

import java.io.File;
import java.io.IOException;
import java.net.CookieHandler;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * @author Sosuke Masui (masui@uphyca.com)
 */
@Module(injects = {
        IdobataService.class, //
        PostImageService.class, //
        PostTextService.class, //
        PostTouchService.class, //
        MainActivity.class, //
        SendTo.Rooms.class, //
        OssLicensesActivity.LicenseDialogFragment.class, //
        NotificationsSettingActivity.PrefsFragment.class, //
        MentionsFilter.class, //
})
public class IdobataModule {

    public static final String PREFS_NAME = "prefs";

    private final Application mApplication;
    private static final long DEFAULT_POLLING_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(5);
    private static final long BACKOFF_SLEEP_MILLIS = TimeUnit.SECONDS.toMillis(5);
    private static final int BACKOFF_TRIES = Integer.MAX_VALUE;

    public IdobataModule(Application application) {
        mApplication = application;
    }

    @Provides
    @Singleton
    OkHttpClient provideOkHttpClient() {
        OkHttpClient okHttpClient = new OkHttpClient();
        File cacheDir = new File(mApplication.getCacheDir(), "okhttp");
        final HttpResponseCache cache;
        try {
            cache = new HttpResponseCache(cacheDir, 10 * 1024 * 1024);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        okHttpClient.setResponseCache(cache);
        return okHttpClient;
    }

    @Provides
    @Singleton
    Client provideClient(OkHttpClient okHttpClient, CookieHandler cookieHandler) {
        return new OkClient(okHttpClient, cookieHandler);
    }

    @Provides
    @Singleton
    Idobata provideIdobata(RequestInterceptor requestInterceptor, Client client) {
        return new IdobataBuilder().setRequestInterceptor(requestInterceptor)
                                   .setClient(client)
                                   .build();
    }

    @Provides
    @Singleton
    CookieHandler provideCookieHandler() {
        return new CookieHandlerAdapter(CookieManager.getInstance());
    }

    @Provides
    @Singleton
    RequestInterceptor provideRequestInterceptor() {
        return new CookieAuthenticator();
    }

    @Provides
    @Singleton
    NotificationManager provideNotificationManager() {
        return (NotificationManager) mApplication.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Provides
    @Singleton
    AlarmManager provideAlarmManager() {
        return (AlarmManager) mApplication.getSystemService(Context.ALARM_SERVICE);
    }

    @Provides
    @Singleton
    SharedPreferences provideSharedPreferences() {
        return mApplication.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Provides
    @Singleton
    @PollingInterval
    LongPreference providePollingIntervalPreference(SharedPreferences pref) {
        return new LongPreference(pref, "polling_interval", DEFAULT_POLLING_INTERVAL_MILLIS);
    }

    @Provides
    @Singleton
    @NotificationsMentions
    BooleanPreference provideNotificationsMentionsPreference(SharedPreferences pref) {
        return new BooleanPreference(pref, "notifications_mentions", false);
    }

    @Provides
    @Singleton
    @NotificationsEffectsVibrate
    BooleanPreference provideNotificationsEffectsVibratePreference(SharedPreferences pref) {
        return new BooleanPreference(pref, "notifications_effects_vibrate", false);
    }

    @Provides
    @Singleton
    @NotificationsEffectsLEDFlash
    BooleanPreference provideNotificationsEffectsLEDFlashPreference(SharedPreferences pref) {
        return new BooleanPreference(pref, "notifications_effects_led_flash", false);
    }

    @Provides
    @Singleton
    @NotificationsEffectsSound
    BooleanPreference provideNotificationsEffectsSoundPreference(SharedPreferences pref) {
        return new BooleanPreference(pref, "notifications_effects_sound", false);
    }

    @Provides(type = Provides.Type.SET)
    @Singleton
    MessageFilter provideMentionFilter(MentionsFilter filter) {
        return filter;
    }

    @Provides
    @Singleton
    @Networking
    Executor provideHttpExecutor() {
        return Executors.newCachedThreadPool();
    }

    @Provides
    @Singleton
    @Main
    Executor provideUiExecutor() {
        return new AndroidExecutor();
    }

    @Provides
    @Singleton
    Environment provideEnvironment() {
        return new AndroidEnvironment();
    }

    @Provides
    @Singleton
    @StreamConnection
    BackoffPolicy provideBackoffPolicy(Environment environment) {
        return new ExponentialBackoff(environment, BACKOFF_SLEEP_MILLIS, BACKOFF_TRIES);
    }
}
