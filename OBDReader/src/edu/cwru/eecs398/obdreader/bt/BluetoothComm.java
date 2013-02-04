package edu.cwru.eecs398.obdreader.bt;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;

public class BluetoothComm {
	
	private static int REQUEST_ENABLE_BT = 7590;
	
	public void setUpBluetooth() {
		BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
		if (btAdapter == null) {
			//device doesn't support bt
		}
		
		if (!btAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    //startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
	}
	

}
