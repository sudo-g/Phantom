package com.tronacademy.phantom.messaging;

import java.io.ByteArrayOutputStream;

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
	 * @return Get string name of this protocol assembler type.
	 */
	public String getName();
	
	/**
	 * Assemble control channel stream data for messaging.
	 * 
	 * @param name          String name of transaction process to generate.
	 * @param channelStream The control channel stream.
	 * @return Transaction handler for this channel stream, null if assembly failed. 
	 */
	public OutgoingTransaction serializeAsChannels(String name, byte[] channelStream);
	
	/**
	 * Assemble terminal commands for messaging.
	 * 
	 * @param name    String name of the transaction process to generate.
	 * @param command The terminal command as a string.
	 * @return Transaction handler for this command string, null if assembly failed.
	 */
	public OutgoingTransaction serializeAsCommands(String name, String command);
	
	/**
	 * Assemble a 8 bit per color RGB bitmap image for imaging.
	 * 
	 * @param name  String name of the transaction progress to generate.
	 * @param image Raw image as bitmap.
	 * @return Transaction handler for this bitmap, null if assembly failed.
	 */
	public OutgoingTransaction serializeAsBitmap(String name, Bitmap image);
	
	/**
	 * Assemble generic data for messaging.
	 * 
	 * @param name  String name of the transaction progress to generate.
	 * @param data  Generic data as byte-stream
	 * @return Transaction handler for this binary data, null if assembly failed.
	 */
	public OutgoingTransaction serializeAsBinary(String name, ByteArrayOutputStream data);
}