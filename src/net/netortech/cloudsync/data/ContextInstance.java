package net.netortech.cloudsync.data;

import android.database.sqlite.SQLiteDatabase;

public class ContextInstance {
	public ContextInstance() {}
	public SettingsDBHelper helper;
	public SQLiteDatabase db = null;
}
