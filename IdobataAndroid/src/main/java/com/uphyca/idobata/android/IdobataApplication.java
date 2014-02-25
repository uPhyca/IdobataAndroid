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

import android.app.Application;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import dagger.ObjectGraph;

/**
 * @author Sosuke Masui (masui@uphyca.com)
 */
public class IdobataApplication extends Application {

    private ObjectGraph mObjectGraph;

    @Override
    public void onCreate() {
        super.onCreate();
        CookieSyncManager.createInstance(this);
        CookieManager.getInstance()
                     .acceptCookie();
        mObjectGraph = ObjectGraph.create(modules());
    }

    Object[] modules() {
        return new Object[] {
            new IdobataModule()
        };
    }

    ObjectGraph getObjectGraph() {
        return mObjectGraph;
    }
}
