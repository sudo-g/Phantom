package com.tronacademy.phantom.utils;

/**
 * <p>
 * Abstract class for all Mixers. Mixers process an array of bytes
 * representing input channels and processes them according to the
 * type of mixer and its configuration. 
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
