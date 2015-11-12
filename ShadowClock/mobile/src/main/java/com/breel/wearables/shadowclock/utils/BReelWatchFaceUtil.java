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

package com.breel.wearables.shadowclock.utils;

import android.graphics.Color;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

/**
 * Class to get the settings and / or save them
 */
public final class BReelWatchFaceUtil {
    private static final String TAG = "BReelWatchFaceUtil";
    public static final String PATH_WITH_FEATURE = "/watch_face_config/breel";
    public static final String KEY_HOUR_FORMAT_TYPE = "HOUR_FORMAT_TYPE";
    public static final String PATH_WITH_FEATURE_ASSET = "/watch_face_config/breel/asset";


    /**
     * Callback interface to perform an action with the current config {@link com.google.android.gms.wearable.DataMap} for
     * {@link BReelWatchFaceUtil}.
     */
    public interface FetchConfigDataMapCallback {
        /**
         * Callback invoked with the current config {@link com.google.android.gms.wearable.DataMap} for {@link BReelWatchFaceUtil}.
         */
        void onConfigDataMapFetched(DataMap config);
    }

    private static int parseColor(String colorName) {
        return Color.parseColor(colorName.toLowerCase());
    }

    /**
     * Asynchronously fetches the current config {@link com.google.android.gms.wearable.DataMap} for {@link BReelWatchFaceUtil} and
     * passes it to the given callback.
     * <p>
     * If the current config {@link com.google.android.gms.wearable.DataItem} doesn't exist, it isn't created and the callback
     * receives an empty DataMap.
     */
    public static void fetchConfigDataMap(final GoogleApiClient client,
                                          final FetchConfigDataMapCallback callback) {
        Wearable.NodeApi.getLocalNode(client).setResultCallback(
                new ResultCallback<NodeApi.GetLocalNodeResult>() {
                    @Override
                    public void onResult(NodeApi.GetLocalNodeResult getLocalNodeResult) {
                        String localNode = getLocalNodeResult.getNode().getId();
                        Uri uri = new Uri.Builder()
                                .scheme("wear")
                                .path(BReelWatchFaceUtil.PATH_WITH_FEATURE)
                                .authority(localNode)
                                .build();
                        Wearable.DataApi.getDataItem(client, uri)
                                .setResultCallback(new DataItemResultCallback(callback));
                    }
                }
        );
    }

    /**
     * Overwrites (or sets, if not present) the keys in the current config {@link com.google.android.gms.wearable.DataItem} with the
     * ones appearing in the given {@link com.google.android.gms.wearable.DataMap}. If the config DataItem doesn't exist, it's
     * created.
     * <p>
     * It is allowed that only some of the keys used in the config DataItem appear in
     * {@code configKeysToOverwrite}. The rest of the keys remains unmodified in this case.
     */
    public static void overwriteKeysInConfigDataMap(final GoogleApiClient googleApiClient,
                                                    final DataMap configKeysToOverwrite) {

        BReelWatchFaceUtil.fetchConfigDataMap(googleApiClient,
                new BReelWatchFaceUtil.FetchConfigDataMapCallback() {
                    @Override
                    public void onConfigDataMapFetched(DataMap currentConfig) {
                        DataMap overwrittenConfig = new DataMap();
                        overwrittenConfig.putAll(currentConfig);
                        overwrittenConfig.putAll(configKeysToOverwrite);
                        BReelWatchFaceUtil.putConfigDataItem(googleApiClient, overwrittenConfig);
                    }
                }
        );
    }

    public static void overwriteKeysInAssetConfigDataMap(final GoogleApiClient googleApiClient,
                                                         final DataMap configKeysToOverwrite) {
        // Do something here...
    }

    /**
     * Overwrites the current config {@link com.google.android.gms.wearable.DataItem}'s {@link com.google.android.gms.wearable.DataMap} with {@code newConfig}. If
     * the config DataItem doesn't exist, it's created.
     */
    public static void putConfigDataItem(GoogleApiClient googleApiClient, DataMap newConfig) {
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(PATH_WITH_FEATURE);
        DataMap configToPut = putDataMapRequest.getDataMap();
        configToPut.putAll(newConfig);
        Wearable.DataApi.putDataItem(googleApiClient, putDataMapRequest.asPutDataRequest())
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG, "putDataItem result status: " + dataItemResult.getStatus());
                        }
                    }
                });
    }

    private static class DataItemResultCallback implements ResultCallback<DataApi.DataItemResult> {

        private final FetchConfigDataMapCallback mCallback;

        public DataItemResultCallback(FetchConfigDataMapCallback callback) {
            mCallback = callback;
        }

        @Override
        public void onResult(DataApi.DataItemResult dataItemResult) {
            if (dataItemResult.getStatus().isSuccess()) {
                if (dataItemResult.getDataItem() != null) {
                    DataItem configDataItem = dataItemResult.getDataItem();
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(configDataItem);
                    DataMap config = dataMapItem.getDataMap();
                    mCallback.onConfigDataMapFetched(config);
                } else {
                    mCallback.onConfigDataMapFetched(new DataMap());
                }
            }
        }
    }

    private BReelWatchFaceUtil() { }
}
