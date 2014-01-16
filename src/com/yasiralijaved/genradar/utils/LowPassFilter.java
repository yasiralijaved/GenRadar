package com.yasiralijaved.genradar.utils;
/**
 * 
 */


/**
 * @author Yasir.Ali
 *
 */
public class LowPassFilter {
	
	static final float SmoothFactorCompass = 0.2f;
	static final float SmoothThresholdCompass = 150.0f;
	static final float oldCompass = 0.0f;
	
	/*
     * time smoothing constant for low-pass filter
     * 0 ≤ α ≤ 1 ; a smaller value basically means more smoothing
     * See: http://en.wikipedia.org/wiki/Low-pass_filter#Discrete-time_realization
     */
    static final float ALPHA = 0.15f;
    
	/**
	 * Some times this method required for 2D Maps (2D Plan)
     * @see http://en.wikipedia.org/wiki/Low-pass_filter#Algorithmic_implementation
     * @see http://developer.android.com/reference/android/hardware/Sensor.html#TYPE_ACCELEROMETER
     */
    public static float[] filter2D( float[] input, float[] output ) {
            if ( output == null ) return input;

            for ( int i=0; i<input.length; i++ ) {
                    output[i] = output[i] + ALPHA * (input[i] - output[i]);
            }
            return output;
    }
	
	public static float filter3D(float newCompass, float oldCompass){
		if (Math.abs(newCompass - oldCompass) < 180) {
		    if (Math.abs(newCompass - oldCompass) > SmoothThresholdCompass) {
		        oldCompass = newCompass;
		    }
		    else {
		        oldCompass = oldCompass + SmoothFactorCompass * (newCompass - oldCompass);
		    }
		}
		else {
		    if (360.0 - Math.abs(newCompass - oldCompass) > SmoothThresholdCompass) {
		        oldCompass = newCompass;
		    }
		    else {
		        if (oldCompass > newCompass) {
		            oldCompass = (oldCompass + SmoothFactorCompass * ((360 + newCompass - oldCompass) % 360) + 360) % 360;
		        } 
		        else {
		            oldCompass = (oldCompass - SmoothFactorCompass * ((360 - newCompass + oldCompass) % 360) + 360) % 360;
		        }
		    }
		}
		return oldCompass;
	}
}
