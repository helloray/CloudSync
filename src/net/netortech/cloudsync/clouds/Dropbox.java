package net.netortech.cloudsync.clouds;

import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.netortech.cloudsync.data.AccountData;
import net.netortech.cloudsync.data.TaskData;
import android.net.Uri;

import com.netortech.utilities.FolderItem;
import com.netortech.utilities.ListItem;
import com.netortech.utilities.clouds.DropboxItem;

public final class Dropbox extends Cloud {
	private static final String CONSUMER_KEY = "consumerKey";
	
	private static final String DONT = "THIS";
	private static final String LOOK = "IS";
	private static final String AT = "A";
	private static final String ME = "SECRET";
	
	private static final String ACCOUNT_DATA_TOKEN_KEY = "tokenKey";
	private static final String ACCOUNT_DATA_TOKEN_SECRET = "tokenSecret";
	
	private static final String TASK_DATA_CLOUD_FOLDER = "cloudFolder";
	
	String tokenKey = null;
	String tokenSecret = null;
	com.netortech.utilities.clouds.Dropbox db;
	
	protected Dropbox() {}
	
	@Override
	public void setAccountData(List<AccountData> accountData)
	{
		this.tokenKey = AccountData.getByName(ACCOUNT_DATA_TOKEN_KEY, accountData).Data;
		this.tokenSecret = AccountData.getByName(ACCOUNT_DATA_TOKEN_SECRET, accountData).Data;
		db = new com.netortech.utilities.clouds.Dropbox(CONSUMER_KEY, (DONT+LOOK+AT+ME).replace("a", "").replace("7", "").replace("x", ""), tokenKey, tokenSecret);
		db.isAuthenticated(true);
	}
	
	@Override public boolean usesOAuth() { return true; }
	@Override public boolean hasContainers() { return false; }
	@Override public boolean hasFolders() { return true; }
	
	@Override
	public void initializeAccountDataFields(List<AccountData> accountData) {
		if(AccountData.getByName(ACCOUNT_DATA_TOKEN_KEY, accountData) == null) accountData.add(new AccountData(ACCOUNT_DATA_TOKEN_KEY));
		if(AccountData.getByName(ACCOUNT_DATA_TOKEN_SECRET, accountData) == null) accountData.add(new AccountData(ACCOUNT_DATA_TOKEN_SECRET));
	}
	@Override
	public void initializeTaskDataFields(List<TaskData> taskData) {
		if(TaskData.getByName(TASK_DATA_CLOUD_FOLDER, taskData) == null) taskData.add(new TaskData(TASK_DATA_CLOUD_FOLDER));
	}
	
	@Override public List<ListItem> getAccountListItems() { throw new UnsupportedOperationException(); }
	@Override
	public List<ListItem> getTaskListItems() {
		List<ListItem> ret = new LinkedList<ListItem>();
		ret.add(new ListItem(-1, TASK_DATA_CLOUD_FOLDER, "Folder Name:"));
		return ret;
	}
	
	@Override
	public void setCloudFolder(FolderItem folder, List<TaskData> taskData) {
		initializeTaskDataFields(taskData);
		log("setCloudFolder(): Saving folder Name: '" + folder.Name + "' Key: '" + folder.Key + "'", true);
		TaskData.getByName(TASK_DATA_CLOUD_FOLDER, taskData).Data = folder.Key;
	}
	@Override
	public FolderItem getCloudFolder(List<TaskData> taskData) {
		initializeTaskDataFields(taskData);
		return FolderItem.construct(TaskData.getByName(TASK_DATA_CLOUD_FOLDER, taskData).Data);
	}

	@Override public void setCloudContainer(String container, List<TaskData> taskData) { throw new UnsupportedOperationException(); }
	@Override public String getCloudContainer(List<TaskData> taskData) { throw new UnsupportedOperationException(); }

	@Override 
	public Uri getOAuthCallback() 
	{  
		return Uri.parse("http://android.netortech.com/resources/OAuthCallback.aspx");
	}
	@Override
	public void setOAuthAuthorizationData(List<AccountData> accountData, String callback)
	{
		db.hasAccessToken(true);
		initializeAccountDataFields(accountData);
		AccountData.getByName(ACCOUNT_DATA_TOKEN_KEY, accountData).Data = db.getTokenKey();
		AccountData.getByName(ACCOUNT_DATA_TOKEN_SECRET, accountData).Data = db.getTokenSecret();
	}
	@Override public Uri getOAuthAuthorization(Uri callback) 
	{ 
		if(db == null)
			db = new com.netortech.utilities.clouds.Dropbox(CONSUMER_KEY, (DONT+LOOK+AT+ME).replace("a", "").replace("7", "").replace("x", ""));
		return db.authorize(callback); 
	}
	
	@Override public List<String> getContainers(List<TaskData> taskData) { throw new UnsupportedOperationException(); }
	@Override
	public Map<String, CloudItem> getFolders(List<TaskData> taskData, String path) {
		Map<String, CloudItem> ret = new LinkedHashMap<String, CloudItem>();
		for(DropboxItem item : db.getFolders(path))
		{
			ret.put(item.Name, new CloudItem(item.getFileName(), item.getFolderName(), item.Name, item.Size, item.LastModified));
		}
		return ret;
	}
	@Override
	public Map<String, CloudItem> getFiles(List<TaskData> taskData) {
		Map<String, CloudItem> ret = new HashMap<String, CloudItem>();
		String folderName = TaskData.getByName(TASK_DATA_CLOUD_FOLDER, taskData).Data;
		for(DropboxItem item : db.getFiles(folderName, true))
		{
			CloudItem cloudItem = new CloudItem(item.getFileName(), item.getFolderName(), item.Name, item.Size, item.LastModified);
			ret.put(item.Name, cloudItem);
			// log("Dropbox.getFiles(): Putting '" + item.Name + "' into list.");
			// log("Cloud Item: " + cloudItem.toString());
		}
		return ret;
	}
	@Override
	public CloudItem downloadFile(String key, SyncFile fullPath, List<TaskData> taskData, DownloadListener listener) {
		CloudItem ret = null;
		DropboxItem dbItem = db.getFile(key, true);
		if(dbItem == null) throw new RuntimeException("Key '" + key + "' does not exist.");
		
		FileOutputStream f = null;
		try
		{
			f = fullPath.getOutputStream();
		}
		catch(Exception ex)
		{
			throw new RuntimeException("Failed to create file stream for file '" + fullPath + "': " + ex);
		}
		
		if(listener != null) listener.DownloadFile(dbItem.getFileName());
		try
		{
			writeStream(dbItem.Data, f, dbItem.Size, listener);
		    if(listener != null) listener.Finished();
		    log("File saved to '" + fullPath + "'.", true);
		    ret = new CloudItem(dbItem.getFileName(), dbItem.getFolderName(), dbItem.Name, dbItem.Size, dbItem.LastModified);
		}
		catch(Exception ex)
		{
			throw new RuntimeException("Failed to write file '" + fullPath + "': " + ex);
		}
		finally
		{
			if(listener != null) listener.Finished();
		}
		
		return ret;
	}

	@Override public boolean isAuthenticated(boolean throwException) { return db.isAuthenticated(throwException); }
}
