package net.netortech.cloudsync.data;

import java.util.Date;

import android.content.ContentValues;
import android.database.Cursor;

public class TaskItem implements IUniqueElement {
	public long getID() { return ID; }
	public void setID(long id) { ID = id; }
	public String getIDField() { return "_id"; }
	public String getTable() { return "task_items"; }
	public ContentValues getValues()
	{
		ContentValues values = new ContentValues();
		values.put("taskid", TaskID);
		values.put("key", Key);
		values.put("name", Name);
		values.put("path", Path);
		values.put("local_modified", LocalModified.toLocaleString());
		values.put("cloud_modified", CloudModified.toLocaleString());
		return values;
	}
	public String[] getFields()
	{
		return new String[] { "_id", "taskid", "key", "name", "path", "local_modified", "cloud_modified" };
	}
	public void Bind(Cursor c)
	{
		String[] columns = c.getColumnNames();
		String column;
		for(int index = 0; index < columns.length; index++)
		{
			column = columns[index].toLowerCase();
			if(column.equals("_id")) 					ID = c.getLong(index);
			if(column.equals("taskid"))					TaskID = c.getLong(index);
			if(column.equals("key")) 					Key = c.getString(index);
			if(column.equals("name")) 					Name = c.getString(index);
			if(column.equals("path")) 					Path = c.getString(index);
			if(column.equals("local_modified")) 		LocalModified = new Date(c.getString(index));
			if(column.equals("cloud_modified")) 		CloudModified = new Date(c.getString(index));
		}
	}
	public Boolean Validate()
	{
		if(TaskID == -1) throw new RuntimeException("Invalid Task Item Record: TaskID must be specified.");
		return true;
	}
	
	public long ID = -1;
	public long TaskID = -1;
	public String Key = "";
	public String Name = "";
	public String Path = "";
	public Date LocalModified;
	public Date CloudModified;
	
	@Override
	public String toString()
	{
		return "{ ID: " + ID 
				+ ", TaskID: " + TaskID 
				+ ", Key: " + Key 
				+ ", Name: " + Name 
				+ ", Path: " + Path 
				+ ", LocalModified: " + LocalModified.toLocaleString() 
				+ ", CloudModified: " + CloudModified.toLocaleString() + " }";
	}
}
