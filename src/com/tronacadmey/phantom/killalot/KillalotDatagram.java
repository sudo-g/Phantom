package com.tronacadmey.phantom.killalot;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/**
 * Packet structure for SLIP protocol developed for use 
 * with project Killalot. Frame format is:
 * <ul>
 * 	<li>4 bytes header
 * 	<li>8 bytes payload
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
public class KillalotDatagram {
	
	// SLIP values
	public static final int HEADER_LEN = 4;
	public static final int PAYLOAD_LEN = 8;
	
	public static final byte SLIP_END = (byte) 0xC0;
	public static final byte SLIP_ESC = (byte) 0xDB;
	public static final byte SLIP_ESC_END = (byte) 0xDC;
	public static final byte SLIP_ESC_ESC = (byte) 0xDD;
	
	// Byte stream form cache
	private ByteArrayOutputStream mByteStreamForm = new ByteArrayOutputStream(26);
	private int mStreamLen = 0; 
	
	// Frame data
	private byte[] mHeader = new byte[HEADER_LEN];
	private byte[] mPayload = new byte[PAYLOAD_LEN];
	
	/**
	 * @param header Header data of this datagram.
	 * @param payload Data if this datagram.
	 */
	public KillalotDatagram(byte[] header, byte[] payload) {
		mHeader = Arrays.copyOfRange(header, 0, HEADER_LEN);
		mPayload = Arrays.copyOfRange(payload, 0, PAYLOAD_LEN);
		
		computeByteStreamForm();	
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
	
	/**
	 * Get serial form for transmission.
	 * 
	 * @return Byte stream with escape characters added.
	 */
	public ByteArrayOutputStream byteStreamForm() {
		return mByteStreamForm;
	}
	
	/**
	 * Returns byte stream in numerical string form for testing.
	 * 
	 * @return String of the byte stream
	 * @throws IOException 
	 */
	public String stringForm() {
		byte[] byteArray = mByteStreamForm.toByteArray();
		
		StringBuilder strBuilder = new StringBuilder();
		for (int i=0; i<mStreamLen; i++) {
			strBuilder.append(String.format("%d ", byteArray[i]));
		}
		return strBuilder.toString();
	}
	
	private void computeByteStreamForm() { 
		// write begin character
		mByteStreamForm.write(SLIP_END);
		mStreamLen++;
		
		// write header
		for (int i=0; i<HEADER_LEN; i++) {
			switch(mHeader[i]) {
			case(SLIP_END):
				mByteStreamForm.write(SLIP_ESC);
				mByteStreamForm.write(SLIP_ESC_END);
				mStreamLen += 2;
				break;
			case(SLIP_ESC):
				mByteStreamForm.write(SLIP_ESC);
				mByteStreamForm.write(SLIP_ESC_ESC);
				mStreamLen += 2;
				break;
			default:
				mByteStreamForm.write(mHeader[i]);
				mStreamLen++;
			} 
		}
		
		// write payload
		for (int i=0; i<PAYLOAD_LEN; i++) {
			switch(mPayload[i]) {
			case(SLIP_END):
				mByteStreamForm.write(SLIP_ESC);
				mByteStreamForm.write(SLIP_ESC_END);
				mStreamLen += 2;
				break;
			case(SLIP_ESC):
				mByteStreamForm.write(SLIP_ESC);
				mByteStreamForm.write(SLIP_ESC_ESC);
				mStreamLen += 2;
				break;
			default:
				mByteStreamForm.write(mPayload[i]);
				mStreamLen++;
			}
		}
		
		// write end character
		mByteStreamForm.write(SLIP_END);
		mStreamLen++;
	}
}