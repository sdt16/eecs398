package edu.cwru.eecs398.obdreader;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import edu.cwru.eecs398.obdreader.elm327.DTCandTestData;
import edu.cwru.eecs398.obdreader.elm327.ELMProtocolHandler;
import edu.cwru.eecs398.obdreader.elm327.ErrorMessageException;
import edu.cwru.eecs398.obdreader.elm327.OBDProtocolHandler;

public class GetCodesAsyncTask extends
AsyncTask<ELMProtocolHandler, Void, AsyncTaskResult<DTCandTestData>> {

	private static final String TAG = "GetCodes";
	private final MainActivity callingActivity;
	private final LinearLayout layoutWithSpinner;
	private final LinearLayout infoTextLayout;
	private final LinearLayout codesLayout;

	public GetCodesAsyncTask(final MainActivity ctx) {
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
	protected AsyncTaskResult<DTCandTestData> doInBackground(
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
			Log.e(TAG, "Generic error getting codes from car", e);
			return new AsyncTaskResult<DTCandTestData>(e);
		}

		DTCandTestData data = null;
		try {
			data = obdHandler.getDTCandTestData();
		} catch (final ErrorMessageException e) {
			Log.e(TAG, "Got an error message from the ELM", e);
			return new AsyncTaskResult<DTCandTestData>(e);
		}
		return new AsyncTaskResult<DTCandTestData>(data);
	}

	@Override
	protected void onPostExecute(final AsyncTaskResult<DTCandTestData> result) {
		if (result.getResult() != null) {
			callingActivity.setObdCodes(result.getResult());
		} else {
			callingActivity.setObdCodesError(result.getError());
		}
		layoutWithSpinner.setVisibility(View.GONE);
		// mainLayout.setVisibility(View.VISIBLE);
	}
}