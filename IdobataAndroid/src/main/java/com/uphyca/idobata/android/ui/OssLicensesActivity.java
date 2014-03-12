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

package com.uphyca.idobata.android.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.ListFragment;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.uphyca.idobata.android.InjectionUtils;
import com.uphyca.idobata.android.R;
import com.uphyca.idobata.android.data.api.Main;
import com.uphyca.idobata.android.data.api.Networking;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;

import javax.inject.Inject;

/**
 * @author Sosuke Masui (masui@uphyca.com)
 */
public class OssLicensesActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_oss_licenses);
    }

    public static class OssLicensesListFragment extends ListFragment {

        private static final String LICENSE_FILE_TEMPLATE = "licenses/%s/LICENSE.txt";
        private static final String LICENSE_TAG = "license";

        private String[] mTitles;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mTitles = getResources().getStringArray(R.array.oss_titles);
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, mTitles);
            setListAdapter(adapter);
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            Resources res = getResources();
            String key = res.getStringArray(R.array.oss_licenses)[position];
            String fileName = String.format(LICENSE_FILE_TEMPLATE, key);
            String title = getString(R.string.label_oss_license, mTitles[position]);

            FragmentManager fm = getFragmentManager();
            DialogFragment prev = (DialogFragment) fm.findFragmentByTag(LICENSE_TAG);
            if (prev != null) {
                prev.dismiss();
            }
            LicenseDialogFragment.newInstance(title, fileName)
                                 .show(fm, LICENSE_TAG);
        }
    }

    public static class LicenseDialogFragment extends DialogFragment {

        private static final String ARGS_TITLE = "title";
        private static final String ARGS_FILE_NAME = "file_name";

        public static LicenseDialogFragment newInstance(String title, String fileName) {
            LicenseDialogFragment f = new LicenseDialogFragment();
            Bundle args = new Bundle();
            args.putString(ARGS_TITLE, title);
            args.putString(ARGS_FILE_NAME, fileName);
            f.setArguments(args);
            return f;
        }

        @Inject
        @Networking
        Executor mExecutor;

        @Inject
        @Main
        Executor mDispatcher;

        private String mTitle;
        private String mFileName;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Bundle args = getArguments();
            mTitle = args.getString(ARGS_TITLE);
            mFileName = args.getString(ARGS_FILE_NAME);
            InjectionUtils.getObjectGraph(getActivity())
                          .inject(this);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.fragment_oss_licenses, null, false);
            final TextView text = (TextView) view.findViewById(R.id.license);
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    final String license = loadLicense();
                    mDispatcher.execute(new Runnable() {
                        @Override
                        public void run() {
                            text.setText(license);
                        }
                    });
                }
            });
            builder.setTitle(mTitle)
                   .setView(view);
            return builder.create();
        }

        private String loadLicense() {
            InputStream is = null;
            ByteArrayOutputStream os = null;
            try {
                is = getResources().getAssets()
                                   .open(mFileName);
                os = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];

                for (int count; (count = is.read(buf)) > -1;) {
                    os.write(buf, 0, count);
                }
                return os.toString("UTF-8");
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                closeQuietly(is);
                closeQuietly(os);
            }
        }

        private void closeQuietly(Closeable resource) {
            if (resource == null) {
                return;
            }
            try {
                resource.close();
            } catch (IOException ignore) {
            }
        }
    }

}
