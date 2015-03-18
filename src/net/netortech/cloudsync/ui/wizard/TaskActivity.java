package net.netortech.cloudsync.ui.wizard;

import java.util.List;

import net.netortech.cloudsync.R;
import net.netortech.cloudsync.data.AccountInfo;
import net.netortech.cloudsync.data.SettingsDB;
import net.netortech.cloudsync.data.TaskInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class TaskActivity extends WizardStepActivity {
	Button btnAddTask;
	ListView lstCurrentTasks;

	List<TaskInfo> tasks;
	
	@Override protected void btnBack_Click() { finish(); }
	@Override protected void btnCancel_Click() { finish(); }
	@Override protected String getActivityName() { return "TaskActivity"; }
	@Override protected int getLayoutId() { return R.layout.wizard_task; }
	@Override protected String getStepInstructions() { return "Would you like to create a new sync job or modify an existing job"; }
	@Override protected String getStepTitle() { return "Welcome to CloudSync!"; }
	
	@Override
	public void onWizardStepCreated(Bundle b)
	{
		tasks = SettingsDB.getAllTaskInfo(getDBContext());
		if(tasks.size() == 0)
		{
			startNextStep();
			finish();
		}
		initStep();
	}

	private void startNextStep()
	{
		task = task == null ? new TaskInfo() : task;
		task_data = SettingsDB.getAllTaskData(task.ID, getDBContext());
		Intent intent = new Intent(this, task.ID == -1 ? TaskNameActivity.class : AccountActivity.class);
		putExtras(intent);
		startActivityForResult(intent, ANONYMOUS_REQUEST_CODE);
	}
	
	@Override
	protected void confirmItem(Object item, int code)
	{
		switch(code)
		{
			case WizardStepActivity.CONFIRM_DELETE_ITEM_CODE:
				SettingsDB.Delete((TaskInfo)item, getDBContext());
				tasks = SettingsDB.getAllTaskInfo(getDBContext());
				bindList();
				break;
		}
	}
	
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	log("onActivityResult triggered. resultCode: " + resultCode);
    	switch(resultCode)
    	{
    		case RESULT_CANCELED:
    			break;
    		case WizardStepActivity.WIZARD_RESTART_RESULT_CODE:
    			tasks = SettingsDB.getAllTaskInfo(getDBContext());
    			bindList();
    			break;
    		default:
    			finish();
    			break;
    	}
    }
    
    private void initStep()
    {
    	btnAddTask = (Button)this.findViewById(R.id.btnAddTask);
    	btnAddTask.setOnClickListener(new OnClickListener() { public void onClick(View v) { startNextStep(); }});
    	lstCurrentTasks = (ListView)this.findViewById(R.id.lstCurrentTasks); 
    	bindList();
    }

	private void bindList()
    {
    	ArrayAdapter<TaskInfo> adapter = new ArrayAdapter<TaskInfo>(this, R.layout.wizard_edit_item, tasks) {
	        @Override
	        public View getView(int position, View convertView, ViewGroup parent) {
                LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                final TaskInfo item = getItem(position);
                final AccountInfo account = SettingsDB.getAccountInfo(item.AccountID, getDBContext());
                final View v = vi.inflate(R.layout.wizard_edit_item, null);
                final Button btnEdit = (Button)v.findViewById(R.id.btnSelect);
                final Button btnDelete = (Button)v.findViewById(R.id.btnDelete);
                final TextView t = (TextView) v.findViewById(R.id.lblItemTitle);
                final TextView d = (TextView) v.findViewById(R.id.lblItemDescription);
                
                btnEdit.setText(R.string.edit);
	        	OnClickListener editListener = new OnClickListener(){ public void onClick(View v) { task = item; startNextStep(); }};

                v.setOnClickListener(editListener);
                btnEdit.setOnClickListener(editListener);
                btnDelete.setOnClickListener(new OnClickListener() { public void onClick(View v) { 
                	TaskActivity.this.showConfirm(TaskActivity.this.getText(R.string.delete_task_confirm), item, WizardStepActivity.CONFIRM_DELETE_ITEM_CODE, -1); 
            	}});
                t.setText(item.Name);  
                d.setText(TaskActivity.this.getText(R.string.account_colon) + " " + account.Name);
                return v;
	        }
		};
		lstCurrentTasks.setAdapter(adapter);
	}
}
