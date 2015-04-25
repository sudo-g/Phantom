package com.tronacademy.phantom.mixer;

/**
 * <p>
 * This unit defines generic methods that apply to all Mixers.
 * </p>
 * 
 * <p>
 * Mixers perform mathematical operations on input channel 
 * streams and returns the resultant output channel stream
 * of the same size as the input.
 * </p>
 * 
 * <p>
 * The size of the channel stream the mixer accepts is fixed
 * upon creation. Performing mix on a channel stream of the 
 * wrong size will cause an exception. 
 * </p>
 * 
 * <p>
 * Different mixers have different methods to customize them.
 * However, they all have a {@code mix} method which accepts 
 * an input channel stream and performs their mathematical
 * operation and return the resultant output channel stream.
 * </p>
 * 
 * @author George Xian
 * @since 2014-12-14
 *
 */
public abstract class Mixer {
	
	private static final String negSizeErrMsg = "%s mixer cannot have negative number of channels";
	protected static final String chanIndexErrMsg = "Channel range of %s mixer is [0, %d), requested channel %d";
	
	private String mName;
	private int mNumChans;
	
	public Mixer(int channels, String name) throws NegativeArraySizeException {
		mName = name;
		
		if (channels >= 0) {
			mNumChans = channels;
		} else {
			throw new NegativeArraySizeException(String.format(negSizeErrMsg, mName));
		}
		
	}
	
	/**
	 * @return String name of the Mixer type.
	 */
	public String getName() {
		return mName;
	}
	
	/**
	 * @return Number of channels this mixer can mix.
	 */
	public int getNumChans() {
		return mNumChans;
	}
	
	/**
	 * Process input channels and send and output result to a frame packager.
	 * 
	 * @param inputChans Input channel stream.
	 * @return Resultant output channel stream.
	 * @throws ChannelStreamSizeMismatchException if input channel stream is wrong size.
	 */
	public byte[] mix(byte[] inputChans) throws ChannelStreamSizeMismatchException {
		if (inputChans.length != mNumChans) {
			throw new ChannelStreamSizeMismatchException(mName, mNumChans, inputChans.length);
		}
		return mixOperation(inputChans);
	}
	
	/**
	 * Override to implement custom mathematical operation for mixer. 
	 * 
	 * @param inputChans Input channel stream.
	 * @return Resultant output channel stream.
	 */
	protected abstract byte[] mixOperation(byte[] inputChans);
}
