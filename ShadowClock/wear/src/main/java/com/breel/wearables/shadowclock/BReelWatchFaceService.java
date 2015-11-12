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

package com.breel.wearables.shadowclock;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.WindowInsets;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.breel.wearables.shadowclock.config.BReelWatchFaceUtil;
import com.breel.wearables.shadowclock.controllers.ShadowDialController;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.List;
import java.util.TimeZone;

/**
 * Main Service of the watch face.
 */
public class BReelWatchFaceService extends CanvasWatchFaceService {
    private static final String TAG = "BReelWatchFaceService";


    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }


    private class Engine extends CanvasWatchFaceService.Engine implements DataApi.DataListener,
            GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

        // Controller for the dial
        private ShadowDialController mShadowDialController;

        // Time vars
        private Time mTime;
        private int mPreviousHour = -1;
        private int mActualHour = -1;
        private int mPreviousMinute = -1;
        private int mActualMinute = -1;

        // Canvas bounding rectangle and size
        private Rect mCanvasRect = new Rect();
        private int mCanvasWidth = 0;
        private int mCanvasHeight = 0;

        // Screen insets
        private Rect mInsetsScreen = new Rect();

        // System Flags
        private boolean mLowBitAmbient;
        private boolean mBurnInProtection;
        private boolean mAmbient;
        private boolean mMute;
        private boolean mIsRound;

        // Sun rotation flag
        private boolean rotateAnimationEnabled = false;
        // 24-hour format flag
        private boolean is24hours = true;

        // Animator for the sun rotation
        private ValueAnimator mSunAnimator;

        // API Client for retrieving
        private GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(BReelWatchFaceService.this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        private boolean mRegisteredTimeZoneReceiver = false;
        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                setTimeToNow();
            }
        };


        @Override
        public void onCreate(SurfaceHolder holder) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onCreate");
            }
            super.onCreate(holder);

            // Set watch face style
            setWatchFaceStyle(new WatchFaceStyle.Builder(BReelWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setPeekOpacityMode(WatchFaceStyle.PEEK_OPACITY_MODE_TRANSLUCENT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setViewProtection((WatchFaceStyle.PROTECT_STATUS_BAR | WatchFaceStyle.PROTECT_HOTWORD_INDICATOR))
                    .setShowSystemUiTime(false)
                    .setStatusBarGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP)
                    .setHotwordIndicatorGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP)
                    .build());

            // Init time
            mTime = new Time();
            setTimeToNow();

            // Starts controller
            mShadowDialController = new ShadowDialController(getBaseContext(), mTime, mCanvasWidth, mCanvasHeight);

            // Sun Rotation Animator
            mSunAnimator = ValueAnimator.ofFloat((float) (1.2 * Math.PI), (float) (2 * Math.PI));
            int mSunAnimatorDuration = 2000; //in millis
            ValueAnimator.setFrameDelay((long) 16);
            mSunAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            mSunAnimator.setDuration(mSunAnimatorDuration);
            mSunAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                public void onAnimationUpdate(ValueAnimator animation) {
                    Float value = (Float) animation.getAnimatedValue();
                    mShadowDialController.updateSunAnimation(mTime, value, mCanvasWidth, mCanvasHeight);
                    invalidate();
                }
            });
        }


        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            Log.d(TAG, "ON SURFACE CHANGED EVENT-----------");
            mCanvasWidth = width;
            mCanvasHeight = height;
        }


        @Override
        public void onDestroy() {
            super.onDestroy();
        }


        @Override
        public void onPeekCardPositionUpdate(Rect rect) {
            Log.d(TAG, "CARD APPEARED!! ----------------------" + rect.centerX() + " " + rect.centerY() + "");
        }


        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);

            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            Log.d(TAG, "onPropertiesChanged: burn-in protection = " + mBurnInProtection + ", low-bit ambient = " + mLowBitAmbient);
        }


        @Override
        public void onTimeTick() {
            super.onTimeTick();

            Log.d(TAG, "onTimeTick---------");

            setTimeToNow();

            // Update the Shadows orientation and distance
            updateCurrentTime();

            // Update the sun position and intensity
            mShadowDialController.updateSunToCurrentTime(mTime, mCanvasWidth, mCanvasHeight);

            if (!rotateAnimationEnabled) {
                if (!isInAmbientMode()) {
                    mShadowDialController.updateElementsStylesBasedOnTime(mTime);
                }
            }

            invalidate();
        }


        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            if (mAmbient != inAmbientMode) {
                Log.d(TAG, "onAmbientModeChanged: " + inAmbientMode);
                mAmbient = inAmbientMode;
                if (!isInAmbientMode()) {
                    if (rotateAnimationEnabled) {
                        mSunAnimator.start();
                    }

                    updateConfigDataItemAndUiOnStartup();
                }
                updateScreenMode();
                invalidate();
            }
        }


        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onInterruptionFilterChanged: " + interruptionFilter);
            }
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);

            if (mMute != inMuteMode) {
                mMute = inMuteMode;
            }
        }


        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            Log.d(TAG, "ON DRAW---------");
            mShadowDialController.drawDial(canvas, bounds, isInAmbientMode(), mIsRound, mInsetsScreen, mCanvasWidth);
        }


        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            Log.d(TAG, "onVisibilityChanged---------");
            if (visible) {

                mGoogleApiClient.connect();
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                setTimeToNow();

                updateConfigDataItemAndUiOnStartup();

            } else {
                unregisterReceiver();
                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    Wearable.DataApi.removeListener(mGoogleApiClient, this);
                    mGoogleApiClient.disconnect();
                }
            }
        }


        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public void onApplyWindowInsets(WindowInsets insets) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onApplyWindowInsets: " + (insets.isRound() ? "round" : "square"));
            }
            super.onApplyWindowInsets(insets);

            mIsRound = insets.isRound();

            // Get Screen insets
            int leftInset = insets.getSystemWindowInsetLeft();
            int topInset = insets.getSystemWindowInsetTop();
            int rightInset = insets.getSystemWindowInsetRight();
            int bottomInset = insets.getSystemWindowInsetBottom();

            mInsetsScreen.set(leftInset, topInset, rightInset, bottomInset);

            mCanvasRect.set(0, 0, mCanvasWidth, mCanvasHeight);

            // Update the sun position and intensity
            mShadowDialController.updateSunToCurrentTime(mTime, mCanvasWidth, mCanvasHeight);

            mShadowDialController.updateShineBounds(mInsetsScreen, mCanvasWidth, mCanvasHeight);
            mShadowDialController.setupShapeShadows(mCanvasRect);

            updateCurrentTime();

            mShadowDialController.setupColorBase(mTime);
            mShadowDialController.setupShineOverlay(mCanvasRect, mIsRound, mTime);

            mShadowDialController.updateElementsStylesBasedOnTime(mTime);
        }


        @Override  // GoogleApiClient.ConnectionCallbacks
        public void onConnected(Bundle connectionHint) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onConnected: " + connectionHint);
            }
            Wearable.DataApi.addListener(mGoogleApiClient, Engine.this);

            updateConfigDataItemAndUiOnStartup();
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


        @Override // DataApi.DataListener
        public void onDataChanged(DataEventBuffer dataEvents) {
            try {
                for (DataEvent dataEvent : dataEvents) {
                    if (dataEvent.getType() != DataEvent.TYPE_CHANGED) {
                        continue;
                    }

                    DataItem dataItem = dataEvent.getDataItem();
                    if (!dataItem.getUri().getPath().equals(BReelWatchFaceUtil.PATH_WITH_FEATURE)) {
                        continue;
                    }

                    DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
                    DataMap config = dataMapItem.getDataMap();
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Config DataItem updated:" + config);
                    }
                    updateUiForConfigDataMap(config);
                }
            } finally {
                dataEvents.close();
            }
        }


        /**
         * Sets the current time into the variable
         */
        private void setTimeToNow() {
            mTime.setToNow();
            mActualHour = mTime.hour;
            mActualMinute = mTime.minute;

            if (!is24hours) {
                mActualHour = mActualHour % 12;
                mActualHour = (mActualHour == 0) ? 12 : mActualHour;
            }
            Log.d(TAG, "ACTUAL HOUR: " + mActualHour + " ");
        }


        /**
         * Updates the current time information on the Dial
         */
        private void updateCurrentTime() {
            boolean updateMinutes = (mPreviousMinute != mActualMinute);
            boolean updateHours = (mPreviousHour != mActualHour);

            mShadowDialController.updateShapeShadowsToCurrentTime(
                    updateMinutes,
                    updateHours,
                    mActualMinute,
                    mActualHour);

            if (updateMinutes) {
                mPreviousMinute = mActualMinute;
            }

            if (updateHours) {
                mPreviousHour = mActualHour;
            }
        }


        /**
         * Updates the Dial screen mode
         */
        private void updateScreenMode() {
            if (isInAmbientMode()) {
                mShadowDialController.setAmbientMode(mLowBitAmbient, mBurnInProtection);
            } else {
                mShadowDialController.setInteractiveMode();
                mShadowDialController.updateElementsStylesBasedOnTime(mTime);
            }
        }


        /**
         * Changes the mode of the watch (12h or 24h)
         */
        private void setIs24hours(Boolean value) {
            is24hours = value;
            setTimeToNow();
            updateCurrentTime();
        }


        /**
         * Register the receiver for timezone changes
         */
        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            BReelWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }


        /**
         * Unregister the receiver for timezone changes
         */
        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            BReelWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }


        /**
         * Get the info from the settings
         */
        private void updateConfigDataItemAndUiOnStartup() {
            BReelWatchFaceUtil.fetchConfigDataMap(mGoogleApiClient,
                    new BReelWatchFaceUtil.FetchConfigDataMapCallback() {
                        @Override
                        public void onConfigDataMapFetched(DataMap startupConfig) {
                            // If the DataItem hasn't been created yet or some keys are missing,
                            // use the default values.
                            setDefaultValuesForMissingConfigKeys(startupConfig);
                            BReelWatchFaceUtil.putConfigDataItem(mGoogleApiClient, startupConfig);
                            updateUiForConfigDataMap(startupConfig);
                        }
                    }
            );
        }


        /**
         * Set the default value for the hour format type
         */
        private void setDefaultValuesForMissingConfigKeys(DataMap config) {
            addStringKeyIfMissing(config, BReelWatchFaceUtil.KEY_HOUR_FORMAT_TYPE, "true");
        }


        /**
         * Adds the string key in case it's missing
         */
        private void addStringKeyIfMissing(DataMap config, String key, String value) {
            if (!config.containsKey(key)) {
                config.putString(key, value);
            }
        }


        /**
         * Update the interface on changes on the settings
         */
        private void updateUiForConfigDataMap(final DataMap config) {

            boolean uiUpdated = false;

            for (String configKey : config.keySet()) {

                if (!config.containsKey(configKey)) {
                    continue;
                }

                if (updateUiForKey(configKey, config)) {
                    uiUpdated = true;
                }
            }

            if (uiUpdated) {

                invalidate();

                sendMessage("WEARABLE_SETTINGS_CHANGE", config.toByteArray());
            }
        }


        /**
         * Update the interface for one specific key
         */
        private boolean updateUiForKey(String configKey, DataMap config) {

            if (configKey.equals(BReelWatchFaceUtil.KEY_HOUR_FORMAT_TYPE)) {
                String twentyFourSwitchString = config.getString(BReelWatchFaceUtil.KEY_HOUR_FORMAT_TYPE);
                Log.d(TAG, "twentyFourSwitch RECEIVED: " + twentyFourSwitchString);
                setIs24hours(Boolean.parseBoolean(twentyFourSwitchString));
                return true;
            } else {
                return false;
            }
        }


        /**
         * Send a message to the config info.
         */
        private void sendMessage(final String message, final byte[] payload) {
            Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                @Override
                public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                    List<Node> nodes = getConnectedNodesResult.getNodes();
                    for (Node node : nodes) {
                        Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), message, payload).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                            @Override
                            public void onResult(MessageApi.SendMessageResult sendMessageResult) {

                            }
                        });
                    }

                }
            });
        }
    }
}

