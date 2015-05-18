package com.tronacademy.phantom.mixer;

public class ChannelStreamSizeMismatchException extends Exception {

	private static final long serialVersionUID = -4563361842915392206L;
	
	private static final String errMsg = 
			"Input channel stream size mis-match, '%s' mixer only accepts channel stream of size %d but passed in stream of size %d ";
	
	public ChannelStreamSizeMismatchException(String mixName, int mixSize, int streamSize) {
		super(String.format(errMsg, mixName, mixSize, streamSize));
	}

}
