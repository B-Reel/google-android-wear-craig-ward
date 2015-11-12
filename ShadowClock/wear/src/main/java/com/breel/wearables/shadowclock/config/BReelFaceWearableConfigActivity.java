/*
 * Copyright (C) 2015 B-Reel
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

package com.breel.wearables.shadowclock.config;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ToggleButton;

import com.breel.wearables.shadowclock.BReelWatchFaceService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Wearable;

import com.breel.wearables.shadowclock.R;

/**
 * The watch-side config activity for {@link BReelWatchFaceService}, which allows for setting the
 * background color.
 */
public class BReelFaceWearableConfigActivity extends Activity {

    private static final String TAG = "BREEL_CONFIG_ACTIVITY";

    private GoogleApiClient mGoogleApiClient;

    private ToggleButton toggle1224Mode;
    private ImageView mImage12Mode;
    private ImageView mImage24Mode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_watch_face_config);

        Log.d(TAG, "onCreate");

        this.toggle1224Mode = (ToggleButton) findViewById(R.id.hourButton);
        this.mImage12Mode = (ImageView) findViewById(R.id.on_12);
        this.mImage24Mode = (ImageView) findViewById(R.id.on_24);

        this.toggle1224Mode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                updateConfigDataItem(BReelWatchFaceUtil.KEY_HOUR_FORMAT_TYPE, String.valueOf(toggle1224Mode.isChecked()));
                mImage24Mode.setVisibility(toggle1224Mode.isChecked() ? View.VISIBLE : View.INVISIBLE);
                mImage12Mode.setVisibility(toggle1224Mode.isChecked() ? View.INVISIBLE : View.VISIBLE);
                finish();
            }
        });

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        updateConfigDataItemAndUiOnStartup();
                    }

                    @Override
                    public void onConnectionSuspended(int cause) {
                        // Do something with the suspended connection...
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        // Do something with the failed connection...
                    }
                })
                .addApi(Wearable.API)
                .build();
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null && !mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }


    @Override
    protected void onStop() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }


    private void updateConfigDataItem(String key, String value) {

        Log.d(TAG, key + " - updateConfigDataItem - " + value);

        DataMap configKeysToOverwrite = new DataMap();
        configKeysToOverwrite.putString(key, value);
        BReelWatchFaceUtil.overwriteKeysInConfigDataMap(mGoogleApiClient, configKeysToOverwrite);
    }


    private void updateConfigDataItemAndUiOnStartup() {
        BReelWatchFaceUtil.fetchConfigDataMap(mGoogleApiClient,
                new BReelWatchFaceUtil.FetchConfigDataMapCallback() {
                    @Override
                    public void onConfigDataMapFetched(DataMap startupConfig) {
                        Log.d(TAG, "startupConfig: " + String.valueOf(startupConfig));
                        String twentyFourClockString = startupConfig.getString(BReelWatchFaceUtil.KEY_HOUR_FORMAT_TYPE);
                        toggle1224Mode.setChecked(Boolean.valueOf((twentyFourClockString != null ? twentyFourClockString : "false")));
                        toggle1224Mode.setEnabled(true);

                        mImage24Mode.setVisibility(toggle1224Mode.isChecked() ? View.VISIBLE : View.INVISIBLE);
                        mImage12Mode.setVisibility(toggle1224Mode.isChecked() ? View.INVISIBLE : View.VISIBLE);
                    }
                }
        );
    }
}
