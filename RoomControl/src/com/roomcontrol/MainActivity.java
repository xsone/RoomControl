/*
 * Copyright (C) oktober 2016 Jack Cop LLC, St. Nicolaasga

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
 * Written by the Jack in 2016. Based on the Android SDK Bluetooth Chat Example... xsone2@gmail.com
 * 
 */

package com.roomcontrol;

import com.roomcontrol.R;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import android.graphics.Color;
import android.graphics.PorterDuff;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;


//public abstract class MainActivity extends Activity implements OnClickListener,
//public class MainActivity extends Activity implements OnClickListener, OnSeekBarChangeListener {
public class MainActivity extends Activity {	
	
	private static final String TAG = "RoomControl";
	//private ListView mConversationView;
	// Name of the connected device
	private String mConnectedDeviceName = null;
	// Array adapter for the conversation thread
	private ArrayAdapter<String> mConversationArrayAdapter;
	// String buffer for outgoing messages
	//private static StringBuffer mOutStringBuffer;
	// Local Bluetooth adapter
	static BluetoothAdapter mBluetoothAdapter = null;
	// Member object for the chat services
	private static BluetoothChatService mChatService = null;
	
	//ArrayList<BluetoothDevice> arrayListBluetoothDevices = null;
	
	
	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
	private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
	private static final int REQUEST_ENABLE_BT = 3;
	
	public MediaPlayer mMediaPlayer;
	
	// Message types sent from the BluetoothChatService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;
	public static final int MESSAGE_ERROR = 6;
	public static final int MESSAGE_LOST = 7;
	
	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";
	
	//private String partnerDevAdd="00:11:07:19:00:40"; //ACC-BT02 eerder connected
	//private String partnerDevAdd="20:14:08:05:41:72"; //itead eerder connected
	//public static String partnerDevAdd = "00:12:12:04:10:57"; //linvor
	public static String btAddress = "00:12:12:04:10:57"; //linvor
	//public static String lastConnectedDevice;
	//public static String btAddressTest = "20:14:08:05:41:72";
	
	//CVserver settings
	String cv_server_url = "http://192.168.178.80:8888"; // final URL after
	
	private static final boolean VERBOSE = true;
	public boolean CVSERVER = true;
	public boolean CVSTATE = false;
	public boolean SPSTATE = false;
	public boolean MOVE = true;
		
	public BluetoothDevice btDevice;
	public BluetoothDevice btDeviceTest;
	boolean btSecure;
	public String btAction;
	public int btState = 0;
	public int moveTest = 0;
	public int smokeTest = 0;
	
	public int gemTeller = 0;
	public int gemAantal = 25;
	public int spStartTeller = 0;
	public long noMoveTimer = 0;
	public long noMoveDelay = 50000;

	//public InputStream mmInStream = null;
	public String[] readStringValuesTest;
	
	TextView tvTemp;
	TextView spTemp;
	TextView tvHumi;
	Button btnKamer1;
	Button btnKamer2;
	Button btnKamer3;
	Button btnMin;
	Button btnPlus;
	
	SeekBar seekBar;
	
	
	public float Temperature = 11;
	public float oldTemperature = 11;
	public float Humidity = 11;
	public String []readStringValues = new String[16]; 
	public float userSetpoint = 11; //*
	public float progress = 11; //*
	public Timer autoUpdate;
	public long cvOnTimer = 10000; //waarde wordt preferences
	//OnCreate screens maken
	@Override
	protected void onCreate(Bundle savedInstanceState) {
	StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
	StrictMode.setThreadPolicy(policy);
		 
	super.onCreate(savedInstanceState);
	setContentView(R.layout.main_layout);
	
	//seekBar = (SeekBar) findViewById(R.id.seekBar2);

	btnKamer1 = (Button) findViewById(R.id.button_kamer1);
	btnKamer2 = (Button) findViewById(R.id.button_kamer2);
	btnKamer3 = (Button) findViewById(R.id.button_kamer3);
	
	tvHumi = (TextView) findViewById(R.id.tvHumi);
	tvTemp = (TextView) findViewById(R.id.tvTemp);
	spTemp = (TextView) findViewById(R.id.spTemp);
	
	mMediaPlayer = MediaPlayer.create(this,R.raw.beep8); //Alarm
	
	//seekBar.setProgress(2);
	//seekBar.incrementProgressBy(1);
	//seekBar.setMax(60);
	//seekBar.setOnSeekBarChangeListener(this);
	
	readStringValues[1] = "11.1";
	readStringValues[2] = "22.2";
	readStringValues[3] = "0";
	readStringValues[4] = "0";				
	
	/*
	Button minButton = (Button) findViewById(R.id.button_min);
	minButton.setOnClickListener(new OnClickListener() {
		public void onClick(View arg0) {
		}
	});
	
	Button plusButton = (Button) findViewById(R.id.button_plus);
	plusButton.setOnClickListener(new OnClickListener() {
		public void onClick(View arg0) {
		}
	});
	*/
	
	
	// Get local Bluetooth adapter
	mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	
	//startService(new Intent(this, BLEService.class)); //BLE service
	//startService(new Intent(this, BluetoothChatService.class)); //BLE service
	Log.d(TAG, "Adapter: " + mBluetoothAdapter);
	 // Start the  service
    //public void startService(View view) {
    //    startService(new Intent(this, BluetoothChatService.class));
    //}

    // Stop the  service
     //   public void stopNewService(View view) {
     //      stopService(new Intent(this, BLEService.class));
      //  }
		
	// If the adapter is null, then Bluetooth is not supported
	if (mBluetoothAdapter == null) {
		Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
		finish();
		return;
	}
}	
	
	//Scan naar connected BlueTooth devices
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	//Connect button van ander submenu kan vervallen
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		Intent serverIntent = null;
		switch (item.getItemId()) {
		case R.id.secure_connect_scan:
				// Launch the DeviceListActivity to see devices and do scan
				serverIntent = new Intent(this, DeviceListActivity.class);
				startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
				return true;
			case R.id.menu_settings:
		    		startActivity(new Intent(this, Prefs.class));
		    		break;
		    case R.id.menu_exit:
		    		onDestroy();
		    		break;
			default: break;
		}
	 return super.onOptionsItemSelected(item);
	}		
		
	//Start programma
	//@Override
	public void onStart(){
		super.onStart();
	    
		//Intent data = null;
		SharedPreferences myPreference=PreferenceManager.getDefaultSharedPreferences(this);
	    SharedPreferences.Editor myPreferenceEditor = myPreference.edit();
	    myPreferenceEditor.putString("btMacAddress", btAddress);
	    String noMoveDelay = myPreference.getString("noMoveDelay","");
	    noMoveTimer = Integer.valueOf(noMoveDelay);
	    //noMoveTimer = 10000;
	    
	    addListenerOnButtonMin();
	    addListenerOnButtonPlus();
	    // If BT is not on, request that it be enabled.
		// setupChat() will then be called during onActivityResult
	    	    
	    if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		// Otherwise, setup the chat session
		} else {
			if (mChatService == null) 
				setupChat(); //orgineel
			//btAddress = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
			//btAddress = BluetoothDevice.EXTRA_DEVICE;
			//data = new Intent(this, DeviceListActivity.class);
			//connectDevice(data, true);
			//btAddress = myPreference.getString("btAddress","");  
			btDevice = mBluetoothAdapter.getRemoteDevice(btAddress);
			mChatService.connect(btDevice, btSecure);
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		autoUpdate = new Timer();
		autoUpdate.schedule(new TimerTask() {
			@Override
			public void run() {
				runOnUiThread(new Runnable() {
					public void run() {
						//if(readStringValues[0].length()==4)	tvTemp.setText(String.valueOf(readStringValues[0]).trim() + " °C");
						if(readStringValues[2].length()==4) tvHumi.setText(String.valueOf(readStringValues[2]).trim() + " %");
						setPoint();
						moveDetect();
						smokeDetect();
						
						//if (commandArduino(cv_server_url)) CVSERVER = true;
						//else CVSERVER = false;
						//if ( (System.currentTimeMillis() >= noMoveTimer+cvOnTimer) && CVSTATE==true) cvOff(); 
					}
				});
			}
		}, 0, 1000); // updates each 1 secs
	}

	//@Override
	public void onPause() {
	super.onPause();
		Toast.makeText(this, " BT Pause Service Started", Toast.LENGTH_LONG).show();
		
		//Test Autoconnect last bonded device
		//Intent intent = getIntent();
	    //lastConnectedDevice = intent.getStringExtra(DeviceListActivity.partnerDevAdd1);
		//mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
		//btDevice = mBluetoothAdapter.getRemoteDevice(lastConnectedDevice); //direct verbinden met 
		
	    //btDevice = mBluetoothAdapter.getRemoteDevice(partnerDevAdd); //direct verbinden met
	    btDevice = mBluetoothAdapter.getRemoteDevice(btAddress); //direct verbinden met
	    mChatService.connect(btDevice, true);
	    
	    
	//	Toast.makeText(this, "Pause: BT Service Destroyed", Toast.LENGTH_LONG).show();
	//	startService(new Intent(this, BluetoothChatService.class)); //keep BT connection
	//	commandArduino(cv_server_url + "/?cmd=ACTval"); //test of er een server is
	}

	@Override
	public void onStop() {
		super.onStop();
		//stopService(new Intent(this, BluetoothChatService.class)); //stop BT connection
		Toast.makeText(this, " BT Stop Service Started", Toast.LENGTH_LONG).show();
		btDevice = mBluetoothAdapter.getRemoteDevice(btAddress); //direct verbinden met 
		mChatService.connect(btDevice, true);
		//Toast.makeText(this, "Stop: BT Service Destroyed", Toast.LENGTH_LONG).show();
		//autoUpdate.cancel();
		//finish();
		//System.exit(0);
		if (VERBOSE) Log.v(TAG, "-- ON STOP --");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		//stopService(new Intent(this, BluetoothChatService.class)); //stop BT connection
		Toast.makeText(this, "Destroy: BT Service Destroyed", Toast.LENGTH_LONG).show();
		//mChatService.connect(btDevice, true); //BLE
		autoUpdate.cancel();
		finish();
		System.exit(0);
		if (VERBOSE) Log.v(TAG, "- ON DESTROY -");
	}
	

	private final void setStatus(int resId) {
		final ActionBar actionBar = getActionBar();
		actionBar.setSubtitle(resId);
	}

	private final void setStatus(CharSequence subTitle) {
		final ActionBar actionBar = getActionBar();
		actionBar.setSubtitle(subTitle);
	}
	
	private void setupChat() {
		Log.d(TAG, "setupChat()");
		
		// Initialize the array adapter for the conversation thread (JCP verwijderd nodig voor msgWindow
			mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
			//mConversationView = (ListView) findViewById(R.id.in);
			//mConversationView.setAdapter(mConversationArrayAdapter);
		
		// Initialize the BluetoothChatService to perform bluetooth connections
		mChatService = new BluetoothChatService(this, mHandler);
		//Log.d("BT Handler SETUP ", "" +  mChatService.BTmsgHandler);
		// Initialize the buffer for outgoing messages
			//mOutStringBuffer = new StringBuffer(""); //jcp verwijderd alleen nodig als je ifomatie verstuurd via BT
		//startService(new Intent(this, BluetoothChatService.class)); //keep BT connection
	}	

	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d("Terminal", "onActivityResult...");
		switch (requestCode) {
			case REQUEST_CONNECT_DEVICE_SECURE:
				// When DeviceListActivity returns with a device to connect
				if (resultCode == Activity.RESULT_OK)connectDevice(data, true);
				break;
			case REQUEST_CONNECT_DEVICE_INSECURE:
				// When DeviceListActivity returns with a device to connect
				if (resultCode == Activity.RESULT_OK)connectDevice(data, false);
				break;
			case REQUEST_ENABLE_BT:
				// When the request to enable Bluetooth returns
				if (resultCode == Activity.RESULT_OK) {
					// Bluetooth is now enabled, so set up a chat session
					setupChat();
				} else {
					// User did not enable Bluetooth or an error occurred
					Log.d(TAG, "BT not enabled");
					Toast.makeText(this, "BT NOT ENABLED", Toast.LENGTH_SHORT).show();
					finish();
				}
		}
	}  
	
	private void connectDevice(Intent data, boolean btSecure) {
		// Get the device MAC address
		btAddress = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
		// Get the BluetoothDevice object
		BluetoothDevice btDevice = mBluetoothAdapter.getRemoteDevice(btAddress);
			// Attempt to connect to the device
		mChatService.connect(btDevice, btSecure);
	}
	
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {

			switch (msg.what) {
				case MESSAGE_STATE_CHANGE:
					
					switch (msg.arg1) {
						case BluetoothChatService.STATE_CONNECTED:
							setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
							mConversationArrayAdapter.clear();
							//onConnect();
							break;
						case BluetoothChatService.STATE_CONNECTING:
							setStatus(R.string.title_connecting);
							break;
						case BluetoothChatService.STATE_LISTEN:
						case BluetoothChatService.STATE_NONE:
							setStatus(R.string.title_not_connected);
							break;
					}
					break;
				case MESSAGE_WRITE:
					break;
				case MESSAGE_READ:
					btState = 1;
					byte[] readBuf = (byte[]) msg.obj;
					// stop de bytes in een buffer 
					//String readMessage = new String(readBuf);
					String readMessage = new String(readBuf, 0, msg.arg1);
					//Tussen variable toepassen i.v.m. outoffbounds stringbuffer 
					//Toast.makeText(getApplicationContext(), readMessage,Toast.LENGTH_SHORT).show();
					//if (readMessage.length() > 1) 
					//readStringValuesTest = readMessage.split("[\\|\\#]");
					readStringValuesTest = readMessage.trim().split(";");
					for (int x = 0; x < readStringValuesTest.length; x++) {
						  readStringValues[x] = readStringValuesTest[x];
						  //Toast.makeText(getApplicationContext(), x + ": " + readStringValues[x],Toast.LENGTH_SHORT).show();
					 }
					readMessage = "";
					readBuf = null;
					break;
				case MESSAGE_DEVICE_NAME:
					// save the connected device's name
					mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
					Toast.makeText(getApplicationContext(), "Connected to "
							+ mConnectedDeviceName, Toast.LENGTH_SHORT).show();
					break;
				case MESSAGE_TOAST:
					Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
							Toast.LENGTH_SHORT).show();
					break;
				case MESSAGE_ERROR:
				    break;
				case MESSAGE_LOST:
				//	    if(Constant.DEBUG)  Log.d(TAG, "Connection lost");
				//	    stopBTService(); //stop the Bluetooth services
				//	    Thread.sleep(500);
				//	       setupBluetoothService(); //re initialize the bluetoothconnection
					    break;    
			}
		}
	};		  

	public void onStartTrackingTouch(SeekBar seekBar) {
	}
	
	//Functie voor dtectie van Beweging test of het altijd maar 1 positie is.
	public void moveDetect() {
		if (readStringValues[3].length()== 1)
		{
			//Toast.makeText(getApplicationContext(),readStringValues[2],Toast.LENGTH_SHORT).show();
			
			if ( Integer.valueOf(readStringValues[3])== 0) {
				 btnKamer2.setBackgroundColor(Color.WHITE);
				 btnKamer2.setText("NO MOVE");
				 if (MOVE == true)
				  {	 
				   noMoveTimer = System.currentTimeMillis();
				   MOVE = false;
				  } 
				 //Toast.makeText(getApplicationContext(), MOVE + ": " + String.valueOf(noMoveTimer),Toast.LENGTH_SHORT).show();
			}
			if ( Integer.valueOf(readStringValues[3])== 1) {
				btnKamer2.setBackgroundColor(Color.RED);
				btnKamer2.setText("MOVE");
				btnPlus.setBackgroundColor(Color.WHITE);
				//if (MOVE == false) 
				// { 	
				  noMoveTimer = 0;
				  MOVE = true;
				// } 
				//Toast.makeText(getApplicationContext(), MOVE + ": " + String.valueOf(noMoveTimer),Toast.LENGTH_SHORT).show();
			}
		}
		//if ( MOVE == false && (System.currentTimeMillis() > noMoveTimer + noMoveDelay)) cvOffNoMove(); //test straks CVoff
	}
	
	//Functie voor detectie van Rook test of het altijd maar 1 positie is.
	public void smokeDetect() {
		if (readStringValues[4].length()==1)
		{
			if ( Integer.valueOf(readStringValues[4])== 0) {
				 btnKamer3.setBackgroundColor(Color.WHITE);
				 btnKamer3.setText("No Smoke");
			}
			if ( Integer.valueOf(readStringValues[4])== 1) {
				btnKamer3.setBackgroundColor(Color.RED);
				btnKamer3.setText("Smoke Detected");
				}
		}
	}
	
	
	public void addListenerOnButtonMin() {
		btnMin = (Button) findViewById(R.id.button_min);
		btnMin.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				userSetpoint -= 0.5f;
				if(userSetpoint <= 0) userSetpoint = 0.0f;
				spTemp.setText(String.valueOf(userSetpoint));
			}
		});	
	}
	
	public void addListenerOnButtonPlus() {
		btnPlus = (Button) findViewById(R.id.button_plus);
		btnPlus.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				userSetpoint += 0.5f;
				if(userSetpoint >= 30) userSetpoint = 30.0f;
				spTemp.setText(String.valueOf(userSetpoint));
			}
		});	
	}
	

	/*
	//Functie voor progressbar om setpoint in te stellen
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		userSetpoint = (float)(progress * 0.5f); // met 0.5 graad verhogen
		//spTemp.setText(String.valueOf(userSetpoint+0.5));
		spTemp.setText(String.valueOf(userSetpoint));
		//if(readStringValues[0].length()>0) Temperature = (float) (Float.valueOf(readStringValues[0]));
		//else Temperature = 11;
	}
	*/
	
	//Functie om te testen op setpoint groter/kleiner is dat actuele temp.
	public void setPoint() {
		//try {
		 //Toast.makeText(getApplicationContext(),String.valueOf(Temperature),Toast.LENGTH_SHORT).show();
		// Toast.makeText(getApplicationContext(),readStringValues[0],Toast.LENGTH_SHORT).show();
		//Toast.makeText(getApplicationContext(), String.valueOf(CVSTATE),Toast.LENGTH_SHORT).show();
		//if(readStringValues[0].length()>0) Temperature = (float) (Float.valueOf(readStringValues[0]));
		//else  Temperature = 11;
		
		if(readStringValues[1].length() == 4)	
		 {	
			tvTemp.setText(String.valueOf(readStringValues[1]).trim() + " °C");
			Temperature = (float) (Float.valueOf(readStringValues[1]));
			//Toast.makeText(getApplicationContext(),String.valueOf(userSetpoint)+ "..." + String.valueOf(Temperature),Toast.LENGTH_SHORT).show();
		
			if ( (spStartTeller > 4) && (SPSTATE == false) )
			{	
			  userSetpoint = Math.round(Temperature)- 0.5f;
			  spTemp.setText(String.valueOf(userSetpoint));
			  //oldTemperature = Temperature;
			  SPSTATE = true;
			  //Toast.makeText(getApplicationContext(),String.valueOf(spStartTeller)+ SPSTATE,Toast.LENGTH_SHORT).show();
			 }
	      if(spStartTeller < 6) spStartTeller++;
	     }
		else
		 { 	
		  Temperature = 11;
		  //userSetpoint = 10;
		 }  
		
		/*
		if(SPSTATE)
		 {
		  float divTemperature = oldTemperature - Temperature;
		  if (divTemperature < 0.0f) divTemperature *= -1.0f;
		  if(divTemperature < 0.5f) Temperature = oldTemperature;
		 }
		*/
		
		try {
		if ( ((float) userSetpoint > (float) Temperature) && CVSTATE == false)
		//if ((float) userSetpoint > (float) Temperature)	 
		  {
			cvOn();
			//float divTemperature = oldTemperature - Temperature;
			//if (divTemperature < 0.0f) divTemperature *= -1.0f;
			//if(divTemperature >= 0.5f) cvOn();
			//oldTemperature = Temperature;
			//else tvTemp.setText("No Server!!");
		   } 
		  //if ((float) userSetpoint <= Temperature && CVSTATE == true)
		//if ((float) userSetpoint <= (float) Temperature) cvOff();
		  if ( ((float) userSetpoint <= (float) Temperature) && CVSTATE == true) cvOff();	
		} catch (Exception e){Log.d(TAG, "setPoint error"); e.printStackTrace();}; 
		
	}
	
	//Functie CV Inschakelen
	public void cvOn() {
	 spTemp.setTextColor(Color.RED);
	 spTemp.setText(String.valueOf(userSetpoint));
	 btnKamer1.setBackgroundColor(Color.RED);
	 btnPlus.setBackgroundColor(Color.RED);
	 btnMin.setBackgroundColor(Color.WHITE);
	 //seekBar.setBackgroundColor(Color.RED);
	 //seekBar.getProgressDrawable().setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
	 btnKamer1.setText("CV ON");
	 if (CVSTATE == false)
	  {	
	   mMediaPlayer.start();
	   commandArduino(cv_server_url + "/?cmd=CVaan");/*tvTemp.setText("Server Oke!!");*/
	  } 
	 CVSTATE = true;
	}
	
	//Functie CV Uitschakelen
	public void cvOff(){
	 spTemp.setTextColor(Color.WHITE);
	 spTemp.setText(String.valueOf(userSetpoint));
	 btnKamer1.setBackgroundColor(Color.WHITE);
	 btnMin.setBackgroundColor(Color.WHITE);
	 btnPlus.setBackgroundColor(Color.WHITE);
	 //seekBar.setBackgroundColor(Color.WHITE);
	 //seekBar.getProgressDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);
	 btnKamer1.setText("CV OFF");
	 if(CVSTATE == true) commandArduino(cv_server_url + "/?cmd=CVuit");
	 CVSTATE = false;
	 //else tvTemp.setText("No Server!!");
	}
	
	//Functie CV Uitschakelen
	public void cvOffNoMove(){
	 //btnKamer1.setBackgroundColor(Color.WHITE);
	 //btnMin.setBackgroundColor(Color.BLUE);
	 btnPlus.setBackgroundColor(Color.BLUE);
	 //seekBar.setBackgroundColor(Color.WHITE);
	 //seekBar.getProgressDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);
	 btnKamer1.setText("CV OFF");
	 if(CVSTATE == true) commandArduino(cv_server_url + "/?cmd=CVuit");
	 CVSTATE = false;
	 userSetpoint = 10;
	 spTemp.setTextColor(Color.WHITE);
	 spTemp.setText(String.valueOf(userSetpoint));
	 
	 //else tvTemp.setText("No Server!!");
	}
	 
	
	
	//Functie om te communieren met arduino serv er die aan CV hangt
	public boolean commandArduino(String url) {
		try {
			HttpClient httpclient = new DefaultHttpClient();
			httpclient.execute(new HttpGet(url)); // was
			CVSERVER = true;
			Toast.makeText(getApplicationContext(), "CV OK", Toast.LENGTH_SHORT).show();
			return true;
		 } catch (Exception e) {
			Toast.makeText(getApplicationContext(), "No Server available", Toast.LENGTH_SHORT).show();
			CVSERVER = false;
			return false;
		 }
	}
	

/* T.g.v. Seekbar	
	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
	}
	
	
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
	}
*/	
	
}//Mainactivity End