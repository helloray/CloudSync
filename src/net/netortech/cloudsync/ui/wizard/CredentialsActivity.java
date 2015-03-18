package net.netortech.cloudsync.ui.wizard;

import java.util.LinkedList;
import java.util.List;

import net.netortech.cloudsync.R;
import net.netortech.cloudsync.clouds.Cloud;
import net.netortech.cloudsync.clouds.ICloud;
import net.netortech.cloudsync.data.AccountData;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.netortech.utilities.ListItem;

public class CredentialsActivity extends WizardStepActivity {
	ListView lstAccountSettings;
	Button btnTestAccount;
	
	ICloud cloud;
	ProgressDialog dialog;
	
	@Override protected void btnBack_Click() { setResult(RESULT_CANCELED, null); finish(); }
	@Override protected String getActivityName() { return "CredentialsActivity"; }
	@Override protected int getLayoutId() { return R.layout.wizard_credentials; }
	
	@Override
	public void onWizardStepCreated(Bundle b)
	{
		initStep();
	}
	@Override protected String getStepTitle() 
	{ 
		if(task != null && task.ID == -1)
			return "Creating New Job!";
		else if(task != null)
			return "Editing Job '" + task.Name + "'";
		else 
			return "Editing Account '" + account.Name + "'";
	}
	@Override 
	protected String getStepInstructions() 
	{ 
		return "What credentials do you use to connect to " + engine.Name;
	}
	@Override
	protected void confirmItem(Object item, int code)
	{
		switch(code)
		{
			case WizardStepActivity.CONFIRM_NEXT_STEP_CODE:
				if(task == null)
					finishWizard(FINISH_NO_ADVANCED);
				else if(task.ID == -1)
					startNextStep();
				else
					finishWizard();
				break;
			case WizardStepActivity.CONFIRM_CONTINUE_CODE:
				startNextStep();
				break;
			case WizardStepActivity.CONFIRM_FINISHED_CODE:
				finishWizard();
				break;
		}
	}
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode == WizardStepActivity.WIZARD_ACCOUNT_FAILED_RESULT_CODE) return;
		super.onActivityResult(requestCode, resultCode, data);
	}
	private void startNextStep()
	{
		Intent intent;
		
    	boolean hasContainers = Cloud.getCloud(engine).hasContainers();
    	boolean hasFolders = Cloud.getCloud(engine).hasFolders();
    	
		intent = new Intent(this, hasContainers && task.ID == -1 ? CloudContainerActivity.class : hasContainers || hasFolders ? SelectCloudResourceActivity.class : SelectLocalFolderActivity.class);
		
		putExtras(intent);
		
		startActivityForResult(intent, ANONYMOUS_REQUEST_CODE);
	}
    protected void testAccount()
    {
    	account.Failures = 0;
    	cloud = null;
    	cloud = Cloud.getCloud(account, account_data, getDBContext(), true);
    }
    protected void beginTestAccount()
    {
		Thread thread =  new Thread(null, new Runnable(){ public void run() { testAccount(); finishTestAccount(); } }, "MagentoBackground");
		thread.start();
		dialog = ProgressDialog.show(this, this.getString(R.string.please_wait), this.getString(R.string.authenticating), true);
    }
    protected void finishTestAccount()
    {
    	this.runOnUiThread(new Runnable() {
	        public void run() {
	        	dialog.dismiss();
	        	if(account.Failures > 0 || cloud == null)
	        		showOkAlert(String.format(CredentialsActivity.this.getString(R.string.account_login_failed), account.LastResult));
	        	else if(account.Name.trim().equals(""))
	        		showOkAlert(CredentialsActivity.this.getString(R.string.account_name_required));
	        	else
	        		showConfirmCredentials(CredentialsActivity.this.getString(R.string.account_login_success) + " " + 
	        				CredentialsActivity.this.getString(R.string.continue_question), WizardStepActivity.CONFIRM_NEXT_STEP_CODE);
	        }
    	});
    }
	
	private void initStep()
	{
		bind();
	}
	
	private void bind()
	{
		lstAccountSettings = (ListView)this.findViewById(R.id.lstAccountSettings);
		btnTestAccount = (Button)this.findViewById(R.id.btnTestAccount);
		btnTestAccount.setOnClickListener(new OnClickListener() { public void onClick(View v) { beginTestAccount(); }});
		try { bindAccountDataItems(); } catch(Exception ex) { log(ex); }
	}
    private void bindAccountDataItems() throws Exception
    {
    	List<ListItem> items = new LinkedList<ListItem>();
    	items.add(new ListItem(1, "", "Friendly Name:"));
    	items.addAll(Cloud.getCloud(engine).getAccountListItems());
    	
    	ArrayAdapter<ListItem> adapter = new ArrayAdapter<ListItem>(this, R.layout.wizard_textbox_item, items) {
	        @Override
	        public View getView(int position, View convertView, ViewGroup parent) {
                LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View v = vi.inflate(R.layout.wizard_textbox_item, null);

                final ListItem item = getItem(position);
                final TextView lblItem = (TextView)v.findViewById(R.id.lblItem);
                final EditText txtItem = (EditText)v.findViewById(R.id.txtItem);
                
    	        lblItem.setText(item.Subtext); 
    	        if(item.ID == 1)
    	        	txtItem.setText(account.Name);
    	        else
    	        {
                    AccountData d = AccountData.getByName(item.Name, account_data);
            		d.AccountID = account.ID;
            		txtItem.setText(d.Data);
    	        }
    	        txtItem.addTextChangedListener(new TextWatcher(){
    				@Override
    				public void afterTextChanged(Editable s) {
    					if(item.ID == 1)
    						account.Name = s.toString();
    					else
    						AccountData.getByName(item.Name, account_data).Data = s.toString();
    				}
    	
    				@Override
    				public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
    				@Override
    				public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
    	        	
    	        });

                return v;
	        }
		};
		lstAccountSettings.setAdapter(adapter);
    }
    
    protected void showConfirmCredentials(CharSequence message, int code)
    {
    	final int finalCode = code;
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setMessage(message)
    	       .setCancelable(false)
    	       .setPositiveButton(CredentialsActivity.this.getString(R.string.yes), new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	        	   CredentialsActivity.this.confirmItem(null, finalCode);
    	           }
    	       })
	       .setNegativeButton(CredentialsActivity.this.getString(R.string.no), new DialogInterface.OnClickListener() { public void onClick(DialogInterface dialog, int id) { } });
    	AlertDialog alert = builder.create();   
    	alert.show();
    }
}
