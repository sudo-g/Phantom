package com.tronacadmey.phantom.killalot;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import com.tronacademy.phantom.messaging.Packet;

/**
 * Packet structure for SLIP protocol developed for use 
 * with project Killalot. Frame format is:
 * <ul>
 * 	<li>4 bytes header</li>
 * 	<li>8 bytes payload</li>
 * </ul>
 * SLIP END characters are both appended and prepended to 
 * the byte stream to delimit the packet.
 * 
 * <p>
 * ESC character defined by SLIP as 0xDB, prefixes bytes to 
 * allow 0xC0 and 0xDB characters to appear inside the frame
 * but get replaced by 0xDC and 0xDD respectively.
 * </p>
 * 
 * @author George Xian
 * @since 2014-07-08
 *
 */
public class KillalotPacket implements Packet {
	
	// SLIP values
	public static final int HEADER_LEN = 4;
	public static final int PAYLOAD_LEN = 8;
	public static final int WORST_CASE_PACKET_LEN = 26;
	
	public static final byte SLIP_END = (byte) 0xC0;
	public static final byte SLIP_ESC = (byte) 0xDB;
	public static final byte SLIP_ESC_END = (byte) 0xDC;
	public static final byte SLIP_ESC_ESC = (byte) 0xDD;
	
	// Byte stream form
	private final ByteArrayOutputStream mByteStreamForm;
	private int mStreamLen = 0; 
	
	// Frame data
	private final byte[] mHeader;
	private final byte[] mPayload;
	
	/**
	 * @param header Header data of this datagram.
	 * @param payload Data if this datagram.
	 */
	public KillalotPacket(byte[] header, byte[] payload) {
		mHeader = Arrays.copyOfRange(header, 0, HEADER_LEN);
		mPayload = Arrays.copyOfRange(payload, 0, PAYLOAD_LEN);
		
		mByteStreamForm = computeByteStreamForm();	
	}
	
	/**
	 * Create a datagram from raw byte stream,
	 * 
	 * @param byteStream Stream without pre-inserted escape characters.
	 */
	public KillalotPacket(byte[] byteStream) {
		mHeader = Arrays.copyOfRange(byteStream, 0, HEADER_LEN);
		mPayload = Arrays.copyOfRange(byteStream, HEADER_LEN, HEADER_LEN + PAYLOAD_LEN);
		
		mByteStreamForm = computeByteStreamForm();
	}
	
	/**
	 * @return Header field of this frame.
	 */
	public byte[] getHeader() {
		return mHeader;
	}
	
	/**
	 * @return Data of this frame. 
	 */
	public byte[] getPayload() {
		return mPayload;
	}
	
	@Override
	public ByteArrayOutputStream serialize() {
		return mByteStreamForm;
	}
	
	@Override 
	public int getSize() {
		return mStreamLen;
	}
	
	public static int getDecodedSize() {
		return HEADER_LEN + PAYLOAD_LEN;
	}
	
	@Override
	public String stringForm() {
		byte[] byteArray = mByteStreamForm.toByteArray();
		
		StringBuilder strBuilder = new StringBuilder();
		for (int i=0; i<mStreamLen; i++) {
			strBuilder.append(String.format("%d ", byteArray[i]));
		}
		return strBuilder.toString();
	}
	
	private ByteArrayOutputStream computeByteStreamForm() { 
		final ByteArrayOutputStream out = new ByteArrayOutputStream(WORST_CASE_PACKET_LEN);
		
		// write begin character
		out.write(SLIP_END);
		mStreamLen++;
		
		// write header
		for (int i=0; i<HEADER_LEN; i++) {
			switch(mHeader[i]) {
			case(SLIP_END):
				out.write(SLIP_ESC);
				out.write(SLIP_ESC_END);
				mStreamLen += 2;
				break;
			case(SLIP_ESC):
				out.write(SLIP_ESC);
				out.write(SLIP_ESC_ESC);
				mStreamLen += 2;
				break;
			default:
				out.write(mHeader[i]);
				mStreamLen++;
			} 
		}
		
		// write payload
		for (int i=0; i<PAYLOAD_LEN; i++) {
			switch(mPayload[i]) {
			case(SLIP_END):
				out.write(SLIP_ESC);
				out.write(SLIP_ESC_END);
				mStreamLen += 2;
				break;
			case(SLIP_ESC):
				out.write(SLIP_ESC);
				out.write(SLIP_ESC_ESC);
				mStreamLen += 2;
				break;
			default:
				out.write(mPayload[i]);
				mStreamLen++;
			}
		}
		
		// write end character
		out.write(SLIP_END);
		mStreamLen++;
		
		return out;
	}
}