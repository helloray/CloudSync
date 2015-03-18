package net.netortech.cloudsync.ui.wizard;

import java.util.LinkedList;
import java.util.List;

import net.netortech.cloudsync.R;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.netortech.utilities.FolderItem;

public abstract class FolderNavigateBase extends WizardStepActivity {
	protected Button btnSelectFolder;
	protected TextView lblFolders;
	ListView lstFolders;
	TextView lblBrowsingPath;
	
	FolderItem currentFolder = FolderItem.construct("");
	@Override
	public final void onWizardStepCreated(Bundle b)
	{
		init();
		onFileNavigateStepCreated(b);
	}
	
	@Override
	protected final int getLayoutId() {
		return R.layout.wizard_folder;
	}

	protected void showFolder(FolderItem folder)
	{
		init();
		beginGetFolders(folder);
	}
	
	protected final void finishGetFolders(FolderItem folder, List<FolderItem> folders, boolean success)
	{
		if(!success && lstFolders.getAdapter() != null)
		{
			alert("Can't access that folder.");
			return;
		}
		currentFolder = folder;
		
		LinkedList<FolderItem> folderItems = new LinkedList<FolderItem>();
		if(currentFolder.Parent != null)
		{
			FolderItem parent = currentFolder.Parent.clone();
			parent.Name = "<Up One Folder>";
			folderItems.add(parent);
		}
		if(folders.size() == 0)
			folderItems.add(new FolderItem("<No Child Folders>", null, null));
		for(FolderItem item : folders)
			folderItems.add(item);

		String browsing = currentFolder.getSeparatedNames(FolderItem.DEFAULT_FOLDER_SEPARATOR);
		if(browsing.equals("")) browsing = FolderItem.DEFAULT_FOLDER_SEPARATOR;
		lblBrowsingPath.setText(browsing);
		
		ArrayAdapter<FolderItem> adapter = new ArrayAdapter<FolderItem>(this, R.layout.wizard_edit_item_single_line, folderItems) {
	        @Override
	        public View getView(int position, View convertView, ViewGroup parent) {
                LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                final FolderItem item = getItem(position);
                
                final View v = vi.inflate(R.layout.wizard_edit_item_single_line, null);
                final Button btnSelect = (Button)v.findViewById(R.id.btnSelect);
                final Button btnDelete = (Button)v.findViewById(R.id.btnDelete);
                final TextView t = (TextView) v.findViewById(R.id.lblItemTitle);
                
            	btnDelete.setVisibility(View.GONE);
            	btnSelect.setText(R.string.select);

            	OnClickListener selectListener = null;
            	if(item.Key == null)
            	{
    				selectListener = new OnClickListener(){ public void onClick(View v) { }};
    				btnSelect.setVisibility(View.GONE);
            	}
            	else if(currentFolder.Parent == null || !item.Key.equals(currentFolder.Parent.Key))
            	{
            		log("Current folder: '" + currentFolder.Name + "' parent is null = '" + (currentFolder.Parent == null) + "'. Adding '" + item.Name + "', key: '" + item.Key + "'");
            		selectListener = new OnClickListener(){ public void onClick(View v) { showFolder(item); }};
            	}
        		else
        			selectListener = new OnClickListener(){ public void onClick(View v) { showFolder(currentFolder.Parent); }};
                v.setOnClickListener(selectListener);
                btnSelect.setOnClickListener(selectListener);

                // if(currentPath().equals(fullPath)) v.setBackgroundColor(Color.parseColor(FolderNavigateBase.this.getString(R.color.selected)));
                
                t.setText(item.Name);  
                
                return v;
	        }
		};
		lstFolders.setAdapter(adapter);
	}
	
	private void init()
	{
		lstFolders = (ListView)this.findViewById(R.id.lstFolders);
		lblFolders = (TextView)this.findViewById(R.id.lblFolders);
		lblBrowsingPath = (TextView)this.findViewById(R.id.lblBrowsingPath);
		btnSelectFolder = (Button)this.findViewById(R.id.btnSelectFolder);
	}

	protected String getSeparator()
	{
		return "/";
	}
	abstract protected void onFileNavigateStepCreated(Bundle b);
	abstract protected void beginGetFolders(FolderItem parent);
	abstract protected String getActivityName();
	abstract protected String getStepInstructions();
	abstract protected String getStepTitle();
	abstract protected void btnBack_Click();

	protected FolderItem getCurrentFolder() { return currentFolder; }
}
