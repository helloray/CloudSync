package net.netortech.cloudsync.data;

import java.io.Serializable;
import java.util.Date;

import net.netortech.cloudsync.clouds.CloudItem;
import net.netortech.cloudsync.clouds.SyncFile;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;


public class TaskInfo implements IUniqueElement, Serializable {
	public static final String DEFAULT_SYNC_FOLDER = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Sync/CloudSync/1";
	public static final String DEFAULT_ANDROID_MUSIC_FOLDER = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath();
	public static final String DEFAULT_MEDIA_FOLDER = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath() + "/CloudSync/1";
	public static final int KILOBYTE = 1024;
	public static final int MEGABYTE = KILOBYTE * KILOBYTE;
	public static final int DEFAULT_MAX_WIFI_FILE_SIZE = -1;
	public static final int DEFAULT_MAX_MOBILE_FILE_SIZE = 5 * MEGABYTE;
	
	public long getID() { return ID; }
	public void setID(long id) { ID = id; }
	public String getIDField() { return "_id"; }
	public String getTable() { return "task_info"; }
	public ContentValues getValues()
	{
		ContentValues values = new ContentValues();
		values.put("accountid", AccountID);
		values.put("name", Name);
		values.put("maxsize", MaxSize);
		values.put("lastrun", LastRun.toString());
		values.put("path", Path);
		values.put("enabled", 0);
		values.put("mobilemaxsize", MobileMaxSize);
		values.put("twoway", 0);
		return values;
	}
	public String[] getFields()
	{
		return new String[] { "_id", "accountid", "enabled", "name", "maxsize", "mobilemaxsize", "twoway", "lastrun", "path" };
	}
	public void Bind(Cursor c)
	{
		String[] columns = c.getColumnNames();
		String column;
		for(int index = 0; index < columns.length; index++)
		{
			column = columns[index].toLowerCase();
			if(column.equals("_id")) 			ID = c.getLong(index);
			if(column.equals("accountid")) 		AccountID = c.getLong(index);
			if(column.equals("name")) 			Name = c.getString(index);
			if(column.equals("maxsize"))		MaxSize = c.getLong(index);
			if(column.equals("mobilemaxsize"))	MobileMaxSize = c.getLong(index);
			try
			{
				if(column.equals("lastrun")) 	LastRun = new Date(c.getString(index));
			} 
			catch(Exception ex)
			{
				throw new RuntimeException("Could not parse date: '" + c.getString(index) + "'.");
			}
			if(column.equals("path")) 			Path = c.getString(index);
		}
	}
	public Boolean Validate()
	{
		if(AccountID == -1) throw new RuntimeException("Invalid Task Info Record: AccountID must be specified.");
		return true;
	}

	public TaskData AddData(String name, String data)
	{
		TaskData ret = new TaskData();
		ret.TaskID = this.ID;
		ret.Name = name;
		ret.Data = data;
		return ret;
	}	
	public TaskItem AddItem(String key, Date localModified, Date cloudModified)
	{
		TaskItem ret = new TaskItem();
		ret.TaskID = this.ID;
		ret.Key = key;
		ret.LocalModified = localModified;
		ret.CloudModified = cloudModified;
		return ret;
	}		
	
	public long ID = -1;
	public long AccountID = -1;
	public String Name = "";
	public long MaxSize = DEFAULT_MAX_WIFI_FILE_SIZE;
	public long MobileMaxSize = DEFAULT_MAX_MOBILE_FILE_SIZE;
	public Date LastRun = new Date("1/1/1900");
	public String Path = DEFAULT_SYNC_FOLDER;
	
	public Boolean shouldSync(CloudItem cloudItem, TaskItem localItem, boolean wifiMode)
	{
		if(isFileTooLarge(cloudItem.getSize(), wifiMode))
		{
			log(Name + ": Sync rejected " + cloudItem.getName() + " because it is too large (" + cloudItem.getSize() + " bytes) for wifiMode=" + Boolean.toString(wifiMode) + ".");
			return false;
		}
		try
		{
			StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
			long bytesAvailable = ((long)stat.getBlockSize() * (long)stat.getBlockCount());
			if(bytesAvailable - cloudItem.getSize() < 100 * MEGABYTE)
			{
				log(Name + ": Sync rejected " + cloudItem.getName() + " because there is not enough disk space. bytesAvailable = " + bytesAvailable);
				return false;
			}
		} 
		catch (Exception ex)
		{
			throw new RuntimeException("shouldSync(): Error determining free disk space: " + ex.toString());
		}
		if(localItem == null)
		{
			return true;
		} 
		else 
		{
			SyncFile localFile = new SyncFile(this.Path, localItem);
			if(!localFile.Exists()) 
			{
				log(Name + ": Sync accepted " + cloudItem.getName() + " because local file does not exist.");
				return true;
			}
			if(cloudItem.getCloudModified().after(localItem.CloudModified))
			{
				log(Name + ": Sync accepted " + cloudItem.getName() + " because item was modified '" + cloudItem.getCloudModified() + "' and the current item was modified '" + localItem.CloudModified + "'");
				return true;
			}
			if(cloudItem.getSize() != localFile.GetSize())
			{
				log(Name + ": Sync accepted " + cloudItem.getName() + " because sizes differed.");
				return true;
			}
		}
		return false;
	}
	private Boolean isFileTooLarge(long fileSize, boolean wifiMode)
	{
		if(wifiMode && MaxSize == -1)
			return false;
		else if(wifiMode)
			return (fileSize / KILOBYTE) > MaxSize;
		else if(!wifiMode && MobileMaxSize == -1)
			return false;
		else
			return (fileSize / KILOBYTE) > MobileMaxSize;
	}
	
	public boolean isEnabled()
	{
		return isWifiEnabled() || isMobileEnabled();
	}
	public boolean isWifiEnabled()
	{
		return MaxSize != 0;
	}
	public boolean isMobileEnabled()
	{
		return MobileMaxSize != 0;
	}
	
	@Override
	public String toString()
	{
		return "{ ID: " + ID + " Name: '" + Name + "' AccountID: " + AccountID + " MaxSize: " + MaxSize + " MobileMaxSize: " + MobileMaxSize + " LastRun: " + LastRun.toString() + " }";
	}
	
	private void log(String message)
	{
		log(message, false);
	}
	private void log(String message, Boolean quiet)
	{
		//if(!quiet) return;
		Log.v("TaskInfo", message);
	}
}
