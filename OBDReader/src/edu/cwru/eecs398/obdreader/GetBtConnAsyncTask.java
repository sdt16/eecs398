package edu.cwru.eecs398.obdreader;

import java.io.IOException;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.view.View;
import android.widget.LinearLayout;

public class GetBtConnAsyncTask extends
AsyncTask<BluetoothDevice, Void, AsyncTaskResult<BluetoothSocket>> {

	static final UUID UUID_RFCOMM_GENERIC = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");

	private final MainActivity callingActivity;
	private final LinearLayout layoutWithSpinner;
	private final LinearLayout mainLayout;


	public GetBtConnAsyncTask(final MainActivity ctx) {
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
	protected AsyncTaskResult<BluetoothSocket> doInBackground(
			final BluetoothDevice... arg0) {

		/*
		 * final BluetoothServerSocket serverSocket = btAdapter
		 * .listenUsingRfcommWithServiceRecord( "OBDBluetooth",
		 * UUID_RFCOMM_GENERIC);
		 * 
		 * btSocket = serverSocket.accept();
		 */

		try {
			final BluetoothSocket btSocket = arg0[0]
					.createRfcommSocketToServiceRecord(UUID_RFCOMM_GENERIC);
			btSocket.connect();
			return new AsyncTaskResult<BluetoothSocket>(btSocket);
		} catch (final IOException e) {
			return new AsyncTaskResult<BluetoothSocket>(e);
		}
	}

	@Override
	protected void onPostExecute(final AsyncTaskResult<BluetoothSocket> result) {
		if (result.getResult() != null) {
			callingActivity.setBluetoothSocket(result.getResult());
		} else {
			callingActivity.setBluetoothSocketError(result.getError());
		}
		layoutWithSpinner.setVisibility(View.GONE);
		mainLayout.setVisibility(View.VISIBLE);
	}
}
