package com.tronacademy.phantom.messaging;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;

/**
 * <p>
 * Custom bitmap class to make bitmaps portable.
 * </p>
 * 
 * <p>
 * Mostly follows standard bitmap implementation for
 * both {@code java.awt.BufferedImage} and 
 * {@code android.graphics.Bitmap}. 
 * </p>
 * 
 * <p>
 * The encoding specifies how the data is stored
 * </p>
 * 
 * @author George Xian
 * @since 2015-05-16
 *
 */
public class PBitmap {
	
	public enum Encoding {RGB565, ARGB8888};
	
	private final int[] mBinary;
	private final Encoding mEnc;
	private final int mWidth;
	private final int mHeight;
	
	/**
	 * @param colors   Packed int representation of pixel data.
	 * @param width    Width of image in pixels.
	 * @param height   Height of image in pixels.
	 * @param encoding Encoding of pixel data in {@code colors} array.
	 * @throws NegativeArraySizeException if negative was specified for width or height.
	 */
	public PBitmap(int[] colors, Encoding encoding, int width, int height) 
			throws NegativeArraySizeException {
		
		if (width < 1 || height < 1) {
			throw new NegativeArraySizeException("Width or height for image cannot be less than 1"); 
		}
		mWidth = width;
		mHeight = height;
		
		mBinary = Arrays.copyOf(colors, colors.length);
		mEnc = encoding;
	}
	
	/**
	 * @return Encoding of image.
	 */
	public Encoding getEncoding() {
		return mEnc;
	}
	
	/**
	 * @return Width of image in pixels.
	 */
	public int getWidth() {
		return mWidth;
	}
	
	/**
	 * @return Height of image in pixels.
	 */
	public int getHeight() {
		return mHeight;
	}
	
	/**
	 * @return Size of image in bytes.
	 */
	public int getSizeInBytes() {
		return mBinary.length * Integer.SIZE / Byte.SIZE;
	}
	
	/**
	 * Serialize data into stream.
	 * 
	 * @return Raw stream of image.
	 */
	public InputStream serialize() {
		final byte[] byteArray = new byte[getSizeInBytes()];
		int index = 0;
		for (int data : mBinary) {
			byteArray[index++] = (byte) ((data >>> 24) & 0xFF);
			byteArray[index++] = (byte) ((data >>> 16) & 0xFF);
			byteArray[index++] = (byte) ((data >>> 8) & 0xFF);
			byteArray[index++] = (byte) (data & 0xFF);
		}
		
		return new ByteArrayInputStream(byteArray);
	}
	
	/**
	 * <p>
	 * Returns a reference to the raw pixel data of image.
	 * </p>
	 * 
	 * <p>
	 * Warning: {@code PBitmap} is meant to immutable, however
	 * it is possible to change the image data by using this 
	 * method and performing an assignment on one of the elements.
	 * Please avoid doing this. Performance is the reason why
	 * this method is unsafe.
	 * </p>
	 * 
	 * @return Raw pixel data of image.
	 */
	public int[] getRawData() {
		return mBinary;
	}
}
