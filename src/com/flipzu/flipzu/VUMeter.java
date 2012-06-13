/**
* Copyright 2011 Flipzu
*
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*  
*  Contributors: 
*  		Dario Rapisardi <dario@rapisardi.org>
*  		Nicolás Gschwind <nicolas@gschwind.com.ar>
*/
package com.flipzu.flipzu;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
//import android.util.Log;
import android.view.View;

public class VUMeter extends View {
    static final long  ANIMATION_INTERVAL = 100;
    private static final String TAG = "VUmeter";
    private Debug debug = new Debug();
    
    private static Paint mPaint;
    private Amplitude maxAmp = Amplitude.getInstance();

    public VUMeter(Context context) 
    {
        super(context);
        init(context);
    }

    public VUMeter(Context context, AttributeSet attrs) 
    {
        super(context, attrs);
        init(context);
    }

    void init(Context context) 
    {
    	debug.logD(TAG, "VUMeter: init()");
    	
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        
    }
    
    @Override
    protected void onDraw(Canvas canvas) 
    {
        super.onDraw(canvas);
        
//        debug.logD(TAG,"onDraw() called");
   
        Rect rect = canvas.getClipBounds();
        int height = rect.height();
        float width = (float)rect.width();
        
        
        
//        debug.logD(TAG, "onDraw height " + height + " width " + width);
        
        if (maxAmp!=null) {
        	int amplitude = maxAmp.getAmplitude();
        	
    		if ( amplitude > 32768*0.85 ) {
    			mPaint.setColor(Color.argb(255, 255, 8, 8));
    		} else if ( amplitude < 32768*0.5 ){
    			mPaint.setColor(Color.argb(255, 255, 243, 0)); 
    		}
    		
            float amp_step = width/32768.0f;
        	canvas.drawRect(0, height, amp_step*amplitude, 0, mPaint);
//        	debug.logD(TAG, "onDraw amplitude " + amplitude + " amp_step " + amp_step);        	            
        }
        postInvalidateDelayed(ANIMATION_INTERVAL);
        
        return;
    }
}

