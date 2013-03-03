package edu.cwru.eecs398.obdreader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class BluetoothSocketThread extends Thread {
	
	private final BluetoothSocket socket;
	
	private final InputStream inputStream;
	private final OutputStream outputStream;
	
	public BluetoothSocketThread(BluetoothSocket btSocket) {
		this.socket = btSocket;
		InputStream tempIS = null;
		OutputStream tempOS = null;
		try {
			tempIS = socket.getInputStream();
			tempOS = socket.getOutputStream();
		} catch (IOException e) {
			Log.e("OBD", "Error getting bluetooth stream", e);
		}
		
		this.inputStream = tempIS;
		this.outputStream = tempOS;
		
	}
	
	public class WriteThread extends Thread {
		@Override
		public void run() {
			String command = "01 05\r";
			try {
				while (true) {
					outputStream.write(command.getBytes("US-ASCII"));
					Thread.sleep(1000);
				}
			} catch (IOException e) {
				Log.e("OBD", "Error writing to output stream", e);
			} catch (InterruptedException e) {
				Log.e("OBD", "Error writing to output stream", e);
			}
		}
	}
	
	@Override
	public void run() {
		int read = Integer.MAX_VALUE;
		WriteThread writeThread = new WriteThread();
		writeThread.start();
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			while (true) {
				while (read != '\r') {
					read = inputStream.read();
					os.write(read);
					//Log.d("OBD", "Value read from socket is: " + read);
					//break;
				}
				String stringRead = new String(os.toByteArray(), "US-ASCII");
				Log.d("OBD", "Value read from socket is: " + stringRead);
				os.reset();
				read = Integer.MAX_VALUE;
			}
			
		} catch (IOException e) {
			Log.e("OBD", "Error reading from input stream", e);
		}
		
		
	}

}
