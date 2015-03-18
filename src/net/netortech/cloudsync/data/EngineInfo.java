package net.netortech.cloudsync.data;

import java.io.Serializable;

import android.content.ContentValues;
import android.database.Cursor;

public class EngineInfo implements IUniqueElement, Serializable {
	public static final String ENGINE_CODE_DROPBOX = "dropbox";
	public static final String ENGINE_CODE_AWS = "aws";
	public static final String ENGINE_CODE_AWSPROXY = "awsproxy";
	public static final String ENGINE_CODE_AZURE = "azure";
	public static final String ENGINE_CODE_AZUREPROXY = "azureproxy";
	public static final String ENGINE_CODE_SKYDRIVE = "skydrive";
	
	
	public long getID() { return ID; }
	public void setID(long id) { ID = id; }
	public String getIDField() { return "_id"; }
	public String getTable() { return "engine_info"; }
	public ContentValues getValues()
	{
		ContentValues values = new ContentValues();
		values.put("name", Name);
		values.put("code", Code);
		values.put("oauth", OAuth ? 1 : 0);
		values.put("engineorder", Order);
		return values;
	}
	public String[] getFields()
	{
		return new String[] { "_id", "name", "code", "oauth", "engineorder" };
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
			if(column.equals("code")) 		Code = c.getString(index);
			if(column.equals("oauth")) 		OAuth = c.getInt(index) == 1;
			if(column.equals("engineorder")) 		Order = c.getInt(index);
		}
	}
	public Boolean Validate()
	{
		if(Name == null || Name.equals("")) throw new RuntimeException("Invalid Engine Info Record: Name must be specified.");
		return true;
	}
	
	
	public long ID = -1;
	public String Name = "";
	public String Code = "";
	public Boolean OAuth = false;
	public long Order = 1;
	
	@Override
	public String toString()
	{
		return Name;
	}
}
