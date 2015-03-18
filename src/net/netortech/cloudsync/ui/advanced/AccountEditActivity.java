package net.netortech.cloudsync.ui.advanced;

import java.util.LinkedList;
import java.util.List;

import net.netortech.cloudsync.BaseActivity;
import net.netortech.cloudsync.CloudSyncActivity;
import net.netortech.cloudsync.R;
import net.netortech.cloudsync.clouds.Cloud;
import net.netortech.cloudsync.clouds.ICloud;
import net.netortech.cloudsync.data.AccountData;
import net.netortech.cloudsync.data.AccountInfo;
import net.netortech.cloudsync.data.EngineInfo;
import net.netortech.cloudsync.data.SettingsDB;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.netortech.utilities.ListItem;
import com.netortech.utilities.hardware.Wifi;

public class AccountEditActivity extends BaseActivity {
	private long originalEngineID;
	private ProgressDialog dialog;
	private SettingsDB db = CloudSyncActivity.db;
	private AccountInfo account;
	private EngineInfo engine;
	private List<AccountData> account_data = new LinkedList<AccountData>();
	
	ICloud cloud;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.advanced_account_edit);
    	init();
    }
    
    protected void testAccount()
    {
    	account.Failures = 0;
    	cloud = null;
    	cloud = Cloud.getCloud(account, account_data, getDBContext(), true);
    }
    protected void saveAccount()
    {
    	SettingsDB.SaveAccountInfo(account, account_data, getDBContext());
    }
    protected void deleteAccount()
    {
   		SettingsDB.Delete(account, getDBContext());
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
	        		showAlert(String.format(AccountEditActivity.this.getString(R.string.account_login_failed), account.LastResult));
	        	else
	        		showAlert(AccountEditActivity.this.getString(R.string.account_login_success));
	        }
    	});
    }
    
	private void init()
	{
		Wifi.setContext(this);
		
		Bundle b = getIntent().getExtras();
    	if(b != null && b.containsKey("AccountID")) 
    		account = SettingsDB.getAccountInfo(b.getLong("AccountID"), getDBContext());
    	else
    		account = new AccountInfo();
    	originalEngineID = account.EngineID;
        
    	bind();
        
        if(account.Failures > account.MAX_FAILURES_COUNT)
        	showAlert(AccountEditActivity.this.getString(R.string.account_disabled));
	}
	private void bind()
    {
    	final EditText txtAccountName = (EditText) findViewById(R.id.txtAccountName);
        final Spinner ddlAccountEngine = (Spinner) findViewById(R.id.ddlAccountEngine);
        final Button btnSave = (Button)findViewById(R.id.btnSaveAccount);
        final Button btnTest = (Button)findViewById(R.id.btnTestAccount);
        final Button btnCancel = (Button)findViewById(R.id.btnCancelAccount);
        final Button btnDelete = (Button)findViewById(R.id.btnDeleteAccount);
        
        List<EngineInfo> engines = SettingsDB.getAllEngineInfo(getDBContext());
        
        btnSave.setOnClickListener(new OnClickListener(){ public void onClick(View v) {
        	saveAccount();
        	AccountEditActivity.this.finish();
        }});
        
        btnTest.setOnClickListener(new OnClickListener(){ public void onClick(View v) {
        	beginTestAccount();
        }});
        
        btnDelete.setOnClickListener(new OnClickListener(){ public void onClick(View v) {
        	AccountEditActivity.this.showDeleteConfirm();
        }});

        btnCancel.setOnClickListener(new OnClickListener(){ public void onClick(View v) {
        	AccountEditActivity.this.finish();
        }});

        ArrayAdapter<EngineInfo> adapter = new ArrayAdapter<EngineInfo>(this, android.R.layout.simple_spinner_item, engines);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		ddlAccountEngine.setAdapter(adapter);
        ddlAccountEngine.setOnItemSelectedListener(new OnItemSelectedListener() {
		    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) 
		    { 
		    	EngineInfo e = (EngineInfo)ddlAccountEngine.getAdapter().getItem(pos);
		    	account.EngineID = e.ID;
		    	showEngineFields(e.ID); 
	    	}
		    public void onNothingSelected(AdapterView<?> parent) { }
		});

        txtAccountName.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable s) {
				account.Name = s.toString();
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
        	
        });
        
    	if(account.ID != -1) 
    	{
	    	txtAccountName.setText(account.Name);
	    	for(int index = 0; index < engines.size(); index++)
	    		if(engines.get(index).ID == account.EngineID) 
	    			ddlAccountEngine.setSelection(((ArrayAdapter<EngineInfo>)ddlAccountEngine.getAdapter()).getPosition(engines.get(index)));
    	}
    }
    private void bindAccountDataItems()
    {
    	LinearLayout lstAccountData = (LinearLayout)findViewById(R.id.lstEngineProperties);
    	lstAccountData.removeAllViews();
    	if(Cloud.getCloud(engine).usesOAuth()) return;
    	for(ListItem item : Cloud.getCloud(engine).getAccountListItems())
    	{
	        final AccountData d = AccountData.getByName(item.Name, account_data);
	        
	        LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	        View v = vi.inflate(R.layout.advanced_account_data_item, null);
	        
	        final TextView lblAccountData = (TextView) v.findViewById(R.id.lblAccountData);
	        final  EditText txtAccountData = (EditText) v.findViewById(R.id.txtAccountData);
	        lblAccountData.setText(item.Name); 
	        txtAccountData.setText(d.Data);
	        txtAccountData.addTextChangedListener(new TextWatcher(){
				@Override
				public void afterTextChanged(Editable s) {
					d.Data = s.toString();
				}
	
				@Override
				public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
				@Override
				public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
	        	
	        });
	    	lstAccountData.addView(v);
    	}
    }

    protected void showEngineFields(long engineID)
    {
    	engine = SettingsDB.getEngineInfo(engineID, getDBContext());
    	account_data = SettingsDB.getAllAccountData(account.ID, getDBContext());
    	Cloud.getCloud(engine).initializeAccountDataFields(account_data);
    	bindAccountDataItems();
    }
    protected void showDeleteConfirm()
    {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setMessage(AccountEditActivity.this.getString(R.string.delete_account_confirm))
    	       .setCancelable(false)
    	       .setPositiveButton(AccountEditActivity.this.getString(R.string.yes), new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	        	   	deleteAccount();
	    	        	AccountEditActivity.this.finish();
    	           }
    	       })
	       .setNegativeButton(AccountEditActivity.this.getString(R.string.no), new DialogInterface.OnClickListener() { public void onClick(DialogInterface dialog, int id) { } });
    	AlertDialog alert = builder.create();   
    	alert.show();
    }
    protected void showAlert(String msg)
    {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setMessage(msg)
    	       .setCancelable(false)
    	       .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	           }
    	       });
    	AlertDialog alert = builder.create();   
    	alert.show();
    }

    protected static void log(String msg)
    {
    	log(msg, false);
    }
    protected static void log(String msg, Boolean quiet)
    {
    	//if(!quiet) return;
    	Log.v("AccountEditActivity", msg);
    }
}
