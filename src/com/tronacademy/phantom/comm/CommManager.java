package com.tronacademy.phantom.comm;

import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import android.content.Intent;

import com.tronacademy.phantom.messaging.OutgoingTransaction;

/**
 * 
 * @author George Xian
 * @since 2015-05-24
 *
 */
public abstract class CommManager {
	
	protected CommManagerListener mListener;
	protected boolean mConnected = false;
	protected boolean mBlocking = true;
	
	// sorts by OutgoingTransaction priority number (small is higher)
	private PriorityQueue<OutgoingTransaction> msgQ = 
			new PriorityQueue<OutgoingTransaction>(11, new Comparator<OutgoingTransaction>() {

				@Override
				public int compare(OutgoingTransaction lhs,
						OutgoingTransaction rhs) {
					return lhs.getPriority() - rhs.getPriority();
				}
			});
	
	/**
	 * Add an outgoing transaction to the transmission queue.
	 * 
	 * @param transaction Outgoing transaction to process.
	 * @return Flag indicating whether transaction was queued.
	 */
	public synchronized boolean processTransaction(OutgoingTransaction transaction) {
		return msgQ.offer(transaction);
	}
	
	/**
	 * Remove an outgoing transaction if it has not begun.
	 * 
	 * @param name String name of the transaction to remove.
	 * @return Flag indicating whether transaction was removed from the queue.
	 */
	public synchronized boolean removeTransaction(String name) {
		for (OutgoingTransaction trans : msgQ) {
			if (trans.getName() == name) {
				msgQ.remove(trans);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Return a list of outgoing transaction in the queue in the order
	 * are being processed.
	 * 
	 * @return List of outgoing transactions in the queue
	 */
	public List<OutgoingTransaction> getTransactionList() {
		return new LinkedList<OutgoingTransaction>(msgQ);
	}
	
	/**
	 * Starts a connection attempt
	 * 
	 * @return True if connection was initiated, false if no device was set.
	 */
	public abstract boolean connect() throws IOException;
	
	/**
	 * Disconnect from network.
	 */
	public abstract void disconnect();
	
	/**
	 * Disconnect from network, used this if disconnecting due to error.
	 * 
	 * @param error Error descriptor to append to.
	 */
	public abstract void disconnect(CommManagerError error);
	
	/**
	 * @return Flag indicating whether this CommManager works in blocking mode.
	 */
	public boolean isBlocking() {
		return mBlocking;
	}
	
	/**
	 * Read incoming message, blocking until a message is received.
	 * 
	 * @param Maximum number of milliseconds to block for.
	 * @return Byte stream of incoming message.
	 * @throws InterruptedException if thread was interrupted during read.
	 */
	public abstract byte[] read(int timeout) throws InterruptedException;
	
	/**
	 * Starts the background read process if non-blocking mode is available.
	 * 
	 * @return Flag indicating whether process was started.
	 */
	public abstract boolean startReadProcess();
	
	/**
	 * Stops the background read process.
	 */
	public abstract void stopReadProcess();

	/**
	 * <p>
	 * Callbacks for communication manager events.
	 * </p>
	 * 
	 * @author George Xian
	 * @since 2015-05-24
	 *
	 */
	public interface CommManagerListener {
		
		/**
		 * Callback when an OS intent is requested.
		 * 
		 * @param intent OS intent to request.
		 */
		public void onIntentRequest(Intent intent);
		
		/**
		 * Callback when a connection was successful.
		 */
		public void onConnectSuccess();
		
		/**
		 * Callback when a connection was unsuccessful.
		 */
		public void onConnectFailure();
		
		/**
		 * Callback when a message is received.
		 * 
		 * @param recv Message received.
		 */
		public void onMessageReceived(byte[] recv);
		
		/**
		 * Callback when CommManager encounters an error.
		 * 
		 * @param error Description of the error.
		 */
		public void onError(CommManagerError error);
	}
}
