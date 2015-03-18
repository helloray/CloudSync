package net.netortech.cloudsync.ui.wizard;

import java.util.ArrayList;
import java.util.LinkedList;

import net.netortech.cloudsync.BaseActivity;
import net.netortech.cloudsync.CloudSyncActivity;
import net.netortech.cloudsync.R;
import net.netortech.cloudsync.data.AccountData;
import net.netortech.cloudsync.data.AccountInfo;
import net.netortech.cloudsync.data.EngineInfo;
import net.netortech.cloudsync.data.SettingsDB;
import net.netortech.cloudsync.data.TaskData;
import net.netortech.cloudsync.data.TaskInfo;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public abstract class WizardStepActivity extends BaseActivity implements CustomLinearLayout.Listener {
	protected static final int ANONYMOUS_REQUEST_CODE = 1;
	
	protected static final int WIZARD_FINISH_RESULT_CODE = RESULT_FIRST_USER + 1;
	protected static final int WIZARD_RESTART_RESULT_CODE = RESULT_FIRST_USER + 2;
	protected static final int WIZARD_CANCEL_RESULT_CODE = RESULT_FIRST_USER + 3;
	protected static final int WIZARD_ACCOUNT_FAILED_RESULT_CODE = RESULT_FIRST_USER + 4;
	
	protected static final int CONFIRM_DELETE_ITEM_CODE = 1;
	protected static final int CONFIRM_SAVE_ITEM_CODE = 2;
	protected static final int CONFIRM_NEXT_STEP_CODE = 3;
	protected static final int CONFIRM_CONTINUE_CODE = 4;
	protected static final int CONFIRM_FINISHED_CODE = 5;
	protected static final int CONFIRM_GO_BACK_CODE = 6;
	
	public static final String BUNDLE_TASK_KEY = "Task";
	public static final String BUNDLE_TASK_DATA_KEY = "TaskData";
	public static final String BUNDLE_ENGINE_KEY = "Engine";
	public static final String BUNDLE_ENGINE_DATA_INFO_KEY = "EngineDataInfo";
	public static final String BUNDLE_ACCOUNT_KEY = "Account";
	public static final String BUNDLE_ACCOUNT_DATA_KEY = "AccountData";
	
	protected static final int FINISH_ANONYMOUS = 0;
	protected static final int FINISH_NO_ADVANCED = 1;
	
	protected final SettingsDB db = CloudSyncActivity.db;
	protected CustomLinearLayout llMain;
	
	protected View incTop;
	protected TextView lblStepTitle;
	protected TextView lblStepInstructions;
	protected TextView lblQuestionMark;
			
	protected LinearLayout llContent;
	
	protected View incBottom;
	protected Button btnBack;
	protected Button btnCancel;
	
	protected AccountInfo account;
	protected LinkedList<AccountData> account_data;
	protected TaskInfo task;
	protected LinkedList<TaskData> task_data;
	protected EngineInfo engine;

	@Override
	public final void onCreate(Bundle b)
	{
		super.onCreate(b);
		setContentView(getLayoutId());
		initWizardStepViews();
		
		getExtras();
		
		lblQuestionMark.setText("?");
		
		onWizardStepCreated(b);

		lblStepTitle.setText(getStepTitle());
		lblStepInstructions.setText(getStepInstructions());
	}

	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		log("onActivityResult triggered. resultCode: " + resultCode);
		switch(resultCode)
		{
			case RESULT_CANCELED:
				// User hit the back button.
				break;
			default:
				// pop
				setResult(resultCode, null);
				finish();
				break;
		}
	}
 
	@Override
    public void onSoftKeyboardShown(boolean isShowing) {
		// log("Current bottom visibility: " + incBottom.getVisibility() + " View.GONE: " + View.GONE);
        if(isShowing && incBottom.getVisibility() != View.GONE)
        {
        	View temp = getCurrentFocus();
        	incTop.setVisibility(View.GONE);
        	incBottom.setVisibility(View.GONE);
        	try { if(temp != null) temp.requestFocus(); } catch(Exception ex) {}
        	// log("Hiding edges.");
        }
        else if(!isShowing && incBottom.getVisibility() != View.VISIBLE)
        {
        	View temp = getCurrentFocus();
        	incTop.setVisibility(View.VISIBLE);
        	incBottom.setVisibility(View.VISIBLE);
        	try { if(temp != null) temp.requestFocus(); } catch(Exception ex) {}
        	// log("Showing edges.");
        }
    }
	
	private void initWizardStepViews()
	{
		llMain = (CustomLinearLayout)this.findViewById(R.id.llMain);
		llMain.setListener(this);
		
		incTop = this.findViewById(R.id.incTop);
		lblStepTitle = (TextView)this.findViewById(R.id.lblStepTitle);
		lblStepInstructions = (TextView)this.findViewById(R.id.lblStepInstructions);
		lblQuestionMark = (TextView)this.findViewById(R.id.lblQuestionMark);
				
		llContent = (LinearLayout)this.findViewById(R.id.llContent);
		
		incBottom = this.findViewById(R.id.incBottom);
		btnBack = (Button)this.findViewById(R.id.btnBack);
		btnCancel = (Button)this.findViewById(R.id.btnCancel);
		
		btnBack.setOnClickListener(new OnClickListener(){ public void onClick(View v) {	btnBack_Click(); }});
		btnCancel.setOnClickListener(new OnClickListener(){ public void onClick(View v) { btnCancel_Click(); }});
	}
	
	abstract void onWizardStepCreated(Bundle b);
	protected abstract int getLayoutId();
	protected abstract String getActivityName();
	protected abstract String getStepInstructions();
	protected abstract String getStepTitle();
	protected abstract void btnBack_Click();
	protected void btnCancel_Click() { setResult(WizardStepActivity.WIZARD_CANCEL_RESULT_CODE, null); finish(); }
	
	protected void confirmItem(Object item, int code) { throw new RuntimeException("Confirm item was not overloaded in " + this.getActivityName()); };
    protected void showConfirm(CharSequence message, Object item, int positiveCode, int negativeCode)
    {
    	final Object finalItem = item;
    	final int negativeFinalCode = negativeCode;
    	final int positiveFinalCode = positiveCode;
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setMessage(message)
    	       .setCancelable(false)
    	       .setPositiveButton(WizardStepActivity.this.getString(R.string.yes), new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	        	   	WizardStepActivity.this.confirmItem(finalItem, positiveFinalCode);
    	           }
    	       })
	       .setNegativeButton(WizardStepActivity.this.getString(R.string.no), new DialogInterface.OnClickListener() { 
	    	   public void onClick(DialogInterface dialog, int id) { 
	    		   if(negativeFinalCode != -1)
	    			   WizardStepActivity.this.confirmItem(finalItem, negativeFinalCode);
	    	   } 
    	   });
    	AlertDialog alert = builder.create();   
    	alert.show();
    }
    protected void showOkAlert(CharSequence message)
    {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setMessage(message)
    	       .setCancelable(false)
    	       .setPositiveButton(WizardStepActivity.this.getString(R.string.ok), new DialogInterface.OnClickListener() { public void onClick(DialogInterface dialog, int id) { } });
    	AlertDialog alert = builder.create();   
    	alert.show();
    }
    protected void alert(String msg)
    {
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
    
    protected void finishWizard()
    {
    	finishWizard(FINISH_ANONYMOUS);
    }
    protected void finishWizard(int code)
    {
    	Intent intent = new Intent(this, FinishedActivity.class);
    	putExtras(intent);
    	intent.putExtra("FinishCode", code);
    	startActivityForResult(intent, ANONYMOUS_REQUEST_CODE);
    }
    
    protected void putExtras(Intent intent)
    {
		if(task != null) { intent.putExtra(BUNDLE_TASK_KEY, task); log("task put in bundle: " + task.toString()); }
		if(task_data != null && task_data.size() != 0) { intent.putExtra(BUNDLE_TASK_DATA_KEY, task_data); log("task data put in bundle. count:" + task_data.size()); }
		if(engine != null) { intent.putExtra(BUNDLE_ENGINE_KEY, engine); log("engine put in bundle: " + engine.toString()); }
		if(account != null) { intent.putExtra(BUNDLE_ACCOUNT_KEY, account); log("account put in bundle:" + account.toString()); }
		if(task_data != null && account_data.size() != 0) { intent.putExtra(BUNDLE_ACCOUNT_DATA_KEY, account_data); log("account data put in bundle. count:" + account_data.size()); }
    }
    private void getExtras()
    {
    	Intent intent = getIntent();
    	task = intent.hasExtra(BUNDLE_TASK_KEY) ? (TaskInfo)intent.getSerializableExtra(BUNDLE_TASK_KEY) : null;
    	//if(task != null) log("task found in bundle: " + task.toString());
    	task_data = new LinkedList<TaskData>();
    	if(intent.hasExtra(BUNDLE_TASK_DATA_KEY)) task_data.addAll((ArrayList<TaskData>)intent.getSerializableExtra(BUNDLE_TASK_DATA_KEY)); 
    	//if(task_data.size() != 0) log("task data found in bundle. count: " + task_data.size());
    	engine = intent.hasExtra(BUNDLE_ENGINE_KEY) ? (EngineInfo)intent.getSerializableExtra(BUNDLE_ENGINE_KEY) : null;
    	//if(engine_data != null) log("engine data found in bundle: " + engine_data.Data);
    	account = intent.hasExtra(BUNDLE_ACCOUNT_KEY) ? (AccountInfo)intent.getSerializableExtra(BUNDLE_ACCOUNT_KEY) : null;
    	//if(account != null) log("account found in bundle: " + account.toString());
    	account_data = new LinkedList<AccountData>();
    	if(intent.hasExtra(BUNDLE_ACCOUNT_DATA_KEY)) account_data.addAll((ArrayList<AccountData>)intent.getSerializableExtra(BUNDLE_ACCOUNT_DATA_KEY));
    	//if(account_data.size() != 0) log("account data found in bundle. count: " + account_data.size());
    }
    
	protected void log(String message)
	{
		log(message, false);
	}
	protected void log(Exception ex)
	{
		log(ex, false);
	}
	protected void log(Exception ex, Boolean quiet)
	{
		String trace = "";
		for(StackTraceElement e : ex.getStackTrace()) trace += e.toString() + " ";
		log(ex.toString() + " trace: " + trace, true);
	}
	protected void log(String message, Boolean quiet)
	{
		if(!quiet) return;
		Log.v(getActivityName(), message);
	}
}
