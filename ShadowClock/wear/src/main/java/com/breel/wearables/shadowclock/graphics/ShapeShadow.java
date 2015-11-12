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
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.Log;

import com.breel.wearables.shadowclock.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.Vector;


/**
 * Graphics util to create the shadow shape of the numbers
 */
public class ShapeShadow {

    private static final boolean DEBUG = false;
    private static final String TAG = "ShapeShadow";

    private Vector<AVector> vertexArray;

    // Sun position vector
    private AVector tmpSunPositionVector = new AVector(0.0f, 0.0f);

    // Gradient helper paint
    Paint gradientHelperPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Bounds path
    private Paint boundsPathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Path boundsPath = new Path();

    private Paint shadowPathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    BlurMaskFilter blurMaskFilter;

    private Path shadowPath = new Path();

    float[] ColorStops = {0.0f, 0.4f, 1.0f};

    private Vector<Path> shadowPaths = new Vector<Path>();

    // Shape
    private Paint shapePathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Path shapePath = new Path();
    private int shapeColor;

    private float positionX = 0.0f;
    private float positionY = 0.0f;
    private float scale = 1.0f;

    private float maxX = 0.0f;
    private float maxY = 0.0f;
    private float minX = 1000.0f;
    private float minY = 1000.0f;
    private float medX = 0.0f;
    private float medY = 0.0f;

    float mDensity;

    // Transformation matrix
    Matrix pathTransform = new Matrix();

    private String id = "-1";

    Context context;


    public ShapeShadow(Context context) {
        this.context = context;
        positionX = 0.0f;
        positionY = 0.0f;

        mDensity = this.context.getResources().getDisplayMetrics().density;

        setupPaint();
        vertexArray = new Vector<AVector>();
    }

    public int getCurrentValue() {
        return Integer.parseInt(id);
    }

    public void parseJSON(String jsonFile) {
        if (jsonFile != null) {
            // Load all the JSONs with the numbers information
            try {
                JSONObject obj = new JSONObject(loadJSONFromAsset(jsonFile));
                Log.d(TAG, "SHAPE SHADOW JSON FILE " + jsonFile + ": LOADED");

                id = obj.getString("id");

                JSONArray jsonPathData;
                JSONArray jsonShadowData;

                if (!id.equals("5")) {
                    jsonPathData = obj.getJSONArray("path");
                    jsonShadowData = obj.getJSONArray("shadow");

                    Log.d(TAG, "JSON PATH DATA LENGTH: " + jsonPathData.length() + "");
                    Log.d(TAG, "JSON SHADOW DATA LENGTH: " + jsonShadowData.length() + "");

                    shapePath.reset();
                    for (int i = 0; i < jsonPathData.length(); i++) {
                        try {
                            JSONObject elem = jsonPathData.getJSONObject(i);
                            String type = elem.getString("type");
                            JSONArray data = elem.getJSONArray("data");
                            if (type.equals("move")) {
                                shapePath.moveTo((float) data.getInt(0), (float) data.getInt(1));
                            } else if (type.equals("line")) {
                                shapePath.lineTo((float) data.getInt(0), (float) data.getInt(1));
                            } else if (type.equals("bezier")) {
                                shapePath.cubicTo((float) data.getInt(0), (float) data.getInt(1), (float) data.getInt(2), (float) data.getInt(3), (float) data.getInt(4), (float) data.getInt(5));
                            }
                        } catch (JSONException e) {
                            Log.d(TAG, "JSON ELEM EXCEPTION" + e.getMessage() + "");
                        }
                    }
                    shapePath.close();

                    Random r = new Random();
                    r.nextGaussian();


                    JSONArray holesContainer = obj.getJSONArray("holes");
                    Path holePath = new Path();
                    for (int i = 0; i < holesContainer.length(); i++) {
                        JSONObject jsonInside = holesContainer.getJSONObject(i);
                        JSONArray hole = jsonInside.getJSONArray("data");
                        holePath.reset();
                        for (int j = 0; j < hole.length(); j++) {
                            try {
                                JSONObject elem = hole.getJSONObject(j);
                                String type = elem.getString("type");
                                JSONArray data = elem.getJSONArray("data");
                                if (type.equals("move")) {
                                    holePath.moveTo((float) data.getInt(0), (float) data.getInt(1));
                                } else if (type.equals("line")) {
                                    holePath.lineTo((float) data.getInt(0), (float) data.getInt(1));
                                } else if (type.equals("bezier")) {
                                    holePath.cubicTo((float) data.getInt(0), (float) data.getInt(1), (float) data.getInt(2), (float) data.getInt(3), (float) data.getInt(4), (float) data.getInt(5));
                                }
                            } catch (JSONException e) {
                                Log.d(TAG, "JSON HOLE EXCEPTION" + e.getMessage() + "");
                            }
                        }
                        holePath.close();
                        shapePath.op(holePath, Path.Op.DIFFERENCE);
                    }

                    pathTransform.reset();
                    pathTransform.setScale(scale + 0.04f, scale + 0.04f);
                    shapePath.transform(pathTransform);
                    boundsPath.transform(pathTransform);

                    pathTransform.setTranslate(positionX - 0.3f, positionY - 0.3f);
                    shapePath.transform(pathTransform);
                    boundsPath.transform(pathTransform);

                    int shadowTmpX;
                    int shadowTmpY;

                    shadowPaths.clear();
                    vertexArray.clear();

                    for (int i = 0; i < jsonShadowData.length(); i += 2) {
                        shadowTmpX = jsonShadowData.getInt(i);
                        shadowTmpY = jsonShadowData.getInt(i + 1);
                        addVertex(shadowTmpX, shadowTmpY);
                    }
                } else {
                    jsonPathData = obj.getJSONArray("path");
                    jsonShadowData = obj.getJSONArray("shadow");

                    Log.d(TAG, "JSON PATH DATA LENGTH: " + jsonPathData.length() + "");
                    Log.d(TAG, "JSON SHADOW DATA LENGTH: " + jsonShadowData.length() + "");

                    shapePath.reset();
                    for (int i = 0; i < jsonPathData.length(); i++) {
                        JSONArray cords = jsonPathData.getJSONArray(i);
                        Path chunk = new Path();
                        chunk.reset();
                        for (int j = 0; j < cords.length(); j++) {
                            try {
                                JSONObject elem = cords.getJSONObject(j);
                                String type = elem.getString("type");
                                JSONArray data = elem.getJSONArray("data");
                                if (type.equals("move")) {
                                    chunk.moveTo((float) data.getInt(0), (float) data.getInt(1));
                                } else if (type.equals("line")) {
                                    chunk.lineTo((float) data.getInt(0), (float) data.getInt(1));
                                } else if (type.equals("bezier")) {
                                    chunk.cubicTo((float) data.getInt(0), (float) data.getInt(1), (float) data.getInt(2), (float) data.getInt(3), (float) data.getInt(4), (float) data.getInt(5));
                                }
                            } catch (JSONException e) {
                                Log.d(TAG, "JSON 5 NUMBER ELEM EXCEPTION" + e.getMessage() + "");
                            }
                        }
                        chunk.close();
                        shapePath.op(chunk, Path.Op.UNION);
                    }

                    pathTransform.reset();
                    pathTransform.setScale(scale, scale);
                    shapePath.transform(pathTransform);
                    boundsPath.transform(pathTransform);

                    pathTransform.setTranslate(positionX, positionY);
                    shapePath.transform(pathTransform);
                    boundsPath.transform(pathTransform);

                    shadowPaths.clear();
                    vertexArray.clear();

                    int shadowTmpX;
                    int shadowTmpY;
                    for (int i = 0; i < jsonShadowData.length(); i++) {
                        JSONArray coords = jsonShadowData.getJSONArray(i);
                        for (int j = 0; j < coords.length(); j += 2) {
                            shadowTmpX = coords.getInt(j);
                            shadowTmpY = coords.getInt(j + 1);
                            addVertex((float) shadowTmpX, (float) shadowTmpY);
                        }

                    }
                }
            } catch (JSONException e) {
                Log.d(TAG, "JSON ROOT EXCEPTION" + e.getMessage() + "");
            }
        }
    }

    public void translate(float _x, float _y) {
        positionX = _x;
        positionY = _y;
    }

    public void scale(float _factor) {
        scale = _factor;
    }

    public void setShapeColor(int _shapeColor) {
        shapeColor = _shapeColor;
        shapePathPaint.setColor(shapeColor);
    }

    public void setAmbientMode() {
        shapePathPaint.setStyle(Paint.Style.FILL);
        shapePathPaint.setColor(this.context.getResources().getColor(R.color.ambientModeTypeface));
        shapePathPaint.setAntiAlias(true);
    }

    public void setLowBitMode() {
        shapePathPaint.setStyle(Paint.Style.FILL);
        shapePathPaint.setColor(this.context.getResources().getColor(R.color.lowBitModeTypeface));
        shapePathPaint.setAntiAlias(false);
    }

    public void set1BitMode() {
        shapePathPaint.setStyle(Paint.Style.STROKE);
        shapePathPaint.setColor(this.context.getResources().getColor(R.color.lowBitModeTypeface));
        shapePathPaint.setStrokeWidth(1);
        shapePathPaint.setAntiAlias(false);
    }

    public void setInteractiveMode() {
        shapePathPaint.setStyle(Paint.Style.FILL);
        shapePathPaint.setAntiAlias(true);
    }


    public void setupBlur(float _amout) {
        blurMaskFilter = new BlurMaskFilter(_amout * mDensity, BlurMaskFilter.Blur.NORMAL);
        shadowPathPaint.setMaskFilter(blurMaskFilter);
    }

    public void resetBlur() {
        shadowPathPaint.setMaskFilter(null);
    }

    public void addVertex(float x, float y) {
        x = x * (scale);
        y = y * (scale);

        if (x > maxX) {
            maxX = x;
        }
        if (y > maxY) {
            maxY = y;
        }
        if (x < minX) {
            minX = x;
        }
        if (y < minY) {
            minY = y;
        }
        medX = (maxX - minX) / 2 + positionX + minX;
        medY = (maxY - minY) / 2 + positionY;

        x += positionX;
        y += positionY;
        vertexArray.add(new AVector(x, y));
        shadowPaths.add(new Path());
    }


    public void calculateGradient(float _radius, int _initColor, int _endColor) {
        int[] Colors = {_initColor, _initColor, _endColor};
        shadowPathPaint.setShader(new RadialGradient(medX, medY, _radius, Colors, ColorStops, Shader.TileMode.CLAMP));
    }


    public void drawShape(Canvas canvas) {
        canvas.drawPath(shapePath, shapePathPaint);
        if (DEBUG) {
            canvas.drawPath(boundsPath, boundsPathPaint);
            canvas.drawCircle(medX, medY, 5, gradientHelperPaint);
        }

    }

    public void drawShadow(Canvas canvas, float _sunPosX, float _sunPosY) {
        tmpSunPositionVector.setPosition(_sunPosX, _sunPosY);
        shadowPath.reset();
        for (int i = 0; i < vertexArray.size(); i++) {
            Path tmpPath = shadowPaths.get(i);
            tmpPath.reset();

            AVector v1 = vertexArray.get(i);
            AVector v2 = vertexArray.get(i == getVertCount() - 1 ? 0 : i + 1);

            tmpPath.moveTo(v2.getX(), v2.getY());
            tmpPath.lineTo(v1.getX(), v1.getY());

            // Current shadow vertex
            AVector tmpShadowVector = AVector.sub(v1, tmpSunPositionVector);
            tmpShadowVector.normalize();
            tmpShadowVector.multiply(600.0f);
            tmpShadowVector.add(v1);

            tmpPath.lineTo(tmpShadowVector.getX(), tmpShadowVector.getY());

            // Current shadow vertex
            tmpShadowVector = AVector.sub(v2, tmpSunPositionVector);
            tmpShadowVector.normalize();
            tmpShadowVector.multiply(600.0f);
            tmpShadowVector.add(v2);

            tmpPath.lineTo(tmpShadowVector.getX(), tmpShadowVector.getY());
            tmpPath.close();

            shadowPath.op(tmpPath, Path.Op.UNION);
        }
        canvas.drawPath(shadowPath, shadowPathPaint);
    }

    public int getVertCount() {
        return vertexArray.size();
    }

    private String loadJSONFromAsset(String file) {
        String json = null;
        try {
            InputStream is = this.context.getAssets().open("json_lowpoly/" + file);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    private void setupPaint() {
        boundsPathPaint.setColor(Color.RED);
        boundsPathPaint.setStyle(Paint.Style.STROKE);
        boundsPathPaint.setStrokeWidth(2);

        shapePathPaint.setColor(Color.WHITE);
        shapePathPaint.setStyle(Paint.Style.FILL);

        shadowPathPaint.setColor(Color.BLUE);
        shadowPathPaint.setStyle(Paint.Style.FILL);

        setupBlur(3.0f);

        gradientHelperPaint.setColor(Color.BLUE);
        gradientHelperPaint.setStyle(Paint.Style.FILL);

        boundsPath.reset();
        boundsPath.moveTo(0, 0);
        boundsPath.lineTo(100, 0);
        boundsPath.lineTo(100, 100);
        boundsPath.lineTo(0, 100);
        boundsPath.close();

        shapeColor = this.context.getResources().getColor(R.color.ambientModeTypeface);
    }

    private void computePathBounds(Path _path, RectF _rBounds) {
        _path.computeBounds(_rBounds, false);
    }
}
