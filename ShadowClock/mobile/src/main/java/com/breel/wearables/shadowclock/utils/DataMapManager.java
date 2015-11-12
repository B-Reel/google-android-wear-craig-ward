/*
 * Copyright (C) 2015 B-Reel (created by Matthew Valverde on 11/28/2014)
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

package com.breel.wearables.shadowclock.utils;

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

/**
 * Helper so send the information using the Data Layer API
 */
public class DataMapManager {

    private static final String PATH_WITH_FEATURE = "/watch_face_config/breel";

    public GoogleApiClient mGoogleApiClient;
    private String mPeerId;


    /**
     * Constructor
     * @param mGoogleApiClient The current GoogleAPIClient object
     * @param mPeerId The ID of the device
     */
    public DataMapManager(GoogleApiClient mGoogleApiClient, String mPeerId) {
        this.mGoogleApiClient = mGoogleApiClient;
        this.mPeerId = mPeerId;
    }


    /**
     * Put new data into the DataMap
     * @param path Path of the data
     * @param value Value of the data
     */
    public void add(String path, final String value) {
        PutDataMapRequest dataMap = PutDataMapRequest.create(path);
        dataMap.getDataMap().putString("value", value + "|" + String.valueOf(System.currentTimeMillis()));
        PutDataRequest request = dataMap.asPutDataRequest();
        Wearable.DataApi.putDataItem(mGoogleApiClient, request).setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(DataApi.DataItemResult dataItemResult) {
                Log.d("onResult", value + " Sending was successful: " + dataItemResult.getStatus().isSuccess());
            }
        });
    }


    /**
     * Send information using the wearable message API
     * @param configKey The key of the setting
     * @param value The value of the setting
     */
    public void sendMessage(String configKey, String value) {

        if (mPeerId != null) {
            DataMap config = new DataMap();
            config.putString(configKey, value);
            byte[] rawData = config.toByteArray();
            Wearable.MessageApi.sendMessage(mGoogleApiClient, mPeerId, PATH_WITH_FEATURE, rawData);
        }
    }
}
