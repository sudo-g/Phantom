package com.tronacadmey.phantom.killalot;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.tronacademy.phantom.messaging.AssemblyException;
import com.tronacademy.phantom.messaging.ProtocolAssembler;

public class KillalotAssembler implements ProtocolAssembler {

	public static final byte CHANNEL_INDICATOR = 1;
	public static final byte COMMAND_INDICATOR = 2;
	public static final byte IMAGEHEAD_INDICATOR = 4;
	public static final byte IMAGE_INDICATOR = 5;
	public static final byte BINARY_INDICATOR = 8;
	
	public static final int CHANNEL_PACK_LIMIT = 255;
	public static final int COMMAND_CHAR_LIMIT = 255;
	public static final int IMAGE_PACK_LIMIT = 16777216;
	public static final int BINARY_PACK_LIMIT = 16777216;
	
	@Override
	public List<ByteArrayOutputStream> serializeAsChannels(byte[] channelStream) 
			throws AssemblyException {
		final int noOfPackets = (int) Math.ceil((double) channelStream.length / 
								(double) KillalotDatagram.PAYLOAD_LEN);
		
		if (noOfPackets > CHANNEL_PACK_LIMIT) {
			String erMsg = String.format("Exceeded packet limit of %d", CHANNEL_PACK_LIMIT);
			throw new AssemblyException("Killalot", DataType.CHANNEL, erMsg);
		}
		
		List<ByteArrayOutputStream> ret = new ArrayList<ByteArrayOutputStream>(noOfPackets);
		for (int i=0; i<noOfPackets; i++) {
			// for channel frames, header 4th byte is first channel index in packet
			byte[] header = {CHANNEL_INDICATOR, 
								0, 
								0, 
								(byte) (i*KillalotDatagram.PAYLOAD_LEN)};
			
			byte[] payload = Arrays.copyOfRange(channelStream, 
								i*KillalotDatagram.PAYLOAD_LEN, 
								(i+1)*KillalotDatagram.PAYLOAD_LEN);
			
			ret.add(new KillalotDatagram(header, payload).byteStreamForm());
		}
		
		return ret;
	}

	@Override
	public List<ByteArrayOutputStream> serializeAsCommands(String command) 
			throws AssemblyException {
		final int noOfPackets = (int) Math.ceil((double) command.length() / 
												(double) KillalotDatagram.PAYLOAD_LEN);
		
		int bytesToSend = command.length();
		if (bytesToSend > COMMAND_CHAR_LIMIT) {
			String erMsg = String.format("Exceeded character limit of %d", COMMAND_CHAR_LIMIT);
			throw new AssemblyException("Killalot", DataType.COMMAND, erMsg);
		}
		
		List<ByteArrayOutputStream> ret = new ArrayList<ByteArrayOutputStream>(noOfPackets);
		for (int i=0; i<noOfPackets; i++) {
			bytesToSend -= KillalotDatagram.PAYLOAD_LEN;
			// for command frames, 3rd byte is bytes remaining, 4th byte is total number of bytes 
			byte[] header = {COMMAND_INDICATOR, 
								0, 
								(byte) ((bytesToSend > 0) ? bytesToSend : 0), 
								(byte) command.length()};
			
			byte[] payload = Arrays.copyOfRange(command.getBytes(),
								i*KillalotDatagram.PAYLOAD_LEN,
								(i+1)*KillalotDatagram.PAYLOAD_LEN);
			
			ret.add(new KillalotDatagram(header, payload).byteStreamForm());
		}
		
		return ret;
	}

	@Override
	public List<ByteArrayOutputStream> serializeAsBitmapRGBA8(Bitmap image) 
			throws AssemblyException {
		final int rows = image.getHeight();
		final int cols = image.getWidth();
		final int pixels = rows*cols;
		final int noOfFrames = 
				(int) Math.ceil((double) pixels / (double) KillalotDatagram.PAYLOAD_LEN);
		
		if (noOfFrames > IMAGE_PACK_LIMIT) {
			String erMsg = String.format("Exceeded packet limit of %d", IMAGE_PACK_LIMIT);
			throw new AssemblyException("Killalot", DataType.IMAGE, erMsg);
		}
		
		List<ByteArrayOutputStream> ret = new ArrayList<ByteArrayOutputStream>(noOfFrames);
		
		// image meta-data
		byte[] imgMeta = {(byte) 0,                 // reserved
						  (byte) 0,	                // reserved
						  (byte) 0,                 // reserved
						  (byte) (rows & 0x00FF),   // row size
						  (byte) (rows & 0xFF00),
						  (byte) (cols & 0x00FF),   // col size
						  (byte) (cols & 0xFF00),
						  (byte) (0x0F)             // encoding (RGBA | 4 channels | 8 bits per color)
						 };
		byte[] metaHeader = {IMAGEHEAD_INDICATOR, 0, 0, 0};
		ret.add(new KillalotDatagram(metaHeader, imgMeta).byteStreamForm());
		
		// pixel index of 1st part of frame
		int r1 = 0;
		int c1 = 0;
		
		for (int i=0; i<noOfFrames; i++) {
			byte[] header = {IMAGE_INDICATOR, 
							 (byte) (i & 0x00FF0000), 
							 (byte) (i & 0x0000FF00),
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
			
			byte[] data = {(byte) (Color.alpha(image.getPixel(c1, r1)) & 0xFF000000),
						   (byte) (Color.red(image.getPixel(c1, r1)) & 0x00FF0000),
						   (byte) (Color.green(image.getPixel(c1, r1)) & 0x0000FF00),
						   (byte) (Color.blue(image.getPixel(c1, r1)) & 0x000000FF),
						   (byte) (Color.alpha(image.getPixel(c2, r2)) & 0xFF000000),
						   (byte) (Color.red(image.getPixel(c2, r2)) & 0x00FF0000),
						   (byte) (Color.green(image.getPixel(c2, r2)) & 0x0000FF00),
						   (byte) (Color.blue(image.getPixel(c2, r2)) & 0x000000FF)
						  };
			
			// advance pixel index for next frame
			if ((c1+2) < cols) {
				c1 += 2;
			} else {
				// next frame will do pixels in next row
				r1++;
				c1 = 0;
			}
			
			ret.add(new KillalotDatagram(header, data).byteStreamForm());
		}
		
		return ret;
	}

	@Override
	public List<ByteArrayOutputStream> serializeAsBinary(ByteArrayOutputStream data) 
			throws AssemblyException {
		
		final int noOfFrames = (int) Math.ceil(((double) data.size())/((double) KillalotDatagram.PAYLOAD_LEN));
		if (noOfFrames > BINARY_PACK_LIMIT) {
			String erMsg = String.format("Exceeded packet limit if %d", BINARY_PACK_LIMIT);
			throw new AssemblyException("Killalot", DataType.BINARY, erMsg);
		}
		
		List<ByteArrayOutputStream> ret = new ArrayList<ByteArrayOutputStream>(noOfFrames);
		
		for (int i=0; i<noOfFrames; i++) {
			byte[] header = {BINARY_INDICATOR, 
							 (byte) (i & 0x00FF0000),
							 (byte) (i & 0x0000FF00),
							 (byte) (i & 0x000000FF)
							};
			
			byte[] payload = new byte[KillalotDatagram.PAYLOAD_LEN];
			data.write(payload, i, i+KillalotDatagram.PAYLOAD_LEN);
			ret.add(new KillalotDatagram(header, payload).byteStreamForm());
		}
		
		return ret;
	}
}
