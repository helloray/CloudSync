package net.netortech.cloudsync;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.netortech.cloudsync.clouds.Cloud;
import net.netortech.cloudsync.clouds.CloudItem;
import net.netortech.cloudsync.clouds.ICloud;
import net.netortech.cloudsync.clouds.SyncFile;
import net.netortech.cloudsync.data.AccountData;
import net.netortech.cloudsync.data.AccountInfo;
import net.netortech.cloudsync.data.EngineInfo;
import net.netortech.cloudsync.data.SettingsDB;
import net.netortech.cloudsync.data.TaskData;
import net.netortech.cloudsync.data.TaskInfo;
import net.netortech.cloudsync.data.TaskItem;
import net.netortech.cloudsync.ui.wizard.CredentialsActivity;
import net.netortech.cloudsync.ui.wizard.OAuthActivity;
import net.netortech.cloudsync.ui.wizard.WizardStepActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import com.netortech.utilities.MediaStoreHelper;
import com.netortech.utilities.StringHelper;
import com.netortech.utilities.TimerService;
import com.netortech.utilities.hardware.Power;
import com.netortech.utilities.hardware.Wifi;

public class CloudSyncService extends TimerService {
	private static final int NORMAL_POLL_PERIOD = 15000;
	private static final int UNPOWERED_POLL_PERIOD = 60000;
	private static final int BANDWIDTH_SAVER_POLL_PERIOD = 60000;
	
	private static final int FOREGROUND_NOTIFICATION_ID = 0;
	
	List<Long> failedAccountsNotified = new LinkedList<Long>();
	ServiceNotification notification;

	boolean batteryMode = false;
	boolean wifiMode = true; 
	private Context dbContext = null;
	
	@Override
	public void onServiceCreated()
	{
		initForeground();
	}
	
	@Override
	public void onServiceStarted(Intent intent)
	{
		CloudSyncActivity.signalServiceStarted();
	}
	public void onServiceStoppedRequest()
	{
		if(notification != null) notification.SetText(getString(R.string.notification_service_finishing), "");
		CloudSyncActivity.signalServiceStopping();
	}
	@Override
	public void onServiceStopped()
	{
		this.stopForegroundCompat(R.layout.main);
		CloudSyncActivity.signalServiceStopped();
	}
	private void initForeground()
	{
		notification = new ServiceNotification(this,
				getString(R.string.notification_service_running),
				getString(R.string.notification_service_running),
				PendingIntent.getActivity(this, 0, new Intent(this, CloudSyncActivity.class), Intent.FLAG_ACTIVITY_CLEAR_TOP));
		startForegroundCompat(R.layout.main, notification.getNotification());
		notification.SetNotificationManager(getNotificationManager(), R.layout.main);
	}
	private void powerSaver()
	{
		if(Power.isConnected() && batteryMode)
		{
			log("task(): Switching from battery mode to powered mode.", true);
			batteryMode = false;
			setPollPeriod(wifiMode ? NORMAL_POLL_PERIOD : BANDWIDTH_SAVER_POLL_PERIOD);
			if(wifiMode) notification.SetStatus("");
		} else if(!Power.isConnected() && !batteryMode) {
			log("task(): Switching from powered mode to battery mode.", true);
			batteryMode = true;
			setPollPeriod(wifiMode ? UNPOWERED_POLL_PERIOD : BANDWIDTH_SAVER_POLL_PERIOD); // Bandwidth save takes precedence
			if(wifiMode) notification.SetStatus(getString(R.string.notification_service_powersave));
		}
	}
	private boolean bandwidthSaver()
	{
		if(!Wifi.isConnected() && wifiMode)
		{
			log("task(): Switching from wifi mode to bandwidth saving mode.", true);
			wifiMode = false;
			setPollPeriod(BANDWIDTH_SAVER_POLL_PERIOD); // Bandwidth save takes precedence
			notification.SetStatus(getString(R.string.notification_service_bandwidthsave));
		} else if(Wifi.isConnected() && !wifiMode){
			log("task(): Switching from bandwidth saving mode to wifi mode.", true);
			wifiMode = true;
			setPollPeriod(!batteryMode ? NORMAL_POLL_PERIOD : UNPOWERED_POLL_PERIOD);
			notification.SetStatus(batteryMode ? getString(R.string.notification_service_powersave) : "");
		}
		return !wifiMode;
	}
	@Override
	protected void task()
	{
		log("Starting tasks. Wifi Connected:" + Wifi.isConnected() + " WifiMode:" + wifiMode + " Power Connected:" + Power.isConnected() + " BatteryMode:" + batteryMode, true);
		try
		{
			List<TaskInfo> tasks = SettingsDB.getAllTaskInfo(getDBContext());
			for(TaskInfo t : tasks)
			{
				bandwidthSaver();
				powerSaver();
				if(!t.isEnabled())
				{
					log("Task '" + t.Name + "' disabled. Skipping.", true);
					continue;
				}
				if(!Wifi.isConnected() && t.MobileMaxSize == 0)
				{
					log("Mobile mode not enabled for '" + t.Name + "'. Skipping.", true);
					continue;
				}
				if(!isRunning())
				{
					break;
				}
				SyncTask(t);
			}
		}
		catch(IllegalStateException ex)
		{
			log("task(): " + ex, true);
		}
	}
	private void SyncTask(TaskInfo t)
	{
		log(t.Name + ": Starting task.", true);
		AccountInfo account = SettingsDB.getAccountInfo(t.AccountID, getDBContext());
		if(account == null || account.ID == -1 || account.Failures > AccountInfo.MAX_FAILURES_COUNT)
		{
			if(account == null || account.ID == -1)
				log(t.Name + ": Account ID " + t.AccountID + " does not exist. Aborting.", true);
			else if(!account.Enabled)
				log(t.Name + ": Account '" + account.Name + "' is disabled. Aborting.", true);
			else if(account.Failures > AccountInfo.MAX_FAILURES_COUNT)
			{
				log(t.Name + ": Account '" + account.Name + "' has failed too many times.", true);
				addAccountFailure(account);
			}
			return;
		}
		removeAccountFailure(account);
		
		List<TaskData> taskData = SettingsDB.getAllTaskData(t.ID, getDBContext());
		if(t.Path == null || t.Path.trim().equals(""))
		{
			log(t.Name + ": No path provided. Aborting.", true);
			return;
		}
		ICloud cloud = Cloud.getCloud(account, getDBContext());
		if(cloud == null)
		{
			log(t.Name + ": Could not connect to cloud. Failure Count: " + account.Failures + " Aborting.", true);
			return;
		}
		Map<String, TaskItem> syncedItems = new HashMap<String, TaskItem>();
		String syncedItemsList = "";
		for(TaskItem item : SettingsDB.getAllTaskItems(t.ID, getDBContext()))
		{
			if(!syncedItemsList.equals("")) syncedItemsList += ", ";
			syncedItemsList += item.Key;
			syncedItems.put(item.Key, item);
		}
		log(t.Name + ": Synced items in db: { " + syncedItemsList + " }");
		
		Map<String, SyncFile> filesToDownload = new HashMap<String, SyncFile>();
		Map<String, SyncFile> localFilesToDelete = new HashMap<String, SyncFile>();
		Map<String, CloudItem> cloudItems;
		try
		{
			 cloudItems = cloud.getFiles(taskData);
		} 
		catch(Exception ex)
		{
			String trace = "";
			for(StackTraceElement e : ex.getStackTrace()) trace += e.toString() + " ";
			log(t.Name + ": " + ex + ", trace: " + trace + " Aborting.", true);
			return;
		}

		for(String key : syncedItems.keySet())
		{
			if(!cloudItems.containsKey(key))
			{
				log(t.Name + ": Queuing '" + syncedItems.get(key) + "' for deletion.", true);
				localFilesToDelete.put(key, new SyncFile(t.Path, syncedItems.get(key)));
			}
			else 
			{
				CloudItem item = cloudItems.get(key);
				if(t.shouldSync(item, syncedItems.get(key), Wifi.isConnected()))
				{
					log(t.Name + ": Queuing '" + key + "' for download.", true);
					filesToDownload.put(key, new SyncFile(t.Path, syncedItems.get(key)));
				}
			}
		}
		log(t.Name + ": Found " + cloudItems.size() + " cloud items.");
		for(String key : cloudItems.keySet())
		{
			CloudItem item = cloudItems.get(key);
			if(!syncedItems.containsKey(key) && t.shouldSync(item, null, Wifi.isConnected()))
			{
				log(t.Name + ": Queuing '" + key + "' for download.", true);
				filesToDownload.put(key, new SyncFile(t.Path, item));
			} 
		}
		performTransfers(filesToDownload, localFilesToDelete, syncedItems, t, taskData, cloud);
	}
	private void performTransfers(
			Map<String, SyncFile> filesToDownload, 
			Map<String, SyncFile> localFilesToDelete, 
			Map<String, TaskItem> syncedItems, 
			TaskInfo t,
			List<TaskData> taskData,
			ICloud cloud)
	{
		boolean wifiStatus = Wifi.isConnected(); // Save this value for comparison later.
		List<String> operationFailures = new LinkedList<String>();
		List<String> pathsToScan = new LinkedList<String>();
		
		log(t.Name + ": Sync logic complete. " + filesToDownload.size() + " files to download, " +
				localFilesToDelete.size() + " files to delete.");
		for(String deleteLocalKey : localFilesToDelete.keySet())
		{
			try
			{
				deleteLocalFile(localFilesToDelete.get(deleteLocalKey));
				SettingsDB.Delete(syncedItems.get(deleteLocalKey), getDBContext());
				pathsToScan.add(localFilesToDelete.get(deleteLocalKey).toString());
			} catch (Exception ex)
			{
				operationFailures.add("Delete " + deleteLocalKey + " from task " + t.Name + " failed: " + ex.toString());
			}
		}
		MediaStoreHelper.RemoveAllForPaths(pathsToScan, this);
		
		pathsToScan.clear();
		int filesDownloaded = 0;
		for(String downloadKey : filesToDownload.keySet())
		{
			SyncFile localFile = null;
			if(!isRunning())
			{
				int filesSkipped = filesToDownload.size() - (operationFailures.size() + filesDownloaded);
				log(t.Name + ": service terminated. Skipping " + Integer.toString(filesSkipped) + " files queued for download.");
				break;
			}
			try
			{
				localFile = filesToDownload.get(downloadKey);
				CloudItem cloudItem = downloadFile(cloud, downloadKey, localFile, taskData);
				
				if(cloudItem != null)
				{
					TaskItem taskItem;
					if(syncedItems.containsKey(downloadKey)) taskItem = syncedItems.get(downloadKey);
					else taskItem = new TaskItem();
					taskItem.TaskID = t.ID;
					taskItem.Key = downloadKey;
					taskItem.Path = cloudItem.getPath();
					taskItem.Name = cloudItem.getName();
					taskItem.CloudModified = cloudItem.getCloudModified();
					taskItem.LocalModified = localFile.lastModified();
					log("About to save task item: " + taskItem);
					SettingsDB.Save(taskItem, getDBContext());
					pathsToScan.add(filesToDownload.get(downloadKey).toString());
					filesDownloaded++;
				} else {
					throw new RuntimeException("cloudItem is null.");
				}
			} catch (Exception ex)
			{
				if(Wifi.isConnected() != wifiStatus)
				{
					// We've switched from mobile to wifi or wifi to mobile and android is
					// re-configuring all the sockets. Best to just clean up and wait for 
					// the next poll to come back.
					if(localFile != null && localFile.Exists()) localFile.Delete();
					log(t.Name + ": Connection change. Aborting."); 
					break;
				}
				else
					operationFailures.add("Download " + downloadKey + " from task " + t.Name + " failed: " + ex.toString());
			}
		}
		MediaStoreHelper.AddPaths(pathsToScan, this);
		t.LastRun = new Date();
		SettingsDB.SaveTaskInfo(t, getDBContext());
		
		if(operationFailures.isEmpty())
			log(t.Name + ": performTransfers completed without errors.");
		else
		{
			log(t.Name + ": performTransfers completed with " + operationFailures.size() + " failures:", true);
			for(String failure : operationFailures) log(" - " + failure, true);
		}
	}
	
	private void addAccountFailure(AccountInfo account)
	{
		if(failedAccountsNotified.contains(account.ID)) return;
		
		failedAccountsNotified.add(account.ID);
		
		EngineInfo engine = SettingsDB.getEngineInfo(account.EngineID, getDBContext());
		LinkedList<AccountData> account_data = SettingsDB.getAllAccountData(account.ID, getDBContext());
		
		Intent intent = new Intent(this, Cloud.getCloud(engine).usesOAuth() ? OAuthActivity.class : CredentialsActivity.class);
		intent.putExtra(WizardStepActivity.BUNDLE_ACCOUNT_KEY, account);
		intent.putExtra(WizardStepActivity.BUNDLE_ACCOUNT_DATA_KEY, account_data);
		intent.putExtra(WizardStepActivity.BUNDLE_ENGINE_KEY, engine);
		
		PendingIntent pendingIntent = PendingIntent.getActivity(this.getBaseContext(), (int)account.ID, intent, Intent.FLAG_ACTIVITY_CLEAR_TOP);
		
		Notification notification = new Notification();
		notification.icon = R.drawable.icon;
		notification.tickerText = String.format(getString(R.string.account_disabled_ticker), account.Name);
		notification.when = System.currentTimeMillis();
		notification.flags = Notification.FLAG_AUTO_CANCEL;
		notification.setLatestEventInfo(this, getText(R.string.account_disabled_notification), (CharSequence)String.format(getString(R.string.account_disabled_fix), account.Name), pendingIntent);
		
		((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify((int)account.ID, notification);
	}
	private void removeAccountFailure(AccountInfo account)
	{
		int index = failedAccountsNotified.indexOf(account.ID);
		if(index != -1) failedAccountsNotified.remove(index);
	}
	
	private void validateMedia(SyncFile fullPath, Boolean createDirectory){
		if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
    		throw new RuntimeException("External storage not currently mounted. Current state: " + Environment.getExternalStorageState());
		
        if(!fullPath.DirectoryExists() && createDirectory) 
        {
        	if(!fullPath.mkdir()) 
        		throw new RuntimeException("Could not create directory '" + fullPath.Path + "'.");
        	else
        		log("Created directory '" + fullPath.Path + "'.");
        } 
        else if (!fullPath.DirectoryExists()) 
        {
        	log("Path '" + fullPath.Path + "' doesn't exist but we don't need it.");
        	return; // If we continue DirectoryIOAccess returns false because the directory doesn't exist.
        }
        else 
        	log("Path '" + fullPath.Path + "' exists.");
        
        if(!fullPath.DirectoryIOAccess())
    		throw new RuntimeException("No IO access for '" + fullPath.Path + "'.");
	}
	private void deleteLocalFile(SyncFile fullPath)
	{
		try
		{
			this.validateMedia(fullPath, false);
			if(fullPath.Exists())
			{
				fullPath.Delete();
				log("File '" + fullPath + "' deleted.", true);
			} else {
				log("Delete requested for file '" + fullPath + "' but it does not exist.");
			}
		} catch(Exception ex)
		{
			throw new RuntimeException("deleteSyncFile('" + fullPath + "') failed: " + ex);
		}
	}
	private CloudItem downloadFile(ICloud cloud, String key, SyncFile fullPath, List<TaskData> taskData)
	{
		// if(!Wifi.isConnected()) throw new RuntimeException("Wifi is not connected.");
		try 
		{
			this.validateMedia(fullPath, true);
			return cloud.downloadFile(key, fullPath, taskData, notification);
		} catch(Exception ex)
		{
			throw new RuntimeException("downloadFile(cloud, '" + key + "', '" + fullPath + "') failed: " + ex + " stack: " + StringHelper.joinElements(ex.getStackTrace()));
		}
	}
	private Context getDBContext()
	{
		if(dbContext == null) dbContext = this.getApplication();
		return dbContext;
	}
	private void log(String message)
	{
		log(message, false);
	}
	private void log(String message, Boolean quiet)
	{
		if(!quiet) return;
		Log.v("CloudSyncService", message);
	}
}
