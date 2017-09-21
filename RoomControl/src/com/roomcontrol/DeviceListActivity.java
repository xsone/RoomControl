package com.roomcontrol;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import com.roomcontrol.R;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings.Global;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

/**
 * This Activity appears as a dialog. It lists any paired devices and
 * devices detected in the area after discovery. When a device is chosen
 * by the user, the MAC address of the device is sent back to the parent
 * Activity in the result Intent.
 */
public class DeviceListActivity extends Activity {
	// Debugging
	private static final String TAG = "DeviceListActivity";
	private static final boolean D = true;

	// Return Intent extra
	public static String EXTRA_DEVICE_ADDRESS = "device_address";
	//public static String myMacAddress = "00:19:5D:24:E5:4A"; //OBD11 adapter
	//public static String myMacAddress = "20:14:08:05:41:72"; //Itead
	//public static String partnerDevAdd1 = "20:14:08:05:41:72"; //Itead
	//public static String partnerDevAdd = "00:12:12:04:10:57"; //linvor
	
	//public static String partnerDevAdd = "00:11:07:19:00:40"; //ACC-BT02 eerder connected
	
	
	public boolean isConnected=true;
	private static BluetoothChatService mChatService = null;
	
	
	// Member fields
	private BluetoothAdapter mBtAdapter;
	private ArrayAdapter<String> mPairedDevicesArrayAdapter;
	private ArrayAdapter<String> mNewDevicesArrayAdapter;
	private ArrayAdapter<String> mNewDevicesSet;
	
	ArrayList<BluetoothDevice> arrayListPairedBluetoothDevices;
	// SPP UUID service
	private UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Setup the window
		//requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.device_list);

		// Set result CANCELED in case the user backs out
		setResult(Activity.RESULT_CANCELED);

		// Initialize the button to perform device discovery
		Button scanButton = (Button) findViewById(R.id.button_scan);
		scanButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				doDiscovery();
				v.setVisibility(View.GONE);
			}
		});

		// Initialize array adapters. One for already paired devices and
		// one for newly discovered devices
		mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
		mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
		
		arrayListPairedBluetoothDevices = new ArrayList<BluetoothDevice>();
		

		// Find and set up the ListView for paired devices
		ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
		pairedListView.setAdapter(mPairedDevicesArrayAdapter);
		pairedListView.setOnItemClickListener(mDeviceClickListener);

		// Find and set up the ListView for newly discovered devices
		ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
		newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
		newDevicesListView.setOnItemClickListener(mDeviceClickListener);

		// Register for broadcasts when a device is discovered
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		this.registerReceiver(mReceiver, filter);

		// Register for broadcasts when discovery has finished
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		this.registerReceiver(mReceiver, filter);

		// Get the local Bluetooth adapter
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();

		// Get a set of currently paired devices
		Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

		
		// If there are paired devices, add each one to the ArrayAdapter
		if (pairedDevices.size() > 0) {
			findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
			for (BluetoothDevice device : pairedDevices) {
				mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
				//mChatService.connect(device, true); //jcp
				//arrayListPairedBluetoothDevices.add(device); //jcp
			}
		 } else {
	            String noDevices = getResources().getText(R.string.none_paired).toString();
	            mPairedDevicesArrayAdapter.add(noDevices);
	       }
	}	

	@Override
    protected void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
    }
	
	/*
	 * Start device discover with the BluetoothAdapter
	*/
	private void doDiscovery() {
	 if (D) Log.d(TAG, "doDiscovery()");
				        
	 // clear results of a previous discovery
	 //mNewDevicesArrayAdapter.clear();
	 //mNewDevicesSet.clear();

	 // Indicate scanning in the title
	 setProgressBarIndeterminateVisibility(true);
	 setTitle(R.string.scanning);

	 // Turn on sub-title for new devices
	 findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

	 // If we're already discovering, stop it
	 if (mBtAdapter.isDiscovering()) {
	     mBtAdapter.cancelDiscovery();
	 }
     // Request discover from BluetoothAdapter
     mBtAdapter.startDiscovery();
	  
} //,0,15000);// Start discovery every 15 second

// The on-click listener for alle devices in the ListViews
private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
	public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
		// Cancel discovery because it's costly and we're about to connect
		mBtAdapter.cancelDiscovery();

		//Get the device MAC address, which is the last 17 chars in the View
		String info = ((TextView) v).getText().toString();
		String address = info.substring(info.length() - 17);
	
		// Create the result Intent and include the MAC address
		Intent intent = new Intent();
		intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

		// Set result and finish this Activity
		setResult(Activity.RESULT_OK, intent);
		finish();
	   }
	};
	
// The BroadcastReceiver that listens for discovered devices and
// changes the title when discovery is finished
private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		
	@Override
    public void onReceive(Context context, Intent intent) {
       String action = intent.getAction();
           
     //We don't want to reconnect to already connected device
     if(isConnected==false){
       // When discovery finds a device
       if (BluetoothDevice.ACTION_FOUND.equals(action)) {
          // Get the BluetoothDevice object from the Intent
          BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
          //String address = device.getAddress();
          	
           // Check if the found device is one we had comm with
     //       if(device.getAddress().equals(partnerDevAdd)==true)
     //           connectToExisting(device);
            
        //  if (! mNewDevicesSet.contains(address)) {
        //    		mNewDevicesSet.add(address);
          	if (device.getBondState() != BluetoothDevice.BOND_BONDED){
            		mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            		 
            	}
            // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle(R.string.select_BTdevice);
                if (mNewDevicesSet.isEmpty()) {
                    String noDevices = getResources().getText(R.string.none_found).toString();
                    mNewDevicesArrayAdapter.add(noDevices);
                }
               // scanButton.setVisibility(View.VISIBLE);
            }
       
       /*
       //nieuw toegevoegd door connection lost
       //if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
       if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
               // Get the BluetoothDevice object from the Intent
               BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

               // Check if the connected device is one we had comm with
               if(device.getAddress().equals(partnerDevAdd)==true)
                   isConnected=true;
    	   //disconnect request
    	   //deze intent.getaction() wordt gactiveerd vanuit de actio die de Intent startte, in dit geval dus de BT-connection
       } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
    	// Get the BluetoothDevice object from the Intent
           BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

           // Check if the connected device is one we had comm with
           if(device.getAddress().equals(partnerDevAdd)==true)
               isConnected=false;
           //disconnected, do what you want to notify user here, toast, or dialog, etc.
       }
      */
      }
    };
    
       
    private void connectToExisting(BluetoothDevice device){
      //  new ConnectThread(device);
    }
 };
}   
	
