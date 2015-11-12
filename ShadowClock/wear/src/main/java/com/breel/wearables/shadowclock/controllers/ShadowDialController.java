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

package com.breel.wearables.shadowclock.controllers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.format.Time;
import android.util.Log;

import com.breel.wearables.shadowclock.graphics.Gaussian;
import com.breel.wearables.shadowclock.R;
import com.breel.wearables.shadowclock.graphics.ShapeShadow;
import com.breel.wearables.shadowclock.graphics.ShineOverlay;

/**
 * Controller for drawing the dial clock
 */
public class ShadowDialController {
    private static final String TAG = "ShadowDialController";

    // Context of the app.
    private final Context ctx;

    // Canvas Paints
    private Paint mCanvasPaint;
    private Paint mShinePaint;

    // Shadow Gradient Colors
    private int initShadowGradientColor;
    private int finalShadowGradientColor;

    // Shadow Paths
    private ShapeShadow hoursTens;
    private ShapeShadow hoursUnits;

    private ShapeShadow minutesTens;
    private ShapeShadow minutesUnits;

    // Shine Bounds
    private Rect mShineRectBounds;

    // Shine Overlay
    private ShineOverlay mShineOverlay;
    private ShineOverlay mShadowOverlay;

    // Sun position
    private float sunPositionX = 0.0f;
    private float sunPositionY = 0.0f;
    private float sunlightRatio = 0.0f;
    private float sunlightRatioMin = 0.5f;
    private float sunlightRatioMax = 1.0f;


    /**
     * Constructor
     * @param context Service context
     * @param time Current time
     * @param canvasWidth Watch face canvas width
     * @param canvasHeight Watch face canvas height
     */
    public ShadowDialController(Context context, Time time, int canvasWidth, int canvasHeight) {
        ctx = context;

        initShadowGradientColor = Color.argb(65, 0, 0, 0);
        finalShadowGradientColor = ctx.getResources().getColor(R.color.shadowPathEnd);

        // Canvas base Paint
        mCanvasPaint = new Paint();
        mCanvasPaint.setColor(ctx.getResources().getColor(R.color.ambientModeBackground));
        mCanvasPaint.setStyle(Paint.Style.FILL);

        mShinePaint = new Paint();

        hoursTens = new ShapeShadow(ctx);
        hoursUnits = new ShapeShadow(ctx);

        minutesTens = new ShapeShadow(ctx);
        minutesUnits = new ShapeShadow(ctx);

        calculateSunlightRatio(time);
        calculateSunRotation(0.0f, time, canvasWidth, canvasHeight);
    }


    /**
     * Creates the shadow effect for each number
     * @param _canvasBounds The bounds of the watch face canvas
     */
    public void setupShapeShadows(Rect _canvasBounds) {
        final int margin = 10;

        int width = _canvasBounds.width();
        int height = _canvasBounds.height();

        float scale = 0.8f;
        float shapeSize = 100 * scale;

        hoursTens.scale(scale);
        hoursTens.translate((width / 2) - shapeSize - margin, (height / 2) - shapeSize - margin);

        hoursUnits.scale(scale);
        hoursUnits.translate((width / 2) + margin, (height / 2) - shapeSize - margin);

        minutesTens.scale(scale);
        minutesTens.translate((width / 2) - shapeSize - margin, (height / 2) + margin);

        minutesUnits.scale(scale);
        minutesUnits.translate((width / 2) + margin, (height / 2) + margin);
    }

    public void setupColorBase(Time time) {
        updateShapeColorBasedOnTime();
        updateShadowGradientBasedOnTime(time);
    }

    /**
     * Creates the shine of the sun.
     * @param _canvasBounds The bounds of the watch face canvas
     * @param isRound if the watch is round or square
     */
    public void setupShineOverlay(Rect _canvasBounds, boolean isRound, Time time) {
        int width = _canvasBounds.width();
        int height = _canvasBounds.height();

        float size = (width > height) ? width : height;
        float dimension;

        if (isRound) {
            dimension = size;
        } else {
            dimension = 2 * (float) Math.sqrt((double) (((size / 2) * (size / 2)) + ((size / 2) * (size / 2))));
            Log.d(TAG, "SQUARE DIMENSION " + dimension + "");
        }

        mShineOverlay = new ShineOverlay(ctx, (int) dimension, (int) dimension);
        mShadowOverlay = new ShineOverlay(ctx, (int) dimension, (int) dimension);

        mShineOverlay.setAngleOffset(90.0f);
        mShineOverlay.updateRadialGradient(0xFFFFFFFF, 0x00FFFFFF);

        mShadowOverlay.setAngleOffset(-90.0f);
        mShadowOverlay.updateRadialGradient(0x66000000, 0x00000000);

        updateShineOverlayBasedOnTime(time);
    }


    /**
     * If the animation is activated, it updates the sun animation
     * @param time The current time
     * @param value The float value representing the position in the current animation
     * @param canvasWidth Watch face canvas width
     * @param canvasHeight Watch face canvas height
     */
    public void updateSunAnimation(Time time, Float value, int canvasWidth, int canvasHeight) {

        calculateSunRotation(value, time, canvasWidth, canvasHeight);

        float radius = map(value, (float) Math.PI, (float) (2 * Math.PI), 80.0f, 200.0f);
        float alpha = map(value, (float) Math.PI, (float) (2 * Math.PI), 0.0f, 60.f);

        hoursTens.calculateGradient(radius, initShadowGradientColor, finalShadowGradientColor);
        hoursUnits.calculateGradient(radius, initShadowGradientColor, finalShadowGradientColor);
        minutesTens.calculateGradient(radius, initShadowGradientColor, finalShadowGradientColor);
        minutesUnits.calculateGradient(radius, initShadowGradientColor, finalShadowGradientColor);

        float minuteMapped = map(time.minute, 0, 60, 0, 360);
        mShineOverlay.updateAngle(minuteMapped + (value * (float) (180 / Math.PI)));
        mShadowOverlay.updateAngle(minuteMapped + (value * (float) (180 / Math.PI)));
    }


    /**
     * Updates the shadows of the numbers to the current time
     * @param updateMinutes Indicates if we need to update the minutes numbers
     * @param updateHours Indicates if we need to update the hours numbers
     * @param currentMinute The current minute
     * @param currentHour The current hour
     */
    public void updateShapeShadowsToCurrentTime(boolean updateMinutes,
                                                boolean updateHours,
                                                int currentMinute,
                                                int currentHour) {
        if (updateMinutes) {
            int tens = (int) Math.floor(currentMinute / 10.0f);
            int units = (int) Math.floor(currentMinute % 10.0f);

            if (minutesTens.getCurrentValue() != tens) {
                String mt = String.format("%d", tens);
                mt = mt.concat(".json");
                Log.d(TAG, "MINUTE TENS STRING: " + mt);
                minutesTens.parseJSON(mt);
            }

            if (minutesUnits.getCurrentValue() != units) {
                String mu = String.format("%d", units);
                mu = mu.concat(".json");
                Log.d(TAG, "MINUTE UNITS STRING: " + mu);
                minutesUnits.parseJSON(mu);
            }
        }

        if (updateHours) {
            int tens = (int) Math.floor(currentHour / 10.0f);
            int units = (int) Math.floor(currentHour % 10.0f);

            if (hoursTens.getCurrentValue() != tens) {
                String ht = String.format("%d", tens);
                ht = ht.concat(".json");
                Log.d(TAG, "HOURS TENS STRING: " + ht);
                hoursTens.parseJSON(ht);
            }

            if (hoursUnits.getCurrentValue() != units) {
                String hu = String.format("%d", units);
                hu = hu.concat(".json");
                Log.d(TAG, "HOURS UNITS STRING: " + hu);
                hoursUnits.parseJSON(hu);
            }
        }
    }


    /**
     * Updates the Sun Position to the current time.
     * @param time Current time
     * @param canvasWidth Watch face canvas width
     * @param canvasHeight Watch face canvas height
     */
    public void updateSunToCurrentTime(Time time, int canvasWidth, int canvasHeight) {
        calculateSunlightRatio(time);
        calculateSunRotation(0.0f, time, canvasWidth, canvasHeight);
    }


    /**
     * Updates the different elements (shine, shadow, shape and background) to the current time
     * @param time The current Time
     */
    public void updateElementsStylesBasedOnTime(Time time) {
        // Updates shine overlay styles
        updateShineOverlayBasedOnTime(time);

        // Updates shadow gradient
        updateShadowGradientBasedOnTime(time);

        // Updates shape color
        updateShapeColorBasedOnTime();

        // Updates background color
        updateBackgroundColorBasedOnTime();
    }


    /**
     * Updates the dial to draw the ambient mode
     * @param lowBitAmbient Indicates if it's low-bit mode
     * @param burnInProtection Indicates if it's burn in protection mode
     */
    public void setAmbientMode(boolean lowBitAmbient, boolean burnInProtection) {
        if (lowBitAmbient) {
            mCanvasPaint.setColor(ctx.getResources().getColor(R.color.lowBitModeBackground));

            if (burnInProtection) {
                hoursTens.set1BitMode();
                hoursUnits.set1BitMode();

                minutesTens.set1BitMode();
                minutesUnits.set1BitMode();
            } else {
                hoursTens.setLowBitMode();
                hoursUnits.setLowBitMode();

                minutesTens.setLowBitMode();
                minutesUnits.setLowBitMode();
            }
        } else {
            mCanvasPaint.setColor(ctx.getResources().getColor(R.color.ambientModeBackground));

            hoursTens.setAmbientMode();
            hoursUnits.setAmbientMode();

            minutesTens.setAmbientMode();
            minutesUnits.setAmbientMode();
        }
    }


    /**
     * Updates the dial to draw the interactive (the regular) mode
     */
    public void setInteractiveMode() {
        hoursTens.setInteractiveMode();
        hoursUnits.setInteractiveMode();

        minutesTens.setInteractiveMode();
        minutesUnits.setInteractiveMode();
    }


    /**
     * Call to draw the dial
     * @param canvas The watch face canvas
     * @param bounds The bounds of the current canvas
     * @param isAmbientMode Indicates if it is in ambient mode
     * @param isRound Indicates if the watch is round
     * @param insetsScreen The insets of the screen
     * @param canvasWidth Watch face canvas width
     */
    public void drawDial(Canvas canvas,
                         Rect bounds,
                         boolean isAmbientMode,
                         boolean isRound,
                         Rect insetsScreen,
                         int canvasWidth) {

        // Draws main background color
        canvas.drawPaint(mCanvasPaint);

        // Draws Shadow Overlay only if it is not ambient mode
        if (!isAmbientMode) {
            if (isRound) {
                canvas.drawBitmap(mShadowOverlay.getBitmap(), null, bounds, mShinePaint);
            } else {
                canvas.drawBitmap(mShadowOverlay.getBitmap(), -insetsScreen.left + (canvasWidth - mShadowOverlay.getWidth()) / 2, -insetsScreen.top + (canvasWidth - mShadowOverlay.getHeight()) / 2, mShinePaint);
            }
        }

        // Draws Shadows Only if it is not ambient mode
        if (!isAmbientMode) {
            minutesTens.drawShadow(canvas, sunPositionX, sunPositionY);
            minutesUnits.drawShadow(canvas, sunPositionX, sunPositionY);

            hoursTens.drawShadow(canvas, sunPositionX, sunPositionY);
            hoursUnits.drawShadow(canvas, sunPositionX, sunPositionY);
        }

        // Draws shapes
        minutesTens.drawShape(canvas);
        minutesUnits.drawShape(canvas);

        hoursTens.drawShape(canvas);
        hoursUnits.drawShape(canvas);


        if (!isAmbientMode) {
            if (isRound) {
                canvas.drawBitmap(mShadowOverlay.getBitmap(), null, mShineRectBounds, mShinePaint);
            } else {
                canvas.drawBitmap(mShadowOverlay.getBitmap(), -insetsScreen.left + (canvasWidth - mShadowOverlay.getWidth()) / 2, -insetsScreen.top + (canvasWidth - mShadowOverlay.getHeight()) / 2, mShinePaint);
            }
        }

        // Draws The Shine
        if (!isAmbientMode) {
            if (isRound) {
                canvas.drawBitmap(mShineOverlay.getBitmap(), null, mShineRectBounds, mShinePaint);
            } else {
                canvas.drawBitmap(mShineOverlay.getBitmap(), -insetsScreen.left + (canvasWidth - mShineOverlay.getWidth()) / 2, -insetsScreen.top + (canvasWidth - mShineOverlay.getHeight()) / 2, mShinePaint);
            }
        }

    }


    /**
     * Updates the bounds for the shine effect
     * @param insetsScreen The insets of the screen
     * @param canvasWidth Watch face canvas width
     * @param canvasHeight Watch face canvas height
     */
    public void updateShineBounds(Rect insetsScreen, int canvasWidth, int canvasHeight) {
        mShineRectBounds = new Rect();
        mShineRectBounds.set(-insetsScreen.top, -insetsScreen.left, canvasWidth, canvasHeight);
    }


    /**
     * Updates the background color based on the current time
     */
    private void updateBackgroundColorBasedOnTime() {
        float mBackgroundColor = map(sunlightRatio, sunlightRatioMin, sunlightRatioMax, 0.96f, 0.29f);
        float[] hsv = {0.0f, 0.0f, mBackgroundColor};
        mCanvasPaint.setColor(Color.HSVToColor(hsv));
    }


    /**
     * Updates the numbers color base on the current time
     */
    private void updateShapeColorBasedOnTime() {
        float mShapeColor = map(sunlightRatio, sunlightRatioMin, sunlightRatioMax, 1.0f, 0.33f);
        float[] hsv = {0.0f, 0.0f, mShapeColor};

        hoursTens.setShapeColor(Color.HSVToColor(hsv));
        hoursUnits.setShapeColor(Color.HSVToColor(hsv));
        minutesTens.setShapeColor(Color.HSVToColor(hsv));
        minutesUnits.setShapeColor(Color.HSVToColor(hsv));
    }


    /**
     * Updates the shadow of the numbers based on the current time
     * @param time The current time
     */
    private void updateShadowGradientBasedOnTime(Time time) {
        // Calculates the shadow length based on the current time
        float mShadowGradientRadius = map(sunlightRatio, sunlightRatioMin, sunlightRatioMax, 80.0f, 160.0f);
        Log.d(TAG, " GRADIENT RADIUS AT " + time.hour + " h. RAD: " + mShadowGradientRadius + "");

        hoursTens.calculateGradient(mShadowGradientRadius, initShadowGradientColor, finalShadowGradientColor);
        hoursUnits.calculateGradient(mShadowGradientRadius, initShadowGradientColor, finalShadowGradientColor);
        minutesTens.calculateGradient(mShadowGradientRadius, initShadowGradientColor, finalShadowGradientColor);
        minutesUnits.calculateGradient(mShadowGradientRadius, initShadowGradientColor, finalShadowGradientColor);
    }


    /**
     * Updates the Shine of the overlay based on the current time
     * @param time The current time
     */
    private void updateShineOverlayBasedOnTime(Time time) {
        float minuteMapped = map(time.minute, 0, 60, 0, 360);
        mShineOverlay.updateAngle(minuteMapped);
        mShadowOverlay.updateAngle(minuteMapped);

        float initShine = map(sunlightRatio, sunlightRatioMin, sunlightRatioMax, 0.6f, 0.0f);
        float finalShine = 1.0f;

        float initShadow = map(sunlightRatio, sunlightRatioMin, sunlightRatioMax, 0.6f, 0.2f);
        float finalShadow = map(sunlightRatio, sunlightRatioMin, sunlightRatioMax, 1.0f, 1.0f);

        float alphaShine = map(sunlightRatio, sunlightRatioMin, sunlightRatioMax, 255.0f, 100.0f);
        float radiusShine = map(sunlightRatio, sunlightRatioMin, sunlightRatioMax, 300.0f, 250.0f);

        float radiusShadow = map(sunlightRatio, sunlightRatioMin, sunlightRatioMax, 300.0f, 350.0f);
        float alphaShadow = map(sunlightRatio, sunlightRatioMin, sunlightRatioMax, 60.0f, 200.0f);

        mShineOverlay.updateRadialGradient(radiusShine, Color.argb((int) alphaShine, 255, 255, 255), 0x00FFFFFF, initShine, finalShine);
        mShadowOverlay.updateRadialGradient(radiusShadow, Color.argb((int) alphaShadow, 0, 0, 0), 0x00000000, initShadow, finalShadow);
    }


    /**
     * Calculates the rotation of the sun
     * @param _offset The rotation offset of the sun
     * @param time Current time
     * @param canvasWidth Watch face canvas width
     * @param canvasHeight Watch face canvas height
     */
    private void calculateSunRotation(float _offset, Time time, int canvasWidth, int canvasHeight) {
        sunPositionX = (float) (canvasWidth / 2 + (canvasWidth / 2 + 200.0f) * Math.cos((2 * Math.PI * time.minute / 60) + _offset - Math.PI / 2));
        sunPositionY = (float) (canvasHeight / 2 + (canvasWidth / 2 + 200.0f) * Math.sin((2 * Math.PI * time.minute / 60) + _offset - Math.PI / 2));
    }


    /**
     * Calculates the sunlight intensity
     * @param time Current time
     */
    private void calculateSunlightRatio(Time time) {
        float value = time.hour + (map(time.minute, 0, 60, 0, 100) / 100);
        // Uses a Gaussian function to estimate the actual movement of the sun and therefore the sunlight intensity.
        sunlightRatio = (float) Gaussian.getPhi(value, 12.0f, 3.65f);
        if (sunlightRatio < 0.5) sunlightRatio = 1 - sunlightRatio;
    }


    /**
     * Converts a value from a range of numbers to another one
     * @param value Value to convert
     * @param low1 Min value of the current range
     * @param high1 Max value of the current range
     * @param low2 Min value of the new range
     * @param high2 Max value of the new range
     * @return the value converted.
     */
    private float map(float value, float low1, float high1, float low2, float high2) {
        return low2 + (value - low1) * (high2 - low2) / (high1 - low1);
    }
}
