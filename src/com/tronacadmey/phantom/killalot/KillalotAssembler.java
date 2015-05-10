package com.tronacadmey.phantom.killalot;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.tronacademy.phantom.messaging.OutgoingTransaction;
import com.tronacademy.phantom.messaging.ProtocolAssembler;

/**
 *
 * @author George Xian
 * @since 2015-04-30
 *
 */
public class KillalotAssembler implements ProtocolAssembler {

	// protocol control
	public static final byte HEARTBEAT_INDICATOR = 1;
	public static final byte CTLERR_INDICATOR = 2;
	public static final byte CTLREQ_INDICATOR = 3;
	
	// channel control and system commands 
	public static final byte CHANNEL_INDICATOR = 10;
	public static final byte COMMAND_INDICATOR = 11;
	
	// images
	public static final byte IMAGEHEAD_INDICATOR = 12;
	public static final byte IMAGE_INDICATOR = 13;
	
	// binary data
	public static final byte BINARYHEAD_INDICATOR = 20;
	public static final byte BINARY_INDICATOR = 21;
	
	
	// limits for each type of data type
	public static final int CHANNEL_PACK_LIMIT = 255;
	public static final int COMMAND_CHAR_LIMIT = 255;
	public static final int IMAGE_PACK_LIMIT = 16777216;
	public static final int BINARY_PACK_LIMIT = 16777216;
	
	@Override
	public String getName() {
		return "Killalot";
	}
	
	@Override
	public OutgoingTransaction serializeAsChannels(String name, byte[] channelStream) {
		final int noOfPackets = (int) Math.ceil((double) channelStream.length / 
								(double) KillalotPacket.PAYLOAD_LEN);
		
		if (noOfPackets > CHANNEL_PACK_LIMIT) {
			return null;
		}
		
		Queue<ByteArrayOutputStream> ret = new ArrayBlockingQueue<ByteArrayOutputStream>(noOfPackets);
		for (int i=0; i<noOfPackets; i++) {
			// for channel frames, header 4th byte is first channel index in packet
			byte[] header = {CHANNEL_INDICATOR, 
					         0, 
					         0, 
					         (byte) (i*KillalotPacket.PAYLOAD_LEN)};
			
			byte[] payload = Arrays.copyOfRange(channelStream, 
					         i*KillalotPacket.PAYLOAD_LEN, 
					         (i+1)*KillalotPacket.PAYLOAD_LEN);
			
			ret.add(new KillalotPacket(header, payload).serialize());
		}
		
		return new OutgoingTransaction(name, ret, 0);
	}

	@Override
	public OutgoingTransaction serializeAsCommands(String name, String command) {
		final int noOfPackets = (int) Math.ceil((double) command.length() / 
				                                (double) KillalotPacket.PAYLOAD_LEN);
		
		int bytesToSend = command.length();
		if (bytesToSend > COMMAND_CHAR_LIMIT) {
			return null;
		}
		
		Queue<ByteArrayOutputStream> ret = new ArrayBlockingQueue<ByteArrayOutputStream>(noOfPackets);
		for (int i=0; i<noOfPackets; i++) {
			bytesToSend -= KillalotPacket.PAYLOAD_LEN;
			// for command frames, 3rd byte is bytes remaining, 4th byte is total number of bytes 
			byte[] header = {COMMAND_INDICATOR, 
					         0, 
					         (byte) ((bytesToSend > 0) ? bytesToSend : 0), 
					         (byte) command.length()};
			
			byte[] payload = Arrays.copyOfRange(command.getBytes(),
								i*KillalotPacket.PAYLOAD_LEN,
								(i+1)*KillalotPacket.PAYLOAD_LEN);
			
			ret.add(new KillalotPacket(header, payload).serialize());
		}
		
		return new OutgoingTransaction(name, ret, 1);
	}

	@Override
	public OutgoingTransaction serializeAsBitmap(String name, Bitmap image) {
		final int rows = image.getHeight();
		final int cols = image.getWidth();
		final int pixels = rows*cols;
		final int noOfFrames = 
				(int) Math.ceil((double) pixels / (double) KillalotPacket.PAYLOAD_LEN) + 1;
		
		if (noOfFrames > IMAGE_PACK_LIMIT) {
			return null;
		}
		
		Queue<ByteArrayOutputStream> ret = new ArrayBlockingQueue<ByteArrayOutputStream>(noOfFrames);
		
		// TODO: Currently ignores Bitmap.Config and encodes as ARGB8888
		// images begin with a header frame
		byte[] metaHeader = {IMAGEHEAD_INDICATOR, 0, 0, 0};
		byte[] metaData = {(byte) 0,                      // reserved
						   (byte) 0,	                  // reserved
						   (byte) 0,                      // reserved
						   (byte) ((rows & 0xFF00) >> 8), // row size
						   (byte) (rows & 0x00FF),
						   (byte) ((cols & 0xFF00) >> 8), // col size
						   (byte) (cols & 0x00FF),
						   (byte) (0x02)                  // encoding (ARGB8888)
						  };
		ret.add(new KillalotPacket(metaHeader, metaData).serialize());
		
		// pixel index of 1st part of frame
		int r1 = 0;
		int c1 = 0;
		for (int i=0; i<noOfFrames; i++) {
			byte[] header = {IMAGE_INDICATOR, 
							 (byte) ((i & 0x00FF0000) >>> 16), 
							 (byte) ((i & 0x0000FF00) >>> 8),
							 (byte) (i & 0x000000FF)
							};
			
			// pixel index of 2nd part of frame
			int r2 = r1;
			int c2;
			if ((c1+1) < cols) {
				c2 = c1 + 1;
			} else {
				// 2nd pixel will be end of column
				r2++;
				c2 = 0;
			}
			
			byte[] data = {(byte) Color.alpha(image.getPixel(c1, r1)),
					       (byte) Color.red(image.getPixel(c1, r1)),
					       (byte) Color.green(image.getPixel(c1, r1)),
					       (byte) Color.blue(image.getPixel(c1, r1)),
					       (byte) Color.alpha(image.getPixel(c2, r2)),
					       (byte) Color.red(image.getPixel(c2, r2)),
					       (byte) Color.green(image.getPixel(c2, r2)),
					       (byte) Color.blue(image.getPixel(c2, r2))
						  };
			
			// advance pixel index for next frame
			if ((c1+2) < cols) {
				c1 += 2;
			} else {
				// next frame will do pixels in next row
				r1++;
				c1 = 0;
			}
			
			ret.add(new KillalotPacket(header, data).serialize());
		}
		
		return new OutgoingTransaction(name, ret, 2);
	}

	@Override
	public OutgoingTransaction serializeAsBinary(String name, ByteArrayOutputStream data) {
		
		final int noOfFrames = (int) Math.ceil(((double) data.size())/((double) KillalotPacket.PAYLOAD_LEN)) + 1;
		if (noOfFrames > BINARY_PACK_LIMIT) {
			return null;
		}
		
		Queue<ByteArrayOutputStream> ret = new ArrayBlockingQueue<ByteArrayOutputStream>(noOfFrames);
		
		//TODO: Binary data header
		
		for (int i=0; i<noOfFrames; i++) {
			byte[] header = {BINARY_INDICATOR, 
					         (byte) ((i & 0x00FF0000) >>> 16),
					         (byte) ((i & 0x0000FF00) >>> 8),
					         (byte) (i & 0x000000FF)
							};
			
			byte[] payload = new byte[KillalotPacket.PAYLOAD_LEN];
			data.write(payload, i, i+KillalotPacket.PAYLOAD_LEN);
			ret.add(new KillalotPacket(header, payload).serialize());
		}
		
		return new OutgoingTransaction(name, ret, 3);
	}
}
