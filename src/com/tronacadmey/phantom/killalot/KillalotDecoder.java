package com.tronacadmey.phantom.killalot;

import java.io.ByteArrayOutputStream;

import com.tronacademy.phantom.messaging.IncomingTransaction;
import com.tronacademy.phantom.messaging.PBitmap;
import com.tronacademy.phantom.messaging.PBitmap.Encoding;
import com.tronacademy.phantom.messaging.ProtocolDecoder;

public class KillalotDecoder implements ProtocolDecoder {
	
	private ProtocolDecodeListener mListener;
	
	// state trackers
	private boolean inFrame = false;
	private boolean escaping = false;
	private ByteArrayOutputStream tempPacket = null; 
	
	// uncompleted transactions (channels are always completed in one frame)
	private IncomingTransaction commandTransaction = null;
	private IncomingTransaction imageTransaction = null;
	private IncomingTransaction binaryTransaction = null;
	
	@Override
	public String getName() {
		return "Killalot";
	}
	
	@Override
	public void setOnProtocolDecodeListener(ProtocolDecodeListener listener) {
		mListener = listener;
	}
	
	@Override 
	public void decodeByte(byte read) {
		if (inFrame) {
			inFrameAction(read);
		} else {
			if (read == KillalotPacket.SLIP_END) {
				// start a new packet
				inFrame = true;
				tempPacket = new ByteArrayOutputStream(KillalotPacket.getDecodedSize());
			}
		}
	}
	
	private void inFrameAction(byte read) {
		if (escaping) {
			// previous character was ESC, only two chars are accepted in this state
			if (read == KillalotPacket.SLIP_ESC_END) {
				tempPacket.write(KillalotPacket.SLIP_END);
			} else if (read == KillalotPacket.SLIP_ESC_ESC) {
				tempPacket.write(KillalotPacket.SLIP_ESC);
			}
			escaping = false;
		} else {
			if (read == KillalotPacket.SLIP_END) {
				inFrame = false;
				if (tempPacket.size() >= KillalotPacket.getDecodedSize()) {
					// full packet has been written
					KillalotPacket recvPacket = new KillalotPacket(tempPacket.toByteArray());
					delegatePacketByType(recvPacket);
					
					tempPacket = null;    // flush temp packet
				}
			} else if (read == KillalotPacket.SLIP_ESC) {
				escaping = true;
			} else {
				tempPacket.write(read);
			}
		}
	}
	
	private void delegatePacketByType(final KillalotPacket recvPacket) {
		final byte packetType = recvPacket.getHeader()[0];
		switch(packetType) {
		case KillalotAssembler.CHANNEL_INDICATOR:
			if (mListener != null) {
				mListener.onRecvChannels(recvPacket.getHeader()[3], recvPacket.getPayload());
			}
			break;
		case KillalotAssembler.COMMAND_INDICATOR:
			handleCommandPacket(recvPacket);
			break;
		case KillalotAssembler.IMAGEHEAD_INDICATOR:
			startNewImageTransaction(recvPacket);
			break;
		case KillalotAssembler.IMAGE_INDICATOR:
			// capture only if image transaction is in progress, otherwise ignore
			if (imageTransaction != null) {
				if (imageTransaction.capturePacket(recvPacket)) {
					if (mListener != null) {
						// TODO: Analyze for errors
						mListener.onRecvBitmap((PBitmap) imageTransaction.getDecodedResult(), null);
					}
				}
			}
			break;
		case KillalotAssembler.BINARYHEAD_INDICATOR:
			// TODO
			break;
		case KillalotAssembler.BINARY_INDICATOR:
			// TODO
			break;
		}
	}
	
	private void handleCommandPacket(final KillalotPacket recvPacket) {
		if (commandTransaction != null) {
			// command transaction already in progress
			if (commandTransaction.capturePacket(recvPacket)) {
				// transaction is complete
				if (mListener != null) {
					// TODO: Analyze for errors
					mListener.onRecvCommands((String) commandTransaction.getDecodedResult(), null);
				}
				
				// reset command transaction state
				commandTransaction = null;
			}
		} else {
			// start a new command transaction
			final byte chars = recvPacket.getHeader()[3];
			// number of characters cannot be negative
			final int ichars = chars & 0xFF; 
			final int noOfPackets = (int) Math.ceil((double) ichars / (double) KillalotPacket.PAYLOAD_LEN);
			commandTransaction = new IncomingKillalotCommandTransaction(noOfPackets, ichars);
			if (commandTransaction.capturePacket(recvPacket)) {
				// transaction is complete
				if (mListener != null) {
					// TODO: Analyze for errors
					mListener.onRecvCommands((String) commandTransaction.getDecodedResult(), null);
					
					commandTransaction = null;
				}
			}
		}
	}
	
	private void startNewImageTransaction(KillalotPacket recvPacket) {
		if (imageTransaction == null) {
			final int width = ((recvPacket.getPayload()[3] & 0xFF) << 8) | (recvPacket.getPayload()[4] & 0xFF);
			final int height = ((recvPacket.getPayload()[5] & 0xFF) << 8) | (recvPacket.getPayload()[6] & 0xFF);
			final byte bEnc = recvPacket.getPayload()[7];
			
			Encoding encoding = null;
			int noOfFrames = 0;
			switch (bEnc) {
			case KillalotAssembler.K_IMG_ENC_RGB565:
				// one pixel takes up 16 bits, which is the size of a short
				noOfFrames = (int) Math.ceil(
						(double) (width * height * Short.SIZE) /
						(double) (Byte.SIZE * KillalotPacket.PAYLOAD_LEN) );
				encoding = Encoding.RGB565;
				break;
			case KillalotAssembler.K_IMG_ENC_ARGB8888:
				// one pixel takes up 32 bits, which is the size of an integer
				noOfFrames = (int) Math.ceil(
						(double) (width * height * Integer.SIZE) / 
						(double) (Byte.SIZE * KillalotPacket.PAYLOAD_LEN) );
				encoding = Encoding.ARGB8888;
				break;
			}
			imageTransaction = new IncomingKillalotImageTransaction(noOfFrames, width, height, encoding);
		} else {
			// TODO: raise error for creating new image transaction before completing previous
		}
	}
}
