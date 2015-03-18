package net.netortech.cloudsync.data;

import java.io.Serializable;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;

public class AccountData implements IUniqueElement, Serializable {
	public static AccountData getByName(String name, List<AccountData> accountData)
	{
		for(AccountData d : accountData) if(d.Name.equals(name)) return d;
		return null;
	}
	
	public AccountData()
	{
		
	}
	public AccountData (String name)
	{
		Name = name;
	}

	public long getID() { return ID; }
	public void setID(long id) { ID = id; }
	public String getIDField() { return "_id"; }
	public String getTable() { return "account_data"; }
	public ContentValues getValues()
	{
		ContentValues values = new ContentValues();
		values.put("accountid", AccountID);
		values.put("name", Name);
		values.put("data", Data);
		return values;
	}
	public String[] getFields()
	{
		return new String[] { "_id", "accountid", "name", "data" };
	}
	public void Bind(Cursor c)
	{
		String[] columns = c.getColumnNames();
		String column;
		for(int index = 0; index < columns.length; index++)
		{
			column = columns[index].toLowerCase();
			if(column.equals("_id")) 		ID = c.getLong(index);
			if(column.equals("accountid")) 	AccountID = c.getLong(index);
			if(column.equals("name")) 		Name = c.getString(index);
			if(column.equals("data")) 		Data = c.getString(index);
		}
	}
	public Boolean Validate()
	{
		if(AccountID == -1) throw new RuntimeException("Invalid Account Data Record: AccountID must be specified.");
		if(Name == null || Name.equals("")) throw new RuntimeException("Invalid Account Data Record: Name must be specified.");
		return true;
	}
	
	
	public long ID = -1;
	public long AccountID = -1;
	public String Name = "";
	public String Data = "";
	
	@Override
	public String toString()
	{
		return "{ ID: " + ID + ", AccountID: " + AccountID + ", Name: " + Name + ", Data: " + Data + " }";
	}
}
