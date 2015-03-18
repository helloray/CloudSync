package net.netortech.cloudsync.clouds;

import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.netortech.cloudsync.data.AccountData;
import net.netortech.cloudsync.data.TaskData;
import android.net.Uri;

import com.netortech.utilities.FolderItem;
import com.netortech.utilities.ListItem;
import com.netortech.utilities.clouds.AzureBlob;

public final class Azure extends Cloud {
	private static String ACCOUNT_DATA_CLOUD_ACCOUNT = "account";
	private static String ACCOUNT_DATA_CLOUD_KEY = "key";
	private static String ACCOUNT_DATA_PROXY_URL = "proxyUrl";
	private static String ACCOUNT_DATA_PROXY_USERNAME = "proxyUsername";
	private static String ACCOUNT_DATA_PROXY_PASSWORD = "proxyPassword";
	
	private static String TASK_DATA_CLOUD_CONTAINER = "cloudContainer";
	
	com.netortech.utilities.clouds.Azure azure;
	private boolean proxyMode = false;
	
	protected Azure() {}
	protected Azure(boolean proxyMode) { this.proxyMode = proxyMode; }
	
	@Override
	public void setAccountData(List<AccountData> accountData)
	{
		if(!proxyMode)
		{
			String account = AccountData.getByName(ACCOUNT_DATA_CLOUD_ACCOUNT, accountData).Data;
			String key = AccountData.getByName(ACCOUNT_DATA_CLOUD_KEY, accountData).Data;
			azure = new com.netortech.utilities.clouds.Azure(account, key);
		} else {
			String username = AccountData.getByName(ACCOUNT_DATA_PROXY_USERNAME, accountData).Data;
			String password = AccountData.getByName(ACCOUNT_DATA_PROXY_PASSWORD, accountData).Data;
			String proxyUrl = AccountData.getByName( ACCOUNT_DATA_PROXY_URL, accountData).Data;
			azure = new com.netortech.utilities.clouds.Azure(username, password, proxyUrl);
		}
		
		azure.isAuthenticated(true);
	}

	@Override public boolean usesOAuth() { return false; }
	@Override public boolean hasContainers() { return true; }
	@Override public boolean hasFolders() { return false; }

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
	}
	
	@Override
	public List<ListItem> getAccountListItems() {
		List<ListItem> ret = new LinkedList<ListItem>();
		if(!proxyMode)
		{
			ret.add(new ListItem(-1, ACCOUNT_DATA_CLOUD_ACCOUNT, "Storage Account Name:"));
			ret.add(new ListItem(-1, ACCOUNT_DATA_CLOUD_KEY, "Storage Account Access Key:"));
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
		return ret;
	}

	@Override public void setCloudFolder(FolderItem folder, List<TaskData> taskData) { throw new UnsupportedOperationException(); }
	@Override public FolderItem getCloudFolder(List<TaskData> taskData) { throw new UnsupportedOperationException(); }

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

	@Override public List<String> getContainers(List<TaskData> taskData) { return azure.getContainers(); }
	@Override public Map<String, CloudItem> getFolders(List<TaskData> taskData, String path) { throw new UnsupportedOperationException(); }
	@Override
	public Map<String, CloudItem> getFiles(List<TaskData> taskData) {
		Map<String, CloudItem> ret = new HashMap<String, CloudItem>();
		String container = TaskData.getByName(TASK_DATA_CLOUD_CONTAINER, taskData).Data;
		
		for(AzureBlob blob : azure.getBlobs(container))
			ret.put(blob.Name, new CloudItem(blob.Name, "", blob.Name, blob.Size, blob.LastModified));
  		return ret;
	}
	@Override
	public CloudItem downloadFile(String key, SyncFile fullPath, List<TaskData> taskData, DownloadListener listener) {
		String container = TaskData.getByName(TASK_DATA_CLOUD_CONTAINER, taskData).Data;
		CloudItem ret = null;
		AzureBlob blob;
		blob = azure.getBlob(container, key);

		FileOutputStream f = null;
		try
		{
			f = fullPath.getOutputStream();
		}
		catch(Exception ex)
		{
			throw new RuntimeException("Failed to create file stream for file '" + fullPath + "': " + ex);
		}
		
		if(listener != null) listener.DownloadFile(key);
		try
		{
			writeStream(blob.Data, f, blob.Size, listener);
		    if(listener != null) listener.Finished();
		    ret = new CloudItem(key, "", key, blob.Size, blob.LastModified);
		    log("File saved to '" + fullPath + "'.", true);
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
	
	@Override public boolean isAuthenticated(boolean throwException) { return azure.isAuthenticated(throwException); }
}
