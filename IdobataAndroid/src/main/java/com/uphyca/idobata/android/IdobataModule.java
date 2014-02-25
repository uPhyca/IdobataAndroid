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

import com.squareup.okhttp.OkHttpClient;
import com.uphyca.idobata.CookieAuthenticator;
import com.uphyca.idobata.Idobata;
import com.uphyca.idobata.IdobataBuilder;
import com.uphyca.idobata.RequestInterceptor;
import com.uphyca.idobata.http.Client;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

/**
 * @author Sosuke Masui (masui@uphyca.com)
 */
@Module(injects = IdobataService.class)
public class IdobataModule {

    @Provides
    @Singleton
    OkHttpClient provideOkHttpClient() {
        return new OkHttpClient();
    }

    @Provides
    @Singleton
    Client provideClient(OkHttpClient okHttpClient) {
        return new OkClient(okHttpClient);
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
    RequestInterceptor provideRequestInterceptor() {
        return new CookieAuthenticator(new CookieHandlerAdapter());
    }
}
