package com.tronacademy.phantom.comm;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

public class BtCommManager extends CommManager {
	
	private static final String TAG = "BtCommManager";
	
	// get a unique UUID for this device
	private static final UUID DEV_UUID = UUID.fromString(Settings.Secure.ANDROID_ID);
	private final int STREAM_POLL_INT = 10;
	
	private BluetoothDevice mBluetoothDevice;         // set by setDevice()
	private BluetoothSocket mBluetoothSocket = null;  // null means not connected
	
	private Thread bgReadThread = new Thread() {
		private InputStream in;
		
		public void run() {
			CommManagerError error = null;
			
			try {
				in = mBluetoothSocket.getInputStream();
			} catch (IOException e) {
				// failed to obtain input stream, terminate thread immediately
				initializeErrorLogIfNull(error);
				// TODO: add info to the error log
				disconnect(error);
				return;
			}
			
			// terminate thread if disconnect() or stopReadProcess() is called
			while(mConnected && !mBlocking) {
				try {
					// wait until message arrives or process is terminated externally
					while (in.available() < 1 && mConnected && !mBlocking) {
						Thread.sleep(STREAM_POLL_INT);
					}
					
					if (mConnected && !mBlocking) {
						// polling was not terminated externally
						byte[] recv = new byte[in.available()]; 
						in.read(recv);
						if (mListener != null) {
							mListener.onMessageReceived(recv);
						}
					}
					
				} catch (IOException ioEx) {
					initializeErrorLogIfNull(error);
					// TODO: add info to the error log (availability check error)
				} catch (InterruptedException intEx) {
					initializeErrorLogIfNull(error);
					// TODO: add info to the error log (interrupted during sleep)
					
					// reading thread has stopped, set state tracker to match
					mBlocking = true;
				}
			}
			
			// clean up before terminating thread
			try {
				in.close();
			} catch (IOException e) {
				initializeErrorLogIfNull(error);
				// TODO: add info to the error log
			}
			in = null;
		}
		
		private void initializeErrorLogIfNull(CommManagerError error) {
			if (error == null) {
				error = new CommManagerError();
			}
		}
	};
	
	/**
	 * @throws UnsupportedOperationException if no bluetooth adapter on device.
	 */
	public BtCommManager() throws UnsupportedOperationException {
		final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
		if (btAdapter != null) {
			if (!btAdapter.isEnabled() && mListener != null) {
				mListener.onIntentRequest(new Intent (BluetoothAdapter.ACTION_REQUEST_ENABLE));
			}
			
		} else {
			throw new UnsupportedOperationException(
					"Bluetooth adapter returned null, device may not have bluetooth support");
		}
	}

	@Override
	public synchronized boolean connect() throws IOException {
		if (mBluetoothDevice != null) {
			
			// declare actions required for performing a connection
			final Thread acceptThread = new Thread() {
				private final BluetoothSocket mBtSock = 
						mBluetoothDevice.createInsecureRfcommSocketToServiceRecord(DEV_UUID);
				
				public void run() {
					final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
					
					// discovery is a heavyweight procedure, cancel to preserve adapter performance
					btAdapter.cancelDiscovery();
					
					// connect device through socket, process is blocking
					try {
						mBtSock.connect();
						if (mBtSock.isConnected()) {
							mBluetoothSocket = mBtSock;
							mConnected = true;
							if (mListener != null) {
								mListener.onConnectSuccess();
							}
						} else {
							if (mListener != null) {
								mListener.onConnectFailure();
							}
						}
						
					} catch (IOException conErr) {
						CommManagerError error = new CommManagerError();
						try {
							// TODO: Add some info to the error
							mBtSock.close();
							
							if (mListener != null) {
								mListener.onError(error);
							}
						} catch (IOException clErr) {
							// TODO: may remove this once CommManagerError is defined
							Log.e(TAG, "Error closing BluetoothSocket after failed connection attempt");
							
							// TODO: Add some more info to the error
							if (mListener != null) {
								mListener.onError(error);
							}
						}
					}
				}
			};
			
			acceptThread.start();    // initialize connection operation
			return true;
		} else {
			// no device was set
			return false;
		}
	}

	@Override
	public synchronized void disconnect() {
		disconnect(null);
	}
	
	@Override
	public synchronized void disconnect(CommManagerError error) {
		if (mBluetoothSocket != null) {
			try {
				mBluetoothSocket.close();
				
				// reset state trackers
				mConnected = false;
				mBlocking = true;
				
				if (error != null && mListener != null) {
					// error pass through
					mListener.onError(error);
				}
			} catch (IOException e) {
				final CommManagerError err = (error == null) ? new CommManagerError() : error;
				// TODO: add info to error
				if (mListener != null) {
					mListener.onError(err);
				}
			}
		}
	}

	@Override
	public byte[] read(int timeout) throws InterruptedException {
		if (mConnected) {
			InputStream in;
			try {
				in = mBluetoothSocket.getInputStream();
				
				try {
					while (in.available() < 1 && timeout > 0) {
						Thread.sleep(STREAM_POLL_INT);
						timeout--;
					}
				} catch (IOException e) {
					final CommManagerError error = new CommManagerError();
					// TODO: add info to the error (availability check error)
					if (mListener != null) {
						mListener.onError(error);
					}
					return null;
				}
				
				if (timeout > 0) {
					// message successfully received
					byte[] recv = new byte[in.available()];
					in.read(recv);
					return recv;
				} else {
					// blocking timed out
					return null;
				}
				
			} catch (IOException e) {
				final CommManagerError error = new CommManagerError();
				// TODO: add info to the error (failed to obtain input stream)
				if (mListener != null) {
					mListener.onError(error);
				}
				return null;
			}
		} else {
			throw new UnsupportedOperationException("Could not read because the manager is not connected");
		}
	}
	
	@Override
	public boolean startReadProcess() {
		if (mConnected) {
			mBlocking = false;
			bgReadThread.start();
			return true;
		} else {
			throw new UnsupportedOperationException("Could start read process because the manager is not connected");
		}
	}

	@Override
	public void stopReadProcess() {
		mBlocking = true;
	}
	
	/**
	 * Set bluetooth device to connect to.
	 * 
	 * @param device Bluetooth device to connect to.
	 * @return True if set, false if manager already connected.
	 */
	public synchronized boolean setDevice(BluetoothDevice device) {
		if (mBluetoothSocket == null) {
			mBluetoothDevice = device;
			return true;
		} else {
			return false;
		}
	}
	
	public void finalize() {
		final CommManagerError error = new CommManagerError();
		// TODO: add info to the error
		disconnect(error);
	}
}
