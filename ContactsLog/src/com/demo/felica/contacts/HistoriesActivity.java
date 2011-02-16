package com.demo.felica.contacts;

import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class HistoriesActivity extends Activity implements OnItemClickListener,
		OnClickListener {

	private ListView mHistoriesList;
	private Button mSendButton;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.histories);

		mSendButton = (Button) findViewById(R.id.btn_send);
		mSendButton.setOnClickListener(this);

		mHistoriesList = (ListView) findViewById(R.id.lst_histories);
		mHistoriesList.setOnItemClickListener(this);

		final SQLiteDatabase db = HistoryDataBase.getDb(this);
		final Cursor cursor = db.query(HistoryDataBase.TBL_NAME, new String[] {
				HistoryDataBase.TOUCHED_AT, HistoryDataBase.TOUCHED_AT,
				HistoryDataBase.NAME, HistoryDataBase.PHONE,
				HistoryDataBase.LAT, HistoryDataBase.LON }, null, null, null,
				null, HistoryDataBase.TOUCHED_AT + " DESC");
		startManagingCursor(cursor);

		mHistoriesList.setAdapter(new HistoriesAdater(cursor,
				getApplicationContext()));
	}

	public void onItemClick(final AdapterView<?> parent, final View view,
			final int position, final long id) {
		final Adapter adapter = parent.getAdapter();
		final Cursor cursor = (Cursor) adapter.getItem(position);
		final double lat = cursor.getDouble(cursor
				.getColumnIndex(HistoryDataBase.LAT));
		final double lon = cursor.getDouble(cursor
				.getColumnIndex(HistoryDataBase.LON));
		final Intent intent = new Intent(Intent.ACTION_VIEW);
		Log.d("AAA", "lat:" + lat);
		Log.d("AAA", "lon:" + lon);
		intent.setData(Uri.parse("geo:" + lat + "," + lon));
		startActivity(intent);
	}

	public void onClick(final View v) {
		if (v == mSendButton) {
			final Intent intent = new Intent(this, SenderActivity.class);
			startActivity(intent);
			finish();
		}
	}

	private class HistoriesAdater extends BaseAdapter {

		private final Cursor mCursor;
		private final LayoutInflater mInflater;
		private final int mTouchIdx;
		private final int mName;
		private final int mPhone;

		public HistoriesAdater(final Cursor cursor, final Context context) {
			this.mCursor = cursor;
			this.mInflater = LayoutInflater.from(context);
			this.mTouchIdx = cursor.getColumnIndex(HistoryDataBase.TOUCHED_AT);
			this.mName = cursor.getColumnIndex(HistoryDataBase.NAME);
			this.mPhone = cursor.getColumnIndex(HistoryDataBase.PHONE);
		}

		public int getCount() {
			if (mCursor == null || mCursor.isClosed()) {
				return 0;
			} else {
				return mCursor.getCount();
			}
		}

		public Object getItem(final int position) {
			if (mCursor == null || mCursor.isClosed()) {
				return null;
			} else {
				mCursor.moveToPosition(position);
				return mCursor;
			}
		}

		public long getItemId(final int position) {
			return position;
		}

		public View getView(final int position, View convertView,
				final ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.histories_row, null);
				holder = new ViewHolder();
				holder.date = (TextView) convertView
						.findViewById(R.id.txt_date);
				holder.name = (TextView) convertView
						.findViewById(R.id.txt_name);
				holder.phoneNumber = (TextView) convertView
						.findViewById(R.id.txt_phone_number);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			mCursor.moveToPosition(position);
			holder.date.setText(new Date(mCursor.getLong(mTouchIdx))
					.toLocaleString());
			holder.name.setText(mCursor.getString(mName));
			final String phoneNumber = mCursor.getString(mPhone);
			if (phoneNumber == null || phoneNumber.length() == 0) {
				holder.phoneNumber.setText("");
			} else {
				holder.phoneNumber.setText(mCursor.getString(mPhone));
			}

			return convertView;
		}

	}

	private static class ViewHolder {
		TextView date;
		TextView name;
		TextView phoneNumber;
	}

}
