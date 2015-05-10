package com.tronacadmey.phantom.killalot;

import android.graphics.Bitmap;

import com.tronacademy.phantom.messaging.IncomingTransaction;
import com.tronacademy.phantom.messaging.Packet;
import com.tronacademy.phantom.messaging.ProtocolAssembler.DataType;

public class IncomingKillalotImageTransaction extends IncomingTransaction {
	
	private int mRows;
	private int mCols;
	
	// state trackers
	private int[] mStream;
	private int mStreamIndex = 0;

	/**
	 * @param packets  Number of packets in the transaction.
	 * @param rows     Number of rows in the bitmap.
	 * @param cols     Number of columns in the bitmap.	
	 * @param encoding How each pixel in encoded.
	 */
	public IncomingKillalotImageTransaction(int packets, int rows, int cols, byte encoding) {
		super(DataType.IMAGE, packets);
		
		mRows = rows;
		mCols = cols;
		
		// TODO: currently ignores encoding and assumes ARGB8888
		mStream = new int[rows*cols];
	}
	
	@Override
	public boolean capturePacket(final Packet packet) {
		// payload contains image data
		final byte[] payload = ((KillalotPacket) packet).getPayload();
		
		// TODO: currently ignores encoding and assumes ARGB8888
		for (int i=0; (i < KillalotPacket.PAYLOAD_LEN || mStreamIndex < mStream.length) ; i+=4) {
			mStream[mStreamIndex++] = (payload[i] << 24) | 
					                  (payload[i+1] << 16) | 
					                  (payload[i+2] << 8) | 
					                  (payload[i+3]);
		}
		
		return super.capturePacket(packet);
	}
	
	public Bitmap getResult() {
		// TODO: currently ignores encoding and assumes ARGB8888
		return Bitmap.createBitmap(mStream, mCols, mRows, Bitmap.Config.ARGB_8888);
	}
}
