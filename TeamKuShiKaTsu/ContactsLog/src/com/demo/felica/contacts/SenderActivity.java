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
import android.widget.Toast;

public class SenderActivity extends Activity implements OnClickListener {

	private static final String TAG = "FEL";
	private static final int FELICA_ACTIVITY = 0;
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
			mSendButton.setEnabled(false);
			final Intent realIntent = new Intent(Consts.RECEIVE_ACTION);
			realIntent.putExtra(Consts.EXTRA_NAME, mNameEdit.getEditableText()
					.toString());
			realIntent.putExtra(Consts.EXTRA_PHONE, mPhoneNumber);
			final Intent felicaIntent = new Intent("jp.andeb.kushikatsu.FELICA_INTENT");
			felicaIntent.addCategory(Intent.CATEGORY_DEFAULT);
			felicaIntent.putExtra("EXTRA_INTENT", realIntent);
			startActivityForResult(felicaIntent, FELICA_ACTIVITY);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode != FELICA_ACTIVITY) {
			return;
		}
		final Toast toast;
		if (resultCode == RESULT_OK) {
			toast = Toast.makeText(this, "FeliCa Push 送信成功。",
					Toast.LENGTH_SHORT);
		} else if (resultCode == RESULT_CANCELED) {
			toast = Toast.makeText(this, "送信をキャンセルしました。",
					Toast.LENGTH_SHORT);
		} else if (resultCode == 1) {
			toast = Toast.makeText(this, "FeliCa Push 送信に失敗しました。",
					Toast.LENGTH_SHORT);
		} else if (resultCode == 2) {
			toast = Toast.makeText(this, "リクエストのパラメータが不正です。",
					Toast.LENGTH_SHORT);
		} else if (resultCode == 3) {
			toast = Toast.makeText(this, "FeliCa デバイスがみつかりません。",
					Toast.LENGTH_SHORT);
		} else if (resultCode == 4) {
			toast = Toast.makeText(this, "FeliCa デバイスは使用中です。",
					Toast.LENGTH_SHORT);
		} else if (resultCode == 5) {
			toast = Toast.makeText(this, "データが大きすぎます。",
					Toast.LENGTH_SHORT);
		} else if (resultCode == 6) {
			toast = Toast.makeText(this, "送信がタイムアウトしました。",
					Toast.LENGTH_SHORT);
		} else {
			toast = Toast.makeText(this, "不明なエラーです: " + resultCode,
					Toast.LENGTH_SHORT);
		}
		toast.show();
		mSendButton.setEnabled(true);
	}
}
