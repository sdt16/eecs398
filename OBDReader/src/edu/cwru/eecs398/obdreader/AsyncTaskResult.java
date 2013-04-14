package edu.cwru.eecs398.obdreader;

/**
 * From http://stackoverflow.com/a/6312491/454167
 * 
 * @author Cagatay Kalan
 * 
 * @param <T>
 */

public class AsyncTaskResult<T> {
	private T result;
	private Exception error;

	public T getResult() {
		return result;
	}

	public Exception getError() {
		return error;
	}

	public AsyncTaskResult(final T result) {
		super();
		this.result = result;
	}

	public AsyncTaskResult(final Exception error) {
		super();
		this.error = error;
	}
}