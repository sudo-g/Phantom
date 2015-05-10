 package com.tronacademy.phantom.messaging;

import java.io.IOException;
import java.io.InputStream;

import com.tronacademy.phantom.comm.CommManager;

import android.graphics.Bitmap;

/**
 * <p>
 * Methods common to all Phantom protocol decoders.
 * ProtocolDecoders act as a proxy to {@code CommManager}
 * to retrieve structured data instead of raw byte 
 * streams. Hence they must be bound to {@code CommManager}
 * instances. 
 * </p>
 * 
 * <p>
 * ProtocolDecoder are asynchronous, callbacks are
 * executed when a file has been fully decoded.
 * </p>
 * 
 * @author George Xian
 * @since 2015-05-03
 * 
 */
public abstract class ProtocolDecoder {
	
	public final static int DEFAULT_POLLING_INT = 100;
	
	public enum RequestType {STOP, RESEND};
	
	/**
	 * This enumeration contains errors from various sources:
	 * <ul>
	 * 	<li>SYS: Raised by the application</li>
	 * 	<li>NET: Broadcast by one of the nodes on the network</li>
	 * </ul>
	 */
	public enum ErrorType {SYS_DECODE_ERR, NET_BUFFER_FULL};
	private volatile boolean mRunning = false;
	private final Thread decodingProcess = new Thread() {
		@Override
		public void run() {
			while(mRunning) {
				InputStream in = mCommManager.getInputStream();
				try {
					if (in.available() > 0) {
						decode((byte) in.read());
						in.close();
					} else {
						in.close();   // close stream ASAP instead of sleeping first
						Thread.sleep(mPollingInterval);
					}
				} catch (IOException e) {
					if (mListener != null) {
						mListener.onError(ErrorType.SYS_DECODE_ERR, 0);
					}
				} catch (InterruptedException e) {
					// set the flag to match actual state
					mRunning = false;
				}
			}
		}
	};
	
	private volatile int mPollingInterval = DEFAULT_POLLING_INT;
	protected ProtocolDecodeListener mListener;
	protected CommManager mCommManager;
	
	/**
	 * @param commManager Manager for the communication hardware to use.
	 */
	public ProtocolDecoder(CommManager commManager) {
		mCommManager = commManager;
	}
	
	/**
	 * Changed the hardware used to perform communication.
	 * 
	 * @return Flag indicating whether operation succeeded.
	 */
	public boolean changeCommManager(CommManager commManager) {
		if (!mRunning) {
			mCommManager = commManager;
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Start decoding process with bound communication manager.
	 * 
	 * @return Flag indicating whether decoder has started.
	 */
	public boolean start() {
		if (mCommManager != null) {
			mRunning = true;
			decodingProcess.start();
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Stop decoding process.
	 */
	public void stop() {
		mRunning = false;
	}
	
	/**
	 * @return Flag indicating whether decoder is running.
	 */
	public boolean isRunning() {
		return mRunning;
	}
	
	/**
	 * Set how often the input buffer is checked. 
	 * 
	 * @param millis Interval in milliseconds.
	 */
	public synchronized void setPollingInterval(int millis) {
		mPollingInterval = millis;
	}
	
	/**
	 * Implementation for how each byte is interpreted.
	 * 
	 * @param read Byte read from stream.
	 * @throws IOException if error encountered reading from stream.
	 */
	protected abstract void decode(byte read);
	
	/**
	 * @return The string name of this protocol decoder type.
	 */
	public abstract String getName();
	
	/**
	 * Attach custom hook to ProtocolDecoder events.
	 * 
	 * @param listener User defined event callbacks for this decoder.
	 */
	public void setOnProtocolDecodeListener(ProtocolDecodeListener listener) {
		mListener = listener;
	}
	
	public interface ProtocolDecodeListener {
		/**
		 * Callback when channel stream data has successfully decoded.
		 * 
		 * @param startChan Leftmost channel index of the values argument.
		 * @param value     The values of the channels decoded.
		 */
		public void onRecvChannels(int startChan, byte[] values);
		
		/**
		 * Callback when terminal command has successfully decoded. 
		 * 
		 * @param command Terminal command decoded.
		 * @param error   Handler for errors that occurred during transaction, null if no error.
		 */
		public void onRecvCommands(String command, TransactionError error);
		
		/**
		 * Callback when a bitmap has been successfully decoded.
		 * 
		 * @param image Image decoded.
		 * @param error Handler for errors that occurred during transaction, null if no error.
		 */
		public void onRecvBitmap(Bitmap image, TransactionError error);
		
		/**
		 * Callback when a request was made by the network.
		 * 
		 * @param request Action requested by the network.
		 * @param code    More information about the request.
		 */
		public void onRequest(RequestType request, int code);
		
		/**
		 * Callback when error is signaled (can be from application or network).
		 * 
		 * @param error Error type signaled by the network. 
		 * @param code  More information about the error.
		 */
		public void onError(ErrorType error, int code);
	}
}
