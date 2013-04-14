package edu.cwru.eecs398.obdreader;

import java.util.ArrayList;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import edu.cwru.eecs398.obdreader.elm327.DTC;
import edu.cwru.eecs398.obdreader.elm327.DTCandTestData;
import edu.cwru.eecs398.obdreader.elm327.ELMProtocolHandler;
import edu.cwru.eecs398.obdreader.elm327.ErrorMessageException;
import edu.cwru.eecs398.obdreader.elm327.OBDProtocolHandler;

public class GetCodesAsyncTask extends
AsyncTask<ELMProtocolHandler, Void, AsyncTaskResult<ArrayList<String>>> {

	private static final String TAG = "GetCodes";
	private final MainActivity callingActivity;
	private final LinearLayout layoutWithSpinner;
	private final LinearLayout mainLayout;

	public GetCodesAsyncTask(final MainActivity ctx) {
		layoutWithSpinner = (LinearLayout) ctx
				.findViewById(R.id.layoutWithSpinner);
		mainLayout = (LinearLayout) ctx.findViewById(R.id.mainLayout);
		callingActivity = ctx;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		layoutWithSpinner.setVisibility(View.VISIBLE);
		mainLayout.setVisibility(View.GONE);
	}

	@Override
	protected AsyncTaskResult<ArrayList<String>> doInBackground(
			final ELMProtocolHandler... arg0) {
		final ArrayList<String> codeStrings = new ArrayList<String>();
		OBDProtocolHandler obdHandler = null;
		try {
			final ELMProtocolHandler elm = arg0[0];
			elm.run();
			elm.setLinefeed(true);
			elm.setCommandEcho(false);
			elm.sendAt("sp0");
			obdHandler = new OBDProtocolHandler(
					callingActivity, elm);
		} catch (final Exception e) {
			Log.e(TAG, "Generic error getting codes from car", e);
			return new AsyncTaskResult<ArrayList<String>>(e);
		}

		DTCandTestData data = null;
		try {
			data = obdHandler.getDTCandTestData();
		} catch (final ErrorMessageException e) {
			Log.e(TAG, "Got an error message from the ELM", e);
			return new AsyncTaskResult<ArrayList<String>>(e);
		}
		if (data.storedCodes != null) {
			for (final int i : data.storedCodes) {
				if (i == 0) {
					codeStrings.add("No Data");
				} else {
					final DTC code = obdHandler.dtcCollection.get(i);
					codeStrings.add(code.codeAsString());
				}
			}
		}

		return new AsyncTaskResult<ArrayList<String>>(codeStrings);
	}

	@Override
	protected void onPostExecute(final AsyncTaskResult<ArrayList<String>> result) {
		if (result.getResult() != null) {
			callingActivity.setObdCodes(result.getResult());
		} else {
			callingActivity.setObdCodesError(result.getError());
		}
		layoutWithSpinner.setVisibility(View.GONE);
		mainLayout.setVisibility(View.VISIBLE);
	}
}