package com.roomcontrol;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

public class BLEService extends Service {
	public BLEService() {
		//mChatService.connect(btDevice, true);
		}

		@Override
		public IBinder onBind(Intent intent) {
		    return null;
		}

		 @Override
		     public void onCreate() {
		         Toast.makeText(this, "The new Service was Created: onCreate", Toast.LENGTH_LONG).show();
		     }
	
		 @Override
		     public void onStart(Intent intent, int startId) {
		         Toast.makeText(this, " Service Started: onStart", Toast.LENGTH_LONG).show();
		     }
		 
			 
		 @Override
		     public void onDestroy() {
		         Toast.makeText(this, "Service Destroyed: onDestroy", Toast.LENGTH_LONG).show();
		     }
	}	