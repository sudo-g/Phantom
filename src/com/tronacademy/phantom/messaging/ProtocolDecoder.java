package com.tronacademy.phantom.messaging;

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
public interface ProtocolDecoder {
	
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
	
	/**
	 * @return The string name of this protocol decoder type.
	 */
	public String getName();
	
	/**
	 * Implementation for how each byte is interpreted.
	 * 
	 * @param read Byte read from stream.
	 */
	public void decodeByte(byte read);
	
	/**
	 * Attach custom hook to ProtocolDecoder events.
	 * 
	 * @param listener User defined event callbacks for this decoder.
	 */
	public void setOnProtocolDecodeListener(ProtocolDecodeListener listener);
	
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
