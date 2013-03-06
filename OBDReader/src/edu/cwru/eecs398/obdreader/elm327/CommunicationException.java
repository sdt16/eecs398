package edu.cwru.eecs398.obdreader.elm327;

public class CommunicationException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public CommunicationException(String msg) {
		super(msg);
	}
	
	public CommunicationException(String msg, Throwable t) {
		super(msg, t);
	}
}
