package com.demo.felica.contacts;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class SenderActivity extends Activity implements OnClickListener {

	private static final String TAG = "FEL";
	private String mPhoneNumber;
	private EditText mNameEdit;
	private Button mSendButton;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sender);

		mNameEdit = (EditText) findViewById(R.id.ed_name);

		final TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		mPhoneNumber = tm.getLine1Number();

		final TextView numberText = (TextView) findViewById(R.id.txt_number);
		numberText.setText(mPhoneNumber);

		mSendButton = (Button) findViewById(R.id.btn_send);
		mSendButton.setOnClickListener(this);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	public void onClick(final View v) {
		if (v == mSendButton) {
			Log.d(TAG, "CLICKED");
			final Intent realIntent = new Intent(Consts.RECEIVE_ACTION);
			realIntent.putExtra(Consts.EXTRA_NAME, mNameEdit.getEditableText()
					.toString());
			realIntent.putExtra(Consts.EXTRA_PHONE, mPhoneNumber);
			final Intent felicaIntent = new Intent("jp.andeb.kushikatsu.FELICA_INTENT");
			felicaIntent.addCategory(Intent.CATEGORY_DEFAULT);
			felicaIntent.putExtra("EXTRA_INTENT", realIntent);
			felicaIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
			startActivity(felicaIntent);
		}
	}
}
