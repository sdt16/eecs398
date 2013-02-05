package edu.cwru.eecs398.obdreader;

import java.io.IOException;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class BluetoothConnectionThread extends Thread {

	static final UUID UUID_RFCOMM_GENERIC = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");

	private final BluetoothServerSocket mmSocket;
	// private final BluetoothDevice mmDevice;
	private final BluetoothAdapter adapter;

	public BluetoothConnectionThread(BluetoothDevice device,
			BluetoothAdapter bluetoothAdapter) {
		// Use a temporary object that is later assigned to mmSocket,
		// because mmSocket is final
		BluetoothServerSocket tmp = null;
		// mmDevice = device;
		adapter = bluetoothAdapter;

		// Get a BluetoothSocket to connect with the given BluetoothDevice
		try {
			// MY_UUID is the app's UUID string, also used by the server code
			tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(
					"OBD RFCOMM", UUID_RFCOMM_GENERIC);
			// ParcelUuid[] uuids = device.getUuids();
			// tmp =
			// device.createRfcommSocketToServiceRecord(uuids[0].getUuid());
		} catch (IOException e) {
		}
		mmSocket = tmp;
	}

	public void run() {
		// Cancel discovery because it will slow down the connection
		// adapter.cancelDiscovery();
		BluetoothSocket socket = null;
		while (true) {
			/*
			 * try { // Connect the device through the socket. This will block
			 * // until it succeeds or throws an exception mmSocket.connect();
			 * Log.d("OBD", "Connected to BT"); try { Thread.sleep(5000); }
			 * catch (InterruptedException e) { // TODO Auto-generated catch
			 * block e.printStackTrace(); } } catch (IOException
			 * connectException) { // Unable to connect; close the socket and
			 * get out Log.d("OBD", "Error connecting with bt",
			 * connectException); try { mmSocket.close(); } catch (IOException
			 * closeException) { Log.d("OBD", "Error closing socket.",
			 * closeException); } return; }
			 */

			try {
				socket = mmSocket.accept();
			} catch (IOException e) {
				Log.e("OBD", "Error accepting", e);
				break;
			}
			// If a connection was accepted
			if (socket != null) {
				// Do work to manage the connection (in a separate thread)
				BluetoothSocketThread socketThread = new BluetoothSocketThread(
						socket);
				socketThread.start();
				try {
					mmSocket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			}

			// Do work to manage the connection (in a separate thread)

		}
	}

	/** Will cancel an in-progress connection, and close the socket */
	public void cancel() {
		try {
			mmSocket.close();
		} catch (IOException e) {
		}
	}
}
