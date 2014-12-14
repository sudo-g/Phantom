package com.tronacademy.phantom;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import android.util.Log;

/**
 * <p>
 * Abstract class for all Mixers, Phantom pipeline components which 
 * are between the ControlInputs and FramePackagers.
 * </p>
 * 
 * <p>
 * Control inputs such as joysticks, seekbars and buttons are 
 * connected to the input channels of mixers. The channel values
 * are then mixed together via some processing specific to that
 * mixer. The output channel of Mixers are connected to frame
 * packagers.
 * </p>
 * 
 * @author George Xian
 * @since 2014-12-14
 *
 */
public abstract class Mixer {
	private static final String TAG = "Phantom::Mixer";
	
	private FramePackager packager = null;
	// prevent packager from being changed whilst a send event is occurring
	private Semaphore packagerSem = new Semaphore(1);
	
	/**
	 * <p>
	 * Select which frame packager to use in the pipeline
	 * </p>
	 * 
	 * @param pkg Frame packager instance to use in pipeline
	 * @param timeout Milliseconds to keep trying to change packager before timing out
	 * @return Flag representing whether changing was successful in timeout period
	 */
	public boolean setFramePackager(FramePackager pkg, int timeout) {
		boolean packagerSemAcquired = false;
		try {
			packagerSemAcquired = packagerSem.tryAcquire(timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Log.e(TAG, "Acquiring packagerSem was interrupted");
			e.printStackTrace();
		}
		if (packagerSemAcquired) {
			packager = pkg;
			packagerSem.release();
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * 
	 * @param channel
	 */
	public void updateChannelValue(int channel) {
		
	}
}
