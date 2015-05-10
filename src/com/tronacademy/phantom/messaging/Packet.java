package com.tronacademy.phantom.messaging;

import java.io.ByteArrayOutputStream;

/**
 * <p>
 * Methods common to all packet container objects for
 * packet switched networks.
 * </p>
 * 
 * @author George Xian
 * @since 2015-05-10
 *
 */
public interface Packet {
	
	/**
	 * Get serial form for transmission.
	 * 
	 * @return Byte stream with escape characters added.
	 */
	public ByteArrayOutputStream serialize();
	
	/**
	 * @return Size of the serialized form in bytes.
	 */
	public int getSize();
	
	/**
	 * Returns byte stream in numerical string form for testing.
	 * 
	 * @return String of the byte stream
	 * @throws IOException 
	 */
	public String stringForm();

}
