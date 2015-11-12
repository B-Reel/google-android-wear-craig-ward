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

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.concurrent.TimeUnit;

/**
 * A {@link com.google.android.gms.wearable.WearableListenerService} listening for {@link BReelWatchFaceUtil} config messages and
 * updating the config {@link com.google.android.gms.wearable.DataItem} accordingly.
 */
public class BReelWatchFaceConfigListenerService extends WearableListenerService
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "DigitalListenerService";

    private GoogleApiClient mGoogleApiClient;

    /**
     * Receive message from mobile app... check for more details: https://developer.android.com/training/wearables/data-layer/messages.html
     * @param messageEvent
     */
    @Override // WearableListenerService
    public void onMessageReceived(MessageEvent messageEvent) {

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Wearable.API)
                    .build();
        }
        if (!mGoogleApiClient.isConnected()) {
            ConnectionResult connectionResult =
                    mGoogleApiClient.blockingConnect(30, TimeUnit.SECONDS);

            if (!connectionResult.isSuccess()) {
                Log.e(TAG, "Failed to connect to GoogleApiClient.");
                return;
            }
        }

        if(messageEvent.getPath().equals(BReelWatchFaceUtil.PATH_WITH_FEATURE)) {
            byte[] rawData = messageEvent.getData();
            // It's allowed that the message carries only some of the keys used in the config DataItem
            // and skips the ones that we don't want to change.
            DataMap configKeysToOverwrite = DataMap.fromByteArray(rawData);
            Log.d(TAG, "Received watch face config message: " + configKeysToOverwrite);
            BReelWatchFaceUtil.overwriteKeysInConfigDataMap(mGoogleApiClient, configKeysToOverwrite);
        }
        if(messageEvent.getPath().equals(BReelWatchFaceUtil.PATH_WITH_FEATURE_ASSET)) {
            byte[] rawData = messageEvent.getData();
            DataMap assetKeyToOverwrite = DataMap.fromByteArray(rawData);
            Log.d(TAG,"ASSET RECEIVED: "+assetKeyToOverwrite);
            BReelWatchFaceUtil.overwriteKeysInAssetConfigDataMap(mGoogleApiClient, assetKeyToOverwrite);
        }
    }

    /**
     * Implement GoogleApiClient methods required to get access to data layer API
     */

    @Override // GoogleApiClient.ConnectionCallbacks
    public void onConnected(Bundle connectionHint) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onConnected: " + connectionHint);
        }
    }

    @Override  // GoogleApiClient.ConnectionCallbacks
    public void onConnectionSuspended(int cause) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onConnectionSuspended: " + cause);
        }
    }

    @Override  // GoogleApiClient.OnConnectionFailedListener
    public void onConnectionFailed(ConnectionResult result) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onConnectionFailed: " + result);
        }
    }
}

