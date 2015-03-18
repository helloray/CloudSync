package net.netortech.cloudsync.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

class SettingsDBHelper extends SQLiteOpenHelper {
	private static final int DATABASE_VERSION_RELEASE_1_00_0000 = 29;
	private static final int DATABASE_VERSION_RELEASE_1_00_0100 = 30;
	
	private static final int DATABASE_VERSION = 30;
	private static final String DATABASE_DELETE =
			"drop table if exists application_data; " +
			"drop table if exists engine_info; " +
			"drop table if exists engine_data; " +
			"drop table if exists account_info; " +
			"drop table if exists account_data; " +
			"drop table if exists task_info; " +
			"drop table if exists task_data; " +
			"drop table if exists task_items; ";
 	private static final String ENGINE_INFO_INSERTS =
 			"insert into engine_info (name, code, engineorder, oauth) " 
 			+ "values('Dropbox', '" + EngineInfo.ENGINE_CODE_DROPBOX + "', 1, 1); " + 
 			"insert into engine_info (name, code, engineorder, oauth) " 
 			+ "values('Amazon S3', '" + EngineInfo.ENGINE_CODE_AWS + "', 1, 0); " + 
 			"insert into engine_info (name, code, engineorder, oauth) " 
 			+ "values('Amazon S3 w/ Proxy Signing', '" + EngineInfo.ENGINE_CODE_AWSPROXY + "', 1, 0); " + 
 			"insert into engine_info (name, code, engineorder, oauth) " 
 			+ "values('Windows Azure', '" + EngineInfo.ENGINE_CODE_AZURE + "', 1, 0); " + 
 			"insert into engine_info (name, code, engineorder, oauth) " 
 			+ "values('Windows Azure w/ Proxy Signing', '" + EngineInfo.ENGINE_CODE_AZUREPROXY + "', 1, 0);"; 
 	private static final String SKYDRIVE_ENGINE_INFO_INSERT =
 			"insert into engine_info (name, code, engineorder, oauth) "
 			+ "values('SkyDrive', '" + EngineInfo.ENGINE_CODE_SKYDRIVE + "', 1, 1); ";
	private static final String DATABASE_CREATE = 
			"create table application_data "
			+ "(_id integer primary key autoincrement, "
			+ "name text not null, " 
			+ "data text not null); " +
			"create table engine_info "
			+ "(_id integer primary key autoincrement, "
			+ "name text not null, " 
			+ "code text not null, " 
			+ "engineorder integer not null, " 
			+ "oauth integer not null); " +
			"create table engine_data "
			+ "(_id integer primary key autoincrement, "
			+ "engineid integer not null, " 
			+ "name text not null, " 
			+ "data text not null); " +
			"create table account_info "
			+ "(_id integer primary key autoincrement, "
			+ "name text not null, " 
			+ "enabled integer not null, "
			+ "failures integer not null, "
			+ "lastrun text not null, "
			+ "lastresult text not null, "
			+ "engineid integer not null); " +
			"create table account_data "
			+ "(_id integer primary key autoincrement, "
			+ "accountid integer not null, "
			+ "name text not null, "
			+ "data text not null); " +
			"create table task_info "
			+ "(_id integer primary key autoincrement, "
			+ "name text not null, "
			+ "accountid integer not null, "
			+ "enabled integer not null, "
			+ "maxsize integer not null, "
			+ "mobilemaxsize integer not null, "
			+ "twoway integer not null, "
			+ "lastrun text not null, "
			+ "path text not null); " +
			"create table task_data "
			+ "(_id integer primary key autoincrement, "
			+ "taskid integer not null, "
			+ "name text not null, "
			+ "data text not null); " +
			"create table task_items "
			+ "(_id integer primary key autoincrement, "
			+ "taskid integer not null, "
			+ "key text not null, "
			+ "name text not null, "
			+ "path text not null, "
			+ "cloud_modified text not null, "
			+ "local_modified text not null); ";
 	private static final String DATABASE_NAME = "app_settings";
 	
 	public static final String TABLE_ACCOUNT_INFO = "account_info";
 	public static final String TABLE_ACCOUNT_DATA = "account_data";
 	public static final String TABLE_TASK_INFO = "task_info";
 	public static final String TABLE_TASK_ITEM = "task_item";
 	public static final String TABLE_TASK_DATA = "task_data";
 	
	public SettingsDBHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.v("SettingsDBHelper", "Creating database.");
		for(String s : DATABASE_CREATE.split(";"))
			if(!s.trim().equals("")) db.execSQL(s);
		insertEngines(db);
	}
	public void insertEngines(SQLiteDatabase db)
	{
		for(String s : ENGINE_INFO_INSERTS.split(";"))
			if(!s.trim().equals("")) db.execSQL(s);
	}
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.v("SettingsDBHelper", "Deleting database.");
		
		if(oldVersion < DATABASE_VERSION_RELEASE_1_00_0000)
		{
			for(String s : DATABASE_DELETE.split(";"))
				if(!s.trim().equals("")) db.execSQL(s);
			onCreate(db);
		} 
		
		if(oldVersion <= DATABASE_VERSION_RELEASE_1_00_0000) 
		{
			db.execSQL(SKYDRIVE_ENGINE_INFO_INSERT);
		}
	}

}
