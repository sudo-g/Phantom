package com.tronacademy.phantom.messaging;

import java.io.ByteArrayOutputStream;
import java.util.Queue;

/**
 * <p>
 * Handler for an outgoing transmission process.
 * </p>
 * 
 * @author George Xian
 * @since 2015-05-04
 */
public class OutgoingTransaction {
	
	private final Queue<ByteArrayOutputStream> mOutgoingStream;
	private final int mJobSize;
	
	public final int mPriority;
	
	/**
	 * The queue of byte streams to transmit over the network.
	 * 
	 * @param name     String name of the transaction process.
	 * @param stream   Byte stream to transmit.
	 * @param priority Priority of the transmission (lower is higher)
	 */
	public OutgoingTransaction(String name, Queue<ByteArrayOutputStream> stream, int priority) {
		mOutgoingStream = stream;
		mJobSize = mOutgoingStream.size();
		
		mPriority = priority;
	}
	
	/**
	 * @return Flag indicating whether transaction is complete.
	 */
	public boolean hasComplete() {
		return mOutgoingStream.size() > 0;
	}
	
	/**
	 * @return Next byte stream of this transaction to transmit.
	 */
	public ByteArrayOutputStream getNext() {
		return mOutgoingStream.remove();
	}
	
	/**
	 * @return Completion of this transaction in percent.
	 */
	public int getProgress() {
		return 100*mOutgoingStream.size() / mJobSize;
	}

}
