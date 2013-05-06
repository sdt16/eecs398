package edu.cwru.eecs398.obdreader.elm327;

/**
 * Simple exception to represent errors from the ELM327.
 * 
 * @author Schuyler Thompson
 */
public class ErrorMessageException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ErrorMessageException(final String msg) {
		super(msg);
	}

}
