package com.demo.felica.contacts;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class HistoryDataBase extends SQLiteOpenHelper {

	private static final String DB_NAME = "db";
	private static final int DB_VERSION = 1;

	public static final String TBL_NAME = "histories";
	public static final String TOUCHED_AT = "touched_at";
	public static final String NAME = "name";
	public static final String PHONE = "phone";
	public static final String LAT = "lat";
	public static final String LON = "long";

	private static final String CREATE = "create table " + TBL_NAME + "("
			+ TOUCHED_AT + " integer primary key," + NAME + " text not null,"
			+ PHONE + " text," + LAT + " real," + LON + " real" + ");";

	public static SQLiteDatabase getDb(final Context context) {
		return new HistoryDataBase(context).getWritableDatabase();
	}

	/**
	 * @param context
	 */
	public HistoryDataBase(final Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(final SQLiteDatabase db) {
		db.execSQL(CREATE);
	}

	@Override
	public void onUpgrade(final SQLiteDatabase db, final int oldVersion,
			final int newVersion) {
	}

}
