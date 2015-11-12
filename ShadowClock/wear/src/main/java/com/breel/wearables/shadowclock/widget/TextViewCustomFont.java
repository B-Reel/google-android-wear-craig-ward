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

package com.breel.wearables.shadowclock.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

import com.breel.wearables.shadowclock.R;


/**
 * Class to create Text views with custom fonts
 */
public class TextViewCustomFont extends TextView {

    public TextViewCustomFont(Context context) {
        super(context);
    }

    public TextViewCustomFont(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFont(attrs);
    }

    public TextViewCustomFont(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setFont(attrs);
    }

    /**
     * Sets the new font, retrieving it from the XML file
     * @param attrs attributesSet object
     */
    private void setFont(AttributeSet attrs) {

        if (attrs != null) {

            TypedArray mTypedArray = getContext().obtainStyledAttributes(attrs, R.styleable.TextViewCustomFont);
            String fontName = mTypedArray.getString(R.styleable.TextViewCustomFont_fontName);

            if (fontName != null) {
                Typeface customTypeface = null;

                if (!this.isInEditMode())
                    customTypeface = Typeface.createFromAsset(getContext().getAssets(), "fonts/" + fontName);

                setTypeface(customTypeface);
            }

            mTypedArray.recycle();
        }
    }

}