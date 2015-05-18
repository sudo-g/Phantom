package com.tronacademy.phantom.messaging;

import com.tronacademy.phantom.messaging.ProtocolAssembler.DataType;

/**
 * <p>
 * Holds context 
 * </p>
 * 
 * @author George Xian
 * @since 2015-05-04
 */
public class IncomingTransaction {
	
	private DataType mType;
	private int mTotalPackets;
	private int mPacketsToGo;
	
	/**
	 * @param type    Type of data being decoded.
	 * @param packets Packets to process in this transaction.
	 */
	public IncomingTransaction(DataType type, int packets) {
		mType = type;
		mTotalPackets = packets;
		mPacketsToGo = packets;
	}
	
	/**
	 * Signal that one packet has been processed.
	 * 
	 * @param packet Incoming packet as a byte stream.
	 * @return Flag indicating whether transaction is completed.
	 */
	public boolean capturePacket(final Packet packet) {
		mPacketsToGo--;
		return (mPacketsToGo <= 0);
	}
	
	/**
	 * @return Remaining number of packets to decode.
	 */
	public int getPacketsToGo() {
		return mPacketsToGo;
	}
	
	/**
	 * @return Data type being decoded.
	 */
	public DataType getType() {
		return mType;
	}
	
	/**
	 * @return Decoding progress in percent.
	 */
	public int getProgress() {
		return 100*mPacketsToGo / mTotalPackets;
	}
	
	/**
	 * @return Object decoded from transaction if completed, null if error.
	 */
	public Object getDecodedResult() {
		return null;
	}
	
	/**
	 * @return A list of errors that occurred during transaction.
	 */
	public TransactionError getTransactionErrors() {
		return null;
	}

}
