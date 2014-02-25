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

import android.webkit.CookieManager;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Sosuke Masui (masui@uphyca.com)
 */
class CookieHandlerAdapter extends CookieHandler {

    private static final String COOKIE = "Cookie";
    private static final String SET_COOKIE = "Set-Cookie";

    private final CookieManager mCookieManager;

    CookieHandlerAdapter() {
        mCookieManager = CookieManager.getInstance();
    }

    @Override
    public Map<String, List<String>> get(URI uri, Map<String, List<String>> requestHeaders) throws IOException {
        String cookie = mCookieManager.getCookie(uri.toString());
        HashMap<String, List<String>> headers = new HashMap<String, List<String>>();
        if (cookie == null) {
            return headers;
        }
        headers.put(COOKIE, Arrays.asList(cookie));
        return headers;
    }

    @Override
    public void put(URI uri, Map<String, List<String>> responseHeaders) throws IOException {
        List<String> cookie = responseHeaders.get(SET_COOKIE);
        if (cookie == null) {
            return;
        }
        for (String each : cookie) {
            mCookieManager.setCookie(uri.toString(), each);
        }
    }
}
