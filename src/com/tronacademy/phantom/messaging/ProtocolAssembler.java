package com.tronacademy.phantom.messaging;

import java.io.ByteArrayOutputStream;
import java.util.List;

import android.graphics.Bitmap;

/**
 * <p>
 * Methods common to all Phantom protocol assemblers. 
 * ProtocolAssemblers transform structured data into a 
 * format as specified by the protocol of that packager.
 * This process often involves adding additional bytes
 * for purposes such as error checking and denoting 
 * the beginning and ends of a message. It then 
 * serializes the formatted data for transmission 
 * over a network.
 * </p>
 * 
 * <p>
 * Some protocols are packet switched, to support
 * this, the methods which format the data for messaging
 * return an array of {@code ByteArrayOutputStream}s
 * as large data structures may need to be divided into 
 * multiple packets.
 * </p>
 * 
 * <p>
 * Phantom protocol assemblers must support the 
 * following types of data:
 * 	<ul>
 * 		<li>Channel streams</li>
 * 		<li>Terminal commands</li>
 * 		<li>Images</li>
 * 	</ul>
 * All Phantom protocol suites must define how each 
 * of these types of data are encoded and interpreted.
 * </p>
 * 
 * <p>
 * Serializing the data as {@code binary} allows
 * generic structured data to be supported. This 
 * allows custom higher level protocols for custom 
 * data structures.
 * </p>
 * 
 * 
 * @author George Xian
 * @since 2015-04-26
 *
 */
public interface ProtocolAssembler {
	
	public enum DataType {CHANNEL, COMMAND, IMAGE, BINARY};
	
	/**
	 * Assemble control channel stream data for messaging.
	 * 
	 * @param channelStream The control channel stream.
	 * @return The packets of serial streams to send. 
	 * @throws AssemblyException 
	 */
	public List<ByteArrayOutputStream> serializeAsChannels(byte[] channelStream) 
			throws AssemblyException;
	
	/**
	 * Assemble terminal commands for messaging.
	 * 
	 * @param command The terminal command as a string.
	 * @return The packets of serial streams to send.
	 * @throws AssemblyException
	 */
	public List<ByteArrayOutputStream> serializeAsCommands(String command) 
			throws AssemblyException;
	
	/**
	 * Assemble a 8 bit per color RGB bitmap image for imaging.
	 * 
	 * @param image Raw image as bitmap.
	 * @return The packets of serial streams to send.
	 * @throws AssemblyException
	 */
	public List<ByteArrayOutputStream> serializeAsBitmapRGBA8(Bitmap image)
			throws AssemblyException;
	
	/**
	 * Assemble generic data for messaging.
	 * 
	 * @param data  Generic data as byte-stream
	 * @return The packets of serial streams to send.
	 * @throw AssemblyException
	 */
	public List<ByteArrayOutputStream> serializeAsBinary(ByteArrayOutputStream data) 
			throws AssemblyException;
}