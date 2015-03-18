package net.netortech.cloudsync.data;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class SettingsDB {
	private static final Map<Context, SettingsDB> instances = new HashMap<Context, SettingsDB>();

	private SettingsDBHelper helper;
	private SQLiteDatabase db;
	
	private SettingsDB(SettingsDBHelper helper) 
	{
		this.helper = helper;
		this.db = helper.getWritableDatabase();
	}
	
	private static void checkState(Context applicationContext)
	{
		if(!instances.containsKey(applicationContext))
		{
			instances.put(applicationContext, new SettingsDB(new SettingsDBHelper(applicationContext)));
		}
		else if(!instances.get(applicationContext).isOpen())
		{
			instances.remove(applicationContext);
			instances.put(applicationContext, new SettingsDB(new SettingsDBHelper(applicationContext)));
		}
		if(instances.size() > 1) log("checkState(): ***NOTICE*** Connections count is: " + instances.size(), true);
	}
	private boolean isOpen()
	{
		return db.isOpen();
	}

	public static void close(Context applicationContext)
	{
		SettingsDBHelper helper = null;
		synchronized(instances)
		{
			
			if(instances.containsKey(applicationContext))
			{
				helper = instances.get(applicationContext).helper;
				instances.remove(applicationContext);
			}
		}
		try
		{
			helper.close();
		}
		catch(Exception ex)
		{
			log("Failed to close database.", true);
		}
	}
	
	public static void Delete(TaskInfo task, Context applicationContext)
	{
		synchronized(instances)
		{
			checkState(applicationContext);
			instances.get(applicationContext).Delete(task);
		}
	}
	private void Delete(TaskInfo task)
	{
		DeleteAllTaskData(task.ID);
		DeleteAllTaskItems(task.ID);
		DeleteElement(task);
	}
	
	public static void Delete(AccountInfo account, Context applicationContext)
	{
		synchronized(instances)
		{
			checkState(applicationContext);
			instances.get(applicationContext).Delete(account);
		}
	}
	private void Delete(AccountInfo account)
	{
		if(getAllTaskInfoByAccount(account.ID).size() != 0)
			throw new DataException("Cannot delete account because there are tasks currently associated with it.");
		DeleteAllAccountData(account.ID);
		DeleteElement(account);
	}
	
	public static void Delete(TaskItem item, Context applicationContext)
	{
		synchronized(instances)
		{
			checkState(applicationContext);
			instances.get(applicationContext).Delete(item);
		}
	}
	private void Delete(TaskItem item)
	{
		DeleteElement(item);
	}	
	
	public static void Save(AccountData data, Context applicationContext)
	{
		synchronized(instances)
		{
			checkState(applicationContext);
			instances.get(applicationContext).Save(data);
		}
	}
	private void Save(AccountData data)
	{
		AccountInfo account = getAccountInfo(data.AccountID);
		if(account.ID == -1) throw new DataException("AccountID " + data.AccountID + " does not exist.");
		SaveElement(data);
	}
	
	public static void Save(TaskData data, Context applicationContext)
	{
		synchronized(instances)
		{
			checkState(applicationContext);
			instances.get(applicationContext).Save(data);
		}
	}
	private void Save(TaskData data)
	{
		TaskInfo task = getTaskInfo(data.TaskID);
		if(task.ID == -1) throw new DataException("TaskID " + data.TaskID + " does not exist.");
		SaveElement(data);
	}
	
	public static void Save(TaskItem data, Context applicationContext)
	{
		synchronized(instances)
		{
			checkState(applicationContext);
			instances.get(applicationContext).Save(data);
		}
	}
	private void Save(TaskItem data)
	{
		TaskInfo task = getTaskInfo(data.TaskID);
		if(task.ID == -1) throw new DataException("TaskID " + data.TaskID + " does not exist.");
		SaveElement(data);
	}
	
	public static void SaveAccountInfo(AccountInfo account, Context applicationContext)
	{
		synchronized(instances)
		{
			checkState(applicationContext);
			instances.get(applicationContext).SaveAccountInfo(account);
		}
	}
	private void SaveAccountInfo(AccountInfo account)
	{
		SaveAccountInfo(account, null, true);
	}
	
	public static void SaveAccountInfo(AccountInfo account, List<AccountData> accountData, Context applicationContext)
	{
		synchronized(instances)
		{
			checkState(applicationContext);
			instances.get(applicationContext).SaveAccountInfo(account, accountData, true);
		}
	}
	private void SaveAccountInfo(AccountInfo account, List<AccountData> accountData, boolean doesJavaSuck)
	{
		AccountInfo oldAccount = null;
		if(account == null) throw new DataException("Account is null.");
		account.Name = account.Name.trim();

		if(account.ID != -1)
		{
			oldAccount = this.getAccountInfo(account.ID);
			if(oldAccount.EngineID != account.EngineID)
			{
				if(getAllTaskInfoByAccount(account.ID).size() != 0)
					throw new DataException("Cannot change account engine type because there are tasks currently associated with it.");
				if(accountData == null) 
					throw new DataException("Required account data for alternate account engine was not provided.");
			} 
		}
		else
		{
			if(accountData == null) throw new DataException("Required account data for new account was not provided.");
		}
		
		// We're good.
		
		if(oldAccount != null && account.EngineID != oldAccount.EngineID)
		{
			// Need to clear out all the old account data.
			DeleteAllAccountData(account.ID);
			// Just in case they forgot.
			for(AccountData d : accountData) d.ID = -1;
		}
		SaveElement(account);
		if(accountData != null)
		{
			for(AccountData d : accountData)
			{
				d.AccountID = account.ID;
				SaveElement(d);
			}
		}
	}
	
	public static void SaveTaskInfo(TaskInfo task, Context applicationContext)
	{
		synchronized(instances)
		{
			checkState(applicationContext);
			instances.get(applicationContext).SaveTaskInfo(task);
		}
	}
	private void SaveTaskInfo(TaskInfo task)
	{
		SaveTaskInfo(task, null, true);
	}
	
	public static void SaveTaskInfo(TaskInfo task, List<TaskData> taskData, Context applicationContext)
	{
		synchronized(instances)
		{
			checkState(applicationContext);
			instances.get(applicationContext).SaveTaskInfo(task, taskData, true);
		}
	}
	private void SaveTaskInfo(TaskInfo task, List<TaskData> taskData, boolean doesJavaSuck)
	{
		AccountInfo oldTaskAccount = null;
		TaskInfo oldTask = null;
		if(task == null) throw new DataException("Task is null.");
		task.Name = task.Name.trim();

		AccountInfo taskAccount = getAccountInfo(task.AccountID);
		if(taskAccount.ID == -1) throw new DataException("Account info record not found or does not exist. task.AccountID = " + task.AccountID);

		if(task.ID != -1)
		{
			oldTask = getTaskInfo(task.ID);
			oldTaskAccount = oldTask.AccountID == task.AccountID ? taskAccount : this.getAccountInfo(oldTask.AccountID);
		}
		else
		{
			if(taskData == null) throw new DataException("Required task data for new task was not provided.");
			
			// To protect users from themselves.
			if(task.Path.equals(TaskInfo.DEFAULT_MEDIA_FOLDER) && getTaskInfoByPath(task.Path).ID != -1)
			{
				String prefix = TaskInfo.DEFAULT_MEDIA_FOLDER.substring(0, TaskInfo.DEFAULT_MEDIA_FOLDER.length() - 1);
				for(int index = 0; index < 1000; index++)
				{
					if(getTaskInfoByPath(prefix + Integer.toString(index)).ID == -1)
					{
						task.Path = prefix + Integer.toString(index);
						break;
					}
				}
			}
		}
		
		// We're good.
		
		if(oldTask != null && oldTaskAccount.EngineID != taskAccount.EngineID)
		{
			// Need to clear out all the old task data.
			DeleteAllTaskData(task.ID);
			// Just in case they forgot.
			for(TaskData d : taskData) d.ID = -1;
		}
		SaveElement(task);
		if(taskData != null)
		{
			for(TaskData d : taskData)
			{
				d.TaskID = task.ID;
				SaveElement(d);
			}
		}
	}

	public static EngineInfo getEngineInfo(long engineID, Context applicationContext)
	{
		synchronized(instances)
		{
			checkState(applicationContext);
			return instances.get(applicationContext).getEngineInfo(engineID);
		}
	}
	private EngineInfo getEngineInfo(long engineID)
	{
		EngineInfo e = new EngineInfo();
		Boolean more;
		
		Cursor c = db.query(true, e.getTable(), e.getFields(), e.getIDField() + "=" + engineID, null, null, null, null, null);
		
		if (c != null) {
			more = c.moveToFirst();
			if(more) e.Bind(c);
		}
		c.close();
		return e;		
	}
	
	public static LinkedList<EngineInfo> getAllEngineInfo(Context applicationContext)
	{
		synchronized(instances)
		{
			checkState(applicationContext);
			return instances.get(applicationContext).getAllEngineInfo();
		}
	}
	private LinkedList<EngineInfo> getAllEngineInfo()
	{
		LinkedList<EngineInfo> ret = new LinkedList<EngineInfo>();
		EngineInfo e = new EngineInfo();
		Boolean more;
		
		Cursor c = db.query(true, e.getTable(), e.getFields(), null, null, null, null, null, null);
		
		if (c != null) {
			more = c.moveToFirst();
			while(more)
			{
				e.Bind(c);
				ret.add(e);
				more = !c.isLast();
				if(more){
					c.moveToNext();
					e = new EngineInfo();
				}
			}
		}
		c.close();
		return ret;
	}
	
	public static AccountInfo getAccountInfo(long accountID, Context applicationContext)
	{
		synchronized(instances)
		{
			checkState(applicationContext);
			return instances.get(applicationContext).getAccountInfo(accountID);
		}
	}
	private AccountInfo getAccountInfo(long accountID)
	{
		AccountInfo e = new AccountInfo();
		Boolean more;
		
		Cursor c = db.query(true, e.getTable(), e.getFields(), e.getIDField() + "=" + accountID, null, null, null, null, null);
		
		if (c != null) {
			more = c.moveToFirst();
			if(more) e.Bind(c);
		}
		c.close();
		return e;		
	}
	
	public static LinkedList<AccountInfo> getAllAccountInfo(Context applicationContext)
	{
		synchronized(instances)
		{
			checkState(applicationContext);
			return instances.get(applicationContext).getAllAccountInfo();
		}
	}
	private LinkedList<AccountInfo> getAllAccountInfo()
	{
		LinkedList<AccountInfo> ret = new LinkedList<AccountInfo>();
		AccountInfo e = new AccountInfo();
		Boolean more;
		
		Cursor c = db.query(true, e.getTable(), e.getFields(), null, null, null, null, null, null);
		
		if (c != null) {
			more = c.moveToFirst();
			while(more)
			{
				e.Bind(c);
				ret.add(e);
				more = !c.isLast();
				if(more){
					c.moveToNext();
					e = new AccountInfo();
				}
			}
		}
		c.close();
		return ret;
	}
	
	public static LinkedList<AccountInfo> getAllAccountInfoByEngine(long engineID, Context applicationContext)
	{
		synchronized(instances)
		{
			checkState(applicationContext);
			return instances.get(applicationContext).getAllAccountInfoByEngine(engineID);
		}
	}
	private LinkedList<AccountInfo> getAllAccountInfoByEngine(long engineID)
	{
		LinkedList<AccountInfo> ret = new LinkedList<AccountInfo>();
		AccountInfo e = new AccountInfo();
		Boolean more;
		
		Cursor c = db.query(true, e.getTable(), e.getFields(), "engineid=" + engineID, null, null, null, null, null);
		
		if (c != null) {
			more = c.moveToFirst();
			while(more)
			{
				e.Bind(c);
				ret.add(e);
				more = !c.isLast();
				if(more){
					c.moveToNext();
					e = new AccountInfo();
				}
			}
		}
		c.close();
		return ret;
	}
	
	public static AccountData getAccountData(long accountID, String name, Context applicationContext)
	{
		synchronized(instances)
		{
			checkState(applicationContext);
			return instances.get(applicationContext).getAccountData(accountID, name);
		}
	}
	private AccountData getAccountData(long accountID, String name)
	{
		AccountData e = new AccountData();
		Boolean more;
		
		Cursor c = db.query(true, e.getTable(), e.getFields(), "accountid=" + accountID + " AND name='" + name.replace("'", "''") + "'", null, null, null, null, null);
		if (c != null) {
			more = c.moveToFirst();
			if(more) e.Bind(c);
		}
		c.close();
		
		e.AccountID = accountID;
		e.Name = name;
		
		return e;		
	}
	
	public static LinkedList<AccountData> getAllAccountData(Context applicationContext)
	{
		synchronized(instances)
		{
			checkState(applicationContext);
			return instances.get(applicationContext).getAllAccountData();
		}
	}
	private LinkedList<AccountData> getAllAccountData()
	{
		LinkedList<AccountData> ret = new LinkedList<AccountData>();
		AccountData e = new AccountData();
		Boolean more;
		
		Cursor c = db.query(true, e.getTable(), e.getFields(), null, null, null, null, null, null);
		
		if (c != null) {
			more = c.moveToFirst();
			while(more)
			{
				e.Bind(c);
				ret.add(e);
				more = !c.isLast();
				if(more){
					c.moveToNext();
					e = new AccountData();
				}
			}
		}
		c.close();
		return ret;
	}	
	
	public static LinkedList<AccountData> getAllAccountData(long accountID, Context applicationContext)
	{
		synchronized(instances)
		{
			checkState(applicationContext);
			return instances.get(applicationContext).getAllAccountData(accountID);
		}
	}
	private LinkedList<AccountData> getAllAccountData(long accountID)
	{
		LinkedList<AccountData> ret = new LinkedList<AccountData>();
		AccountData e = new AccountData();
		Boolean more;
		
		Cursor c = db.query(true, e.getTable(), e.getFields(), "accountid=" + accountID, null, null, null, null, null);
		
		if (c != null) {
			more = c.moveToFirst();
			while(more)
			{
				e.Bind(c);
				ret.add(e);
				more = !c.isLast();
				if(more){
					c.moveToNext();
					e = new AccountData();
				}
			}
		}
		c.close();
		return ret;
	}

	public static TaskInfo getTaskInfo(long taskID, Context applicationContext)
	{
		synchronized(instances)
		{
			checkState(applicationContext);
			return instances.get(applicationContext).getTaskInfo(taskID);
		}
	}
	private TaskInfo getTaskInfo(long taskID)
	{
		TaskInfo e = new TaskInfo();
		Boolean more;
		
		Cursor c = db.query(true, e.getTable(), e.getFields(), e.getIDField() + "=" + taskID, null, null, null, null, null);
		
		if (c != null) {
			more = c.moveToFirst();
			if(more) e.Bind(c);
		}
		c.close();
		return e;		
	}
	
	public static TaskInfo getTaskInfoByPath(String path, Context applicationContext)
	{
		synchronized(instances)
		{
			checkState(applicationContext);
			return instances.get(applicationContext).getTaskInfoByPath(path);
		}
	}
	private TaskInfo getTaskInfoByPath(String path)
	{
		TaskInfo e = new TaskInfo();
		Boolean more;
		
		Cursor c = db.query(true, e.getTable(), e.getFields(), "path=?", new String[] { path }, null, null, null, null);
		
		if (c != null) {
			more = c.moveToFirst();
			if(more) e.Bind(c);
		}
		c.close();
		return e;		
	}
	
	public static LinkedList<TaskInfo> getAllTaskInfo(Context applicationContext)
	{
		synchronized(instances)
		{
			checkState(applicationContext);
			return instances.get(applicationContext).getAllTaskInfo();
		}
	}
	private LinkedList<TaskInfo> getAllTaskInfo()
	{
		LinkedList<TaskInfo> ret = new LinkedList<TaskInfo>();
		TaskInfo e = new TaskInfo();
		Boolean more;
		
		Cursor c = db.query(true, e.getTable(), e.getFields(), null, null, null, null, null, null);
		
		if (c != null) {
			more = c.moveToFirst();
			while(more)
			{
				e.Bind(c);
				ret.add(e);
				more = !c.isLast();
				if(more){
					c.moveToNext();
					e = new TaskInfo();
				}
			}
		}
		c.close();
		return ret;
	}
	
	public static LinkedList<TaskInfo> getAllTaskInfoByAccount(long accountID, Context applicationContext)
	{
		synchronized(instances)
		{
			checkState(applicationContext);
			return instances.get(applicationContext).getAllTaskInfoByAccount(accountID);
		}
	}
	private LinkedList<TaskInfo> getAllTaskInfoByAccount(long accountID)
	{
		LinkedList<TaskInfo> ret = new LinkedList<TaskInfo>();
		TaskInfo e = new TaskInfo();
		Boolean more;
		
		Cursor c = db.query(true, e.getTable(), e.getFields(), "accountid=" + accountID, null, null, null, null, null);
		
		if (c != null) {
			more = c.moveToFirst();
			while(more)
			{
				e.Bind(c);
				ret.add(e);
				more = !c.isLast();
				if(more){
					c.moveToNext();
					e = new TaskInfo();
				}
			}
		}
		c.close();
		return ret;
	}
	
	public static TaskData getTaskData(long taskID, String name, Context applicationContext)
	{
		synchronized(instances)
		{
			checkState(applicationContext);
			return instances.get(applicationContext).getTaskData(taskID, name);
		}
	}
	private TaskData getTaskData(long taskID, String name)
	{
		TaskData e = new TaskData();
		Boolean more;
		
		Cursor c = db.query(true, e.getTable(), e.getFields(), "taskid=" + taskID + " AND name='" + name.replace("'", "''") + "'", null, null, null, null, null);
		if (c != null) {
			more = c.moveToFirst();
			if(more) e.Bind(c);
		}
		c.close();
		
		e.TaskID = taskID;
		e.Name = name;
		
		return e;		
	}	
	
	public static LinkedList<TaskData> getAllTaskData(long taskID, Context applicationContext)
	{
		synchronized(instances)
		{
			checkState(applicationContext);
			return instances.get(applicationContext).getAllTaskData(taskID);
		}
	}
	private LinkedList<TaskData> getAllTaskData(long taskID)
	{
		LinkedList<TaskData> ret = new LinkedList<TaskData>();
		TaskData e = new TaskData();
		Boolean more;
		
		Cursor c = db.query(true, e.getTable(), e.getFields(), "taskid=" + taskID, null, null, null, null, null);
		
		if (c != null) {
			more = c.moveToFirst();
			while(more)
			{
				e.Bind(c);
				ret.add(e);
				more = !c.isLast();
				if(more){
					c.moveToNext();
					e = new TaskData();
				}
			}
		}
		c.close();
		return ret;
	}
	
	public static LinkedList<TaskItem> getAllTaskItems(long taskID, Context applicationContext)
	{
		synchronized(instances)
		{
			checkState(applicationContext);
			return instances.get(applicationContext).getAllTaskItems(taskID);
		}
	}
	private LinkedList<TaskItem> getAllTaskItems(long taskID)
	{
		LinkedList<TaskItem> ret = new LinkedList<TaskItem>();
		TaskItem e = new TaskItem();
		Boolean more;
		
		Cursor c = db.query(true, e.getTable(), e.getFields(), "taskid=" + taskID, null, null, null, null, null);
		
		if (c != null) {
			more = c.moveToFirst();
			while(more)
			{
				e.Bind(c);
				ret.add(e);
				more = !c.isLast();
				if(more){
					c.moveToNext();
					e = new TaskItem();
				}
			}
		}
		c.close();
		return ret;
	}
	
	private void SaveElement(IUniqueElement e)
	{
		if(e.getID() == -1)
			e.setID(db. insertOrThrow(e.getTable(), null, e.getValues()));
		else
			db.update(e.getTable(), e.getValues(), e.getIDField() + "=" + e.getID(), null);
	}
	private void DeleteElement(IUniqueElement e)
	{
		if(e.getID() == -1) return;
		db.delete(e.getTable(), e.getIDField() + "=" + e.getID(), null);
	}
	private void DeleteAllAccountData(long accountID)
	{
		AccountData e = new AccountData();
		db.delete(e.getTable(), "accountid=" + accountID, null);
	}
	private void DeleteAllTaskData(long taskID)
	{
		TaskData e = new TaskData();
		db.delete(e.getTable(), "taskid=" + taskID, null);
	}
	private void DeleteAllTaskItems(long taskID)
	{
		TaskItem e = new TaskItem();
		db.delete(e.getTable(), "taskid=" + taskID, null);
	}
	
	private static void log(String msg)
	{
		log(msg, false);
	}
	private static void log(String msg, Boolean quiet)
	{
		if(!quiet) return;
		Log.v("SettingsDB", msg);
	}
}
