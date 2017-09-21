/*
 * Copyright (C) 2014 Petrolr LLC, a Colorado limited liability company
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* 
 * Written by the Petrolr team in 2014. Based on the Android SDK Bluetooth Chat Example... matthew.helm@gmail.com
 */

package com.roomcontrol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;



/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
@SuppressLint("NewApi")
public class CopyOfBluetoothChatService {
	
	public Handler BTmsgHandler;
	
	// Debugging
	private static final String TAG = "BluetoothChatService";
	private static final boolean D = true;

	// Name for the SDP record when creating server socket
	private static final String NAME_SECURE = "BluetoothChatSecure";
	private static final String NAME_INSECURE = "BluetoothChatInsecure";

	// Unique UUID for this application
	private static final UUID MY_UUID_SECURE =
			UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
	private static final UUID MY_UUID_INSECURE =
			UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

	// Member fields
	private final BluetoothAdapter mAdapter;
	private final Handler mHandler;
	private AcceptThread mSecureAcceptThread;
	private AcceptThread mInsecureAcceptThread;
	private ConnectThread mConnectThread;
	private ConnectedThread mConnectedThread;
	private int mState;
	//private Set<String> mNewDevicesSet;

	//Copy voor integratie
	//private static final String TAG = "DeviceListActivity";
	//private static final boolean D = true;

	// Return Intent extra
	public static String EXTRA_DEVICE_ADDRESS = "device_address";
	//public static String myMacAddress = "00:19:5D:24:E5:4A"; //OBD11 adapter
	//public static String myMacAddress = "20:14:08:05:41:72"; //Itead
	public static String partnerDevAdd = "20:14:08:05:41:72"; //Itead
	//public static String partnerDevAdd = "00:11:07:19:00:40"; //ACC-BT02 eerder connected
	public boolean isConnected=true;

	
	
	// Member fields
	private BluetoothAdapter mBtAdapter;
	private ArrayAdapter<String> mPairedDevicesArrayAdapter;
	private ArrayAdapter<String> mNewDevicesArrayAdapter;
	private ArrayAdapter<String> mNewDevicesSet;
	// SPP UUID service
	private UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	//**einde integratie
	
	
	// Constants that indicate the current connection state
	public static final int STATE_NONE = 0;       // we're doing nothing
	public static final int STATE_LISTEN = 1;     // now listening for incoming connections
	public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
	public static final int STATE_CONNECTED = 3;  // now connected to a remote device

	/**
	 * Constructor. Prepares a new BluetoothChat session.
	 * @param context  The UI Activity Context
	 * @param handler  A Handler to send messages back to the UI Activity
	 */
	public CopyOfBluetoothChatService(Context context, Handler handler) {
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		Log.d(TAG, "Adapter: " + mAdapter);
		//mAdapter = MainActivity.mBluetoothAdapter;
		mState = STATE_NONE;
		mHandler = handler;
	}

		
	
	/**
	 * Set the current state of the chat connection
	 * @param state  An integer defining the current connection state
	 */
	private synchronized void setState(int state) {
		if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
		mState = state;

		// Give the new state to the Handler so the UI Activity can update
		mHandler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
	}

	/**
	 * Return the current connectDevice(device.getAddress().trim(), true);ion state. */
	public synchronized int getState() {
		return mState;
	}

	/**
	 * Start the chat service. Specifically start AcceptThread to begin a
	 * session in listening (server) mode. Called by the Activity onResume() */
	public synchronized void start() {
		if (D) Log.d(TAG, "start");

		// Cancel any thread attempting to make a connection
		if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

		setState(STATE_LISTEN);
		
		// Start the thread to listen on a BluetoothServerSocket
		if (mSecureAcceptThread == null) {
			mSecureAcceptThread = new AcceptThread(true);
			mSecureAcceptThread.start();
		}
		if (mInsecureAcceptThread == null) {
			mInsecureAcceptThread = new AcceptThread(false);
			mInsecureAcceptThread.start();
		}
	}

	/**
	 * Start the ConnectThread to initiate a connection to a remote device.
	 * @param device  The BluetoothDevice to connect
	 * @param secure Socket Security type - Secure (true) , Insecure (false)
	 */
	public synchronized void connect(BluetoothDevice device, boolean secure) {
		if (D) Log.d(TAG, "connect to: " + device);

		// Cancel any thread attempting to make a connection
		if (mState == STATE_CONNECTING) {
			if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
		}

		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

		// Start the thread to connect with the given device
		mConnectThread = new ConnectThread(device, secure);
		mConnectThread.start();
		setState(STATE_CONNECTING);
	}

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected 
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        if (D) Log.d(TAG, "connected, Socket Type:" + socketType);

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Cancel the accept thread because we only want to connect to one device
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }
        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
    	setState(STATE_LISTEN); //jcp
    	
    	// Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        CopyOfBluetoothChatService.this.start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
    	//setState(STATE_LISTEN); //jcp
    	
    	// Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        setState(STATE_CONNECTED); //jcp
        // Start the service over to restart listening mode
        CopyOfBluetoothChatService.this.start();
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptThread(boolean secure) {
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure":"Insecure";

            // Create a new listening server socket
            try {
                if (secure) {
                    tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE,
                        MY_UUID_SECURE);
                } else {
                    tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(
                            NAME_INSECURE, MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            if (D) Log.d(TAG, "Socket Type: " + mSocketType +
                    "BEGIN mAcceptThread" + this);
            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (CopyOfBluetoothChatService.this) {
                        switch (mState) {
                        case STATE_LISTEN:
                        case STATE_CONNECTING:
                             connected(socket, socket.getRemoteDevice(),mSocketType); 
                             break;
                        case STATE_NONE:
                        case STATE_CONNECTED:
                            // Either not ready or already connected. Terminate new socket.
                            try {
                                socket.close();
                            } catch (IOException e) {
                                Log.e(TAG, "Could not close unwanted socket", e);
                            }
                            break;
                        }
                    }
                }
            }
            if (D) Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);

        }

        public void cancel() {
            if (D) Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;
        
        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";
            
            //Modified to work with SPP Devices
            final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
           
            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                if (secure) {
                    //tmp = device.createRfcommSocketToServiceRecord(
                    //        MY_UUID_SECURE);
                	 
                	tmp = device.createRfcommSocketToServiceRecord(SPP_UUID);
                	
                	
                } else {
                    //tmp = device.createInsecureRfcommSocketToServiceRecord(
                    //        MY_UUID_INSECURE);
                	tmp = device.createRfcommSocketToServiceRecord(SPP_UUID);
                	
                	//Method m = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
                    //tmp = (BluetoothSocket) m.invoke(device, 1);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (CopyOfBluetoothChatService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            Log.d(TAG, "create ConnectedThread: " + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;


            // Keep listening to the InputStream while connected
            while (true) {
                try {

                	try {
                        sleep(100);
                    } catch (InterruptedException e) {
                    }
                	
                    // Read from the InputStream

                    bytes = mmInStream.read(buffer);
                    StringBuilder sb = new StringBuilder(); //jcp
                    
                    byte[] buffer_clone = new byte[1024];
                    System.arraycopy(buffer, 0, buffer_clone, 0, bytes);
                    
                        mHandler.obtainMessage(MainActivity.MESSAGE_READ, bytes, -1, buffer_clone)
                                .sendToTarget();
                        //mHandler.obtainMessage(MainActivity.MESSAGE_READ, bytes, -1, sb.toString().trim().getBytes())
                       // .sendToTarget(); //jcp

                	

                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    // Start the service over to restart listening mode
                    CopyOfBluetoothChatService.this.start();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(MainActivity.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
        
        
    }
    
//*********Integratie DeviceList************
    /**
     * This Activity appears as a dialog. It lists any paired devices and
     * devices detected in the area after discovery. When a device is chosen
     * by the user, the MAC address of the device is sent back to the parent
     * Activity in the result Intent.
     */
   /*
    public class DeviceListActivity extends Activity {
    	// Debugging
    	private static final String TAG = "DeviceListActivity";
    	private static final boolean D = true;

    	// Return Intent extra
    	public static String EXTRA_DEVICE_ADDRESS = "device_address";
    	//public static String myMacAddress = "00:19:5D:24:E5:4A"; //OBD11 adapter
    	//public static String myMacAddress = "20:14:08:05:41:72"; //Itead
    	public static String partnerDevAdd = "20:14:08:05:41:72"; //Itead
    	//public static String partnerDevAdd = "00:11:07:19:00:40"; //ACC-BT02 eerder connected
    	public boolean isConnected=true;

    	
    	
    	// Member fields
    	private BluetoothAdapter mBtAdapter;
    	private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    	private ArrayAdapter<String> mNewDevicesArrayAdapter;
    	private ArrayAdapter<String> mNewDevicesSet;
    	// SPP UUID service
    	private UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    */
    
    /*
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
    				//connect to device
    				//connect(device.getAddress().trim(), true);
    				//doDiscovery();
    				v.setVisibility(View.GONE);
    			}
    		});

    		// Initialize array adapters. One for already paired devices and
    		// one for newly discovered devices
    		mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
    		mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

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
   
    	*/
    	
    	/*
    	 * Start device discover with the BluetoothAdapter
    	*/
    
    /*
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
    	*/
    
   
    	
//*********************Einde integratie***********
    
}


