package net.netortech.cloudsync.ui.wizard;

import java.util.LinkedList;
import java.util.List;

import net.netortech.cloudsync.R;
import net.netortech.cloudsync.clouds.Cloud;
import net.netortech.cloudsync.data.AccountInfo;
import net.netortech.cloudsync.data.EngineInfo;
import net.netortech.cloudsync.data.SettingsDB;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.netortech.utilities.ListItem;

public class AccountActivity extends WizardStepActivity {
	Spinner ddlAccountEngine;
	ListView lstCurrentAccounts;
	Button btnChangeTaskBehavior;
	Button btnChangeAccountInfo;
	
	List<AccountInfo> accounts;
	boolean showSelection = false;
	
	@Override protected void btnBack_Click() { setResult(RESULT_CANCELED, null); finish(); }
	@Override protected String getActivityName() { return "AccountActivity"; }
	@Override protected int getLayoutId() { return R.layout.wizard_account; }
	
	@Override
	public void onWizardStepCreated(Bundle b)
	{
		initStep();
	}
	@Override protected String getStepTitle() 
	{ 
		if(task.ID == -1)
			return "Creating New Job!";
		else
			return "Editing Job '" + task.Name + "'";
	}
	@Override protected String getStepInstructions() 
	{ 
		if(showSelection)
			return "What cloud service would you like to sync from";
		else 
			return "What do you need to change about this job";
	}
	@Override
	protected void confirmItem(Object item, int code)
	{
		switch(code)
		{
			case WizardStepActivity.CONFIRM_DELETE_ITEM_CODE:
				SettingsDB.Delete((AccountInfo)item, getDBContext());
				accounts = SettingsDB.getAllAccountInfo(getDBContext());
				bindList();
				break;
			case WizardStepActivity.CONFIRM_SAVE_ITEM_CODE:
				task.AccountID = ((AccountInfo)item).ID;
				finishWizard();
				break;
		}
	}
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if(requestCode == WizardStepActivity.WIZARD_ACCOUNT_FAILED_RESULT_CODE)
    	{
    		log("onActivityResult: Account login failed after being skipped. Forwarding to OAuth/Credentials test.");
    		startNextStep(false);
    		return;
    	}
    	super.onActivityResult(requestCode, resultCode, data);
    }
	
	private void taskBehaviorSelected()
	{
		account = SettingsDB.getAccountInfo(task.AccountID, getDBContext());
		account_data = SettingsDB.getAllAccountData(account.ID, getDBContext());
		engine = SettingsDB.getEngineInfo(account.EngineID, getDBContext());
		startNextStep(true);
	}
	private void accountSelected()
	{
		engine = SettingsDB.getEngineInfo(account.EngineID, getDBContext());
		account_data = SettingsDB.getAllAccountData(account.ID, getDBContext());
    	if(task.ID == -1)
    	{
    		startNextStep(true);
    	}
    	else
    	{
    		if(task.AccountID == account.ID)
    			startNextStep(false);
    		else
    			this.showConfirm(getString(R.string.change_task_account), account, WizardStepActivity.CONFIRM_SAVE_ITEM_CODE, -1);
    	}
	}
	private void engineSelected()
	{
		account = new AccountInfo();
		account.EngineID = engine.ID;
		account_data.clear();
		Cloud.getCloud(engine).initializeAccountDataFields(account_data);
		startNextStep(false);
	}
	private void startNextStep(boolean skipAccount)
	{
		Intent intent;
		
    	boolean usesOAuth = Cloud.getCloud(engine).usesOAuth();
    	boolean hasContainers = Cloud.getCloud(engine).hasContainers();
    	boolean hasFolders = Cloud.getCloud(engine).hasFolders();
    	
		if(skipAccount)
		{
			intent = new Intent(this, hasContainers && task.ID == -1 ? CloudContainerActivity.class : hasContainers || hasFolders ? SelectCloudResourceActivity.class : SelectLocalFolderActivity.class);
		} else {
			intent = new Intent(this, usesOAuth ? OAuthActivity.class : CredentialsActivity.class);
		}
		
		putExtras(intent);
		
		startActivityForResult(intent, ANONYMOUS_REQUEST_CODE);
	}

	private void initStep()
	{
		Intent i = getIntent();
		showSelection = task.ID == -1 ? true : i.hasExtra("showSelection") ? i.getBooleanExtra("showSelection", showSelection) : showSelection;
		if(!showSelection) showEditTask(); 
		bind();
	}
	private void bind()
	{
		ddlAccountEngine = (Spinner)this.findViewById(R.id.ddlAccountEngine);
		lstCurrentAccounts = (ListView)this.findViewById(R.id.lstCurrentAccounts);
		btnChangeAccountInfo = (Button)this.findViewById(R.id.btnChangeAccountInfo);
		btnChangeTaskBehavior = (Button)this.findViewById(R.id.btnChangeTaskBehavior);

		btnChangeAccountInfo.setOnClickListener(new OnClickListener() { public void onClick(View v) { showSelection(); }});
		btnChangeTaskBehavior.setOnClickListener(new OnClickListener() { public void onClick(View v) { taskBehaviorSelected(); }});
		
		if(!showSelection) return;
		List<ListItem> items = new LinkedList<ListItem>();
		List<EngineInfo> engines = SettingsDB.getAllEngineInfo(getDBContext());
		items.add(new ListItem(-1, "Select a Cloud Service", ""));
		for(EngineInfo engine : engines) items.add(new ListItem(engine.ID, engine.Name, ""));
		
        ArrayAdapter<ListItem> adapter = new ArrayAdapter<ListItem>(this, android.R.layout.simple_spinner_item, items);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		ddlAccountEngine.setAdapter(adapter);
        ddlAccountEngine.setOnItemSelectedListener(new OnItemSelectedListener() {
		    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) 
		    { 
		    	ListItem i = (ListItem)ddlAccountEngine.getAdapter().getItem(pos);
		    	if(i.ID == -1) return;
		    	engine = SettingsDB.getEngineInfo(i.ID, getDBContext());
		    	engineSelected();
	    	}
		    public void onNothingSelected(AdapterView<?> parent) { }
		});

        accounts = task.ID == -1 ? SettingsDB.getAllAccountInfo(getDBContext()) : SettingsDB.getAllAccountInfoByEngine(SettingsDB.getAccountInfo(task.AccountID, getDBContext()).EngineID, getDBContext());
		
		if(accounts.size() == 0) { hideAccounts(); return; }
		
		bindList();
		
	}
	private void bindList()
    {
		if(accounts.size() == 0) hideAccounts();
    	ArrayAdapter<AccountInfo> adapter = new ArrayAdapter<AccountInfo>(this, R.layout.wizard_edit_item, accounts) {
	        @Override
	        public View getView(int position, View convertView, ViewGroup parent) {
                LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                final AccountInfo item = getItem(position);
                final EngineInfo engine = SettingsDB.getEngineInfo(item.EngineID, getDBContext());
                final View v = vi.inflate(R.layout.wizard_edit_item, null);
                final Button btnSelect = (Button)v.findViewById(R.id.btnSelect);
                final Button btnDelete = (Button)v.findViewById(R.id.btnDelete);
                final TextView t = (TextView) v.findViewById(R.id.lblItemTitle);
                final TextView d = (TextView) v.findViewById(R.id.lblItemDescription);
                
                if(task.ID == -1 || task.AccountID != item.ID)
                {
                	btnSelect.setText(R.string.select);
                }
                else
                {
                    v.setBackgroundColor(Color.parseColor(AccountActivity.this.getString(R.color.selected)));
                	btnDelete.setVisibility(View.GONE);
                	btnSelect.setText(R.string.edit);
                }
	        	OnClickListener selectListener = new OnClickListener(){ public void onClick(View v) { account = item; accountSelected(); }};

                v.setOnClickListener(selectListener);
                btnSelect.setOnClickListener(selectListener);
                btnDelete.setOnClickListener(new OnClickListener() { public void onClick(View v) {
                	if(SettingsDB.getAllTaskInfoByAccount(item.ID, getDBContext()).size() > 0)
                		alert("Cannot delete this account, it is being used.");
                	else
                		AccountActivity.this.showConfirm(AccountActivity.this.getText(R.string.delete_account_confirm), item, WizardStepActivity.CONFIRM_DELETE_ITEM_CODE, -1); 
            	}});
                t.setText(item.Name);  
                d.setText(AccountActivity.this.getText(R.string.service_colon) + " " + engine.Name);
                return v;
	        }
		};
		lstCurrentAccounts.setAdapter(adapter);
	}
	

	private void showSelection()
	{
		Intent i = new Intent(this, AccountActivity.class);
		putExtras(i);
		i.putExtra("showSelection", true);
		this.startActivityForResult(i, WizardStepActivity.ANONYMOUS_REQUEST_CODE);
	}
	
	private void hideAccounts()
	{
		this.findViewById(R.id.lblCurrentAccounts).setVisibility(View.GONE);
		lstCurrentAccounts.setVisibility(View.GONE);
	}
	
	private void showEditTask()
	{
		// binding may not have been called yet. Just use the find method.
		this.findViewById(R.id.ddlAccountEngine).setVisibility(View.GONE);
		this.findViewById(R.id.lblCurrentAccounts).setVisibility(View.GONE);
		this.findViewById(R.id.lstCurrentAccounts).setVisibility(View.GONE);

		this.findViewById(R.id.btnChangeAccountInfo).setVisibility(View.VISIBLE);
		this.findViewById(R.id.btnChangeTaskBehavior).setVisibility(View.VISIBLE);
	}
	
}
