package net.netortech.cloudsync.ui.wizard;

import java.io.File;
import java.io.FileFilter;
import java.util.LinkedList;
import java.util.List;

import net.netortech.cloudsync.R;
import net.netortech.cloudsync.data.TaskInfo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import com.netortech.utilities.FileHelper;
import com.netortech.utilities.FolderItem;

public class LocalFolderActivity extends FolderNavigateBase {
	
	@Override protected void btnBack_Click() { finish(); }
	@Override protected String getActivityName() { return "LocalFolderActivity"; }
	@Override protected String getStepInstructions() { return "Which local folder should this job sync to"; }
	@Override
	protected void onFileNavigateStepCreated(Bundle b) {
		init();
		if(task.ID == -1 && task.Path.equals(TaskInfo.DEFAULT_MEDIA_FOLDER))
			showFolder(FolderItem.construct(TaskInfo.DEFAULT_ANDROID_MUSIC_FOLDER));
		else
		{
			showFolder(FolderItem.construct(task.Path));
		}
	}
	@Override protected String getStepTitle() 
	{ 
		if(task.ID == -1)
			return "Creating New Job!";
		else
			return "Editing Job '" + task.Name + "'";
	}
	@Override
	protected void beginGetFolders(FolderItem folder) {
		File folderFile = new File(folder.Key);
		List<FolderItem> folders = new LinkedList<FolderItem>();
		FileFilter fileFilter = new FileFilter() {
		    public boolean accept(File file) {
		        return file.isDirectory();
		    }
		};
		
		boolean success = true;
		
		try
		{
			for(File item : folderFile.listFiles(fileFilter)) 
				folders.add(new FolderItem(item.getName(), item.getAbsolutePath(), folder));
		} 
		catch(Exception ex)
		{
			success = false;
		}
		this.finishGetFolders(folder, folders, success);
	}
	private void startNextStep()
	{
		finishWizard();
	}

	private void pathSelected() {
		log("Selected path: " + getCurrentFolder().Key);
		task.Path = getCurrentFolder().Key;
		startNextStep();
	}
	private void init()
	{
		lblFolders.setText(this.getText(R.string.local_folders));
		this.btnSelectFolder.setOnClickListener(new OnClickListener() { public void onClick(View v) { pathSelected(); }});
	}
}
