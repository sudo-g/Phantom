package com.tronacadmey.phantom.killalot;

import java.util.Arrays;

import com.tronacademy.phantom.messaging.IncomingTransaction;
import com.tronacademy.phantom.messaging.Packet;
import com.tronacademy.phantom.messaging.ProtocolAssembler.DataType;

public class IncomingKillalotCommandTransaction extends IncomingTransaction {
	
	// state trackers
	private final StringBuilder mCmdStr;

	/**
	 * @param packets    Number of packets in the transaction.
	 * @param characters Number of characters in the command.
	 * @param encoding How each pixel in encoded.
	 */
	public IncomingKillalotCommandTransaction(int packets, int characters) {
		super(DataType.COMMAND, packets);
		
		mCmdStr = new StringBuilder(characters);
	}
	
	@Override
	public boolean capturePacket(final Packet packet) {
		KillalotPacket kPacket = (KillalotPacket) packet;
		byte[] relevantFragment;
		if (kPacket.getHeader()[2] < KillalotPacket.PAYLOAD_LEN) {
			// index cannot be negative
			relevantFragment = Arrays.copyOfRange(kPacket.getPayload(), 0, (int) kPacket.getHeader()[2] & 0xFF);
		} else {
			relevantFragment = kPacket.getPayload();
		}
		
		mCmdStr.append(new String(relevantFragment));
		
		return super.capturePacket(packet);
	}
	
	@Override
	public Object getDecodedResult() {
		if (getPacketsToGo() < 1) {
			return mCmdStr.toString();
		} else {
			return null;
		}
	}
}
