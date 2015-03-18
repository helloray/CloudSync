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
import com.netortech.utilities.clouds.SkyDriveItem;

public class SkyDrive extends Cloud {
	private static final String SEPARATOR = com.netortech.utilities.clouds.SkyDrive.SEPARATOR;
	
	private static final String CLIENT_ID = "CLIENT ID"; 
	private static final String CLIENT_SECRET = "THIS IS A SECRET";
	
	private static final String ACCOUNT_DATA_REFRESH_TOKEN = "refreshToken";

	private static final String TASK_DATA_CLOUD_FOLDER = "cloudFolder";
	private static final String TASK_DATA_CLOUD_FOLDER_IDS = "cloudFolderIDs";
	
	private String refreshToken = null;
	private com.netortech.utilities.clouds.SkyDrive skydrive;
	
	protected SkyDrive() {}
	
	@Override
	public void setAccountData(List<AccountData> accountData) {
		this.refreshToken = AccountData.getByName(ACCOUNT_DATA_REFRESH_TOKEN, accountData).Data;
		skydrive = new com.netortech.utilities.clouds.SkyDrive(CLIENT_ID, CLIENT_SECRET, refreshToken);
		skydrive.isAuthenticated(true);
	}

	@Override public boolean usesOAuth() { return true; }
	@Override public boolean hasContainers() { return false; }
	@Override public boolean hasFolders() {	return true; }

	@Override
	public void initializeAccountDataFields(List<AccountData> accountData) {
		if(AccountData.getByName(ACCOUNT_DATA_REFRESH_TOKEN, accountData) == null) accountData.add(new AccountData(ACCOUNT_DATA_REFRESH_TOKEN));
	}

	@Override
	public void initializeTaskDataFields(List<TaskData> taskData) {
		if(TaskData.getByName(TASK_DATA_CLOUD_FOLDER, taskData) == null) taskData.add(new TaskData(TASK_DATA_CLOUD_FOLDER));
		if(TaskData.getByName(TASK_DATA_CLOUD_FOLDER_IDS, taskData) == null) taskData.add(new TaskData(TASK_DATA_CLOUD_FOLDER_IDS));
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
		TaskData.getByName(TASK_DATA_CLOUD_FOLDER, taskData).Data = folder.getSeparatedNames(FolderItem.DEFAULT_FOLDER_SEPARATOR);
		TaskData.getByName(TASK_DATA_CLOUD_FOLDER_IDS, taskData).Data = folder.getSeparatedKeys(FolderItem.DEFAULT_KEY_SEPARATOR);
	}
	@Override
	public FolderItem getCloudFolder(List<TaskData> taskData) {
		initializeTaskDataFields(taskData);
		return FolderItem.construct(TaskData.getByName(TASK_DATA_CLOUD_FOLDER, taskData).Data, TaskData.getByName(TASK_DATA_CLOUD_FOLDER_IDS, taskData).Data);
	}
	
	@Override public void setCloudContainer(String container, List<TaskData> taskData) { throw new UnsupportedOperationException(); }
	@Override public String getCloudContainer(List<TaskData> taskData) { throw new UnsupportedOperationException(); }

	@Override 
	public Uri getOAuthCallback() 
	{  
		return Uri.parse(com.netortech.utilities.clouds.SkyDrive.SKYDRIVE_OAUTH_DESKTOP_CALLBACK);
	}
	@Override
	public Uri getOAuthAuthorization(Uri callback) {
		if(skydrive == null)
			skydrive = new com.netortech.utilities.clouds.SkyDrive(CLIENT_ID, CLIENT_SECRET);
		return skydrive.authorize(Uri.parse(skydrive.SKYDRIVE_OAUTH_DESKTOP_CALLBACK));
	}

	@Override
	public void setOAuthAuthorizationData(List<AccountData> accountData, String callback) {
		skydrive.verifyAuthorize(Uri.parse(callback), Uri.parse(skydrive.SKYDRIVE_OAUTH_DESKTOP_CALLBACK));
		initializeAccountDataFields(accountData);
		AccountData.getByName(ACCOUNT_DATA_REFRESH_TOKEN, accountData).Data = skydrive.getRefreshToken();
	}

	@Override public List<String> getContainers(List<TaskData> taskData) { throw new UnsupportedOperationException(); }
	@Override
	public Map<String, CloudItem> getFolders(List<TaskData> taskData, String path) {
		Map<String, CloudItem> ret = new LinkedHashMap<String, CloudItem>();
		for(SkyDriveItem item : skydrive.getFolders(path))
		{
			if(item.Name.startsWith(SEPARATOR)) item.Name = item.Name.substring(SEPARATOR.length());
			ret.put(item.ID, new CloudItem(item.Name, item.getFolderName(), item.ID, item.Size, item.LastModified));
		}
		return ret;
	}
	@Override
	public Map<String, CloudItem> getFiles(List<TaskData> taskData) {
		Map<String, CloudItem> ret = new HashMap<String, CloudItem>();
		String folderName = TaskData.getByName(TASK_DATA_CLOUD_FOLDER, taskData).Data;
		for(SkyDriveItem item : skydrive.getFiles(folderName, true))
		{
			CloudItem cloudItem = new CloudItem(item.Name, item.getFolderName(), item.ID, item.Size, item.LastModified);
			ret.put(item.Name, cloudItem);
		}
		return ret;
	}
	@Override
	public CloudItem downloadFile(String key, SyncFile fullPath, List<TaskData> taskData, DownloadListener listener) {
		CloudItem ret = null;
		SkyDriveItem item = skydrive.getFile(key, true);
		if(item == null) throw new RuntimeException("Key '" + key + "' does not exist.");
		
		FileOutputStream f = null;
		try
		{
			f = fullPath.getOutputStream();
		}
		catch(Exception ex)
		{
			throw new RuntimeException("Failed to create file stream for file '" + fullPath + "': " + ex);
		}
		
		if(listener != null) listener.DownloadFile(item.getFileName());
		try
		{
			writeStream(item.Data, f, item.Size, listener);
		    if(listener != null) listener.Finished();
		    log("File saved to '" + fullPath + "'.", true);
		    ret = new CloudItem(item.Name, item.getFolderName(), item.ID, item.Size, item.LastModified);
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

	@Override public boolean isAuthenticated(boolean throwException) { return skydrive.isAuthenticated(throwException); }
}
