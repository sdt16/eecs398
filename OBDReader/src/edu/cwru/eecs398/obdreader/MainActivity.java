package edu.cwru.eecs398.obdreader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import edu.cwru.eecs398.obdreader.elm327.DTC;
import edu.cwru.eecs398.obdreader.elm327.DTCandTestData;
import edu.cwru.eecs398.obdreader.elm327.ELMProtocolHandler;
import edu.cwru.eecs398.obdreader.elm327.OBDProtocolHandler;

public class MainActivity extends FragmentActivity implements ActionBar.TabListener {

	private static final String TAG = "MainActivity";

	/**
	 * The serialization (saved instance state) Bundle key representing the
	 * current tab position.
	 */
	private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";

	private static final String STATE_BLUETOOTH_DEVICE_PICKED = "bt_device_picked";

	private final static int REQUEST_ENABLE_BT = 7590;

	static final UUID UUID_RFCOMM_GENERIC = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");

	private BluetoothAdapter btAdapter;
	private BluetoothSocket btSocket;

	private Boolean pickedDevice = false;

	private ArrayList<String> dtcCodes;


	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Set up the action bar to show tabs.
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		// For each of the sections in the app, add a tab to the action bar.
		actionBar.addTab(actionBar.newTab().setText(R.string.title_section1).setTabListener(this));
		actionBar.addTab(actionBar.newTab().setText(R.string.title_section2).setTabListener(this));
		actionBar.addTab(actionBar.newTab().setText(R.string.title_section3).setTabListener(this));

		btAdapter = BluetoothAdapter.getDefaultAdapter();
		if (btAdapter == null) {
			//device doesn't support bt
		} else if (!btAdapter.isEnabled()) {
			final Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		} else {
			pickBtDevice();
		}
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

	private void pickBtDevice() {
		if (!pickedDevice) {
			final ArrayList<BluetoothDevice> pairedDevices = new ArrayList<BluetoothDevice>(btAdapter.getBondedDevices());
			if (pairedDevices.size() > 0) {
				final AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.pick_bt_device);
				final String[] deviceStrings = new String[pairedDevices.size()];
				for (int i = 0; i<pairedDevices.size(); i++) {
					deviceStrings[i] = pairedDevices.get(i).getName() + " (" + pairedDevices.get(i).getAddress() + ")\n";
				}
				builder.setItems(deviceStrings, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(final DialogInterface dialog, final int which) {
						pickedDevice = true;
						try {

							final BluetoothServerSocket serverSocket = btAdapter
									.listenUsingRfcommWithServiceRecord(
											"OBDBluetooth",
											UUID_RFCOMM_GENERIC);

							btSocket = serverSocket.accept();
							/*
							 * btSocket = pairedDevices.get(which)
							 * .createRfcommSocketToServiceRecord(
							 * UUID_RFCOMM_GENERIC); btSocket.connect();
							 */
						} catch (final IOException e) {
							Log.e(TAG, "Error getting socket", e);
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
	}

	@Override
	public void onRestoreInstanceState(final Bundle savedInstanceState) {
		// Restore the previously serialized current tab position.
		if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
			getActionBar().setSelectedNavigationItem(
					savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
		}

		pickedDevice = savedInstanceState.getBoolean(STATE_BLUETOOTH_DEVICE_PICKED);
	}

	@Override
	public void onSaveInstanceState(final Bundle outState) {
		// Serialize the current tab position.
		outState.putInt(STATE_SELECTED_NAVIGATION_ITEM,
				getActionBar().getSelectedNavigationIndex());
		outState.putBoolean(STATE_BLUETOOTH_DEVICE_PICKED, pickedDevice);
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public void onTabSelected(final ActionBar.Tab tab, final FragmentTransaction fragmentTransaction) {
		// When the given tab is selected, show the tab contents in the
		// container view.
		final Fragment fragment = new DummySectionFragment();
		// final ArrayList<String> codes = getCodes();
		final Bundle args = new Bundle();
		if (dtcCodes != null) {
			args.putStringArrayList(DummySectionFragment.ARG_CODES, dtcCodes);
		}
		args.putInt(DummySectionFragment.ARG_SECTION_NUMBER, tab.getPosition() + 1);
		fragment.setArguments(args);
		getSupportFragmentManager().beginTransaction()
		.replace(R.id.container, fragment)
		.commit();
	}

	private ArrayList<String> getCodes() {
		OBDProtocolHandler obdHandler = null;
		final ArrayList<String> codeStrings = new ArrayList<String>();
		try {
			final ELMProtocolHandler elm = new ELMProtocolHandler(btSocket.getInputStream(),
					btSocket.getOutputStream());
			elm.run();
			elm.setCommandEcho(false);
			obdHandler = new OBDProtocolHandler(this, elm);
		} catch (final IOException e) {
			Log.e(TAG, "Error init BT socket", e);
		}

		final DTCandTestData data = obdHandler.getDTCandTestData();
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

		return codeStrings;
	}

	@Override
	public void onTabUnselected(final ActionBar.Tab tab, final FragmentTransaction fragmentTransaction) {
	}

	@Override
	public void onTabReselected(final ActionBar.Tab tab, final FragmentTransaction fragmentTransaction) {
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_get_codes:
			dtcCodes = getCodes();
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

		public DummySectionFragment() {
		}

		@Override
		public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
				final Bundle savedInstanceState) {
			// Create a new TextView and set its text to the fragment's section
			// number argument value.

			if (getArguments().getInt(ARG_SECTION_NUMBER) == 1) {

				final ListView listView = new ListView(getActivity());
				final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1);
				final ArrayList<String> codes = getArguments()
						.getStringArrayList(ARG_CODES);
				if (codes != null) {
					arrayAdapter.addAll(codes);
				}
				listView.setAdapter(arrayAdapter);
				return listView;
			} else {
				final TextView textView = new TextView(getActivity());
				textView.setGravity(Gravity.CENTER);
				textView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));
				return textView;
			}


		}
	}

}
