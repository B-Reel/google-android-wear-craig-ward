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

package com.breel.wearables.shadowclock.graphics;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.Log;

/**
 * Graphics util to create the shine of the sun
 */
public class ShineOverlay {

    private static final String TAG = "ShineOverlay";

    private Bitmap shine;
    private Canvas canvas;
    private int width, height;

    BlurMaskFilter blurMaskFilter;
    float mDensity;

    private RadialGradient gradient;

    private Paint shinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float[] ColorPosition = {0.60f, 1.0f};
    int[] Colors = {0x00000000, 0x00000000};

    private float shineAngle = 0.0f;
    private float shineAngleOffset = 0.0f;

    Context context;

    public ShineOverlay(Context context, int width, int height) {
        this.width = width;
        this.height = height;
        shine = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(shine);

        this.context = context;
        mDensity = this.context.getResources().getDisplayMetrics().density;
    }

    private float map(float value, float low1, float high1, float low2, float high2) {
        return low2 + (value - low1) * (high2 - low2) / (high1 - low1);
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public void updateAngle(float shineAngle) {
        this.shineAngle = shineAngle;
        Log.d(TAG, "SHINE ANGLE:" + this.shineAngle + "");
        update();
    }

    public void setupBlur(float _amout) {
        blurMaskFilter = new BlurMaskFilter(_amout * mDensity, BlurMaskFilter.Blur.NORMAL);
        shinePaint.setMaskFilter(blurMaskFilter);
    }

    public void resetBlur() {
        shinePaint.setMaskFilter(null);
    }

    public void updateRadialGradient(float _radius, int initColor, int finalColor, float _initPosition, float _finalPosition) {
        Colors[0] = initColor;
        Colors[1] = finalColor;
        ColorPosition[0] = _initPosition;
        ColorPosition[1] = _finalPosition;
        gradient = new android.graphics.RadialGradient(this.width + 150.0f, this.height / 2, _radius, Colors, ColorPosition, Shader.TileMode.CLAMP);
        shinePaint.setShader(gradient);
    }

    public void updateRadialGradient(int initColor, int finalColor) {
        Colors[0] = initColor;
        Colors[1] = finalColor;
        gradient = new android.graphics.RadialGradient(this.width + 150.0f, this.height / 2, 300.0f, Colors, ColorPosition, Shader.TileMode.CLAMP);
        shinePaint.setShader(gradient);
    }

    public Bitmap getBitmap() {
        return shine;
    }

    public int getSize() {
        return shine.getWidth();
    }

    public void setAngleOffset(float _offset) {
        shineAngleOffset = _offset;
    }

    public void update() {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        canvas.save();
        canvas.translate(width / 2, height / 2);
        canvas.rotate(shineAngle - shineAngleOffset);
        canvas.translate(-width / 2, -height / 2);
        canvas.drawRect(0, 0, this.width, this.height, shinePaint);
        canvas.restore();
    }
}
