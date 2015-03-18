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
import net.netortech.cloudsync.data.TaskData;
import net.netortech.cloudsync.data.TaskInfo;
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
import com.netortech.utilities.clouds.CloudAuthenticationException;
import com.netortech.utilities.clouds.CloudException;

public class TaskEditActivity extends BaseActivity {
	private long originalAccountID;
	private ProgressDialog dialog;
	private SettingsDB db = CloudSyncActivity.db;
	private TaskInfo task;
	private EngineInfo engine;
	private List<TaskData> task_data = new LinkedList<TaskData>();
	
	final private class TestData {
		public AccountInfo account;
		public boolean failed;
		public String result;
	}
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.advanced_task_edit);
    	init();
    }

    protected void testTask(TestData testData)
    {
    	testData.failed = false;
    	testData.account = SettingsDB.getAccountInfo(task.AccountID, getDBContext());
    	List<AccountData> data = SettingsDB.getAllAccountData(testData.account.ID, getDBContext());
    	testData.account.Failures = 0;
    	ICloud cloud = Cloud.getCloud(testData.account, data, getDBContext(), true);
    	if(testData.account.Failures == 0)
    	{
    		try
    		{
    			int count = cloud.getFiles(task_data).size();
    			testData.result = String.format(this.getString(R.string.count_files_parsed), Integer.toString(count));
    		}
    		catch(CloudAuthenticationException ex)
    		{
    			testData.failed = true;
    			testData.result = ex.getAuthenticationMessage();
    			if(testData.result == null) testData.result = ex.toString();
    		}
    		catch(CloudException ex)
    		{
    			testData.failed = true;
    			testData.result = ex.getMessage();
    		}
    		catch(Exception ex)
    		{
    			testData.failed = true;
    			testData.result = ex.toString();
    		}
    	} 
    }
    protected void deleteTask()
    {
		SettingsDB.Delete(task, getDBContext());
    }
    protected void saveTask()
    {
    	SettingsDB.SaveTaskInfo(task, task_data, getDBContext());
    }
    
    protected void beginTestTask()
    {
	     Thread thread =  new Thread(null, new Runnable(){ public void run() { TestData testData = new TestData(); testTask(testData); finishTestTask(testData); } }, "MagentoBackground");
         thread.start();
         dialog = ProgressDialog.show(this, this.getString(R.string.please_wait), this.getString(R.string.running_task), true);
    }
    protected void finishTestTask(TestData testData)
    {
    	final TestData test = testData;
    	this.runOnUiThread(new Runnable() {
	        public void run() {
	        	dialog.dismiss();
	        	if(test.account.Failures > 0)
	        		showAlert(String.format(TaskEditActivity.this.getString(R.string.account_login_failed), test.account.LastResult));
	        	else if(test.failed)
	        		showAlert(String.format(TaskEditActivity.this.getString(R.string.task_failed), test.result));
	        	else
	        		showAlert(String.format(TaskEditActivity.this.getString(R.string.task_success), test.result));
	        }
    	});
    }
    
    
	protected void init()
	{
		Bundle b = getIntent().getExtras();
    	if(b != null && b.containsKey("TaskID")) 
    		task = SettingsDB.getTaskInfo(b.getLong("TaskID"), getDBContext());
    	else
    		task = new TaskInfo();
    	originalAccountID = task.AccountID;
        bind();
	}
	protected void bind()
    {
    	final EditText txtTaskName = (EditText) findViewById(R.id.txtTaskName);
    	final EditText txtTaskPath = (EditText) findViewById(R.id.txtTaskPath);
    	final EditText txtTaskMaxSize = (EditText) findViewById(R.id.txtTaskMaxSize);
    	final EditText txtTaskMobileMaxSize = (EditText) findViewById(R.id.txtTaskMobileMaxSize);
        final Spinner ddlAccount = (Spinner) findViewById(R.id.ddlAccountInfo);
        final Button btnSave = (Button)findViewById(R.id.btnSaveTask);
        final Button btnTest = (Button)findViewById(R.id.btnTestTask);
        final Button btnCancel = (Button)findViewById(R.id.btnCancelTask);
        final Button btnDelete = (Button)findViewById(R.id.btnDeleteTask);
        
        List<AccountInfo> accounts = SettingsDB.getAllAccountInfo(getDBContext());
        
        btnSave.setOnClickListener(new OnClickListener(){ public void onClick(View v) {
        	saveTask();
        	TaskEditActivity.this.finish();
        }});

        btnTest.setOnClickListener(new OnClickListener(){ public void onClick(View v) {
        	beginTestTask();
        }});
        
        btnDelete.setOnClickListener(new OnClickListener(){ public void onClick(View v) {
        	TaskEditActivity.this.showDeleteConfirm();
        }});

        btnCancel.setOnClickListener(new OnClickListener(){ public void onClick(View v) {
        	TaskEditActivity.this.finish();
        }});

        ArrayAdapter<AccountInfo> adapter = new ArrayAdapter<AccountInfo>(this, android.R.layout.simple_spinner_item, accounts);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		ddlAccount.setAdapter(adapter);
        ddlAccount.setOnItemSelectedListener(new OnItemSelectedListener() {
		    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) 
		    { 
		    	AccountInfo a = (AccountInfo)ddlAccount.getAdapter().getItem(pos);
		    	task.AccountID = a.ID;
		    	showEngineFields(a.EngineID); 
	    	}
		    public void onNothingSelected(AdapterView<?> parent) { }
		});
        

        txtTaskName.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable s) {
				task.Name = s.toString();
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
        	
        });
        txtTaskPath.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable s) {
				task.Path = s.toString();
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
        });        
        txtTaskMaxSize.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable s) {
				try { task.MaxSize = Integer.parseInt(s.toString()); } catch (Exception ex) {}
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
        	
        });        
        txtTaskMobileMaxSize.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable s) {
				try { task.MobileMaxSize = Integer.parseInt(s.toString()); } catch (Exception ex) {}
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
        	
        });    
    	txtTaskPath.setText(task.Path == null ? "" : task.Path);
    	txtTaskMaxSize.setText(Long.toString(task.MaxSize));
    	txtTaskMobileMaxSize.setText(Long.toString(task.MobileMaxSize));
        
        if(task.ID != -1) 
    	{
	    	txtTaskName.setText(task.Name);
	    	for(int index = 0; index < accounts.size(); index++)
	    		if(accounts.get(index).ID == task.AccountID) 
	    			ddlAccount.setSelection(((ArrayAdapter<AccountInfo>)ddlAccount.getAdapter()).getPosition(accounts.get(index)));
    	}
    }
	protected void bindDataItems()
    {
    	LinearLayout lstAccountData = (LinearLayout)findViewById(R.id.lstTaskData);
    	lstAccountData.removeAllViews();
    	
    	for(ListItem item : Cloud.getCloud(engine).getTaskListItems())
    	{
	        final TaskData d = TaskData.getByName(item.Name, task_data);

	        LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	        View v = vi.inflate(R.layout.advanced_task_data_item, null);
	        
	        final TextView lblTaskData = (TextView) v.findViewById(R.id.lblTaskData);
	        final  EditText txtTaskData = (EditText) v.findViewById(R.id.txtTaskData);
	        lblTaskData.setText(item.Subtext); 
	        txtTaskData.setText(d.Data);
	        txtTaskData.addTextChangedListener(new TextWatcher(){
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
    	task_data = SettingsDB.getAllTaskData(task.ID, getDBContext());
    	Cloud.getCloud(engine).initializeTaskDataFields(task_data);
    	bindDataItems();
    }
    protected void showDeleteConfirm()
    {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setMessage(TaskEditActivity.this.getString(R.string.delete_task_confirm))
    	       .setCancelable(false)
    	       .setPositiveButton(TaskEditActivity.this.getString(R.string.yes), new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	        	   deleteTask();
    	        	   TaskEditActivity.this.finish();
    	           }
    	       })
	       .setNegativeButton(TaskEditActivity.this.getString(R.string.no), new DialogInterface.OnClickListener() { public void onClick(DialogInterface dialog, int id) { } });
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
    	if(!quiet) return;
    	Log.v("TaskEditActivity", msg);
    }
}