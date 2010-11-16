package com.demo.felica.contacts;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

public class ReceiverActivity extends Activity implements LocationListener {

	private static final String TAG = "RECEIVE";

	private LocationManager mLocationManager;
	private boolean mLocChanged = false;
	private double mLat;
	private double mLon;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d(TAG, "RECEIVE");

		final Context context = getApplicationContext();
		final Intent intent = getIntent();
		final Bundle bundle = intent.getExtras();
		final String name = bundle.getString(Consts.EXTRA_NAME);
		final String phoneNumber = bundle.getString(Consts.EXTRA_PHONE);

		Log.d(TAG, phoneNumber);
		new RecordingTask(getApplicationContext(), name, phoneNumber).execute();
	}

	@Override
	protected void onStart() {
		super.onStart();
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		final String provider = mLocationManager.getBestProvider(
				new Criteria(), true);

		mLocationManager.requestLocationUpdates(provider, 0, 0, this);
	}

	@Override
	protected void onStop() {
		if (mLocationManager != null) {
			mLocationManager.removeUpdates(this);
			mLocationManager = null;
		}
		super.onStop();
	}

	public void onLocationChanged(final Location location) {
		Log.d(TAG, "onLocationChanged");
		mLocChanged = true;
		mLat = location.getLatitude();
		mLon = location.getLongitude();
	}

	public void onProviderDisabled(final String provider) {
		Log.d(TAG, "onProviderDisabled");
		// TODO Auto-generated method stub

	}

	public void onProviderEnabled(final String provider) {
		Log.d(TAG, "onProviderEnabled");
		// TODO Auto-generated method stub

	}

	public void onStatusChanged(final String provider, final int status,
			final Bundle extras) {
		Log.d(TAG, "onStatusChanged");
		// TODO Auto-generated method stub

	}

	public void moveToList() {
		final Intent intent = new Intent(this, HistoriesActivity.class);
		startActivity(intent);
		finish();
	}

	private class RecordingTask extends AsyncTask<Void, Void, Void> {

		private final Context mContext;
		private final String mName;
		private final String mPhoneNumber;

		public RecordingTask(final Context context, final String name,
				final String phoneNumber) {
			this.mContext = context;
			this.mName = name;
			this.mPhoneNumber = phoneNumber;
		}

		@Override
		protected Void doInBackground(final Void... params) {

			Log.d(TAG, "doInBackground 1");

			final long touchedAt = System.currentTimeMillis();

			while (true) {
				if (mLocChanged) {
					break;
				}
				try {
					Thread.sleep(1000);
				} catch (final InterruptedException e) {
				}
			}
			Log.d(TAG, "lat:" + mLat);
			Log.d(TAG, "lon:" + mLon);

			final SQLiteDatabase db = HistoryDataBase.getDb(mContext);
			final ContentValues values = new ContentValues();
			values.put(HistoryDataBase.TOUCHED_AT, touchedAt);
			values.put(HistoryDataBase.NAME, mName);
			values.put(HistoryDataBase.PHONE, mPhoneNumber);
			values.put(HistoryDataBase.LAT, mLat);
			values.put(HistoryDataBase.LON, mLon);
			db.insert(HistoryDataBase.TBL_NAME, null, values);

			// TODO Auto-generated method stub
			db.close();
			Log.d(TAG, "doInBackground 99");
			return null;
		}

		@Override
		protected void onPostExecute(final Void result) {
			moveToList();
		}

	}

}
