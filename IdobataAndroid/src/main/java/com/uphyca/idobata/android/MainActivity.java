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

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.CookieSyncManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.uphyca.idobata.AuthenticityTokenHandler;

import javax.inject.Inject;

/**
 * @author Sosuke Masui (masui@uphyca.com)
 */
public class MainActivity extends ActionBarActivity {

    private static final int READ_REQUEST_CODE = 42;

    @Inject
    AuthenticityTokenHandler mAuthenticityTokenHandler;

    private WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        InjectionUtils.getObjectGraph(this)
                      .inject(this);
        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        mWebView = new WebView(this);
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setPluginState(WebSettings.PluginState.ON);
        settings.setBuiltInZoomControls(false);
        settings.setAppCacheEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setDomStorageEnabled(true);

        mWebView.addJavascriptInterface(new IdobataInterface(), "$IdobataInterface");

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Uri uri = Uri.parse(url);
                if (uri.getHost()
                       .equals("idobata.io")) {
                    view.loadUrl(url);
                    return true;
                }
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                setSupportProgressBarIndeterminateVisibility(true);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                setSupportProgressBarIndeterminateVisibility(false);
                startService(new Intent(MainActivity.this, IdobataService.class));
                mWebView.loadUrl(new StringBuilder().append("javascript:")
                                                    .append("(function(){")
                                                    .append("var $=Array.prototype.pop.call(document.getElementsByClassName('image-upload-field'));")
                                                    .append("if($ && !$.$weaved){$.addEventListener('click', function(){$IdobataInterface.uploadFile()}, false);$.$weaved=1;}")
                                                    .append("})();")
                                                    .toString());
                mWebView.loadUrl(new StringBuilder().append("javascript:")
                                                    .append("(function(){")
                                                    .append("Array.prototype.forEach.call(document.getElementsByTagName(\"meta\"), function(meta){if(/csrf-token/.test(meta.name))$IdobataInterface.setAuthenticityToken(meta.content)});")
                                                    .append("})();")
                                                    .toString());
            }
        });

        setContentView(mWebView);

        Uri data = getIntent().getData();
        String url = data == null ? "https://idobata.io/" : data.toString();
        mWebView.loadUrl(url);
    }

    @Override
    protected void onResume() {
        super.onResume();
        CookieSyncManager.getInstance()
                         .stopSync();
    }

    @Override
    protected void onPause() {
        CookieSyncManager.getInstance()
                         .startSync();
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == READ_REQUEST_CODE && resultCode == RESULT_OK) {
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                FileUploadService.uploadFile(this, Uri.parse(mWebView.getUrl()), uri);
            }
        }
    }

    private void performFileSearch() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    public class IdobataInterface {

        @JavascriptInterface
        @SuppressWarnings("unused")
        //Used by WebView.addJavascriptInterface()
        public void uploadFile() {
            performFileSearch();
        }

        @JavascriptInterface
        @SuppressWarnings("unused")
        //Used by WebView.addJavascriptInterface()
        public void setAuthenticityToken(String authenticityToken) {
            mAuthenticityTokenHandler.set(authenticityToken);
        }
    }
}
