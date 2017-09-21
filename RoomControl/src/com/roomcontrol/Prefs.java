package com.roomcontrol;


import android.os.Bundle;
import android.preference.PreferenceActivity;
//import com.cowsapp.R;

public class Prefs extends PreferenceActivity {
@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
	}
  }	
