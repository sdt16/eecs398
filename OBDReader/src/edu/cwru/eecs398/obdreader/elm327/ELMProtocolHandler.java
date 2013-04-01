package edu.cwru.eecs398.obdreader.elm327;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import android.util.Log;

/**
 * Handles the ELM327 protocol.
 * 
 * Heavily influenced by:
 * http://elmhandler.svn.sourceforge.net/viewvc/elmhandler
 * /ELMHandler/src/net/sourceforge
 * /ELMHandler/obd/elm/ELMProtocolHandler.java?revision=2&view=markup
 * 
 * @author schuyler
 * 
 */
public class ELMProtocolHandler implements Runnable {
	private static final String TAG = "OBDProtocolHandler";

	protected static final String PROMPT = ">";
	protected static final String OK = "OK";
	private static final String BUS_INIT = "BUS INIT: ...";
	private static final String[] ERROR_MSGS = { "BUFFER FULL", "BUS BUSY",
		"BUS ERROR", "CAN ERROR", "FB ERROR", "DATA ERROR", "<DATA ERROR",
		"NO DATA", "<RX ERROR", "UNABLE TO CONNECT", "?" };

	private final InputStream in;
	private final OutputStream out;
	private String rxData = "";
	protected String LT = "\r\n";

	private static final int TIMEOUT_DELAY = 3000;

	private class UpdateRxData extends Thread {

		@Override
		public void run() {
			final byte[] readBuffer = new byte[256];

			try {
				while (true) {
					while (in.available() > 0) {
						final int numBytes = in.read(readBuffer);
						rxData = rxData + new String(readBuffer, 0, numBytes);
					}
				}
			} catch (final IOException e) {
				Log.e(TAG, "Error reading from InputStream", e);
			}
		}
	};

	public ELMProtocolHandler(final InputStream in, final OutputStream out) {
		this.in = in;
		this.out = out;
	}

	private byte[] waitForBytes(final int bytes) {
		final byte[] retVal;
		final String rxTemp;
		while ((rxData.length() < bytes)) {
			try {
				Thread.sleep(200);
			} catch (final InterruptedException e) {
			}
		}
		if (rxData.length() < bytes) {
			rxTemp = rxData.substring(1, bytes);
			retVal = rxTemp.getBytes();
			rxData = rxData.substring(bytes, rxData.length() - 1);
			return retVal;
		} else {
			return null;
		}
	}

	private boolean waitForPrompt() {
		Log.d(TAG, "Waiting for prompt");
		if (waitForString(LT + PROMPT) != null) {
			return true;
		} else {
			return false;
		}
	}

	protected boolean waitForOk() {
		Log.d(TAG, "Waiting for OK");
		if (waitForString(OK + LT /* + LT */+ PROMPT) != null) {
			return true;
		} else {
			return false;
		}
	}

	protected String waitForString(final String str) {
		final String preString;
		final Date firstTime = new Date();
		Date currentTime = new Date();
		while (true) { //currentTime.getTime() < (firstTime.getTime() + TIMEOUT_DELAY)) {
			if (rxData.contains(BUS_INIT + LT)) {
				rxData = rxData.substring(rxData.indexOf(BUS_INIT + LT)
						+ BUS_INIT.length() + LT.length());
			}
			for (int i = 0; i < ERROR_MSGS.length; i++) {
				if (rxData.contains(ERROR_MSGS[i] + LT /* + LT */+ PROMPT)) {
					rxData = rxData.substring(rxData.indexOf(ERROR_MSGS[i])
							+ ERROR_MSGS[i].length()
							+ ((/* 2 * */LT.length()) + PROMPT.length()));
					return null;
				}
			}
			if (rxData.contains(str)) {
				preString = rxData.substring(0, rxData.indexOf(str));
				rxData = rxData.substring(rxData.indexOf(str) + str.length());
				return preString;
			}
			try {
				Thread.sleep(200);
			} catch (final InterruptedException e) {
			}
			currentTime = new Date();
		}
		//return null;
	}

	// General AT Commands

	public synchronized boolean resetToFactoryConfig() {
		Log.d(TAG, "Reseting ELM to factory config");
		sendAt("d");
		return waitForOk();
	}

	public synchronized boolean setCommandEcho(final boolean echo) {
		if (echo) {
			Log.d(TAG, "Sending echo on");
			sendAt("e1");
		} else {
			Log.d(TAG, "Sending echo off");
			sendAt("e0");
		}

		return waitForOk();
	}

	public synchronized String chipId() {
		Log.d(TAG, "Getting Chip ID");
		sendAt("i");
		return waitForString(LT + PROMPT);
	}

	public synchronized boolean setLinefeed(final boolean lf) {
		if (lf) {
			Log.d(TAG, "Sending enable line feed");
			LT = "\r\n";
			sendAt("l1");
		} else {
			Log.d(TAG, "Sending disable line feed");
			LT = "\r";
			sendAt("l0");
		}
		return waitForOk();
	}

	public synchronized boolean reset() {
		Log.d(TAG, "Sending reset");
		sendAt("z");
		return waitForPrompt();
	}

	public synchronized boolean warmStart() {
		Log.d(TAG, "Sending warm start");
		sendAt("ws");
		return waitForPrompt();
	}

	public boolean sendAt(final String msg) {
		return send("at" + msg);
	}

	public boolean send(final String msg) {
		rxData = "";
		if (out == null) {
			return false;
		}

		Log.d(TAG, "tx: " + msg);
		try {
			for (int i = 0; i < msg.length(); i++) {
				final char ch = msg.charAt(i);
				out.write(ch);
			}
			out.write(0x0d);
		} catch (final IOException e) {
			Log.w(TAG, "IOException while sending msg to ELM", e);
			return false;
		}
		return true;
	}

	protected synchronized String sendOBDC(final byte mode, final byte pid) {
		String obdcReq = "";
		obdcReq = pad(mode) + pad(pid);
		send(obdcReq);
		return waitForString(LT + PROMPT);
	}

	protected synchronized String sendOBDC(final byte mode) {
		send(pad(mode));
		return waitForString(LT + PROMPT);
	}

	protected String hexdump(final String str) {
		char c;
		int numVal;
		String ret = "";
		for (int i = 0; i < str.length(); i++) {
			c = str.charAt(i);
			numVal = c;
			ret = ret + " " + Integer.toHexString(numVal).toUpperCase();
		}
		return ret;
	}

	public void interrupt() {
		try {
			out.write('*');
		} catch (final IOException e) {
			Log.w(TAG, "IOException while sending interrut char", e);
		}
	}

	protected String pad(final int val) {
		String retVal = "";
		if ((val < 0) || (val > 254)) {
			return "RG";
		} else {
			if (val < 16) {
				retVal = retVal + "0";
			}
			retVal = retVal + Integer.toHexString(val).toUpperCase();
		}
		return retVal;
	}

	@Override
	public void run() {
		final UpdateRxData update = new UpdateRxData();
		update.start();
	}

}
