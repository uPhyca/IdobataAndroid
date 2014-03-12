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
import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.uphyca.idobata.android.IdobataModule;
import com.uphyca.idobata.android.R;

/**
 * @author Sosuke Masui (masui@uphyca.com)
 */
public class NotificationsSettingActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications_settings);
    }

    public static class PrefsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getPreferenceManager().setSharedPreferencesName("");
            getPreferenceManager().setSharedPreferencesName(IdobataModule.PREFS_NAME);
            addPreferencesFromResource(R.xml.prefs_notifications_setting);
        }
    }
}
