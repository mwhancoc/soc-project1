/*****************************************************************
JADE - Java Agent DEvelopment Framework is a framework to develop 
multi-agent systems in compliance with the FIPA specifications.
Copyright (C) 2000 CSELT S.p.A. 

GNU Lesser General Public License

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation, 
version 2.1 of the License. 

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the
Free Software Foundation, Inc., 59 Temple Place - Suite 330,
Boston, MA  02111-1307, USA.
 *****************************************************************/

package chat.client.gui;

import java.util.logging.Level;

import jade.core.MicroRuntime;
import jade.util.Logger;
import jade.wrapper.ControllerException;
import jade.wrapper.O2AException;
import jade.wrapper.StaleProxyException;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import chat.client.agent.ChatClientInterface;


/**
 * This activity implement the chat interface.
 * Testing GitHub
 * 
 * Mike test complete, Test 2 of Commit and Push upstream, Test 3 of Commit and Push upstream from Andrew
 * Test 4 - Andrew
 * 
 * Test comment 2/2/2015, Test two after merge fix
 * 
 * @author Michele Izzo - Telecomitalia
 */

public class ChatActivity extends Activity {
	private Logger logger = Logger.getJADELogger(this.getClass().getName());

	static final int PARTICIPANTS_REQUEST = 0;

	private MyReceiver myReceiver;

	private String nickname;
	private ChatClientInterface chatClientInterface;
	
	private String latLongLocation;
	
	private Location ebIICentennialNCSULoc;
	private Location washingtonDCLoc;
	private Location parisLoc;
	
	private LocationManager locationManager;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			nickname = extras.getString("nickname");
		}

		try {
			chatClientInterface = MicroRuntime.getAgent(nickname)
					.getO2AInterface(ChatClientInterface.class);
		} catch (StaleProxyException e) {
			showAlertDialog(getString(R.string.msg_interface_exc), true);
		} catch (ControllerException e) {
			showAlertDialog(getString(R.string.msg_controller_exc), true);
		}

		myReceiver = new MyReceiver();

		IntentFilter refreshChatFilter = new IntentFilter();
		refreshChatFilter.addAction("jade.demo.chat.REFRESH_CHAT");
		registerReceiver(myReceiver, refreshChatFilter);

		IntentFilter clearChatFilter = new IntentFilter();
		clearChatFilter.addAction("jade.demo.chat.CLEAR_CHAT");
		registerReceiver(myReceiver, clearChatFilter);

		setContentView(R.layout.chat);

		Button button = (Button) findViewById(R.id.button_send);
		button.setOnClickListener(buttonSendListener);
		
		// integrate location information with the chat application
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

	    Criteria criteria = new Criteria();
	    criteria.setAccuracy(Criteria.ACCURACY_FINE);
	    criteria.setAltitudeRequired(false);
	    criteria.setBearingRequired(false);
	    criteria.setCostAllowed(true);
	    criteria.setPowerRequirement(Criteria.POWER_LOW);

	    String provider = locationManager.getBestProvider(criteria, true);

	    /*
	     * You should add a LocationListener to request location updates.
	     */
	    this.ebIICentennialNCSULoc = new Location(LocationManager.GPS_PROVIDER);
	    this.ebIICentennialNCSULoc.setLatitude(35.77198);
	    this.ebIICentennialNCSULoc.setLongitude(-78.67385);
	    
	    this.washingtonDCLoc = new Location(LocationManager.GPS_PROVIDER);
	    this.washingtonDCLoc.setLatitude(38.90719);
	    this.washingtonDCLoc.setLongitude(-77.03687);
	    
	    this.parisLoc = new Location(LocationManager.GPS_PROVIDER);
	    this.parisLoc.setLatitude(48.85661);
	    this.parisLoc.setLongitude(2.35222);
	    
	    // Define a listener that responds to location updates
	    LocationListener locationListener = new LocationListener() {
	        public void onLocationChanged(Location location) {
	          // Called when a new location is found by the network location provider.
	          //makeUseOfNewLocation(location);
	        	updateWithNewLocation(location);
	        }

	        public void onStatusChanged(String provider, int status, Bundle extras) {}

	        public void onProviderEnabled(String provider) {}

	        public void onProviderDisabled(String provider) {}
	      };

	    // Register the listener with the Location Manager to receive location updates
	    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
	    
	    Location location = locationManager.getLastKnownLocation(provider);

	    if (location != null)
	      updateWithNewLocation(location);

	  
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		unregisterReceiver(myReceiver);

		logger.log(Level.INFO, "Destroy activity!");
	}

	private OnClickListener buttonSendListener = new OnClickListener() {
		public void onClick(View v) {
			final EditText messageField = (EditText) findViewById(R.id.edit_message);
			String message = messageField.getText().toString();
			if (message != null && !message.equals("")) {
				try {
					chatClientInterface.handleSpoken(message + latLongLocation);
					messageField.setText("");
				} catch (O2AException e) {
					showAlertDialog(e.getMessage(), false);
				}
			}

		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.chat_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_participants:
			Intent showParticipants = new Intent(ChatActivity.this,
					ParticipantsActivity.class);
			showParticipants.putExtra("nickname", nickname);
			startActivityForResult(showParticipants, PARTICIPANTS_REQUEST);
			return true;
		case R.id.menu_clear:
			/*
			Intent broadcast = new Intent();
			broadcast.setAction("jade.demo.chat.CLEAR_CHAT");
			logger.info("Sending broadcast " + broadcast.getAction());
			sendBroadcast(broadcast);
			*/
			final TextView chatField = (TextView) findViewById(R.id.chatTextView);
			chatField.setText("");
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == PARTICIPANTS_REQUEST) {
			if (resultCode == RESULT_OK) {
				// TODO: A partecipant was picked. Send a private message.
			}
		}
	}

	private class MyReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			logger.log(Level.INFO, "Received intent " + action);
			if (action.equalsIgnoreCase("jade.demo.chat.REFRESH_CHAT")) {
				final TextView chatField = (TextView) findViewById(R.id.chatTextView);
				chatField.append(intent.getExtras().getString("sentence"));
				scrollDown();
			}
			if (action.equalsIgnoreCase("jade.demo.chat.CLEAR_CHAT")) {
				final TextView chatField = (TextView) findViewById(R.id.chatTextView);
				chatField.setText("");
			}
		}
	}

	private void scrollDown() {
		final ScrollView scroller = (ScrollView) findViewById(R.id.scroller);
		final TextView chatField = (TextView) findViewById(R.id.chatTextView);
		scroller.smoothScrollTo(0, chatField.getBottom());
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		final TextView chatField = (TextView) findViewById(R.id.chatTextView);
		savedInstanceState.putString("chatField", chatField.getText()
				.toString());
		super.onSaveInstanceState(savedInstanceState);
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		final TextView chatField = (TextView) findViewById(R.id.chatTextView);
		chatField.setText(savedInstanceState.getString("chatField"));
	}

	private void showAlertDialog(String message, final boolean fatal) {
		AlertDialog.Builder builder = new AlertDialog.Builder(
				ChatActivity.this);
		builder.setMessage(message)
				.setCancelable(false)
				.setPositiveButton("Ok",
						new DialogInterface.OnClickListener() {
							public void onClick(
									DialogInterface dialog, int id) {
								dialog.cancel();
								if(fatal) finish();
							}
						});
		AlertDialog alert = builder.create();
		alert.show();		
	}
	
	 private void updateWithNewLocation(Location location) {
		    //TextView myLocationText = (TextView) findViewById(R.id.myLocationText);

		    String latLongString;

		    if (location != null) {
		      double lat = location.getLatitude();
		      double lng = location.getLongitude();
		      
		      latLongString = "\n  @Latitude: " + lat + "\n      Longitude: " + lng;
		      
		      /**
		       * check pre-identified locations
		       */
			  double fiftyMeters = 50.0;
			  double distance;
			  
		      //if ebII < 50meters away set Chat Participants Latitude and Longitude to ebII's location
		      double ebIILat = this.ebIICentennialNCSULoc.getLatitude();
		      double ebIILng = this.ebIICentennialNCSULoc.getLongitude();
		      distance = getDistance(lat, lng, ebIILat, ebIILng);
		      if(distance < fiftyMeters)
		      {
		    	  latLongString = "\n  @Latitude: " + ebIILat + "\n      Longitude: " + ebIILng
		    			  			+ "\n This is EBII(Centennial)";
		      }
		    	  
		      //if DC
		      double dcLat = this.washingtonDCLoc.getLatitude();
		      double dcLng = this.washingtonDCLoc.getLongitude();
		      distance = getDistance(lat, lng, dcLat, dcLng);
		      if(distance < fiftyMeters)
		      {
		    	  latLongString = "\n  @Latitude: " + dcLat + "\n      Longitude: " + dcLng
		    			  			+ "\n This is Washington DC";
		      }
		      
		      //if Paris
		      double parisLat = this.parisLoc.getLatitude();
		      double parisLng = this.parisLoc.getLongitude();
		      distance = getDistance(lat, lng, parisLat, parisLng);
		      if(distance < fiftyMeters)
		      {
		    	  latLongString = "\n  @Latitude: " + parisLat + "\n      Longitude: " + parisLng
		    			  			+ "\n This is Paris";
		      }
		    		      
		      
		    } else {
		      latLongString = "No location found";
		    }
		    
		    //myLocationText.setText("Your Current Position is:\n" + latLongString);
		    this.latLongLocation = latLongString;
		  }
	 
	 private static double getDistance(double lat1, double lon1, double lat2, double lon2) {
		 final double Radius = 6371 * 1E3; //Earth's mean radius
		 double dLat = Math.toRadians(lat2-lat1);
		 double dLon = Math.toRadians(lon2-lon1);
		 
		 double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
		     	    Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
		 			Math.sin(dLon/2) * Math.sin(dLon/2);
		 
		 double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
		 
		 return Radius * c;
	}

}