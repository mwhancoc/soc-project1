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

import jade.core.MicroRuntime;
import jade.util.Logger;
//import jade.util.leap.ArrayList;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;

import java.util.logging.Level;
import java.util.*;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import chat.client.agent.ChatClientInterface;

/**
 * This activity implement the participants interface.
 * 
 * @author Michele Izzo - Telecomitalia
 */

public class ParticipantsActivity extends ListActivity {
	private Logger logger = Logger.getJADELogger(this.getClass().getName());

	private MyReceiver myReceiver;

	private String nickname;
	private ChatClientInterface chatClientInterface;

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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ControllerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		myReceiver = new MyReceiver();

		IntentFilter refreshParticipantsFilter = new IntentFilter();
		refreshParticipantsFilter
				.addAction("jade.demo.chat.REFRESH_PARTICIPANTS");
		registerReceiver(myReceiver, refreshParticipantsFilter);

		setContentView(R.layout.participants);

		setListAdapter(new ArrayAdapter<String>(this, R.layout.participant,
				//chatClientInterface.getParticipantNames()));
				getModifiedParticipantNames(getContacts(), chatClientInterface.getParticipantNames())));

		ListView listView = getListView();
		listView.setTextFilterEnabled(true);
		listView.setOnItemClickListener(listViewtListener);
	}

	private OnItemClickListener listViewtListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			
			// Split the participant string, original form "Tim [N]" or "Time [Y]"
			String[] chatParticipant = ((TextView)view).getText().toString().split(" ");
			
			// retrieve [Y] or [N] to determine if an Android Contact
			String isContact = chatParticipant[1];
			
			if(isContact.contains("N"))
			{
				String name = chatParticipant[0];

				// Add code to check if contact already exists before writing to contacts

				ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

				int rawContactInsertIndex = ops.size();

				ops.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
				.withValue(RawContacts.ACCOUNT_TYPE, null)
				.withValue(RawContacts.ACCOUNT_NAME, null).build());

				ops.add(ContentProviderOperation
				.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID,rawContactInsertIndex)
				.withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
				.withValue(StructuredName.DISPLAY_NAME, name) // Name of the person
				.build());

				try
				{
					ContentProviderResult[] res = getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
				}
				catch (RemoteException e)
				{
					// error
				}
				catch (OperationApplicationException e)
				{
					// error
				}
			}
			// TODO: A partecipant was picked. Send a private message.
			finish();
		}
	};

	@Override
	protected void onDestroy() {
		super.onDestroy();

		unregisterReceiver(myReceiver);

		logger.log(Level.INFO, "Destroy activity!");
	}
	
	/**
	 * Gets and returns the name of all contacts saved on the device
	 * 
	 * @return the string array of contact names
	 */
	protected String[] getContacts() {
		Cursor myContacts = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
		String contactsList[] = new String[myContacts.getCount()];
		
		int i = 0;
	
		int nameIndex = myContacts.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);	

		while(myContacts.moveToNext()) {

	    		String name = myContacts.getString(nameIndex);
	    		contactsList[i++] = name;     		
		}

		myContacts.close();
		
		return contactsList;
	}
	
	/**
	 * Compares the list of participants in the chat to the device's contact list
	 * Appends [Y] to the participant name if the participant is saved as a contact,
	 *   otherwise appends [N]
	 * 
	 * @param contacts the array of contact names saved in the device
	 * @param participants the array of participants currently in the chat
	 * @return the modified participant name appended by [Y] or [N]
	 */
	protected String[] getModifiedParticipantNames(String[] contacts, String[] participants) {
		int i = 0;
		String[] modified = new String[participants.length];
		for(String participant : participants) {
			String inContacts = " [N]";
			for(String contact : contacts) {
				if(participant.equals(contact)) {
					inContacts = " [Y]";
					break;
				}							
			}
			modified[i++] = participant + inContacts;
		}
		
		return modified;
	}

	private class MyReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			logger.log(Level.INFO, "Received intent " + action);
			if (action.equalsIgnoreCase("jade.demo.chat.REFRESH_PARTICIPANTS")) {
				setListAdapter(new ArrayAdapter<String>(
						ParticipantsActivity.this, R.layout.participant,
						//chatClientInterface.getParticipantNames()));
						getModifiedParticipantNames(getContacts(), chatClientInterface.getParticipantNames())));						
			}
		}
	}

}
