package net.netortech.cloudsync.ui.wizard;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.netortech.cloudsync.R;
import net.netortech.cloudsync.clouds.Cloud;
import net.netortech.cloudsync.clouds.CloudItem;
import net.netortech.cloudsync.clouds.ICloud;
import net.netortech.cloudsync.data.TaskData;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import com.netortech.utilities.FolderItem;

public class CloudFolderActivity extends FolderNavigateBase {
	
	ICloud cloud = null;
	ProgressDialog dialog;
	
	@Override protected void btnBack_Click() { finish(); }
	@Override protected String getActivityName() { return "CloudFolderActivity"; }
	@Override protected String getStepInstructions() { return "Which cloud folder should this job sync from"; }
	@Override
	protected void onFileNavigateStepCreated(Bundle b) {
		init();
		showFolder(Cloud.getCloud(engine).getCloudFolder(task_data));
	}
	@Override protected String getStepTitle() 
	{ 
		if(task.ID == -1)
			return "Creating New Job!";
		else
			return "Editing Job '" + task.Name + "'";
	}
	@Override
	protected void confirmItem(Object item, int code)
	{
		switch(code)
		{
			case WizardStepActivity.CONFIRM_GO_BACK_CODE:
				setResult(WizardStepActivity.WIZARD_ACCOUNT_FAILED_RESULT_CODE, null); 
				finish();
				break;
			case WizardStepActivity.CONFIRM_CONTINUE_CODE:
				beginGetFolders((FolderItem)item);
				break;
		}
	}
	@Override
	protected void beginGetFolders(FolderItem parent) {
		final FolderItem finalParent = parent;
	    Thread thread = new Thread(null, new Runnable(){ public void run() { getFolders(finalParent); } }, "MagentoBackground");
        thread.start();
        dialog = ProgressDialog.show(this, this.getString(R.string.please_wait), this.getString(R.string.retrieving_data), true);
	}
	private void startNextStep()
	{
		Intent intent = new Intent(this, SelectLocalFolderActivity.class);
		putExtras(intent);
		startActivityForResult(intent, ANONYMOUS_REQUEST_CODE);
	}
    private void getFolders(FolderItem parent)
    {
    	Map<String, CloudItem> folders = null;
    	if(cloud == null)
    	{
    		account.Failures = 0;
    		cloud = Cloud.getCloud(account, account_data, getDBContext(), true);
    	}
    	if(account.Failures == 0 || cloud == null) 
    	{
    		try
    		{
    			log("Gathering folders from '" + parent.Key + "'.");
    			folders = cloud.getFolders(new LinkedList<TaskData>(task_data), parent.Key);
    		} catch(Exception ex)
    		{
    			log("Error trying to get path: '" + parent.Name + "'");
    			log(ex);
    		}
    	} 
		final List<FolderItem> finalFolders = new LinkedList<FolderItem>();
		CloudItem folder;
		for(String key : folders.keySet())
		{
			folder = folders.get(key);
			finalFolders.add(new FolderItem(folder.getName(), folder.getKey(), parent));
		}
		final FolderItem finalParent = parent;
		runOnUiThread(new Runnable(){ public void run() { internalFinishGetFolders(finalFolders, finalParent); } });
    }
    private void internalFinishGetFolders(List<FolderItem> folders, FolderItem parent)
    {
    	dialog.dismiss(); 
    	if(account.Failures != 0 || cloud == null)
    		showError(CloudFolderActivity.this.getString(R.string.account_login_failed_retry), null);
    	else if(folders == null)
    		showError(CloudFolderActivity.this.getString(R.string.cloud_folders_failed_retry), parent.Name);
    	else 
    		CloudFolderActivity.this.finishGetFolders(parent, folders, true);
    }

    private void showError(CharSequence msg, Object item)
    {
    	showConfirm(msg, item, WizardStepActivity.CONFIRM_CONTINUE_CODE, WizardStepActivity.CONFIRM_GO_BACK_CODE);
    }
	private void pathSelected() {
		log("Selected path: " + getCurrentFolder().Key);
		Cloud.getCloud(engine).setCloudFolder(getCurrentFolder(), task_data);
		startNextStep();
	}
	private void init()
	{
		this.btnSelectFolder.setOnClickListener(new OnClickListener() { public void onClick(View v) { pathSelected(); }});
	}
}
