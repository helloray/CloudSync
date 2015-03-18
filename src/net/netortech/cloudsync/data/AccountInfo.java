package net.netortech.cloudsync.data;

import java.io.Serializable;
import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

public class AccountInfo implements IUniqueElement, Serializable {
	public static final int MAX_FAILURES_COUNT = 9;

	public long getID() { return ID; }
	public void setID(long id) { ID = id; }
	public String getIDField() { return "_id"; }
	public String getTable() { return "account_info"; }
	public ContentValues getValues()
	{
		ContentValues values = new ContentValues();
		values.put("name", Name);
		values.put("engineid", EngineID);
		values.put("failures", Failures);
		values.put("lastrun", LastRun.toString());
		values.put("lastresult", LastResult);
		values.put("enabled", Enabled ? 1 : 0);
		return values;
	}
	public String[] getFields()
	{
		return new String[] { "_id", "name", "enabled", "failures", "lastresult", "lastrun", "engineid" };
	}
	public void Bind(Cursor c)
	{
		String[] columns = c.getColumnNames();
		String column;
		for(int index = 0; index < columns.length; index++)
		{
			column = columns[index].toLowerCase();
			if(column.equals("_id")) 		ID = c.getLong(index);
			if(column.equals("name")) 		Name = c.getString(index);
			if(column.equals("engineid")) 	EngineID = c.getLong(index);
			if(column.equals("failures")) 	Failures = c.getInt(index);
			if(column.equals("lastresult")) LastResult = c.getString(index);
			try
			{
				if(column.equals("lastrun")) 	LastRun = new Date(c.getString(index));
			} 
			catch(Exception ex)
			{
				throw new RuntimeException("Could not parse date: '" + c.getString(index) + "'.");
			}
			
			if(column.equals("enabled")) 	Enabled = c.getLong(index) == 1;
		}
	}
	public Boolean Validate()
	{
		return true;
	}
	
	public AccountData AddData(String name, String data)
	{
		AccountData ret = new AccountData();
		ret.AccountID = this.ID;
		ret.Name = name;
		ret.Data = data;
		return ret;
	}	
	public AccountData SetData(Context dbContext, String name, String data)
	{
		AccountData ret = SettingsDB.getAccountData(this.ID, name, dbContext);
		ret.Name = name;
		ret.Data = data;
		SettingsDB.Save(ret, dbContext);
		return ret;
	}
	
	public long ID = -1;
	public String Name = "";
	public long EngineID = 1;
	public int Failures = 0;
	public Date LastRun = new Date("1/1/1900");
	public String LastResult = "";
	public Boolean Enabled = true;
	
	@Override
	public String toString()
	{
		return Name;
	}
	public String toString(boolean longForm)
	{
		if(!longForm) return toString();
		
		return "{ Name: " + (Name == null ? "<null>" : "'" + Name + "'") + ", " +
				"EngineID: " + EngineID + ", " +
				"Failures: " + Failures + ", " +
				"LastRun: " + (LastRun == null ? "<null>" : "'" + LastRun.toString() + "'") + ", " +
				"LastResult: " + (LastResult == null ? "<null>" : "'" + LastResult + "'") + ", " +
				"Enabled: " + (Enabled == null ? "<null>" : Enabled) + " }";
	}	
}
