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
import com.netortech.utilities.clouds.AmazonS3;
import com.netortech.utilities.clouds.S3Key;

public class S3 extends Cloud {
	private static String ACCOUNT_DATA_CLOUD_ACCOUNT = "account";
	private static String ACCOUNT_DATA_CLOUD_KEY = "key";
	private static String ACCOUNT_DATA_PROXY_URL = "proxyUrl";
	private static String ACCOUNT_DATA_PROXY_USERNAME = "proxyUsername";
	private static String ACCOUNT_DATA_PROXY_PASSWORD = "proxyPassword";
	
	private static String TASK_DATA_CLOUD_CONTAINER = "cloudContainer";
	private static String TASK_DATA_CLOUD_FOLDER = "cloudFolder";

	AmazonS3 s3;
	private boolean proxyMode = false;
	
	protected S3() {}
	protected S3(boolean proxyMode) { this.proxyMode = proxyMode; }
	
	@Override
	public void setAccountData(List<AccountData> accountData)
	{
		if(!proxyMode)
		{
			String awsKey = AccountData.getByName(ACCOUNT_DATA_CLOUD_ACCOUNT, accountData).Data;
			String awsSecret = AccountData.getByName(ACCOUNT_DATA_CLOUD_KEY, accountData).Data;
			s3 = new AmazonS3(awsKey, awsSecret);
		} else {
			String username = AccountData.getByName(ACCOUNT_DATA_PROXY_USERNAME, accountData).Data;
			String password = AccountData.getByName(ACCOUNT_DATA_PROXY_PASSWORD, accountData).Data;
			String proxyUrl = AccountData.getByName( ACCOUNT_DATA_PROXY_URL, accountData).Data;
			s3 = new AmazonS3(username, password, proxyUrl);
		}

		s3.isAuthenticated(true);
	}
	
	@Override public boolean usesOAuth() { return false; }
	@Override public boolean hasContainers() { return true; }
	@Override public boolean hasFolders() { return true; }
	
	@Override
	public void initializeAccountDataFields(List<AccountData> accountData) 
	{
		if(!proxyMode)
		{
			if(AccountData.getByName(ACCOUNT_DATA_CLOUD_ACCOUNT, accountData) == null) accountData.add(new AccountData(ACCOUNT_DATA_CLOUD_ACCOUNT));
			if(AccountData.getByName(ACCOUNT_DATA_CLOUD_KEY, accountData) == null) accountData.add(new AccountData(ACCOUNT_DATA_CLOUD_KEY));
		} else {
			if(AccountData.getByName(ACCOUNT_DATA_PROXY_URL, accountData) == null) accountData.add(new AccountData(ACCOUNT_DATA_PROXY_URL));
			if(AccountData.getByName(ACCOUNT_DATA_PROXY_USERNAME, accountData) == null) accountData.add(new AccountData(ACCOUNT_DATA_PROXY_USERNAME));
			if(AccountData.getByName(ACCOUNT_DATA_PROXY_PASSWORD, accountData) == null) accountData.add(new AccountData(ACCOUNT_DATA_PROXY_PASSWORD));
		}
	}
	@Override
	public void initializeTaskDataFields(List<TaskData> taskData) {
		if(TaskData.getByName(TASK_DATA_CLOUD_CONTAINER, taskData) == null) taskData.add(new TaskData(TASK_DATA_CLOUD_CONTAINER));
		if(TaskData.getByName(TASK_DATA_CLOUD_FOLDER, taskData) == null) taskData.add(new TaskData(TASK_DATA_CLOUD_FOLDER));
	}
	
	@Override
	public List<ListItem> getAccountListItems() {
		List<ListItem> ret = new LinkedList<ListItem>();
		if(!proxyMode)
		{
			ret.add(new ListItem(-1, ACCOUNT_DATA_CLOUD_ACCOUNT, "AWS Key:"));
			ret.add(new ListItem(-1, ACCOUNT_DATA_CLOUD_KEY, "AWS Secret:"));
		} else {
			ret.add(new ListItem(-1, ACCOUNT_DATA_PROXY_URL, "Proxy Signing Service URL:"));
			ret.add(new ListItem(-1, ACCOUNT_DATA_PROXY_USERNAME, "Username:"));
			ret.add(new ListItem(-1, ACCOUNT_DATA_PROXY_PASSWORD, "Password:"));
		}
		return ret;
	}
	@Override
	public List<ListItem> getTaskListItems() {
		List<ListItem> ret = new LinkedList<ListItem>();
		ret.add(new ListItem(-1, TASK_DATA_CLOUD_CONTAINER, "Container Name:"));
		ret.add(new ListItem(-1, TASK_DATA_CLOUD_FOLDER, "Folder Name:"));
		return ret;
	}
	
	@Override
	public void setCloudFolder(FolderItem folder, List<TaskData> taskData) {
		initializeTaskDataFields(taskData);
		TaskData.getByName(TASK_DATA_CLOUD_FOLDER, taskData).Data = folder.Key;
	}
	@Override
	public FolderItem getCloudFolder(List<TaskData> taskData) {
		initializeTaskDataFields(taskData);
		return FolderItem.construct(TaskData.getByName(TASK_DATA_CLOUD_FOLDER, taskData).Data);
	}
	
	@Override
	public void setCloudContainer(String container, List<TaskData> taskData) {
		initializeTaskDataFields(taskData);
		TaskData.getByName(TASK_DATA_CLOUD_CONTAINER, taskData).Data = container;
	}
	@Override
	public String getCloudContainer(List<TaskData> taskData) {
		initializeTaskDataFields(taskData);
		return TaskData.getByName(TASK_DATA_CLOUD_CONTAINER, taskData).Data;
	}
	
	@Override public Uri getOAuthCallback() { throw new UnsupportedOperationException(); }
	@Override public Uri getOAuthAuthorization(Uri callback) { throw new UnsupportedOperationException(); }
	@Override public void setOAuthAuthorizationData(List<AccountData> accountData, String callback) { throw new UnsupportedOperationException(); }

	@Override public List<String> getContainers(List<TaskData> taskData) { return s3.getBuckets(); }
	@Override
	public Map<String, CloudItem> getFolders(List<TaskData> taskData, String path)
	{
		int pathSplits, keySplits;
		String bucketName = TaskData.getByName(TASK_DATA_CLOUD_CONTAINER, taskData).Data;
		List<S3Key> keys = s3.getKeys(bucketName, path);
		
		Map<String, CloudItem> folders = new LinkedHashMap<String, CloudItem>();
		
		pathSplits = path.split(AmazonS3.SEPARATOR).length;
		for(S3Key key : keys) if(key.isFolder())
		{
			keySplits = key.Key.split(AmazonS3.SEPARATOR).length;
			
			log("key '" + key.Key + "' pathSplits: " + pathSplits + " keySplits: " + keySplits + " path: '" + path + "'");
			
			if(!path.equals("") && pathSplits == keySplits)
				continue; // Same folder
			else if(path.equals("") && pathSplits < keySplits)
				continue; // Grandchild folder
			else if(pathSplits + 1 < keySplits)
				continue; // Grandchild folder
				
			String folderName = key.getFolderName();
			if(folderName.endsWith(AmazonS3.SEPARATOR)) folderName = folderName.substring(0, folderName.length() - AmazonS3.SEPARATOR.length());
			if(folderName.lastIndexOf(AmazonS3.SEPARATOR) != -1) folderName = folderName.substring(folderName.lastIndexOf(AmazonS3.SEPARATOR) + AmazonS3.SEPARATOR.length());
			folders.put(key.Key, new CloudItem(folderName, key.getFolderName(), key.Key, key.Size, key.LastModified));
		}
		return folders;
	}
	@Override
	public Map<String, CloudItem> getFiles(List<TaskData> taskData)
	{
		String bucketName = TaskData.getByName(TASK_DATA_CLOUD_CONTAINER, taskData).Data;
		String folderName = TaskData.getByName(TASK_DATA_CLOUD_FOLDER, taskData).Data;
		
		log("Gathering items from bucket '" + bucketName + "'");
		Map<String, CloudItem> ret = new HashMap<String, CloudItem>();
		
    	List<S3Key> keys = s3.getKeys(bucketName, folderName);
        if(keys == null) return ret;
        
        CloudItem item;
        for(S3Key key : keys)
    	{
        	if(key.isFolder()) continue;
        	item = new CloudItem(key.getFileName(), key.getFolderName(), key.Key, key.Size, key.LastModified);
			ret.put(key.Key, item);
    	}
        
        return ret;
	}
	@Override
	public CloudItem downloadFile(String key, SyncFile fullPath, List<TaskData> taskData, DownloadListener listener) {
		String bucketName = TaskData.getByName(TASK_DATA_CLOUD_CONTAINER, taskData).Data;
		CloudItem ret = null;
		
		S3Key s3key = s3.getKey(bucketName, key);
		if(s3key == null) throw new RuntimeException("Key '" + key + "' does not exist.");
		
		FileOutputStream f = null;
		try
		{
			f = fullPath.getOutputStream();
		}
		catch(Exception ex)
		{
			throw new RuntimeException("Failed to create file stream for file '" + fullPath + "': " + ex);
		}
		
		if(listener != null) listener.DownloadFile(s3key.getFileName());
		try
		{
			writeStream(s3key.Data, f, s3key.Size, listener);
		    if(listener != null) listener.Finished();
		    log("File saved to '" + fullPath + "'.", true);
		    ret = new CloudItem(s3key.getFileName(), s3key.getFolderName(), s3key.Key, s3key.Size, s3key.LastModified);
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

	@Override public boolean isAuthenticated() { return isAuthenticated(false); }
	@Override public boolean isAuthenticated(boolean throwException) { return s3.isAuthenticated(throwException); }
}
