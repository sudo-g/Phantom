package com.tronacademy.phantom.messaging;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

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
	
	private final int mPriority;
	private final String mName;
	
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
		mName = name;
	}
	
	public OutgoingTransaction(OutgoingTransaction trans) {
		mJobSize = trans.mJobSize;
		// TODO: clone() is not working
		mOutgoingStream = new LinkedBlockingQueue<ByteArrayOutputStream>(trans.showContents());
		
		mName = trans.mName;
		mPriority = trans.mPriority;
		
	}
	
	/**
	 * @return String name for this transaction.
	 */
	public String getName() {
		return mName;
	}
	
	/**
	 * @return Priority for this transaction.
	 */
	public int getPriority() {
		return mPriority;
	}
	
	public int getNumPackets() {
		return mOutgoingStream.size();
	}
	
	/**
	 * @return Flag indicating whether transaction is complete.
	 */
	public boolean hasComplete() {
		return mOutgoingStream.size() > 0;
	}
	
	/**
	 * @return Next byte stream of this transaction to transmit, null if complete.
	 */
	public InputStream getNext() {
		return new ByteArrayInputStream(mOutgoingStream.poll().toByteArray());
	}
	
	/**
	 * @return Completion of this transaction in percent.
	 */
	public int getProgress() {
		return 100*mOutgoingStream.size() / mJobSize;
	}

	/**
	 * @return A copy of the contents of this transaction.
	 */
	public List<ByteArrayOutputStream> showContents() {
		 return new ArrayList<ByteArrayOutputStream>(mOutgoingStream);
	}
}
