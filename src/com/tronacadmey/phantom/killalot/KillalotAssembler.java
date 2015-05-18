package com.tronacadmey.phantom.killalot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.tronacademy.phantom.messaging.OutgoingTransaction;
import com.tronacademy.phantom.messaging.PBitmap;
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
	
	public static final byte K_IMG_ENC_RGB565 = 0x02;
	public static final byte K_IMG_ENC_ARGB8888 = 0x04;
	
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
		
		BlockingQueue<ByteArrayOutputStream> ret = new LinkedBlockingQueue<ByteArrayOutputStream>(noOfPackets);
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
		
		BlockingQueue<ByteArrayOutputStream> ret = new LinkedBlockingQueue<ByteArrayOutputStream>(noOfPackets);
		for (int i=0; i<noOfPackets; i++) {
			// for command frames, 3rd byte is bytes remaining, 4th byte is total number of bytes 
			byte[] header = {COMMAND_INDICATOR, 
					         0, 
					         (byte) ((bytesToSend > 0) ? bytesToSend : 0), 
					         (byte) command.length()};
			
			byte[] payload = Arrays.copyOfRange(command.getBytes(),
								i*KillalotPacket.PAYLOAD_LEN,
								(i+1)*KillalotPacket.PAYLOAD_LEN);
			
			try {
				ret.put(new KillalotPacket(header, payload).serialize());
			} catch(InterruptedException e) {
				// TODO
				e.printStackTrace();
			}
			
			if (bytesToSend >= KillalotPacket.PAYLOAD_LEN) {
				bytesToSend -= KillalotPacket.PAYLOAD_LEN;
			} else {
				bytesToSend = 0;
			}
		}
		
		return new OutgoingTransaction(name, ret, 1);
	}

	@Override
	public OutgoingTransaction serializeAsBitmap(String name, PBitmap bmp) {
		final int height = bmp.getHeight();
		final int width = bmp.getWidth();
		
		byte encoding = 0x00;
		switch (bmp.getEncoding()) {
		case RGB565:
			encoding = K_IMG_ENC_RGB565;
			break;
		case ARGB8888:
			encoding = K_IMG_ENC_ARGB8888;
			break;
		}
				
		final int noOfFrames = 
				(int) Math.ceil((double) bmp.getSizeInBytes() / (double) KillalotPacket.PAYLOAD_LEN) + 1;
		
		if (noOfFrames > IMAGE_PACK_LIMIT) {
			return null;
		}
		
		BlockingQueue<ByteArrayOutputStream> ret = new LinkedBlockingQueue<ByteArrayOutputStream>(noOfFrames);
		
		// images begin with a header frame
		byte[] metaHeader = {IMAGEHEAD_INDICATOR, 0, 0, 0};
		byte[] metaData = {(byte) 0,                        // reserved
						   (byte) 0,	                    // reserved
						   (byte) 0,                        // reserved
						   (byte) ((width & 0xFF00) >>> 8), // 16 bit for image width
						   (byte) (width & 0x00FF),
						   (byte) ((height & 0xFF00) >>> 8),// 16 bit for image height
						   (byte) (height & 0x00FF),
						   (byte) encoding
						  };
		ret.add(new KillalotPacket(metaHeader, metaData).serialize());
		
		// image data frames
		final InputStream imgData = bmp.serialize();
		for (int i=0; i<noOfFrames-1; i++) {
			byte[] packet = new byte[KillalotPacket.getDecodedSize()];
			
			// header data
			packet[0] = IMAGE_INDICATOR;
			packet[1] = (byte) ((i & 0x00FF0000) >>> 16);
			packet[2] = (byte) ((i & 0x0000FF00) >>> 8);
			packet[3] = (byte) (i & 0x000000FF);
			
			try {
				// payload data
				for (int j=KillalotPacket.HEADER_LEN; 
						(j<KillalotPacket.PAYLOAD_LEN + KillalotPacket.HEADER_LEN) && (imgData.available() > 0); 
						j++) {
					
					packet[j] = ((byte) imgData.read());
				}
				ret.put(new KillalotPacket(packet).serialize());
			} catch (IOException e) {
				// another thread closes the stream prematurely
				return null;
			} catch (InterruptedException e) {
				// TODO: Log cancelled download
				e.printStackTrace();
			}
		}
		
		return new OutgoingTransaction(name, ret, 2);
	}

	@Override
	public OutgoingTransaction serializeAsBinary(String name, ByteArrayOutputStream data) {
		
		final int noOfFrames = (int) Math.ceil(((double) data.size())/((double) KillalotPacket.PAYLOAD_LEN)) + 1;
		if (noOfFrames > BINARY_PACK_LIMIT) {
			return null;
		}
		
		BlockingQueue<ByteArrayOutputStream> ret = new LinkedBlockingQueue<ByteArrayOutputStream>(noOfFrames);
		
		//TODO: Binary data header
		
		for (int i=0; i<noOfFrames; i++) {
			byte[] header = {BINARY_INDICATOR, 
					         (byte) ((i & 0x00FF0000) >>> 16),
					         (byte) ((i & 0x0000FF00) >>> 8),
					         (byte) (i & 0x000000FF)
							};
			
			byte[] payload = new byte[KillalotPacket.PAYLOAD_LEN];
			data.write(payload, i, i+KillalotPacket.PAYLOAD_LEN);
			try {
				ret.put(new KillalotPacket(header, payload).serialize());
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return new OutgoingTransaction(name, ret, 3);
	}
}
