package com.tronacadmey.phantom.killalot;

import com.tronacademy.phantom.messaging.IncomingTransaction;
import com.tronacademy.phantom.messaging.PBitmap;
import com.tronacademy.phantom.messaging.PBitmap.Encoding;
import com.tronacademy.phantom.messaging.Packet;
import com.tronacademy.phantom.messaging.ProtocolAssembler.DataType;

public class IncomingKillalotImageTransaction extends IncomingTransaction {
	
	final private int mWidth;
	final private int mHeight;
	final private Encoding mEnc;
	
	// state trackers
	final private int[] mStream;
	private int mStreamIndex = 0;

	/**
	 * @param packets  Number of packets in the transaction.
	 * @param rows     Number of rows in the bitmap.
	 * @param cols     Number of columns in the bitmap.	
	 * @param encoding How each pixel in encoded.
	 */
	public IncomingKillalotImageTransaction(int packets, int width, int height, Encoding encoding) {
		super(DataType.IMAGE, packets);
		
		mWidth = width;
		mHeight = height;
		mEnc = encoding;
		
		int bits = 0;
		switch (encoding) {
		case RGB565:
			// 2B per pixel
			bits = (width * height) * Short.SIZE;
			break;
		case ARGB8888:
			// 4B per pixel
			bits = (width * height) * Integer.SIZE;
			break;
		}

		mStream = new int[bits / Integer.SIZE];
	}
	
	@Override
	public boolean capturePacket(final Packet packet) {
		// payload contains image data
		final byte[] payload = ((KillalotPacket) packet).getPayload();
		
		final int increment = Integer.SIZE / Byte.SIZE;
		for (int i=0; 
			(i<KillalotPacket.PAYLOAD_LEN) && (mStreamIndex<mStream.length); 
			i+=increment) {
			
			mStream[mStreamIndex++] = (payload[i] << 24) | 
					                  (payload[i+1] << 16) | 
					                  (payload[i+2] << 8) | 
					                  (payload[i+3]);
		}
		
		return super.capturePacket(packet);
	}
	
	@Override
	public Object getDecodedResult() {
		return new PBitmap(mStream, mEnc, mWidth, mHeight);
	}
}
