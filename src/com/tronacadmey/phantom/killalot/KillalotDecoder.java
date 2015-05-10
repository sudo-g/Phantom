package com.tronacadmey.phantom.killalot;

import java.io.ByteArrayOutputStream;

import android.graphics.Bitmap;

import com.tronacademy.phantom.messaging.IncomingTransaction;
import com.tronacademy.phantom.messaging.ProtocolDecoder;
import com.tronacademy.phantom.comm.CommManager;

public class KillalotDecoder extends ProtocolDecoder {
	
	// state trackers
	private boolean inFrame = false;
	private boolean escaping = false;
	private ByteArrayOutputStream tempPacket = null; 
	
	// uncompleted transactions (channels are always completed in one frame)
	private IncomingTransaction commandTransaction = null;
	private IncomingTransaction imageTransaction = null;
	private IncomingTransaction binaryTransaction = null;
	
	
	public KillalotDecoder(CommManager commManager) {
		super(commManager);
	}
	
	@Override
	public String getName() {
		return "Killalot";
	}
	
	@Override 
	protected void decode(byte read) {
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
					
					tempPacket = null;    // empty temp packet
				}
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
						mListener.onRecvBitmap((Bitmap) imageTransaction.getDecodedResult(), null);
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
			final int chars = recvPacket.getHeader()[3];
			final int noOfPackets = (int) Math.ceil((double) chars / (double) KillalotPacket.PAYLOAD_LEN);
			commandTransaction = new IncomingKillalotCommandTransaction(noOfPackets, chars);
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
			final int rows = (recvPacket.getPayload()[3] << 8) | recvPacket.getPayload()[4];
			final int cols = (recvPacket.getPayload()[5] << 8) | recvPacket.getPayload()[6];
			final byte encoding = recvPacket.getPayload()[7];
			// TODO: Currently ignores encoding, decodes as ARGB8888 
			final int noOfFrames = (int) Math.ceil((double) rows * cols / 2); 
			
			imageTransaction = new IncomingKillalotImageTransaction(noOfFrames, rows, cols, encoding);
		} else {
			// TODO: raise error for creating new image transaction before completing previous
		}
	}
}
