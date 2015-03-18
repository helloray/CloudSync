package net.netortech.cloudsync.clouds;

import java.util.List;
import java.util.Map;

import net.netortech.cloudsync.data.AccountData;
import net.netortech.cloudsync.data.TaskData;
import android.net.Uri;

import com.netortech.utilities.FolderItem;
import com.netortech.utilities.ListItem;

public interface ICloud {
	public void setAccountData(List<AccountData> accountData);

	public boolean usesOAuth();
	public boolean hasContainers();
	public boolean hasFolders();
	
	public void initializeAccountDataFields(List<AccountData> accountData);
	public void initializeTaskDataFields(List<TaskData> taskData);
	
	public List<ListItem> getAccountListItems();
	public List<ListItem> getTaskListItems();

	public void setCloudFolder(FolderItem folder, List<TaskData> taskData);
	public FolderItem getCloudFolder(List<TaskData> taskData);

	public void setCloudContainer(String container, List<TaskData> taskData);
	public String getCloudContainer(List<TaskData> taskData);
	
	public Uri getOAuthCallback();
	public Uri getOAuthAuthorization(Uri callback);
	public void setOAuthAuthorizationData(List<AccountData> accountData, String callback);
	
	public List<String> getContainers(List<TaskData> taskData);
	public Map<String, CloudItem> getFolders(List<TaskData> taskData, String path);
	public Map<String, CloudItem> getFiles(List<TaskData> taskData);
	public CloudItem downloadFile(String key, SyncFile fullPath, List<TaskData> taskData, DownloadListener listener);

	public boolean isAuthenticated();
	public boolean isAuthenticated(boolean throwException);
}
