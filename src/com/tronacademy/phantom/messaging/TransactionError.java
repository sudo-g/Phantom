package com.tronacademy.phantom.messaging;

public class TransactionError {
	
	public final IncomingTransaction mTransaction;
	
	public TransactionError(IncomingTransaction transaction) {
		mTransaction = transaction;
	}
}
