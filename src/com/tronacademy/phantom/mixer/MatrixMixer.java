package com.tronacademy.phantom.mixer;

import org.ejml.simple.*;

/**
 * <p>
 * Matrix mixers produces an output channel stream by 
 * matrix multiplication of the mix matrix to the input 
 * channel stream as a matrix. 
 * </p>
 * 
 * @author George Xian
 * @since 2015-04-20
 * 
 */
public class MatrixMixer extends Mixer {
	
	private static final String matIndxErrMsg = "'%s' matrix mixer has no mix co-efficient at [%d, %d]";
	
	SimpleMatrix mixMat;

	public MatrixMixer(int channels, String name) {
		super(channels, name);
		
		mixMat = SimpleMatrix.identity(channels);
	}
	
	/**
	 * Gets the mix matrix element value at specified position.
	 * 
	 * @param row Vertical index of element to request.
	 * @param col Horizontal index of element to request.
	 * @return Value of co-efficient at requested position.
	 * @throws ArrayIndexOutOfBoundsException if requested position is out of bounds.
	 */
	public double getMixCoefficientAt(int row, int col) throws ArrayIndexOutOfBoundsException {
		if (row>=0 && row<getNumChans() && col>=0 && row<getNumChans()) {
			return mixMat.get(row, col);
		} else {
			throw new ArrayIndexOutOfBoundsException(
					String.format(matIndxErrMsg, getName(), row, col));
		}
	}
	
	/**
	 * Set the value of the mix matrix element at specified position.
	 * 
	 * @param row Vertical index of element to alter.
	 * @param col Horizontal index of element to alter.
	 * @param val Value to set mix-coefficient to.
	 * @throws ArrayIndexOutOfBoundsException if specified position is out of bounds.
	 */
	public void setMixCoefficient(int row, int col, double val) throws 
	ArrayIndexOutOfBoundsException {
		if (row>=0 && row<getNumChans() && col>=0 && row<getNumChans()) {
			mixMat.set(row, col, val);
		} else {
			throw new ArrayIndexOutOfBoundsException(
					String.format(matIndxErrMsg, getName(), row, col));
		}
	}
	
	/**
	 * Makes the mix matrix an identity matrix.
	 */
	public void setMixToIdentity() {
		mixMat = SimpleMatrix.identity(getNumChans());
	}

	@Override
	protected byte[] mixOperation(byte[] inputChans) {
		
		// put input channels into a matrix
		SimpleMatrix inputChannels = new SimpleMatrix(getNumChans(), 1);
		for (int i=0; i<getNumChans(); i++) {
			inputChannels.set(i, (double) inputChans[i]);
		}
		
		// perform the mix
		SimpleMatrix outputChannels = mixMat.mult(inputChannels);
		
		// convert to byte array
		byte[] out = new byte[getNumChans()];
		for (int i=0; i<getNumChans(); i++) {
			out[i] = (byte) outputChannels.get(i);
		}
		
		return out;
	}
}
