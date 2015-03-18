package net.netortech.cloudsync.data;

import java.io.Serializable;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;

public class TaskData implements IUniqueElement, Serializable {
	public static TaskData getByName(String name, List<TaskData> taskData)
	{
		for(TaskData d : taskData) if(d.Name.equals(name)) return d;
		return null;
	}
	
	public TaskData() {}
	public TaskData(String name) { Name = name; }
	
	public long getID() { return ID; }
	public void setID(long id) { ID = id; }
	public String getIDField() { return "_id"; }
	public String getTable() { return "task_data"; }
	public ContentValues getValues()
	{
		ContentValues values = new ContentValues();
		values.put("taskid", TaskID);
		values.put("name", Name);
		values.put("data", Data);
		return values;
	}
	public String[] getFields()
	{
		return new String[] { "_id", "taskid", "name", "data" };
	}
	public void Bind(Cursor c)
	{
		String[] columns = c.getColumnNames();
		String column;
		for(int index = 0; index < columns.length; index++)
		{
			column = columns[index].toLowerCase();
			if(column.equals("_id")) 		ID = c.getLong(index);
			if(column.equals("taskid")) 	TaskID = c.getLong(index);
			if(column.equals("name")) 		Name = c.getString(index);
			if(column.equals("data")) 		Data = c.getString(index);
		}
	}
	public Boolean Validate()
	{
		if(TaskID == -1) throw new RuntimeException("Invalid Task Data Record: TaskID must be specified.");
		if(Name == null || Name.equals("")) throw new RuntimeException("Invalid Task Data Record: Name must be specified.");
		return true;
	}

	public long ID = -1;
	public long TaskID = -1;
	public String Name = "";
	public String Data = "";
}
