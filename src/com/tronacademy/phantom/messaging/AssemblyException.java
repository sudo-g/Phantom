package com.tronacademy.phantom.messaging;

import com.tronacademy.phantom.messaging.ProtocolAssembler.DataType;

public class AssemblyException extends Exception {
	
	private static final long serialVersionUID = 2621738536384541968L;
	
	private static final String erMsg = "'%s' protocol cannot assemble this %s: %s";
	
	private static String dataTypeString(DataType type) {
		switch (type) {
		case CHANNEL: return "channel stream";
		case COMMAND: return "command string";
		case IMAGE: return "image";
		case BINARY: return "binary data";
		default: return "data";
		}
	}
	
	/**
	 * @param protocol The protocol that data was being assembled to.
	 * @param message  Additional message from the protocol assembler.
	 */
	public AssemblyException(String protocol, DataType type, String message) {
		super(String.format(erMsg, protocol, dataTypeString(type), message));
	}
}
