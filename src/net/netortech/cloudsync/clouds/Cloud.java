package net.netortech.cloudsync.clouds;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import net.netortech.cloudsync.data.AccountData;
import net.netortech.cloudsync.data.AccountInfo;
import net.netortech.cloudsync.data.EngineInfo;
import net.netortech.cloudsync.data.SettingsDB;
import net.netortech.cloudsync.data.TaskData;
import android.content.Context;
import android.util.Log;

import com.netortech.utilities.ListItem;
import com.netortech.utilities.clouds.CloudAuthenticationException;
import com.netortech.utilities.clouds.CloudException;
import com.netortech.utilities.web.OAuthException;
import com.netortech.utilities.web.WebMethodException;

public abstract class Cloud implements ICloud {

	protected Cloud() {}
	
	public boolean isAuthenticated() { return isAuthenticated(false); }
	public abstract boolean isAuthenticated(boolean throwException);

	public static Cloud getCloud(EngineInfo engine)
	{
		Cloud cloud = null;
		if(engine.Code.equals(EngineInfo.ENGINE_CODE_AWS)) cloud = new S3();
		if(engine.Code.equals(EngineInfo.ENGINE_CODE_AWSPROXY)) cloud = new S3(true);
		if(engine.Code.equals(EngineInfo.ENGINE_CODE_AZURE)) cloud = new Azure();
		if(engine.Code.equals(EngineInfo.ENGINE_CODE_AZUREPROXY)) cloud = new Azure(true);
		if(engine.Code.equals(EngineInfo.ENGINE_CODE_DROPBOX)) cloud = new Dropbox();
		if(engine.Code.equals(EngineInfo.ENGINE_CODE_SKYDRIVE)) cloud = new SkyDrive();
		if(cloud == null) throw new RuntimeException("Unknown Engine Type. Engine Info: " + engine.toString());
		return cloud;
	}
	public static ICloud getCloud(AccountInfo account, Context dbContext)
	{
		return getCloud(account, SettingsDB.getAllAccountData(account.ID, dbContext), dbContext, false);
	}
	public static ICloud getCloud(AccountInfo account, List<AccountData> accountData, Context dbContext, boolean testingAccount)
	{
		account.LastRun = new Date();
		ICloud cloud = null;

		if(account.Failures > AccountInfo.MAX_FAILURES_COUNT & !testingAccount)
		{
			log("Account '" + account.Name + "' has failed too many times. Please check the credentials and try again.", true);
			return null;
		}
		EngineInfo engine = SettingsDB.getEngineInfo(account.EngineID, dbContext);
		cloud = getCloud(engine);

		try
		{
			cloud.setAccountData(accountData);
		} 
		catch (CloudAuthenticationException ex)
		{
			account.LastResult = ex.getAuthenticationMessage();
			if(account.LastResult == null) account.LastResult = ex.toString();
			account.Failures++;
			cloud = null;
		}
		catch(OAuthException ex)
		{
			account.LastResult = ex.getMessage();
			if(account.LastResult == null) account.LastResult = ex.toString();
			account.Failures++;
			cloud = null;
		}
		catch(CloudException ex)
		{
			account.LastResult = ex.getMessage();
			account.Failures++;
			cloud = null;
		}
		catch (WebMethodException ex)
		{
			if(testingAccount) account.Failures++;
			account.LastResult = "Failed to establish cloud connection. Reason: " + ex.toString();
			cloud = null;
		}
		
		if(cloud != null & !testingAccount)
		{
			account.Failures = 0;
			account.LastResult = "Success.";
		}

		if(!testingAccount)	SettingsDB.SaveAccountInfo(account, dbContext);
		return cloud;
	}
		
	protected void writeStream(InputStream in, FileOutputStream f, int size, DownloadListener listener) throws IOException
	{
		byte[] buffer = new byte[1024];
	    int length = 0;
	    int count = 0;
	    int percent = 0;
	    int lastpercent = 0;
	    while ( (length = in.read(buffer)) > 0 )
	    { 
	    	count += length; 
	    	f.write(buffer, 0, length); 
			if(listener != null)
			{
				percent = (int)(((double)count / (double)size) * 100);
				if(lastpercent != percent) listener.ProgressChanged(percent);
				lastpercent = percent;
			}
    	}
	    f.close();
	}
	
	protected static void log(String message)
	{
		log(message, false);
	}
	protected static void log(String message, Boolean quiet)
	{
		// if(!quiet) return;
		Log.v("Cloud", message);
	}
}
