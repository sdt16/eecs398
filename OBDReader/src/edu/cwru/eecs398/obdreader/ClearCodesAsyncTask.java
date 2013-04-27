package edu.cwru.eecs398.obdreader;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import edu.cwru.eecs398.obdreader.elm327.ELMProtocolHandler;
import edu.cwru.eecs398.obdreader.elm327.ErrorMessageException;
import edu.cwru.eecs398.obdreader.elm327.OBDProtocolHandler;

public class ClearCodesAsyncTask extends
AsyncTask<ELMProtocolHandler, Void, AsyncTaskResult<Boolean>> {

	private static final String TAG = "ClearCodes";
	private final MainActivity callingActivity;
	private final LinearLayout layoutWithSpinner;
	private final LinearLayout infoTextLayout;
	private final LinearLayout codesLayout;

	public ClearCodesAsyncTask(final MainActivity ctx) {
		layoutWithSpinner = (LinearLayout) ctx
				.findViewById(R.id.layoutWithSpinner);
		infoTextLayout = (LinearLayout) ctx.findViewById(R.id.infoTextLayout);
		codesLayout = (LinearLayout) ctx.findViewById(R.id.codesLayout);
		callingActivity = ctx;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		layoutWithSpinner.setVisibility(View.VISIBLE);
		infoTextLayout.setVisibility(View.GONE);
		codesLayout.setVisibility(View.GONE);

	}

	@Override
	protected AsyncTaskResult<Boolean> doInBackground(
			final ELMProtocolHandler... arg0) {
		OBDProtocolHandler obdHandler = null;
		final ELMProtocolHandler elm = arg0[0];
		try {
			elm.flush();
			elm.reset();
			elm.setLinefeed(true);
			elm.setCommandEcho(false);
			elm.sendAt("sp0");
			obdHandler = new OBDProtocolHandler(callingActivity, elm);
		} catch (final Exception e) {
			Log.e(TAG, "Generic error clearing codes", e);
			return new AsyncTaskResult<Boolean>(e);
		}

		boolean success = false;
		try {
			success = obdHandler.clearTroubleCodes();
			if (!success) {
				throw new ErrorMessageException("Error clearing codes");
			}
		} catch (final ErrorMessageException e) {
			Log.e(TAG, "Got an error message from the ELM", e);
			return new AsyncTaskResult<Boolean>(e);
		}
		return new AsyncTaskResult<Boolean>(success);
	}

	@Override
	protected void onPostExecute(final AsyncTaskResult<Boolean> result) {
		if (result.getResult() != null) {
			callingActivity.setObdCodes(null);
		} else {
			callingActivity.setObdCodesError(result.getError());
		}
		layoutWithSpinner.setVisibility(View.GONE);
		// mainLayout.setVisibility(View.VISIBLE);
	}
}