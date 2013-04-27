package edu.cwru.eecs398.obdreader;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.UUID;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import edu.cwru.eecs398.obdreader.elm327.DTC;
import edu.cwru.eecs398.obdreader.elm327.DTCandTestData;
import edu.cwru.eecs398.obdreader.elm327.ELMProtocolHandler;
import edu.cwru.eecs398.obdreader.elm327.OBDProtocolHandler;

public class MainActivity extends FragmentActivity {

	private static final String TAG = "MainActivity";

	private static final String STATE_BLUETOOTH_DEVICE_PICKED = "bt_device_picked";
	private static final String STATE_DOWNLOADED_CODES = "downloaded_codes";

	private final static int REQUEST_ENABLE_BT = 7590;

	static final UUID UUID_RFCOMM_GENERIC = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");

	private BluetoothAdapter btAdapter;
	protected BluetoothSocket btSocket;

	private ELMProtocolHandler elm;

	private Boolean pickedDevice = false;

	private DTCandTestData dtcCodes;
	private OBDProtocolHandler obdHandler;


	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_main);

		// Set up the action bar to show tabs.
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		setProgressBarIndeterminateVisibility(true);

		btAdapter = BluetoothAdapter.getDefaultAdapter();
		if (btAdapter == null) {
			//device doesn't support bt
		} else if (!btAdapter.isEnabled()) {
			final Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		} else {
			pickBtDevice();
		}

		setProgressBarIndeterminateVisibility(false);
		obdHandler = new OBDProtocolHandler(this, elm);
	}

	@Override
	public void onConfigurationChanged(final Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		setupFragmentDisplay();
	}

	@Override
	public void onStart() {
		super.onStart();
		setupFragmentDisplay();
	}

	private void setupFragmentDisplay() {
		final Bundle args = new Bundle();
		final ArrayList<String> codeStrings = new ArrayList<String>();

		if ((dtcCodes != null) && (dtcCodes.storedCodes != null)) {
			for (final int i : dtcCodes.storedCodes) {
				if (i == 0) {
					// codeStrings.add("No Data");
				} else {
					final DTC code = obdHandler.dtcCollection.get(i);
					if (code != null) {
						codeStrings.add(code.codeAsString() + " "
								+ code.description());
					}
				}
			}
			args.putStringArrayList(DummySectionFragment.ARG_CODES, codeStrings);
		}

		if (dtcCodes != null) {
			args.putBoolean(DummySectionFragment.ARG_MIL, dtcCodes.mil);
		}
		final boolean btSockNull = btSocket != null ? true : false;
		args.putBoolean(DummySectionFragment.ARG_BT_CONNECTED, btSockNull);
		final FragmentManager fragmentManager = getFragmentManager();

		final FragmentTransaction fragmentTransaction = fragmentManager
				.beginTransaction();
		final Fragment fragment = new DummySectionFragment();
		fragment.setArguments(args);
		if (dtcCodes == null) {
			fragmentTransaction.replace(R.id.infoTextLayout, fragment);
			final LinearLayout layout = (LinearLayout) findViewById(R.id.infoTextLayout);
			layout.setVisibility(View.VISIBLE);
		} else {
			fragmentTransaction.replace(R.id.codesLayout, fragment);
			final LinearLayout layout = (LinearLayout) findViewById(R.id.codesLayout);
			layout.setVisibility(View.VISIBLE);
		}

		fragmentTransaction.commit();
	}

	@Override
	public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		if (requestCode == REQUEST_ENABLE_BT) {
			if (resultCode == RESULT_OK) {
				pickBtDevice();
			} else {
				//bt is off
			}
		}
	}

	private void makeErrorDialog(final Throwable t) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(t.getClass().getName());
		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		builder.setMessage(sw.toString());
		builder.setPositiveButton("OK", null);
		builder.show();
	}

	private void pickBtDevice() {
		final ArrayList<BluetoothDevice> pairedDevices = new ArrayList<BluetoothDevice>(btAdapter.getBondedDevices());
		if (pairedDevices.size() > 0) {
			final AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.pick_bt_device);
			final String[] deviceStrings = new String[pairedDevices.size()];
			for (int i = 0; i<pairedDevices.size(); i++) {
				deviceStrings[i] = pairedDevices.get(i).getName() + " (" + pairedDevices.get(i).getAddress() + ")\n";
			}
			final MainActivity ctx = this;
			builder.setItems(deviceStrings,
					new DialogInterface.OnClickListener() {

				@Override
				public void onClick(final DialogInterface dialog, final int which) {
					pickedDevice = true;
					try {
						new GetBtConnAsyncTask(ctx).execute(pairedDevices.get(which));
					} catch (final RuntimeException e) {
						Log.e(TAG, "Error getting socket", e);
						makeErrorDialog(e);
					}
					// final BluetoothConnectionThread
					// connectionThread = new
					// BluetoothConnectionThread(pairedDevices.get(which),
					// btAdapter);
					// connectionThread.start();
				}
			});
			builder.show();
		}
	}

	public void setBluetoothSocket(final BluetoothSocket socket) {
		if (btSocket != null) {
			try {
				btSocket.close();
			} catch (final IOException e) {
				Log.e(TAG, "Error closing BT conn", e);
				makeErrorDialog(e);
			}
		}
		btSocket = socket;
		try {
			elm = new ELMProtocolHandler(
					btSocket.getInputStream(), btSocket.getOutputStream());
			elm.start();
		} catch (final IOException e) {
			Log.e(TAG, "Error init BT socket", e);
			makeErrorDialog(e);
		}
		setupFragmentDisplay();
	}

	public void setBluetoothSocketError(final Exception e) {
		if (btSocket != null) {
			try {
				btSocket.close();
			} catch (final IOException e1) {
				Log.e(TAG, "Error closing BT conn", e);
				makeErrorDialog(e);
			}
		}
		btSocket = null;
		makeErrorDialog(e);
		setupFragmentDisplay();
	}

	public void setObdCodes(final DTCandTestData codes) {
		dtcCodes = codes;
		setupFragmentDisplay();
	}

	public void setObdCodesError(final Exception e) {
		dtcCodes = null;
		makeErrorDialog(e);
		setupFragmentDisplay();
	}

	@Override
	public void onRestoreInstanceState(final Bundle savedInstanceState) {
		pickedDevice = savedInstanceState.getBoolean(STATE_BLUETOOTH_DEVICE_PICKED);
		dtcCodes = (DTCandTestData) savedInstanceState
				.getSerializable(STATE_DOWNLOADED_CODES);
	}

	@Override
	public void onSaveInstanceState(final Bundle outState) {
		outState.putSerializable(STATE_DOWNLOADED_CODES, dtcCodes);
		outState.putBoolean(STATE_BLUETOOTH_DEVICE_PICKED, pickedDevice);
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	private void getCodes() {
		if (btSocket != null) {
			final GetCodesAsyncTask task = new GetCodesAsyncTask(this);
			// Thread.sleep(2000);
			task.execute(new ELMProtocolHandler[] { elm });
		} else {
			final AlertDialog.Builder builder = new AlertDialog.Builder(
					this);
			builder.setTitle(R.string.Error);
			builder.setMessage(R.string.connect_to_bt_first);
			builder.setPositiveButton("OK", null);
			builder.show();
		}
	}

	private void clearCodes() {
		if (btSocket != null) {
			final AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.clear_codes_dialog);
			builder.setMessage(R.string.clear_codes_message);
			final ClearCodesAsyncTask clearCodesTask = new ClearCodesAsyncTask(
					this);
			builder.setPositiveButton("OK", new OnClickListener() {

				@Override
				public void onClick(final DialogInterface dialog,
						final int which) {
					clearCodesTask.execute(new ELMProtocolHandler[] { elm });
				}
			});
			builder.setNegativeButton("Cancel", null);
			builder.show();
		} else {
			final AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.Error);
			builder.setMessage(R.string.connect_to_bt_first);
			builder.setPositiveButton("OK", null);
			builder.show();
		}
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_get_codes:
			getCodes();
			return true;
		case R.id.menu_bt_connect:
			pickBtDevice();
			return true;
		case R.id.menu_clear_codes:
			clearCodes();
			return true;
		default:
			return false;
		}
	}

	/**
	 * A dummy fragment representing a section of the app, but that simply
	 * displays dummy text.
	 */
	public static class DummySectionFragment extends Fragment {
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		public static final String ARG_SECTION_NUMBER = "section_number";
		public static final String ARG_CODES = "codes";
		public static final String ARG_STRING = "string";
		public static final String ARG_BT_CONNECTED = "bt_connected";
		public static final String ARG_MIL = "mil";

		public DummySectionFragment() {
		}

		@Override
		public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
				final Bundle savedInstanceState) {
			// Create a new TextView and set its text to the fragment's section
			// number argument value.
			final ArrayList<String> codes = getArguments().getStringArrayList(
					ARG_CODES);
			final boolean mil = getArguments().getBoolean(ARG_MIL);
			final boolean btConnected = getArguments().getBoolean(ARG_BT_CONNECTED);
			if (codes != null) {
				final ListView listView = new ListView(getActivity());
				final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
						getActivity(), android.R.layout.simple_list_item_1);
				arrayAdapter.add("MIL Status: " + (mil ? "On" : "Off"));
				arrayAdapter.addAll(codes);
				listView.setAdapter(arrayAdapter);
				return listView;
			} else {
				final TextView textView = new TextView(getActivity());
				textView.setGravity(Gravity.CENTER);
				if (btConnected) {
					textView.setText(R.string.bluetooth_connected);
				} else {
					textView.setText(R.string.bluetooth_not_connected);
				}
				return textView;
			}

		}
	}

}
