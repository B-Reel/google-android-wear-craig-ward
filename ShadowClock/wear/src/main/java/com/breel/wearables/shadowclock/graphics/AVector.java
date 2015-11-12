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

/**
 * Vector class
 */
public class AVector {

    private float x;
    private float y;

    public AVector(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public void reset() {
        this.x = 0.0f;
        this.y = 0.0f;
    }

    public float getLength() {
        return (float) (Math.sqrt((x * x) + (y * y)));
    }

    public void normalize() {
        float length = getLength();
        this.x = (this.x / length);
        this.y = (this.y / length);
    }

    public void multiply(float _scalar) {
        this.x = this.x * _scalar;
        this.y = this.y * _scalar;
    }

    public void add(AVector _v) {
        this.x += _v.getX();
        this.y += _v.getY();
    }

    public void add(float x, float y) {
        this.x += x;
        this.y += y;
    }

    public static AVector sub(AVector v1, AVector v2) {
        float x = v1.getX() - v2.getX();
        float y = v1.getY() - v2.getY();
        return new AVector(x, y);
    }


}
