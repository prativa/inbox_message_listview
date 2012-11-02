package com.example.inboxlistproject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ListView;

public class MainActivity extends Activity {
	private static final int TYPE_INCOMING_MESSAGE = 1;
	private ListView messageList;
	private MessageListAdapter messageListAdapter;
	private ArrayList<Message> recordsStored;
	private ArrayList<Message> listInboxMessages;
	private ProgressDialog progressDialogInbox;
	private CustomHandler customHandler;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initViews();

	}

	@Override
	public void onResume() {
		super.onResume();
		populateMessageList();
	}

	private void initViews() {
		customHandler = new CustomHandler(this);
		progressDialogInbox = new ProgressDialog(this);

		recordsStored = new ArrayList<Message>();

		messageList = (ListView) findViewById(R.id.messageList);
		populateMessageList();
	}

	public void populateMessageList() {
		fetchInboxMessages();

		messageListAdapter = new MessageListAdapter(this,
				R.layout.message_list_item, recordsStored);
		messageList.setAdapter(messageListAdapter);
	}

	private void showProgressDialog(String message) {
		progressDialogInbox.setMessage(message);
		progressDialogInbox.setIndeterminate(true);
		progressDialogInbox.setCancelable(true);
		progressDialogInbox.show();
	}

	private void fetchInboxMessages() {
		if (listInboxMessages == null) {
			showProgressDialog("Fetching Inbox Messages...");
			startThread();
		} else {
			// messageType = TYPE_INCOMING_MESSAGE;
			recordsStored = listInboxMessages;
			messageListAdapter.setArrayList(recordsStored);
		}
	}

	public class FetchMessageThread extends Thread {

		public int tag = -1;

		public FetchMessageThread(int tag) {
			this.tag = tag;
		}

		@Override
		public void run() {

			recordsStored = fetchInboxSms(TYPE_INCOMING_MESSAGE);
			listInboxMessages = recordsStored;
			customHandler.sendEmptyMessage(0);

		}

	}

	public ArrayList<Message> fetchInboxSms(int type) {
		ArrayList<Message> smsInbox = new ArrayList<Message>();

		Uri uriSms = Uri.parse("content://sms");
	
		Cursor cursor = this.getContentResolver()
				.query(uriSms,
						new String[] { "_id", "address", "date", "body",
								"type", "read" }, "type=" + type, null,
						"date" + " COLLATE LOCALIZED ASC");
		if (cursor != null) {
			cursor.moveToLast();
			if (cursor.getCount() > 0) {

				do {

					Message message = new Message();
					message.messageNumber = cursor.getString(cursor
							.getColumnIndex("address"));
					message.messageContent = cursor.getString(cursor
							.getColumnIndex("body"));
					smsInbox.add(message);
				} while (cursor.moveToPrevious());
			}
		}

		return smsInbox;

	}

	private FetchMessageThread fetchMessageThread;

	private int currentCount = 0;

	public synchronized void startThread() {

		if (fetchMessageThread == null) {
			fetchMessageThread = new FetchMessageThread(currentCount);
			fetchMessageThread.start();
		}
	}

	public synchronized void stopThread() {
		if (fetchMessageThread != null) {
			Log.i("Cancel thread", "stop thread");
			FetchMessageThread moribund = fetchMessageThread;
			currentCount = fetchMessageThread.tag == 0 ? 1 : 0;
			fetchMessageThread = null;
			moribund.interrupt();
		}
	}

	static class CustomHandler extends Handler {
		private final WeakReference<MainActivity> activityHolder;

		CustomHandler(MainActivity inboxListActivity) {
			activityHolder = new WeakReference<MainActivity>(inboxListActivity);
		}

		@Override
		public void handleMessage(android.os.Message msg) {

			MainActivity inboxListActivity = activityHolder.get();
			if (inboxListActivity.fetchMessageThread != null
					&& inboxListActivity.currentCount == inboxListActivity.fetchMessageThread.tag) {
				Log.i("received result", "received result");
				inboxListActivity.fetchMessageThread = null;
				
				inboxListActivity.messageListAdapter
						.setArrayList(inboxListActivity.recordsStored);
				inboxListActivity.progressDialogInbox.dismiss();
			}
		}
	}

	private OnCancelListener dialogCancelListener = new OnCancelListener() {

		@Override
		public void onCancel(DialogInterface dialog) {
			stopThread();
		}

	};

}
