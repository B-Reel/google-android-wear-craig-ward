/*
 * Copyright (C) 2015 B-Reel (created by Matthew Valverde on 11/22/2014)
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

package com.breel.wearables.shadowclock.viewers;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.breel.wearables.shadowclock.utils.BReelWatchFaceUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;

import com.breel.wearables.shadowclock.R;
import com.breel.wearables.shadowclock.utils.DataMapManager;

/**
 * Class to create the view of the settings Activity
 */
public class WatchFaceSettingsViewer {

    public static final String KEY_HOUR_FORMAT_TYPE = BReelWatchFaceUtil.KEY_HOUR_FORMAT_TYPE;
    private Activity context;
    private static final String PATH_WITH_FEATURE = BReelWatchFaceUtil.PATH_WITH_FEATURE;
    private GoogleApiClient mGoogleApiClient;
    private String mPeerId;
    private ToggleButton twentyFourClockBtn;
    TextView styleLabelA;
    TextView styleLabelB;
    Typeface tf;

    public DataMapManager dataMapManager;


    /**
     * Constructor
     * @param context The context of the view
     * @param mGoogleApiClient The current GoogleAPIClient object
     * @param mPeerId The ID of the device
     */
    public WatchFaceSettingsViewer(Activity context, GoogleApiClient mGoogleApiClient, String mPeerId) {
        this.context = context;
        this.mGoogleApiClient = mGoogleApiClient;
        this.mPeerId = mPeerId;

        dataMapManager = new DataMapManager(mGoogleApiClient, mPeerId);

        createUI();
    }


    /**
     * sets the default values of
     */
    public void setDefaults(DataMap config) {

        String twentyFourClockString = config.getString(KEY_HOUR_FORMAT_TYPE);

        twentyFourClockBtn.setChecked(Boolean.valueOf((twentyFourClockString != null ? twentyFourClockString : "false")));
    }


    /**
     * Creates the UI of the view
     */
    private void createUI() {
        tf = Typeface.createFromAsset(context.getAssets(), "fonts/Brandon_med.otf");

        styleLabelA = (TextView) context.findViewById(R.id.styleLabelA);
        styleLabelA.setTypeface(tf);

        styleLabelB = (TextView) context.findViewById(R.id.styleLabelB);
        styleLabelB.setTypeface(tf);

        twentyFourClockBtn = (ToggleButton) context.findViewById(R.id.twentyFourClockBtn);
        twentyFourClockBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dataMapManager.sendMessage(KEY_HOUR_FORMAT_TYPE, String.valueOf(twentyFourClockBtn.isChecked()));
            }
        });
    }

}
