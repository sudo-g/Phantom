package com.tronacademy.phantom.utils;

/**
 * <p>
 * Mixer.java
 * </p>
 * 
 * <p>
 * Methods common to all Mixers. 
 * Objects which processes input channel values and returns
 * an array of values representing output channels.
 * </p>
 * 
 * @author George Xian
 * @since 2014-12-14
 *
 */
public interface Mixer {
	
	/**
	 * <p> 
	 * Reveals an interface to configure this Mixer instance 
	 * </p>
	 */
	public void configure();
	
	/**
	 * <p>
	 * Process input channels and send and output result to a frame packager
	 * </p>
	 * 
	 * @param inputChans Value of each input channel
	 * @return Resultant output channel values
	 */
	public byte[] mix(byte[] inputChans);
}
