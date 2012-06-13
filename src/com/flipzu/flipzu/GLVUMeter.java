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

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLU;
import android.opengl.GLSurfaceView.Renderer;

public class GLVUMeter implements Renderer {
	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * android.opengl.GLSurfaceView.Renderer#onSurfaceCreated(javax.
         * microedition.khronos.opengles.GL10, javax.microedition.khronos.
         * egl.EGLConfig)
	 */
	private static final String TAG = "GLVUmeter";
    private Debug debug = new Debug();
	private Square square = new Square();
	private static float STEP = 25.0f/32768;
	
	Recorder mRecorder = null;
	
	private static int NUM_BARS = 1;
	private float[] amps = new float[NUM_BARS];
	
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		// Set the background color ( rgba ).
		gl.glClearColor(31.0f/255.0f, 34.0f/255.0f, 71.0f/255.0f, 0.0f);
		// Enable Smooth Shading, default not really needed.
		gl.glShadeModel(GL10.GL_SMOOTH);
		// Depth buffer setup.
		gl.glClearDepthf(1.0f);
		// Enables depth testing.
		gl.glEnable(GL10.GL_DEPTH_TEST);
		// The type of depth testing to do.
		gl.glDepthFunc(GL10.GL_LEQUAL);
		// Really nice perspective calculations.
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT,
                          GL10.GL_NICEST);
		
	}

	public void setRecorder(Recorder recorder) 
    {
    	debug.logD(TAG, "VUMeter: setRecorder() called");
    	mRecorder = recorder;
    	
    	for ( int i = 0; i < NUM_BARS; i++) {
    		amps[i] = 0;
    	}
    }
    
	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * android.opengl.GLSurfaceView.Renderer#onDrawFrame(javax.
         * microedition.khronos.opengles.GL10)
	 */
	public void onDrawFrame(GL10 gl) {
		// Clears the screen and depth buffer.
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT |
                           GL10.GL_DEPTH_BUFFER_BIT);

		
		if ( mRecorder == null ) {
			debug.logD(TAG, "GLVUMeter: recorder is null!");
			return;
		}
		
//		float amplitude = mRecorder.getMaxAmplitude();
		float amplitude = 0;
		for ( int i=NUM_BARS-1; i > 0; i-- ) {
			amps[i] = amps[i-1];
		}
		amps[0] = amplitude;
		
		//debug.logD(TAG, "GLVUMeter: amplitude " + amplitude + " step  " + amplitude*STEP);
		
		gl.glLoadIdentity();
		gl.glTranslatef(-29.5f, 0.0f, -0.5f);
		
				
		if ( amplitude > 32768*0.85 ) {
			gl.glColor4f(172.0f/255.0f, 8.0f/255.0f, 8.0f/255.0f, 0.0f); // RED	
		} else if ( amplitude < 32768*0.5 ){
			gl.glColor4f(44.0f/255.0f, 82.0f/255.0f, 157.0f/255.0f, 0.0f); 
		}
				
		for ( int i=0; i < NUM_BARS; i++ ) {
			gl.glPushMatrix();
			gl.glTranslatef(amps[i]*STEP, 0, 0);
			square.draw(gl);
			gl.glPopMatrix();
		}
		square.draw(gl);
		gl.glPopMatrix();
		
		

		
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * android.opengl.GLSurfaceView.Renderer#onSurfaceChanged(javax.
         * microedition.khronos.opengles.GL10, int, int)
	 */
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		// Sets the current view port to the new size.
		gl.glViewport(0, 0, width, height);
		// Select the projection matrix
		gl.glMatrixMode(GL10.GL_PROJECTION);
		// Reset the projection matrix
		gl.glLoadIdentity();
		// Calculate the aspect ratio of the window
		GLU.gluPerspective(gl, 45.0f,
                                   (float) width / (float) height,
                                   0.1f, 100.0f);
		// Select the modelview matrix
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		// Reset the modelview matrix
		gl.glLoadIdentity();
	}
}
