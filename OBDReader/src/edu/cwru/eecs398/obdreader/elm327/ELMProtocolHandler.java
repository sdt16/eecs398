package edu.cwru.eecs398.obdreader.elm327;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
public class ELMProtocolHandler extends Thread {
	private static final String TAG = "OBDProtocolHandler";

	protected static final String PROMPT = ">";
	protected static final String OK = "OK";
	private static final String BUS_INIT = "BUS INIT: ...";
	private static final String NO_DATA = "NO DATA";
	private static final String[] ERROR_MSGS = { "BUFFER FULL", "BUS BUSY",
		"BUS ERROR", "CAN ERROR", "FB ERROR", "DATA ERROR", "<DATA ERROR",
		"<RX ERROR", "UNABLE TO CONNECT", "?" };

	private final InputStream in;
	private final OutputStream out;
	private String rxData = "";
	protected String LT = "\r\n";

	private final Lock mutex = new ReentrantLock(true);

	private static final int TIMEOUT_DELAY = 10000;

	public void flush() {
		final byte[] readBuffer = new byte[256];

		try {
			if (in.available() > 0) {
				in.read(readBuffer);
				Log.i(TAG, "Read during flush: " + new String(readBuffer));
			}
		} catch (final IOException e) {
			// ignore, just want to flush out the buffer.
		}
		rxData = "";
	}

	@Override
	public void run() {
		final byte[] readBuffer = new byte[256];
		try {
			while (!Thread.interrupted()) {
				while (in.available() > 0) {
					final int numBytes = in.read(readBuffer);
					synchronized (mutex) {
						rxData = rxData + new String(readBuffer, 0, numBytes);
					}
				}
			}
		} catch (final IOException e) {
			Log.e(TAG, "Error reading from InputStream", e);
		}
	}

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

	private boolean waitForResetPrompt() {
		Log.d(TAG, "Waiting for reset prompt");
		if (waitForString(PROMPT) != null) {
			return true;
		} else {
			return false;
		}
	}

	protected boolean waitForOk() {
		Log.d(TAG, "Waiting for OK");
		if (waitForString(OK + LT + LT + PROMPT) != null) {
			return true;
		} else {
			return false;
		}
	}

	protected String waitForString(final String str) {
		final String preString;
		final Date firstTime = new Date();
		Date currentTime = new Date();
		while (currentTime.getTime() < (firstTime.getTime() + TIMEOUT_DELAY)) {
			synchronized (mutex) {
				Log.i(TAG, "Waiting for string: " + str + " Got: " + rxData);
				if (rxData.contains(BUS_INIT + LT)) {
					rxData = rxData.substring(rxData.indexOf(BUS_INIT + LT)
							+ BUS_INIT.length() + LT.length());
				}
				if (rxData.contains(NO_DATA + LT + LT + PROMPT)) {
					Log.e(TAG, "No data rx: " + rxData);
					final String oldRx = rxData;
					rxData = rxData.substring(rxData.indexOf(NO_DATA)
							+ NO_DATA.length()
							+ ((2 * LT.length()) + PROMPT.length()));
					return NO_DATA;
				}
				for (int i = 0; i < ERROR_MSGS.length; i++) {
					if (rxData.contains(ERROR_MSGS[i]/* + LT + LT + PROMPT */)) {
						Log.e(TAG, "Error Msg rx: " + rxData);
						final String oldRx = rxData;
						rxData = rxData.substring(rxData.indexOf(ERROR_MSGS[i])
								+ ERROR_MSGS[i].length()
								/* + ((2 * LT.length()) + PROMPT.length()) */);
						throw new ErrorMessageException(oldRx);
					}
				}
				if (rxData.contains(str)) {
					preString = rxData.substring(0, rxData.indexOf(str));
					rxData = rxData.substring(rxData.indexOf(str) + str.length());
					return preString;
				}
			}
			try {
				Thread.sleep(200);
			} catch (final InterruptedException e) {
			}
			currentTime = new Date();
		}
		return null;
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
		return waitForResetPrompt();
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
		synchronized (mutex) {
			rxData = "";
			if (out == null) {
				return false;
			}

			Log.d(TAG, "tx: " + msg);
			try {
				final byte[] msgArray = msg.getBytes("US-ASCII");
				/*
				 * for (int i = 0; i < msg.length(); i++) { final char ch =
				 * msg.charAt(i); out.write(ch); }
				 */
				out.write(msgArray);
				out.write(0x0d);
			} catch (final IOException e) {
				Log.w(TAG, "IOException while sending msg to ELM", e);
				return false;
			}
			return true;
		}
	}

	protected synchronized String sendOBDC(final byte mode, final byte pid) {
		String obdcReq = "";
		obdcReq = pad(mode) + pad(pid);
		send(obdcReq);
		final String ltPrompt = LT + PROMPT;
		final String ret = waitForString(ltPrompt);
		if (NO_DATA.equals(ret)) {
			final byte respConv = 0x40;
			return (mode | respConv) + " " + pad(pid);
		} else {
			return ret;
		}
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

	@Override
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


}
